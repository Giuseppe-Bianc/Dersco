package org.dersbian.util;

import java.util.List;
import java.util.Objects;

/** Describes a size system: a name, a base, and the ordered list of prefixes. */
public record SizeSystem(String name, double base, List<String> prefixes) {

    /** Expected number of prefixes (indices 0..5). */
    private static final int PREFIX_COUNT = 6;

    /** Minimum accepted value for {@link #base}. */
    private static final double EXPECTED_BASE = 1.0;

    /** Compact constructor: applies an immutable defensive copy of the prefixes list. */
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
