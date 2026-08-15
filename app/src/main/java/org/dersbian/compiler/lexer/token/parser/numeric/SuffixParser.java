package org.dersbian.compiler.lexer.token.parser.numeric;

import java.util.Locale;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.number.INumber;

/**
 * Analysis and handling of type suffixes.
 *
 * <p>Provides functionality for splitting numeric literals into their numeric component and
 * optional type suffix, then dispatching to the appropriate specific parser.
 *
 * <p>{@link #handleSuffix} returns {@code null} on parsing failure or an unknown suffix,
 * analogously to {@code Option::None} in the original Rust implementation. The suffix-pattern
 * detection methods ({@link #checkSingleCharSuffix}, {@link #checkTwoCharSuffix}, {@link
 * #checkThreeCharSuffix}) continue to return {@code Optional<SuffixPattern>} because they do not
 * produce an {@link INumber}.
 */
@SuppressWarnings({"PMD.ShortVariable", "PMD.OnlyOneReturn", "PMD.LongVariable"})
public final class SuffixParser {

    /** Minimum string length required for a two-character suffix. */
    private static final int TWO_CHAR_MIN_LENGTH = 2;

    /** Minimum string length required for a three-character suffix. */
    private static final int THREE_CHAR_MIN_LENGTH = 3;

    private SuffixParser() {}

    /** Represents the possible suffix-length patterns for numeric literals. */
    public enum SuffixPattern {
        /** Single-character suffixes: {@code u}, {@code f}, {@code d} (the most common case). */
        SINGLE_CHAR(1),
        /** Two-character suffixes: {@code i8}, {@code u8}. */
        TWO_CHAR(2),
        /** Three-character suffixes: {@code i16}, {@code i32}, {@code u16}, {@code u32}. */
        THREE_CHAR(3);

        /** Length of the suffix expressed as a character count. */
        private final int charCount;

        SuffixPattern(final int length) {
            this.charCount = length;
        }

        /** Returns the character length of this suffix pattern. */
        public int length() {
            return charCount;
        }
    }

    /**
     * Result of splitting a numeric literal into its numeric part and optional suffix.
     *
     * @param numericPart the numeric portion, without any suffix
     * @param suffix the suffix (with its original casing preserved), or {@code null} if absent
     */
    public record SplitResult(String numericPart, String suffix) {}

    /**
     * Returns whether the last character is a single-character suffix ({@code u}, {@code f}, or
     * {@code d}, case-insensitive).
     */
    public static Optional<SuffixPattern> checkSingleCharSuffix(final String s) {
        if (s.isEmpty()) {
            return Optional.empty();
        }
        final char last = Character.toLowerCase(s.charAt(s.length() - 1));
        return (last == 'u' || last == 'f' || last == 'd')
                ? Optional.of(SuffixPattern.SINGLE_CHAR)
                : Optional.empty();
    }

    /**
     * Returns whether the last three characters form a valid three-character suffix ({@code i16},
     * {@code i32}, {@code u16}, {@code u32}, case-insensitive).
     */
    public static Optional<SuffixPattern> checkThreeCharSuffix(final String s) {
        if (s.length() < THREE_CHAR_MIN_LENGTH) {
            return Optional.empty();
        }
        final String lastThree = s.substring(s.length() - THREE_CHAR_MIN_LENGTH);
        final char c0 = Character.toLowerCase(lastThree.charAt(0));
        final char c1 = lastThree.charAt(1);
        final char c2 = lastThree.charAt(2);

        final boolean valid =
                (c0 == 'i' || c0 == 'u') && ((c1 == '1' && c2 == '6') || (c1 == '3' && c2 == '2'));
        return valid ? Optional.of(SuffixPattern.THREE_CHAR) : Optional.empty();
    }

    /**
     * Returns whether the last two characters form a valid two-character suffix ({@code i8}, {@code
     * u8}, case-insensitive).
     */
    public static Optional<SuffixPattern> checkTwoCharSuffix(final String s) {
        if (s.length() < TWO_CHAR_MIN_LENGTH) {
            return Optional.empty();
        }
        final String lastTwo = s.substring(s.length() - TWO_CHAR_MIN_LENGTH);
        final char c0 = Character.toLowerCase(lastTwo.charAt(0));
        final char c1 = lastTwo.charAt(1);

        final boolean valid = (c0 == 'i' || c0 == 'u') && c1 == '8';
        return valid ? Optional.of(SuffixPattern.TWO_CHAR) : Optional.empty();
    }

