package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** A string literal expression. */
public record StringLiteral(String value, Span range, ExpressionAnnotations annotations)
        implements Expr {

    /** Creates a string literal without semantic annotations. */
    public StringLiteral(final String value, final Span range) {
        this(value, range, new ExpressionAnnotations());
    }

    /** Validates the literal expression. */
    public StringLiteral {
        value = AstValidation.required(value, "value");
        range = AstValidation.range(range);
        annotations = AstValidation.required(annotations, "annotations");
    }

    @Override
    public NodeKind kind() {
        return NodeKind.STRING_LITERAL;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitStringLiteral(this);
    }
}
