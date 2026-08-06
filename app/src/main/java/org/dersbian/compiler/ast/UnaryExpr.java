package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** An expression that applies a unary operator to one operand. */
public record UnaryExpr(
        UnaryOperator operator, Expr operand, Span range, ExpressionAnnotations annotations)
        implements Expr {

    /** Creates a unary expression without semantic annotations. */
    public UnaryExpr(final UnaryOperator operator, final Expr operand, final Span range) {
        this(operator, operand, range, new ExpressionAnnotations());
    }

    /** Validates the unary expression. */
    public UnaryExpr {
        operator = AstValidation.required(operator, "operator");
        operand = AstValidation.required(operand, "operand");
        range = AstValidation.range(range);
        annotations = AstValidation.required(annotations, "annotations");
    }

    @Override
    public NodeKind kind() {
        return NodeKind.UNARY_EXPR;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitUnaryExpr(this);
    }
}
