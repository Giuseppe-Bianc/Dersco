package org.dersbian.compiler.semantics.symbol;

/** Stable identity of a symbol within one symbol table. */
public record SymbolId(long value) {
    /** Validates the symbol identifier. */
    public SymbolId {
        if (value <= 0) {
            throw new IllegalArgumentException("symbol id must be greater than zero");
        }
    }
}
