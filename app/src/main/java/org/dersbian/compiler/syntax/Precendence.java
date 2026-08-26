package org.dersbian.compiler.syntax;

import org.dersbian.compiler.lexer.token.Token;
import org.dersbian.compiler.lexer.token.TokenKind;

/**
 * Defines operator binding powers (precedence and associativity) used by the Pratt-style expression
 * parser in {@link Parser}.
 */
@SuppressWarnings("PMD.CyclomaticComplexity")
public record Precendence(int left, int right) {

    /**
     * Returns the left/right binding powers for the given binary/infix operator token, used to
     * decide when to stop or continue parsing an expression via {@code led}.
     *
     * @param token the operator token
     * @return the binding power pair for the token
     */
    public static Precendence bindingPower(final Token token) {
        return switch (token.type()) {
            case TokenKind.Simple.Operator.EQUAL,
                    TokenKind.Simple.Operator.PLUS_EQUAL,
                    TokenKind.Simple.Operator.MINUS_EQUAL,
                    TokenKind.Simple.Operator.PERCENT_EQUAL,
                    TokenKind.Simple.Operator.XOR_EQUAL ->
                    new Precendence(2, 1);
            case TokenKind.Simple.Operator.OR_OR -> new Precendence(4, 3);
            case TokenKind.Simple.Operator.AND_AND -> new Precendence(6, 5);
            case TokenKind.Simple.Operator.EQUAL_EQUAL, TokenKind.Simple.Operator.NOT_EQUAL ->
                    new Precendence(8, 7);
            case TokenKind.Simple.Operator.LESS,
                    TokenKind.Simple.Operator.LESS_EQUAL,
                    TokenKind.Simple.Operator.GREATER,
                    TokenKind.Simple.Operator.GREATER_EQUAL ->
                    new Precendence(10, 9);
            case TokenKind.Simple.Operator.OR -> new Precendence(12, 11);
            case TokenKind.Simple.Operator.XOR -> new Precendence(14, 13);
            case TokenKind.Simple.Operator.AND -> new Precendence(16, 15);
            case TokenKind.Simple.Operator.SHIFT_LEFT, TokenKind.Simple.Operator.SHIFT_RIGHT ->
                    new Precendence(18, 17);
            case TokenKind.Simple.Operator.PLUS, TokenKind.Simple.Operator.MINUS ->
                    new Precendence(20, 19);
            case TokenKind.Simple.Operator.STAR,
                    TokenKind.Simple.Operator.SLASH,
                    TokenKind.Simple.Operator.PERCENT ->
                    new Precendence(22, 21);
            case TokenKind.Simple.Delimiter.OPEN_PAREN,
                    TokenKind.Simple.Delimiter.OPEN_BRACKET,
                    TokenKind.Simple.Operator.DOT ->
                    new Precendence(27, 26);
            default -> new Precendence(0, 0);
        };
    }

    /**
     * Returns the binding power for the given unary/prefix operator token, used to determine how
     * tightly the operand binds via {@code nud}.
     *
     * @param token the unary operator token
     * @return the binding power for the unary operator
     */
    public static Precendence unaryBindingPower(final Token token) {
        return switch (token.type()) {
            case TokenKind.Simple.Operator.NOT, TokenKind.Simple.Operator.MINUS ->
                    new Precendence(24, 23);
            default -> new Precendence(0, 0);
        };
    }
}
