package org.dersbian.util;

import java.util.Locale;
import java.util.Objects;

/**
 * Combines a byte-count snapshot with the SI and IEC size systems used to format it.
 *
 * @param info the size snapshot to report
 * @param siSys the SI (decimal) size system
 * @param iecSys the IEC (binary) size system
 */
public record FileSizeReport(FileSizeInfo info, SizeSystem siSys, SizeSystem iecSys) {

    /**
     * Compact constructor: enforces non-null record components.
     *
     * @throws NullPointerException if any record component is {@code null}
     */
    public FileSizeReport {
        Objects.requireNonNull(info, "info must not be null");
        Objects.requireNonNull(siSys, "siSys must not be null");
        Objects.requireNonNull(iecSys, "iecSys must not be null");
    }

    /**
     * Builds the SI/IEC formatted pair for {@link #info}.
     *
     * @return the pair containing the values formatted with {@link #siSys} and {@link #iecSys}, in
     *     that order
     */
    public FormattedSizePair makePair() {
        return new FormattedSizePair(info.format(siSys), info.format(iecSys));
    }

    @Override
    public String toString() {
        final FormattedSizePair pair = makePair();
        final String separator = "-".repeat(41);

        // Use %n for platform-specific line separator (fixes
        // VA_FORMAT_STRING_USES_NEWLINE)
        final String bytesLine =
                String.format(
                        Locale.getDefault(), "Bytes : %s%n", Long.toUnsignedString(info.bytes()));
        final String headerLine = String.format(Locale.getDefault(), "%-20s %-20s%n", "SI", "IEC");

        return bytesLine + separator + '\n' + headerLine + separator + '\n' + pair; // no trailing
        // '\n',
        // matching the
        // original
    }
}
