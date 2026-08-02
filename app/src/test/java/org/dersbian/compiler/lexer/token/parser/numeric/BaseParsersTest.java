package org.dersbian.compiler.lexer.token.parser.numeric;

import org.dersbian.compiler.lexer.token.number.INumber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestAssertionsShouldIncludeMessage",
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.TooManyMethods"
})
class BaseParsersTest {

    @Test
    void parseBinaryWithOnlyPrefixReturnsNull() {
        final INumber result = BaseNumberParser.parseBinary("#b");

        Assertions.assertNull(result);
    }

    @Test
    void parseHexWithOnlyPrefixReturnsNull() {
        final INumber result = BaseNumberParser.parseHex("#x");

        Assertions.assertNull(result);
    }

    @Test
    void parseOctalWithOnlyPrefixReturnsNull() {
        final INumber result = BaseNumberParser.parseOctal("#o");

        Assertions.assertNull(result);
    }

    @Test
    void parseBinaryWithLowercaseUnsignedSuffixReturnsUnsignedInteger() {
        final INumber result = BaseNumberParser.parseBinary("#b1111u");
        final INumber.UnsignedInteger value =
                Assertions.assertInstanceOf(INumber.UnsignedInteger.class, result);

        Assertions.assertEquals(15L, value.value());
    }

    @Test
    void parseBinaryWithUppercaseUnsignedSuffixReturnsUnsignedInteger() {
        final INumber result = BaseNumberParser.parseBinary("#b1111U");
        final INumber.UnsignedInteger value =
                Assertions.assertInstanceOf(INumber.UnsignedInteger.class, result);

        Assertions.assertEquals(15L, value.value());
    }

    @Test
    void parseHexWithoutUnsignedSuffixReturnsSignedInteger() {
        final INumber result = BaseNumberParser.parseHex("#xFF");
        final INumber.Integer value = Assertions.assertInstanceOf(INumber.Integer.class, result);

        Assertions.assertEquals(255L, value.value());
    }

    @Test
    void parseOctalWithLowercaseUnsignedSuffixStripsSuffixCorrectly() {
        final INumber result = BaseNumberParser.parseOctal("#o77u");
        final INumber.UnsignedInteger value =
                Assertions.assertInstanceOf(INumber.UnsignedInteger.class, result);

        Assertions.assertEquals(63L, value.value());
    }

    @Test
    void parseOctalWithUppercaseUnsignedSuffixStripsSuffixCorrectly() {
        final INumber result = BaseNumberParser.parseOctal("#o77U");
        final INumber.UnsignedInteger value =
                Assertions.assertInstanceOf(INumber.UnsignedInteger.class, result);

        Assertions.assertEquals(63L, value.value());
    }

    @Test
    void parseBinaryWithoutUnsignedSuffixUsesEntireNumericPart() {
        final INumber result = BaseNumberParser.parseBinary("#b1010");
        final INumber.Integer value = Assertions.assertInstanceOf(INumber.Integer.class, result);

        Assertions.assertEquals(10L, value.value());
    }

    @Test
    void parseHexWithMaximumUnsignedLongReturnsUnsignedInteger() {
        final INumber result = BaseNumberParser.parseHex("#xFFFFFFFFFFFFFFFFu");
        final INumber.UnsignedInteger value =
                Assertions.assertInstanceOf(INumber.UnsignedInteger.class, result);

        Assertions.assertEquals(-1L, value.value());
    }

    @Test
    void parseHexWithMaximumSignedLongReturnsInteger() {
        final INumber result = BaseNumberParser.parseHex("#x7FFFFFFFFFFFFFFF");
        final INumber.Integer value = Assertions.assertInstanceOf(INumber.Integer.class, result);

        Assertions.assertEquals(Long.MAX_VALUE, value.value());
    }

    @Test
    void parseBinaryWithMinimumSignedLongReturnsInteger() {
        final String minBinary = "-1" + "0".repeat(63);
        final INumber result = BaseNumberParser.parseBinary("#b" + minBinary);
        final INumber.Integer value = Assertions.assertInstanceOf(INumber.Integer.class, result);

        Assertions.assertEquals(Long.MIN_VALUE, value.value());
    }

    @Test
    void parseBinaryWithInvalidDigitReturnsNull() {
        final INumber result = BaseNumberParser.parseBinary("#b1012");

        Assertions.assertNull(result);
    }

    @Test
    void parseOctalWithInvalidDigitReturnsNull() {
        final INumber result = BaseNumberParser.parseOctal("#o789");

        Assertions.assertNull(result);
    }

    @Test
    void parseHexWithInvalidCharacterReturnsNull() {
        final INumber result = BaseNumberParser.parseHex("#xDEADGEEF");

        Assertions.assertNull(result);
    }

    @Test
    void parseBinaryWithOnlyLowercaseUnsignedSuffixAfterPrefixReturnsNull() {
        final INumber result = BaseNumberParser.parseBinary("#bu");

        Assertions.assertNull(result);
    }

    @Test
    void parseHexWithOnlyUppercaseUnsignedSuffixAfterPrefixReturnsNull() {
        final INumber result = BaseNumberParser.parseHex("#xU");

        Assertions.assertNull(result);
    }

    @Test
    void parseHexWithUnsignedOverflowReturnsNull() {
        final INumber result = BaseNumberParser.parseHex("#x10000000000000000u");

        Assertions.assertNull(result);
    }

    @Test
    void parseHexWithSignedOverflowReturnsNull() {
        final INumber result = BaseNumberParser.parseHex("#x8000000000000000");

        Assertions.assertNull(result);
    }
}
