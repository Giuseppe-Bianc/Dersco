package org.dersbian.compiler.lexer;

import java.nio.file.Path;
import java.util.List;
import org.dersbian.compiler.error.ErrorCode;
import org.dersbian.compiler.lexer.token.Token;
import org.dersbian.compiler.lexer.token.TokenKind;
import org.dersbian.compiler.lexer.token.number.INumber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestAssertionsShouldIncludeMessage",
    "PMD.UnitTestContainsTooManyAsserts"
})
class LexerTest {
    private static final String TEST_PATH = "test.dr";

    @Test
    void testOperators() {
        final Lexer lexer =
                new Lexer(
                        Path.of(TEST_PATH),
                        "+ += ++ = - -= -- == != < <= > >= || && << <<= >> >>= %= ^= * *= / /= % ^"
                                + " | & ! : , .");
        final LexerResult result = lexer.tokenize();
        final List<Token> tokens = result.tokens();

        Assertions.assertEquals(34, tokens.size());

        Assertions.assertEquals(TokenKind.Simple.Operator.PLUS, tokens.get(0).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.PLUS_EQUAL, tokens.get(1).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.PLUS_PLUS, tokens.get(2).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.EQUAL, tokens.get(3).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.MINUS, tokens.get(4).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.MINUS_EQUAL, tokens.get(5).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.MINUS_MINUS, tokens.get(6).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.EQUAL_EQUAL, tokens.get(7).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.NOT_EQUAL, tokens.get(8).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.LESS, tokens.get(9).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.LESS_EQUAL, tokens.get(10).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.GREATER, tokens.get(11).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.GREATER_EQUAL, tokens.get(12).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.OR_OR, tokens.get(13).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.AND_AND, tokens.get(14).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.SHIFT_LEFT, tokens.get(15).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.SHIFT_LEFT_EQUAL, tokens.get(16).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.SHIFT_RIGHT, tokens.get(17).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.SHIFT_RIGHT_EQUAL, tokens.get(18).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.PERCENT_EQUAL, tokens.get(19).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.XOR_EQUAL, tokens.get(20).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.STAR, tokens.get(21).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.STAR_EQUAL, tokens.get(22).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.SLASH, tokens.get(23).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.SLASH_EQUAL, tokens.get(24).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.PERCENT, tokens.get(25).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.XOR, tokens.get(26).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.OR, tokens.get(27).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.AND, tokens.get(28).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.NOT, tokens.get(29).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.COLON, tokens.get(30).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.COMMA, tokens.get(31).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.DOT, tokens.get(32).type());
        Assertions.assertEquals(TokenKind.Simple.Special.EOF, tokens.get(33).type());
    }

    @Test
    void testDecimalNumbers() {
        final Lexer lexer =
                new Lexer(
                        Path.of(TEST_PATH),
                        "123 45.67 9.01 1e5 2E-3 1.2e3 123. .456 10e5 3.4e+5 5e0 0e0");
        final LexerResult result = lexer.tokenize();
        final List<TokenKind> tokens = result.tokens().stream().map(Token::type).toList();

        Assertions.assertEquals(13, tokens.size());
        final List<TokenKind> expectedTokens =
                List.of(
                        new TokenKind.Numeric(new INumber.Integer(123)),
                        new TokenKind.Numeric(new INumber.Float64(45.67)),
                        new TokenKind.Numeric(new INumber.Float64(9.01)),
                        new TokenKind.Numeric(new INumber.Scientific64(1.0, 5)),
                        new TokenKind.Numeric(new INumber.Scientific64(2.0, -3)),
                        new TokenKind.Numeric(new INumber.Scientific64(1.2, 3)),
                        new TokenKind.Numeric(new INumber.Float64(123.0)),
                        new TokenKind.Numeric(new INumber.Float64(0.456)),
                        new TokenKind.Numeric(new INumber.Scientific64(10.0, 5)),
                        new TokenKind.Numeric(new INumber.Scientific64(3.4, 5)),
                        new TokenKind.Numeric(new INumber.Scientific64(5.0, 0)),
                        new TokenKind.Numeric(new INumber.Scientific64(0.0, 0)),
                        TokenKind.Simple.Special.EOF);

        Assertions.assertEquals(expectedTokens, tokens);
    }

