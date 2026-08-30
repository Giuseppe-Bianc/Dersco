package org.dersbian.compiler.semantics.symbol;

import java.util.Objects;
import java.util.Optional;

/** Immutable structural description of a lexical scope. */
@SuppressWarnings("PMD.ShortVariable")
public record Scope(
        ScopeId id,
        ScopeKind kind,
        Optional<ScopeId> parentId,
        int depth,
        Optional<SymbolId> ownerSymbolId) {
    /** Validates the immutable scope snapshot. */
    public Scope {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(parentId, "parentId must not be null");
        Objects.requireNonNull(ownerSymbolId, "ownerSymbolId must not be null");
        if (depth < 0) {
            throw new IllegalArgumentException("depth must not be negative");
        }
        if (kind == ScopeKind.GLOBAL
                && (parentId.isPresent() || depth != 0 || ownerSymbolId.isPresent())) {
            throw new IllegalArgumentException(
                    "global scope must have no parent, depth zero, or owner");
        }
        if (kind != ScopeKind.GLOBAL && (parentId.isEmpty() || depth == 0)) {
            throw new IllegalArgumentException(
                    "non-global scope must have a parent and positive depth");
        }
    }
}
