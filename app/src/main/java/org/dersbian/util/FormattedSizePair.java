package org.dersbian.util;

import java.util.Locale;

/** Pairs the SI and IEC {@link FormattedSize}s of the same byte count. */
public record FormattedSizePair(FormattedSize siSize, FormattedSize iecSize) {

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%-20s %-20s", siSize.toString(), iecSize.toString());
    }
}