    @Test
    void testBaseSpecificNumbers() {
        final Lexer lexer =
                new Lexer(
                        Path.of(TEST_PATH),
                        "#b1010 #o777 #x1f #b0 #o0 #x0 #b11111111 #o377 #xdeadBEEF");
        final LexerResult result = lexer.tokenize();
        final List<TokenKind> tokens = result.tokens().stream().map(Token::type).toList();

        Assertions.assertEquals(10, tokens.size());
        final List<TokenKind> expectedTokens =
                List.of(
                        new TokenKind.Binary(new INumber.Integer(10L)),
                        new TokenKind.Octal(new INumber.Integer(511L)),
                        new TokenKind.Hexadecimal(new INumber.Integer(31L)),
                        new TokenKind.Binary(new INumber.Integer(0L)),
                        new TokenKind.Octal(new INumber.Integer(0L)),
                        new TokenKind.Hexadecimal(new INumber.Integer(0L)),
                        new TokenKind.Binary(new INumber.Integer(255L)),
                        new TokenKind.Octal(new INumber.Integer(255L)),
                        new TokenKind.Hexadecimal(new INumber.Integer(3_735_928_559L)),
                        TokenKind.Simple.Special.EOF);

        Assertions.assertEquals(expectedTokens, tokens);
    }

    @Test
    void testBaseSpecificNumbersUnsigned() {
        final Lexer lexer =
                new Lexer(
                        Path.of(TEST_PATH),
                        "#b1010u #o777u #x1fu #b0u #o0u #x0u #b11111111u #o377u #xdeadBEEFu");
        final LexerResult result = lexer.tokenize();
        final List<TokenKind> tokens = result.tokens().stream().map(Token::type).toList();

        Assertions.assertEquals(10, tokens.size());
        final List<TokenKind> expectedTokens =
                List.of(
                        new TokenKind.Binary(new INumber.UnsignedInteger(10L)),
                        new TokenKind.Octal(new INumber.UnsignedInteger(511L)),
                        new TokenKind.Hexadecimal(new INumber.UnsignedInteger(31L)),
                        new TokenKind.Binary(new INumber.UnsignedInteger(0L)),
                        new TokenKind.Octal(new INumber.UnsignedInteger(0L)),
                        new TokenKind.Hexadecimal(new INumber.UnsignedInteger(0L)),
                        new TokenKind.Binary(new INumber.UnsignedInteger(255L)),
                        new TokenKind.Octal(new INumber.UnsignedInteger(255L)),
                        new TokenKind.Hexadecimal(new INumber.UnsignedInteger(3_735_928_559L)),
                        TokenKind.Simple.Special.EOF);

        // Ensures the underlying class/record holds a verified distinct unsigned form.
        Assertions.assertEquals(expectedTokens, tokens);
    }

    @Test
    void testNuberEdgeCases() {
        final Lexer lexer =
                new Lexer(
                        Path.of(TEST_PATH),
                        "#b111111111111111111111111111111111111111111111111111111111111111");
        final LexerResult result = lexer.tokenize();
        final List<TokenKind> tokens = result.tokens().stream().map(Token::type).toList();
        final List<TokenKind> expectedTokens =
                List.of(
                        new TokenKind.Binary(new INumber.Integer(Long.MAX_VALUE)),
                        TokenKind.Simple.Special.EOF);
        Assertions.assertEquals(expectedTokens, tokens);
    }

    @Test
    void testUnicodeLineTerminatorsInStringLiteral() {
        // String literal contains a line separator \u2028, which should be treated as a line
        // terminator, making the string literal unterminated.
        final Lexer lexer = new Lexer(Path.of(TEST_PATH), "\"hello\u2028world\"");
        final LexerResult result = lexer.tokenize();

        Assertions.assertFalse(result.errors().isEmpty());
        Assertions.assertEquals(ErrorCode.E0005, result.errors().get(0).code().orElse(null));
    }

    @Test
    void testUnicodeLineTerminatorsIncrementLineNumber() {
        final Lexer lexer = new Lexer(Path.of(TEST_PATH), "foo\u2028bar\u2029baz");
        final LexerResult result = lexer.tokenize();
        final List<Token> tokens = result.tokens();

        // tokens should be "foo", "bar", "baz", EOF
        Assertions.assertEquals(4, tokens.size());
        Assertions.assertEquals("foo", ((TokenKind.IdentifierAscii) tokens.get(0).type()).value());
        Assertions.assertEquals(1, tokens.get(0).span().start().line());

        Assertions.assertEquals("bar", ((TokenKind.IdentifierAscii) tokens.get(1).type()).value());
        Assertions.assertEquals(2, tokens.get(1).span().start().line());

        Assertions.assertEquals("baz", ((TokenKind.IdentifierAscii) tokens.get(2).type()).value());
        Assertions.assertEquals(3, tokens.get(2).span().start().line());
    }

