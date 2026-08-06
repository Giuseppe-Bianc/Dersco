package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** A statement that starts the next iteration of the innermost loop. */
public record ContinueStmt(Span range) implements Stmt {

    /** Validates the mandatory source range. */
    public ContinueStmt {
        range = AstValidation.range(range);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.CONTINUE_STMT;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitContinueStmt(this);
    }
}
