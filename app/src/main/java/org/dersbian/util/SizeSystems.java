package org.dersbian.util;

import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Catalog of the built-in size systems used by the compiler reporting layer. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SizeSystems {
    /** Decimal system (SI), base 1000. */
    public static final SizeSystem SI_SYSTEM =
            new SizeSystem("SI", 1000.0, List.of("B", "KB", "MB", "GB", "TB", "PB"));

    /** Binary system (IEC), base 1024. */
    public static final SizeSystem IEC =
            new SizeSystem("IEC", 1024.0, List.of("B", "KiB", "MiB", "GiB", "TiB", "PiB"));
}
