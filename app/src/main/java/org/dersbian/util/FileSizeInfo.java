package org.dersbian.util;

/** Carries a raw byte count and formats it against a {@link SizeSystem}. */
public record FileSizeInfo(long bytes) {

    /** Maximum reachable index in the prefixes array (0..5). */
    private static final int MAX_PREFIX_INDEX = 5;

    /**
     * Formats the byte count using the given size system.
     *
     * @param sys the size system to apply
     * @return the formatted size
     */
    public FormattedSize format(final SizeSystem sys) {
        double value = bytes;
        int index = 0;

        while (index < MAX_PREFIX_INDEX && value >= sys.base()) {
            value /= sys.base();
            index++;
        }

        return new FormattedSize(value, sys.prefixes().get(index));
    }
}
