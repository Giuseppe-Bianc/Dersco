package org.dersbian.compiler.semantics.symbol;

import java.util.Objects;
import java.util.Optional;

/** Immutable public snapshot describing a lexical scope. */
@SuppressWarnings({"PMD.ShortVariable"})
public record Scope(
        ScopeId id,
        ScopeKind kind,
        Optional<ScopeId> parentId,
        int depth,
        Optional<SymbolId> ownerSymbolId) {
    /**
     * Validates the scope snapshot.
     *
     * @throws NullPointerException if any reference component is {@code null}
     * @throws IllegalArgumentException if {@code depth} is negative, if a global scope has a
     *     parent, owner, or non-zero depth, if a non-global scope lacks a parent, if a function
     *     scope lacks an owner symbol, or if a non-function scope has an owner symbol
     */
    public Scope {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(parentId, "parentId must not be null");
        Objects.requireNonNull(ownerSymbolId, "ownerSymbolId must not be null");
        if (depth < 0) {
            throw new IllegalArgumentException("depth must be non-negative");
        }
        if (kind == ScopeKind.GLOBAL) {
            if (parentId.isPresent() || ownerSymbolId.isPresent() || depth != 0) {
                throw new IllegalArgumentException(
                        "global scope must be root-owned and depth zero");
            }
        } else if (parentId.isEmpty()) {
            throw new IllegalArgumentException("non-global scope must have a parent");
        }
        if (kind == ScopeKind.FUNCTION && ownerSymbolId.isEmpty()) {
            throw new IllegalArgumentException("function scope must have an owner symbol");
        }
        if (kind != ScopeKind.FUNCTION && ownerSymbolId.isPresent()) {
            throw new IllegalArgumentException("only function scopes may have an owner symbol");
        }
    }
}
