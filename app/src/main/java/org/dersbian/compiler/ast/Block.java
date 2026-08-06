package org.dersbian.compiler.ast;

import java.util.List;
import org.dersbian.compiler.lexer.token.Span;

/** A sequence of statements sharing one lexical scope. */
public record Block(List<Stmt> statements, Span range) implements Stmt {

    /** Validates and defensively copies the statements. */
    public Block {
        statements = AstValidation.list(statements, "statements");
        range = AstValidation.range(range);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.BLOCK;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitBlock(this);
    }
}
