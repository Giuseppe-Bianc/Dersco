package org.dersbian.compiler.lexer.token.number;

import java.util.Objects;

/**
 * Represents numeric literals in the various formats supported by the language.
 *
 * <p>This sealed interface captures the different number representations found in source code,
 * preserving the original format for precise error reporting and accurate processing during
 * compilation.
 *
 * <h2>Type safety</h2>
 *
 * <p>Each record enforces type-specific bounds through the chosen primitive type, allowing
 * overflow/underflow errors to be detected at tokenization time rather than deferred to later
 * compilation phases.
 *
 * <h2>Examples</h2>
 *
 * <pre>{@code
 * Number small     = new Number.I8((byte) -42);
 * Number large     = new Number.Integer(9223372036854775807L);
 * Number byteVal   = new Number.U8((short) 255);
 * Number unsigned  = new Number.UnsignedInteger(42L);
 * Number pi        = new Number.Float64(3.14159);
 * Number planck    = new Number.Scientific64(6.62607015, -34);
 * }</pre>
 *
 * <p>Note on unsigned integer representation: Java has no primitive types narrower than {@code
 * long} that are unsigned. For {@code u8}, {@code u16}, and {@code u32} the immediately wider
 * signed type ({@code short}, {@code int}, {@code long} respectively) is used, since it can
 * represent the full non-negative range as a positive value. For {@code u64}, {@code long} is used
 * retaining the raw bit pattern; printing and decimal parsing rely on {@link Long#toUnsignedString}
 * and {@link Long#parseUnsignedLong}.
 */
@SuppressWarnings({"AvoidCommonTypeNames", "checkstyle:AbbreviationAsWordInName"})
public sealed interface INumber {

    /** Signed 8-bit integer literal (e.g. {@code -42i8}). Range: -128..127. */
    record I8(byte value) implements INumber {
        @Override
        public String toString() {
            return value + "i8";
        }
    }

    /** Signed 16-bit integer literal (e.g. {@code 1234i16}). Range: -32768..32767. */
    record I16(short value) implements INumber {
        @Override
        public String toString() {
            return value + "i16";
        }
    }

    /** Signed 32-bit integer literal (e.g. {@code 123456i32}). */
    record I32(int value) implements INumber {
        @Override
        public String toString() {
            return value + "i32";
        }
    }

    /**
     * Signed 64-bit integer literal (e.g. {@code -42}, {@code 1234}). Default integer type when no
     * suffix is specified.
     */
    record Integer(long value) implements INumber {
        @Override
        public String toString() {
            return Long.toString(value);
        }
    }

    /**
     * Unsigned 8-bit integer literal (e.g. {@code 42u8}). Represented as {@code short} because Java
     * has no 8-bit type capable of holding the full 0..255 range as a positive value.
     */
    record U8(short value) implements INumber {
        @Override
        public String toString() {
            return value + "u8";
        }
    }

    /**
     * Unsigned 16-bit integer literal (e.g. {@code 1234u16}). Represented as {@code int} to hold
     * the 0..65535 range.
     */
    record U16(int value) implements INumber {
        @Override
        public String toString() {
            return value + "u16";
        }
    }

    /**
     * Unsigned 32-bit integer literal (e.g. {@code 123456u32}). Represented as {@code long} to hold
     * the 0..4294967295 range.
     */
    record U32(long value) implements INumber {
        @Override
        public String toString() {
            return value + "u32";
        }
    }

    /**
     * Unsigned 64-bit integer literal (e.g. {@code 42u}, {@code 1234u}). Represented as a {@code
     * long} holding the raw bit pattern; use {@link Long#toUnsignedString(long)} for printing when
     * the value may exceed {@link Long#MAX_VALUE}.
     */
    record UnsignedInteger(long value) implements INumber {
        @Override
        public String toString() {
            return Long.toUnsignedString(value);
        }
    }

    /** 32-bit floating-point literal (e.g. {@code 3.14f}, {@code 6.022e23f}). */
    record Float32(float value) implements INumber {
        /**
         * Bit-by-bit comparison (via {@link Float#floatToRawIntBits}) to handle NaN and signed zero
         * consistently, analogous to {@code to_bits()} in Rust.
         */
        @Override
        public boolean equals(final Object obj) {
            return obj instanceof Float32 other
                    && Float.floatToRawIntBits(value) == Float.floatToRawIntBits(other.value());
        }

        @Override
        public int hashCode() {
            return Objects.hash(Float32.class, Float.floatToRawIntBits(value));
        }

        @Override
        public String toString() {
            return Float.toString(value);
        }
    }

    /** 64-bit floating-point literal (e.g. {@code 3.14159}, {@code 6.02214076e23}). */
    record Float64(double value) implements INumber {
        @Override
        public boolean equals(final Object obj) {
            return obj instanceof Float64 other
                    && Double.doubleToRawLongBits(value)
                            == Double.doubleToRawLongBits(other.value());
        }

        @Override
        public int hashCode() {
            return Objects.hash(Float64.class, Double.doubleToRawLongBits(value));
        }

        @Override
        public String toString() {
            return Double.toString(value);
        }
    }

    /**
     * Scientific notation with a 32-bit base and exponent (e.g. {@code 6.022e23f}). Stores the
     * number in the form: base × 10^exponent.
     *
     * @param base base value (mantissa)
     * @param exponent exponent (power of 10)
     */
    record Scientific32(float base, int exponent) implements INumber {
        @Override
        public boolean equals(final Object obj) {
            return obj instanceof Scientific32 other
                    && Float.floatToRawIntBits(base) == Float.floatToRawIntBits(other.base())
                    && exponent == other.exponent();
        }

        @Override
        public int hashCode() {
            return Objects.hash(Scientific32.class, Float.floatToRawIntBits(base), exponent);
        }

        @Override
        public String toString() {
            return base + "e" + exponent;
        }
    }

    /**
     * Scientific notation with a 64-bit base and exponent (e.g. {@code 6.02214076e23}). Stores the
     * number in the form: base × 10^exponent.
     *
     * @param base base value (mantissa)
     * @param exponent exponent (power of 10)
     */
    record Scientific64(double base, int exponent) implements INumber {
        @Override
        public boolean equals(final Object obj) {
            if (!(obj instanceof Scientific64 other)) {
                return false;
            }

            final boolean sameBase =
                    Double.doubleToRawLongBits(base) == Double.doubleToRawLongBits(other.base());
            final boolean sameExponent = exponent == other.exponent();

            return sameBase && sameExponent;
        }

        @Override
        public int hashCode() {
            return Objects.hash(Scientific64.class, Double.doubleToRawLongBits(base), exponent);
        }

        @Override
        public String toString() {
            return base + "e" + exponent;
        }
    }
}
