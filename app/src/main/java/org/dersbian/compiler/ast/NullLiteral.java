package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** A null literal expression. */
public record NullLiteral(Span range, ExpressionAnnotations annotations) implements Expr {

    /** Creates a null literal without semantic annotations. */
    public NullLiteral(final Span range) {
        this(range, new ExpressionAnnotations());
    }

    /** Validates the literal expression. */
    public NullLiteral {
        range = AstValidation.range(range);
        annotations = AstValidation.required(annotations, "annotations");
    }

    @Override
    public NodeKind kind() {
        return NodeKind.NULL_LITERAL;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitNullLiteral(this);
    }
}
