package org.dersbian.compiler.lexer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.dersbian.compiler.Constants;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.error.ErrorCode;
import org.dersbian.compiler.lexer.token.SourceId;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.lexer.token.Token;
import org.dersbian.compiler.lexer.token.TokenKind;
import org.dersbian.compiler.lexer.token.parser.numeric.BaseParsers;
import org.dersbian.compiler.location.LineTracker;

/** A simple lexer for tokenizing Dersco source code. */
@SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.TooManyMethods"})
public class Lexer {

    /** Tracker used to map source positions to line numbers. */
    @Getter private final LineTracker lineTracker;

    /** Collected tokens produced by the lexer. */
    private final List<Token> tokens = new ArrayList<>();

    /** Collected lexical and syntax errors. */
    private final List<CompileError> errors = new ArrayList<>();

    /** Stable identifier of the source being lexed. */
    private final SourceId sourceId;

    /** Cursor over the source text; owns all position, line and column bookkeeping. */
    private final SourceCursor cursor;

    /** Creates a lexer for the given source file and text. */
    public Lexer(final Path filePath, final String source) {
        this.sourceId = new SourceId.FilePath(filePath);
        final String normalized = CodePoints.stripByteOrderMark(source);
        this.cursor = new SourceCursor(normalized);
        this.lineTracker = LineTracker.fromText(normalized);
    }

    /**
     * Tokenizes the source text and returns the resulting tokens and any errors encountered.
     *
     * @return the result containing the produced tokens and collected errors.
     */
    public LexerResult tokenize() {
        while (!cursor.isAtEnd()) {
            skipWhitespace();
            if (cursor.isAtEnd()) {
                break;
            }
            final int codePoint = cursor.peekCodePoint();
            switch (codePoint) {
                case '+',
                        '-',
                        '*',
                        '/',
                        '=',
                        '!',
                        '<',
                        '>',
                        '|',
                        '&',
                        '%',
                        '^',
                        ':',
                        ',',
                        '.',
                        '~' ->
                        scanOperator();
                case '(', '[', '{', ')', ']', '}' -> scanDelimiter();
                case '#' -> scanRadixLiteral();
                case '"' -> scanStringLiteral();
                case '\'' -> scanCharLiteral();
                default -> {
                    if (CodePoints.isIdentifierStart(codePoint)) {
                        scanIdentifierOrKeyword();
                    } else {
                        // TODO: wire up the remaining lexer rules (numbesr, comments...).
                        // Right now any character that isn't an operator start, a delimiter,
                        // a radix/string/char literal start or an identifier start is silently
                        // skipped instead of being reported.
                        // Codepoints (not raw chars) are consumed one at a time so that surrogate
                        // pairs are never split in half, and UTF-8 byte / code point offsets stay
                        // in sync.
                        final SourceLocation start = cursor.currentLocation();
                        final int ecodePoint = cursor.advance();
                        reportError(
                                ErrorCode.E0008,
                                start,
                                "Unrecognized character: '" + Character.toString(ecodePoint) + "'",
                                null);
                    }
                }
            }
        }
        tokens.add(Token.eof(sourceId, cursor.currentLocation()));
        return new LexerResult(tokens, errors);
    }

    private void scanDelimiter() {
        final SourceLocation start = cursor.currentLocation();
        final int codePoint = cursor.advance();
        final TokenKind.Simple.Delimiter kind =
                switch (codePoint) {
                    case '(' -> TokenKind.Simple.Delimiter.OPEN_PAREN;
                    case ')' -> TokenKind.Simple.Delimiter.CLOSE_PAREN;
                    case '[' -> TokenKind.Simple.Delimiter.OPEN_BRACKET;
                    case ']' -> TokenKind.Simple.Delimiter.CLOSE_BRACKET;
                    case '{' -> TokenKind.Simple.Delimiter.OPEN_BRACE;
                    case '}' -> TokenKind.Simple.Delimiter.CLOSE_BRACE;
                    default ->
                            throw new IllegalStateException(
                                    "Unreachable: unexpected delimiter code point '"
                                            + codePoint
                                            + "'");
                };
        final Span span = Span.create(start, cursor.currentLocation());
        tokens.add(Token.create(sourceId, kind, span));
    }

