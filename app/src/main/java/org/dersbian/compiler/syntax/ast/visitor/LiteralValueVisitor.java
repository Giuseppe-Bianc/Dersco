package org.dersbian.compiler.syntax.ast.visitor;

import org.dersbian.compiler.syntax.ast.LiteralValue;

/**
 * Visitor contract for {@link LiteralValue} variants.
 *
 * <p>This visitor is intentionally separate from {@link ExprVisitor}: {@link LiteralValue} is not
 * an {@link org.dersbian.compiler.syntax.ast.Expr} and does not carry a {@link
 * org.dersbian.compiler.lexer.token.Span}; it is a pure value payload embedded in {@link
 * org.dersbian.compiler.syntax.ast.Expr.Literal}. Keeping the hierarchies distinct avoids mixing
 * structural (span-bearing) nodes with value payloads in the same visitor.
 *
 * @param <R> result type produced by each visit method
 * @param <C> context type threaded through the traversal
 */
public interface LiteralValueVisitor<R, C> {

    /**
     * Visits a {@link LiteralValue.Numeric} variant.
     *
     * @param value the numeric literal value
     * @param ctx traversal context
     * @return visit result
     */
    R visitNumeric(LiteralValue.Numeric value, C ctx);

    /**
     * Visits a {@link LiteralValue.StringLit} variant.
     *
     * @param value the string literal value
     * @param ctx traversal context
     * @return visit result
     */
    R visitStringLit(LiteralValue.StringLit value, C ctx);

    /**
     * Visits a {@link LiteralValue.CharLit} variant.
     *
     * @param value the character literal value
     * @param ctx traversal context
     * @return visit result
     */
    R visitCharLit(LiteralValue.CharLit value, C ctx);

    /**
     * Visits a {@link LiteralValue.Bool} variant.
     *
     * @param value the boolean literal value
     * @param ctx traversal context
     * @return visit result
     */
    R visitBool(LiteralValue.Bool value, C ctx);

    /**
     * Visits a {@link LiteralValue.NullPtr} variant.
     *
     * @param value the null pointer literal value
     * @param ctx traversal context
     * @return visit result
     */
    R visitNullPtr(LiteralValue.NullPtr value, C ctx);
}
