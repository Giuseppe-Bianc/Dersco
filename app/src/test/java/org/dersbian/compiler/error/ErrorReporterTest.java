package org.dersbian.compiler.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.location.LineTracker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Test suite for {@link ErrorReporter}.
 *
 * <p>Each test exercises one observable behavior of the reporter. ANSI escape codes are stripped
 * before assertion so the tests remain stable across any future styling change, while a dedicated
 * check asserts the escape sequences are still emitted on the wire.
 */
@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.ShortVariable",
    "PMD.OnlyOneReturn",
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.TooManyMethods"
})
class ErrorReporterTest {

    /** ANSI CSI escape sequence: {@code ESC [} ... letter. */
    private static final Pattern ANSI_REGEX = Pattern.compile("\\u001B\\[[0-?]*[ -/]*[@-~]");

    /** Default source-file name used across most tests. */
    private static final String SOURCE_FILE = "f.vn";

    /** The help-label prefix checked in negative assertions. */
    private static final String HELP_LABEL = "help:";

    /** Location label prefix used in all rendered error blocks. */
    private static final String LOCATION_LABEL = "Location: ";

    /** Simple three-line source text shared by several tests. */
    private static final String SOURCE_TEXT = "let x = 1;\nlet y = ;\nlet z = 3;\n";

    /** Removes ANSI escape codes so tests assert on the human-readable text. */
    private static String stripAnsiCodes(final String s) {
        if (s == null) {
            return null;
        }
        return ANSI_REGEX.matcher(s).replaceAll("");
    }

    // ---------------------------------------------------------------
    // Helpers for building Span / SourceLocation instances.
    // ---------------------------------------------------------------

    private static SourceLocation loc(final int line, final int column, final long offset) {
        return SourceLocation.create(line, column, offset);
    }

    private static Span span(
            final int startLine,
            final int startCol,
            final long startOffset,
            final int endLine,
            final int endCol,
            final long endOffset) {
        return Span.create(loc(startLine, startCol, startOffset), loc(endLine, endCol, endOffset));
    }

    private static ErrorReporter reporterFor(final String text, final String fileName) {
        return new ErrorReporter(LineTracker.fromText(text), fileName);
    }

    // ---------------------------------------------------------------
    // Constructor validation
    // ---------------------------------------------------------------

