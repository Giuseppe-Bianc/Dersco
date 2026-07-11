package org.dersbian.compiler.lexer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.lexer.token.SourceId;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.lexer.token.Token;
import org.dersbian.compiler.lexer.token.TokenKind;
import org.dersbian.compiler.location.LineTracker;

/** A simple lexer for tokenizing Dersco source code. */
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
                default -> {
                    if (CodePoints.isIdentifierStart(codePoint)) {
                        scanIdentifierOrKeyword();
                    } else {
                        // TODO: wire up the remaining lexer rules (delimiters, literals,
                        // comments...). Right now any character that isn't an operator start or
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

    /** Returns the number of source lines tracked by this lexer. */
    public int lineCount() {
        return lineTracker.lineCount();
    }
}
