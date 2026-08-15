package org.dersbian.compiler.lexer.token.number;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Test suite for {@link INumber}, the sealed interface representing all numeric literals supported
 * by the language.
 *
 * <p>The following behaviours are verified for every record variant:
 *
 * <ul>
 *   <li>value preservation after construction ({@code value()}, {@code base()}, {@code
 *       exponent()});
 *   <li>{@code toString()} format with the expected suffix ({@code i8}, {@code u32}, etc.) or no
 *       suffix for the default integer and unsigned-integer types;
 *   <li>{@code equals()} contract — reflexivity, symmetry, transitivity — and consistency with
 *       {@code hashCode()};
 *   <li>bitwise semantics for {@code Float32}, {@code Float64}, {@code Scientific32} and {@code
 *       Scientific64}: {@code +0.0}/{@code -0.0} are distinct, NaN instances with the same or
 *       different bit patterns behave correctly;
 *   <li>boundary values: {@code MIN_VALUE}/{@code MAX_VALUE}, zero, infinities, subnormal numbers;
 *   <li>inequality between variants of different types and against non-{@code INumber} objects;
 *   <li>sealed-interface completeness: exactly 12 permitted subclasses, all of which are records.
 * </ul>
 */
@SuppressWarnings({
    "checkstyle:AbbreviationAsWordInName",
    "checkstyle:AvoidEscapedUnicodeCharacters",
    "PMD.TooManyMethods",
    "PMD.UnitTestAssertionsShouldIncludeMessage",
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.ShortVariable",
    "PMD.GodClass",
    "PMD.CyclomaticComplexity",
    "PMD.AtLeastOneConstructor"
})
class INumberTest {

    // ──────────────────────────────────────────────────────────────────────
    // I8
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("I8: value is preserved correctly")
    void i8ValueIsPreserved() {
        final INumber.I8 i8 = new INumber.I8((byte) -42);
        Assertions.assertEquals((byte) -42, i8.value());
    }

    @Test
    @DisplayName("I8: toString returns the value followed by the i8 suffix")
    void i8ToStringFormat() {
        Assertions.assertEquals("-42i8", new INumber.I8((byte) -42).toString());
    }

    @Test
    @DisplayName("I8: Byte.MIN_VALUE boundary")
    void i8MinValue() {
        final INumber.I8 i8 = new INumber.I8(Byte.MIN_VALUE);
        Assertions.assertAll(
                () -> Assertions.assertEquals(Byte.MIN_VALUE, i8.value()),
                () -> Assertions.assertEquals("-128i8", i8.toString()));
    }

    @Test
    @DisplayName("I8: Byte.MAX_VALUE boundary")
    void i8MaxValue() {
        final INumber.I8 i8 = new INumber.I8(Byte.MAX_VALUE);
        Assertions.assertAll(
                () -> Assertions.assertEquals(Byte.MAX_VALUE, i8.value()),
                () -> Assertions.assertEquals("127i8", i8.toString()));
    }

    @Test
    @DisplayName("I8: zero")
    void i8Zero() {
        Assertions.assertEquals("0i8", new INumber.I8((byte) 0).toString());
    }

    @Test
    @DisplayName("I8: two instances with the same value are equal")
    void i8EqualsSameValue() {
        final INumber.I8 a = new INumber.I8((byte) 10);
        final INumber.I8 b = new INumber.I8((byte) 10);
        Assertions.assertAll(
                () -> Assertions.assertEquals(a, b),
                () -> Assertions.assertEquals(a.hashCode(), b.hashCode()));
    }

    @Test
    @DisplayName("I8: two instances with different values are not equal")
    void i8NotEqualsDifferentValue() {
        Assertions.assertNotEquals(new INumber.I8((byte) 1), new INumber.I8((byte) 2));
    }

    // ──────────────────────────────────────────────────────────────────────
    // I16
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("I16: value is preserved correctly")
    void i16ValueIsPreserved() {
        final INumber.I16 i16 = new INumber.I16((short) 1_234);
        Assertions.assertEquals((short) 1_234, i16.value());
    }

    @Test
    @DisplayName("I16: toString returns the value with the i16 suffix")
    void i16ToStringFormat() {
        Assertions.assertEquals("1234i16", new INumber.I16((short) 1_234).toString());
    }

    @Test
    @DisplayName("I16: Short.MIN_VALUE boundary")
    void i16MinValue() {
        Assertions.assertEquals("-32768i16", new INumber.I16(Short.MIN_VALUE).toString());
    }

    @Test
    @DisplayName("I16: Short.MAX_VALUE boundary")
    void i16MaxValue() {
        Assertions.assertEquals("32767i16", new INumber.I16(Short.MAX_VALUE).toString());
    }

    @Test
    @DisplayName("I16: equals and hashCode are consistent")
    void i16EqualsAndHashCode() {
        final INumber.I16 a = new INumber.I16((short) -100);
        final INumber.I16 b = new INumber.I16((short) -100);
        Assertions.assertAll(
                () -> Assertions.assertEquals(a, b),
                () -> Assertions.assertEquals(a.hashCode(), b.hashCode()));
    }

    // ──────────────────────────────────────────────────────────────────────
    // I32
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("I32: value is preserved correctly")
    void i32ValueIsPreserved() {
        final INumber.I32 i32 = new INumber.I32(123_456);
        Assertions.assertEquals(123_456, i32.value());
    }

    @Test
    @DisplayName("I32: toString with i32 suffix")
    void i32ToStringFormat() {
        Assertions.assertEquals("123456i32", new INumber.I32(123_456).toString());
    }

    @Test
    @DisplayName("I32: Integer.MIN_VALUE boundary")
    void i32MinValue() {
        Assertions.assertEquals("-2147483648i32", new INumber.I32(Integer.MIN_VALUE).toString());
    }

