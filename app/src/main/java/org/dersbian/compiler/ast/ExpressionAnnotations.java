package org.dersbian.compiler.ast;

import java.util.Optional;

/** Mutable semantic facts attached to an otherwise immutable expression node. */
@SuppressWarnings("PMD.LongVariable")
public final class ExpressionAnnotations {

    /** Lazily assigned semantic type from type checking; {@code null} until set. */
    private SemanticType cachedResolvedType;

    /** Lazily assigned symbol from name resolution; {@code null} until set. */
    private Symbol cachedSymbol;

    /** Creates a new, empty set of expression annotations. */
    public ExpressionAnnotations() {
        // All annotations start as absent and are populated during semantic analysis.
    }

    /** Returns the semantic type known after type checking, if any. */
    public Optional<SemanticType> resolvedType() {
        return Optional.ofNullable(cachedResolvedType);
    }

    /** Records the semantic type assigned by type checking. */
    public void setResolvedType(final SemanticType type) {
        cachedResolvedType = AstValidation.required(type, "type");
    }

    /** Returns the symbol resolved for a name expression, if any. */
    public Optional<Symbol> symbol() {
        return Optional.ofNullable(cachedSymbol);
    }

    /** Records the symbol resolved for a name expression. */
    public void setSymbol(final Symbol resolvedSymbol) {
        cachedSymbol = AstValidation.required(resolvedSymbol, "resolvedSymbol");
    }
}
