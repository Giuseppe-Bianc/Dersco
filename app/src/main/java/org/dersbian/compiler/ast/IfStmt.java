package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** A conditional statement with an optional alternative branch. */
public record IfStmt(Expr condition, Stmt thenBranch, Stmt elseBranch, Span range) implements Stmt {

    /** Validates the mandatory parts of the conditional statement. */
    public IfStmt {
        condition = AstValidation.required(condition, "condition");
        thenBranch = AstValidation.required(thenBranch, "thenBranch");
        range = AstValidation.range(range);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.IF_STMT;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitIfStmt(this);
    }
}
