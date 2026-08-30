package org.dersbian.compiler.semantics.symbol;

/** Stable positive identifier of a lexical scope. */
public record ScopeId(long value) {
    /** Minimum valid identifier value (exclusive lower bound for the positivity check). */
    private static final long MIN_VALID = 0L;

    /**
     * Validates that {@code value} is strictly positive.
     *
     * @throws IllegalArgumentException if {@code value} is zero or negative
     */
    public ScopeId {
        if (value <= MIN_VALID) {
            throw new IllegalArgumentException("scope id must be positive");
        }
    }
}
