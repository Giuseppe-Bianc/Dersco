package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** A source-level element in the abstract syntax tree. */
@SuppressWarnings("PMD.ShortClassName")
public sealed interface Node permits Program, Decl, Stmt, Expr, TypeNode {

    /** Returns the mandatory source range occupied by this node. */
    Span range();

    /** Returns the stable kind of this node. */
    NodeKind kind();

    /** Dispatches to the visitor method for this concrete node. */
    <R> R accept(AstVisitor<R> visitor);
}
