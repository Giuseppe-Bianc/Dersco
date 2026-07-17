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
        final INumber parsed = NumericParser.parseNumber("42");
        final INumber.Integer value = Assertions.assertInstanceOf(INumber.Integer.class, parsed);

        Assertions.assertEquals(42L, value.value());
    }

    @Test
    void parseIntegerWithMultiCharacterSuffix() {
        final INumber parsed = NumericParser.parseNumber("255u32");
        final INumber.U32 value = Assertions.assertInstanceOf(INumber.U32.class, parsed);

        Assertions.assertEquals(255L, value.value());
    }

    @Test
    void parseDecimalFloatingPoint() {
        final INumber parsed = NumericParser.parseNumber("3.5f");
        final INumber.Float32 value = Assertions.assertInstanceOf(INumber.Float32.class, parsed);

        Assertions.assertEquals(3.5D, value.value());
    }

    @Test
    void parseExponentFloatingPoint() {
        final INumber parsed = NumericParser.parseNumber("1e3");
        final INumber.Scientific64 value =
                Assertions.assertInstanceOf(INumber.Scientific64.class, parsed);

        Assertions.assertEquals(1.0D, value.base());
        Assertions.assertEquals(3.0D, value.exponent());
    }

    @Test
    void floatingSuffixMakesWholeNumberFloatingPoint() {
        final INumber parsed = NumericParser.parseNumber("7f");
        final INumber.Float32 value = Assertions.assertInstanceOf(INumber.Float32.class, parsed);

        Assertions.assertEquals(7.0D, value.value());
    }
}
