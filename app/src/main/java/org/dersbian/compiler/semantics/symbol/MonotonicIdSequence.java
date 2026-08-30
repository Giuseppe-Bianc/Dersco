package org.dersbian.compiler.semantics.symbol;

/** Internal monotonic sequence generator for positive identifiers. */
final class MonotonicIdSequence {
    private long nextValue = 1L;

    long next() {
        if (nextValue <= 0L) {
            throw new IllegalStateException("identifier sequence is exhausted");
        }
        final long value = nextValue;
        if (value == Long.MAX_VALUE) {
            nextValue = -1L;
        } else {
            nextValue = value + 1L;
        }
        return value;
    }
}
