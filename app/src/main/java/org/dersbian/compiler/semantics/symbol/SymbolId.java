package org.dersbian.compiler.semantics.symbol;

/** Stable identifier assigned to a declared symbol. */
public record SymbolId(long value) {
    public SymbolId {
        if (value < 0L) {
            throw new IllegalArgumentException("symbol id must be non-negative");
        }
    }
}