    @Test
    void testUnrecognizedCharacter() {
        final Lexer lexer = new Lexer(Path.of(TEST_PATH), "foo ? bar");
        final LexerResult result = lexer.tokenize();
        final List<Token> tokens = result.tokens();

        Assertions.assertEquals(3, tokens.size());
        Assertions.assertEquals("foo", ((TokenKind.IdentifierAscii) tokens.get(0).type()).value());
        Assertions.assertEquals("bar", ((TokenKind.IdentifierAscii) tokens.get(1).type()).value());
        Assertions.assertEquals(TokenKind.Simple.Special.EOF, tokens.get(2).type());

        Assertions.assertEquals(1, result.errors().size());
        Assertions.assertEquals(ErrorCode.E0001, result.errors().get(0).code().orElse(null));
    }

    @Test
    void testMalformedBaseSpecificNumbers() {
        record TestCase(String input, ErrorCode expectedErrorCode) {}

        final List<TestCase> cases =
                List.of(
                        new TestCase("#b", ErrorCode.E0002),
                        new TestCase("#o", ErrorCode.E0003),
                        new TestCase("#x", ErrorCode.E0004));

        for (final TestCase testCase : cases) {
            final Lexer lexer = new Lexer(Path.of(TEST_PATH), testCase.input());
            final LexerResult result = lexer.tokenize();

            Assertions.assertFalse(
                    result.errors().isEmpty(),
                    "Expected an error for malformed input: " + testCase.input());

            Assertions.assertEquals(
                    1,
                    result.errors().size(),
                    "Expected exactly one error for input: " + testCase.input());

            Assertions.assertEquals(
                    testCase.expectedErrorCode(),
                    result.errors().get(0).code().orElse(null),
                    "Expected error code "
                            + testCase.expectedErrorCode()
                            + " for input: "
                            + testCase.input());
        }
    }

    @Test
    void testUnrecognizedCharacters() {
        record TestCase(String input, String expectedMessage) {}

        final List<TestCase> cases =
                List.of(
                        new TestCase("@", "[E0001] Unrecognized character: '@' at line 1:column 1-line 1:column 2"),
                        new TestCase("`", "[E0001] Unrecognized character: '`' at line 1:column 1-line 1:column 2"));

        for (final TestCase testCase : cases) {
            final Lexer lexer = new Lexer(Path.of(TEST_PATH), testCase.input());
            final LexerResult result = lexer.tokenize();

            Assertions.assertFalse(
                    result.errors().isEmpty(),
                    "Expected an error for unrecognized character: " + testCase.input());

            Assertions.assertEquals(
                    1,
                    result.errors().size(),
                    "Expected exactly one error for input: " + testCase.input());

            Assertions.assertEquals(
                    ErrorCode.E0001,
                    result.errors().get(0).code().orElse(null),
                    "Expected error code E0001 for input: " + testCase.input());

            Assertions.assertEquals(
                    testCase.expectedMessage(),
                    result.errors().get(0).toString(),
                    "Expected error message to match for input: " + testCase.input());
        }
    }

    @Test
    void testTooLargeBinaryNumberIsInvalidToken() {
        final String input = "#b1111111111111111111111111111111111111111111111111111111111111111";
        final Lexer lexer = new Lexer(Path.of(TEST_PATH), input);
        final LexerResult result = lexer.tokenize();
        final List<Token> tokens = result.tokens();

        Assertions.assertEquals(1, tokens.size());
        Assertions.assertEquals(TokenKind.Simple.Special.EOF, tokens.get(0).type());

        Assertions.assertEquals(1, result.errors().size());
        Assertions.assertEquals(ErrorCode.E0002, result.errors().get(0).code().orElse(null));
        Assertions.assertEquals(
                "[E0002] Numeric value out of range for literal '"
                        + input
                        + "'. at line 1:column 1-line 1:column 67",
                result.errors().get(0).toString());
    }
}
