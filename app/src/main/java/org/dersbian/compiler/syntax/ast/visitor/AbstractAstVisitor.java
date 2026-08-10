package org.dersbian.compiler.syntax.ast.visitor;

import java.util.List;
import java.util.Optional;
import lombok.NoArgsConstructor;
import org.dersbian.compiler.syntax.ast.Expr;
import org.dersbian.compiler.syntax.ast.LiteralValue;
import org.dersbian.compiler.syntax.ast.Parameter;
import org.dersbian.compiler.syntax.ast.Stmt;
import org.dersbian.compiler.syntax.ast.Type;

/**
 * Abstract base class providing a complete, recursive traversal of the AST.
 *
 * <p>Every {@code visit*} method calls {@link #defaultResult()} and {@link #aggregateResult} on
 * each child's result, in left-to-right declaration order, mirroring {@code TreeScanner} in javac
 * (§7.1 of the design document). Subclasses override only the methods they care about.
 *
 * <p>The default traversal order for each node is documented on the corresponding method. Traversal
 * is depth-first, children visited before the method returns their aggregate to the parent —
 * effectively a post-order accumulation, though the order in which the visitor method body executes
 * relative to child calls is left to the subclass when it overrides.
 *
 * <p>The context {@code C} is passed unchanged to every child; if a subclass needs to thread a
 * mutable scope or environment, it should wrap it in the context object.
 *
 * @param <R> result type; use {@link Void} for side-effect-only visitors
 * @param <C> context type; use {@link Void} and pass {@code null} if unused
 */
@SuppressWarnings({
    "PMD.TooManyMethods",
    "PMD.ShortVariable",
    "PMD.CouplingBetweenObjects",
    "checkstyle:OverloadMethodsDeclarationOrder"
})
@NoArgsConstructor
public abstract class AbstractAstVisitor<R, C> implements AstVisitor<R, C> {

    // -------------------------------------------------------------------------
    // Aggregation contract — subclasses define what "combine results" means
    // -------------------------------------------------------------------------

    /**
     * Returns the neutral result used when a node has no children or as the starting accumulator
     * before any child is visited.
     *
     * @return neutral / identity result
     */
    protected abstract R defaultResult();

    /**
     * Combines the accumulated result with the result of visiting the next child.
     *
     * <p>The default implementation simply returns {@code nextResult}, discarding {@code
     * aggregate}. Subclasses that need to collect all results (e.g., collect diagnostic messages
     * from all children) must override this.
     *
     * @param aggregate result accumulated so far
     * @param nextResult result of the child just visited
     * @return new accumulated result
     */
    protected R aggregateResult(final R aggregate, final R nextResult) {
        return nextResult;
    }

    // -------------------------------------------------------------------------
    // ExprVisitor implementation
    // -------------------------------------------------------------------------

    /**
     * Default traversal: left → right. Order: {@link Expr.Binary#left()}, {@link
     * Expr.Binary#right()}.
     */
    @Override
    public R visitBinary(final Expr.Binary expr, final C ctx) {
        R result = defaultResult();
        result = aggregateResult(result, expr.left().accept(this, ctx));
        result = aggregateResult(result, expr.right().accept(this, ctx));
        return result;
    }

    /** Default traversal. Order: {@link Expr.Unary#expr()}. */
    @Override
    public R visitUnary(final Expr.Unary expr, final C ctx) {
        return expr.expr().accept(this, ctx);
    }

    /** Default traversal. Order: {@link Expr.Grouping#expr()}. */
    @Override
    public R visitGrouping(final Expr.Grouping expr, final C ctx) {
        return expr.expr().accept(this, ctx);
    }

    /**
     * Default traversal: delegates into the {@link LiteralValue} sub-hierarchy. Order: {@link
     * Expr.Literal#value()}.
     */
    @Override
    public R visitLiteral(final Expr.Literal expr, final C ctx) {
        return expr.value().accept(this, ctx);
    }

    /**
     * Default traversal. Order: each element of {@link Expr.ArrayLiteral#elements()}, left to
     * right.
     */
    @Override
    public R visitArrayLiteral(final Expr.ArrayLiteral expr, final C ctx) {
        R result = defaultResult();
        for (final Expr element : expr.elements()) {
            result = aggregateResult(result, element.accept(this, ctx));
        }
        return result;
    }

