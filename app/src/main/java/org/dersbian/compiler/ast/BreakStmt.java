package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** A statement that exits the innermost enclosing loop. */
public record BreakStmt(Span range) implements Stmt {

    /** Validates the mandatory source range. */
    public BreakStmt {
        range = AstValidation.range(range);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.BREAK_STMT;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitBreakStmt(this);
    }
}
