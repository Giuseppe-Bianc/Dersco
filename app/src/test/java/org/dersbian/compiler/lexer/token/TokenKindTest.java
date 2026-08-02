package org.dersbian.compiler.lexer.token;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.dersbian.compiler.lexer.token.number.INumber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.LooseCoupling",
    "PMD.UnitTestAssertionsShouldIncludeMessage",
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.GodClass",
    "PMD.TooManyMethods"
})
class TokenKindTest {

    @Test
    void simpleTypeKeywordsReportAsTypes() {
        final EnumSet<TokenKind.Simple.TypeKeyword> expected =
                EnumSet.of(
                        TokenKind.Simple.TypeKeyword.I8,
                        TokenKind.Simple.TypeKeyword.I16,
                        TokenKind.Simple.TypeKeyword.I32,
                        TokenKind.Simple.TypeKeyword.I64,
                        TokenKind.Simple.TypeKeyword.U8,
                        TokenKind.Simple.TypeKeyword.U16,
                        TokenKind.Simple.TypeKeyword.U32,
                        TokenKind.Simple.TypeKeyword.U64,
                        TokenKind.Simple.TypeKeyword.F32,
                        TokenKind.Simple.TypeKeyword.F64,
                        TokenKind.Simple.TypeKeyword.CHAR,
                        TokenKind.Simple.TypeKeyword.STRING,
                        TokenKind.Simple.TypeKeyword.BOOL);

        final EnumSet<TokenKind.Simple.TypeKeyword> actual =
                Arrays.stream(TokenKind.Simple.TypeKeyword.values())
                        .filter(TokenKind.Simple::isType)
                        .collect(
                                Collectors.toCollection(
                                        () -> EnumSet.noneOf(TokenKind.Simple.TypeKeyword.class)));

        Assertions.assertEquals(expected, actual);
    }

    @Test
    void payloadTokenKindsAreNotTypeKeywords() {
        Assertions.assertFalse(new TokenKind.IdentifierAscii("i32").isType());
    }

    @Test
    void stringRepresentationsExposeDisplayText() {
        Assertions.assertEquals("Keyword 'fun'", TokenKind.Simple.Keyword.FUN.toString());
        Assertions.assertEquals(
                "Identifier 'value'", new TokenKind.IdentifierAscii("value").toString());
        Assertions.assertEquals(
                "String literal \"hello\"", new TokenKind.StringLiteral("hello").toString());
    }

    // ------------------------------------------------------------------
    // TokenKind.Simple.Operator#toString() - lines 103-142
    // ------------------------------------------------------------------

