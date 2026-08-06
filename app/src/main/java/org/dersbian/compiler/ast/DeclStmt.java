package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** A variable declaration used as a statement. */
public record DeclStmt(VariableDecl decl, Span range) implements Stmt {

    /** Validates the declaration statement. */
    public DeclStmt {
        decl = AstValidation.required(decl, "decl");
        range = AstValidation.range(range);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.DECL_STMT;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitDeclStmt(this);
    }
}
