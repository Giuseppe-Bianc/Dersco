package org.dersbian.compiler.lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Tests for {@link SourceCursor}.
 *
 * <p>Covers basic traversal, UTF-16 surrogate pair handling, line/column tracking across all line
 * terminator styles, peek operations, match operations, edge cases (empty source, single character,
 * end-of-source), and corner cases (unpaired surrogates, mixed BMP and supplementary characters,
 * consecutive line terminators).
 */
@SuppressWarnings({
    "checkstyle:AvoidEscapedUnicodeCharacters",
    "PMD.TooManyMethods",
    "PMD.UnitTestAssertionsShouldIncludeMessage",
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.ShortVariable",
    "PMD.GodClass"
})
@NoArgsConstructor(access = AccessLevel.PRIVATE)
class SourceCursorTest {
    /** Reusable three-character ASCII string used across multiple tests. */
    private static final String ABC = "abc";

    // ──────────────────────────────────────────────────────────────────
    // isAtEnd
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("isAtEnd returns true for empty source")
    void isAtEndReturnsTrueForEmptySource() {
        final SourceCursor cursor = new SourceCursor("");
        assertTrue(cursor.isAtEnd());
    }

    @Test
    @DisplayName("isAtEnd returns false when source has content")
    void isAtEndReturnsFalseWhenSourceHasContent() {
        final SourceCursor cursor = new SourceCursor("a");
        assertFalse(cursor.isAtEnd());
    }

    @Test
    @DisplayName("isAtEnd returns true after consuming all characters")
    void isAtEndReturnsTrueAfterConsumingAllCharacters() {
        final SourceCursor cursor = new SourceCursor("ab");
        cursor.advance();
        cursor.advance();
        assertTrue(cursor.isAtEnd());
    }

    @Test
    @DisplayName("isAtEnd returns true after consuming a surrogate pair")
    void isAtEndReturnsTrueAfterConsumingSurrogatePair() {
        final SourceCursor cursor = new SourceCursor("\uD83D\uDE00");
        assertFalse(cursor.isAtEnd());
        cursor.advance();
        assertTrue(cursor.isAtEnd());
    }

    // ──────────────────────────────────────────────────────────────────
    // peekCodePoint
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("peekCodePoint returns -1 for empty source")
    void peekCodePointReturnsMinusOneForEmptySource() {
        final SourceCursor cursor = new SourceCursor("");
        assertEquals(-1, cursor.peekCodePoint());
    }

    @Test
    @DisplayName("peekCodePoint returns first code point without consuming it")
    void peekCodePointReturnsFirstCodePointWithoutConsuming() {
        final SourceCursor cursor = new SourceCursor("hello");
        assertEquals('h', cursor.peekCodePoint());
        assertEquals('h', cursor.peekCodePoint());
    }

    @Test
    @DisplayName("peekCodePoint correctly decodes a supplementary code point")
    void peekCodePointDecodesSupplementaryCodePoint() {
        final int expected = 0x1F600;
        final SourceCursor cursor = new SourceCursor(new String(Character.toChars(expected)));
        assertEquals(expected, cursor.peekCodePoint());
    }

    @Test
    @DisplayName("peekCodePoint returns -1 after source is exhausted")
    void peekCodePointReturnsMinusOneAfterExhaustion() {
        final SourceCursor cursor = new SourceCursor("x");
        cursor.advance();
        assertEquals(-1, cursor.peekCodePoint());
    }

    // ──────────────────────────────────────────────────────────────────
    // peekNextCodePoint
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("peekNextCodePoint returns -1 for empty source")
    void peekNextCodePointReturnsMinusOneForEmptySource() {
        final SourceCursor cursor = new SourceCursor("");
        assertEquals(-1, cursor.peekNextCodePoint());
    }

    @Test
    @DisplayName("peekNextCodePoint returns -1 when only one character remains")
    void peekNextCodePointReturnsMinusOneForSingleChar() {
        final SourceCursor cursor = new SourceCursor("a");
        assertEquals(-1, cursor.peekNextCodePoint());
    }

