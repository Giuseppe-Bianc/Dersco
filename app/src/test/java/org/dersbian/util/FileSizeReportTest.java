package org.dersbian.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}
