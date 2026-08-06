package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** An explicit conversion to a source-level type. */
public record CastExpr(
        TypeNode targetType, Expr operand, Span range, ExpressionAnnotations annotations)
        implements Expr {

    /** Creates a cast expression without semantic annotations. */
    public CastExpr(final TypeNode targetType, final Expr operand, final Span range) {
        this(targetType, operand, range, new ExpressionAnnotations());
    }

    /** Validates the cast expression. */
    public CastExpr {
        targetType = AstValidation.required(targetType, "targetType");
        operand = AstValidation.required(operand, "operand");
        range = AstValidation.range(range);
        annotations = AstValidation.required(annotations, "annotations");
    }

    @Override
    public NodeKind kind() {
        return NodeKind.CAST_EXPR;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitCastExpr(this);
    }
}
