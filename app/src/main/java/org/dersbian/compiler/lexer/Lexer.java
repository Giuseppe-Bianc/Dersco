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
    @SuppressWarnings("PMD.CyclomaticComplexity")
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
                case '(', '[', '{', ')', ']', '}' -> scanDeliminter();
                case '#' -> scanRadixLiteral();
                default -> {
                    if (CodePoints.isIdentifierStart(codePoint)) {
                        scanIdentifierOrKeyword();
                    } else {
                        // TODO: wire up the remaining lexer rules (literals, comments...).
                        // Right now any character that isn't an operator start or
                        // an identifier start is silently skipped instead of being reported: this
                        // is not wired to `errors` yet, so invalid input currently tokenizes
                        // without complaint.
                        // Codepoints (not raw chars) are consumed one at a time so that surrogate
                        // pairs are never split in half, and UTF-8 byte / code point offsets stay
                        // in sync.
                        cursor.advance();
                    }
                }
            }
        }
        tokens.add(Token.eof(sourceId, cursor.currentLocation()));
        return new LexerResult(tokens, errors);
    }

    private void scanDeliminter() {
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
        while (CodePoints.isWhitespaceCodePoint(cursor.peekCodePoint())) {
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
     * <p>Precondizione: {@link SourceCursor#peekCodePoint()} deve corrispondere a uno dei caratteri
     * gestiti dal ramo {@code case} dedicato agli operatori in {@link #tokenize()}.
     */
    private void scanOperator() {
        final SourceLocation start = cursor.currentLocation();
        final TokenKind.Simple.Operator kind = consumeOperatorKind();
        final Span span = Span.create(start, cursor.currentLocation());
        tokens.add(Token.create(sourceId, kind, span));
    }

    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.CognitiveComplexity"})
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
            case '*' -> TokenKind.Simple.Operator.STAR;
            case '/' -> TokenKind.Simple.Operator.SLASH;
            case '=' ->
                    cursor.match('=')
                            ? TokenKind.Simple.Operator.EQUAL_EQUAL
                            : TokenKind.Simple.Operator.EQUAL;
            case '!' ->
                    cursor.match('=')
                            ? TokenKind.Simple.Operator.NOT_EQUAL
                            : TokenKind.Simple.Operator.NOT;
            case '<' ->
                    cursor.match('=')
                            ? TokenKind.Simple.Operator.LESS_EQUAL
                            : cursor.match('<', '=')
                                    ? TokenKind.Simple.Operator.SHIFT_LEFT_EQUAL
                                    : cursor.match('<')
                                            ? TokenKind.Simple.Operator.SHIFT_LEFT
                                            : TokenKind.Simple.Operator.LESS;
            case '>' ->
                    cursor.match('=')
                            ? TokenKind.Simple.Operator.GREATER_EQUAL
                            : cursor.match('>', '=')
                                    ? TokenKind.Simple.Operator.SHIFT_RIGHT_EQUAL
                                    : cursor.match('>')
                                            ? TokenKind.Simple.Operator.SHIFT_RIGHT
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
        cursor.advance(); // consuma '#'

        final char prefixLetter = radixPrefixLetter(cursor.peekCodePoint());
        if (prefixLetter == 0) {
            reportError(
                    ErrorCode.E0001,
                    start,
                    "Prefisso numerico non valido dopo '#': atteso 'b', 'o' o 'x'.",
                    null);
        } else {
            cursor.advance(); // consuma la lettera di prefisso

            final int radix = radixFromPrefixLetter(prefixLetter);
            final String body = scanRadixDigits(radix);
            if (body.isEmpty()) {
                reportError(
                        ErrorCode.E0001,
                        start,
                        "Letterale numerico vuoto dopo il prefisso '#" + prefixLetter + "'.",
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
        reportError(
                code,
                start,
                "Valore numerico fuori range per il letterale '" + literal + "'.",
                null);
    }

    private static boolean isRadixDigit(final int codePoint, final int radix) {
        return switch (radix) {
            case Constants.RADIX_BINARY -> codePoint == '0' || codePoint == '1';
            case Constants.RADIX_OCTAL -> codePoint >= '0' && codePoint <= '7';
            case Constants.RADIX_HEX ->
                    (codePoint >= '0' && codePoint <= '9')
                            || (codePoint >= 'a' && codePoint <= 'f')
                            || (codePoint >= 'A' && codePoint <= 'F');
            default -> false;
        };
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