    @Test
    @DisplayName("I32: Integer.MAX_VALUE boundary")
    void i32MaxValue() {
        Assertions.assertEquals("2147483647i32", new INumber.I32(Integer.MAX_VALUE).toString());
    }

    @Test
    @DisplayName("I32: zero")
    void i32Zero() {
        Assertions.assertEquals("0i32", new INumber.I32(0).toString());
    }

    @Test
    @DisplayName("I32: negative value")
    void i32Negative() {
        Assertions.assertEquals("-1i32", new INumber.I32(-1).toString());
    }

    @Test
    @DisplayName("I32: equals and hashCode are consistent")
    void i32EqualsAndHashCode() {
        final INumber.I32 a = new INumber.I32(999);
        final INumber.I32 b = new INumber.I32(999);
        Assertions.assertAll(
                () -> Assertions.assertEquals(a, b),
                () -> Assertions.assertEquals(a.hashCode(), b.hashCode()));
    }

    // ──────────────────────────────────────────────────────────────────────
    // Integer (default i64)
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Integer: value is preserved correctly")
    void integerValueIsPreserved() {
        final INumber.Integer integer = new INumber.Integer(Long.MAX_VALUE);
        Assertions.assertEquals(9_223_372_036_854_775_807L, integer.value());
    }

    @Test
    @DisplayName("Integer: toString without suffix")
    void integerToStringNoSuffix() {
        Assertions.assertEquals(
                "9223372036854775807", new INumber.Integer(Long.MAX_VALUE).toString());
    }

    @Test
    @DisplayName("Integer: Long.MIN_VALUE boundary")
    void integerMinValue() {
        Assertions.assertEquals(
                "-9223372036854775808", new INumber.Integer(Long.MIN_VALUE).toString());
    }

    @Test
    @DisplayName("Integer: zero")
    void integerZero() {
        Assertions.assertEquals("0", new INumber.Integer(0L).toString());
    }

    @Test
    @DisplayName("Integer: negative value")
    void integerNegative() {
        Assertions.assertEquals("-42", new INumber.Integer(-42L).toString());
    }

    @Test
    @DisplayName("Integer: equals and hashCode are consistent")
    void integerEqualsAndHashCode() {
        final INumber.Integer a = new INumber.Integer(42L);
        final INumber.Integer b = new INumber.Integer(42L);
        Assertions.assertAll(
                () -> Assertions.assertEquals(a, b),
                () -> Assertions.assertEquals(a.hashCode(), b.hashCode()));
    }

    @Test
    @DisplayName("Integer: different values are not equal")
    void integerNotEquals() {
        Assertions.assertNotEquals(new INumber.Integer(1L), new INumber.Integer(-1L));
    }

    // ──────────────────────────────────────────────────────────────────────
    // U8
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("U8: value is preserved correctly")
    void u8ValueIsPreserved() {
        final INumber.U8 u8 = new INumber.U8((short) 255);
        Assertions.assertEquals((short) 255, u8.value());
    }

    @Test
    @DisplayName("U8: toString with u8 suffix")
    void u8ToStringFormat() {
        Assertions.assertEquals("255u8", new INumber.U8((short) 255).toString());
    }

    @Test
    @DisplayName("U8: minimum value 0")
    void u8MinValue() {
        Assertions.assertEquals("0u8", new INumber.U8((short) 0).toString());
    }

    @Test
    @DisplayName("U8: maximum value 255")
    void u8MaxValue() {
        Assertions.assertEquals("255u8", new INumber.U8((short) 255).toString());
    }

    @Test
    @DisplayName("U8: equals and hashCode are consistent")
    void u8EqualsAndHashCode() {
        final INumber.U8 a = new INumber.U8((short) 128);
        final INumber.U8 b = new INumber.U8((short) 128);
        Assertions.assertAll(
                () -> Assertions.assertEquals(a, b),
                () -> Assertions.assertEquals(a.hashCode(), b.hashCode()));
    }

    // ──────────────────────────────────────────────────────────────────────
    // U16
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("U16: value is preserved correctly")
    void u16ValueIsPreserved() {
        final INumber.U16 u16 = new INumber.U16(65_535);
        Assertions.assertEquals(65_535, u16.value());
    }

    @Test
    @DisplayName("U16: toString with u16 suffix")
    void u16ToStringFormat() {
        Assertions.assertEquals("65535u16", new INumber.U16(65_535).toString());
    }

    @Test
    @DisplayName("U16: zero")
    void u16Zero() {
        Assertions.assertEquals("0u16", new INumber.U16(0).toString());
    }

    @Test
    @DisplayName("U16: equals and hashCode are consistent")
    void u16EqualsAndHashCode() {
        final INumber.U16 a = new INumber.U16(1_000);
        final INumber.U16 b = new INumber.U16(1_000);
        Assertions.assertAll(
                () -> Assertions.assertEquals(a, b),
                () -> Assertions.assertEquals(a.hashCode(), b.hashCode()));
    }

    // ──────────────────────────────────────────────────────────────────────
    // U32
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("U32: value is preserved correctly")
    void u32ValueIsPreserved() {
        final INumber.U32 u32 = new INumber.U32(4_294_967_295L);
        Assertions.assertEquals(4_294_967_295L, u32.value());
    }

    @Test
    @DisplayName("U32: toString with u32 suffix")
    void u32ToStringFormat() {
        Assertions.assertEquals("4294967295u32", new INumber.U32(4_294_967_295L).toString());
    }

    @Test
    @DisplayName("U32: zero")
    void u32Zero() {
        Assertions.assertEquals("0u32", new INumber.U32(0L).toString());
    }

