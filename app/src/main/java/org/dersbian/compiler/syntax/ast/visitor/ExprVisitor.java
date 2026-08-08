package org.dersbian.compiler.syntax.ast.visitor;

import org.dersbian.compiler.syntax.ast.Expr;

/**
 * Visitor contract for {@link Expr} nodes.
 *
 * <p>Each method corresponds to a concrete permitted type of the sealed {@link Expr} interface.
 * Implementations provide one method per node kind; the double dispatch is resolved by {@link
 * Expr#accept(ExprVisitor, Object)} in each record, which calls the appropriate {@code visit}
 * overload without any {@code instanceof} chain.
 *
 * <p>The type parameter {@code R} is the result produced by each visit method. Use {@link Void}
 * (returning {@code null}) for visitors that operate only for side effects. The context parameter
 * {@code C} carries any traversal state (symbol table, type environment, configuration) needed by
 * the implementation; pass {@code Void} and {@code null} when no context is required.
 *
 * @param <R> result type produced by each visit method
 * @param <C> context type threaded through the traversal
 */
public interface ExprVisitor<R, C> {

    /**
     * Visits a {@link Expr.Binary} node.
     *
     * @param expr the binary expression node
     * @param ctx traversal context
     * @return visit result
     */
    R visitBinary(Expr.Binary expr, C ctx);

    /**
     * Visits a {@link Expr.Unary} node.
     *
     * @param expr the unary expression node
     * @param ctx traversal context
     * @return visit result
     */
    R visitUnary(Expr.Unary expr, C ctx);

    /**
     * Visits a {@link Expr.Grouping} node.
     *
     * @param expr the grouping expression node
     * @param ctx traversal context
     * @return visit result
     */
    R visitGrouping(Expr.Grouping expr, C ctx);

    /**
     * Visits a {@link Expr.Literal} node.
     *
     * <p>The {@link org.dersbian.compiler.syntax.ast.LiteralValue} payload is accessible via {@link
     * Expr.Literal#value()}; implementations that need to distinguish literal kinds should pass the
     * payload to a {@link LiteralValueVisitor}.
     *
     * @param expr the literal expression node
     * @param ctx traversal context
     * @return visit result
     */
    R visitLiteral(Expr.Literal expr, C ctx);

    /**
     * Visits a {@link Expr.ArrayLiteral} node.
     *
     * @param expr the array literal expression node
     * @param ctx traversal context
     * @return visit result
     */
    R visitArrayLiteral(Expr.ArrayLiteral expr, C ctx);

    /**
     * Visits a {@link Expr.Variable} node.
     *
     * @param expr the variable reference expression node
     * @param ctx traversal context
     * @return visit result
     */
    R visitVariable(Expr.Variable expr, C ctx);

    /**
     * Visits a {@link Expr.Assign} node.
     *
     * @param expr the assignment expression node
     * @param ctx traversal context
     * @return visit result
     */
    R visitAssign(Expr.Assign expr, C ctx);

    /**
     * Visits a {@link Expr.Call} node.
     *
     * @param expr the function call expression node
     * @param ctx traversal context
     * @return visit result
     */
    R visitCall(Expr.Call expr, C ctx);

    /**
     * Visits a {@link Expr.ArrayAccess} node.
     *
     * @param expr the array access expression node
     * @param ctx traversal context
     * @return visit result
     */
    R visitArrayAccess(Expr.ArrayAccess expr, C ctx);
}