    @Test
    @DisplayName("peekNextCodePoint returns second code point without consuming anything")
    void peekNextCodePointReturnsSecondCodePoint() {
        final SourceCursor cursor = new SourceCursor("ab");
        assertEquals('b', cursor.peekNextCodePoint());
        assertEquals('a', cursor.peekCodePoint());
    }

    @Test
    @DisplayName("peekNextCodePoint works when current is a supplementary code point")
    void peekNextCodePointAfterSupplementaryCodePoint() {
        final String source = "\uD83D\uDE00A";
        final SourceCursor cursor = new SourceCursor(source);
        assertEquals('A', cursor.peekNextCodePoint());
    }

    @Test
    @DisplayName("peekNextCodePoint works when next is a supplementary code point")
    void peekNextCodePointIsSupplementary() {
        final int supplementary = 0x1F601;
        final String source = "A" + new String(Character.toChars(supplementary));
        final SourceCursor cursor = new SourceCursor(source);
        assertEquals(supplementary, cursor.peekNextCodePoint());
    }

    @Test
    @DisplayName("peekNextCodePoint returns -1 when current is last supplementary code point")
    void peekNextCodePointReturnsMinusOneWhenSupplementaryIsLast() {
        final SourceCursor cursor = new SourceCursor("\uD83D\uDE00");
        assertEquals(-1, cursor.peekNextCodePoint());
    }

    @Test
    @DisplayName("peekNextCodePoint when both current and next are supplementary")
    void peekNextCodePointBothSupplementary() {
        final int cp1 = 0x10000;
        final int cp2 = 0x10FFFF;
        final String source =
                new String(Character.toChars(cp1)) + new String(Character.toChars(cp2));
        final SourceCursor cursor = new SourceCursor(source);
        assertEquals(cp1, cursor.peekCodePoint());
        assertEquals(cp2, cursor.peekNextCodePoint());
        assertEquals(cp1, cursor.peekCodePoint());
    }

    // ──────────────────────────────────────────────────────────────────
    // advance – basic behavior
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("advance returns -1 for empty source")
    void advanceReturnsMinusOneForEmptySource() {
        final SourceCursor cursor = new SourceCursor("");
        assertEquals(-1, cursor.advance());
    }

    @Test
    @DisplayName("advance returns each code point in sequence for ASCII text")
    void advanceReturnsCodePointsInSequence() {
        final SourceCursor cursor = new SourceCursor(ABC);
        assertEquals('a', cursor.advance());
        assertEquals('b', cursor.advance());
        assertEquals('c', cursor.advance());
        assertEquals(-1, cursor.advance());
    }

    @Test
    @DisplayName("advance returns -1 repeatedly after exhaustion")
    void advanceReturnsMinusOneRepeatedlyAfterExhaustion() {
        final SourceCursor cursor = new SourceCursor("x");
        cursor.advance();
        assertEquals(-1, cursor.advance());
        assertEquals(-1, cursor.advance());
    }

    @Test
    @DisplayName("advance correctly consumes supplementary code points")
    void advanceConsumesSupplementaryCodePoints() {
        final int cp1 = 0x1F600;
        final int cp2 = 0x1F601;
        final String source =
                new String(Character.toChars(cp1)) + new String(Character.toChars(cp2));
        final SourceCursor cursor = new SourceCursor(source);
        assertEquals(cp1, cursor.advance());
        assertEquals(cp2, cursor.advance());
        assertTrue(cursor.isAtEnd());
    }

    @Test
    @DisplayName("advance handles mixed BMP and supplementary code points")
    void advanceMixedBmpAndSupplementary() {
        final int emoji = 0x1F4A9;
        final String source = "A" + new String(Character.toChars(emoji)) + "B";
        final SourceCursor cursor = new SourceCursor(source);
        assertEquals('A', cursor.advance());
        assertEquals(emoji, cursor.advance());
        assertEquals('B', cursor.advance());
        assertTrue(cursor.isAtEnd());
    }

