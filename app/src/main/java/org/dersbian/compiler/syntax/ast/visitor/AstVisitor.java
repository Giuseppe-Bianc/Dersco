package org.dersbian.compiler.syntax.ast.visitor;

/**
 * Composite visitor that unifies traversal of all four AST hierarchies.
 *
 * <p>Concrete visitors that need to traverse a complete AST — expressions, statements, types, and
 * literal values — can extend this interface instead of implementing all four separately. It
 * imposes no additional contract beyond the union of its parents.
 *
 * <p>Implementations are free to implement only the sub-interfaces they need; this composite exists
 * purely as a convenience declaration for full-tree visitors such as a type checker, an
 * interpreter, or a code generator.
 *
 * @param <R> result type produced by each visit method
 * @param <C> context type threaded through the traversal
 */
public interface AstVisitor<R, C>
        extends ExprVisitor<R, C>,
                StmtVisitor<R, C>,
                TypeVisitor<R, C>,
                LiteralValueVisitor<R, C> {}
