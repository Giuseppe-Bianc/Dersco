package org.dersbian.compiler.lexer.token;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestAssertionsShouldIncludeMessage"
})
class SourceLocationTest {

    @Test
    void createBuildsMinimalLocationWithUnknownOptionalOffsets() {
        final SourceLocation location = SourceLocation.create(2, 5, 12L);

        Assertions.assertAll(
                () -> Assertions.assertEquals(2, location.line()),
                () -> Assertions.assertEquals(5, location.column()),
                () -> Assertions.assertEquals(12L, location.offset()),
                () -> Assertions.assertEquals(12, location.index()),
                () -> Assertions.assertEquals(SourceLocation.UNKNOWN, location.utf8Offset()),
                () -> Assertions.assertEquals(SourceLocation.UNKNOWN, location.codePointOffset()),
                () -> Assertions.assertFalse(location.hasUtf8Offset()),
                () -> Assertions.assertFalse(location.hasCodePointOffset()),
                () -> Assertions.assertEquals("line 2:column 5", location.toString()));
    }

    @Test
    void optionalOffsetsCanBeSetWithoutChangingTextPosition() {
        final SourceLocation location =
                SourceLocation.create(1, 3, 2L).withUtf8Offset(4L).withCodePointOffset(2L);

        Assertions.assertAll(
                () -> Assertions.assertEquals(1, location.line()),
                () -> Assertions.assertEquals(3, location.column()),
                () -> Assertions.assertEquals(2L, location.offset()),
                () -> Assertions.assertEquals(4L, location.utf8Offset()),
                () -> Assertions.assertEquals(2L, location.codePointOffset()),
                () -> Assertions.assertTrue(location.hasUtf8Offset()),
                () -> Assertions.assertTrue(location.hasCodePointOffset()));
    }

    @Test
    void invalidCoordinatesAreRejected() {
        Assertions.assertAll(
                () ->
                        Assertions.assertThrows(
                                IllegalArgumentException.class,
                                () -> SourceLocation.create(0, 1, 0L)),
                () ->
                        Assertions.assertThrows(
                                IllegalArgumentException.class,
                                () -> SourceLocation.create(1, 0, 0L)),
                () ->
                        Assertions.assertThrows(
                                IllegalArgumentException.class,
                                () -> SourceLocation.create(1, 1, -1L)),
                () ->
                        Assertions.assertThrows(
                                IllegalArgumentException.class,
                                () ->
                                        SourceLocation.create(
                                                1, 1, 0L, 0, -2L, SourceLocation.UNKNOWN)));
    }

    @Test
    void locationsAreOrderedByOffsetOnly() {
        final SourceLocation first = SourceLocation.create(10, 1, 3L);
        final SourceLocation second = SourceLocation.create(1, 20, 9L);

        Assertions.assertTrue(first.compareTo(second) < 0);
    }
}
