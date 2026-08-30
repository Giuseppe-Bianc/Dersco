package org.dersbian.compiler.semantics.symbol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;

/** Tests the positive, monotonic and overflow-safe identifier sequence. */
class MonotonicIdSequenceTest {
    @Test
    void sequenceStartsAtOneAndIncreases() {
        final MonotonicIdSequence sequence = new MonotonicIdSequence();

        assertThat(sequence.next()).isEqualTo(1L);
        assertThat(sequence.next()).isEqualTo(2L);
        assertThat(sequence.next()).isEqualTo(3L);
    }

    @Test
    void maxValueCanBeAssignedExactlyOnce() throws Exception {
        final MonotonicIdSequence sequence = new MonotonicIdSequence();
        final Field nextValue = MonotonicIdSequence.class.getDeclaredField("nextValue");
        nextValue.setAccessible(true);
        nextValue.setLong(sequence, Long.MAX_VALUE);

        assertThat(sequence.next()).isEqualTo(Long.MAX_VALUE);
        assertThatThrownBy(sequence::next)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("identifier sequence is exhausted");
    }

    @Test
    void exhaustedSequenceNeverReturnsNonPositiveValue() throws Exception {
        final MonotonicIdSequence sequence = new MonotonicIdSequence();
        final Field nextValue = MonotonicIdSequence.class.getDeclaredField("nextValue");
        nextValue.setAccessible(true);
        nextValue.setLong(sequence, -1L);

        assertThatThrownBy(sequence::next)
                .isInstanceOf(IllegalStateException.class);
    }
}
