package org.dersbian.compiler.lexer.token.parser.numeric;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dersbian.compiler.lexer.token.number.INumber;

/**
 * Utility class providing static parser methods for non-decimal (binary, octal, hexadecimal)
 * numeric literals.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class BaseParsers {

    /**
     * Parses a binary literal (prefixed with {@code #b}) into an {@link INumber}.
     *
     * @param literal the binary literal string to parse
     * @return the parsed {@link INumber} value
     */
    public static INumber parseBinary(final String literal) {
        return parseWithRadix(literal, "#b", 2);
    }

    /**
     * Parses an octal literal (prefixed with {@code #o}) into an {@link INumber}.
     *
     * @param literal the octal literal string to parse
     * @return the parsed {@link INumber} value
     */
    public static INumber parseOctal(final String literal) {
        return parseWithRadix(literal, "#o", 8);
    }

    /**
     * Parses a hexadecimal literal (prefixed with {@code #x}) into an {@link INumber}.
     *
     * @param literal the hexadecimal literal string to parse
     * @return the parsed {@link INumber} value
     */
    public static INumber parseHex(final String literal) {
        return parseWithRadix(literal, "#x", 16);
    }

    private static INumber parseWithRadix(
            final String literal, final String prefix, final int radix) {
        String body = literal.substring(prefix.length());
        String suffix = null;
        if (!body.isEmpty()) {
            final char last = body.charAt(body.length() - 1);
            if (last == 'u' || last == 'U') {
                suffix = String.valueOf(last);
                body = body.substring(0, body.length() - 1);
            }
        }
        final long value = Long.parseLong(body, radix);
        return new INumber.IntegerValue(value, suffix);
    }
}