    /**
     * Consumes consecutive whitespace code points without producing any tokens for them. Line,
     * column, UTF-8 offset and code point offset bookkeeping is delegated to {@link
     * SourceCursor#advance()}, so it stays correct for multi-byte whitespace characters too.
     */
    private void skipWhitespace() {
        while (!cursor.isAtEnd() && CodePoints.isWhitespaceCodePoint(cursor.peekCodePoint())) {
            cursor.advance();
        }
    }

    /**
     * Scans a full identifier, type-keyword, keyword or boolean literal starting at the current
     * position, and appends the resulting token to {@link #tokens}. Code points are consumed one at
     * a time via {@link SourceCursor#advance()}, so multi-byte / surrogate-pair characters (e.g.
     * accented letters, CJK ideographs, emoji used as identifiers if allowed by {@link
     * Character#isUnicodeIdentifierPart(int)}) are never split.
     */
    private void scanIdentifierOrKeyword() {
        final StringBuilder builder = new StringBuilder();
        boolean asciiOnly = true;

        final SourceLocation start = cursor.currentLocation();

        int codePoint = cursor.peekCodePoint();
        while (CodePoints.isIdentifierPart(codePoint)) {
            if (!CodePoints.isAscii(codePoint)) {
                asciiOnly = false;
            }
            builder.appendCodePoint(codePoint);
            cursor.advance();
            codePoint = cursor.peekCodePoint();
        }

        final String value = builder.toString();
        final Span span = Span.create(start, cursor.currentLocation());
        final TokenKind kind = CodePoints.resolveIdentifierKind(value, asciiOnly);
        tokens.add(Token.create(sourceId, kind, span));
    }

    /**
     * Scans a single operator token (single- or multi-character) starting at the current position
     * and appends the resulting token to {@link #tokens}.
     *
     * <p>Precondition: {@link SourceCursor#peekCodePoint()} must correspond to one of the
     * characters handled by the dedicated {@code case} branch for operators in {@link #tokenize()}.
     */
    private void scanOperator() {
        final SourceLocation start = cursor.currentLocation();
        final TokenKind.Simple.Operator kind = consumeOperatorKind();
        final Span span = Span.create(start, cursor.currentLocation());
        tokens.add(Token.create(sourceId, kind, span));
    }

    @SuppressWarnings("PMD.CognitiveComplexity")
    private TokenKind.Simple.Operator consumeOperatorKind() {
        final int codePoint = cursor.advance();

        return switch (codePoint) {
            case '+' ->
                    cursor.match('+')
                            ? TokenKind.Simple.Operator.PLUS_PLUS
                            : cursor.match('=')
                                    ? TokenKind.Simple.Operator.PLUS_EQUAL
                                    : TokenKind.Simple.Operator.PLUS;
            case '-' ->
                    cursor.match('-')
                            ? TokenKind.Simple.Operator.MINUS_MINUS
                            : cursor.match('=')
                                    ? TokenKind.Simple.Operator.MINUS_EQUAL
                                    : TokenKind.Simple.Operator.MINUS;
            case '*' ->
                    cursor.match('=')
                            ? TokenKind.Simple.Operator.STAR_EQUAL
                            : TokenKind.Simple.Operator.STAR;
            case '/' ->
                    cursor.match('=')
                            ? TokenKind.Simple.Operator.SLASH_EQUAL
                            : TokenKind.Simple.Operator.SLASH;
            case '=' ->
                    cursor.match('=')
                            ? TokenKind.Simple.Operator.EQUAL_EQUAL
                            : TokenKind.Simple.Operator.EQUAL;
            case '!' ->
                    cursor.match('=')
                            ? TokenKind.Simple.Operator.NOT_EQUAL
                            : TokenKind.Simple.Operator.NOT;
            case '<' ->
                    cursor.match('<', '=')
                            ? TokenKind.Simple.Operator.SHIFT_LEFT_EQUAL
                            : cursor.match('<')
                                    ? TokenKind.Simple.Operator.SHIFT_LEFT
                                    : cursor.match('=')
                                            ? TokenKind.Simple.Operator.LESS_EQUAL
                                            : TokenKind.Simple.Operator.LESS;
            case '>' ->
                    cursor.match('>', '=')
                            ? TokenKind.Simple.Operator.SHIFT_RIGHT_EQUAL
                            : cursor.match('>')
                                    ? TokenKind.Simple.Operator.SHIFT_RIGHT
                                    : cursor.match('=')
                                            ? TokenKind.Simple.Operator.GREATER_EQUAL
                                            : TokenKind.Simple.Operator.GREATER;
            case '|' ->
                    cursor.match('|')
                            ? TokenKind.Simple.Operator.OR_OR
                            : cursor.match('=')
                                    ? TokenKind.Simple.Operator.OR_EQUAL
                                    : TokenKind.Simple.Operator.OR;
            case '&' ->
                    cursor.match('&')
                            ? TokenKind.Simple.Operator.AND_AND
                            : cursor.match('=')
                                    ? TokenKind.Simple.Operator.AND_EQUAL
                                    : TokenKind.Simple.Operator.AND;
            case '%' ->
                    cursor.match('=')
                            ? TokenKind.Simple.Operator.PERCENT_EQUAL
                            : TokenKind.Simple.Operator.PERCENT;
            case '^' ->
                    cursor.match('=')
                            ? TokenKind.Simple.Operator.XOR_EQUAL
                            : TokenKind.Simple.Operator.XOR;
            case ':' -> TokenKind.Simple.Operator.COLON;
            case ',' -> TokenKind.Simple.Operator.COMMA;
            case '.' -> TokenKind.Simple.Operator.DOT;
            case '~' -> TokenKind.Simple.Operator.BITWISE_NOT;
            default ->
                    throw new IllegalStateException(
                            "Unreachable: unexpected operator start code point '"
                                    + codePoint
                                    + "'");
        };
    }

