package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** A loop that evaluates its condition before each iteration. */
public record WhileStmt(Expr condition, Stmt body, Span range) implements Stmt {

    /** Validates the loop statement. */
    public WhileStmt {
        condition = AstValidation.required(condition, "condition");
        body = AstValidation.required(body, "body");
        range = AstValidation.range(range);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.WHILE_STMT;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitWhileStmt(this);
    }
}
