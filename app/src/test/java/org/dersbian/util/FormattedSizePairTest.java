package org.dersbian.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestAssertionsShouldIncludeMessage",
    "PMD.UnitTestContainsTooManyAsserts"
})
class FormattedSizePairTest {

    @Test
    void exposesBothFormattedSizes() {
        final FormattedSize siFormattedSize = new FormattedSize(1.0, "KB");
        final FormattedSize iecFormattedSize = new FormattedSize(1.0, "KiB");
        final FormattedSizePair pair = new FormattedSizePair(siFormattedSize, iecFormattedSize);

        assertEquals(siFormattedSize, pair.siSize());
        assertEquals(iecFormattedSize, pair.iecSize());
    }

    @Test
    void alignsBothColumnsToTwentyChars() {
        final FormattedSizePair pair =
                new FormattedSizePair(new FormattedSize(1.0, "KB"), new FormattedSize(1.0, "KiB"));

        final String text = pair.toString();
        final String expected = String.format(Locale.ROOT, "%-20s %-20s", "1.00 KB", "1.00 KiB");

        assertEquals(expected, text);
    }

    @Test
    void padsShortSuffixes() {
        final FormattedSizePair pair =
                new FormattedSizePair(new FormattedSize(0.5, "B"), new FormattedSize(0.5, "B"));

        final String text = pair.toString();
        final String expected = String.format(Locale.ROOT, "%-20s %-20s", "0.50 B", "0.50 B");

        assertEquals(expected, text);
    }
}
