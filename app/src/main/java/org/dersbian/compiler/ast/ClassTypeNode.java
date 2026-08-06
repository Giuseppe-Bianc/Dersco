package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** A type reference to a named class. */
public record ClassTypeNode(String name, Span range) implements TypeNode {

    /** Validates the class type node. */
    public ClassTypeNode {
        name = AstValidation.name(name);
        range = AstValidation.range(range);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.CLASS_TYPE_NODE;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitClassTypeNode(this);
    }
}
