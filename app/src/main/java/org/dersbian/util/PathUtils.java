package org.dersbian.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

/** Utility methods for working with {@link Path} instances. */
public final class PathUtils {

    private PathUtils() {}

    /**
     * Truncates a path so that only the last {@code depth} name elements are preserved.
     *
     * <p>If the path contains at most {@code depth} components, its string representation is
     * returned unchanged. Otherwise, the result consists of {@code ".."} followed by the last
     * {@code depth} components.
     *
     * <p>The {@code depth} value must be positive. A non-positive value is rejected before any path
     * components are inspected.
     *
     * @param path the original path, must not be {@code null}
     * @param depth number of trailing components to preserve, must be positive
     * @return the truncated path as a string
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws IllegalArgumentException if {@code depth} is not positive
     */
    public static String truncatePath(final Path path, final int depth) {
        Objects.requireNonNull(path, "path");
        if (depth <= 0) {
            throw new IllegalArgumentException("depth must be positive: " + depth);
        }

        final int nameCount = path.getNameCount();
        final String result;

        if (nameCount <= depth) {
            result = path.toString();
        } else {
            final Path tail = path.subpath(nameCount - depth, nameCount);
            result = Paths.get("..").resolve(tail).toString();
        }

        return result;
    }
}
