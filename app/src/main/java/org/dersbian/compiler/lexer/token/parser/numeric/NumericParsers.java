package org.dersbian.compiler.lexer.token.parser.numeric;

import java.util.Locale;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dersbian.compiler.lexer.token.number.INumber;

/**
 * Utility class providing static methods to parse numeric literal strings into {@link INumber}
 * instances, handling integer and floating-point types with optional type suffixes.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NumericParsers {

    /** Minimum body length required to check for two-character suffixes. */
    private static final int MIN_TAIL2_LENGTH = 2;

    /** Minimum body length required to check for three-character suffixes. */
    private static final int MIN_TAIL3_LENGTH = 3;

    /**
     * Parses a numeric literal string into an {@link INumber}, detecting integer or floating-point
     * type and optional type suffixes (e.g. {@code u}, {@code f}, {@code i32}).
     *
     * @param literal the numeric literal string to parse
     * @return the parsed {@link INumber} value
     */
    public static INumber parseNumber(final String literal) {
        final SuffixResult result = extractSuffix(literal);
        final String body = result.body();
        final String suffix = result.suffix();

        final boolean isFloating =
                body.contains(".")
                        || body.toLowerCase(Locale.ROOT).contains("e")
                        || (suffix != null && "fFdD".contains(suffix));

        return isFloating
                ? new INumber.FloatingValue(Double.parseDouble(body), suffix)
                : new INumber.IntegerValue(Long.parseLong(body), suffix);
    }

    /**
     * Attempts to strip a single-character type suffix from the body.
     *
     * @param body the current numeric literal body
     * @return a {@link SuffixResult}, or {@code null} if not found
     */
    private static SuffixResult trySingleCharSuffix(final String body) {
        final char last = body.charAt(body.length() - 1);
        final SuffixResult found;
        if ("uUfFdD".indexOf(last) >= 0) {
            found = new SuffixResult(body.substring(0, body.length() - 1), String.valueOf(last));
        } else {
            found = null;
        }
        return found;
    }

    /**
     * Attempts to strip a three-character type suffix from the body.
     *
     * @param body the current numeric literal body
     * @return a {@link SuffixResult}, or {@code null} if not found
     */
    private static SuffixResult tryTail3Suffix(final String body) {
        final SuffixResult found;
        if (body.length() < MIN_TAIL3_LENGTH) {
            found = null;
        } else {
            final String tail3 = body.substring(body.length() - MIN_TAIL3_LENGTH);
            if ("i32".equals(tail3)
                    || "u32".equals(tail3)
                    || "i16".equals(tail3)
                    || "u16".equals(tail3)) {
                found =
                        new SuffixResult(
                                body.substring(0, body.length() - MIN_TAIL3_LENGTH), tail3);
            } else {
                found = null;
            }
        }
        return found;
    }

    /**
     * Attempts to strip a two-character type suffix from the body.
     *
     * @param body the current numeric literal body
     * @return a {@link SuffixResult}, or {@code null} if not found
     */
    private static SuffixResult tryTail2Suffix(final String body) {
        final SuffixResult found;
        if (body.length() < MIN_TAIL2_LENGTH) {
            found = null;
        } else {
            final String tail2 = body.substring(body.length() - MIN_TAIL2_LENGTH);
            if ("i8".equals(tail2) || "u8".equals(tail2)) {
                found =
                        new SuffixResult(
                                body.substring(0, body.length() - MIN_TAIL2_LENGTH), tail2);
            } else {
                found = null;
            }
        }
        return found;
    }

    /**
     * Attempts to strip a multi-character type suffix (two or three characters) from the body.
     *
     * @param body the current numeric literal body
     * @return a {@link SuffixResult}, or {@code null} if not found
     */
    private static SuffixResult tryMultiCharSuffix(final String body) {
        final SuffixResult tail3 = tryTail3Suffix(body);
        final SuffixResult found;
        if (tail3 != null) {
            found = tail3;
        } else {
            found = tryTail2Suffix(body);
        }
        return found;
    }

    /**
     * Extracts an optional type suffix from the numeric literal, returning the stripped body and
     * the detected suffix (or {@code null} if none was found).
     *
     * @param literal the full numeric literal string
     * @return a {@link SuffixResult} containing the body and optional suffix
     */
    private static SuffixResult extractSuffix(final String literal) {
        final SuffixResult result;
        if (literal.isEmpty()) {
            result = new SuffixResult(literal, null);
        } else {
            result = extractSuffixFromNonEmpty(literal);
        }
        return result;
    }

    private static SuffixResult extractSuffixFromNonEmpty(final String literal) {
        final SuffixResult single = trySingleCharSuffix(literal);
        final SuffixResult result;
        if (single != null) {
            result = single;
        } else {
            final SuffixResult multi = tryMultiCharSuffix(literal);
            if (multi != null) {
                result = multi;
            } else {
                result = new SuffixResult(literal, null);
            }
        }
        return result;
    }

    /**
     * Immutable value type holding a numeric body string and its optional type suffix.
     *
     * @param body the numeric literal with any suffix stripped
     * @param suffix the detected type suffix, or {@code null} if none
     */
    private record SuffixResult(String body, String suffix) {}
}
