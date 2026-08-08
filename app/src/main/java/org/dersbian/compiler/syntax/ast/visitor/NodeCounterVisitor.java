package org.dersbian.compiler.syntax.ast.visitor;

import org.dersbian.compiler.syntax.ast.Expr;
import org.dersbian.compiler.syntax.ast.LiteralValue;
import org.dersbian.compiler.syntax.ast.Stmt;
import org.dersbian.compiler.syntax.ast.Type;

/**
 * A concrete visitor that counts the total number of AST nodes visited.
 *
 * <p>Demonstrates use of {@link AbstractAstVisitor} with {@link Integer} as result type and {@code
 * aggregateResult} as additive combiner. No context is needed.
 *
 * <p>Every visit method overrides the default to return {@code 1} (this node) plus the sum of all
 * children (via super), implementing a full node count.
 */
@SuppressWarnings({
    "PMD.ShortVariable",
    "PMD.TooManyMethods",
    "PMD.CouplingBetweenObjects",
    "checkstyle:OverloadMethodsDeclarationOrder"
})
public final class NodeCounterVisitor extends AbstractAstVisitor<Integer, Void> {

    /** Creates a node counter visitor. */
    public NodeCounterVisitor() {
        super();
    }

    @Override
    protected Integer defaultResult() {
        return 0;
    }

    /**
     * Combines counts additively: the total is the sum of all individual node counts.
     *
     * @param aggregate count accumulated so far
     * @param nextResult count from the next child
     * @return sum of the two counts
     */
    @Override
    protected Integer aggregateResult(final Integer aggregate, final Integer nextResult) {
        return aggregate + nextResult;
    }

    // -------------------------------------------------------------------------
    // Expr — each method adds 1 for the current node, then recurses via super
    // -------------------------------------------------------------------------

    @Override
    public Integer visitBinary(final Expr.Binary e, final Void ctx) {
        return 1 + super.visitBinary(e, null);
    }

    @Override
    public Integer visitUnary(final Expr.Unary e, final Void ctx) {
        return 1 + super.visitUnary(e, null);
    }

    @Override
    public Integer visitGrouping(final Expr.Grouping e, final Void ctx) {
        return 1 + super.visitGrouping(e, null);
    }

    @Override
    public Integer visitLiteral(final Expr.Literal e, final Void ctx) {
        return 1 + super.visitLiteral(e, null);
    }

    @Override
    public Integer visitArrayLiteral(final Expr.ArrayLiteral e, final Void ctx) {
        return 1 + super.visitArrayLiteral(e, null);
    }

    @Override
    public Integer visitVariable(final Expr.Variable e, final Void ctx) {
        return 1;
    }

    @Override
    public Integer visitAssign(final Expr.Assign e, final Void ctx) {
        return 1 + super.visitAssign(e, null);
    }

    @Override
    public Integer visitCall(final Expr.Call e, final Void ctx) {
        return 1 + super.visitCall(e, null);
    }

    @Override
    public Integer visitArrayAccess(final Expr.ArrayAccess e, final Void ctx) {
        return 1 + super.visitArrayAccess(e, null);
    }

    // -------------------------------------------------------------------------
    // Stmt
    // -------------------------------------------------------------------------

    @Override
    public Integer visitExpression(final Stmt.Expression s, final Void ctx) {
        return 1 + super.visitExpression(s, null);
    }

    @Override
    public Integer visitVarDeclaration(final Stmt.VarDeclaration s, final Void ctx) {
        return 1 + super.visitVarDeclaration(s, null);
    }

    @Override
    public Integer visitFunction(final Stmt.Function s, final Void ctx) {
        return 1 + super.visitFunction(s, null);
    }

    @Override
    public Integer visitIf(final Stmt.If s, final Void ctx) {
        return 1 + super.visitIf(s, null);
    }

    @Override
    public Integer visitWhile(final Stmt.While s, final Void ctx) {
        return 1 + super.visitWhile(s, null);
    }

    @Override
    public Integer visitFor(final Stmt.For s, final Void ctx) {
        return 1 + super.visitFor(s, null);
    }

    @Override
    public Integer visitBlock(final Stmt.Block s, final Void ctx) {
        return 1 + super.visitBlock(s, null);
    }

    @Override
    public Integer visitReturn(final Stmt.Return s, final Void ctx) {
        return 1 + super.visitReturn(s, null);
    }

    @Override
    public Integer visitBreak(final Stmt.Break s, final Void ctx) {
        return 1;
    }

    @Override
    public Integer visitContinue(final Stmt.Continue s, final Void ctx) {
        return 1;
    }

    @Override
    public Integer visitMainFunction(final Stmt.MainFunction s, final Void ctx) {
        return 1 + super.visitMainFunction(s, null);
    }

    // -------------------------------------------------------------------------
    // LiteralValue — all leaves, no children
    // -------------------------------------------------------------------------

    @Override
    public Integer visitNumeric(final LiteralValue.Numeric v, final Void ctx) {
        return 1;
    }

    @Override
    public Integer visitStringLit(final LiteralValue.StringLit v, final Void ctx) {
        return 1;
    }

    @Override
    public Integer visitCharLit(final LiteralValue.CharLit v, final Void ctx) {
        return 1;
    }

    @Override
    public Integer visitBool(final LiteralValue.Bool v, final Void ctx) {
        return 1;
    }

    @Override
    public Integer visitNullPtr(final LiteralValue.NullPtr v, final Void ctx) {
        return 1;
    }

    // -------------------------------------------------------------------------
    // Type — primitives are leaves, Array and Vector recurse
    // -------------------------------------------------------------------------

    @Override
    public Integer visitI8(final Type.I8 t, final Void ctx) {
        return 1;
    }

    @Override
    public Integer visitI16(final Type.I16 t, final Void ctx) {
        return 1;
    }

    @Override
    public Integer visitI32(final Type.I32 t, final Void ctx) {
        return 1;
    }

    @Override
    public Integer visitI64(final Type.I64 t, final Void ctx) {
        return 1;
    }

    @Override
    public Integer visitU8(final Type.U8 t, final Void ctx) {
        return 1;
    }

    @Override
    public Integer visitU16(final Type.U16 t, final Void ctx) {
        return 1;
    }

    @Override
    public Integer visitU32(final Type.U32 t, final Void ctx) {
        return 1;
    }

    @Override
    public Integer visitU64(final Type.U64 t, final Void ctx) {
        return 1;
    }

    @Override
    public Integer visitF32(final Type.F32 t, final Void ctx) {
        return 1;
    }

    @Override
    public Integer visitF64(final Type.F64 t, final Void ctx) {
        return 1;
    }

    @Override
    public Integer visitChar(final Type.Char t, final Void ctx) {
        return 1;
    }

    @Override
    public Integer visitStringT(final Type.StringT t, final Void ctx) {
        return 1;
    }

    @Override
    public Integer visitBool(final Type.Bool t, final Void ctx) {
        return 1;
    }

    @Override
    public Integer visitCustom(final Type.Custom t, final Void ctx) {
        return 1;
    }

    @Override
    public Integer visitVoidT(final Type.VoidT t, final Void ctx) {
        return 1;
    }

    @Override
    public Integer visitNullPtr(final Type.NullPtr t, final Void ctx) {
        return 1;
    }

    @Override
    public Integer visitArray(final Type.Array t, final Void ctx) {
        return 1 + super.visitArray(t, null);
    }

    @Override
    public Integer visitVector(final Type.Vector t, final Void ctx) {
        return 1 + super.visitVector(t, null);
    }
}
