package org.dersbian.compiler.ast;

import java.util.Optional;
import org.dersbian.compiler.lexer.token.Span;

/** A reference to a name in source code. */
public record NameExpr(String name, Span range, ExpressionAnnotations annotations) implements Expr {

    /** Creates an unresolved name expression. */
    public NameExpr(final String name, final Span range) {
        this(name, range, new ExpressionAnnotations());
    }

    /** Validates the name expression. */
    public NameExpr {
        name = AstValidation.name(name);
        range = AstValidation.range(range);
        annotations = AstValidation.required(annotations, "annotations");
    }

    /** Returns the declaration resolved for this name, if one is available. */
    public Optional<Symbol> symbol() {
        return annotations.symbol();
    }

    /** Records the declaration resolved for this name. */
    public void setSymbol(final Symbol symbol) {
        annotations.setSymbol(symbol);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.NAME_EXPR;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitNameExpr(this);
    }
}
