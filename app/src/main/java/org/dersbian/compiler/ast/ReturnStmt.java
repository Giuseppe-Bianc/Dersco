package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** A function return, optionally carrying a value. */
public record ReturnStmt(Expr value, Span range) implements Stmt {

    /** Validates the mandatory source range. */
    public ReturnStmt {
        range = AstValidation.range(range);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.RETURN_STMT;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitReturnStmt(this);
    }
}
