package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** A Boolean literal expression. */
public record BoolLiteral(boolean value, Span range, ExpressionAnnotations annotations)
        implements Expr {

    /** Creates a Boolean literal without semantic annotations. */
    public BoolLiteral(final boolean value, final Span range) {
        this(value, range, new ExpressionAnnotations());
    }

    /** Validates the literal expression. */
    public BoolLiteral {
        range = AstValidation.range(range);
        annotations = AstValidation.required(annotations, "annotations");
    }

    @Override
    public NodeKind kind() {
        return NodeKind.BOOL_LITERAL;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitBoolLiteral(this);
    }
}