    @Test
    @DisplayName("U32: equals and hashCode are consistent")
    void u32EqualsAndHashCode() {
        final INumber.U32 a = new INumber.U32(100_000L);
        final INumber.U32 b = new INumber.U32(100_000L);
        Assertions.assertAll(
                () -> Assertions.assertEquals(a, b),
                () -> Assertions.assertEquals(a.hashCode(), b.hashCode()));
    }

    // ──────────────────────────────────────────────────────────────────────
    // UnsignedInteger (u64)
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("UnsignedInteger: value is preserved correctly")
    void unsignedIntegerValueIsPreserved() {
        final INumber.UnsignedInteger ui = new INumber.UnsignedInteger(42L);
        Assertions.assertEquals(42L, ui.value());
    }

    @Test
    @DisplayName("UnsignedInteger: toString without suffix, unsigned decimal")
    void unsignedIntegerToStringPositive() {
        Assertions.assertEquals("42", new INumber.UnsignedInteger(42L).toString());
    }

    @Test
    @DisplayName("UnsignedInteger: toString for value exceeding Long.MAX_VALUE as unsigned")
    void unsignedIntegerToStringLargeValue() {
        Assertions.assertEquals(
                "18446744073709551615", new INumber.UnsignedInteger(-1L).toString());
    }

    @Test
    @DisplayName("UnsignedInteger: Long.MIN_VALUE as unsigned = 9223372036854775808")
    void unsignedIntegerMinValueAsBitPattern() {
        Assertions.assertEquals(
                "9223372036854775808", new INumber.UnsignedInteger(Long.MIN_VALUE).toString());
    }

    @Test
    @DisplayName("UnsignedInteger: zero")
    void unsignedIntegerZero() {
        Assertions.assertEquals("0", new INumber.UnsignedInteger(0L).toString());
    }

    @Test
    @DisplayName("UnsignedInteger: Long.MAX_VALUE")
    void unsignedIntegerMaxSignedValue() {
        Assertions.assertEquals(
                "9223372036854775807", new INumber.UnsignedInteger(Long.MAX_VALUE).toString());
    }

    @Test
    @DisplayName("UnsignedInteger: equals and hashCode are consistent")
    void unsignedIntegerEqualsAndHashCode() {
        final INumber.UnsignedInteger a = new INumber.UnsignedInteger(-1L);
        final INumber.UnsignedInteger b = new INumber.UnsignedInteger(-1L);
        Assertions.assertAll(
                () -> Assertions.assertEquals(a, b),
                () -> Assertions.assertEquals(a.hashCode(), b.hashCode()));
    }

    @Test
    @DisplayName("UnsignedInteger: different values are not equal")
    void unsignedIntegerNotEquals() {
        Assertions.assertNotEquals(
                new INumber.UnsignedInteger(0L), new INumber.UnsignedInteger(-1L));
    }

    // ──────────────────────────────────────────────────────────────────────
    // Float32
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Float32: value is preserved correctly")
    void float32ValueIsPreserved() {
        final INumber.Float32 f = new INumber.Float32(7.25f);
        Assertions.assertEquals(7.25f, f.value());
    }

    @Test
    @DisplayName("Float32: toString produces the decimal representation")
    void float32ToStringFormat() {
        Assertions.assertEquals("7.25", new INumber.Float32(7.25f).toString());
    }

    @Test
    @DisplayName("Float32: positive zero and negative zero are distinct (bitwise comparison)")
    void float32PositiveZeroNotEqualsNegativeZero() {
        final INumber.Float32 posZero = new INumber.Float32(0.0f);
        final INumber.Float32 negZero = new INumber.Float32(-0.0f);
        Assertions.assertNotEquals(
                posZero, negZero, "+0.0f and -0.0f must differ under bitwise comparison");
    }

    @Test
    @DisplayName("Float32: two NaN instances with the same bit pattern are equal")
    void float32NanEqualsSamePattern() {
        final INumber.Float32 nan1 = new INumber.Float32(Float.NaN);
        final INumber.Float32 nan2 = new INumber.Float32(Float.NaN);
        Assertions.assertAll(
                () -> Assertions.assertEquals(nan1, nan2),
                () -> Assertions.assertEquals(nan1.hashCode(), nan2.hashCode()));
    }

    @Test
    @DisplayName("Float32: two NaN instances with different bit patterns are not equal")
    void float32DifferentNanPatternsNotEqual() {
        final INumber.Float32 nan1 = new INumber.Float32(Float.NaN);
        final INumber.Float32 nan2 = new INumber.Float32(Float.intBitsToFloat(0x7F800001));
        Assertions.assertNotEquals(nan1, nan2);
    }

    @Test
    @DisplayName("Float32: POSITIVE_INFINITY")
    void float32PositiveInfinity() {
        final INumber.Float32 inf = new INumber.Float32(Float.POSITIVE_INFINITY);
        Assertions.assertEquals(Float.POSITIVE_INFINITY, inf.value());
    }

    @Test
    @DisplayName("Float32: NEGATIVE_INFINITY")
    void float32NegativeInfinity() {
        final INumber.Float32 inf = new INumber.Float32(Float.NEGATIVE_INFINITY);
        Assertions.assertEquals(Float.NEGATIVE_INFINITY, inf.value());
    }

    @Test
    @DisplayName("Float32: POSITIVE_INFINITY and NEGATIVE_INFINITY are not equal")
    void float32PosInfNotEqualsNegInf() {
        Assertions.assertNotEquals(
                new INumber.Float32(Float.POSITIVE_INFINITY),
                new INumber.Float32(Float.NEGATIVE_INFINITY));
    }

    @Test
    @DisplayName("Float32: Float.MAX_VALUE")
    void float32MaxValue() {
        final INumber.Float32 f = new INumber.Float32(Float.MAX_VALUE);
        Assertions.assertEquals(Float.MAX_VALUE, f.value());
    }

