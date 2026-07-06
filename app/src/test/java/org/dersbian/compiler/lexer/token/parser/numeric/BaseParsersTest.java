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
class BaseParsersTest {

    @Test
    void parseBinarySupportsUnsignedSuffix() {
        final INumber parsed = BaseParsers.parseBinary("#b1010u");
        final INumber.IntegerValue value =
                Assertions.assertInstanceOf(INumber.IntegerValue.class, parsed);

        Assertions.assertAll(
                () -> Assertions.assertEquals(10L, value.value()),
                () -> Assertions.assertEquals("u", value.suffix()));
    }

    @Test
    void parseOctalReturnsIntegerValue() {
        final INumber parsed = BaseParsers.parseOctal("#o755");
        final INumber.IntegerValue value =
                Assertions.assertInstanceOf(INumber.IntegerValue.class, parsed);

        Assertions.assertAll(
                () -> Assertions.assertEquals(493L, value.value()),
                () -> Assertions.assertNull(value.suffix()));
    }

    @Test
    void parseHexAcceptsUppercaseDigits() {
        final INumber parsed = BaseParsers.parseHex("#xBEEF");
        final INumber.IntegerValue value =
                Assertions.assertInstanceOf(INumber.IntegerValue.class, parsed);

        Assertions.assertEquals(48_879L, value.value());
    }
}