    private static Stream<Arguments> operatorAndExpectedText() {
        return Stream.of(
                Arguments.of(TokenKind.Simple.Operator.PLUS, "Operator '+'"),
                Arguments.of(TokenKind.Simple.Operator.MINUS, "Operator '-'"),
                Arguments.of(TokenKind.Simple.Operator.STAR, "Operator '*'"),
                Arguments.of(TokenKind.Simple.Operator.SLASH, "Operator '/'"),
                Arguments.of(TokenKind.Simple.Operator.PLUS_EQUAL, "Operator '+='"),
                Arguments.of(TokenKind.Simple.Operator.MINUS_EQUAL, "Operator '-='"),
                Arguments.of(TokenKind.Simple.Operator.EQUAL_EQUAL, "Operator '=='"),
                Arguments.of(TokenKind.Simple.Operator.NOT_EQUAL, "Operator '!='"),
                Arguments.of(TokenKind.Simple.Operator.STAR_EQUAL, "Operator '*='"),
                Arguments.of(TokenKind.Simple.Operator.SLASH_EQUAL, "Operator '/='"),
                Arguments.of(TokenKind.Simple.Operator.LESS, "Operator '<'"),
                Arguments.of(TokenKind.Simple.Operator.GREATER, "Operator '>'"),
                Arguments.of(TokenKind.Simple.Operator.SHIFT_LEFT_EQUAL, "Operator '<<='"),
                Arguments.of(TokenKind.Simple.Operator.SHIFT_RIGHT_EQUAL, "Operator '>>='"),
                Arguments.of(TokenKind.Simple.Operator.LESS_EQUAL, "Operator '<='"),
                Arguments.of(TokenKind.Simple.Operator.GREATER_EQUAL, "Operator '>='"),
                Arguments.of(TokenKind.Simple.Operator.PLUS_PLUS, "Operator '++'"),
                Arguments.of(TokenKind.Simple.Operator.MINUS_MINUS, "Operator '--'"),
                Arguments.of(TokenKind.Simple.Operator.OR_OR, "Operator '||'"),
                Arguments.of(TokenKind.Simple.Operator.AND_AND, "Operator '&&'"),
                Arguments.of(TokenKind.Simple.Operator.AND_EQUAL, "Operator '&='"),
                Arguments.of(TokenKind.Simple.Operator.OR_EQUAL, "Operator '|='"),
                Arguments.of(TokenKind.Simple.Operator.SHIFT_LEFT, "Operator '<<'"),
                Arguments.of(TokenKind.Simple.Operator.SHIFT_RIGHT, "Operator '>>'"),
                Arguments.of(TokenKind.Simple.Operator.PERCENT_EQUAL, "Operator '%='"),
                Arguments.of(TokenKind.Simple.Operator.XOR_EQUAL, "Operator '^='"),
                Arguments.of(TokenKind.Simple.Operator.BITWISE_NOT, "Operator '~'"),
                Arguments.of(TokenKind.Simple.Operator.NOT, "Operator '!'"),
                Arguments.of(TokenKind.Simple.Operator.XOR, "Operator '^'"),
                Arguments.of(TokenKind.Simple.Operator.PERCENT, "Operator '%'"),
                Arguments.of(TokenKind.Simple.Operator.OR, "Operator '|'"),
                Arguments.of(TokenKind.Simple.Operator.AND, "Operator '&'"),
                Arguments.of(TokenKind.Simple.Operator.EQUAL, "Operator '='"),
                Arguments.of(TokenKind.Simple.Operator.COLON, "Operator ':'"),
                Arguments.of(TokenKind.Simple.Operator.COMMA, "Operator ','"),
                Arguments.of(TokenKind.Simple.Operator.DOT, "Operator '.'"));
    }

    @ParameterizedTest
    @MethodSource("operatorAndExpectedText")
    @DisplayName("every operator maps to its exact documented symbol")
    void everyOperatorFormatsToItsExactSymbol(
            final TokenKind.Simple.Operator operator, final String expected) {
        Assertions.assertEquals(expected, operator.toString());
    }

    @Test
    @DisplayName("the dataset above exercises every declared constant, none are skipped")
    void datasetCoversEveryDeclaredConstant() {
        Assertions.assertEquals(
                (long) TokenKind.Simple.Operator.values().length,
                operatorAndExpectedText().count());
    }

    @ParameterizedTest
    @EnumSource(TokenKind.Simple.Operator.class)
    @DisplayName("output always has the shape: Operator '<symbol>'")
    void outputAlwaysFollowsThePrefixAndQuoteShape(final TokenKind.Simple.Operator operator) {
        final String text = operator.toString();
        Assertions.assertNotNull(text);
        Assertions.assertTrue(text.startsWith("Operator '"));
        Assertions.assertTrue(text.endsWith("'"));

        final String symbol = text.substring("Operator '".length(), text.length() - 1);
        Assertions.assertFalse(symbol.isEmpty());
    }

    @ParameterizedTest
    @EnumSource(TokenKind.Simple.Operator.class)
    @DisplayName("toString() is deterministic across repeated calls")
    void stringRerprIsDeterministic(final TokenKind.Simple.Operator operator) {
        Assertions.assertEquals(operator.toString(), operator.toString());
    }

