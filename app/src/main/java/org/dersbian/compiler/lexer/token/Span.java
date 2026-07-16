package org.dersbian.compiler.lexer.token;

import java.util.Objects;

/**
 * Extent of a token in the source text.
 *
 * @param start start position (inclusive).
 * @param end end position (exclusive).
 */
public record Span(SourceLocation start, SourceLocation end) {

    /** Compact constructor that validates start/end ordering. */
    public Span {
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");
        if (end.offset() < start.offset()) {
            throw new IllegalArgumentException(
                    "end offset (%d) must not precede start offset (%d)"
                            .formatted(end.offset(), start.offset()));
        }
    }

    /**
     * Creates a new span.
     *
     * @param start start position
     * @param end end position
     * @return the new span
     */
    public static Span create(final SourceLocation start, final SourceLocation end) {
        return new Span(start, end);
    }

    /** Creates a zero-length span at the given location. */
    public static Span point(final SourceLocation location) {
        Objects.requireNonNull(location, "location must not be null");
        return new Span(location, location);
    }

    /** Length in UTF-16 code units (consistent with {@link String#length()}). */
    public long length() {
        return end.offset() - start.offset();
    }

    /**
     * Verifica se lo span ha lunghezza zero.
     *
     * @return {@code true} if empty
     */
    public boolean isEmpty() {
        return length() == 0L;
    }

    /**
     * Verifica se lo span si estende su più righe.
     *
     * @return {@code true} if multiline
     */
    public boolean isMultiline() {
        return start.line() != end.line();
    }

    /** Verifica se la posizione ricade nell'intervallo [start, end). */
    public boolean contains(final SourceLocation location) {
        Objects.requireNonNull(location, "location must not be null");
        return location.offset() >= start.offset() && location.offset() < end.offset();
    }

    /** Verifica se due span condividono almeno un carattere. */
    public boolean overlaps(final Span other) {
        Objects.requireNonNull(other, "other must not be null");
        return this.start.offset() < other.end.offset() && other.start.offset() < this.end.offset();
    }

    /** Restituisce lo span minimo che contiene entrambi gli span. */
    public Span merge(final Span other) {
        Objects.requireNonNull(other, "other must not be null");
        final SourceLocation mergedStart =
                this.start.offset() <= other.start.offset() ? this.start : other.start;
        final SourceLocation mergedEnd =
                this.end.offset() >= other.end.offset() ? this.end : other.end;
        return new Span(mergedStart, mergedEnd);
    }

    /** Estrae il testo dallo span. */
    public String extractFrom(final CharSequence source) {
        Objects.requireNonNull(source, "source must not be null");
        return source.subSequence(Math.toIntExact(start.offset()), Math.toIntExact(end.offset()))
                .toString();
    }

    @Override
    public String toString() {
        final String result;
        if (this.isEmpty()) {
            result = start.toString();
        } else {
            result = "%s-%s".formatted(start, end);
        }
        return result;
    }
}
