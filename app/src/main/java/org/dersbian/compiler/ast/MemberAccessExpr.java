package org.dersbian.compiler.ast;

import org.dersbian.compiler.lexer.token.Span;

/** Access to a named member of an object. */
public record MemberAccessExpr(
        Expr object, String member, Span range, ExpressionAnnotations annotations) implements Expr {

    /** Creates a member-access expression without semantic annotations. */
    public MemberAccessExpr(final Expr object, final String member, final Span range) {
        this(object, member, range, new ExpressionAnnotations());
    }

    /** Validates the member-access expression. */
    public MemberAccessExpr {
        object = AstValidation.required(object, "object");
        member = AstValidation.name(member);
        range = AstValidation.range(range);
        annotations = AstValidation.required(annotations, "annotations");
    }

    @Override
    public NodeKind kind() {
        return NodeKind.MEMBER_ACCESS_EXPR;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitMemberAccessExpr(this);
    }
}
