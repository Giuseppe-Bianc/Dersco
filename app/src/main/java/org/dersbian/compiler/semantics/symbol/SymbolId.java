package org.dersbian.compiler.semantics.symbol;

/** Stable positive identifier assigned to a declared symbol. */
public record SymbolId(long value) {
    /** Minimum valid identifier value (exclusive lower bound for the positivity check). */
    private static final long MIN_VALID = 0L;

    /**
     * Validates that {@code value} is strictly positive.
     *
     * @throws IllegalArgumentException if {@code value} is zero or negative
     */
    public SymbolId {
        if (value <= MIN_VALID) {
            throw new IllegalArgumentException("symbol id must be positive");
        }
    }
}
