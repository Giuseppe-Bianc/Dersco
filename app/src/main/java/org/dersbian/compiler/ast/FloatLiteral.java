package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** A floating-point literal expression. */
public record FloatLiteral(double value, Span range, ExpressionAnnotations annotations)
        implements Expr {

    /** Creates a floating-point literal without semantic annotations. */
    public FloatLiteral(final double value, final Span range) {
        this(value, range, new ExpressionAnnotations());
    }

    /** Validates the literal expression. */
    public FloatLiteral {
        range = AstValidation.range(range);
        annotations = AstValidation.required(annotations, "annotations");
    }

    @Override
    public NodeKind kind() {
        return NodeKind.FLOAT_LITERAL;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitFloatLiteral(this);
    }
}
