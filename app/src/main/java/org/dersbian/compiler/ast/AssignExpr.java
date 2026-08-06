package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** An assignment expression whose target is validated during semantic analysis. */
public record AssignExpr(Expr target, Expr value, Span range, ExpressionAnnotations annotations)
        implements Expr {

    /** Creates an assignment expression without semantic annotations. */
    public AssignExpr(final Expr target, final Expr value, final Span range) {
        this(target, value, range, new ExpressionAnnotations());
    }

    /** Validates the assignment expression. */
    public AssignExpr {
        target = AstValidation.required(target, "target");
        value = AstValidation.required(value, "value");
        range = AstValidation.range(range);
        annotations = AstValidation.required(annotations, "annotations");
    }

    @Override
    public NodeKind kind() {
        return NodeKind.ASSIGN_EXPR;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitAssignExpr(this);
    }
}
