package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** An integer literal expression. */
public record IntLiteral(long value, Span range, ExpressionAnnotations annotations)
        implements Expr {

    /** Creates an integer literal without semantic annotations. */
    public IntLiteral(final long value, final Span range) {
        this(value, range, new ExpressionAnnotations());
    }

    /** Validates the literal expression. */
    public IntLiteral {
        range = AstValidation.range(range);
        annotations = AstValidation.required(annotations, "annotations");
    }

    @Override
    public NodeKind kind() {
        return NodeKind.INT_LITERAL;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitIntLiteral(this);
    }
}