    /** Leaf node — no children to recurse into. */
    @Override
    public R visitVariable(final Expr.Variable expr, final C ctx) {
        return defaultResult();
    }

    /** Default traversal. Order: {@link Expr.Assign#target()}, {@link Expr.Assign#value()}. */
    @Override
    public R visitAssign(final Expr.Assign expr, final C ctx) {
        R result = expr.target().accept(this, ctx);
        result = aggregateResult(result, expr.value().accept(this, ctx));
        return result;
    }

    /** Default traversal. Order: {@link Expr.Call#callee()}, then each argument left to right. */
    @Override
    public R visitCall(final Expr.Call expr, final C ctx) {
        R result = expr.callee().accept(this, ctx);
        for (final Expr arg : expr.arguments()) {
            result = aggregateResult(result, arg.accept(this, ctx));
        }
        return result;
    }

    /**
     * Default traversal. Order: {@link Expr.ArrayAccess#array()}, {@link Expr.ArrayAccess#index()}.
     */
    @Override
    public R visitArrayAccess(final Expr.ArrayAccess expr, final C ctx) {
        R result = expr.array().accept(this, ctx);
        result = aggregateResult(result, expr.index().accept(this, ctx));
        return result;
    }

    // -------------------------------------------------------------------------
    // StmtVisitor implementation
    // -------------------------------------------------------------------------

    /** Default traversal. Order: {@link Stmt.Expression#expr()}. */
    @Override
    public R visitExpression(final Stmt.Expression stmt, final C ctx) {
        return stmt.expr().accept(this, ctx);
    }

    /** Default traversal. Order: type annotation, then each initializer left to right. */
    @Override
    public R visitVarDeclaration(final Stmt.VarDeclaration stmt, final C ctx) {
        R result = stmt.typeAnnotation().accept(this, ctx);
        for (final Stmt.VarBinding binding : stmt.bindings()) {
            final Optional<Expr> initializer = binding.initializer();
            if (initializer.isPresent()) {
                result = aggregateResult(result, initializer.get().accept(this, ctx));
            }
        }
        return result;
    }

    /**
     * Default traversal. Order: return type, each parameter's type annotation, then each body
     * statement.
     */
    @Override
    public R visitFunction(final Stmt.Function stmt, final C ctx) {
        R result = stmt.returnType().accept(this, ctx);
        for (final Parameter param : stmt.parameters()) {
            result = aggregateResult(result, param.typeAnnotation().accept(this, ctx));
        }
        result = aggregateResult(result, stmt.body().accept(this, ctx));
        return result;
    }

    /**
     * Default traversal. Order: condition, then-branch statements, else-branch statements (if
     * present).
     */
    @Override
    public R visitIf(final Stmt.If stmt, final C ctx) {
        R result = stmt.condition().accept(this, ctx);
        for (final Stmt s : stmt.thenBranch()) {
            result = aggregateResult(result, s.accept(this, ctx));
        }
        for (final Stmt s : stmt.elseBranch().orElse(List.of())) {
            result = aggregateResult(result, s.accept(this, ctx));
        }
        return result;
    }

    /** Default traversal. Order: condition, body statements. */
    @Override
    public R visitWhile(final Stmt.While stmt, final C ctx) {
        R result = stmt.condition().accept(this, ctx);
        result = aggregateResult(result, stmt.body().accept(this, ctx));
        return result;
    }

    /**
     * Default traversal. Order: initializer (if present), condition (if present), increment (if
     * present), body statements.
     */
    @Override
    public R visitFor(final Stmt.For stmt, final C ctx) {
        R result = defaultResult();
        if (stmt.initializer().isPresent()) {
            result = aggregateResult(result, stmt.initializer().get().accept(this, ctx));
        }
        if (stmt.condition().isPresent()) {
            result = aggregateResult(result, stmt.condition().get().accept(this, ctx));
        }
        if (stmt.increment().isPresent()) {
            result = aggregateResult(result, stmt.increment().get().accept(this, ctx));
        }
        result = aggregateResult(result, stmt.body().accept(this, ctx));
        return result;
    }

