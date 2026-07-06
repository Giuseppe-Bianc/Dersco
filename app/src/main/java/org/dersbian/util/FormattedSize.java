package org.dersbian.util;

import java.util.Locale;

/** A normalized size value paired with its unit suffix. */
public record FormattedSize(double value, String suffix) {

    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%.2f %s", value, suffix);
    }
}
