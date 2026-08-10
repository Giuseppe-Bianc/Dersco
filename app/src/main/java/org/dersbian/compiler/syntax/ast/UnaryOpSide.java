package org.dersbian.compiler.syntax.ast;

/**
 * Represents the side on which a unary operator is applied relative to its operand.
 *
 * <p>A unary operator can either precede (prefix) or follow (postfix) its operand.
 *
 * <p>Examples:
 *
 * <ul>
 *   <li>{@link #PREFIX} - {@code -x}, {@code !flag}, {@code ++i}
 *   <li>{@link #POSTFIX} - {@code i++}, {@code i--}
 * </ul>
 */
public enum UnaryOpSide {

    /** The operator appears before the operand (e.g., {@code -x} or {@code ++i}). */
    PREFIX,

    /** The operator appears after the operand (e.g., {@code i++} or {@code i--}). */
    POSTFIX,
}
