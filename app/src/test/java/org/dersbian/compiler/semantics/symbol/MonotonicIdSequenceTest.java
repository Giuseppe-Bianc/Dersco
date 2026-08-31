package org.dersbian.compiler.semantics.symbol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** Tests the positive, monotonic and overflow-safe identifier sequence. */
@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.AtLeastOneConstructor"})
class MonotonicIdSequenceTest {
    @Test
    void sequenceStartsAtOneAndIncreases() {
        final MonotonicIdSequence sequence = new MonotonicIdSequence();

        assertThat(sequence.next()).isEqualTo(1L);
        assertThat(sequence.next()).isEqualTo(2L);
        assertThat(sequence.next()).isEqualTo(3L);
    }

    @Test
    void maxValueCanBeAssignedExactlyOnce() {
        final MonotonicIdSequence sequence = new MonotonicIdSequence(Long.MAX_VALUE);

        assertThat(sequence.next()).isEqualTo(Long.MAX_VALUE);
        assertThatThrownBy(sequence::next)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("identifier sequence is exhausted");
    }

    @Test
    void exhaustedSequenceNeverReturnsNonPositiveValue() {
        final MonotonicIdSequence sequence = new MonotonicIdSequence(-1L);

        assertThatThrownBy(sequence::next).isInstanceOf(IllegalStateException.class);
    }
}
