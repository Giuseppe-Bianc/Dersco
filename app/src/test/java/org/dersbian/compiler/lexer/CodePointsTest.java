package org.dersbian.compiler.lexer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import org.dersbian.compiler.Constants;
import org.dersbian.compiler.lexer.token.TokenKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link CodePoints}.
 *
 * <p>Since every method in {@link CodePoints} is a pure, stateless function, each test is a
 * straightforward input/output assertion. Tests focus heavily on edge and corner cases: the empty
 * string, the sentinel {@code -1} end-of-input value, the exact UTF-8 encoding boundaries,
 * surrogate code points, {@code Character#MAX_CODE_POINT}, keyword vs. identifier resolution, and
 * ASCII vs. Unicode identifier classification.
 *
 * <p>These tests intentionally avoid {@code @Nested} classes and instead rely on
 * {@code @DisplayName}, parameterized tests, and grouped assertions ({@code assertAll}) to keep
 * related behavior legible while remaining flat.
 */
@SuppressWarnings({
    "PMD.UnitTestAssertionsShouldIncludeMessage",
    "PMD.TooManyMethods",
    "PMD.AtLeastOneConstructor",
    "PMD.ShortVariable",
    "PMD.UnitTestContainsTooManyAsserts",
})
class CodePointsTest {

    // ---------------------------------------------------------------------
    // Constructor / instantiability
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("is a final utility class")
    void classIsFinal() {
        assertTrue(Modifier.isFinal(CodePoints.class.getModifiers()));
    }

    @Test
    @DisplayName("private constructor throws AssertionError to forbid instantiation")
    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    void constructorForbidsInstantiation() throws NoSuchMethodException {
        final Constructor<CodePoints> constructor = CodePoints.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()), "constructor must be private");
        constructor.setAccessible(true);

        final InvocationTargetException wrapper =
                Assertions.assertThrows(InvocationTargetException.class, constructor::newInstance);
        Assertions.assertInstanceOf(AssertionError.class, wrapper.getCause());
    }

    // ---------------------------------------------------------------------
    // stripByteOrderMark
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("stripByteOrderMark removes a leading BOM")
    void stripByteOrderMarkRemovesLeadingBom() {
        final String withBom = new String(Character.toChars(Constants.BYTE_ORDER_MARK)) + "hello";
        assertEquals("hello", CodePoints.stripByteOrderMark(withBom));
    }

    @Test
    @DisplayName("stripByteOrderMark leaves text without a BOM unchanged")
    void stripByteOrderMarkKeepsTextWithoutBom() {
        final String text = "hello";
        Assertions.assertSame(
                text,
                CodePoints.stripByteOrderMark(text),
                "no BOM present, so the identical instance should be returned");
    }

    @Test
    @DisplayName("stripByteOrderMark on the empty string returns the empty string")
    void stripByteOrderMarkOnEmptyString() {
        assertEquals("", CodePoints.stripByteOrderMark(""));
    }

    @Test
    @DisplayName("stripByteOrderMark removes only a single leading BOM")
    void stripByteOrderMarkRemovesOnlyOneBom() {
        final String bom = new String(Character.toChars(Constants.BYTE_ORDER_MARK));
        final String withTwoBoms = bom + bom + "x";
        assertEquals(bom + "x", CodePoints.stripByteOrderMark(withTwoBoms));
    }

    @Test
    @DisplayName("stripByteOrderMark keeps a BOM that is not at position zero")
    void stripByteOrderMarkKeepsInteriorBom() {
        final String bom = new String(Character.toChars(Constants.BYTE_ORDER_MARK));
        final String text = "a" + bom + "b";
        assertEquals(text, CodePoints.stripByteOrderMark(text));
    }

    // ---------------------------------------------------------------------
    // utf8ByteLength
    // ---------------------------------------------------------------------

    @ParameterizedTest(name = "code point {0} -> {1} UTF-8 byte(s)")
    @DisplayName("utf8ByteLength returns the correct length for representative code points")
    @CsvSource({
        "0,   1",
        "65,  1",
        "127, 1",
        "128, 2",
        "233, 2",
        "2047, 2",
        "2048, 3",
        "8364, 3",
        "65535, 3",
        "65536, 4",
        "128512, 4"
    })
    void utf8ByteLengthKnownValues(final int codePoint, final int expectedLength) {
        assertEquals(expectedLength, CodePoints.utf8ByteLength(codePoint));
    }

    @Test
    @DisplayName("utf8ByteLength for MAX_CODE_POINT is four bytes")
    void utf8ByteLengthMaxCodePoint() {
        assertEquals(4, CodePoints.utf8ByteLength(Character.MAX_CODE_POINT));
    }

    @Test
    @DisplayName("utf8ByteLength agrees with the JDK's own UTF-8 encoder")
    void utf8ByteLengthMatchesJdkEncoder() {
        final int[] samples = {
            0, 1, 0x7F, 0x80, 0x7FF, 0x800, 0xFFFF, 0x10000, Character.MAX_CODE_POINT
        };
        assertAll(
                Stream.of(samples)
                        .flatMapToInt(java.util.stream.IntStream::of)
                        .mapToObj(
                                cp ->
                                        (org.junit.jupiter.api.function.Executable)
                                                () -> {
                                                    final int expected =
                                                            new String(Character.toChars(cp))
                                                                    .getBytes(
                                                                            StandardCharsets.UTF_8)
                                                                    .length;
                                                    assertEquals(
                                                            expected,
                                                            CodePoints.utf8ByteLength(cp),
                                                            () ->
                                                                    "mismatch for U+"
                                                                            + Integer.toHexString(
                                                                                    cp));
                                                }));
    }

    @ParameterizedTest(name = "invalid code point {0}")
    @DisplayName("utf8ByteLength rejects invalid code points")
    @ValueSource(
            ints = {-1, -100, Integer.MIN_VALUE, Character.MAX_CODE_POINT + 1, Integer.MAX_VALUE})
    void utf8ByteLengthRejectsInvalidCodePoints(final int invalidCodePoint) {
        Assertions.assertThrows(
                IllegalArgumentException.class, () -> CodePoints.utf8ByteLength(invalidCodePoint));
    }

    // ---------------------------------------------------------------------
    // isAscii
    // ---------------------------------------------------------------------

    @ParameterizedTest(name = "code point {0} is ASCII")
    @DisplayName("isAscii is true for the full ASCII range [0, 127]")
    @ValueSource(ints = {0, 1, 65, 97, 126, 127})
    void isAsciiTrueForAsciiRange(final int codePoint) {
        assertTrue(CodePoints.isAscii(codePoint));
    }

    @ParameterizedTest(name = "code point {0} is not ASCII")
    @DisplayName("isAscii is false for non-ASCII and invalid code points")
    @ValueSource(
            ints = {
                128,
                233,
                8364,
                128_512,
                Character.MAX_CODE_POINT,
                -1,
                Character.MAX_CODE_POINT + 1
            })
    void isAsciiFalseForNonAscii(final int codePoint) {
        assertFalse(CodePoints.isAscii(codePoint));
    }

    @Test
    @DisplayName("isAscii boundary: 127 is ASCII, 128 is not")
    void isAsciiBoundary() {
        assertAll(
                () ->
                        assertTrue(
                                CodePoints.isAscii(127), "127 (0x7F) is the last ASCII code point"),
                () ->
                        assertFalse(
                                CodePoints.isAscii(128),
                                "128 (0x80) is the first non-ASCII code point"));
    }

    // ---------------------------------------------------------------------
    // isWhitespaceCodePoint
    // ---------------------------------------------------------------------

    @ParameterizedTest(name = "code point {0} is whitespace")
    @DisplayName("isWhitespaceCodePoint is true for common and Unicode whitespace")
    @ValueSource(ints = {' ', '\t', '\n', '\r', '\f', 0x0B, 0x00A0, 0x2000, 0x3000})
    void isWhitespaceTrue(final int codePoint) {
        assertTrue(CodePoints.isWhitespaceCodePoint(codePoint));
    }

    @ParameterizedTest(name = "code point {0} is not whitespace")
    @DisplayName("isWhitespaceCodePoint is false for non-whitespace characters")
    @ValueSource(ints = {'a', 'Z', '0', '_', '$', 8364})
    void isWhitespaceFalse(final int codePoint) {
        assertFalse(CodePoints.isWhitespaceCodePoint(codePoint));
    }

    @Test
    @DisplayName("isWhitespaceCodePoint is false for the -1 end-of-input sentinel")
    void isWhitespaceFalseForSentinel() {
        assertFalse(CodePoints.isWhitespaceCodePoint(-1));
    }

    // ---------------------------------------------------------------------
    // isIdentifierStart
    // ---------------------------------------------------------------------

    @ParameterizedTest(name = "code point {0} can start an identifier")
    @DisplayName("isIdentifierStart is true for letters and underscore")
    @ValueSource(ints = {'_', 'a', 'z', 'A', 'Z', 0x00E9, 0x3042})
    void isIdentifierStartTrue(final int codePoint) {
        assertTrue(CodePoints.isIdentifierStart(codePoint));
    }

    @ParameterizedTest(name = "code point {0} cannot start an identifier")
    @DisplayName("isIdentifierStart is false for digits, symbols and whitespace")
    @ValueSource(ints = {'0', '9', '$', '-', ' ', '.', '@'})
    void isIdentifierStartFalse(final int codePoint) {
        assertFalse(CodePoints.isIdentifierStart(codePoint));
    }

    @Test
    @DisplayName("isIdentifierStart is false for the -1 end-of-input sentinel")
    void isIdentifierStartFalseForSentinel() {
        assertFalse(CodePoints.isIdentifierStart(-1));
    }

    @Test
    @DisplayName("isIdentifierStart: a leading digit is not allowed but underscore is")
    void isIdentifierStartUnderscoreVsDigit() {
        assertAll(
                () -> assertTrue(CodePoints.isIdentifierStart('_')),
                () -> assertFalse(CodePoints.isIdentifierStart('0')));
    }

    // ---------------------------------------------------------------------
    // isIdentifierPart
    // ---------------------------------------------------------------------

    @ParameterizedTest(name = "code point {0} can continue an identifier")
    @DisplayName("isIdentifierPart is true for letters, digits and underscore")
    @ValueSource(ints = {'_', 'a', 'Z', '0', '9', 0x00E9, 0x3042})
    void isIdentifierPartTrue(final int codePoint) {
        assertTrue(CodePoints.isIdentifierPart(codePoint));
    }

    @ParameterizedTest(name = "code point {0} cannot continue an identifier")
    @DisplayName("isIdentifierPart is false for symbols and whitespace")
    @ValueSource(ints = {'$', '-', ' ', '.', '@', '#'})
    void isIdentifierPartFalse(final int codePoint) {
        assertFalse(CodePoints.isIdentifierPart(codePoint));
    }

    @Test
    @DisplayName("isIdentifierPart is false for the -1 end-of-input sentinel")
    void isIdentifierPartFalseForSentinel() {
        assertFalse(CodePoints.isIdentifierPart(-1));
    }

    @Test
    @DisplayName("a digit cannot start but can continue an identifier")
    void digitStartVsPart() {
        assertAll(
                () ->
                        assertFalse(
                                CodePoints.isIdentifierStart('5'),
                                "digits cannot start identifiers"),
                () ->
                        assertTrue(
                                CodePoints.isIdentifierPart('5'),
                                "digits can continue identifiers"));
    }

    // ---------------------------------------------------------------------
    // resolveIdentifierKind - boolean literals
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("resolveIdentifierKind resolves the 'true' literal to KeywordBool(true)")
    void resolveTrueLiteral() {
        final TokenKind kind = CodePoints.resolveIdentifierKind(Constants.TRUE_LITERAL, true);
        final TokenKind.KeywordBool bool =
                Assertions.assertInstanceOf(TokenKind.KeywordBool.class, kind);
        assertTrue(bool.value(), "the resolved boolean literal must carry the value true");
    }

    @Test
    @DisplayName("resolveIdentifierKind resolves the 'false' literal to KeywordBool(false)")
    void resolveFalseLiteral() {
        final TokenKind kind = CodePoints.resolveIdentifierKind(Constants.FALSE_LITERAL, true);
        final TokenKind.KeywordBool bool =
                Assertions.assertInstanceOf(TokenKind.KeywordBool.class, kind);
        assertFalse(bool.value(), "the resolved boolean literal must carry the value false");
    }

    @Test
    @DisplayName("boolean literal resolution ignores the asciiOnly flag")
    void resolveBooleanLiteralIgnoresAsciiFlag() {
        Assertions.assertInstanceOf(
                TokenKind.KeywordBool.class,
                CodePoints.resolveIdentifierKind(Constants.TRUE_LITERAL, false));
    }

    // ---------------------------------------------------------------------
    // resolveIdentifierKind - keywords and type-keywords
    // ---------------------------------------------------------------------

    @ParameterizedTest(name = "keyword \"{0}\" -> {1}")
    @DisplayName("resolveIdentifierKind maps every keyword to its Simple.Keyword")
    @CsvSource({
        "fun,      FUN",
        "if,       IF",
        "else,     ELSE",
        "return,   RETURN",
        "while,    WHILE",
        "for,      FOR",
        "main,     MAIN",
        "var,      VAR",
        "const,    CONST",
        "nullptr,  NULLPTR",
        "break,    BREAK",
        "continue, CONTINUE"
    })
    void resolveKeywords(final String lexeme, final String expectedName) {
        final TokenKind kind = CodePoints.resolveIdentifierKind(lexeme, true);
        final TokenKind.Simple.Keyword keyword =
                Assertions.assertInstanceOf(TokenKind.Simple.Keyword.class, kind);
        assertEquals(expectedName, keyword.name());
    }

    @ParameterizedTest(name = "type-keyword \"{0}\" -> {1}")
    @DisplayName("resolveIdentifierKind maps every type-keyword to its Simple.TypeKeyword")
    @CsvSource({
        "i8,     I8",
        "i16,    I16",
        "i32,    I32",
        "i64,    I64",
        "u8,     U8",
        "u16,    U16",
        "u32,    U32",
        "u64,    U64",
        "f32,    F32",
        "f64,    F64",
        "char,   CHAR",
        "string, STRING",
        "bool,   BOOL"
    })
    void resolveTypeKeywords(final String lexeme, final String expectedName) {
        final TokenKind kind = CodePoints.resolveIdentifierKind(lexeme, true);
        final TokenKind.Simple.TypeKeyword typeKeyword =
                Assertions.assertInstanceOf(TokenKind.Simple.TypeKeyword.class, kind);
        assertEquals(expectedName, typeKeyword.name());
    }

    @Test
    @DisplayName("keyword resolution is case-sensitive: 'Fun' is a plain identifier")
    void keywordResolutionIsCaseSensitive() {
        final TokenKind kind = CodePoints.resolveIdentifierKind("Fun", true);
        Assertions.assertInstanceOf(
                TokenKind.IdentifierAscii.class,
                kind,
                "keywords are lowercase; 'Fun' must not be treated as a keyword");
    }

    @Test
    @DisplayName("a keyword prefix such as 'ifx' is an identifier, not the keyword 'if'")
    void keywordPrefixIsIdentifier() {
        final TokenKind kind = CodePoints.resolveIdentifierKind("ifx", true);
        final TokenKind.IdentifierAscii id =
                Assertions.assertInstanceOf(TokenKind.IdentifierAscii.class, kind);
        assertEquals("ifx", id.value());
    }

    // ---------------------------------------------------------------------
    // resolveIdentifierKind - identifiers (ASCII vs Unicode)
    // ---------------------------------------------------------------------

    @Test
    @DisplayName("an ASCII identifier with asciiOnly=true resolves to IdentifierAscii")
    void resolveAsciiIdentifier() {
        final TokenKind kind = CodePoints.resolveIdentifierKind("myVar", true);
        final TokenKind.IdentifierAscii id =
                Assertions.assertInstanceOf(TokenKind.IdentifierAscii.class, kind);
        assertEquals("myVar", id.value());
    }

    @Test
    @DisplayName("an identifier with asciiOnly=false resolves to IdentifierUnicode")
    void resolveUnicodeIdentifier() {
        final TokenKind kind = CodePoints.resolveIdentifierKind("café", false);
        final TokenKind.IdentifierUnicode id =
                Assertions.assertInstanceOf(TokenKind.IdentifierUnicode.class, kind);
        assertEquals("café", id.value());
    }

    @Test
    @DisplayName("the asciiOnly flag alone decides between ASCII and Unicode identifier kinds")
    void asciiFlagDrivesIdentifierKind() {
        assertAll(
                () ->
                        Assertions.assertInstanceOf(
                                TokenKind.IdentifierAscii.class,
                                CodePoints.resolveIdentifierKind("value", true)),
                () ->
                        Assertions.assertInstanceOf(
                                TokenKind.IdentifierUnicode.class,
                                CodePoints.resolveIdentifierKind("value", false)));
    }

    @ParameterizedTest(name = "single-underscore-ish identifier \"{0}\"")
    @DisplayName("underscore-only and underscore-prefixed lexemes are valid identifiers")
    @ValueSource(strings = {"_", "__", "_x", "_0"})
    void underscoreIdentifiers(final String lexeme) {
        Assertions.assertInstanceOf(
                TokenKind.IdentifierAscii.class, CodePoints.resolveIdentifierKind(lexeme, true));
    }

    @ParameterizedTest
    @DisplayName("resolveIdentifierKind treats an empty lexeme as an identifier")
    @EmptySource
    void resolveEmptyLexeme(final String lexeme) {
        Assertions.assertInstanceOf(
                TokenKind.IdentifierAscii.class, CodePoints.resolveIdentifierKind(lexeme, true));
    }

    @MethodSource("nonKeywordIdentifiers")
    @ParameterizedTest(name = "\"{0}\" resolves to an identifier")
    @DisplayName("look-alike lexemes that are not exact keywords resolve to identifiers")
    void lookAlikesAreIdentifiers(final String lexeme) {
        final TokenKind kind = CodePoints.resolveIdentifierKind(lexeme, true);
        Assertions.assertInstanceOf(TokenKind.IdentifierAscii.class, kind);
    }

    private static Stream<Arguments> nonKeywordIdentifiers() {
        return Stream.of(
                Arguments.of("funn"),
                Arguments.of("fu"),
                Arguments.of("IF"),
                Arguments.of("i128"),
                Arguments.of("truely"),
                Arguments.of("bool_"));
    }
}
