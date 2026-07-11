package org.dersbian.compiler.lexer;

import org.dersbian.compiler.Constants;
import org.dersbian.compiler.lexer.token.SourceLocation;

/**
 * Owns the mutable scanning state for one source text: the UTF-16 cursor position, the
 * corresponding UTF-8 byte offset, the code point count, and the current line/column. Every method
 * here is about moving through the text or reading around the current position; nothing here knows
 * what a token is.
 */
public final class SourceCursor {

    /** The full source text being scanned, indexed by UTF-16 code unit. */
    private final String source;

    /**
     * Cached length of {@link #source} in UTF-16 code units, avoids repeated {@link
     * String#length()} calls.
     */
    private final int length;

    /**
     * Current absolute offset into the source text, expressed in UTF-8 bytes. This mirrors the
     * on-disk byte offset of the original UTF-8 encoded file, unlike {@link #position} which counts
     * UTF-16 code units.
     */
    private long utf8Offset;

    /** Current absolute offset into the source text, expressed in UTF-16 code units. */
    private int position;

    /**
     * Number of Unicode code points consumed so far. Differs from {@link #position} whenever the
     * source contains characters outside the Basic Multilingual Plane (encoded as surrogate pairs,
     * i.e. two UTF-16 code units for a single code point).
     */
    private long codePointOffset;

    /** Current 1-based line number in the source text. */
    private int lineNumber = 1;

    /** Current 1-based column number in the source text. */
    private int columnNumber = 1;

    /**
     * Creates a cursor positioned at the start of {@code source}.
     *
     * @param source the text to scan; must not be {@code null}
     */
    public SourceCursor(final String source) {
        this.source = source;
        this.length = source.length();
    }

    /** Returns {@code true} if the cursor has consumed the entire source text. */
    public boolean isAtEnd() {
        return position >= length;
    }

    /**
     * Returns the Unicode code point at the current position without consuming it, or {@code -1} if
     * the end of the source has been reached.
     */
    public int peekCodePoint() {
        return isAtEnd() ? -1 : source.codePointAt(position);
    }

    /** Returns the Unicode code point immediately after the current one, or {@code -1} at end. */
    public int peekNextCodePoint() {
        final int result;
        if (isAtEnd()) {
            result = -1;
        } else {
            final int codePoint = source.codePointAt(position);
            final int next = position + Character.charCount(codePoint);
            result = next < length ? source.codePointAt(next) : -1;
        }
        return result;
    }

    /**
     * Consumes and returns the Unicode code point at the current position, correctly advancing past
     * surrogate pairs, and updates line/column tracking along with the UTF-8 byte offset and code
     * point count. Line terminators {@code \n}, {@code \r} and {@code \r\n} are all normalized to a
     * single line break.
     *
     * @return the consumed code point, or {@code -1} at end of source.
     */
    public int advance() {
        final int codePoint = peekCodePoint();
        if (codePoint != -1) {
            position += Character.charCount(codePoint);
            utf8Offset += CodePoints.utf8ByteLength(codePoint);
            codePointOffset++;
            switch (codePoint) {
                case Constants.LINE_FEED -> {
                    lineNumber++;
                    columnNumber = 1;
                }
                case Constants.CARRIAGE_RETURN -> {
                    if (peekCodePoint() != Constants.LINE_FEED) {
                        lineNumber++;
                        columnNumber = 1;
                    }
                }
                default -> columnNumber++;
            }
        }
        return codePoint;
    }

    /**
     * Consumes the current code point if it equals {@code expected}; otherwise consumes nothing.
     */
    public boolean match(final int expected) {
        final boolean matched = peekCodePoint() == expected;
        if (matched) {
            advance();
        }
        return matched;
    }

    /**
     * Consumes the current and next code point atomically if they equal {@code first} and {@code
     * second} respectively; otherwise consumes nothing. Useful for two-character sequences that
     * must be recognized before deciding how to treat the first character alone, e.g. {@code "//"}
     * versus a lone {@code '/'}.
     */
    public boolean match(final int first, final int second) {
        final boolean matched = peekCodePoint() == first && peekNextCodePoint() == second;
        if (matched) {
            advance();
            advance();
        }
        return matched;
    }

    /** Builds the {@link SourceLocation} for the current cursor position. */
    public SourceLocation currentLocation() {
        return SourceLocation.create(
                lineNumber, columnNumber, position, position, utf8Offset, codePointOffset);
    }
}
