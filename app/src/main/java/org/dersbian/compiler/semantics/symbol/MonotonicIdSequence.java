package org.dersbian.compiler.semantics.symbol;

/** Internal monotonic sequence generator for positive identifiers. */
@SuppressWarnings("PMD.LongVariable")
public final class MonotonicIdSequence {

    /** Threshold below which the sequence is considered exhausted. */
    private static final long EXHAUSTION_THRESHOLD = 0L;

    /** Sentinel value indicating the sequence has been exhausted. */
    private static final long EXHAUSTED_VALUE = -1L;

    /** Increment step for generating subsequent identifiers. */
    private static final long INCREMENT_STEP = 1L;

    /** Next ID to vend, or a non-positive value if exhausted. */
    private long nextValue;

    /** Creates a new monotonic identifier sequence starting at 1. */
    public MonotonicIdSequence() {
        this(1L);
    }

    /**
     * Creates a new monotonic identifier sequence starting at the specified value.
     *
     * @param startValue the initial identifier value
     */
    public MonotonicIdSequence(final long startValue) {
        this.nextValue = startValue;
    }

    /**
     * Returns the next positive identifier in sequence.
     *
     * @return the next identifier
     * @throws IllegalStateException if the identifier sequence is exhausted
     */
    public long next() {
        if (nextValue <= EXHAUSTION_THRESHOLD) {
            throw new IllegalStateException("identifier sequence is exhausted");
        }
        final long value = nextValue;
        if (value == Long.MAX_VALUE) {
            nextValue = EXHAUSTED_VALUE;
        } else {
            nextValue = value + INCREMENT_STEP;
        }
        return value;
    }
}
