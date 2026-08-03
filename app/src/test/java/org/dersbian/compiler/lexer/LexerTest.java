package org.dersbian.compiler.lexer;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.dersbian.compiler.error.ErrorCode;
import org.dersbian.compiler.lexer.token.Token;
import org.dersbian.compiler.lexer.token.TokenKind;
import org.dersbian.compiler.lexer.token.TokenKind.Simple;
import org.dersbian.compiler.lexer.token.number.INumber;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.TooManyMethods",
    "PMD.UnitTestAssertionsShouldIncludeMessage",
    "PMD.UnitTestContainsTooManyAsserts"
})
class LexerTest {
    private static final Path SOURCE_PATH = Path.of("test.dr");

    @Test
    void emptySourceProducesOnlyEof() {
        final LexerResult result = tokenize("");

        assertAll(
                () -> assertEquals(List.of(Simple.Special.EOF), kinds(result)),
                () -> assertTrue(result.errors().isEmpty()),
                () -> assertEquals(0, new Lexer(SOURCE_PATH, "").lineCount()));
    }

    @Test
    void byteOrderMarkAtTheBeginningIsIgnored() {
        final LexerResult result = tokenize("\uFEFFalpha");

        assertEquals(
                List.of(new TokenKind.IdentifierAscii("alpha"), Simple.Special.EOF), kinds(result));
        assertTrue(result.errors().isEmpty());
        assertEquals(0, result.tokens().getFirst().span().start().offset());
    }

    @Test
    void whitespaceIncludingUnicodeSpaceSeparatesTokensAndDoesNotProduceTokens() {
        final LexerResult result = tokenize("\talpha\u00A0\u2003\nbeta\r\ngamma");

        assertEquals(
                List.of(
                        new TokenKind.IdentifierAscii("alpha"),
                        new TokenKind.IdentifierAscii("beta"),
                        new TokenKind.IdentifierAscii("gamma"),
                        Simple.Special.EOF),
                kinds(result));
        assertTrue(result.errors().isEmpty());
        assertEquals(3, result.tokens().get(2).span().start().line());
    }

    @ParameterizedTest(name = "{0} lexes as {1}")
    @MethodSource("operatorCases")
    void operatorsAreLexedWithoutPrefixAmbiguity(final String source, final Simple expected) {
        final LexerResult result = tokenize(source);

        assertEquals(List.of(expected, Simple.Special.EOF), kinds(result));
        assertTrue(result.errors().isEmpty(), () -> "Unexpected errors for " + source);
    }

    private static Stream<Arguments> operatorCases() {
        return Stream.of(
                Arguments.of("+", Simple.Operator.PLUS),
                Arguments.of("+=", Simple.Operator.PLUS_EQUAL),
                Arguments.of("++", Simple.Operator.PLUS_PLUS),
                Arguments.of("-", Simple.Operator.MINUS),
                Arguments.of("-=", Simple.Operator.MINUS_EQUAL),
                Arguments.of("--", Simple.Operator.MINUS_MINUS),
                Arguments.of("*", Simple.Operator.STAR),
                Arguments.of("*=", Simple.Operator.STAR_EQUAL),
                Arguments.of("/", Simple.Operator.SLASH),
                Arguments.of("/=", Simple.Operator.SLASH_EQUAL),
                Arguments.of("=", Simple.Operator.EQUAL),
                Arguments.of("==", Simple.Operator.EQUAL_EQUAL),
                Arguments.of("!", Simple.Operator.NOT),
                Arguments.of("!=", Simple.Operator.NOT_EQUAL),
                Arguments.of("<", Simple.Operator.LESS),
                Arguments.of("<=", Simple.Operator.LESS_EQUAL),
                Arguments.of("<<", Simple.Operator.SHIFT_LEFT),
                Arguments.of("<<=", Simple.Operator.SHIFT_LEFT_EQUAL),
                Arguments.of(">", Simple.Operator.GREATER),
                Arguments.of(">=", Simple.Operator.GREATER_EQUAL),
                Arguments.of(">>", Simple.Operator.SHIFT_RIGHT),
                Arguments.of(">>=", Simple.Operator.SHIFT_RIGHT_EQUAL),
                Arguments.of("|", Simple.Operator.OR),
                Arguments.of("|=", Simple.Operator.OR_EQUAL),
                Arguments.of("||", Simple.Operator.OR_OR),
                Arguments.of("&", Simple.Operator.AND),
                Arguments.of("&=", Simple.Operator.AND_EQUAL),
                Arguments.of("&&", Simple.Operator.AND_AND),
                Arguments.of("%", Simple.Operator.PERCENT),
                Arguments.of("%=", Simple.Operator.PERCENT_EQUAL),
                Arguments.of("^", Simple.Operator.XOR),
                Arguments.of("^=", Simple.Operator.XOR_EQUAL),
                Arguments.of("~", Simple.Operator.BITWISE_NOT),
                Arguments.of(":", Simple.Operator.COLON),
                Arguments.of(",", Simple.Operator.COMMA),
                Arguments.of(".", Simple.Operator.DOT),
                Arguments.of(";", Simple.Special.SEMICOLON));
    }

