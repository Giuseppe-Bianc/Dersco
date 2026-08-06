package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** An expression that applies a binary operator to two operands. */
public record BinaryExpr(
        BinaryOperator operator,
        Expr left,
        Expr right,
        Span range,
        ExpressionAnnotations annotations)
        implements Expr {

    /** Creates a binary expression without semantic annotations. */
    public BinaryExpr(
            final BinaryOperator operator, final Expr left, final Expr right, final Span range) {
        this(operator, left, right, range, new ExpressionAnnotations());
    }

    /** Validates the binary expression. */
    public BinaryExpr {
        operator = AstValidation.required(operator, "operator");
        left = AstValidation.required(left, "left");
        right = AstValidation.required(right, "right");
        range = AstValidation.range(range);
        annotations = AstValidation.required(annotations, "annotations");
    }

    @Override
    public NodeKind kind() {
        return NodeKind.BINARY_EXPR;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitBinaryExpr(this);
    }
}