    @Test
    @DisplayName("Float32: Float.MIN_VALUE (smallest positive)")
    void float32MinValue() {
        final INumber.Float32 f = new INumber.Float32(Float.MIN_VALUE);
        Assertions.assertEquals(Float.MIN_VALUE, f.value());
    }

    @Test
    @DisplayName("Float32: equals and hashCode are consistent for normal values")
    void float32EqualsAndHashCodeNormal() {
        final INumber.Float32 a = new INumber.Float32(1.5f);
        final INumber.Float32 b = new INumber.Float32(1.5f);
        Assertions.assertAll(
                () -> Assertions.assertEquals(a, b),
                () -> Assertions.assertEquals(a.hashCode(), b.hashCode()));
    }

    @Test
    @DisplayName("Float32: not equal to null")
    void float32NotEqualsNull() {
        Assertions.assertNotEquals(null, new INumber.Float32(1.0f));
    }

    @Test
    @DisplayName("Float32: not equal to a different INumber type")
    void float32NotEqualsOtherType() {
        Assertions.assertNotEquals(new INumber.Float32(1.0f), new INumber.Float64(1.0));
    }

    // ──────────────────────────────────────────────────────────────────────
    // Float64
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Float64: value is preserved correctly")
    void float64ValueIsPreserved() {
        final INumber.Float64 f = new INumber.Float64(9.81);
        Assertions.assertEquals(9.81, f.value());
    }

    @Test
    @DisplayName("Float64: toString produces the decimal representation")
    void float64ToStringFormat() {
        Assertions.assertEquals("9.81", new INumber.Float64(9.81).toString());
    }

    @Test
    @DisplayName("Float64: positive zero and negative zero are distinct (bitwise comparison)")
    void float64PositiveZeroNotEqualsNegativeZero() {
        Assertions.assertNotEquals(
                new INumber.Float64(0.0),
                new INumber.Float64(-0.0),
                "+0.0 and -0.0 must differ under bitwise comparison");
    }

    @Test
    @DisplayName("Float64: two NaN instances with the same bit pattern are equal")
    void float64NanEqualsSamePattern() {
        final INumber.Float64 nan1 = new INumber.Float64(Double.NaN);
        final INumber.Float64 nan2 = new INumber.Float64(Double.NaN);
        Assertions.assertAll(
                () -> Assertions.assertEquals(nan1, nan2),
                () -> Assertions.assertEquals(nan1.hashCode(), nan2.hashCode()));
    }

    @Test
    @DisplayName("Float64: two NaN instances with different bit patterns are not equal")
    void float64DifferentNanPatternsNotEqual() {
        final INumber.Float64 nan1 = new INumber.Float64(Double.NaN);
        final INumber.Float64 nan2 =
                new INumber.Float64(Double.longBitsToDouble(0x7FF0000000000001L));
        Assertions.assertNotEquals(nan1, nan2);
    }

    @Test
    @DisplayName("Float64: POSITIVE_INFINITY and NEGATIVE_INFINITY are not equal")
    void float64PosInfNotEqualsNegInf() {
        Assertions.assertNotEquals(
                new INumber.Float64(Double.POSITIVE_INFINITY),
                new INumber.Float64(Double.NEGATIVE_INFINITY));
    }

    @Test
    @DisplayName("Float64: Double.MAX_VALUE")
    void float64MaxValue() {
        final INumber.Float64 f = new INumber.Float64(Double.MAX_VALUE);
        Assertions.assertEquals(Double.MAX_VALUE, f.value());
    }

    @Test
    @DisplayName("Float64: Double.MIN_VALUE (smallest positive)")
    void float64MinValue() {
        final INumber.Float64 f = new INumber.Float64(Double.MIN_VALUE);
        Assertions.assertEquals(Double.MIN_VALUE, f.value());
    }

    @Test
    @DisplayName("Float64: equals and hashCode are consistent for normal values")
    void float64EqualsAndHashCodeNormal() {
        final INumber.Float64 a = new INumber.Float64(1.234_567_89);
        final INumber.Float64 b = new INumber.Float64(1.234_567_89);
        Assertions.assertAll(
                () -> Assertions.assertEquals(a, b),
                () -> Assertions.assertEquals(a.hashCode(), b.hashCode()));
    }

    @Test
    @DisplayName("Float64: not equal to null")
    void float64NotEqualsNull() {
        Assertions.assertNotEquals(null, new INumber.Float64(1.0));
    }

    @Test
    @DisplayName("Float64: not equal to Float32 with the same numeric value")
    void float64NotEqualsFloat32() {
        Assertions.assertNotEquals(new INumber.Float64(1.0), new INumber.Float32(1.0f));
    }

    // ──────────────────────────────────────────────────────────────────────
    // Scientific32
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Scientific32: base and exponent are preserved")
    void scientific32ValuesPreserved() {
        final INumber.Scientific32 s = new INumber.Scientific32(6.022f, 23);
        Assertions.assertAll(
                () -> Assertions.assertEquals(6.022f, s.base()),
                () -> Assertions.assertEquals(23, s.exponent()));
    }

    @Test
    @DisplayName("Scientific32: toString in baseEexponent format")
    void scientific32ToStringFormat() {
        Assertions.assertEquals("6.022e23", new INumber.Scientific32(6.022f, 23).toString());
    }

    @Test
    @DisplayName("Scientific32: negative exponent")
    void scientific32NegativeExponent() {
        final INumber.Scientific32 s = new INumber.Scientific32(1.6f, -19);
        Assertions.assertAll(
                () -> Assertions.assertEquals("1.6e-19", s.toString()),
                () -> Assertions.assertEquals(-19, s.exponent()));
    }

