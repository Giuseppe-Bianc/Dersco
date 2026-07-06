package org.dersbian.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestAssertionsShouldIncludeMessage",
})
class FormattedSizeTest {

    @Test
    void formatsValueUsingStringFormatPattern() {
        final FormattedSize formatted = new FormattedSize(1.5, "KB");

        final String text = formatted.toString();

        assertEquals(String.format(Locale.ROOT, "%.2f %s", 1.5, "KB"), text);
    }

    @Test
    void formatsValueWithTwoDecimalsAndSuffix() {
        final FormattedSize formatted = new FormattedSize(1.5, "KB");

        final String text = formatted.toString();

        assertEquals("1.50 KB", text);
    }

    @Test
    void valueIsExposed() {
        final FormattedSize formatted = new FormattedSize(2.0, "MiB");

        assertEquals(2.0, formatted.value(), 1.0e-9);
    }

    @Test
    void suffixIsExposed() {
        final FormattedSize formatted = new FormattedSize(2.0, "MiB");

        assertEquals("MiB", formatted.suffix());
    }

    @Test
    void zeroValueStillRendersWithTwoDecimals() {
        final String text = new FormattedSize(0.0, "B").toString();

        assertEquals("0.00 B", text);
    }

    @Test
    void negativeValueFormatsWithLeadingMinus() {
        final String text = new FormattedSize(-3.25, "B").toString();

        assertEquals("-3.25 B", text);
    }
}