    // ──────────────────────────────────────────────────────────────────
    // advance – line/column tracking
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("advance increments column for regular characters")
    void advanceIncrementsColumnForRegularChars() {
        final SourceCursor cursor = new SourceCursor(ABC);
        cursor.advance();
        cursor.advance();
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(1, loc.line());
        assertEquals(3, loc.column());
    }

    @Test
    @DisplayName("advance increments line on LF")
    void advanceIncrementsLineOnLf() {
        final SourceCursor cursor = new SourceCursor("a\nb");
        cursor.advance();
        cursor.advance();
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(2, loc.line());
        assertEquals(1, loc.column());
    }

    @Test
    @DisplayName("advance increments line on standalone CR")
    void advanceIncrementsLineOnStandaloneCr() {
        final SourceCursor cursor = new SourceCursor("a\rb");
        cursor.advance();
        cursor.advance();
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(2, loc.line());
        assertEquals(1, loc.column());
    }

    @Test
    @DisplayName("advance treats CR LF as a single line break")
    void advanceTreatsCrLfAsSingleLineBreak() {
        final SourceCursor cursor = new SourceCursor("a\r\nb");
        cursor.advance(); // 'a'
        cursor.advance(); // '\r' – sees '\n' next, does NOT increment line
        cursor.advance(); // '\n' – increments line
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(2, loc.line());
        assertEquals(1, loc.column());
    }

    @Test
    @DisplayName("advance increments line on Unicode LINE SEPARATOR (U+2028)")
    void advanceIncrementsLineOnLineSeparator() {
        final SourceCursor cursor = new SourceCursor("a\u2028b");
        cursor.advance();
        cursor.advance();
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(2, loc.line());
        assertEquals(1, loc.column());
    }

    @Test
    @DisplayName("advance increments line on Unicode PARAGRAPH SEPARATOR (U+2029)")
    void advanceIncrementsLineOnParagraphSeparator() {
        final SourceCursor cursor = new SourceCursor("a\u2029b");
        cursor.advance();
        cursor.advance();
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(2, loc.line());
        assertEquals(1, loc.column());
    }

    @Test
    @DisplayName("advance tracks lines through multiple consecutive LFs")
    void advanceTracksConsecutiveLfs() {
        final SourceCursor cursor = new SourceCursor("\n\n\n");
        cursor.advance();
        cursor.advance();
        cursor.advance();
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(4, loc.line());
        assertEquals(1, loc.column());
    }

    @Test
    @DisplayName("advance tracks lines through multiple consecutive CRs")
    void advanceTracksConsecutiveCrs() {
        final SourceCursor cursor = new SourceCursor("\r\r\r");
        cursor.advance();
        cursor.advance();
        cursor.advance();
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(4, loc.line());
        assertEquals(1, loc.column());
    }

    @Test
    @DisplayName("advance tracks lines through multiple CRLF sequences")
    void advanceTracksMultipleCrLf() {
        final SourceCursor cursor = new SourceCursor("\r\n\r\n");
        cursor.advance(); // '\r'
        cursor.advance(); // '\n' -> line 2
        cursor.advance(); // '\r'
        cursor.advance(); // '\n' -> line 3
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(3, loc.line());
        assertEquals(1, loc.column());
    }

    @Test
    @DisplayName("advance handles CR at very end of source")
    void advanceHandlesCrAtEnd() {
        final SourceCursor cursor = new SourceCursor("\r");
        cursor.advance();
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(2, loc.line());
        assertEquals(1, loc.column());
    }

    @Test
    @DisplayName("advance handles LF at very end of source")
    void advanceHandlesLfAtEnd() {
        final SourceCursor cursor = new SourceCursor("\n");
        cursor.advance();
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(2, loc.line());
        assertEquals(1, loc.column());
    }

