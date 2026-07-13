package org.dersbian.compiler;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Holds shared constant values used throughout the compiler, including character codes and UTF-8
 * encoding boundaries/lengths.
 *
 * <p>This class is not meant to be instantiated.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@SuppressWarnings({"PMD.LongVariable", "PMD.DataClass"})
public final class Constants {
    /** Code point for the line feed character ({@code \n}). */
    public static final int LINE_FEED = '\n';

    /** Code point for the carriage return character ({@code \r}). */
    public static final int CARRIAGE_RETURN = '\r';

    /** Unicode byte-order mark, occasionally left at the start of UTF-8 encoded files. */
    public static final int BYTE_ORDER_MARK = '\uFEFF';

    /** Exclusive upper bound (code point) for a 1-byte UTF-8 encoding. */
    public static final int UTF8_ONE_BYTE_LIMIT = 0x80;

    /** Exclusive upper bound (code point) for a 2-byte UTF-8 encoding. */
    public static final int UTF8_TWO_BYTE_LIMIT = 0x800;

    /** Exclusive upper bound (code point) for a 3-byte UTF-8 encoding. */
    public static final int UTF8_THREE_BYTE_LIMIT = 0x10000;

    /** Number of bytes used by a 1-byte UTF-8 encoding. */
    public static final int UTF8_ONE_BYTE_LENGTH = 1;

    /** Number of bytes used by a 2-byte UTF-8 encoding. */
    public static final int UTF8_TWO_BYTE_LENGTH = 2;

    /** Number of bytes used by a 3-byte UTF-8 encoding. */
    public static final int UTF8_THREE_BYTE_LENGTH = 3;

    /** Number of bytes used by a 4-byte UTF-8 encoding. */
    public static final int UTF8_FOUR_BYTE_LENGTH = 4;

    /** Letterale booleano "true". */
    public static final String TRUE_LITERAL = "true";

    /** Letterale booleano "false". */
    public static final String FALSE_LITERAL = "false";

    /** Radix used for {@code #b} binary literals. */
    public static final int RADIX_BINARY = 2;

    /** Radix used for {@code #o} octal literals. */
    public static final int RADIX_OCTAL = 8;

    /** Radix used for {@code #x} hexadecimal literals. */
    public static final int RADIX_HEX = 16;

    /** Numero fisso di cifre esadecimali richieste da una escape sequence {@code \xHH}. */
    public static final int HEX_ESCAPE_DIGIT_COUNT = 2;

    /** Character constant for double quote ("). */
    public static final int CHAR_DOUBLE_QUOTE = '"';

    /** Character constant for single quote ('). */
    public static final int CHAR_SINGLE_QUOTE = '\'';

    /** Character constant for backslash (\). */
    public static final int CHAR_BACKSLASH = '\\';
}
