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
                        new TokenKind.Numeric(new INumber.IntegerValue(123, null)),
                        new TokenKind.Numeric(new INumber.FloatingValue(45.67, null)),
                        new TokenKind.Numeric(new INumber.FloatingValue(9.01, null)),
                        new TokenKind.Numeric(new INumber.FloatingValue(1e5, null)),
                        new TokenKind.Numeric(new INumber.FloatingValue(2E-3, null)),
                        new TokenKind.Numeric(new INumber.FloatingValue(1.2e3, null)),
                        new TokenKind.Numeric(new INumber.FloatingValue(123.0, null)),
                        new TokenKind.Numeric(new INumber.FloatingValue(0.456, null)),
                        new TokenKind.Numeric(new INumber.FloatingValue(10e5, null)),
                        new TokenKind.Numeric(new INumber.FloatingValue(3.4e+5, null)),
                        new TokenKind.Numeric(new INumber.FloatingValue(5e0, null)),
                        new TokenKind.Numeric(new INumber.FloatingValue(0e0, null)),
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
                        new TokenKind.Binary(new INumber.IntegerValue(10L, null)),
                        new TokenKind.Octal(new INumber.IntegerValue(511L, null)),
                        new TokenKind.Hexadecimal(new INumber.IntegerValue(31L, null)),
                        new TokenKind.Binary(new INumber.IntegerValue(0L, null)),
                        new TokenKind.Octal(new INumber.IntegerValue(0L, null)),
                        new TokenKind.Hexadecimal(new INumber.IntegerValue(0L, null)),
                        new TokenKind.Binary(new INumber.IntegerValue(255L, null)),
                        new TokenKind.Octal(new INumber.IntegerValue(255L, null)),
                        new TokenKind.Hexadecimal(new INumber.IntegerValue(3735928559L, null)),
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
                        new TokenKind.Binary(new INumber.IntegerValue(10L, "u")),
                        new TokenKind.Octal(new INumber.IntegerValue(511L, "u")),
                        new TokenKind.Hexadecimal(new INumber.IntegerValue(31L, "u")),
                        new TokenKind.Binary(new INumber.IntegerValue(0L, "u")),
                        new TokenKind.Octal(new INumber.IntegerValue(0L, "u")),
                        new TokenKind.Hexadecimal(new INumber.IntegerValue(0L, "u")),
                        new TokenKind.Binary(new INumber.IntegerValue(255L, "u")),
                        new TokenKind.Octal(new INumber.IntegerValue(255L, "u")),
                        new TokenKind.Hexadecimal(new INumber.IntegerValue(3735928559L, "u")),
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
}
