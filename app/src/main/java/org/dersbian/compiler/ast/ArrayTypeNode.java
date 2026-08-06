package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** An array type written in source code. */
public record ArrayTypeNode(TypeNode elementType, Span range) implements TypeNode {

    /** Validates the array type node. */
    public ArrayTypeNode {
        elementType = AstValidation.required(elementType, "elementType");
        range = AstValidation.range(range);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.ARRAY_TYPE_NODE;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitArrayTypeNode(this);
    }
}
