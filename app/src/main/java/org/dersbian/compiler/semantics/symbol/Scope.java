package org.dersbian.compiler.semantics.symbol;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Immutable public view of a lexical scope. */
public record Scope(ScopeId id, ScopeId parentId, ScopeKind kind, Map<String, Symbol> symbols) {
    public Scope {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(symbols, "symbols must not be null");
        if (kind == ScopeKind.GLOBAL && parentId != null) {
            throw new IllegalArgumentException("global scope cannot have a parent");
        }
        if (kind != ScopeKind.GLOBAL && parentId == null) {
            throw new IllegalArgumentException("non-global scope must have a parent");
        }
        symbols = Map.copyOf(symbols);
    }

    /** Looks up a binding declared directly in this scope. */
    public Optional<Symbol> lookupLocal(final String name) {
        Objects.requireNonNull(name, "name must not be null");
        return Optional.ofNullable(symbols.get(name));
    }
}
