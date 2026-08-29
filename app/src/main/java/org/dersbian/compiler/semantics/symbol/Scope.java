package org.dersbian.compiler.semantics.symbol;

import java.util.Objects;
import java.util.Optional;

/** Immutable public snapshot describing a lexical scope. */
public record Scope(
        ScopeId id,
        ScopeKind kind,
        Optional<ScopeId> parentId,
        int depth,
        Optional<SymbolId> ownerSymbolId) {
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
                throw new IllegalArgumentException("global scope must be root-owned and depth zero");
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
