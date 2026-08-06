package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** Access to an array element by index. */
public record IndexExpr(Expr array, Expr index, Span range, ExpressionAnnotations annotations)
        implements Expr {

    /** Creates an index expression without semantic annotations. */
    public IndexExpr(final Expr array, final Expr index, final Span range) {
        this(array, index, range, new ExpressionAnnotations());
    }

    /** Validates the index expression. */
    public IndexExpr {
        array = AstValidation.required(array, "array");
        index = AstValidation.required(index, "index");
        range = AstValidation.range(range);
        annotations = AstValidation.required(annotations, "annotations");
    }

    @Override
    public NodeKind kind() {
        return NodeKind.INDEX_EXPR;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitIndexExpr(this);
    }
}