    private void scanRadixLiteral() {
        final SourceLocation start = cursor.currentLocation();
        cursor.advance(); // consume '#'

        final char prefixLetter = radixPrefixLetter(cursor.peekCodePoint());
        if (prefixLetter == 0) {
            reportError(
                    ErrorCode.E0001,
                    start,
                    "Invalid numeric prefix after '#': expected 'b', 'o' or 'x'.",
                    null);
        } else {
            cursor.advance(); // consume the prefix letter

            final int radix = radixFromPrefixLetter(prefixLetter);
            final String body = scanRadixDigits(radix);
            if (body.isEmpty()) {
                reportError(
                        ErrorCode.E0001,
                        start,
                        "Empty numeric literal after prefix '#" + prefixLetter + "'.",
                        null);
            } else {
                final String suffix = scanRadixSuffix();
                final String literal = "#" + prefixLetter + body + suffix;
                final Span span = Span.create(start, cursor.currentLocation());

                try {
                    tokens.add(Token.create(sourceId, parseRadixToken(radix, literal), span));
                } catch (final NumberFormatException e) {
                    reportRadixOverflow(radix, start, literal);
                }
            }
        }
    }

    private static char radixPrefixLetter(final int prefixChar) {
        return switch (prefixChar) {
            case 'b' -> 'b';
            case 'o' -> 'o';
            case 'x' -> 'x';
            default -> 0;
        };
    }

    private static int radixFromPrefixLetter(final char prefixLetter) {
        return switch (prefixLetter) {
            case 'b' -> Constants.RADIX_BINARY;
            case 'o' -> Constants.RADIX_OCTAL;
            default -> Constants.RADIX_HEX;
        };
    }

    private String scanRadixDigits(final int radix) {
        final StringBuilder body = new StringBuilder();
        int codePoint = cursor.peekCodePoint();
        while (isRadixDigit(codePoint, radix)) {
            body.appendCodePoint(codePoint);
            cursor.advance();
            codePoint = cursor.peekCodePoint();
        }
        return body.toString();
    }

    private String scanRadixSuffix() {
        String suffix = "";
        final int codePoint = cursor.peekCodePoint();
        if (codePoint == 'u' || codePoint == 'U') {
            cursor.advance();
            suffix = Character.toString(codePoint);
        }
        return suffix;
    }

    private static TokenKind parseRadixToken(final int radix, final String literal) {
        return switch (radix) {
            case Constants.RADIX_BINARY -> new TokenKind.Binary(BaseParsers.parseBinary(literal));
            case Constants.RADIX_OCTAL -> new TokenKind.Octal(BaseParsers.parseOctal(literal));
            default -> new TokenKind.Hexadecimal(BaseParsers.parseHex(literal));
        };
    }

    private void reportRadixOverflow(
            final int radix, final SourceLocation start, final String literal) {
        final ErrorCode code =
                switch (radix) {
                    case Constants.RADIX_BINARY -> ErrorCode.E0002;
                    case Constants.RADIX_OCTAL -> ErrorCode.E0003;
                    default -> ErrorCode.E0004;
                };
        reportError(code, start, "Numeric value out of range for literal '" + literal + "'.", null);
    }