    @ParameterizedTest
    @EnumSource(TokenKind.Simple.Operator.class)
    @DisplayName("the override actually replaces Enum#name(), it isn't a pass-through")
    void stringRerprDivergesFromRawEnumName(final TokenKind.Simple.Operator operator) {
        Assertions.assertNotEquals(operator.name(), operator.toString());
    }

    @Test
    @DisplayName("no two operators collapse onto the same display text")
    void everyDisplayTextIsUnique() {
        final Set<String> renderedForms = new HashSet<>();
        Arrays.stream(TokenKind.Simple.Operator.values())
                .map(TokenKind.Simple.Operator::toString)
                .forEach(renderedForms::add);

        Assertions.assertEquals(TokenKind.Simple.Operator.values().length, renderedForms.size());
    }

    // ------------------------------------------------------------------
    // TokenKind.Simple.Keyword#toString() - lines 164-179
    // ------------------------------------------------------------------

    private static Stream<Arguments> keywordAndExpectedText() {
        return Stream.of(
                Arguments.of(TokenKind.Simple.Keyword.FUN, "Keyword 'fun'"),
                Arguments.of(TokenKind.Simple.Keyword.IF, "Keyword 'if'"),
                Arguments.of(TokenKind.Simple.Keyword.ELSE, "Keyword 'else'"),
                Arguments.of(TokenKind.Simple.Keyword.RETURN, "Keyword 'return'"),
                Arguments.of(TokenKind.Simple.Keyword.WHILE, "Keyword 'while'"),
                Arguments.of(TokenKind.Simple.Keyword.FOR, "Keyword 'for'"),
                Arguments.of(TokenKind.Simple.Keyword.MAIN, "Keyword 'main'"),
                Arguments.of(TokenKind.Simple.Keyword.VAR, "Keyword 'var'"),
                Arguments.of(TokenKind.Simple.Keyword.CONST, "Keyword 'const'"),
                Arguments.of(TokenKind.Simple.Keyword.NULLPTR, "Keyword 'nullptr'"),
                Arguments.of(TokenKind.Simple.Keyword.BREAK, "Keyword 'break'"),
                Arguments.of(TokenKind.Simple.Keyword.CONTINUE, "Keyword 'continue'"));
    }

    @ParameterizedTest
    @MethodSource("keywordAndExpectedText")
    @DisplayName("every keyword maps to its exact documented text")
    void everyKeywordFormatsToItsExactText(
            final TokenKind.Simple.Keyword keyword, final String expected) {
        Assertions.assertEquals(expected, keyword.toString());
    }

    @Test
    @DisplayName("keyword dataset exercises every declared constant")
    void keywordDatasetCoversEveryDeclaredConstant() {
        Assertions.assertEquals(
                (long) TokenKind.Simple.Keyword.values().length, keywordAndExpectedText().count());
    }

