package org.dersbian.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link PathUtils#truncatePath(Path, int)}.
 *
 * <p>Covers null and invalid argument handling, paths that are already within the requested depth,
 * paths that must be truncated, empty paths, root paths, absolute paths, and paths containing
 * dot-segments ({@code .} / {@code ..}).
 */
@SuppressWarnings({
    "PMD.UnitTestAssertionsShouldIncludeMessage",
    "PMD.TooManyMethods",
    "PMD.AtLeastOneConstructor"
})
class PathUtilsTest {

    @Test
    void truncatePathThrowsNullPointerExceptionForNullPath() {
        assertThrows(NullPointerException.class, () -> PathUtils.truncatePath(null, 1));
    }

    @ParameterizedTest(name = "depth={0}")
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void truncatePathThrowsIllegalArgumentExceptionForNonPositiveDepth(final int depth) {
        assertThrows(
                IllegalArgumentException.class, () -> PathUtils.truncatePath(Path.of("a"), depth));
    }

    @ParameterizedTest(name = "[{index}] path={0}, depth={1}")
    @MethodSource("pathsThatShouldRemainUnchanged")
    void truncatePathReturnsOriginalPathWhenDepthIsSufficient(final Path path, final int depth) {
        assertEquals(path.toString(), PathUtils.truncatePath(path, depth));
    }

    private static Stream<Arguments> pathsThatShouldRemainUnchanged() {
        return Stream.of(
                arguments(Path.of("file.txt"), 1),
                arguments(Path.of("dir", "file.txt"), 2),
                arguments(Path.of("dir", "subdir"), 5),
                arguments(Path.of("dir", "subdir"), Integer.MAX_VALUE));
    }

    @Test
    void truncatePathReturnsEmptyPathUnchanged() {
        final Path emptyPath = Path.of("");

        assertEquals(emptyPath.toString(), PathUtils.truncatePath(emptyPath, 1));
    }

    @ParameterizedTest(name = "[{index}] path={0}, depth={1}, expected={2}")
    @MethodSource("pathsThatShouldBeTruncated")
    void truncatePathReturnsLastDepthElementsWhenPathIsDeeper(
            final Path path, final int depth, final String expected) {
        assertEquals(expected, PathUtils.truncatePath(path, depth));
    }

    private static Stream<Arguments> pathsThatShouldBeTruncated() {
        return Stream.of(
                arguments(Path.of("a", "b"), 1, Path.of("..", "b").toString()),
                arguments(Path.of("a", "b", "c"), 1, Path.of("..", "c").toString()),
                arguments(Path.of("a", "b", "c"), 2, Path.of("..", "b", "c").toString()),
                arguments(Path.of("a", "b", "c", "d"), 3, Path.of("..", "b", "c", "d").toString()));
    }

    @Test
    void truncatePathReturnsRootPathUnchanged() {
        final Path root = defaultRoot();

        assertEquals(root.toString(), PathUtils.truncatePath(root, 1));
    }

    @Test
    void truncatePathReturnsAbsolutePathUnchangedWhenDepthIsSufficient() {
        final Path path = defaultRoot().resolve(Path.of("alpha", "beta"));

        assertEquals(path.toString(), PathUtils.truncatePath(path, 2));
    }

    @Test
    void truncatePathTruncatesAbsolutePathWhenNeeded() {
        final Path path = defaultRoot().resolve(Path.of("alpha", "beta", "gamma"));

        assertEquals(Path.of("..", "beta", "gamma").toString(), PathUtils.truncatePath(path, 2));
    }

    @Test
    void truncatePathDoesNotNormalizeDotSegmentsBeforeTruncating() {
        final Path path = Path.of("a", ".", "b", "..", "c");

        assertEquals(Path.of("..", "..", "c").toString(), PathUtils.truncatePath(path, 2));
    }

    private static Path defaultRoot() {
        final Path root = Path.of("").toAbsolutePath().getRoot();
        assertNotNull(root, "The default file system should provide a root for absolute paths");
        return root;
    }
}
