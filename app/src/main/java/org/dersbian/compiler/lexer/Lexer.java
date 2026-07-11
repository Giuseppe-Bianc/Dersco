package org.dersbian.compiler.lexer;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.dersbian.compiler.Constants;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.lexer.token.SourceId;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.lexer.token.Token;
import org.dersbian.compiler.lexer.token.TokenKind;
import org.dersbian.compiler.location.LineTracker;

/** A simple lexer for tokenizing Dersco source code. */
// @SuppressWarnings({"UnusedMethod", "PMD.UnusedPrivateMethod", "PMD.TooManyMethods"})
public class Lexer {

    /** Tracker used to map source positions to line numbers. */
    @Getter private final LineTracker lineTracker;

    /** Collected tokens produced by the lexer. */
    private final List<Token> tokens = new ArrayList<>();

    /** Collected lexical and syntax errors. */
    private final List<CompileError> errors = new ArrayList<>();

    /** Mappa statica, immutabile, di tutte le keyword e type-keyword del linguaggio. */
    private static final Map<String, TokenKind.Simple> KEYWORDS =
            Map.ofEntries(
                    Map.entry("fun", TokenKind.Simple.Keyword.FUN),
                    Map.entry("if", TokenKind.Simple.Keyword.IF),
                    Map.entry("else", TokenKind.Simple.Keyword.ELSE),
                    Map.entry("return", TokenKind.Simple.Keyword.RETURN),
                    Map.entry("while", TokenKind.Simple.Keyword.WHILE),
                    Map.entry("for", TokenKind.Simple.Keyword.FOR),
                    Map.entry("main", TokenKind.Simple.Keyword.MAIN),
                    Map.entry("var", TokenKind.Simple.Keyword.VAR),
                    Map.entry("const", TokenKind.Simple.Keyword.CONST),
                    Map.entry("nullptr", TokenKind.Simple.Keyword.NULLPTR),
                    Map.entry("break", TokenKind.Simple.Keyword.BREAK),
                    Map.entry("continue", TokenKind.Simple.Keyword.CONTINUE),
                    Map.entry("i8", TokenKind.Simple.TypeKeyword.I8),
                    Map.entry("i16", TokenKind.Simple.TypeKeyword.I16),
                    Map.entry("i32", TokenKind.Simple.TypeKeyword.I32),
                    Map.entry("i64", TokenKind.Simple.TypeKeyword.I64),
                    Map.entry("u8", TokenKind.Simple.TypeKeyword.U8),
                    Map.entry("u16", TokenKind.Simple.TypeKeyword.U16),
                    Map.entry("u32", TokenKind.Simple.TypeKeyword.U32),
                    Map.entry("u64", TokenKind.Simple.TypeKeyword.U64),
                    Map.entry("f32", TokenKind.Simple.TypeKeyword.F32),
                    Map.entry("f64", TokenKind.Simple.TypeKeyword.F64),
                    Map.entry("char", TokenKind.Simple.TypeKeyword.CHAR),
                    Map.entry("string", TokenKind.Simple.TypeKeyword.STRING),
                    Map.entry("bool", TokenKind.Simple.TypeKeyword.BOOL));

    /** Source text being lexed (decoded to a Java {@code String}, i.e. UTF-16 internally). */
    private final String source;

    /** Length of the source text expressed in UTF-16 code units. */
    private final int length;

    /** Stable identifier of the source being lexed. */
    private final SourceId sourceId;

    /**
     * Current absolute offset into the source text, expressed in UTF-8 bytes. This mirrors the
     * on-disk byte offset of the original UTF-8 encoded file and is surfaced via {@link
     * SourceLocation#utf8Offset()}, unlike {@link #position} which counts UTF-16 code units.
     */
    private long utf8Offset;

    /** Current absolute offset into the source text, expressed in UTF-16 code units. */
    private int position;

    /**
     * Number of Unicode code points consumed so far. Differs from {@link #position} whenever the
     * source contains characters outside the Basic Multilingual Plane (encoded as surrogate pairs,
     * i.e. two UTF-16 code units for a single code point).
     */
    private long codePointOffset;

    /** Current 1-based line number in the source text. */
    private int lineNumber;

    /** Current 1-based column number in the source text. */
    private int columnNumber;

    /**
     * Removes a leading UTF-8 byte-order mark ({@code U+FEFF}), if present. Some editors write it
     * at the start of UTF-8 encoded files; if left untouched it would surface as a spurious invalid
     * character at position zero.
     */
    private static String stripByteOrderMark(final String text) {
        final String result;
        if (!text.isEmpty() && text.codePointAt(0) == Constants.BYTE_ORDER_MARK) {
            result = text.substring(Character.charCount(Constants.BYTE_ORDER_MARK));
        } else {
            result = text;
        }
        return result;
    }

