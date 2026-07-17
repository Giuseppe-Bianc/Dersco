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
        final INumber parsed = BaseNumberParser.parseBinary("#b1010u");
        final INumber.UnsignedInteger value =
                Assertions.assertInstanceOf(INumber.UnsignedInteger.class, parsed);

        Assertions.assertEquals(10L, value.value());
    }

    @Test
    void parseOctalReturnsIntegerValue() {
        final INumber parsed = BaseNumberParser.parseOctal("#o755");
        final INumber.Integer value = Assertions.assertInstanceOf(INumber.Integer.class, parsed);

        Assertions.assertEquals(493L, value.value());
    }

    @Test
    void parseHexAcceptsUppercaseDigits() {
        final INumber parsed = BaseNumberParser.parseHex("#xBEEF");
        final INumber.Integer value = Assertions.assertInstanceOf(INumber.Integer.class, parsed);

        Assertions.assertEquals(48_879L, value.value());
    }
}
