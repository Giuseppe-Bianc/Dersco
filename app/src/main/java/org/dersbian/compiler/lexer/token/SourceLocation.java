package org.dersbian.compiler.lexer.token;

import java.util.Objects;

/** Source position within a compilation unit. */
public record SourceLocation(
        int line, int column, long offset, int index, long utf8Offset, long codePointOffset)
        implements Comparable<SourceLocation> {

    /** Sentinel value for optional fields that have not been computed. */
    public static final long UNKNOWN = -1L;

    /** Minimum value for 1-based fields. */
    private static final int MIN_1_BASED = 1;

    /** Minimum value for 0-based fields. */
    private static final int MIN_0_BASED = 0;

    /** Compact constructor that validates all parameters. */
    public SourceLocation {
        if (line < MIN_1_BASED) {
            throw new IllegalArgumentException("line must be >= 1 (1-based), got: " + line);
        }
        if (column < MIN_1_BASED) {
            throw new IllegalArgumentException("column must be >= 1 (1-based), got: " + column);
        }
        if (offset < MIN_0_BASED) {
            throw new IllegalArgumentException("offset must be >= 0, got: " + offset);
        }
        if (index < MIN_0_BASED) {
            throw new IllegalArgumentException("index must be >= 0, got: " + index);
        }
        if (utf8Offset != UNKNOWN && utf8Offset < MIN_0_BASED) {
            throw new IllegalArgumentException("utf8Offset must be >= 0 or UNKNOWN");
        }
        if (codePointOffset != UNKNOWN && codePointOffset < MIN_0_BASED) {
            throw new IllegalArgumentException("codePointOffset must be >= 0 or UNKNOWN");
        }
    }

    /** Creates a minimal position with line, column and offset only. */
    public static SourceLocation create(final int line, final int column, final long offset) {
        return new SourceLocation(line, column, offset, Math.toIntExact(offset), UNKNOWN, UNKNOWN);
    }

    /** Creates a fully specified position with all offset variants. */
    public static SourceLocation create(
            final int line,
            final int column,
            final long offset,
            final int index,
            final long utf8Offset,
            final long codePointOffset) {
        return new SourceLocation(line, column, offset, index, utf8Offset, codePointOffset);
    }

    /** Returns a copy with the given UTF-8 byte offset. */
    public SourceLocation withUtf8Offset(final long newUtf8Offset) {
        return new SourceLocation(line, column, offset, index, newUtf8Offset, codePointOffset);
    }

    /** Returns a copy with the given code-point offset. */
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