    /**
     * Detects the suffix pattern present at the end of {@code s}, checking single-character
     * patterns first (the most common), then three-character patterns, and finally two-character
     * patterns.
     */
    private static Optional<SuffixPattern> detectSuffixPattern(
            final String sufx) { // fix #1: added final
        final Optional<SuffixPattern> single = checkSingleCharSuffix(sufx); // fix #2: added final
        if (single.isPresent()) {
            return single;
        }
        final Optional<SuffixPattern> three = checkThreeCharSuffix(sufx); // fix #3: added final
        if (three.isPresent()) {
            return three;
        }
        return checkTwoCharSuffix(sufx);
    }

    /**
     * Splits a numeric literal into its numeric part and optional type suffix.
     *
     * <p>Supported suffixes: {@code u}, {@code f}, {@code d} (one character); {@code i8}, {@code
     * u8} (two characters); {@code i16}, {@code i32}, {@code u16}, {@code u32} (three characters).
     *
     * @param slice full string of the numeric literal, including any suffix
     * @return the numeric portion and the optional suffix (original casing preserved)
     */
    public static SplitResult splitNumericAndSuffix(final String slice) {
        final SplitResult result;
        if (slice.isEmpty()) {
            result = new SplitResult(slice, null);
        } else {
            result =
                    detectSuffixPattern(slice)
                            .map(
                                    pattern -> {
                                        final int splitPos =
                                                slice.length()
                                                        - pattern.length(); // fix #4: added final
                                        return new SplitResult(
                                                slice.substring(0, splitPos),
                                                slice.substring(splitPos));
                                    })
                            .orElse(new SplitResult(slice, null));
        }
        return result;
    }

    /**
     * Routes numeric literal parsing based on the type suffix.
     *
     * <table>
     *   <caption>Type resolution table</caption>
     *   <tr><th>Suffix</th><th>Type</th><th>Example</th></tr>
     *   <tr><td>(none)</td><td>i64/f64</td>
     *   <td>{@code 42}&rarr;Integer(42), {@code 3.14}&rarr;Float64(3.14)</td></tr>
     *   <tr><td>{@code u}</td><td>u64</td><td>{@code 42u}&rarr;UnsignedInteger(42)</td></tr>
     *   <tr><td>{@code i8}</td><td>i8</td><td>{@code 42i8}&rarr;I8(42)</td></tr>
     *   <tr><td>{@code u16}</td><td>u16</td><td>{@code 1000u16}&rarr;U16(1000)</td></tr>
     *   <tr><td>{@code f}</td><td>f32</td><td>{@code 3.14f}&rarr;Float32(3.14)</td></tr>
     *   <tr><td>{@code d}</td><td>f64</td><td>{@code 3.14d}&rarr;Float64(3.14)</td></tr>
     * </table>
     *
     * @param numericPart numeric part without suffix
     * @param suffix optional type suffix (case-insensitive), may be {@code null}
     * @return the resulting {@link INumber}, or {@code null} if the format is invalid or the suffix
     *     is unsupported
     */
    @SuppressWarnings("PMD.CyclomaticComplexity")
    public static INumber handleSuffix(final String numericPart, final String suffix) {
        final INumber result;
        if (suffix == null) {
            result = NumericParser.handleDefaultSuffix(numericPart);
        } else {
            result =
                    switch (suffix.toLowerCase(Locale.ROOT)) {
                        case "u" -> NumericParser.parseUnsigned64(numericPart);
                        case "u8" ->
                                NumericParser.parseIntegerInRange(
                                        numericPart, 255, v -> new INumber.U8((short) v));
                        case "u16" ->
                                NumericParser.parseIntegerInRange(
                                        numericPart, 65_535, v -> new INumber.U16((int) v));
                        case "u32" ->
                                NumericParser.parseIntegerInRange(
                                        numericPart, 4_294_967_295L, INumber.U32::new);
                        case "i8" ->
                                NumericParser.parseIntegerInRange(
                                        numericPart, 127, v -> new INumber.I8((byte) v));
                        case "i16" ->
                                NumericParser.parseIntegerInRange(
                                        numericPart, 32_767, v -> new INumber.I16((short) v));
                        case "i32" ->
                                NumericParser.parseIntegerInRange(
                                        numericPart, 2_147_483_647L, v -> new INumber.I32((int) v));

                        case "f" -> NumericParser.handleFloatSuffix(numericPart);
                        case "d" -> NumericParser.handleDefaultSuffix(numericPart);
                        default -> null;
                    };
        }
        return result;
    }
}