    @ParameterizedTest
    @EnumSource(TokenKind.Simple.Keyword.class)
    @DisplayName("keyword output always has the shape: Keyword '<text>'")
    void keywordOutputAlwaysFollowsThePrefixAndQuoteShape(final TokenKind.Simple.Keyword keyword) {
        final String text = keyword.toString();
        Assertions.assertTrue(text.startsWith("Keyword '"));
        Assertions.assertTrue(text.endsWith("'"));
        Assertions.assertFalse(text.substring("Keyword '".length(), text.length() - 1).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(TokenKind.Simple.Keyword.class)
    @DisplayName("keyword toString diverges from the raw enum name")
    void keywordStringDivergesFromRawEnumName(final TokenKind.Simple.Keyword keyword) {
        Assertions.assertNotEquals(keyword.name(), keyword.toString());
    }

    @Test
    @DisplayName("no two keywords collapse onto the same display text")
    void everyKeywordDisplayTextIsUnique() {
        final Set<String> forms =
                Arrays.stream(TokenKind.Simple.Keyword.values())
                        .map(TokenKind.Simple.Keyword::toString)
                        .collect(Collectors.toSet());
        Assertions.assertEquals(TokenKind.Simple.Keyword.values().length, forms.size());
    }

    @Test
    @DisplayName("keyword tokens are never reported as type keywords")
    void keywordsAreNeverTypeKeywords() {
        for (final TokenKind.Simple.Keyword keyword : TokenKind.Simple.Keyword.values()) {
            Assertions.assertFalse(keyword.isType());
        }
    }

    // ------------------------------------------------------------------
    // TokenKind.Simple.TypeKeyword#toString() - lines 202-218
    // ------------------------------------------------------------------

    private static Stream<Arguments> typeKeywordAndExpectedText() {
        return Stream.of(
                Arguments.of(TokenKind.Simple.TypeKeyword.I8, "Type 'i8'"),
                Arguments.of(TokenKind.Simple.TypeKeyword.I16, "Type 'i16'"),
                Arguments.of(TokenKind.Simple.TypeKeyword.I32, "Type 'i32'"),
                Arguments.of(TokenKind.Simple.TypeKeyword.I64, "Type 'i64'"),
                Arguments.of(TokenKind.Simple.TypeKeyword.U8, "Type 'u8'"),
                Arguments.of(TokenKind.Simple.TypeKeyword.U16, "Type 'u16'"),
                Arguments.of(TokenKind.Simple.TypeKeyword.U32, "Type 'u32'"),
                Arguments.of(TokenKind.Simple.TypeKeyword.U64, "Type 'u64'"),
                Arguments.of(TokenKind.Simple.TypeKeyword.F32, "Type 'f32'"),
                Arguments.of(TokenKind.Simple.TypeKeyword.F64, "Type 'f64'"),
                Arguments.of(TokenKind.Simple.TypeKeyword.CHAR, "Type 'char'"),
                Arguments.of(TokenKind.Simple.TypeKeyword.STRING, "Type 'string'"),
                Arguments.of(TokenKind.Simple.TypeKeyword.BOOL, "Type 'bool'"));
    }

    @ParameterizedTest
    @MethodSource("typeKeywordAndExpectedText")
    @DisplayName("every type keyword maps to its exact documented text")
    void everyTypeKeywordFormatsToItsExactText(
            final TokenKind.Simple.TypeKeyword typeKeyword, final String expected) {
        Assertions.assertEquals(expected, typeKeyword.toString());
    }

    @Test
    @DisplayName("type keyword dataset exercises every declared constant")
    void typeKeywordDatasetCoversEveryDeclaredConstant() {
        Assertions.assertEquals(
                (long) TokenKind.Simple.TypeKeyword.values().length,
                typeKeywordAndExpectedText().count());
    }

    @ParameterizedTest
    @EnumSource(TokenKind.Simple.TypeKeyword.class)
    @DisplayName("type keyword output always has the shape: Type '<text>'")
    void typeKeywordOutputAlwaysFollowsThePrefixAndQuoteShape(
            final TokenKind.Simple.TypeKeyword typeKeyword) {
        final String text = typeKeyword.toString();
        Assertions.assertTrue(text.startsWith("Type '"));
        Assertions.assertTrue(text.endsWith("'"));
        Assertions.assertFalse(text.substring("Type '".length(), text.length() - 1).isEmpty());
    }

    @ParameterizedTest
    @EnumSource(TokenKind.Simple.TypeKeyword.class)
    @DisplayName("every type keyword still reports isType() == true")
    void everyTypeKeywordIsRecognizedAsType(final TokenKind.Simple.TypeKeyword typeKeyword) {
        Assertions.assertTrue(typeKeyword.isType());
    }

    @Test
    @DisplayName("no two type keywords collapse onto the same display text")
    void everyTypeKeywordDisplayTextIsUnique() {
        final Set<String> forms =
                Arrays.stream(TokenKind.Simple.TypeKeyword.values())
                        .map(TokenKind.Simple.TypeKeyword::toString)
                        .collect(Collectors.toSet());
        Assertions.assertEquals(TokenKind.Simple.TypeKeyword.values().length, forms.size());
    }

    // ------------------------------------------------------------------
    // TokenKind.Simple.Delimiter#toString() - lines 233-242
    // ------------------------------------------------------------------

    private static Stream<Arguments> delimiterAndExpectedText() {
        return Stream.of(
                Arguments.of(TokenKind.Simple.Delimiter.OPEN_PAREN, "Delimiter '('"),
                Arguments.of(TokenKind.Simple.Delimiter.CLOSE_PAREN, "Delimiter ')'"),
                Arguments.of(TokenKind.Simple.Delimiter.OPEN_BRACKET, "Delimiter '['"),
                Arguments.of(TokenKind.Simple.Delimiter.CLOSE_BRACKET, "Delimiter ']'"),
                Arguments.of(TokenKind.Simple.Delimiter.OPEN_BRACE, "Delimiter '{'"),
                Arguments.of(TokenKind.Simple.Delimiter.CLOSE_BRACE, "Delimiter '}'"));
    }

    @ParameterizedTest
    @MethodSource("delimiterAndExpectedText")
    @DisplayName("every delimiter maps to its exact documented symbol")
    void everyDelimiterFormatsToItsExactSymbol(
            final TokenKind.Simple.Delimiter delimiter, final String expected) {
        Assertions.assertEquals(expected, delimiter.toString());
    }

    @Test
    @DisplayName("delimiter dataset exercises every declared constant")
    void delimiterDatasetCoversEveryDeclaredConstant() {
        Assertions.assertEquals(
                (long) TokenKind.Simple.Delimiter.values().length,
                delimiterAndExpectedText().count());
    }

    @ParameterizedTest
    @EnumSource(TokenKind.Simple.Delimiter.class)
    @DisplayName("delimiter output always has the shape: Delimiter '<symbol>'")
    void delimiterOutputAlwaysFollowsThePrefixAndQuoteShape(
            final TokenKind.Simple.Delimiter delimiter) {
        final String text = delimiter.toString();
        Assertions.assertTrue(text.startsWith("Delimiter '"));
        Assertions.assertTrue(text.endsWith("'"));
        Assertions.assertFalse(text.substring("Delimiter '".length(), text.length() - 1).isEmpty());
    }

    @Test
    @DisplayName("no two delimiters collapse onto the same display text")
    void everyDelimiterDisplayTextIsUnique() {
        final Set<String> forms =
                Arrays.stream(TokenKind.Simple.Delimiter.values())
                        .map(TokenKind.Simple.Delimiter::toString)
                        .collect(Collectors.toSet());
        Assertions.assertEquals(TokenKind.Simple.Delimiter.values().length, forms.size());
    }

    // ------------------------------------------------------------------
    // TokenKind.Simple.Special#toString() - lines 256-261
    // Note: unlike the other Simple enums, Special does NOT prefix its
    // output with the enum family name; it returns the raw display text.
    // ------------------------------------------------------------------

    private static Stream<Arguments> specialAndExpectedText() {
        return Stream.of(
                Arguments.of(TokenKind.Simple.Special.SEMICOLON, "';'"),
                Arguments.of(TokenKind.Simple.Special.COMMENT, "Comment"),
                Arguments.of(TokenKind.Simple.Special.MULTILINE_COMMENT, "Multiline Comment"),
                Arguments.of(TokenKind.Simple.Special.EOF, "EOF"));
    }

    @ParameterizedTest
    @MethodSource("specialAndExpectedText")
    @DisplayName("every special token maps to its exact documented text")
    void everySpecialFormatsToItsExactText(
            final TokenKind.Simple.Special special, final String expected) {
        Assertions.assertEquals(expected, special.toString());
    }

    @Test
    @DisplayName("special token dataset exercises every declared constant")
    void specialDatasetCoversEveryDeclaredConstant() {
        Assertions.assertEquals(
                (long) TokenKind.Simple.Special.values().length, specialAndExpectedText().count());
    }

    @Test
    @DisplayName("no two special tokens collapse onto the same display text")
    void everySpecialDisplayTextIsUnique() {
        final Set<String> forms =
                Arrays.stream(TokenKind.Simple.Special.values())
                        .map(TokenKind.Simple.Special::toString)
                        .collect(Collectors.toSet());
        Assertions.assertEquals(TokenKind.Simple.Special.values().length, forms.size());
    }

    @Test
    @DisplayName("EOF is a documented marker and is never a type keyword")
    void eofSpecialNeverReportsAsType() {
        Assertions.assertFalse(TokenKind.Simple.Special.EOF.isType());
    }

    // ------------------------------------------------------------------
    // TokenKind.KeywordBool#toString() - line 275
    // ------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    @DisplayName("KeywordBool toString reflects its boolean payload verbatim")
    void keywordBoolToStringReflectsPayload(final boolean value) {
        final TokenKind.KeywordBool bool = new TokenKind.KeywordBool(value);
        Assertions.assertEquals("boolean '" + value + "'", bool.toString());
    }

    @Test
    @DisplayName("KeywordBool with the same payload yields the same display text")
    void keywordBoolDisplayTextIsConsistentForEqualPayloads() {
        Assertions.assertEquals(
                new TokenKind.KeywordBool(true).toString(),
                new TokenKind.KeywordBool(true).toString());
    }

    @Test
    @DisplayName("KeywordBool true/false render to distinct display text")
    void keywordBoolTrueAndFalseRenderDifferently() {
        Assertions.assertNotEquals(
                new TokenKind.KeywordBool(true).toString(),
                new TokenKind.KeywordBool(false).toString());
    }

    // ------------------------------------------------------------------
    // TokenKind.IdentifierAscii#toString() - line 293
    // ------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"x", "value", "_underscore", "camelCase123", ""})
    @DisplayName(
            "IdentifierAscii toString wraps the raw value, including the empty-string edge case")
    void identifierAsciiToStringWrapsValue(final String value) {
        Assertions.assertEquals(
                "Identifier '" + value + "'", new TokenKind.IdentifierAscii(value).toString());
    }

    @Test
    @DisplayName("IdentifierAscii toString handles a null payload gracefully (corner case)")
    void identifierAsciiToStringHandlesNullPayload() {
        final TokenKind.IdentifierAscii identifier = new TokenKind.IdentifierAscii(null);
        Assertions.assertEquals("Identifier 'null'", identifier.toString());
    }

    // ------------------------------------------------------------------
    // TokenKind.IdentifierUnicode#toString() - line 311
    // ------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"héllo", "日本語", "Ñandú", "café_42", "Ω_variable"})
    @DisplayName("IdentifierUnicode toString wraps international payloads verbatim")
    void identifierUnicodeToStringWrapsValue(final String value) {
        Assertions.assertEquals(
                "Identifier '" + value + "'", new TokenKind.IdentifierUnicode(value).toString());
    }

    @Test
    @DisplayName("IdentifierUnicode toString handles a null payload gracefully (corner case)")
    void identifierUnicodeToStringHandlesNullPayload() {
        final TokenKind.IdentifierUnicode identifier = new TokenKind.IdentifierUnicode(null);
        Assertions.assertEquals("Identifier 'null'", identifier.toString());
    }

    @Test
    @DisplayName(
            "Ascii and Unicode identifiers sharing the same payload render the same display prefix"
                    + " but remain distinct token kinds")
    void identifierAsciiAndUnicodeShareDisplayShapeButDifferAsRecords() {
        final TokenKind ascii = new TokenKind.IdentifierAscii("foo");
        final TokenKind unicode = new TokenKind.IdentifierUnicode("foo");
        Assertions.assertEquals(ascii.toString(), unicode.toString());
        Assertions.assertNotEquals(ascii, unicode);
    }

    // ------------------------------------------------------------------
    // TokenKind.Numeric#toString() - line 320
    // ------------------------------------------------------------------

    private static Stream<Arguments> numericValuesAndExpectedText() {
        return Stream.of(
                Arguments.of(new INumber.Integer(42L), "Number '42'"),
                Arguments.of(new INumber.Integer(-1L), "Number '-1'"),
                Arguments.of(new INumber.I8((byte) -5), "Number '-5i8'"),
                Arguments.of(new INumber.Float64(1.5), "Number '1.5'"));
    }

    @ParameterizedTest
    @MethodSource("numericValuesAndExpectedText")
    @DisplayName("Numeric toString delegates to the underlying INumber payload")
    void numericToStringDelegatesToPayload(final INumber value, final String expected) {
        Assertions.assertEquals(expected, new TokenKind.Numeric(value).toString());
    }

    @Test
    @DisplayName("Numeric toString handles a null payload gracefully (corner case)")
    void numericToStringHandlesNullPayload() {
        final TokenKind.Numeric numeric = new TokenKind.Numeric(null);
        Assertions.assertEquals("Number 'null'", numeric.toString());
    }

    // ------------------------------------------------------------------
    // TokenKind.Binary#toString() - line 329
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Binary toString delegates to the underlying INumber payload")
    void binaryToStringDelegatesToPayload() {
        final TokenKind.Binary binary = new TokenKind.Binary(new INumber.UnsignedInteger(10L));
        Assertions.assertEquals("Binary '10'", binary.toString());
    }

    @Test
    @DisplayName("Binary toString handles a null payload gracefully (corner case)")
    void binaryToStringHandlesNullPayload() {
        Assertions.assertEquals("Binary 'null'", new TokenKind.Binary(null).toString());
    }

    // ------------------------------------------------------------------
    // TokenKind.Octal / Hexadecimal / StringLiteral / CharLiteral
    // toString() - lines 343-349
    // ------------------------------------------------------------------

    @Test
    @DisplayName("Octal toString delegates to the underlying INumber payload")
    void octalToStringDelegatesToPayload() {
        final TokenKind.Octal octal = new TokenKind.Octal(new INumber.Integer(493L));
        Assertions.assertEquals("Octal '493'", octal.toString());
    }

    @Test
    @DisplayName("Octal toString handles a null payload gracefully (corner case)")
    void octalToStringHandlesNullPayload() {
        Assertions.assertEquals("Octal 'null'", new TokenKind.Octal(null).toString());
    }

    @Test
    @DisplayName("Hexadecimal toString delegates to the underlying INumber payload")
    void hexadecimalToStringDelegatesToPayload() {
        final TokenKind.Hexadecimal hexadecimal =
                new TokenKind.Hexadecimal(new INumber.U32(3_735_928_559L));
        Assertions.assertEquals("Hexadecimal '3735928559u32'", hexadecimal.toString());
    }

    @Test
    @DisplayName("Hexadecimal toString handles a null payload gracefully (corner case)")
    void hexadecimalToStringHandlesNullPayload() {
        Assertions.assertEquals("Hexadecimal 'null'", new TokenKind.Hexadecimal(null).toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"hello", "", "with \"quotes\"", "line\nbreak"})
    @DisplayName("StringLiteral toString wraps the raw payload in double quotes")
    void stringLiteralToStringWrapsPayloadInDoubleQuotes(final String value) {
        Assertions.assertEquals(
                "String literal \"" + value + "\"", new TokenKind.StringLiteral(value).toString());
    }

    @Test
    @DisplayName("StringLiteral toString handles a null payload gracefully (corner case)")
    void stringLiteralToStringHandlesNullPayload() {
        Assertions.assertEquals(
                "String literal \"null\"", new TokenKind.StringLiteral(null).toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"a", "\\n", "'", ""})
    @DisplayName("CharLiteral toString wraps the raw payload in single quotes")
    void charLiteralToStringWrapsPayloadInSingleQuotes(final String value) {
        Assertions.assertEquals(
                "Character literal '" + value + "'", new TokenKind.CharLiteral(value).toString());
    }

    @Test
    @DisplayName("CharLiteral toString handles a null payload gracefully (corner case)")
    void charLiteralToStringHandlesNullPayload() {
        Assertions.assertEquals(
                "Character literal 'null'", new TokenKind.CharLiteral(null).toString());
    }
}
