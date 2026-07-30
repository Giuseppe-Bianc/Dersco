package org.dersbian.compiler.lexer.token.parser.numeric;

import java.util.function.LongFunction;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dersbian.compiler.lexer.token.number.INumber;

/**
 * Core parsing functionality for decimal numeric literals.
 *
 * <p>Contains the parsing logic for integers, floating-point numbers, and scientific notation.
 *
 * <p>All methods return {@code null} on parsing failure, analogously to {@code Option::None} in the
 * original Rust implementation.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings({"PMD.ShortVariable", "PMD.OnlyOneReturn"})
public class NumericParser {

    /**
     * Parses a numeric literal into a structured {@link INumber}.
     *
     * @param slice full text of the literal as recognized by the lexer (numeric part + optional
     *     suffix)
     * @return the resulting {@link INumber}, or {@code null} if the format is invalid or an
     *     overflow/underflow occurred
     */
    public static INumber parseNumber(final String slice) {
        final SuffixParser.SplitResult split = SuffixParser.splitNumericAndSuffix(slice);
        return SuffixParser.handleSuffix(split.numericPart(), split.suffix());
    }

    /**
     * Parses an unsigned integer within a specific range, returning the result through {@code
     * mapFn}.
     *
     * <p>This replaces, due to Java's type system constraints, the generic Rust function {@code
     * parse_integer::<T>}: since Java offers no truly generic primitive types nor narrow unsigned
     * integers, parsing always operates on {@code long} followed by an explicit bounds check for
     * the target type.
     *
     * @param numericPart the numeric string without a suffix
     * @param max the maximum allowed value (the minimum is always 0, since {@link
     *     #isValidIntegerLiteral} excludes sign characters)
     * @param mapFn function that wraps the value in an {@link INumber} variant
     * @return the wrapped number, or {@code null} if the format is invalid or the value exceeds the
     *     range
     */
    public static INumber parseIntegerInRange(
            final String numericPart, final long max, final LongFunction<INumber> mapFn) {
        if (!isValidIntegerLiteral(numericPart)) {
            return null;
        }
        try {
            final long value = Long.parseLong(numericPart);
            if (value < 0 || value > max) {
                return null;
            }
            return mapFn.apply(value);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses an unsigned 64-bit integer (suffix {@code u}), covering the full 0..2^64&#8722;1 range
     * via {@link Long#parseUnsignedLong(String)}.
     *
     * @param numericPart the numeric string without a suffix
     * @return an {@code INumber.UnsignedInteger}, or {@code null} if the format is invalid or the
     *     value exceeds u64::MAX
     */
    public static INumber parseUnsigned64(final String numericPart) {
        if (!isValidIntegerLiteral(numericPart)) {
            return null;
        }
        try {
            final long value = Long.parseUnsignedLong(numericPart);
            return new INumber.UnsignedInteger(value);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    /**
     * Validates that a string represents a pure integer literal.
     *
     * <p>A valid integer literal must:
     *
     * <ul>
     *   <li>contain only ASCII digits (0-9);
     *   <li>not contain a decimal point ({@code .});
     *   <li>not contain an exponent marker ({@code e} or {@code E});
     *   <li>not contain a sign character (handled as a separate token by the lexer).
     * </ul>
     *
     * @param numericPart the numeric string to validate
     * @return {@code true} if the string is a valid integer literal
     */
    public static boolean isValidIntegerLiteral(final String numericPart) {
        if (numericPart.isEmpty()) {
            return false;
        }
        for (int i = 0; i < numericPart.length(); i++) {
            final char c = numericPart.charAt(i);
            if (c == '.' || c == 'e' || c == 'E') {
                return false;
            }
            if (c < '0' || c > '9') {
                return false;
            }
        }
        return true;
    }

    /**
     * Parses numeric strings with a 32-bit float suffix ({@code f}).
     *
     * <p>Handles both simple decimal notation and scientific notation, producing {@link
     * INumber.Float32} or {@link INumber.Scientific32} respectively.
     *
     * @param numericPart the numeric string with the {@code f} suffix already removed
     * @return the resulting number, or {@code null} if parsing fails
     */
    public static INumber handleFloatSuffix(final String numericPart) {
        final INumber scientific = parseScientific(numericPart, true);
        if (scientific != null) {
            return scientific;
        }
        try {
            return new INumber.Float32(Float.parseFloat(numericPart));
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses numeric strings with no suffix or with the {@code d} suffix.
     *
     * <p>Implements the default type-inference rules: integer literals (no decimal/exponent) &rarr;
     * i64; floating-point literals &rarr; f64; scientific notation &rarr; Scientific64.
     *
     * @param numericPart the numeric string without a suffix (or with the {@code d} suffix already
     *     removed)
     * @return the resulting number, or {@code null} if parsing fails
     */
    public static INumber handleDefaultSuffix(final String numericPart) {
        final INumber scientific = parseScientific(numericPart, false);
        return scientific != null ? scientific : handleNonScientific(numericPart);
    }

    /**
     * Parses non-scientific numbers (plain integers and floats).
     *
     * <p>Determines the appropriate type based on the presence of a decimal point: absent &rarr;
     * {@code i64}; present &rarr; {@code f64}.
     *
     * @param numericPart the numeric string to analyse
     * @return {@code INumber.Integer} for literals without a decimal point, {@code INumber.Float64}
     *     for those with one, or {@code null} if parsing fails (overflow, underflow, or invalid
     *     format)
     */
    public static INumber handleNonScientific(final String numericPart) {
        try {
            if (numericPart.indexOf('.') >= 0) {
                return new INumber.Float64(Double.parseDouble(numericPart));
            }
            return new INumber.Integer(Long.parseLong(numericPart));
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    /**
     * Parses numbers in scientific notation (e.g. {@code "6.022e23"}).
     *
     * <p>Format: {@code base[e|E][+|-]exponent}, where the base may be an integer or a
     * floating-point number.
     *
     * @param s full numeric string, potentially in scientific notation
     * @param isF32 if {@code true} the base is interpreted as a 32-bit float, otherwise as 64-bit
     * @return {@code INumber.Scientific32}/{@code INumber.Scientific64}, or {@code null} if the
     *     string is not in scientific notation or parsing fails
     */
    public static INumber parseScientific(final String s, final boolean isF32) {
        final int pos = indexOfExponentMarker(s);
        if (pos < 0) {
            return null;
        }
        final String baseStr = s.substring(0, pos);
        final String expStr = s.substring(pos + 1);

        final int exp;
        try {
            exp = Integer.parseInt(expStr);
        } catch (final NumberFormatException e) {
            return null;
        }

        try {
            if (isF32) {
                final float base = Float.parseFloat(baseStr);
                return new INumber.Scientific32(base, exp);
            }
            final double base = Double.parseDouble(baseStr);
            return new INumber.Scientific64(base, exp);
        } catch (final NumberFormatException e) {
            return null;
        }
    }

    private static int indexOfExponentMarker(final String s) {
        final int idxLower = s.indexOf('e');
        final int idxUpper = s.indexOf('E');
        if (idxLower < 0) {
            return idxUpper;
        }
        if (idxUpper < 0) {
            return idxLower;
        }
        return Math.min(idxLower, idxUpper);
    }
}