    @Test
    @DisplayName("CR followed by another CR counts as two separate line breaks")
    void crFollowedByCr() {
        final SourceCursor cursor = new SourceCursor("\r\r");
        cursor.advance();
        cursor.advance();
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(3, loc.line());
    }

    @Test
    @DisplayName("CR followed by non-LF character counts as line break")
    void crFollowedByNonLf() {
        final SourceCursor cursor = new SourceCursor("\ra");
        cursor.advance();
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(2, loc.line());
        assertEquals(1, loc.column());
        assertEquals('a', cursor.peekCodePoint());
    }

    @ParameterizedTest(name = "line terminator U+{0} increments line number")
    @ValueSource(strings = {"000A", "2028", "2029"})
    @DisplayName("all non-CR line terminators increment line number on advance")
    void allNonCrLineTerminatorsIncrementLine(final String hex) {
        final int cp = Integer.parseInt(hex, 16);
        final String source = "a" + new String(Character.toChars(cp)) + "b";
        final SourceCursor cursor = new SourceCursor(source);
        cursor.advance();
        cursor.advance();
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(2, loc.line());
        assertEquals(1, loc.column());
    }

    @Test
    @DisplayName("mixed line terminators track lines independently")
    void mixedLineTerminators() {
        // LF, CR, CRLF, LS, PS -> 5 line breaks -> line 6
        final String source = "\n\r\r\n\u2028\u2029";
        final SourceCursor cursor = new SourceCursor(source);
        cursor.advance(); // \n -> line 2
        cursor.advance(); // \r -> line 3 (next is \r, not \n)
        cursor.advance(); // \r -> does NOT increment (next is \n)
        cursor.advance(); // \n -> line 4
        cursor.advance(); // LS -> line 5
        cursor.advance(); // PS -> line 6
        assertTrue(cursor.isAtEnd());
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(6, loc.line());
        assertEquals(1, loc.column());
    }

    @Test
    @DisplayName("column increments correctly for supplementary code points")
    void columnIncrementsForSupplementary() {
        final String source = "A\uD83D\uDE00B";
        final SourceCursor cursor = new SourceCursor(source);
        cursor.advance(); // 'A' -> col 2
        cursor.advance(); // 😀 -> col 3
        cursor.advance(); // 'B' -> col 4
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(1, loc.line());
        assertEquals(4, loc.column());
    }

    @Test
    @DisplayName("advance does not confuse a non-newline control character with a line terminator")
    void advanceNonNewlineControlChar() {
        final SourceCursor cursor = new SourceCursor("\t\t");
        cursor.advance();
        cursor.advance();
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(1, loc.line());
        assertEquals(3, loc.column());
    }

    // ──────────────────────────────────────────────────────────────────
    // advance – UTF-8 offset tracking
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("advance calculates correct UTF-8 offset for ASCII text")
    void advanceUtf8OffsetAscii() {
        final SourceCursor cursor = new SourceCursor(ABC);
        cursor.advance();
        cursor.advance();
        cursor.advance();
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(3L, loc.utf8Offset());
    }

    @Test
    @DisplayName("advance calculates correct UTF-8 offset for multi-byte BMP characters")
    void advanceUtf8OffsetMultiByteBmp() {
        // U+00E9 (é) = 2 bytes, U+20AC (€) = 3 bytes
        final SourceCursor cursor = new SourceCursor("\u00E9\u20AC");
        cursor.advance();
        cursor.advance();
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(5L, loc.utf8Offset());
    }

    @Test
    @DisplayName("advance calculates correct UTF-8 offset for supplementary code point")
    void advanceUtf8OffsetSupplementary() {
        final SourceCursor cursor = new SourceCursor("\uD83D\uDE00");
        cursor.advance();
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(4L, loc.utf8Offset());
    }