    /**
     * Computes how many bytes the given Unicode code point would occupy when encoded as UTF-8. This
     * lets {@link #utf8Offset} track the real byte offset in the original source file, even though
     * the in-memory representation is a Java {@code String} (UTF-16).
     */
    private static int utf8ByteLength(final int codePoint) {
        final int result;
        if (codePoint < Constants.UTF8_ONE_BYTE_LIMIT) {
            result = Constants.UTF8_ONE_BYTE_LENGTH;
        } else if (codePoint < Constants.UTF8_TWO_BYTE_LIMIT) {
            result = Constants.UTF8_TWO_BYTE_LENGTH;
        } else if (codePoint < Constants.UTF8_THREE_BYTE_LIMIT) {
            result = Constants.UTF8_THREE_BYTE_LENGTH;
        } else {
            result = Constants.UTF8_FOUR_BYTE_LENGTH;
        }
        return result;
    }

    /*
        private boolean match(final int expected) {
            final boolean matched = peekCodePoint() == expected;
            if (matched) {
                advance();
            }
            return matched;
        }

        private int peekNextCodePoint() {
            final int result;
            if (isAtEnd()) {
                result = -1;
            } else {
                final int codePoint = source.codePointAt(position);
                final int next = position + Character.charCount(codePoint);
                result = next < length ? source.codePointAt(next) : -1;
            }
            return result;
        }
    */
    /**
     * Returns {@code true} if the given code point is considered whitespace and should be skipped
     * between tokens. This combines {@link Character#isWhitespace(int)} (which covers standard
     * ASCII whitespace such as space, tab, newline, carriage return, form feed, vertical tab, plus
     * most Unicode whitespace) with {@link Character#isSpaceChar(int)} (which additionally covers
     * Unicode space separators that {@code isWhitespace} deliberately excludes, e.g. the
     * non-breaking space {@code U+00A0}, the figure space {@code U+2007} and the narrow no-break
     * space {@code U+202F}), so that any whitespace-looking byte sequence from a UTF-8 source file
     * is skipped regardless of which category it falls under.
     */
    private static boolean isWhitespaceCodePoint(final int codePoint) {
        return codePoint != -1
                && (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint));
    }

    /**
     * Consumes consecutive whitespace code points (ASCII whitespace such as space/tab/newline as
     * well as Unicode whitespace and space-separator characters that may appear when the source
     * file is UTF-8 encoded), without producing any tokens for them. Line/column, UTF-8 offset and
     * code point offset bookkeeping is delegated to {@link #advance()} so it stays correct for
     * multi-byte whitespace characters too.
     */
    private void skipWhitespace() {
        while (isWhitespaceCodePoint(peekCodePoint())) {
            advance();
        }
    }

    /**
     * Returns {@code true} se il code point può iniziare un identificatore: lettera Unicode
     * (secondo {@link Character#isUnicodeIdentifierStart(int)}) oppure underscore {@code '_'}.
     */
    private static boolean isIdentifierStart(final int codePoint) {
        return codePoint != -1
                && (codePoint == '_' || Character.isUnicodeIdentifierStart(codePoint));
    }

    /**
     * Returns {@code true} se il code point può proseguire un identificatore già iniziato: lettere,
     * cifre e altri caratteri Unicode "identifier part" (secondo {@link
     * Character#isUnicodeIdentifierPart(int)}), oltre all'underscore {@code '_'}.
     */
    private static boolean isIdentifierPart(final int codePoint) {
        return codePoint != -1
                && (codePoint == '_' || Character.isUnicodeIdentifierPart(codePoint));
    }

    /**
     * Scans a full identifier, type-keyword, keyword or boolean literal starting at the current
     * position, and appends the resulting token to {@link #tokens}. Code points are consumed one at
     * a time via {@link #advance()}, so multi-byte / surrogate-pair characters (e.g. accented
     * letters, CJK ideographs, emoji used as identifiers if allowed by {@link
     * Character#isUnicodeIdentifierPart(int)}) are never split.
     *
     * <p>Resolution order:
     *
     * <ol>
     *   <li>{@code true} / {@code false} → {@link TokenKind.KeywordBool}
     *   <li>presente in {@link #KEYWORDS} → {@link TokenKind.Simple.Keyword} o {@link
     *       TokenKind.Simple.TypeKeyword}
     *   <li>altrimenti → {@link TokenKind.IdentifierAscii} se composto solo da code point ASCII,
     *       {@link TokenKind.IdentifierUnicode} altrimenti
     * </ol>
     */
    private void scanIdentifierOrKeyword() {
        final SourceLocation start = getCurrentLocation();
        final StringBuilder builder = new StringBuilder();
        boolean asciiOnly = true;

        int codePoint = peekCodePoint();
        while (isIdentifierPart(codePoint)) {
            if (codePoint > Constants.UTF8_ONE_BYTE_LIMIT - 1) {
                asciiOnly = false;
            }
            builder.appendCodePoint(codePoint);
            advance();
            codePoint = peekCodePoint();
        }

        final String value = builder.toString();
        final Span span = Span.create(start, getCurrentLocation());
        final TokenKind kind = resolveIdentifierKind(value, asciiOnly);
        tokens.add(Token.create(sourceId, kind, span));
    }

    /**
     * Determina il {@link TokenKind} corretto per un lessema già interamente scansionato, secondo
     * la logica descritta in {@link #scanIdentifierOrKeyword()}.
     */
    private static TokenKind resolveIdentifierKind(final String value, final boolean asciiOnly) {
        final TokenKind kind;
        if (Constants.TRUE_LITERAL.equals(value)) {
            kind = new TokenKind.KeywordBool(true);
        } else if (Constants.FALSE_LITERAL.equals(value)) {
            kind = new TokenKind.KeywordBool(false);
        } else if (KEYWORDS.containsKey(value)) {
            kind = KEYWORDS.get(value);
        } else if (asciiOnly) {
            kind = new TokenKind.IdentifierAscii(value);
        } else {
            kind = new TokenKind.IdentifierUnicode(value);
        }
        return kind;
    }

    /** Creates a lexer for the given source file and text. */
    public Lexer(final Path filePath, final String source) {
        this.sourceId = new SourceId.FilePath(filePath);
        this.source = stripByteOrderMark(source);
        this.length = this.source.length();
        this.lineTracker = LineTracker.fromText(this.source);
        this.utf8Offset = 0L;
        this.position = 0;
        this.codePointOffset = 0L;
        this.columnNumber = 1;
        this.lineNumber = 1;
    }

    /**
     * Tokenizes the source text and returns the resulting tokens and any errors encountered.
     *
     * @return the result containing the produced tokens and collected errors.
     */
    public LexerResult tokenize() {
        while (!isAtEnd()) {
            skipWhitespace();
            if (isAtEnd()) {
                break;
            }
            final int codePoint = peekCodePoint();
            if (isIdentifierStart(codePoint)) {
                scanIdentifierOrKeyword();
            } else {
                // TODO: wire up the remaining lexer rules (operators, delimiters, literals,
                // comments...). Codepoints (not raw chars) are consumed one at a time so that
                // surrogate pairs (characters outside the BMP, e.g. many emoji) are never split
                // in half, and UTF-8 byte / code point offsets stay in sync.
                advance();
            }
        }
        tokens.add(Token.eof(sourceId, getCurrentLocation()));
        return new LexerResult(tokens, errors);
    }

    /**
     * Builds the {@link SourceLocation} for the current cursor position, fully populating the
     * UTF-16 based {@code offset}/{@code index}, the UTF-8 byte offset and the code point offset.
     */
    private SourceLocation getCurrentLocation() {
        return SourceLocation.create(
                lineNumber, columnNumber, position, position, utf8Offset, codePointOffset);
    }

    /** Returns {@code true} if the lexer has consumed the entire source text. */
    private boolean isAtEnd() {
        return position >= length;
    }

    /**
     * Returns the Unicode code point at the current position without consuming it, or {@code -1} if
     * the end of the source has been reached.
     */
    private int peekCodePoint() {
        return isAtEnd() ? -1 : source.codePointAt(position);
    }

    /**
     * Consumes and returns the Unicode code point at the current position, correctly advancing past
     * surrogate pairs, and updates line/column tracking along with the UTF-8 byte offset and code
     * point count. Line terminators {@code \n}, {@code \r} and {@code \r\n} are all normalized to a
     * single line break.
     *
     * @return the consumed code point, or {@code -1} at end of source.
     */
    private int advance() {
        final int codePoint = peekCodePoint();
        if (codePoint != -1) {
            position += Character.charCount(codePoint);
            utf8Offset += utf8ByteLength(codePoint);
            codePointOffset++;
            switch (codePoint) {
                case Constants.LINE_FEED -> {
                    lineNumber++;
                    columnNumber = 1;
                }
                case Constants.CARRIAGE_RETURN -> {
                    if (peekCodePoint() != Constants.LINE_FEED) {
                        lineNumber++;
                        columnNumber = 1;
                    }
                }
                default -> columnNumber++;
            }
        }
        return codePoint;
    }

    /** Returns the number of source lines tracked by this lexer. */
    public int lineCount() {
        return lineTracker.lineCount();
    }
    // TODO: wire up the real lexer.
}
