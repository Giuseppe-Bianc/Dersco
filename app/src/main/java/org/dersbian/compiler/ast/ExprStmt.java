package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** An expression evaluated for its side effects. */
public record ExprStmt(Expr expr, Span range) implements Stmt {

    /** Validates the expression statement. */
    public ExprStmt {
        expr = AstValidation.required(expr, "expr");
        range = AstValidation.range(range);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.EXPR_STMT;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitExprStmt(this);
    }
}