    @ParameterizedTest(name = "UTF-8 byte length: U+{0} = {1} bytes")
    @CsvSource({
        "0041, 1", // 'A'
        "007F, 1", // DEL
        "0080, 2", // first 2-byte
        "07FF, 2", // last 2-byte
        "0800, 3", // first 3-byte
        "FFFF, 3", // last BMP (3 bytes)
    })
    @DisplayName("advance tracks UTF-8 offset correctly for various BMP code points")
    void advanceUtf8OffsetVariousBmp(final String hexCodePoint, final int expectedBytes) {
        final int cp = Integer.parseInt(hexCodePoint, 16);
        final String source = new String(Character.toChars(cp));
        final SourceCursor cursor = new SourceCursor(source);
        cursor.advance();
        assertEquals(expectedBytes, cursor.currentLocation().utf8Offset());
    }

    @Test
    @DisplayName("advance tracks UTF-8 offset for supplementary (4-byte) code point U+10000")
    void advanceUtf8OffsetSupplementary4Bytes() {
        final String source = new String(Character.toChars(0x10000));
        final SourceCursor cursor = new SourceCursor(source);
        cursor.advance();
        assertEquals(4L, cursor.currentLocation().utf8Offset());
    }

    // ──────────────────────────────────────────────────────────────────
    // advance – code point offset tracking
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("advance increments codePointOffset by 1 for each BMP character")
    void advanceCodePointOffsetBmp() {
        final SourceCursor cursor = new SourceCursor("ab");
        cursor.advance();
        cursor.advance();
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(2L, loc.codePointOffset());
    }

    @Test
    @DisplayName("advance increments codePointOffset by 1 for each supplementary character")
    void advanceCodePointOffsetSupplementary() {
        final String source = "\uD83D\uDE00\uD83D\uDE01";
        final SourceCursor cursor = new SourceCursor(source);
        cursor.advance();
        cursor.advance();
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(2L, loc.codePointOffset());
    }

    // ──────────────────────────────────────────────────────────────────
    // match(int)
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("match(int) returns true and advances when code point matches")
    void matchSingleReturnsTrue() {
        final SourceCursor cursor = new SourceCursor(ABC);
        assertTrue(cursor.match('a'));
        assertEquals('b', cursor.peekCodePoint());
    }

    @Test
    @DisplayName("match(int) returns false and does not advance when code point differs")
    void matchSingleReturnsFalse() {
        final SourceCursor cursor = new SourceCursor(ABC);
        assertFalse(cursor.match('x'));
        assertEquals('a', cursor.peekCodePoint());
    }

    @Test
    @DisplayName("match(int) returns false at end of source")
    void matchSingleReturnsFalseAtEnd() {
        final SourceCursor cursor = new SourceCursor("");
        assertFalse(cursor.match('a'));
    }

    @Test
    @DisplayName("match(int) works with supplementary code point")
    void matchSingleSupplementary() {
        final int cp = 0x1F600;
        final SourceCursor cursor = new SourceCursor(new String(Character.toChars(cp)));
        assertTrue(cursor.match(cp));
        assertTrue(cursor.isAtEnd());
    }

    @Test
    @DisplayName("match(int) does not modify state on mismatch at end")
    void matchSingleNoStateChangeOnMismatchAtEnd() {
        final SourceCursor cursor = new SourceCursor("");
        final SourceLocation before = cursor.currentLocation();
        assertFalse(cursor.match('a'));
        final SourceLocation after = cursor.currentLocation();
        assertEquals(before.line(), after.line());
        assertEquals(before.column(), after.column());
        assertEquals(before.utf8Offset(), after.utf8Offset());
        assertEquals(before.codePointOffset(), after.codePointOffset());
    }

    // ──────────────────────────────────────────────────────────────────
    // match(int, int)
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("match(int,int) returns true and advances two code points when both match")
    void matchDoubleReturnsTrue() {
        final SourceCursor cursor = new SourceCursor("//comment");
        assertTrue(cursor.match('/', '/'));
        assertEquals('c', cursor.peekCodePoint());
    }

