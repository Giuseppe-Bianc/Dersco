package org.dersbian.util;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Locale;
import java.util.Objects;

/**
 * A report combining snapshot with the SI and IEC sizes used to format it.
 *
 * @param info the size snapshot to report
 * @param siSys the SI (decimal) size system
 * @param iecSys the IEC (binary) size system
 */
public record FileSizeReport(FileSizeInfo info, SizeSystem siSys, SizeSystem iecSys) {

    /** Compact constructor: enforces non-null record components. */
    public FileSizeReport {
        Objects.requireNonNull(info, "info must not be null");
        Objects.requireNonNull(siSys, "siSys must not be null");
        Objects.requireNonNull(iecSys, "iecSys must not be null");
    }

    /**
     * Builds the SI/IEC formatted pair for {@link #info}.
     *
     * @return the pair of formatted values
     */
    public FormattedSizePair makePair() {
        return new FormattedSizePair(info.format(siSys), info.format(iecSys));
    }

    @Override
    @SuppressFBWarnings(
            value = "VA_FORMAT_STRING_USES_NEWLINE",
            justification = "Literal \\n is intentional: matches the original C++ source output.")
    public String toString() {
        final FormattedSizePair pair = makePair();
        final String separator = "-".repeat(41);
        final String bytesLine =
                String.format(
                        Locale.getDefault(), "Bytes : %s\n", Long.toUnsignedString(info.bytes()));
        final String headerLine = String.format(Locale.getDefault(), "%-20s %-20s\n", "SI", "IEC");

        return bytesLine + separator + '\n' + headerLine + separator + '\n' + pair; // no trailing
        // '\n',
        // matching the
        // original
    }
}
