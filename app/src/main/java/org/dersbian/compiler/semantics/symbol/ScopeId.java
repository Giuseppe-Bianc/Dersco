package org.dersbian.compiler.semantics.symbol;

/** Stable positive identifier of a lexical scope. */
public record ScopeId(long value) {
    public ScopeId {
        if (value <= 0L) {
            throw new IllegalArgumentException("scope id must be positive");
        }
    }
}
