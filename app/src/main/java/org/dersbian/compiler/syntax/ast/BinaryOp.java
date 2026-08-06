package org.dersbian.compiler.syntax.ast;

import java.util.Objects;
import org.dersbian.compiler.CompilerException;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.error.ErrorCode;
import org.dersbian.compiler.lexer.token.Token;
import org.dersbian.compiler.lexer.token.TokenKind;

/** Binary operators supported in syntax expressions. */
@SuppressWarnings({"PMD.ShortVariable", "PMD.CyclomaticComplexity"})
public enum BinaryOp {
    /** Addition operator (+). */
    ADD,

    /** Subtraction operator (-). */
    SUBTRACT,

    /** Multiplication operator (*). */
    MULTIPLY,

    /** Division operator (/). */
    DIVIDE,

    /** Modulo operator (%). */
    MODULO,

    /** Equality comparison operator (==). */
    EQUAL,

    /** Inequality comparison operator (!=). */
    NOT_EQUAL,

    /** Less-than comparison operator (&lt;). */
    LESS,

    /** Less-than-or-equal comparison operator (&lt;=). */
    LESS_EQUAL,

    /** Greater-than comparison operator (&gt;). */
    GREATER,

    /** Greater-than-or-equal comparison operator (&gt;=). */
    GREATER_EQUAL,

    /** Logical AND operator (&amp;&amp;). */
    AND,

    /** Logical OR operator (||). */
    OR,

    /** Bitwise AND operator (&amp;). */
    BITWISE_AND,

    /** Bitwise OR operator (|). */
    BITWISE_OR,

    /** Bitwise XOR operator (^). */
    BITWISE_XOR,

    /** Shift left operator (&lt;&lt;). */
    SHIFT_LEFT,

    /** Shift right operator (&gt;&gt;). */
    SHIFT_RIGHT;

    /**
     * Converts a token into its corresponding binary operator.
     *
     * @param token source token
     * @return corresponding {@link BinaryOp}
     * @throws CompilerException if the token kind is not a valid binary operator
     */
    public static BinaryOp getOp(final Token token) {
        Objects.requireNonNull(token, "token must not be null");
        if (token.type() instanceof TokenKind.Simple.Operator operator) {
            return switch (operator) {
                case PLUS -> ADD;
                case MINUS -> SUBTRACT;
                case STAR -> MULTIPLY;
                case SLASH -> DIVIDE;
                case PERCENT -> MODULO;
                case EQUAL_EQUAL -> EQUAL;
                case NOT_EQUAL -> NOT_EQUAL;
                case LESS -> LESS;
                case LESS_EQUAL -> LESS_EQUAL;
                case GREATER -> GREATER;
                case GREATER_EQUAL -> GREATER_EQUAL;
                case AND_AND -> AND;
                case OR_OR -> OR;
                case AND -> BITWISE_AND;
                case OR -> BITWISE_OR;
                case XOR -> BITWISE_XOR;
                case SHIFT_LEFT -> SHIFT_LEFT;
                case SHIFT_RIGHT -> SHIFT_RIGHT;
                default -> throw createInvalidOpException(token);
            };
        }
        throw createInvalidOpException(token);
    }

    /**
     * Creates a {@link CompileError.SyntaxError} for an invalid binary operator token.
     *
     * @param token invalid token
     * @return formatted syntax error
     */
    public static CompileError.SyntaxError createSyntaxError(final Token token) {
        Objects.requireNonNull(token, "token must not be null");
        return CompileError.syntaxError(
                ErrorCode.E1005, "Invalid binary operator: " + token.type(), token.span(), null);
    }

    private static CompilerException createInvalidOpException(final Token token) {
        final CompileError.SyntaxError error = createSyntaxError(token);
        return new CompilerException(error.toString());
    }
}
