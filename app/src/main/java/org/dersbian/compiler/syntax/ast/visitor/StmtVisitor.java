package org.dersbian.compiler.syntax.ast.visitor;

import org.dersbian.compiler.syntax.ast.Stmt;

/**
 * Visitor contract for {@link Stmt} nodes.
 *
 * <p>Mirrors the structure of {@link ExprVisitor}: one method per concrete permitted type,
 * double-dispatched from {@link Stmt#accept(StmtVisitor, Object)}.
 *
 * @param <R> result type produced by each visit method
 * @param <C> context type threaded through the traversal
 */
@SuppressWarnings("PMD.TooManyMethods")
public interface StmtVisitor<R, C> {

    /**
     * Visits a {@link Stmt.Expression} node.
     *
     * @param stmt the expression statement node
     * @param ctx traversal context
     * @return visit result
     */
    R visitExpression(Stmt.Expression stmt, C ctx);

    /**
     * Visits a {@link Stmt.VarDeclaration} node.
     *
     * @param stmt the variable declaration statement node
     * @param ctx traversal context
     * @return visit result
     */
    R visitVarDeclaration(Stmt.VarDeclaration stmt, C ctx);

    /**
     * Visits a {@link Stmt.Function} node.
     *
     * @param stmt the function declaration statement node
     * @param ctx traversal context
     * @return visit result
     */
    R visitFunction(Stmt.Function stmt, C ctx);

    /**
     * Visits a {@link Stmt.If} node.
     *
     * @param stmt the conditional statement node
     * @param ctx traversal context
     * @return visit result
     */
    R visitIf(Stmt.If stmt, C ctx);

    /**
     * Visits a {@link Stmt.While} node.
     *
     * @param stmt the while loop statement node
     * @param ctx traversal context
     * @return visit result
     */
    R visitWhile(Stmt.While stmt, C ctx);

    /**
     * Visits a {@link Stmt.For} node.
     *
     * @param stmt the for loop statement node
     * @param ctx traversal context
     * @return visit result
     */
    R visitFor(Stmt.For stmt, C ctx);

    /**
     * Visits a {@link Stmt.Block} node.
     *
     * @param stmt the block statement node
     * @param ctx traversal context
     * @return visit result
     */
    R visitBlock(Stmt.Block stmt, C ctx);

    /**
     * Visits a {@link Stmt.Return} node.
     *
     * @param stmt the return statement node
     * @param ctx traversal context
     * @return visit result
     */
    R visitReturn(Stmt.Return stmt, C ctx);

    /**
     * Visits a {@link Stmt.Break} node.
     *
     * @param stmt the break statement node
     * @param ctx traversal context
     * @return visit result
     */
    R visitBreak(Stmt.Break stmt, C ctx);

    /**
     * Visits a {@link Stmt.Continue} node.
     *
     * @param stmt the continue statement node
     * @param ctx traversal context
     * @return visit result
     */
    R visitContinue(Stmt.Continue stmt, C ctx);

    /**
     * Visits a {@link Stmt.MainFunction} node.
     *
     * @param stmt the main function statement node
     * @param ctx traversal context
     * @return visit result
     */
    R visitMainFunction(Stmt.MainFunction stmt, C ctx);
}
