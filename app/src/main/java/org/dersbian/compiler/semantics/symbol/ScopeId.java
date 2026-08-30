package org.dersbian.compiler.semantics.symbol;

/** Stable identity of a scope within one symbol table. */
public record ScopeId(long value) {
    /** Validates the scope identifier. */
    public ScopeId {
        if (value <= 0) {
            throw new IllegalArgumentException("scope id must be greater than zero");
        }
    }
}
