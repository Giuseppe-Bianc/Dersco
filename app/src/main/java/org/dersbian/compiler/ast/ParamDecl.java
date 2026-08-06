package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** A typed function parameter. */
public record ParamDecl(String name, TypeNode type, Span range) implements Decl {

    /** Validates the parameter declaration. */
    public ParamDecl {
        name = AstValidation.name(name);
        type = AstValidation.required(type, "type");
        range = AstValidation.range(range);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.PARAM_DECL;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitParamDecl(this);
    }
}