    private static boolean isRadixDigit(final int codePoint, final int radix) {
        return switch (radix) {
            case Constants.RADIX_BINARY -> codePoint == '0' || codePoint == '1';
            case Constants.RADIX_OCTAL -> codePoint >= '0' && codePoint <= '7';
            case Constants.RADIX_HEX -> isHexDigit(codePoint);
            default -> false;
        };
    }

    // ------------------------------------------------------------------
    // String / char literals
    // ------------------------------------------------------------------

    /**
     * Scans a string literal delimited by double quotes, handling escape sequences and multi-byte
     * Unicode characters. If the string is not terminated before the end of the line or the end of
     * the source, {@link ErrorCode#E0005} is reported, but the token is still produced with the
     * content accumulated so far, to allow the parser to recover.
     */
    private void scanStringLiteral() {
        final SourceLocation start = cursor.currentLocation();
        cursor.advance(); // consume the opening double quote

        final StringBuilder builder = new StringBuilder();
        boolean terminated = false;

        while (!cursor.isAtEnd()) {
            final int codePoint = cursor.peekCodePoint();
            if (codePoint == Constants.CHAR_DOUBLE_QUOTE) { // was: codePoint == '"'
                cursor.advance();
                terminated = true;
                break;
            }
            if (isLineTerminator(codePoint)) {
                break;
            }
            if (codePoint == Constants.CHAR_BACKSLASH) { // was: codePoint == '\\'
                consumeEscapeSequence(builder);
            } else {
                builder.appendCodePoint(codePoint);
                cursor.advance();
            }
        }

        if (!terminated) {
            reportError(ErrorCode.E0005, start, "Unterminated string literal.", null);
        }

        final Span span = Span.create(start, cursor.currentLocation());
        tokens.add(Token.create(sourceId, new TokenKind.StringLiteral(builder.toString()), span));
    }

    /**
     * Scans an ASCII/Unicode char literal, handling escape sequences (including {@code
     * \u005cu{XXXX}}). Reports {@link ErrorCode#E0006} if the literal is not correctly terminated
     * with a single character.
     */
    private void scanCharLiteral() {
        final SourceLocation start = cursor.currentLocation();
        cursor.advance(); // consume the opening quote

        final StringBuilder builder = new StringBuilder();

        if (cursor.isAtEnd() || isLineTerminator(cursor.peekCodePoint())) {
            reportError(ErrorCode.E0006, start, "Unterminated char literal.", null);
        } else if (cursor.peekCodePoint() == Constants.CHAR_SINGLE_QUOTE) {
            cursor.advance();
            reportError(ErrorCode.E0006, start, "Empty char literal.", null);
        } else {
            consumeCharLiteralBody(builder, start);
        }

        final Span span = Span.create(start, cursor.currentLocation());
        tokens.add(Token.create(sourceId, new TokenKind.CharLiteral(builder.toString()), span));
    }

    /**
     * Consumes the body of a char literal (after the opening quote has been consumed), including
     * the closing quote. Reports an error if the literal contains more than one character or is not
     * properly terminated.
     */
    private void consumeCharLiteralBody(final StringBuilder builder, final SourceLocation start) {
        consumeSingleCharOrEscape(builder);
        if (isClosingQuote()) {
            cursor.advance();
        } else {
            skipToClosingQuoteOrLineEnd();
            reportError(
                    ErrorCode.E0006,
                    start,
                    "Unterminated char literal or containing more than one character.",
                    null);
        }
    }

    /** Consumes exactly one logical character or escape sequence into {@code builder}. */
    private void consumeSingleCharOrEscape(final StringBuilder builder) {
        if (cursor.peekCodePoint() == Constants.CHAR_BACKSLASH) {
            consumeEscapeSequence(builder);
        } else {
            builder.appendCodePoint(cursor.peekCodePoint());
            cursor.advance();
        }
    }

    /**
     * Returns {@code true} if the cursor is not at end and the current code point is a single-quote
     * closing delimiter.
     */
    private boolean isClosingQuote() {
        return !cursor.isAtEnd() && cursor.peekCodePoint() == Constants.CHAR_SINGLE_QUOTE;
    }

