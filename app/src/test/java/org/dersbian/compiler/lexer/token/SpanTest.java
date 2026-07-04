package org.dersbian.compiler.lexer.token;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
  "PMD.AtLeastOneConstructor",
  "PMD.CommentRequired",
  "PMD.UnitTestAssertionsShouldIncludeMessage"
})
class SpanTest {

  @Test
  void spanUsesHalfOpenOffsetsForLengthContainsAndExtraction() {
    final SourceLocation start = SourceLocation.create(1, 1, 0L);
    final SourceLocation end = SourceLocation.create(1, 6, 5L);
    final Span span = Span.create(start, end);

    Assertions.assertAll(
        () -> Assertions.assertEquals(5L, span.length()),
        () -> Assertions.assertFalse(span.isEmpty()),
        () -> Assertions.assertFalse(span.isMultiline()),
        () -> Assertions.assertTrue(span.contains(start)),
        () -> Assertions.assertFalse(span.contains(end)),
        () -> Assertions.assertEquals("alpha", span.extractFrom("alpha beta")),
        () -> Assertions.assertEquals("1:1-6", span.toString()));
  }

  @Test
  void pointSpanIsEmptyAndContainsNoCharacter() {
    final SourceLocation location = SourceLocation.create(3, 4, 12L);
    final Span span = Span.point(location);

    Assertions.assertAll(
        () -> Assertions.assertEquals(0L, span.length()),
        () -> Assertions.assertTrue(span.isEmpty()),
        () -> Assertions.assertFalse(span.contains(location)));
  }

  @Test
  void spansOverlapOnlyWhenTheyShareCharacters() {
    final Span first = Span.create(location(1, 1, 0L), location(1, 4, 3L));
    final Span touching = Span.create(location(1, 4, 3L), location(1, 7, 6L));
    final Span overlapping = Span.create(location(1, 3, 2L), location(1, 5, 4L));

    Assertions.assertAll(
        () -> Assertions.assertFalse(first.overlaps(touching)),
        () -> Assertions.assertTrue(first.overlaps(overlapping)));
  }

  @Test
  void mergeKeepsOuterStartAndEnd() {
    final Span first = Span.create(location(1, 5, 4L), location(1, 8, 7L));
    final Span second = Span.create(location(1, 1, 0L), location(2, 3, 11L));
    final Span merged = first.merge(second);

    Assertions.assertAll(
        () -> Assertions.assertEquals(second.start(), merged.start()),
        () -> Assertions.assertEquals(second.end(), merged.end()),
        () -> Assertions.assertTrue(merged.isMultiline()),
        () -> Assertions.assertEquals("1:1-2:3", merged.toString()));
  }

  @Test
  void spanRejectsEndBeforeStart() {
    Assertions.assertThrows(
        IllegalArgumentException.class,
        () -> Span.create(SourceLocation.create(1, 4, 3L), SourceLocation.create(1, 1, 0L)));
  }

  private static SourceLocation location(final int line, final int column, final long offset) {
    return SourceLocation.create(line, column, offset);
  }
}
