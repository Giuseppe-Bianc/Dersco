package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** A local, field, or global variable declaration. */
public record VariableDecl(String name, TypeNode declaredType, Expr initializer, Span range)
        implements Decl {

    /** Validates the mandatory components of the declaration. */
    public VariableDecl {
        name = AstValidation.name(name);
        range = AstValidation.range(range);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.VARIABLE_DECL;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitVariableDecl(this);
    }
}