    @Test
    @DisplayName("match(int,int) returns false when first does not match")
    void matchDoubleReturnsFalseFirstMismatch() {
        final SourceCursor cursor = new SourceCursor("x/comment");
        assertFalse(cursor.match('/', '/'));
        assertEquals('x', cursor.peekCodePoint());
    }

    @Test
    @DisplayName("match(int,int) returns false when second does not match")
    void matchDoubleReturnsFalseSecondMismatch() {
        final SourceCursor cursor = new SourceCursor("/xcomment");
        assertFalse(cursor.match('/', '/'));
        assertEquals('/', cursor.peekCodePoint());
    }

    @Test
    @DisplayName("match(int,int) returns false when only one character remains")
    void matchDoubleReturnsFalseOneCharRemaining() {
        final SourceCursor cursor = new SourceCursor("/");
        assertFalse(cursor.match('/', '/'));
        assertEquals('/', cursor.peekCodePoint());
    }

    @Test
    @DisplayName("match(int,int) returns false at end of source")
    void matchDoubleReturnsFalseAtEnd() {
        final SourceCursor cursor = new SourceCursor("");
        assertFalse(cursor.match('/', '/'));
    }

    @Test
    @DisplayName("match(int,int) works with supplementary code points")
    void matchDoubleSupplementary() {
        final int cp1 = 0x1F600;
        final int cp2 = 0x1F601;
        final String source =
                new String(Character.toChars(cp1)) + new String(Character.toChars(cp2));
        final SourceCursor cursor = new SourceCursor(source);
        assertTrue(cursor.match(cp1, cp2));
        assertTrue(cursor.isAtEnd());
    }

    @Test
    @DisplayName("match(int,int) with supplementary first and BMP second")
    void matchDoubleMixedSupplementaryBmp() {
        final int cp1 = 0x1F600;
        final String source = new String(Character.toChars(cp1)) + "A";
        final SourceCursor cursor = new SourceCursor(source);
        assertTrue(cursor.match(cp1, 'A'));
        assertTrue(cursor.isAtEnd());
    }

    @Test
    @DisplayName("match(int,int) does not advance on mismatch with supplementary first")
    void matchDoubleNoAdvanceOnMismatchSupplementary() {
        final int cp1 = 0x1F600;
        final String source = new String(Character.toChars(cp1)) + "B";
        final SourceCursor cursor = new SourceCursor(source);
        assertFalse(cursor.match(cp1, 'A'));
        assertEquals(cp1, cursor.peekCodePoint());
    }

    @Test
    @DisplayName("match(int,int) does not modify state on mismatch")
    void matchDoubleNoStateChangeOnMismatch() {
        final SourceCursor cursor = new SourceCursor("ab");
        final SourceLocation before = cursor.currentLocation();
        assertFalse(cursor.match('a', 'x'));
        final SourceLocation after = cursor.currentLocation();
        assertEquals(before.line(), after.line());
        assertEquals(before.column(), after.column());
        assertEquals(before.utf8Offset(), after.utf8Offset());
        assertEquals(before.codePointOffset(), after.codePointOffset());
        assertEquals('a', cursor.peekCodePoint());
    }

    @Test
    @DisplayName("match(int,int) at exactly two character source that matches")
    void matchDoubleExactFit() {
        final SourceCursor cursor = new SourceCursor("ab");
        assertTrue(cursor.match('a', 'b'));
        assertTrue(cursor.isAtEnd());
    }

    @Test
    @DisplayName("consecutive match calls succeed when source matches")
    void consecutiveMatchCalls() {
        final SourceCursor cursor = new SourceCursor("abcdef");
        assertTrue(cursor.match('a'));
        assertTrue(cursor.match('b'));
        assertTrue(cursor.match('c', 'd'));
        assertTrue(cursor.match('e'));
        assertTrue(cursor.match('f'));
        assertTrue(cursor.isAtEnd());
    }

