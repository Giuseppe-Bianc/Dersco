package org.dersbian.compiler.lexer.token;

import java.util.Objects;

/** Posizione. */
public record SourceLocation(
        int line, int column, long offset, int index, long utf8Offset, long codePointOffset)
        implements Comparable<SourceLocation> {

    /** Valore sentinella per campi opzionali non calcolati. */
    public static final long UNKNOWN = -1L;

    /** Min 1-based. */
    private static final int MIN_1_BASED = 1;

    /** Min 0-based. */
    private static final int MIN_0_BASED = 0;

    /** Costruttore compatto per validare i parametri. */
    public SourceLocation {
        if (line < MIN_1_BASED) {
            throw new IllegalArgumentException("line deve essere >= 1 (1-based), valore: " + line);
        }
        if (column < MIN_1_BASED) {
            throw new IllegalArgumentException(
                    "column deve essere >= 1 (1-based), valore: " + column);
        }
        if (offset < MIN_0_BASED) {
            throw new IllegalArgumentException("offset deve essere >= 0, valore: " + offset);
        }
        if (index < MIN_0_BASED) {
            throw new IllegalArgumentException("index deve essere >= 0, valore: " + index);
        }
        if (utf8Offset != UNKNOWN && utf8Offset < MIN_0_BASED) {
            throw new IllegalArgumentException("utf8Offset deve essere >= 0 oppure UNKNOWN");
        }
        if (codePointOffset != UNKNOWN && codePointOffset < MIN_0_BASED) {
            throw new IllegalArgumentException("codePointOffset deve essere >= 0 oppure UNKNOWN");
        }
    }

    /** Crea una posizione minimale. */
    public static SourceLocation create(final int line, final int column, final long offset) {
        return new SourceLocation(line, column, offset, Math.toIntExact(offset), UNKNOWN, UNKNOWN);
    }

    /** Crea una posizione completa. */
    public static SourceLocation create(
            final int line,
            final int column,
            final long offset,
            final int index,
            final long utf8Offset,
            final long codePointOffset) {
        return new SourceLocation(line, column, offset, index, utf8Offset, codePointOffset);
    }

    /** Copia con offset UTF-8 valorizzato. */
    public SourceLocation withUtf8Offset(final long newUtf8Offset) {
        return new SourceLocation(line, column, offset, index, newUtf8Offset, codePointOffset);
    }

    /** Copia con offset in code point valorizzato. */
    public SourceLocation withCodePointOffset(final long cpOffset) {
        return new SourceLocation(line, column, offset, index, utf8Offset, cpOffset);
    }

    /**
     * Verifica se l'offset UTF-8 è stato calcolato.
     *
     * @return true se l'offset UTF-8 è diverso da {@link #UNKNOWN}.
     */
    public boolean hasUtf8Offset() {
        return utf8Offset != UNKNOWN;
    }

    /**
     * Verifica se l'offset in code point è stato calcolato.
     *
     * @return true se l'offset in code point è diverso da {@link #UNKNOWN}.
     */
    public boolean hasCodePointOffset() {
        return codePointOffset != UNKNOWN;
    }

    @Override
    public int compareTo(final SourceLocation other) {
        Objects.requireNonNull(other, "other must not be null");
        return Long.compare(this.offset, other.offset);
    }

    @Override
    public String toString() {
        return "line %d:column %d".formatted(line, column);
    }
}
