package org.dersbian.compiler.ast;

import java.util.Optional;

/** An expression, optionally annotated during semantic analysis. */
@SuppressWarnings("PMD.ShortClassName")
public sealed interface Expr extends Node
        permits IntLiteral,
                FloatLiteral,
                BoolLiteral,
                StringLiteral,
                NullLiteral,
                NameExpr,
                BinaryExpr,
                UnaryExpr,
                AssignExpr,
                CallExpr,
                MemberAccessExpr,
                IndexExpr,
                NewExpr,
                CastExpr {

    /** Returns the mutable semantic facts attached to this expression. */
    ExpressionAnnotations annotations();

    /** Returns the type resolved for this expression, if semantic analysis has run. */
    default Optional<SemanticType> resolvedType() {
        return annotations().resolvedType();
    }

    /** Records the type resolved for this expression. */
    default void setResolvedType(final SemanticType type) {
        annotations().setResolvedType(type);
    }
}