    /**
     * Advances past any extra characters until the closing single-quote or a line terminator is
     * reached, then consumes the closing quote if present.
     */
    private void skipToClosingQuoteOrLineEnd() {
        while (!cursor.isAtEnd()
                && cursor.peekCodePoint() != Constants.CHAR_SINGLE_QUOTE
                && !isLineTerminator(cursor.peekCodePoint())) {
            cursor.advance();
        }
        if (isClosingQuote()) {
            cursor.advance();
        }
    }

    /**
     * Consumes and resolves an escape sequence starting with {@code '\'} under the cursor, adding
     * the resulting code point(s) to {@code builder}. If the sequence is unrecognized or malformed,
     * {@link ErrorCode#E0007} is reported.
     */
    private void consumeEscapeSequence(final StringBuilder builder) {
        final SourceLocation escapeStart = cursor.currentLocation();
        cursor.advance(); // consume '\'

        if (cursor.isAtEnd()) {
            reportError(ErrorCode.E0007, escapeStart, "Unterminated escape sequence.", null);
            return;
        }

        final int codePoint = cursor.peekCodePoint();
        switch (codePoint) {
            case 'n' -> {
                builder.append('\n');
                cursor.advance();
            }
            case 'r' -> {
                builder.append('\r');
                cursor.advance();
            }
            case 't' -> {
                builder.append('\t');
                cursor.advance();
            }
            case '\\' -> {
                builder.append('\\');
                cursor.advance();
            }
            case '\'' -> {
                builder.append('\'');
                cursor.advance();
            }
            case '"' -> {
                builder.append('"');
                cursor.advance();
            }
            case '0' -> {
                builder.append('\0');
                cursor.advance();
            }
            case 'x' -> consumeHexEscape(builder, escapeStart);
            case 'u' -> consumeUnicodeEscape(builder, escapeStart, 'u');
            case 'U' -> consumeUnicodeEscape(builder, escapeStart, 'U');
            default -> {
                reportError(
                        ErrorCode.E0007,
                        escapeStart,
                        "Invalid escape sequence: '\\" + Character.toString(codePoint) + "'.",
                        null);
                // recovery: consume the character anyway to avoid infinite loops.
                builder.appendCodePoint(codePoint);
                cursor.advance();
            }
        }
    }

    /**
     * Consumes a Unicode escape sequence in the format {@code \u005cu{XXXX}}, where {@code XXXX} is
     * a sequence of hexadecimal digits representing a valid Unicode code point. At the time of the
     * call, the cursor must be positioned on the 'u' following the backslash.
     */
    private void consumeUnicodeEscape(
            final StringBuilder builder,
            final SourceLocation escapeStart,
            final char prefixLetter) {

        final int maxDigits =
                (prefixLetter == 'U')
                        ? Constants.UNICODE_ESCAPE_LONG_DIGIT_COUNT // 8
                        : Constants.UNICODE_ESCAPE_SHORT_DIGIT_COUNT; // 4

        cursor.advance(); // consume 'u' or 'U'

        boolean valid = expectOpenBrace(escapeStart);

        String hex = null;
        if (valid) {
            hex = consumeHexDigits(maxDigits);

            // Check if the digit limit was reached before the '}'
            if (!cursor.isAtEnd()
                    && cursor.peekCodePoint() != '}'
                    && isHexDigit(cursor.peekCodePoint())) {
                reportError(
                        ErrorCode.E0007,
                        escapeStart,
                        "Unicode escape sequence with too many digits: "
                                + "maximum "
                                + maxDigits
                                + " hexadecimal digits for '\\"
                                + prefixLetter
                                + "'.",
                        null);
                drainExcessHexDigits();
                valid = false;
            } else {
                valid = expectClosingBrace(escapeStart, hex);
            }
        }

        if (valid) {
            appendUnicodeCodePoint(builder, escapeStart, hex);
        }
    }

    /**
     * Recovery method: advances the cursor past excess hexadecimal digits until reaching {@code
     * '}'}, a non-hexadecimal character, or the end of the source. If a {@code '}'} is present at
     * the end, it is consumed to keep the cursor in a consistent state and allow lexing to
     * continue.
     */
    private void drainExcessHexDigits() {
        while (!cursor.isAtEnd()
                && cursor.peekCodePoint() != '}'
                && isHexDigit(cursor.peekCodePoint())) {
            cursor.advance();
        }
        // Consume the closing '}' if present, to avoid leaving the cursor on it
        if (!cursor.isAtEnd() && cursor.peekCodePoint() == '}') {
            cursor.advance();
        }
    }

