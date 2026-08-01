package org.dersbian.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Locale;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestAssertionsShouldIncludeMessage"
})
class FileSizeReportTest {

    @Test
    void constructorRejectsNullInfo() {
        assertThrows(
                NullPointerException.class,
                () -> new FileSizeReport(null, SizeSystems.SI_SYSTEM, SizeSystems.IEC));
    }

    @Test
    void constructorRejectsNullSiSystem() {
        assertThrows(
                NullPointerException.class,
                () -> new FileSizeReport(new FileSizeInfo(1L), null, SizeSystems.IEC));
    }

    @Test
    void constructorRejectsNullIecSystem() {
        assertThrows(
                NullPointerException.class,
                () -> new FileSizeReport(new FileSizeInfo(1L), SizeSystems.SI_SYSTEM, null));
    }

    @Test
    void makePairUsesBothSizeSystemsForTheSameSnapshot() {
        final FileSizeInfo info = new FileSizeInfo(1_536L);
        final FileSizeReport report =
                new FileSizeReport(info, SizeSystems.SI_SYSTEM, SizeSystems.IEC);

        assertEquals(
                new FormattedSizePair(
                        info.format(SizeSystems.SI_SYSTEM), info.format(SizeSystems.IEC)),
                report.makePair());
    }

    @Test
    void rendersTheExpectedReportForZeroBytes() {
        final FileSizeReport report =
                new FileSizeReport(new FileSizeInfo(0L), SizeSystems.SI_SYSTEM, SizeSystems.IEC);

        assertEquals(
                expectedReport(0L, new FormattedSize(0.0, "B"), new FormattedSize(0.0, "B")),
                report.toString());
    }

    @Test
    void rendersNegativeBytesUsingUnsignedRepresentation() {
        final FileSizeReport report =
                new FileSizeReport(new FileSizeInfo(-1L), SizeSystems.SI_SYSTEM, SizeSystems.IEC);

        assertEquals(
                expectedReport(-1L, new FormattedSize(-1.0, "B"), new FormattedSize(-1.0, "B")),
                report.toString());
    }

    private static String expectedReport(
            final long bytes, final FormattedSize siSize, final FormattedSize iecSize) {
        final String separator = "-".repeat(41);
        final String bytesLine =
                String.format(Locale.ROOT, "Bytes : %s", Long.toUnsignedString(bytes));
        final String headerLine = String.format(Locale.ROOT, "%-20s %-20s", "SI", "IEC");

        return bytesLine
                + '\n'
                + separator
                + '\n'
                + headerLine
                + '\n'
                + separator
                + '\n'
                + new FormattedSizePair(siSize, iecSize);
    }
}
