package org.dersbian.compiler.lexer;

import java.util.Map;
import org.dersbian.compiler.Constants;
import org.dersbian.compiler.lexer.token.TokenKind;

/**
 * Static, stateless helpers for classifying individual Unicode code points and resolving whole
 * identifier lexemes. Nothing here touches lexer state; every method is a pure function of its
 * arguments, which makes each one testable without constructing a {@link Lexer}.
 */
public final class CodePoints {

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

    private CodePoints() {
        throw new AssertionError("no instances");
    }

    /**
     * Removes a leading UTF-8 byte-order mark ({@code U+FEFF}), if present. Some editors write it
     * at the start of UTF-8 encoded files; if left untouched it would surface as a spurious invalid
     * character at position zero.
     */
    public static String stripByteOrderMark(final String text) {
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
     * lets the cursor's UTF-8 offset track the real byte offset in the original source file, even
     * though the in-memory representation is a Java {@code String} (UTF-16).
     */
    public static int utf8ByteLength(final int codePoint) {
        if (!Character.isValidCodePoint(codePoint)) {
            throw new IllegalArgumentException("Invalid Unicode code point: " + codePoint);
        }
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

    /** Returns {@code true} se il code point rientra nell'intervallo ASCII (un solo byte UTF-8). */
    public static boolean isAscii(final int codePoint) {
        return Character.isValidCodePoint(codePoint) && codePoint < Constants.UTF8_ONE_BYTE_LIMIT;
    }

    /**
     * Returns {@code true} if the given code point is considered whitespace and should be skipped
     * between tokens. Combines {@link Character#isWhitespace(int)} with {@link
     * Character#isSpaceChar(int)} so that any whitespace-looking byte sequence from a UTF-8 source
     * file is skipped regardless of which category it falls under.
     */
    public static boolean isWhitespaceCodePoint(final int codePoint) {
        return codePoint != -1
                && (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint));
    }

    /**
     * Returns {@code true} se il code point può iniziare un identificatore: lettera Unicode
     * (secondo {@link Character#isUnicodeIdentifierStart(int)}) oppure underscore {@code '_'}.
     */
    public static boolean isIdentifierStart(final int codePoint) {
        return codePoint != -1
                && (codePoint == '_' || Character.isUnicodeIdentifierStart(codePoint));
    }

    /**
     * Returns {@code true} se il code point può proseguire un identificatore già iniziato: lettere,
     * cifre e altri caratteri Unicode "identifier part" (secondo {@link
     * Character#isUnicodeIdentifierPart(int)}), oltre all'underscore {@code '_'}.
     */
    public static boolean isIdentifierPart(final int codePoint) {
        return codePoint != -1
                && (codePoint == '_' || Character.isUnicodeIdentifierPart(codePoint));
    }

    /**
     * Determina il {@link TokenKind} corretto per un lessema già interamente scansionato: literal
     * booleano, keyword, type-keyword, oppure identificatore ASCII o Unicode.
     */
    public static TokenKind resolveIdentifierKind(final String value, final boolean asciiOnly) {
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
}
