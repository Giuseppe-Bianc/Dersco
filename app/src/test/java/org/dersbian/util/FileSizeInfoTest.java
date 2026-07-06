package org.dersbian.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestAssertionsShouldIncludeMessage",
    "PMD.TooManyMethods",
    "PMD.UnitTestContainsTooManyAsserts"
})
class FileSizeInfoTest {

    @Test
    void bytesAccessorReturnsTheStoredValue() {
        final FileSizeInfo info = new FileSizeInfo(42L);

        assertEquals(42L, info.bytes());
    }

    @Test
    void zeroBytesFormatsAtIndexZero() {
        final FileSizeInfo info = new FileSizeInfo(0L);

        assertFormatted(SizeSystems.SI_SYSTEM, info, 0.0, "B");
        assertFormatted(SizeSystems.IEC, info, 0.0, "B");
    }

    @Test
    void subBaseValueStaysAtIndexZero() {
        final FileSizeInfo info = new FileSizeInfo(999L);

        assertFormatted(SizeSystems.SI_SYSTEM, info, 999.0, "B");
        assertFormatted(SizeSystems.IEC, info, 999.0, "B");
    }

    @Test
    void oneKilobyteFormatsAsOneKilobyteInSi() {
        final FileSizeInfo info = new FileSizeInfo(1_000L);

        assertFormatted(SizeSystems.SI_SYSTEM, info, 1.0, "KB");
    }

    @Test
    void oneKibibyteFormatsAsOneKibibyteInIec() {
        final FileSizeInfo info = new FileSizeInfo(1_024L);

        assertFormatted(SizeSystems.IEC, info, 1.0, "KiB");
    }

    @Test
    void boundaryValueExactlyAtBaseStaysAtIndexZero() {
        final FileSizeInfo info = new FileSizeInfo(1_000L);

        assertEquals("KB", info.format(SizeSystems.SI_SYSTEM).suffix());
    }

    @Test
    void progressesThroughPrefixesInSi() {
        final long kilobyte = 1_000L;
        final long megabyte = 1_000L * kilobyte;
        final long gigabyte = 1_000L * megabyte;
        final long terabyte = 1_000L * gigabyte;
        final long petabyte = 1_000L * terabyte;

        assertSuffix(SizeSystems.SI_SYSTEM, kilobyte, "KB");
        assertSuffix(SizeSystems.SI_SYSTEM, megabyte, "MB");
        assertSuffix(SizeSystems.SI_SYSTEM, gigabyte, "GB");
        assertSuffix(SizeSystems.SI_SYSTEM, terabyte, "TB");
        assertSuffix(SizeSystems.SI_SYSTEM, petabyte, "PB");
    }

    @Test
    void progressesThroughPrefixesInIec() {
        final long kibibyte = 1_024L;
        final long mebibyte = 1_024L * kibibyte;
        final long gibibyte = 1_024L * mebibyte;
        final long tebibyte = 1_024L * gibibyte;
        final long pebibyte = 1_024L * tebibyte;

        assertSuffix(SizeSystems.IEC, kibibyte, "KiB");
        assertSuffix(SizeSystems.IEC, mebibyte, "MiB");
        assertSuffix(SizeSystems.IEC, gibibyte, "GiB");
        assertSuffix(SizeSystems.IEC, tebibyte, "TiB");
        assertSuffix(SizeSystems.IEC, pebibyte, "PiB");
    }

    @Test
    void valueAboveMaxPrefixStaysAtLastIndex() {
        final long pebibyteInBytes = 1_024L * 1_024L * 1_024L * 1_024L * 1_024L;
        final long beyondMaxPrefix = pebibyteInBytes * 1_024L;
        final FileSizeInfo info = new FileSizeInfo(beyondMaxPrefix);

        assertFormatted(SizeSystems.IEC, info, 1024.0, "PiB");
    }

    @Test
    void negativeBytesIsHandledAsValue() {
        final FileSizeInfo info = new FileSizeInfo(-1L);

        assertFormatted(SizeSystems.SI_SYSTEM, info, -1.0, "B");
    }

    @Test
    void formatRejectsNullSystem() {
        final FileSizeInfo info = new FileSizeInfo(1L);

        final NullPointerException exception =
                assertThrows(NullPointerException.class, () -> info.format(null));

        assertNotNull(exception.getMessage());
        assertTrue(exception.getMessage().toLowerCase(Locale.getDefault()).contains("sys"));
    }

    // Helper methods are intentionally NOT annotated with @Test, so
    // PMD's UnitTestContainsTooManyAsserts rule does not flag the
    // callers even though multiple properties are checked here.

    private static void assertFormatted(
            final SizeSystem system,
            final FileSizeInfo info,
            final double expectedValue,
            final String expectedSuffix) {
        final FormattedSize formatted = info.format(system);

        assertEquals(expectedValue, formatted.value(), 1.0e-9);
        assertEquals(expectedSuffix, formatted.suffix());
    }

    private static void assertSuffix(
            final SizeSystem system, final long bytes, final String expectedSuffix) {
        assertEquals(expectedSuffix, new FileSizeInfo(bytes).format(system).suffix());
    }
}