    @Test
    @DisplayName("Scientific32: zero exponent")
    void scientific32ZeroExponent() {
        Assertions.assertEquals("5.0e0", new INumber.Scientific32(5.0f, 0).toString());
    }

    @Test
    @DisplayName("Scientific32: zero base")
    void scientific32ZeroBase() {
        Assertions.assertEquals("0.0e10", new INumber.Scientific32(0.0f, 10).toString());
    }

    @Test
    @DisplayName("Scientific32: +0.0 and -0.0 as base are distinct (bitwise comparison)")
    void scientific32PosZeroBaseNotEqualsNegZeroBase() {
        Assertions.assertNotEquals(
                new INumber.Scientific32(0.0f, 5), new INumber.Scientific32(-0.0f, 5));
    }

    @Test
    @DisplayName("Scientific32: same base but different exponents are not equal")
    void scientific32DifferentExponent() {
        Assertions.assertNotEquals(
                new INumber.Scientific32(1.0f, 10), new INumber.Scientific32(1.0f, 20));
    }

    @Test
    @DisplayName("Scientific32: same exponent but different bases are not equal")
    void scientific32DifferentBase() {
        Assertions.assertNotEquals(
                new INumber.Scientific32(1.0f, 10), new INumber.Scientific32(2.0f, 10));
    }

    @Test
    @DisplayName("Scientific32: NaN as base — two instances are equal")
    void scientific32NanBase() {
        final INumber.Scientific32 a = new INumber.Scientific32(Float.NaN, 0);
        final INumber.Scientific32 b = new INumber.Scientific32(Float.NaN, 0);
        Assertions.assertAll(
                () -> Assertions.assertEquals(a, b),
                () -> Assertions.assertEquals(a.hashCode(), b.hashCode()));
    }

    @Test
    @DisplayName(
            "Scientific32: equals and hashCode are consistent — Planck constant truncated to float")
    void scientific32EqualsAndHashCode() {
        final INumber.Scientific32 a = new INumber.Scientific32(6.626_07f, -34);
        final INumber.Scientific32 b = new INumber.Scientific32(6.626_07f, -34);
        Assertions.assertAll(
                () -> Assertions.assertEquals(a, b),
                () -> Assertions.assertEquals(a.hashCode(), b.hashCode()));
    }

    @Test
    @DisplayName("Scientific32: not equal to null")
    void scientific32NotEqualsNull() {
        Assertions.assertNotEquals(null, new INumber.Scientific32(1.0f, 0));
    }

    @Test
    @DisplayName("Scientific32: not equal to Scientific64")
    void scientific32NotEqualsScientific64() {
        Assertions.assertNotEquals(
                new INumber.Scientific32(1.0f, 10), new INumber.Scientific64(1.0, 10));
    }

    // ──────────────────────────────────────────────────────────────────────
    // Scientific64
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Scientific64: base and exponent are preserved — Avogadro number")
    void scientific64ValuesPreserved() {
        final INumber.Scientific64 s = new INumber.Scientific64(6.022_140_76, 23);
        Assertions.assertAll(
                () -> Assertions.assertEquals(6.022_140_76, s.base()),
                () -> Assertions.assertEquals(23, s.exponent()));
    }

    @Test
    @DisplayName("Scientific64: toString in baseEexponent format — Avogadro number")
    void scientific64ToStringFormat() {
        Assertions.assertEquals(
                "6.02214076e23", new INumber.Scientific64(6.022_140_76, 23).toString());
    }

    @Test
    @DisplayName("Scientific64: negative exponent — Planck constant")
    void scientific64NegativeExponent() {
        final INumber.Scientific64 planck = new INumber.Scientific64(6.626_070_15, -34);
        Assertions.assertAll(
                () -> Assertions.assertEquals("6.62607015e-34", planck.toString()),
                () -> Assertions.assertEquals(-34, planck.exponent()));
    }

    @Test
    @DisplayName("Scientific64: zero exponent")
    void scientific64ZeroExponent() {
        Assertions.assertEquals("5.0e0", new INumber.Scientific64(5.0, 0).toString());
    }

    @Test
    @DisplayName("Scientific64: zero base")
    void scientific64ZeroBase() {
        Assertions.assertEquals("0.0e100", new INumber.Scientific64(0.0, 100).toString());
    }

    @Test
    @DisplayName("Scientific64: +0.0 and -0.0 as base are distinct")
    void scientific64PosZeroBaseNotEqualsNegZeroBase() {
        Assertions.assertNotEquals(
                new INumber.Scientific64(0.0, 5), new INumber.Scientific64(-0.0, 5));
    }

    @Test
    @DisplayName("Scientific64: NaN as base — two instances are equal")
    void scientific64NanBase() {
        final INumber.Scientific64 a = new INumber.Scientific64(Double.NaN, 0);
        final INumber.Scientific64 b = new INumber.Scientific64(Double.NaN, 0);
        Assertions.assertAll(
                () -> Assertions.assertEquals(a, b),
                () -> Assertions.assertEquals(a.hashCode(), b.hashCode()));
    }

    @Test
    @DisplayName("Scientific64: equals and hashCode are consistent — speed of light")
    void scientific64EqualsAndHashCode() {
        final INumber.Scientific64 a = new INumber.Scientific64(2.997_924_58, 8);
        final INumber.Scientific64 b = new INumber.Scientific64(2.997_924_58, 8);
        Assertions.assertAll(
                () -> Assertions.assertEquals(a, b),
                () -> Assertions.assertEquals(a.hashCode(), b.hashCode()));
    }

    @Test
    @DisplayName("Scientific64: same base but different exponents are not equal")
    void scientific64DifferentExponent() {
        Assertions.assertNotEquals(
                new INumber.Scientific64(1.0, 10), new INumber.Scientific64(1.0, 11));
    }

