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
     * Decodes the code point starting at the given UTF-16 index, or {@code -1} if {@code index} is
     * at or past the end of {@link #source}. Centralizes the bounds check so that callers needing
     * to look more than one code point ahead can reuse an already-decoded code point instead of
     * re-decoding the same position, as {@link #match(int, int)} does.
     */
    private int codePointAt(final int index) {
        return index < length ? source.codePointAt(index) : -1;
    }

    /**
     * Returns the Unicode code point at the current position without consuming it, or {@code -1} if
     * the end of the source has been reached.
     */
    public int peekCodePoint() {
        return codePointAt(position);
    }

    /** Returns the Unicode code point immediately after the current one, or {@code -1} at end. */
    public int peekNextCodePoint() {
        final int codePoint = peekCodePoint();
        return codePoint == -1 ? -1 : codePointAt(position + Character.charCount(codePoint));
    }

    /**
     * Consumes and returns the Unicode code point at the current position, correctly advancing past
     * surrogate pairs, and updates line/column tracking along with the UTF-8 byte offset and code
     * point count. Line terminators {@code \n}, {@code \r} and {@code \r\n} are all normalized to a
     * single line break.
     *
     * <h2>Surrogate-pair handling</h2>
     *
     * <p>UTF-16 encodes code points above {@code U+FFFF} as a pair of {@code char} units: a high
     * surrogate ({@code 0xD800}..{@code 0xDBFF}) followed by a low surrogate ({@code
     * 0xDC00}..{@code 0xDFFF}). The Java {@link String} type stores text as a sequence of UTF-16
     * code units, so a single visible character can occupy two {@code char} slots.
     *
     * <p>This method is the single point that crosses that boundary. It must:
     *
     * <ul>
     *   <li>Read the code point via {@link String#codePointAt(int)}, which decodes a lone BMP code
     *       unit as itself and a high surrogate followed by a low surrogate as a single
     *       supplementary code point.
     *   <li>Advance {@link #position} by {@link Character#charCount(int)}, which is {@code 1} for
     *       BMP code points and {@code 2} for supplementary code points. Never advance by {@code 1}
     *       unconditionally; doing so would split a surrogate pair and leave {@code position}
     *       pointing at the low surrogate half on the next call.
     *   <li>Increment {@link #codePointOffset} and {@link #utf8Offset} by <em>one</em> each (code
     *       points and UTF-8 bytes are counted per logical character, not per UTF-16 code unit).
     *       The UTF-8 byte length comes from {@link CodePoints#utf8ByteLength(int)}.
     * </ul>
     *
     * <p>Malformed input (an unpaired high or low surrogate) is treated by {@link
     * String#codePointAt(int)} as a single code-unit value, so the cursor still moves forward, but
     * the resulting {@link SourceLocation} will reference an invalid code point. The lexer is
     * expected to surface this through {@link ErrorCode#E0001} at the call site; this method does
     * not throw.
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
                case Constants.LINE_FEED,
                        Constants.LINE_SEPARATOR,
                        Constants.PARAGRAPH_SEPARATOR -> {
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
        final int codePoint = peekCodePoint();
        final boolean matched =
                codePoint == first
                        && codePointAt(position + Character.charCount(codePoint)) == second;
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
