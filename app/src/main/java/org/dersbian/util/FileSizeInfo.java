package org.dersbian.util;

/**
 * Carries a raw byte count and provides formatting against a {@link SizeSystem}.
 *
 * @param bytes number of bytes represented by this value
 */
public record FileSizeInfo(long bytes) {

    /** Maximum reachable index in the prefixes array (0..5). */
    private static final int MAX_PREFIX_INDEX = 5;

    /**
     * Formats the byte count using the given size system.
     *
     * <p>The value starts in bytes and is repeatedly divided by the system base while it is at
     * least the base. At most five divisions are performed, so the selected prefix never exceeds
     * index {@code 5}.
     *
     * @param sys the size system to apply
     * @return the normalized value and the corresponding prefix
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