    @Test
    @DisplayName("Scientific64: not equal to null")
    void scientific64NotEqualsNull() {
        Assertions.assertNotEquals(null, new INumber.Scientific64(1.0, 0));
    }

    @Test
    @DisplayName("Scientific64: Integer.MAX_VALUE exponent")
    void scientific64MaxExponent() {
        final INumber.Scientific64 s = new INumber.Scientific64(1.0, Integer.MAX_VALUE);
        Assertions.assertEquals(Integer.MAX_VALUE, s.exponent());
    }

    @Test
    @DisplayName("Scientific64: Integer.MIN_VALUE exponent")
    void scientific64MinExponent() {
        final INumber.Scientific64 s = new INumber.Scientific64(1.0, Integer.MIN_VALUE);
        Assertions.assertEquals(Integer.MIN_VALUE, s.exponent());
    }

    // ──────────────────────────────────────────────────────────────────────
    // Sealed interface — permitted subclass completeness
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("INumber is a sealed interface")
    void sealedInterfaceIsDetected() {
        Assertions.assertTrue(INumber.class.isSealed(), "INumber must be a sealed interface");
    }

    @Test
    @DisplayName("INumber permits exactly 12 implementations")
    void permittedSubclassesCountIsExpected() {
        final Class<?>[] permitted = INumber.class.getPermittedSubclasses();
        Assertions.assertNotNull(permitted);
        Assertions.assertEquals(12, permitted.length);
    }

    @Test
    @DisplayName("All permitted implementations are records")
    void allPermittedSubclassesAreRecords() {
        final Class<?>[] permitted = INumber.class.getPermittedSubclasses();
        Assertions.assertNotNull(permitted);
        for (final Class<?> clazz : permitted) {
            Assertions.assertTrue(clazz.isRecord(), clazz.getSimpleName() + " must be a record");
        }
    }

    @Test
    @DisplayName("Permitted implementations include all expected types")
    void permittedSubclassesContainAllExpectedTypes() {
        final Class<?>[] permitted = INumber.class.getPermittedSubclasses();
        Assertions.assertNotNull(permitted);
        final Set<String> names = new HashSet<>();
        for (final Class<?> c : permitted) {
            names.add(c.getSimpleName());
        }
        Assertions.assertAll(
                () -> Assertions.assertTrue(names.contains("I8"), "Missing I8"),
                () -> Assertions.assertTrue(names.contains("I16"), "Missing I16"),
                () -> Assertions.assertTrue(names.contains("I32"), "Missing I32"),
                () -> Assertions.assertTrue(names.contains("Integer"), "Missing Integer"),
                () -> Assertions.assertTrue(names.contains("U8"), "Missing U8"),
                () -> Assertions.assertTrue(names.contains("U16"), "Missing U16"),
                () -> Assertions.assertTrue(names.contains("U32"), "Missing U32"),
                () ->
                        Assertions.assertTrue(
                                names.contains("UnsignedInteger"), "Missing UnsignedInteger"),
                () -> Assertions.assertTrue(names.contains("Float32"), "Missing Float32"),
                () -> Assertions.assertTrue(names.contains("Float64"), "Missing Float64"),
                () -> Assertions.assertTrue(names.contains("Scientific32"), "Missing Scientific32"),
                () ->
                        Assertions.assertTrue(
                                names.contains("Scientific64"), "Missing Scientific64"));
    }

    // ──────────────────────────────────────────────────────────────────────
    // Polymorphism — every variant implements INumber
    // ──────────────────────────────────────────────────────────────────────

