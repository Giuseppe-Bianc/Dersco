package org.dersbian.compiler.ast;

import java.util.List;
import org.dersbian.compiler.lexer.token.Span;

/** A function or method invocation. */
public record CallExpr(Expr callee, List<Expr> args, Span range, ExpressionAnnotations annotations)
        implements Expr {

    /** Creates a call expression without semantic annotations. */
    public CallExpr(final Expr callee, final List<Expr> args, final Span range) {
        this(callee, args, range, new ExpressionAnnotations());
    }

    /** Validates and defensively copies the call expression. */
    public CallExpr {
        callee = AstValidation.required(callee, "callee");
        args = AstValidation.list(args, "args");
        range = AstValidation.range(range);
        annotations = AstValidation.required(annotations, "annotations");
    }

    @Override
    public NodeKind kind() {
        return NodeKind.CALL_EXPR;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitCallExpr(this);
    }
}
