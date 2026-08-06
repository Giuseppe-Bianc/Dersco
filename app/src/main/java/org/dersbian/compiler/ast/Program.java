package org.dersbian.compiler.ast;

import java.util.List;
import org.dersbian.compiler.lexer.token.Span;

/** The root of a compilation unit. */
public record Program(List<Decl> declarations, Span range) implements Node {

    /** Validates and defensively copies the root declarations. */
    public Program {
        declarations = AstValidation.list(declarations, "declarations");
        range = AstValidation.range(range);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.PROGRAM;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitProgram(this);
    }
}