    private static Stream<Arguments> allINumberInstances() {
        return Stream.of(
                Arguments.of(new INumber.I8((byte) 1)),
                Arguments.of(new INumber.I16((short) 1)),
                Arguments.of(new INumber.I32(1)),
                Arguments.of(new INumber.Integer(1L)),
                Arguments.of(new INumber.U8((short) 1)),
                Arguments.of(new INumber.U16(1)),
                Arguments.of(new INumber.U32(1L)),
                Arguments.of(new INumber.UnsignedInteger(1L)),
                Arguments.of(new INumber.Float32(1.0f)),
                Arguments.of(new INumber.Float64(1.0)),
                Arguments.of(new INumber.Scientific32(1.0f, 0)),
                Arguments.of(new INumber.Scientific64(1.0, 0)));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("allINumberInstances")
    @DisplayName("Every variant is non-null and produces a non-null toString")
    void everyVariantIsNonNullWithValidToString(final INumber number) {
        Assertions.assertAll(
                () -> Assertions.assertNotNull(number),
                () -> Assertions.assertNotNull(number.toString()));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("allINumberInstances")
    @DisplayName("Every variant is equal to itself (reflexivity)")
    void everyVariantEqualsItself(final INumber number) {
        Assertions.assertEquals(number, number);
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("allINumberInstances")
    @DisplayName("toString never throws for any variant")
    void stringRepresentationNeverThrows(final INumber number) {
        Assertions.assertDoesNotThrow(number::toString);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Cross-type: different types are never equal to each other
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("I8 and I16 with the same numeric value are not equal")
    void i8NotEqualsI16SameValue() {
        Assertions.assertNotEquals(new INumber.I8((byte) 42), new INumber.I16((short) 42));
    }

    @Test
    @DisplayName("I32 and Integer with the same numeric value are not equal")
    void i32NotEqualsIntegerSameValue() {
        Assertions.assertNotEquals(new INumber.I32(42), new INumber.Integer(42L));
    }

    @Test
    @DisplayName("Integer and UnsignedInteger with the same numeric value are not equal")
    void integerNotEqualsUnsignedIntegerSameValue() {
        Assertions.assertNotEquals(new INumber.Integer(42L), new INumber.UnsignedInteger(42L));
    }

    @Test
    @DisplayName("U8 and U16 with the same numeric value are not equal")
    void u8NotEqualsU16SameValue() {
        Assertions.assertNotEquals(new INumber.U8((short) 100), new INumber.U16(100));
    }

    @Test
    @DisplayName("Float32 and Scientific32 are not equal even with a similar value")
    void float32NotEqualsScientific32() {
        Assertions.assertNotEquals(new INumber.Float32(1.0f), new INumber.Scientific32(1.0f, 0));
    }

    @Test
    @DisplayName("Float64 and Scientific64 are not equal even with a similar value")
    void float64NotEqualsScientific64() {
        Assertions.assertNotEquals(new INumber.Float64(1.0), new INumber.Scientific64(1.0, 0));
    }

    // ──────────────────────────────────────────────────────────────────────
    // Edge cases: comparison with non-INumber objects
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("I8 is not equal to a String")
    void i8NotEqualsString() {
        Assertions.assertNotEquals(new INumber.I8((byte) 1), "1i8");
    }

    @Test
    @DisplayName("Integer is not equal to a boxed Long")
    void integerNotEqualsBoxedLong() {
        Assertions.assertNotEquals(new INumber.Integer(42L), 42L);
    }

    @Test
    @DisplayName("Float32 is not equal to a boxed Float")
    void float32NotEqualsBoxedFloat() {
        Assertions.assertNotEquals(new INumber.Float32(1.0f), 1.0f);
    }

    @Test
    @DisplayName("Float64 is not equal to a boxed Double")
    void float64NotEqualsBoxedDouble() {
        Assertions.assertNotEquals(new INumber.Float64(1.0), 1.0);
    }

    // ──────────────────────────────────────────────────────────────────────
    // Float32 / Float64: hashCode consistency for +0.0 and -0.0
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Float32: hashCode differs for +0.0f and -0.0f")
    void float32HashCodeDiffersForPosAndNegZero() {
        final int h1 = new INumber.Float32(0.0f).hashCode();
        final int h2 = new INumber.Float32(-0.0f).hashCode();
        Assertions.assertNotEquals(
                h1,
                h2,
                "hashCode should differ for +0.0f and -0.0f given the bitwise implementation");
    }

    @Test
    @DisplayName("Float64: hashCode differs for +0.0 and -0.0")
    void float64HashCodeDiffersForPosAndNegZero() {
        final int h1 = new INumber.Float64(0.0).hashCode();
        final int h2 = new INumber.Float64(-0.0).hashCode();
        Assertions.assertNotEquals(
                h1,
                h2,
                "hashCode should differ for +0.0 and -0.0 given the bitwise implementation");
    }

    // ──────────────────────────────────────────────────────────────────────
    // Edge cases: subnormal float values
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Float32: subnormal value is preserved and equal")
    void float32Subnormal() {
        final float subnormal = Float.MIN_VALUE / 2.0f;
        final INumber.Float32 a = new INumber.Float32(subnormal);
        final INumber.Float32 b = new INumber.Float32(subnormal);
        Assertions.assertAll(
                () -> Assertions.assertEquals(a, b),
                () -> Assertions.assertEquals(a.hashCode(), b.hashCode()));
    }

    @Test
    @DisplayName("Float64: subnormal value is preserved and equal")
    void float64Subnormal() {
        final double subnormal = Double.MIN_VALUE / 2.0;
        final INumber.Float64 a = new INumber.Float64(subnormal);
        final INumber.Float64 b = new INumber.Float64(subnormal);
        Assertions.assertAll(
                () -> Assertions.assertEquals(a, b),
                () -> Assertions.assertEquals(a.hashCode(), b.hashCode()));
    }

    // ──────────────────────────────────────────────────────────────────────
    // Parameterized: I8 boundary values
    // ──────────────────────────────────────────────────────────────────────

    @ParameterizedTest(name = "I8({0})")
    @ValueSource(bytes = {Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE})
    @DisplayName("I8: boundary values produce the correct toString")
    void i8BoundaryValues(final byte val) {
        final INumber.I8 i8 = new INumber.I8(val);
        Assertions.assertEquals(val + "i8", i8.toString());
    }

    // ──────────────────────────────────────────────────────────────────────
    // Symmetry of equals
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Float32: equals is symmetric")
    void float32EqualsSymmetric() {
        final INumber.Float32 a = new INumber.Float32(42.0f);
        final INumber.Float32 b = new INumber.Float32(42.0f);
        Assertions.assertAll(
                () -> Assertions.assertEquals(a, b), () -> Assertions.assertEquals(b, a));
    }

    @Test
    @DisplayName("Float64: equals is symmetric")
    void float64EqualsSymmetric() {
        final INumber.Float64 a = new INumber.Float64(42.0);
        final INumber.Float64 b = new INumber.Float64(42.0);
        Assertions.assertAll(
                () -> Assertions.assertEquals(a, b), () -> Assertions.assertEquals(b, a));
    }

    @Test
    @DisplayName("Scientific32: equals is symmetric")
    void scientific32EqualsSymmetric() {
        final INumber.Scientific32 a = new INumber.Scientific32(1.5f, 10);
        final INumber.Scientific32 b = new INumber.Scientific32(1.5f, 10);
        Assertions.assertAll(
                () -> Assertions.assertEquals(a, b), () -> Assertions.assertEquals(b, a));
    }

    @Test
    @DisplayName("Scientific64: equals is symmetric")
    void scientific64EqualsSymmetric() {
        final INumber.Scientific64 a = new INumber.Scientific64(1.5, 10);
        final INumber.Scientific64 b = new INumber.Scientific64(1.5, 10);
        Assertions.assertAll(
                () -> Assertions.assertEquals(a, b), () -> Assertions.assertEquals(b, a));
    }

    // ──────────────────────────────────────────────────────────────────────
    // Transitivity of equals
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Float64: equals is transitive")
    void float64EqualsTransitive() {
        final INumber.Float64 a = new INumber.Float64(Math.PI);
        final INumber.Float64 b = new INumber.Float64(Math.PI);
        final INumber.Float64 c = new INumber.Float64(Math.PI);
        Assertions.assertAll(
                () -> Assertions.assertEquals(a, b),
                () -> Assertions.assertEquals(b, c),
                () -> Assertions.assertEquals(a, c));
    }

    // ──────────────────────────────────────────────────────────────────────
    // Scientific with negative base
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Scientific32: negative base")
    void scientific32NegativeBase() {
        final INumber.Scientific32 s = new INumber.Scientific32(-1.5f, 3);
        Assertions.assertAll(
                () -> Assertions.assertEquals(-1.5f, s.base()),
                () -> Assertions.assertEquals("-1.5e3", s.toString()));
    }

    @Test
    @DisplayName("Scientific64: negative base")
    void scientific64NegativeBase() {
        final INumber.Scientific64 s = new INumber.Scientific64(-2.998, 8);
        Assertions.assertAll(
                () -> Assertions.assertEquals(-2.998, s.base()),
                () -> Assertions.assertEquals("-2.998e8", s.toString()));
    }

    // ──────────────────────────────────────────────────────────────────────
    // Corner cases: Scientific with Infinity as base
    // ──────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Scientific32: Infinity as base")
    void scientific32InfinityBase() {
        final INumber.Scientific32 s = new INumber.Scientific32(Float.POSITIVE_INFINITY, 0);
        Assertions.assertEquals(Float.POSITIVE_INFINITY, s.base());
    }

    @Test
    @DisplayName("Scientific64: Infinity as base")
    void scientific64InfinityBase() {
        final INumber.Scientific64 s = new INumber.Scientific64(Double.POSITIVE_INFINITY, 0);
        Assertions.assertEquals(Double.POSITIVE_INFINITY, s.base());
    }

    // ── 1. Exponent is the SOLE difference — base bits are identical ──────────
    @Test
    @DisplayName("Scientific64: equal bases, exponent is sole discriminator → not equal")
    void scientific64ExponentIsSoleDiscriminator() {
        final double base = Math.PI; // non-trivial, identical bit pattern
        final INumber.Scientific64 a = new INumber.Scientific64(base, 5);
        final INumber.Scientific64 b = new INumber.Scientific64(base, 6);

        // The base-bits comparison passes; only exponent check makes them unequal
        Assertions.assertNotEquals(
                a,
                b,
                "Differs only in exponent: the exponent clause must be reached and return false");
    }

    // ── 2. Exponent boundary: Integer.MAX_VALUE vs Integer.MIN_VALUE ──────────
    @Test
    @DisplayName("Scientific64: Integer.MAX_VALUE exponent != Integer.MIN_VALUE exponent")
    void scientific64MaxVsMinExponent() {
        final INumber.Scientific64 a = new INumber.Scientific64(1.0, Integer.MAX_VALUE);
        final INumber.Scientific64 b = new INumber.Scientific64(1.0, Integer.MIN_VALUE);
        Assertions.assertNotEquals(a, b);
    }

    // ── 3. Exponent boundary: zero vs one ────────────────────────────────────
    @Test
    @DisplayName("Scientific64: exponent 0 != exponent 1 with identical base")
    void scientific64ZeroVsOneExponent() {
        final INumber.Scientific64 a = new INumber.Scientific64(1.0, 0);
        final INumber.Scientific64 b = new INumber.Scientific64(1.0, 1);
        Assertions.assertNotEquals(a, b);
    }

    // ── 4. Exponent boundary: negative vs positive ────────────────────────────
    @Test
    @DisplayName("Scientific64: negative exponent != positive exponent with identical base")
    void scientific64NegativeVsPositiveExponent() {
        final double base = 1.5;
        final INumber.Scientific64 a = new INumber.Scientific64(base, -1);
        final INumber.Scientific64 b = new INumber.Scientific64(base, 1);
        Assertions.assertNotEquals(a, b);
    }

    // ── 5. Same exponent, same base bits → must be EQUAL (transitivity check) ─
    @Test
    @DisplayName("Scientific64: identical base bits and exponent → equal (exponent clause: true)")
    void scientific64ExponentClauseReturnsTrue() {
        final double base = 6.626_070_15;
        final int exp = -34;
        final INumber.Scientific64 a = new INumber.Scientific64(base, exp);
        final INumber.Scientific64 b = new INumber.Scientific64(base, exp);
        Assertions.assertAll(
                () -> Assertions.assertEquals(a, b),
                () -> Assertions.assertEquals(a.hashCode(), b.hashCode()));
    }

    // ── 6. NaN base, differing exponents → not equal ─────────────────────────
    @Test
    @DisplayName("Scientific64: NaN base with different exponents are not equal")
    void scientific64NanBaseDifferentExponents() {
        // NaN bits are equal → short-circuit does NOT skip exponent check
        final INumber.Scientific64 a = new INumber.Scientific64(Double.NaN, 0);
        final INumber.Scientific64 b = new INumber.Scientific64(Double.NaN, 1);
        Assertions.assertNotEquals(
                a, b, "NaN bits match, so exponent is the sole discriminator here");
    }

    // ── 7. +0.0 base, differing exponents → not equal ────────────────────────
    @Test
    @DisplayName("Scientific64: +0.0 base with different exponents are not equal")
    void scientific64PosZeroBaseDifferentExponents() {
        final INumber.Scientific64 a = new INumber.Scientific64(0.0, 10);
        final INumber.Scientific64 b = new INumber.Scientific64(0.0, 20);
        Assertions.assertNotEquals(a, b);
    }
}
