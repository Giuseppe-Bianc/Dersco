package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** A loop with optional initialization, condition, and update clauses. */
public record ForStmt(Stmt init, Expr condition, Expr update, Stmt body, Span range)
        implements Stmt {

    /** Validates the mandatory loop body and source range. */
    public ForStmt {
        body = AstValidation.required(body, "body");
        range = AstValidation.range(range);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.FOR_STMT;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitForStmt(this);
    }
}
