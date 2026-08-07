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

    /** Addition assignment operator (+=). */
    ADD_EQUAL,

    /** Subtraction operator (-). */
    SUBTRACT,

    /** Subtraction assignment operator (-=). */
    SUBTRACT_EQUAL,

    /** Multiplication operator (*). */
    MULTIPLY,

    /** Multiplication assignment operator (*=). */
    MULTIPLY_EQUAL,

    /** Division operator (/). */
    DIVIDE,

    /** Division assignment operator (/=). */
    DIVIDE_EQUAL,

    /** Modulo operator (%). */
    MODULO,

    /** Modulo assignment operator (%=). */
    MODULO_EQUAL,

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

    /** Bitwise AND assignment operator (&amp;=). */
    BITWISE_AND_EQUAL,

    /** Bitwise OR operator (|). */
    BITWISE_OR,

    /** Bitwise OR assignment operator (|=). */
    BITWISE_OR_EQUAL,

    /** Bitwise XOR operator (^). */
    BITWISE_XOR,

    /** Bitwise XOR assignment operator (^=). */
    BITWISE_XOR_EQUAL,

    /** Shift left operator (<<). */
    SHIFT_LEFT,

    /** Shift left assignment operator (<<=). */
    SHIFT_LEFT_EQUAL,

    /** Shift right operator (>>). */
    SHIFT_RIGHT,

    /** Shift right assignment operator (>>=). */
    SHIFT_RIGHT_EQUAL;

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
                case PLUS_EQUAL -> ADD_EQUAL;

                case MINUS -> SUBTRACT;
                case MINUS_EQUAL -> SUBTRACT_EQUAL;

                case STAR -> MULTIPLY;
                case STAR_EQUAL -> MULTIPLY_EQUAL;

                case SLASH -> DIVIDE;
                case SLASH_EQUAL -> DIVIDE_EQUAL;

                case PERCENT -> MODULO;
                case PERCENT_EQUAL -> MODULO_EQUAL;

                case EQUAL_EQUAL -> EQUAL;
                case NOT_EQUAL -> NOT_EQUAL;

                case LESS -> LESS;
                case LESS_EQUAL -> LESS_EQUAL;

                case GREATER -> GREATER;
                case GREATER_EQUAL -> GREATER_EQUAL;

                case AND_AND -> AND;
                case OR_OR -> OR;

                case AND -> BITWISE_AND;
                case AND_EQUAL -> BITWISE_AND_EQUAL;

                case OR -> BITWISE_OR;
                case OR_EQUAL -> BITWISE_OR_EQUAL;

                case XOR -> BITWISE_XOR;
                case XOR_EQUAL -> BITWISE_XOR_EQUAL;

                case SHIFT_LEFT -> SHIFT_LEFT;
                case SHIFT_LEFT_EQUAL -> SHIFT_LEFT_EQUAL;

                case SHIFT_RIGHT -> SHIFT_RIGHT;
                case SHIFT_RIGHT_EQUAL -> SHIFT_RIGHT_EQUAL;

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