    /** Default traversal. Order: each statement in the block, left to right. */
    @Override
    public R visitBlock(final Stmt.Block stmt, final C ctx) {
        R result = defaultResult();
        for (final Stmt s : stmt.statements()) {
            result = aggregateResult(result, s.accept(this, ctx));
        }
        return result;
    }

    /** Default traversal. Order: return value expression (if present). */
    @Override
    public R visitReturn(final Stmt.Return stmt, final C ctx) {
        return stmt.value().map(expr -> expr.accept(this, ctx)).orElseGet(this::defaultResult);
    }

    /** Leaf node — no children to recurse into. */
    @Override
    public R visitBreak(final Stmt.Break stmt, final C ctx) {
        return defaultResult();
    }

    /** Leaf node — no children to recurse into. */
    @Override
    public R visitContinue(final Stmt.Continue stmt, final C ctx) {
        return defaultResult();
    }

    /** Default traversal. Order: each body statement left to right. */
    @Override
    public R visitMainFunction(final Stmt.MainFunction stmt, final C ctx) {
        R result = defaultResult();
        for (final Stmt s : stmt.body()) {
            result = aggregateResult(result, s.accept(this, ctx));
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // TypeVisitor implementation — all primitives are leaves
    // -------------------------------------------------------------------------

    @Override
    public R visitI8(final Type.I8 t, final C ctx) {
        return defaultResult();
    }

    @Override
    public R visitI16(final Type.I16 t, final C ctx) {
        return defaultResult();
    }

    @Override
    public R visitI32(final Type.I32 t, final C ctx) {
        return defaultResult();
    }

    @Override
    public R visitI64(final Type.I64 t, final C ctx) {
        return defaultResult();
    }

    @Override
    public R visitU8(final Type.U8 t, final C ctx) {
        return defaultResult();
    }

    @Override
    public R visitU16(final Type.U16 t, final C ctx) {
        return defaultResult();
    }

    @Override
    public R visitU32(final Type.U32 t, final C ctx) {
        return defaultResult();
    }

    @Override
    public R visitU64(final Type.U64 t, final C ctx) {
        return defaultResult();
    }

    @Override
    public R visitF32(final Type.F32 t, final C ctx) {
        return defaultResult();
    }

    @Override
    public R visitF64(final Type.F64 t, final C ctx) {
        return defaultResult();
    }

    @Override
    public R visitChar(final Type.Char t, final C ctx) {
        return defaultResult();
    }

    @Override
    public R visitStringT(final Type.StringT t, final C ctx) {
        return defaultResult();
    }

    @Override
    public R visitBool(final Type.Bool t, final C ctx) {
        return defaultResult();
    }

    @Override
    public R visitCustom(final Type.Custom t, final C ctx) {
        return defaultResult();
    }

    @Override
    public R visitVoidT(final Type.VoidT t, final C ctx) {
        return defaultResult();
    }

    @Override
    public R visitNullPtr(final Type.NullPtr t, final C ctx) {
        return defaultResult();
    }

    /**
     * Default traversal for {@link Type.Array}: recurse into element type and size expression.
     * Order: {@link Type.Array#elementType()}, {@link Type.Array#size()}.
     */
    @Override
    public R visitArray(final Type.Array type, final C ctx) {
        R result = type.elementType().accept(this, ctx);
        result = aggregateResult(result, type.size().accept(this, ctx));
        return result;
    }

    /** Default traversal for {@link Type.Vector}: recurse into element type. */
    @Override
    public R visitVector(final Type.Vector type, final C ctx) {
        return type.elementType().accept(this, ctx);
    }

    // -------------------------------------------------------------------------
    // LiteralValueVisitor implementation — all are leaves
    // -------------------------------------------------------------------------

    @Override
    public R visitNumeric(final LiteralValue.Numeric value, final C ctx) {
        return defaultResult();
    }

    @Override
    public R visitStringLit(final LiteralValue.StringLit value, final C ctx) {
        return defaultResult();
    }

    @Override
    public R visitCharLit(final LiteralValue.CharLit value, final C ctx) {
        return defaultResult();
    }

    @Override
    public R visitBool(final LiteralValue.Bool value, final C ctx) {
        return defaultResult();
    }

    @Override
    public R visitNullPtr(final LiteralValue.NullPtr value, final C ctx) {
        return defaultResult();
    }
}
