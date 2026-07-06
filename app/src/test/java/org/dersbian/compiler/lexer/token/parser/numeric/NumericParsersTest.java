package org.dersbian.compiler.lexer.token.parser.numeric;

import org.dersbian.compiler.lexer.token.number.INumber;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestAssertionsShouldIncludeMessage",
    "PMD.UnitTestContainsTooManyAsserts"
})
class NumericParsersTest {

    @Test
    void parseIntegerWithoutSuffix() {
        final INumber parsed = NumericParsers.parseNumber("42");
        final INumber.IntegerValue value =
                Assertions.assertInstanceOf(INumber.IntegerValue.class, parsed);

        Assertions.assertAll(
                () -> Assertions.assertEquals(42L, value.value()),
                () -> Assertions.assertNull(value.suffix()));
    }

    @Test
    void parseIntegerWithMultiCharacterSuffix() {
        final INumber parsed = NumericParsers.parseNumber("255u32");
        final INumber.IntegerValue value =
                Assertions.assertInstanceOf(INumber.IntegerValue.class, parsed);

        Assertions.assertAll(
                () -> Assertions.assertEquals(255L, value.value()),
                () -> Assertions.assertEquals("u32", value.suffix()));
    }

    @Test
    void parseDecimalFloatingPoint() {
        final INumber parsed = NumericParsers.parseNumber("3.5f");
        final INumber.FloatingValue value =
                Assertions.assertInstanceOf(INumber.FloatingValue.class, parsed);

        Assertions.assertAll(
                () -> Assertions.assertEquals(3.5D, value.value()),
                () -> Assertions.assertEquals("f", value.suffix()));
    }

    @Test
    void parseExponentFloatingPoint() {
        final INumber parsed = NumericParsers.parseNumber("1e3");
        final INumber.FloatingValue value =
                Assertions.assertInstanceOf(INumber.FloatingValue.class, parsed);

        Assertions.assertAll(
                () -> Assertions.assertEquals(1_000.0D, value.value()),
                () -> Assertions.assertNull(value.suffix()));
    }

    @Test
    void floatingSuffixMakesWholeNumberFloatingPoint() {
        final INumber parsed = NumericParsers.parseNumber("7f");
        final INumber.FloatingValue value =
                Assertions.assertInstanceOf(INumber.FloatingValue.class, parsed);

        Assertions.assertAll(
                () -> Assertions.assertEquals(7.0D, value.value()),
                () -> Assertions.assertEquals("f", value.suffix()));
    }
}