    // ──────────────────────────────────────────────────────────────────
    // currentLocation
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("currentLocation at start of non-empty source has line 1, column 1, zero offsets")
    void currentLocationAtStart() {
        final SourceCursor cursor = new SourceCursor("hello");
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(1, loc.line());
        assertEquals(1, loc.column());
        assertEquals(0L, loc.utf8Offset());
        assertEquals(0L, loc.codePointOffset());
    }

    @Test
    @DisplayName("currentLocation at start of empty source has line 1, column 1, zero offsets")
    void currentLocationAtStartEmpty() {
        final SourceCursor cursor = new SourceCursor("");
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(1, loc.line());
        assertEquals(1, loc.column());
        assertEquals(0L, loc.utf8Offset());
        assertEquals(0L, loc.codePointOffset());
    }

    @Test
    @DisplayName("currentLocation after advancing through multi-line text")
    void currentLocationMultiLine() {
        final SourceCursor cursor = new SourceCursor("ab\ncd");
        cursor.advance(); // 'a'
        cursor.advance(); // 'b'
        cursor.advance(); // '\n'
        cursor.advance(); // 'c'
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(2, loc.line());
        assertEquals(2, loc.column());
    }

    @Test
    @DisplayName("currentLocation offset and index reflect UTF-16 position")
    void currentLocationOffsetAndIndex() {
        final SourceCursor cursor = new SourceCursor(ABC);
        cursor.advance(); // 'a'
        cursor.advance(); // 'b'
        final SourceLocation loc = cursor.currentLocation();
        // offset and index should both equal the UTF-16 position (2)
        assertEquals(2L, loc.offset());
        assertEquals(2, loc.index());
    }

    @Test
    @DisplayName("currentLocation offset accounts for surrogate pair width in UTF-16")
    void currentLocationOffsetWithSurrogatePair() {
        // 😀 = 2 UTF-16 code units, then 'A' = 1
        final String source = "\uD83D\uDE00A";
        final SourceCursor cursor = new SourceCursor(source);
        cursor.advance(); // 😀 (2 UTF-16 units)
        cursor.advance(); // 'A'
        final SourceLocation loc = cursor.currentLocation();
        // UTF-16 position: 2 + 1 = 3
        assertEquals(3L, loc.offset());
        assertEquals(3, loc.index());
        // but codePointOffset should be 2
        assertEquals(2L, loc.codePointOffset());
    }

    // ──────────────────────────────────────────────────────────────────
    // Edge / corner cases
    // ──────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("single character source is fully consumed by one advance")
    void singleCharacterSource() {
        final SourceCursor cursor = new SourceCursor("z");
        assertEquals('z', cursor.advance());
        assertTrue(cursor.isAtEnd());
        assertEquals(-1, cursor.advance());
    }

    @Test
    @DisplayName("source containing only a newline")
    void sourceContainingOnlyNewline() {
        final SourceCursor cursor = new SourceCursor("\n");
        assertFalse(cursor.isAtEnd());
        assertEquals('\n', cursor.advance());
        assertTrue(cursor.isAtEnd());
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(2, loc.line());
        assertEquals(1, loc.column());
    }

    @Test
    @DisplayName("source containing only CRLF")
    void sourceContainingOnlyCrLf() {
        final SourceCursor cursor = new SourceCursor("\r\n");
        assertEquals('\r', cursor.advance());
        assertEquals('\n', cursor.advance());
        assertTrue(cursor.isAtEnd());
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(2, loc.line());
        assertEquals(1, loc.column());
    }

    @Test
    @DisplayName("unpaired high surrogate is consumed as a single code unit")
    void unpairedHighSurrogate() {
        final String source = "\uD83D" + "A";
        final SourceCursor cursor = new SourceCursor(source);
        final int cp = cursor.advance();
        assertEquals(0xD83D, cp);
        assertEquals('A', cursor.advance());
        assertTrue(cursor.isAtEnd());
    }

