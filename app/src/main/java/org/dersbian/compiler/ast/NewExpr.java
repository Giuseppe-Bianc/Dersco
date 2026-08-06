package org.dersbian.compiler.ast;

import java.util.List;
import org.dersbian.compiler.lexer.token.Span;

/** Object or array construction. */
public record NewExpr(TypeNode type, List<Expr> args, Span range, ExpressionAnnotations annotations)
        implements Expr {

    /** Creates a construction expression without semantic annotations. */
    public NewExpr(final TypeNode type, final List<Expr> args, final Span range) {
        this(type, args, range, new ExpressionAnnotations());
    }

    /** Validates and defensively copies the construction expression. */
    public NewExpr {
        type = AstValidation.required(type, "type");
        args = AstValidation.list(args, "args");
        range = AstValidation.range(range);
        annotations = AstValidation.required(annotations, "annotations");
    }

    @Override
    public NodeKind kind() {
        return NodeKind.NEW_EXPR;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitNewExpr(this);
    }
}