    @Test
    void delimitersAreAllRecognized() {
        final LexerResult result = tokenize("()[]{}");

        assertEquals(
                List.of(
                        Simple.Delimiter.OPEN_PAREN,
                        Simple.Delimiter.CLOSE_PAREN,
                        Simple.Delimiter.OPEN_BRACKET,
                        Simple.Delimiter.CLOSE_BRACKET,
                        Simple.Delimiter.OPEN_BRACE,
                        Simple.Delimiter.CLOSE_BRACE,
                        Simple.Special.EOF),
                kinds(result));
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void keywordsBooleansAsciiIdentifiersAndUnicodeIdentifiersAreDistinguished() {
        final LexerResult result = tokenize("fun i32 true false _name café 变量");

        assertEquals(
                List.of(
                        Simple.Keyword.FUN,
                        Simple.TypeKeyword.I32,
                        new TokenKind.KeywordBool(true),
                        new TokenKind.KeywordBool(false),
                        new TokenKind.IdentifierAscii("_name"),
                        new TokenKind.IdentifierUnicode("café"),
                        new TokenKind.IdentifierUnicode("变量"),
                        Simple.Special.EOF),
                kinds(result));
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void decimalNumbersSupportIntegersFractionsExponentsSuffixesAndDotLookahead() {
        final LexerResult result = tokenize("0 123 45.67 .5 1e5 2E-3 3.4e+5 8u 9f 10i16 11U32");

        assertEquals(
                List.of(
                        new TokenKind.Numeric(new INumber.Integer(0)),
                        new TokenKind.Numeric(new INumber.Integer(123)),
                        new TokenKind.Numeric(new INumber.Float64(45.67)),
                        new TokenKind.Numeric(new INumber.Float64(0.5)),
                        new TokenKind.Numeric(new INumber.Scientific64(1.0, 5)),
                        new TokenKind.Numeric(new INumber.Scientific64(2.0, -3)),
                        new TokenKind.Numeric(new INumber.Scientific64(3.4, 5)),
                        new TokenKind.Numeric(new INumber.UnsignedInteger(8)),
                        new TokenKind.Numeric(new INumber.Float32(9.0f)),
                        new TokenKind.Numeric(new INumber.I16((short) 10)),
                        new TokenKind.Numeric(new INumber.U32(11)),
                        Simple.Special.EOF),
                kinds(result));
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void rangeAndMethodCallUseDotTokens() {
        final LexerResult result = tokenize("1..5 42.toString()");

        assertEquals(
                List.of(
                        new TokenKind.Numeric(new INumber.Integer(1)),
                        Simple.Operator.DOT,
                        new TokenKind.Numeric(new INumber.Float64(0.5)),
                        new TokenKind.Numeric(new INumber.Integer(42)),
                        Simple.Operator.DOT,
                        new TokenKind.IdentifierAscii("toString"),
                        Simple.Delimiter.OPEN_PAREN,
                        Simple.Delimiter.CLOSE_PAREN,
                        Simple.Special.EOF),
                kinds(result));
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void radixNumbersSupportAllBasesAndUnsignedSuffixes() {
        final LexerResult result = tokenize("#b1010 #o777 #xdeadBEEF #b1010U #o7u #xFFU");

        assertEquals(
                List.of(
                        new TokenKind.Binary(new INumber.Integer(10)),
                        new TokenKind.Octal(new INumber.Integer(511)),
                        new TokenKind.Hexadecimal(new INumber.Integer(3_735_928_559L)),
                        new TokenKind.Binary(new INumber.UnsignedInteger(10)),
                        new TokenKind.Octal(new INumber.UnsignedInteger(7)),
                        new TokenKind.Hexadecimal(new INumber.UnsignedInteger(255)),
                        Simple.Special.EOF),
                kinds(result));
        assertTrue(result.errors().isEmpty());
    }

    @ParameterizedTest(name = "malformed radix literal {0} reports {1}")
    @MethodSource("malformedRadixCases")
    void malformedRadixNumbersReportTheSpecificError(
            final String source, final ErrorCode expected) {
        final LexerResult result = tokenize(source);

        final List<TokenKind> expectedTokens =
                "#q12".equals(source)
                        ? List.of(new TokenKind.IdentifierAscii("q12"), Simple.Special.EOF)
                        : List.of(Simple.Special.EOF);
        assertEquals(expectedTokens, kinds(result));
        assertEquals(List.of(expected), errorCodes(result));
    }

    private static Stream<Arguments> malformedRadixCases() {
        return Stream.of(
                Arguments.of("#b", ErrorCode.E0002),
                Arguments.of("#o", ErrorCode.E0003),
                Arguments.of("#x", ErrorCode.E0004),
                Arguments.of("#q12", ErrorCode.E0001));
    }

    @Test
    void commentsBecomeTokensAndDoNotConsumeFollowingSource() {
        final LexerResult result = tokenize("// line\nalpha /* multi\nline */ beta");

        assertEquals(
                List.of(
                        Simple.Special.COMMENT,
                        new TokenKind.IdentifierAscii("alpha"),
                        Simple.Special.MULTILINE_COMMENT,
                        new TokenKind.IdentifierAscii("beta"),
                        Simple.Special.EOF),
                kinds(result));
        assertTrue(result.errors().isEmpty());
        assertTrue(result.tokens().get(2).span().isMultiline());
    }

    @Test
    void unterminatedMultilineCommentStillProducesRecoveryTokenAndError() {
        final LexerResult result = tokenize("/* never closes");

        assertEquals(List.of(Simple.Special.MULTILINE_COMMENT, Simple.Special.EOF), kinds(result));
        assertEquals(List.of(ErrorCode.E0008), errorCodes(result));
    }

    @Test
    void stringsDecodeStandardHexAndUnicodeEscapes() {
        final String unicodeEscape = "\\" + "U{0001F600}";
        final LexerResult result =
                tokenize("\"a\\n\\r\\t\\\\\\'\\\"\\0\\x41" + unicodeEscape + "\"");

        assertEquals(
                List.of(new TokenKind.StringLiteral("a\n\r\t\\'\"\0A😀"), Simple.Special.EOF),
                kinds(result));
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void excessiveUnicodeEscapeDigitsConsumeTheClosingBraceAndResumeLexing() {
        final String excessiveEscape = "\\" + "u{12345}";
        final LexerResult result = tokenize("\"" + excessiveEscape + "\" tail");

        assertEquals(
                List.of(
                        new TokenKind.StringLiteral(""),
                        new TokenKind.IdentifierAscii("tail"),
                        Simple.Special.EOF),
                kinds(result));
        assertEquals(List.of(ErrorCode.E0007), errorCodes(result));
    }

    @Test
    void excessiveUnicodeEscapeDigitsAreDrainedWhenTheSourceEndsWithoutClosingBrace() {
        final String excessiveEscape = "\\" + "u{12345";
        final LexerResult result = tokenize("\"" + excessiveEscape);

        assertEquals(List.of(new TokenKind.StringLiteral(""), Simple.Special.EOF), kinds(result));
        assertEquals(List.of(ErrorCode.E0007, ErrorCode.E0005), errorCodes(result));
    }

    @Test
    void unterminatedStringStopsAtLineTerminatorAndReportsError() {
        final LexerResult result = tokenize("\"hello\nworld");

        assertEquals(
                List.of(
                        new TokenKind.StringLiteral("hello"),
                        new TokenKind.IdentifierAscii("world"),
                        Simple.Special.EOF),
                kinds(result));
        assertEquals(List.of(ErrorCode.E0005), errorCodes(result));
    }

    @Test
    void characterLiteralsSupportCharactersEscapesAndUnicodeCodePoints() {
        final String unicodeEscape = "\\" + "U{0001F600}";
        final LexerResult result = tokenize("'a' '\\n' '" + unicodeEscape + "'");

        assertEquals(
                List.of(
                        new TokenKind.CharLiteral("a"),
                        new TokenKind.CharLiteral("\n"),
                        new TokenKind.CharLiteral("😀"),
                        Simple.Special.EOF),
                kinds(result));
        assertTrue(result.errors().isEmpty());
    }

    @ParameterizedTest(name = "invalid character literal {0}")
    @MethodSource("invalidCharacterCases")
    void invalidCharacterLiteralsReportE0006(final String source) {
        final LexerResult result = tokenize(source);

        assertEquals(List.of(ErrorCode.E0006), errorCodes(result));
        assertEquals(2, result.tokens().size(), "A recovery token and EOF are expected");
        assertInstanceOf(TokenKind.CharLiteral.class, result.tokens().getFirst().type());
    }

    private static Stream<String> invalidCharacterCases() {
        return Stream.of("''", "'ab'", "'unterminated", "'a\n");
    }

    @Test
    void invalidEscapeIsReportedButLexingContinues() {
        final LexerResult result = tokenize("\"bad\\q\" next");

        assertEquals(
                List.of(
                        new TokenKind.StringLiteral("badq"),
                        new TokenKind.IdentifierAscii("next"),
                        Simple.Special.EOF),
                kinds(result));
        assertEquals(List.of(ErrorCode.E0007), errorCodes(result));
    }

    @Test
    void unrecognizedCharactersAreReportedAndOtherTokensAreRetained() {
        final LexerResult result = tokenize("left ? right @");

        assertEquals(
                List.of(
                        new TokenKind.IdentifierAscii("left"),
                        new TokenKind.IdentifierAscii("right"),
                        Simple.Special.EOF),
                kinds(result));
        assertEquals(List.of(ErrorCode.E0001, ErrorCode.E0001), errorCodes(result));
    }

    @Test
    void unicodeLineTerminatorsUpdateTokenLocationsAndLineCount() {
        final Lexer lexer = new Lexer(SOURCE_PATH, "one\u2028two\u2029three");
        final LexerResult result = lexer.tokenize();

        assertEquals(2, lexer.lineCount());
        assertEquals(1, result.tokens().get(0).span().start().line());
        assertEquals(2, result.tokens().get(1).span().start().line());
        assertEquals(3, result.tokens().get(2).span().start().line());
        assertTrue(result.errors().isEmpty());
    }

    @Test
    void tokenSpansAreHalfOpenAndUseUtf8AndCodePointOffsets() {
        final LexerResult result = tokenize("é 变量");
        final Token first = result.tokens().get(0);
        final Token second = result.tokens().get(1);

        assertAll(
                () -> assertEquals("é", first.span().extractFrom("é 😀")),
                () -> assertEquals(0, first.span().start().offset()),
                () -> assertEquals(1, first.span().end().offset()),
                () -> assertEquals(0, first.span().start().utf8Offset()),
                () -> assertEquals(0, first.span().start().codePointOffset()),
                () -> assertEquals("变量", second.span().extractFrom("é 变量")),
                () -> assertEquals(2, second.span().start().offset()),
                () -> assertEquals(4, second.span().end().offset()),
                () -> assertEquals(3, second.span().start().utf8Offset()),
                () -> assertEquals(2, second.span().start().codePointOffset()));
    }

    @Test
    void overflowingDecimalAndRadixNumbersProduceNoInvalidNumericToken() {
        final String decimal = "99999999999999999999999999999999";
        final String binary = "#b" + "1".repeat(64);

        final LexerResult decimalResult = tokenize(decimal);
        final LexerResult binaryResult = tokenize(binary);

        assertEquals(List.of(Simple.Special.EOF), kinds(decimalResult));
        assertEquals(List.of(ErrorCode.E0010), errorCodes(decimalResult));
        assertEquals(List.of(Simple.Special.EOF), kinds(binaryResult));
        assertEquals(List.of(ErrorCode.E0002), errorCodes(binaryResult));
    }

    private static LexerResult tokenize(final String source) {
        return new Lexer(SOURCE_PATH, source).tokenize();
    }

    private static List<TokenKind> kinds(final LexerResult result) {
        return result.tokens().stream().map(Token::type).toList();
    }

    private static List<ErrorCode> errorCodes(final LexerResult result) {
        return result.errors().stream().map(error -> error.code().orElseThrow()).toList();
    }
}