    private boolean expectOpenBrace(final SourceLocation escapeStart) {
        boolean isValid = true;

        if (cursor.isAtEnd() || cursor.peekCodePoint() != '{') {
            reportError(
                    ErrorCode.E0007,
                    escapeStart,
                    "Invalid unicode escape sequence: expected '{' after \\u.",
                    null);
            isValid = false;
        } else {
            cursor.advance(); // consume '{'
        }

        return isValid;
    }

    private String consumeHexDigits(final int maxDigits) {
        final StringBuilder hex = new StringBuilder();
        while (hex.length() < maxDigits
                && !cursor.isAtEnd()
                && cursor.peekCodePoint() != '}'
                && isHexDigit(cursor.peekCodePoint())) {
            hex.appendCodePoint(cursor.peekCodePoint());
            cursor.advance();
        }
        return hex.toString();
    }

    private boolean expectClosingBrace(final SourceLocation escapeStart, final String hex) {
        boolean result = true;

        if (hex.isEmpty()) {
            reportError(
                    ErrorCode.E0007,
                    escapeStart,
                    "Unicode escape sequence without hexadecimal digits.",
                    null);
            result = false;
        } else if (cursor.isAtEnd() || cursor.peekCodePoint() != '}') {
            reportError(
                    ErrorCode.E0007,
                    escapeStart,
                    "Unterminated unicode escape sequence: missing '}'.",
                    null);
            result = false;
        } else {
            cursor.advance(); // consume '}'
        }

        return result;
    }

    private void appendUnicodeCodePoint(
            final StringBuilder builder, final SourceLocation escapeStart, final String hex) {
        try {
            final int codePointValue = Integer.parseInt(hex, 16);
            if (Character.isValidCodePoint(codePointValue)) {
                builder.appendCodePoint(codePointValue);
            } else {
                reportError(
                        ErrorCode.E0007, escapeStart, "Invalid unicode code point: U+" + hex, null);
            }
        } catch (final NumberFormatException e) {
            reportError(
                    ErrorCode.E0007,
                    escapeStart,
                    "Invalid hexadecimal value in unicode escape sequence.",
                    null);
        }
    }

    /**
     * Consumes a two-digit hexadecimal escape sequence in the format {@code \xHH}, where {@code HH}
     * represents a byte value between {@code 0x00} and {@code 0xFF}. At the time of the call, the
     * cursor must be positioned on the 'x' following the backslash. If the number of digits differs
     * from {@link Constants#HEX_ESCAPE_DIGIT_COUNT}, {@link ErrorCode#E0007} is reported.
     */
    private void consumeHexEscape(final StringBuilder builder, final SourceLocation escapeStart) {
        cursor.advance(); // consume 'x'

        final StringBuilder hex = new StringBuilder();
        while (hex.length() < Constants.HEX_ESCAPE_DIGIT_COUNT
                && !cursor.isAtEnd()
                && isHexDigit(cursor.peekCodePoint())) {
            hex.appendCodePoint(cursor.peekCodePoint());
            cursor.advance();
        }

        if (hex.length() != Constants.HEX_ESCAPE_DIGIT_COUNT) {
            reportError(
                    ErrorCode.E0007,
                    escapeStart,
                    "Invalid hexadecimal escape sequence: expected exactly "
                            + Constants.HEX_ESCAPE_DIGIT_COUNT
                            + " hexadecimal digits after \\x.",
                    null);
            return;
        }

        final int value = Integer.parseInt(hex.toString(), 16);
        builder.appendCodePoint(value);
    }

    private static boolean isLineTerminator(final int codePoint) {
        return codePoint == Constants.LINE_FEED
                || codePoint == Constants.CARRIAGE_RETURN
                || codePoint == Constants.LINE_SEPARATOR
                || codePoint == Constants.PARAGRAPH_SEPARATOR;
    }

    private static boolean isHexDigit(final int codePoint) {
        return (codePoint >= '0' && codePoint <= '9')
                || (codePoint >= 'a' && codePoint <= 'f')
                || (codePoint >= 'A' && codePoint <= 'F');
    }

    private void reportError(
            final ErrorCode errorCode,
            final SourceLocation start,
            final String message,
            final String help) {
        final Span span = Span.create(start, cursor.currentLocation());
        errors.add(CompileError.lexerError(errorCode, message, span, help));
    }

    /** Returns the number of source lines tracked by this lexer. */
    public int lineCount() {
        return lineTracker.lineCount();
    }
}
