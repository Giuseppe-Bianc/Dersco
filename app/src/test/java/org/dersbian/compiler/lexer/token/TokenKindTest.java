package org.dersbian.compiler.lexer.token;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.LooseCoupling",
    "PMD.UnitTestAssertionsShouldIncludeMessage",
    "PMD.UnitTestContainsTooManyAsserts"
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
    // TokenKind.Simple.Operator#toString() - the switch expression on lines 103-142
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
}
