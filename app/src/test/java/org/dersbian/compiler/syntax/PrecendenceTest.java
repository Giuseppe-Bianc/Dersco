package org.dersbian.compiler.syntax;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;
import org.dersbian.compiler.lexer.token.SourceId;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.lexer.token.Token;
import org.dersbian.compiler.lexer.token.TokenKind;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.UnitTestAssertionsShouldIncludeMessage"
})
class PrecendenceTest {

    private static final Span SPAN = Span.point(SourceLocation.create(1, 1, 0));
    private static final SourceId SOURCE = new SourceId.InMemoryModule("precedence-test");

    @Test
    void assignsBindingPowersByOperatorFamily() {
        final Map<TokenKind, Precendence> expected =
                Map.ofEntries(
                        Map.entry(TokenKind.Simple.Operator.EQUAL, new Precendence(2, 1)),
                        Map.entry(TokenKind.Simple.Operator.OR_OR, new Precendence(4, 3)),
                        Map.entry(TokenKind.Simple.Operator.AND_AND, new Precendence(6, 5)),
                        Map.entry(TokenKind.Simple.Operator.EQUAL_EQUAL, new Precendence(8, 7)),
                        Map.entry(TokenKind.Simple.Operator.LESS, new Precendence(10, 9)),
                        Map.entry(TokenKind.Simple.Operator.OR, new Precendence(12, 11)),
                        Map.entry(TokenKind.Simple.Operator.XOR, new Precendence(14, 13)),
                        Map.entry(TokenKind.Simple.Operator.AND, new Precendence(16, 15)),
                        Map.entry(TokenKind.Simple.Operator.SHIFT_LEFT, new Precendence(18, 17)),
                        Map.entry(TokenKind.Simple.Operator.PLUS, new Precendence(20, 19)),
                        Map.entry(TokenKind.Simple.Operator.STAR, new Precendence(22, 21)),
                        Map.entry(TokenKind.Simple.Delimiter.OPEN_PAREN, new Precendence(27, 26)));

        expected.forEach(
                (kind, bindingPower) ->
                        assertEquals(bindingPower, Precendence.bindingPower(token(kind))));
    }

    @Test
    void assignsZeroBindingPowerToNonInfixTokens() {
        assertEquals(
                new Precendence(0, 0),
                Precendence.bindingPower(token(TokenKind.Simple.Keyword.IF)));
    }

    @Test
    void assignsUnaryBindingPowerOnlyToPrefixOperators() {
        assertEquals(
                new Precendence(24, 23),
                Precendence.unaryBindingPower(token(TokenKind.Simple.Operator.NOT)));
        assertEquals(
                new Precendence(24, 23),
                Precendence.unaryBindingPower(token(TokenKind.Simple.Operator.MINUS_MINUS)));
        assertEquals(
                new Precendence(0, 0),
                Precendence.unaryBindingPower(token(TokenKind.Simple.Operator.PLUS)));
    }

    private static Token token(final TokenKind kind) {
        return Token.create(SOURCE, kind, SPAN);
    }
}