    @Test
    @DisplayName("constructor rejects null LineTracker")
    void constructorRejectsNullLineTracker() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ErrorReporter(null, SOURCE_FILE))
                .withMessageContaining("lineTracker");
    }

    @Test
    @DisplayName("constructor rejects null sourceFile")
    void constructorRejectsNullSourceFile() {
        final LineTracker tracker = LineTracker.fromText(SOURCE_TEXT);
        assertThatNullPointerException()
                .isThrownBy(() -> new ErrorReporter(tracker, null))
                .withMessageContaining("sourceFile");
    }

    // ---------------------------------------------------------------
    // Empty / trivial input
    // ---------------------------------------------------------------

    @Test
    @DisplayName("reportErrors on empty list returns empty string")
    void reportErrorsEmptyListReturnsEmptyString() {
        final ErrorReporter reporter = reporterFor(SOURCE_TEXT, SOURCE_FILE);
        final String result = reporter.reportErrors(Collections.emptyList());
        assertThat(result).isEmpty();
    }

    // ---------------------------------------------------------------
    // Full LexerError rendering (code + help present)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("LexerError with code and help renders all sections")
    void lexerErrorWithCodeAndHelpRendersAllSections() {
        final ErrorReporter reporter = reporterFor(SOURCE_TEXT, SOURCE_FILE);
        final Span errSpan = span(1, 5, 4, 1, 8, 7);
        final CompileError error =
                CompileError.lexerError(ErrorCode.E0001, "invalid token", errSpan, "try again");

        final String raw = reporter.reportErrors(List.of(error));
        final String result = stripAnsiCodes(raw);

        assertThat(result).contains("ERROR");
        assertThat(result).contains("[E0001]");
        assertThat(result).contains("LEX");
        assertThat(result).contains("invalid token");
        assertThat(result).contains(LOCATION_LABEL + SOURCE_FILE + ":" + errSpan.toString());
        assertThat(result).contains(String.format("%4d │ %s", 1, "let x = 1;"));
        assertThat(result).contains("     │     ^^^");
        assertThat(result).contains(HELP_LABEL + " try again");
    }

    // ---------------------------------------------------------------
    // Missing optional code
    // ---------------------------------------------------------------

    @Test
    @DisplayName("LexerError without ErrorCode omits code brackets")
    void lexerErrorWithoutCodeOmitsBrackets() {
        final ErrorReporter reporter = reporterFor(SOURCE_TEXT, SOURCE_FILE);
        final Span errSpan = span(1, 1, 0, 1, 3, 2);
        final CompileError error = CompileError.lexerError(null, "bad char", errSpan, null);

        final String result = stripAnsiCodes(reporter.reportErrors(List.of(error)));

        assertThat(result).doesNotContain("[");
        assertThat(result).doesNotContain("]");
        assertThat(result).contains("bad char");
    }

    // ---------------------------------------------------------------
    // Missing optional help
    // ---------------------------------------------------------------

    @Test
    @DisplayName("LexerError without help omits the help section")
    void lexerErrorWithoutHelpOmitsHelpSection() {
        final ErrorReporter reporter = reporterFor(SOURCE_TEXT, SOURCE_FILE);
        final Span errSpan = span(1, 1, 0, 1, 3, 2);
        final CompileError error =
                CompileError.lexerError(ErrorCode.E0001, "bad char", errSpan, null);

        final String result = stripAnsiCodes(reporter.reportErrors(List.of(error)));

        assertThat(result).doesNotContain(HELP_LABEL);
    }

    // ---------------------------------------------------------------
    // Category labeling for span-based errors
    // ---------------------------------------------------------------

    // Package-private: required so JUnit's @MethodSource can resolve this factory method.
    private static Stream<Arguments> spanErrorFactories() {
        final Span errSpan = span(1, 1, 0, 1, 4, 3);
        return Stream.of(
                Arguments.of(
                        CompileError.lexerError(ErrorCode.E0001, "lex msg", errSpan, null), "LEX"),
                Arguments.of(
                        CompileError.syntaxError(ErrorCode.E1004, "syn msg", errSpan, null),
                        "SYNTAX"),
                Arguments.of(
                        CompileError.typeError(ErrorCode.E2002, "type msg", errSpan, null), "TYPE"),
                Arguments.of(
                        CompileError.irGeneratorError(ErrorCode.E3001, "ir msg", errSpan, null),
                        "IR GEN"));
    }

    @ParameterizedTest
    @MethodSource("spanErrorFactories")
    @DisplayName("each error kind renders its own category label")
    void eachErrorKindRendersOwnCategory(final CompileError error, final String expectedCategory) {
        final ErrorReporter reporter = reporterFor(SOURCE_TEXT, SOURCE_FILE);
        final String result = stripAnsiCodes(reporter.reportErrors(List.of(error)));

        assertThat(result).contains(expectedCategory);
        assertThat(result).contains(LOCATION_LABEL);
    }

    // ---------------------------------------------------------------
    // AsmGeneratorError: no span, simple one-liner
    // ---------------------------------------------------------------

    @Test
    @DisplayName("AsmGeneratorError has no Location section")
    void asmGeneratorErrorHasNoLocationSection() {
        final ErrorReporter reporter = reporterFor(SOURCE_TEXT, SOURCE_FILE);
        final CompileError error = CompileError.asmGeneratorError(ErrorCode.E4001, "cannot emit");

        final String result = stripAnsiCodes(reporter.reportErrors(List.of(error)));

        assertThat(result).contains("ASM GEN");
        assertThat(result).contains("cannot emit");
        assertThat(result).contains("[E4001]");
        assertThat(result).doesNotContain(LOCATION_LABEL);
        assertThat(result).endsWith("\n");
    }

    @Test
    @DisplayName("AsmGeneratorError without code has plain single space prefix")
    void asmGeneratorErrorWithoutCode() {
        final ErrorReporter reporter = reporterFor(SOURCE_TEXT, SOURCE_FILE);
        final CompileError error = CompileError.asmGeneratorError(null, "boom");

        final String result = stripAnsiCodes(reporter.reportErrors(List.of(error)));

        assertThat(result).doesNotContain("[");
        assertThat(result).contains("boom");
    }

    // ---------------------------------------------------------------
    // IoError
    // ---------------------------------------------------------------

    @Test
    @DisplayName("IoError with message uses the exception message")
    void ioErrorWithMessageUsesExceptionMessage() {
        final ErrorReporter reporter = reporterFor(SOURCE_TEXT, SOURCE_FILE);
        final CompileError error = CompileError.ioError(new IOException("disk full"));

        final String result = stripAnsiCodes(reporter.reportErrors(List.of(error)));

        assertThat(result).contains("I/O");
        assertThat(result).contains("disk full");
    }

    @Test
    @DisplayName("IoError without message falls back to toString()")
    void ioErrorWithoutMessageFallsBackToToString() {
        final ErrorReporter reporter = reporterFor(SOURCE_TEXT, SOURCE_FILE);
        final IOException cause = new IOException();
        final CompileError error = CompileError.ioError(cause);

        final String result = stripAnsiCodes(reporter.reportErrors(List.of(error)));

        assertThat(result).contains("I/O");
        assertThat(result).contains(cause.toString());
    }

    // ---------------------------------------------------------------
    // Multiline span rendering
    // ---------------------------------------------------------------

    @Test
    @DisplayName("multiline span shows the span-note and only the first source line")
    void multilineSpanShowsSpanNote() {
        final ErrorReporter reporter = reporterFor(SOURCE_TEXT, SOURCE_FILE);
        final Span errSpan = span(1, 1, 0, 3, 4, 25);
        final CompileError error =
                CompileError.syntaxError(ErrorCode.E1010, "unmatched paren", errSpan, null);

        final String result = stripAnsiCodes(reporter.reportErrors(List.of(error)));

        assertThat(result).contains("(error spans lines 1-3)");
        assertThat(result).contains("let x = 1;");
        assertThat(result).doesNotContain("let z = 3;");
    }

    // ---------------------------------------------------------------
    // Single-line underline geometry
    // ---------------------------------------------------------------

    @Test
    @DisplayName("single-line span underline width matches column range")
    void singleLineUnderlineWidthMatchesColumnRange() {
        final ErrorReporter reporter = reporterFor(SOURCE_TEXT, SOURCE_FILE);
        final Span errSpan = span(1, 5, 4, 1, 10, 9);
        final CompileError error =
                CompileError.typeError(ErrorCode.E2002, "mismatch", errSpan, null);

        final String result = stripAnsiCodes(reporter.reportErrors(List.of(error)));

        // startColumn=5 -> 4 leading spaces, width = 10-5 = 5 carets.
        assertThat(result).contains("     │     ^^^^^");
    }

    @Test
    @DisplayName("zero-width column span still renders at least one caret")
    void zeroWidthColumnSpanRendersAtLeastOneCaret() {
        final ErrorReporter reporter = reporterFor(SOURCE_TEXT, SOURCE_FILE);
        final Span errSpan = span(1, 3, 2, 1, 3, 2);
        final CompileError error =
                CompileError.lexerError(ErrorCode.E0008, "unexpected", errSpan, null);

        final String result = stripAnsiCodes(reporter.reportErrors(List.of(error)));

        assertThat(result).contains("     │   ^");
        assertThat(result).doesNotContain("^^");
    }

    @Test
    @DisplayName("column one produces no leading spaces before the caret")
    void columnOneProducesNoLeadingSpaces() {
        final ErrorReporter reporter = reporterFor(SOURCE_TEXT, SOURCE_FILE);
        final Span errSpan = span(1, 1, 0, 1, 2, 1);
        final CompileError error =
                CompileError.lexerError(ErrorCode.E0008, "bad start", errSpan, null);

        final String result = stripAnsiCodes(reporter.reportErrors(List.of(error)));

        assertThat(result).contains("     │ ^");
        assertThat(result).doesNotContain("     │  ^");
    }

    // ---------------------------------------------------------------
    // Missing source line (out-of-range line number)
    // ---------------------------------------------------------------

    @Test
    @DisplayName("span pointing past tracked lines omits the source-context block")
    void spanPastTrackedLinesOmitsSourceBlock() {
        final ErrorReporter reporter = reporterFor(SOURCE_TEXT, SOURCE_FILE);
        final Span errSpan = span(999, 1, 0, 999, 2, 1);
        final CompileError error =
                CompileError.syntaxError(ErrorCode.E1004, "phantom", errSpan, "fix it");

        final String result = stripAnsiCodes(reporter.reportErrors(List.of(error)));

        assertThat(result).contains(LOCATION_LABEL);
        assertThat(result).doesNotContain("│");
        // Help is still rendered even without a source-context block.
        assertThat(result).contains(HELP_LABEL + " fix it");
    }

    // ---------------------------------------------------------------
    // Multiple errors: order & concatenation
    // ---------------------------------------------------------------

    @Test
    @DisplayName("multiple errors are concatenated preserving input order")
    void multipleErrorsAreConcatenatedInOrder() {
        final ErrorReporter reporter = reporterFor(SOURCE_TEXT, SOURCE_FILE);
        final Span sp1 = span(1, 1, 0, 1, 2, 1);
        final Span sp2 = span(2, 1, 0, 2, 2, 1);
        final CompileError first =
                CompileError.lexerError(ErrorCode.E0001, "first error", sp1, null);
        final CompileError second =
                CompileError.syntaxError(ErrorCode.E1004, "second error", sp2, null);

        final String result = stripAnsiCodes(reporter.reportErrors(List.of(first, second)));

        final int firstIndex = result.indexOf("first error");
        final int secondIndex = result.indexOf("second error");

        assertThat(firstIndex).isGreaterThanOrEqualTo(0);
        assertThat(secondIndex).isGreaterThan(firstIndex);
    }

    // ---------------------------------------------------------------
    // ANSI escape codes are actually emitted on the raw (unstripped) output
    // ---------------------------------------------------------------

    @Test
    @DisplayName("raw output contains ANSI escape codes, stripped output does not")
    void rawOutputContainsAnsiCodesStrippedDoesNot() {
        final ErrorReporter reporter = reporterFor(SOURCE_TEXT, SOURCE_FILE);
        final Span errSpan = span(1, 1, 0, 1, 2, 1);
        final CompileError error =
                CompileError.lexerError(ErrorCode.E0001, "colored", errSpan, "hint");

        final String raw = reporter.reportErrors(List.of(error));
        final String stripped = stripAnsiCodes(raw);

        assertThat(raw).contains("\u001B[31m"); // RED
        assertThat(raw).contains("\u001B[1m"); // BOLD
        assertThat(raw).contains("\u001B[0m"); // RESET
        assertThat(ANSI_REGEX.matcher(stripped).find()).isFalse();
    }

    // ---------------------------------------------------------------
    // Sanity check: throwing on an unsupported/null list should not be silently
    // swallowed
    // ---------------------------------------------------------------

    @Test
    @DisplayName("reportErrors rejects a null error list")
    void reportErrorsRejectsNullList() {
        final ErrorReporter reporter = reporterFor(SOURCE_TEXT, SOURCE_FILE);
        assertThatThrownBy(() -> reporter.reportErrors(null))
                .isInstanceOf(NullPointerException.class);
    }
}
