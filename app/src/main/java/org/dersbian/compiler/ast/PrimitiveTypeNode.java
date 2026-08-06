package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** A primitive type written in source code. */
public record PrimitiveTypeNode(PrimitiveTypeName name, Span range) implements TypeNode {

    /** Validates the primitive type node. */
    public PrimitiveTypeNode {
        name = AstValidation.required(name, "name");
        range = AstValidation.range(range);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.PRIMITIVE_TYPE_NODE;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitPrimitiveTypeNode(this);
    }
}
