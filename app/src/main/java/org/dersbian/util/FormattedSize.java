package org.dersbian.util;

import java.util.Locale;

/**
 * Represents a size value together with the unit suffix selected by a {@link SizeSystem}.
 *
 * @param value normalized numeric value for the selected unit
 * @param suffix unit suffix associated with the value
 */
public record FormattedSize(double value, String suffix) {

    /**
     * Returns the value formatted with two fractional digits and its unit suffix.
     *
     * @return a string in the form {@code "%.2f suffix"}, using {@link Locale#ROOT}
     */
    @Override
    public String toString() {
        return String.format(Locale.ROOT, "%.2f %s", value, suffix);
    }
}