    @Test
    @DisplayName("unpaired low surrogate is consumed as a single code unit")
    void unpairedLowSurrogate() {
        final String source = "\uDE00" + "B";
        final SourceCursor cursor = new SourceCursor(source);
        final int cp = cursor.advance();
        assertEquals(0xDE00, cp);
        assertEquals('B', cursor.advance());
        assertTrue(cursor.isAtEnd());
    }

    @Test
    @DisplayName("advance through a string of only supplementary characters")
    void allSupplementaryCharacters() {
        final int[] codePoints = {0x10000, 0x10FFFF, 0x1F4A9};
        final StringBuilder sb = new StringBuilder();
        for (final int cp : codePoints) {
            sb.appendCodePoint(cp);
        }
        final SourceCursor cursor = new SourceCursor(sb.toString());
        for (final int expected : codePoints) {
            assertFalse(cursor.isAtEnd());
            assertEquals(expected, cursor.advance());
        }
        assertTrue(cursor.isAtEnd());
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(3L, loc.codePointOffset());
    }

    @Test
    @DisplayName("advance through string with null character (U+0000)")
    void advanceThroughNullCharacter() {
        final String source = "a\0b";
        final SourceCursor cursor = new SourceCursor(source);
        assertEquals('a', cursor.advance());
        assertEquals(0, cursor.advance());
        assertEquals('b', cursor.advance());
        assertTrue(cursor.isAtEnd());
        assertEquals(1, cursor.currentLocation().line());
        assertEquals(4, cursor.currentLocation().column());
    }

    @Test
    @DisplayName("long source traversal maintains consistent offsets")
    void longSourceTraversal() {
        final int count = 1000;
        final String source = "x".repeat(count);
        final SourceCursor cursor = new SourceCursor(source);
        for (int i = 0; i < count; i++) {
            assertEquals('x', cursor.advance());
        }
        assertTrue(cursor.isAtEnd());
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(1, loc.line());
        assertEquals(count + 1, loc.column());
        assertEquals(count, loc.utf8Offset());
        assertEquals(count, loc.codePointOffset());
    }

    @Test
    @DisplayName("full traversal of complex source yields correct final location")
    void fullTraversalComplexSource() {
        // "Ä\n😀\r\nx"
        // Ä = U+00C4 (2 UTF-8 bytes, 1 UTF-16 unit)
        // \n = 1 UTF-8 byte, 1 UTF-16 unit
        // 😀 = U+1F600 (4 UTF-8 bytes, 2 UTF-16 units)
        // \r = 1 UTF-8 byte, 1 UTF-16 unit
        // \n = 1 UTF-8 byte, 1 UTF-16 unit
        // x = 1 UTF-8 byte, 1 UTF-16 unit
        final String source = "\u00C4\n\uD83D\uDE00\r\nx";
        final SourceCursor cursor = new SourceCursor(source);

        assertEquals('\u00C4', cursor.advance()); // line 1 col 2
        assertEquals('\n', cursor.advance()); // line 2 col 1
        assertEquals(0x1F600, cursor.advance()); // line 2 col 2
        assertEquals('\r', cursor.advance()); // CR, next is LF -> no line break yet
        assertEquals('\n', cursor.advance()); // line 3 col 1
        assertEquals('x', cursor.advance()); // line 3 col 2

        assertTrue(cursor.isAtEnd());
        final SourceLocation loc = cursor.currentLocation();
        assertEquals(3, loc.line());
        assertEquals(2, loc.column());
        // UTF-8 bytes: 2 + 1 + 4 + 1 + 1 + 1 = 10
        assertEquals(10L, loc.utf8Offset());
        // Code points: 6
        assertEquals(6L, loc.codePointOffset());
        // UTF-16 units: 1 + 1 + 2 + 1 + 1 + 1 = 7
        assertEquals(7L, loc.offset());
        assertEquals(7, loc.index());
    }
}
