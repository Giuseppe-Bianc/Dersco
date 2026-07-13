package org.dersbian.compiler.lexer;

import java.nio.file.Path;
import java.util.List;
import org.dersbian.compiler.error.ErrorCode;
import org.dersbian.compiler.lexer.token.Token;
import org.dersbian.compiler.lexer.token.TokenKind;
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
    void testOperatorPrecedence() {
        final Lexer lexer = new Lexer(Path.of(TEST_PATH), "<< <<= >> >>= <= >= < >");
        final LexerResult result = lexer.tokenize();
        final List<Token> tokens = result.tokens();

        Assertions.assertEquals(9, tokens.size()); // 8 operators + 1 EOF
        Assertions.assertEquals(TokenKind.Simple.Operator.SHIFT_LEFT, tokens.get(0).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.SHIFT_LEFT_EQUAL, tokens.get(1).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.SHIFT_RIGHT, tokens.get(2).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.SHIFT_RIGHT_EQUAL, tokens.get(3).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.LESS_EQUAL, tokens.get(4).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.GREATER_EQUAL, tokens.get(5).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.LESS, tokens.get(6).type());
        Assertions.assertEquals(TokenKind.Simple.Operator.GREATER, tokens.get(7).type());
        Assertions.assertEquals(TokenKind.Simple.Special.EOF, tokens.get(8).type());
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

        // Should recover and produce foo, bar, and EOF
        Assertions.assertEquals(3, tokens.size());
        Assertions.assertEquals("foo", ((TokenKind.IdentifierAscii) tokens.get(0).type()).value());
        Assertions.assertEquals("bar", ((TokenKind.IdentifierAscii) tokens.get(1).type()).value());
        Assertions.assertEquals(TokenKind.Simple.Special.EOF, tokens.get(2).type());

        // Should report exactly 1 error of code E0008
        Assertions.assertEquals(1, result.errors().size());
        Assertions.assertEquals(ErrorCode.E0001, result.errors().get(0).code().orElse(null));
    }
}
