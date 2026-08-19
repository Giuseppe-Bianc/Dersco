package org.dersbian.util;

import java.util.List;
import java.util.Objects;

/**
 * Describes a size system by its name, numeric base, and ordered list of unit prefixes.
 *
 * <p>The prefix list must contain exactly six entries. The base must be at least {@code 1.0}; the
 * list is defensively copied by the compact constructor so subsequent changes to the input list do
 * not affect the record.
 *
 * @param name name of the size system
 * @param base numeric factor used between successive prefixes
 * @param prefixes ordered unit prefixes, from index {@code 0} through {@code 5}
 */
public record SizeSystem(String name, double base, List<String> prefixes) {

    /** Expected number of prefixes (indices 0..5). */
    private static final int PREFIX_COUNT = 6;

    /** Minimum accepted value for {@link #base}. */
    private static final double EXPECTED_BASE = 1.0;

    /**
     * Compact constructor: validates the record components and applies an immutable defensive copy
     * of the prefixes list.
     *
     * @throws NullPointerException if {@code name} or {@code prefixes} is {@code null}
     * @throws IllegalArgumentException if {@code prefixes} does not contain exactly six elements or
     *     if {@code base} is less than {@code 1.0}
     */
    public SizeSystem {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(prefixes, "prefixes must not be null");
        if (prefixes.size() != PREFIX_COUNT) {
            throw new IllegalArgumentException(
                    "prefixes must contain exactly "
                            + PREFIX_COUNT
                            + " elements, found: "
                            + prefixes.size());
        }
        if (base < EXPECTED_BASE) {
            throw new IllegalArgumentException("base must be > 1, found: " + base);
        }
        prefixes = List.copyOf(prefixes);
    }
}
