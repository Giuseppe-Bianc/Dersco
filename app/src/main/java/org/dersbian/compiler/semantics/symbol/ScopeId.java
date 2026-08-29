package org.dersbian.compiler.semantics.symbol;

/** Stable identifier of a lexical scope. */
public record ScopeId(long value) {
    public ScopeId {
        if (value < 0L) {
            throw new IllegalArgumentException("scope id must be non-negative");
        }
    }
}
