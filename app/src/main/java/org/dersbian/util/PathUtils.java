package org.dersbian.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Utility methods for working with {@link Path} instances. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PathUtils {

    /**
     * Truncates a path so that only the last {@code depth} name elements are preserved.
     *
     * <p>If the path contains at most {@code depth} components, the original path is returned
     * unchanged. Otherwise, the returned path starts with {@code ".."} followed by the last {@code
     * depth} components.
     *
     * @param path the original path, must not be {@code null}
     * @param depth number of trailing components to preserve
     * @return the truncated path
     * @throws NullPointerException if {@code path} is {@code null}
     */
    public static String truncatePath(final Path path, final int depth) {
        Objects.requireNonNull(path, "path");

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
