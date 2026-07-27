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
import org.junit.jupiter.api.Nested;
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
    "PMD.CommentRequired",
    "PMD.ShortVariable",
    "PMD.OnlyOneReturn",
    "PMD.UnitTestContainsTooManyAsserts"
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

    /** Removes ANSI escape codes so tests assert on the human-readable text. */
    private static String stripAnsiCodes(final String s) {
        if (s == null) {
            return null;
        }
        return ANSI_REGEX.matcher(s).replaceAll("");
    }

    // ---------------------------------------------------------------------
    // Construction contract
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("constructor")
    public final class Constructor {

        @Test
        @DisplayName("rejects a null LineTracker")
        void rejectsNullLineTracker() {
            assertThatNullPointerException()
                    .isThrownBy(() -> new ErrorReporter(null, "src.vn"))
                    .withMessageContaining("lineTracker");
        }

        @Test
        @DisplayName("rejects a null sourceFile")
        void rejectsNullSourceFile() {
            final LineTracker tracker = LineTracker.fromLines(List.of("a"));
            assertThatNullPointerException()
                    .isThrownBy(() -> new ErrorReporter(tracker, null))
                    .withMessageContaining("sourceFile");
        }
    }

    // ---------------------------------------------------------------------
    // Empty / degenerate inputs
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("empty input")
    public final class EmptyInput {

        @Test
        @DisplayName("empty error list produces empty output")
        void emptyErrorListProducesEmptyString() {
            final ErrorReporter reporter =
                    new ErrorReporter(LineTracker.fromLines(List.of("x")), SOURCE_FILE);
            assertThat(reporter.reportErrors(List.of())).isEmpty();
        }

        @Test
        @DisplayName("empty LineTracker yields no source-line/underline block")
        void emptyLineTrackerSkipsSourceLine() {
            final ErrorReporter reporter =
                    new ErrorReporter(LineTracker.fromLines(List.of()), SOURCE_FILE);
            final Span span = Span.point(SourceLocation.create(1, 1, 0L));
            final CompileError.LexerError error =
                    CompileError.lexerError(ErrorCode.E0001, "boom", span, null);

            final String rendered = stripAnsiCodes(reporter.reportErrors(List.of(error)));

            assertThat(rendered)
                    .startsWith("ERROR [E0001] LEX: boom")
                    .contains(LOCATION_LABEL + SOURCE_FILE + ":line 1:column 1")
                    .doesNotContain("│")
                    .doesNotContain(HELP_LABEL);
        }
    }

    // ---------------------------------------------------------------------
    // Per-variant rendering
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("per-variant rendering")
    public final class PerVariant {

        private final LineTracker tracker = LineTracker.fromLines(List.of("let value = 42;"));
        private final Span span =
                Span.create(SourceLocation.create(1, 5, 4L), SourceLocation.create(1, 10, 9L));
        private final ErrorReporter reporter = new ErrorReporter(tracker, "test.vn");

        @Test
        @DisplayName("LexerError renders with LEX category")
        void lexerError() {
            final String rendered =
                    stripAnsiCodes(
                            reporter.reportErrors(
                                    List.of(
                                            CompileError.lexerError(
                                                    ErrorCode.E0001,
                                                    "Invalid token",
                                                    span,
                                                    "Check the input"))));

            assertThat(rendered)
                    .startsWith("ERROR [E0001] LEX: Invalid token\n")
                    .contains("Location: test.vn:line 1:column 5-line 1:column 10")
                    .contains("   1 │ let value = 42;")
                    .contains("     │     ^^^^^")
                    .contains(HELP_LABEL + " Check the input");
        }

        @Test
        @DisplayName("SyntaxError renders with SYNTAX category")
        void syntaxError() {
            final String rendered =
                    stripAnsiCodes(
                            reporter.reportErrors(
                                    List.of(
                                            CompileError.syntaxError(
                                                    ErrorCode.E1004,
                                                    "Unexpected token",
                                                    span,
                                                    "Check brackets"))));

            assertThat(rendered)
                    .startsWith("ERROR [E1004] SYNTAX: Unexpected token\n")
                    .contains(HELP_LABEL + " Check brackets");
        }

        @Test
        @DisplayName("TypeError renders with TYPE category")
        void typeError() {
            final String rendered =
                    stripAnsiCodes(
                            reporter.reportErrors(
                                    List.of(
                                            CompileError.typeError(
                                                    ErrorCode.E2002,
                                                    "Type mismatch",
                                                    span,
                                                    "Cast the value"))));

            assertThat(rendered)
                    .startsWith("ERROR [E2002] TYPE: Type mismatch\n")
                    .contains(HELP_LABEL + " Cast the value");
        }

        @Test
        @DisplayName("IrGeneratorError renders with IR GEN category")
        void irGeneratorError() {
            final String rendered =
                    stripAnsiCodes(
                            reporter.reportErrors(
                                    List.of(
                                            CompileError.irGeneratorError(
                                                    ErrorCode.E3003, "Bad IR", span, null))));

            assertThat(rendered)
                    .startsWith("ERROR [E3003] IR GEN: Bad IR\n")
                    .doesNotContain(HELP_LABEL);
        }

        @Test
        @DisplayName("AsmGeneratorError renders without span or source line")
        void asmGeneratorError() {
            final String rendered =
                    stripAnsiCodes(
                            reporter.reportErrors(
                                    List.of(
                                            CompileError.asmGeneratorError(
                                                    ErrorCode.E4001, "Invalid instruction"))));

            assertThat(rendered)
                    .isEqualTo("ERROR [E4001] ASM GEN: Invalid instruction\n")
                    .doesNotContain("Location:")
                    .doesNotContain("│")
                    .doesNotContain(HELP_LABEL);
        }

        @Test
        @DisplayName("IoError renders the cause message with I/O category")
        void ioErrorWithMessage() {
            final String rendered =
                    stripAnsiCodes(
                            reporter.reportErrors(
                                    List.of(CompileError.ioError(new IOException("disk full")))));

            assertThat(rendered)
                    .isEqualTo("ERROR I/O: disk full\n")
                    .doesNotContain("Location:")
                    .doesNotContain("│");
        }

        @Test
        @DisplayName("IoError falls back to cause.toString() when message is null")
        void ioErrorWithoutMessage() {
            final IOException cause = new IOException((String) null);
            final String rendered =
                    stripAnsiCodes(reporter.reportErrors(List.of(CompileError.ioError(cause))));

            assertThat(rendered)
                    .startsWith("ERROR I/O: ")
                    .doesNotContain("null")
                    .contains(cause.toString());
        }

        @Test
        @DisplayName("IoError rejects a null cause at construction")
        void ioErrorRejectsNullCause() {
            assertThatNullPointerException()
                    .isThrownBy(() -> CompileError.ioError(null))
                    .withMessageContaining("cause");
        }
    }

    // ---------------------------------------------------------------------
    // Span behavior
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("span rendering")
    public final class SpanRendering {

        @Test
        @DisplayName("single-line span emits a caret run matching the column range")
        void singleLineSpan() {
            final LineTracker tracker = LineTracker.fromLines(List.of("let value = 42;"));
            final ErrorReporter reporter = new ErrorReporter(tracker, SOURCE_FILE);
            final Span span =
                    Span.create(SourceLocation.create(1, 5, 4L), SourceLocation.create(1, 10, 9L));

            final String rendered =
                    stripAnsiCodes(
                            reporter.reportErrors(
                                    List.of(
                                            CompileError.syntaxError(
                                                    ErrorCode.E1004, "x", span, null))));

            assertThat(rendered).contains("     │     ^^^^^");
        }

        @Test
        @DisplayName("multi-line span emits a single caret and a continuation note")
        void multiLineSpan() {
            final LineTracker tracker = LineTracker.fromLines(List.of("first line", "second line"));
            final ErrorReporter reporter = new ErrorReporter(tracker, SOURCE_FILE);
            final Span span =
                    Span.create(SourceLocation.create(1, 3, 2L), SourceLocation.create(2, 4, 14L));

            final String rendered =
                    stripAnsiCodes(
                            reporter.reportErrors(
                                    List.of(
                                            CompileError.syntaxError(
                                                    ErrorCode.E1004, "x", span, null))));

            assertThat(rendered)
                    .contains(LOCATION_LABEL + SOURCE_FILE + ":line 1:column 3-line 2:column 4")
                    .contains("   1 │ first line")
                    .contains("     │   ^")
                    .contains("     │ ... (error spans lines 1-2)");
        }

        @Test
        @DisplayName("zero-length point span emits a single caret at the column")
        void pointSpan() {
            final LineTracker tracker = LineTracker.fromLines(List.of("abcdef"));
            final ErrorReporter reporter = new ErrorReporter(tracker, SOURCE_FILE);
            final Span span = Span.point(SourceLocation.create(1, 4, 3L));

            final String rendered =
                    stripAnsiCodes(
                            reporter.reportErrors(
                                    List.of(
                                            CompileError.syntaxError(
                                                    ErrorCode.E1004, "x", span, null))));

            assertThat(rendered).contains("     │    ^");
        }

        @Test
        @DisplayName("span pointing past the last tracked line omits the source-line block")
        void spanPastEndOfSource() {
            final LineTracker tracker = LineTracker.fromLines(List.of("only line"));
            final ErrorReporter reporter = new ErrorReporter(tracker, SOURCE_FILE);
            final Span span =
                    Span.create(
                            SourceLocation.create(42, 1, 100L), SourceLocation.create(42, 2, 101L));

            final String rendered =
                    stripAnsiCodes(
                            reporter.reportErrors(
                                    List.of(
                                            CompileError.syntaxError(
                                                    ErrorCode.E1004, "x", span, null))));

            assertThat(rendered)
                    .contains(LOCATION_LABEL + SOURCE_FILE + ":line 42:column 1-line 42:column 2")
                    .doesNotContain("│");
        }

        @Test
        @DisplayName("span starting at column 1 renders the caret at column 0 (no leading space)")
        void spanStartingAtColumnOne() {
            final LineTracker tracker = LineTracker.fromLines(List.of("abcdef"));
            final ErrorReporter reporter = new ErrorReporter(tracker, SOURCE_FILE);
            final Span span =
                    Span.create(SourceLocation.create(1, 1, 0L), SourceLocation.create(1, 3, 2L));

            final String rendered =
                    stripAnsiCodes(
                            reporter.reportErrors(
                                    List.of(
                                            CompileError.syntaxError(
                                                    ErrorCode.E1004, "x", span, null))));

            assertThat(rendered).contains("     │ ^^");
        }

        @Test
        @DisplayName("inverted offsets are rejected by Span itself")
        void spanRejectsInvertedOffsets() {
            final SourceLocation end = SourceLocation.create(1, 5, 4L);
            final SourceLocation start = SourceLocation.create(1, 1, 5L);
            assertThatThrownBy(() -> new Span(start, end))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("must not precede start offset");
        }
    }

    // ---------------------------------------------------------------------
    // Help and code-prefix handling
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("help and code prefix")
    public final class HelpAndCodePrefix {

        private final LineTracker tracker = LineTracker.fromLines(List.of("let x = 1;"));
        private final Span span =
                Span.create(SourceLocation.create(1, 1, 0L), SourceLocation.create(1, 3, 2L));

        @Test
        @DisplayName("null help omits the help line entirely")
        void nullHelpOmitted() {
            final ErrorReporter reporter = new ErrorReporter(tracker, SOURCE_FILE);
            final String rendered =
                    stripAnsiCodes(
                            reporter.reportErrors(
                                    List.of(
                                            new CompileError.LexerError(
                                                    java.util.Optional.of(ErrorCode.E0001),
                                                    "m",
                                                    span,
                                                    java.util.Optional.ofNullable(null)))));

            assertThat(rendered).doesNotContain(HELP_LABEL);
        }

        @Test
        @DisplayName("missing ErrorCode renders a blank prefix slot")
        void missingErrorCodeBlankPrefix() {
            final ErrorReporter reporter = new ErrorReporter(tracker, SOURCE_FILE);
            final String rendered =
                    stripAnsiCodes(
                            reporter.reportErrors(
                                    List.of(
                                            new CompileError.LexerError(
                                                    java.util.Optional.empty(),
                                                    "m",
                                                    span,
                                                    java.util.Optional.empty()))));

            assertThat(rendered).startsWith("ERROR LEX: m").doesNotContain("[E");
        }
    }

    // ---------------------------------------------------------------------
    // ANSI output behavior
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("ANSI styling")
    public final class AnsiStyling {

        @Test
        @DisplayName("output is wrapped in ANSI escape sequences")
        void outputContainsAnsiEscapes() {
            final LineTracker tracker = LineTracker.fromLines(List.of("x"));
            final ErrorReporter reporter = new ErrorReporter(tracker, SOURCE_FILE);
            final Span span = Span.point(SourceLocation.create(1, 1, 0L));
            final String raw =
                    reporter.reportErrors(
                            List.of(CompileError.lexerError(ErrorCode.E0001, "m", span, "h")));
            assertThat(raw)
                    .contains("\u001B[1m") // bold
                    .contains("\u001B[31m") // red
                    .contains("\u001B[33m") // yellow message
                    .contains("\u001B[34m") // blue label
                    .contains("\u001B[36m") // cyan location
                    .contains("\u001B[32m") // green help
                    .contains("\u001B[0m"); // reset
        }

        @Test
        @DisplayName("strip helper removes every ANSI code and keeps the readable text")
        void stripAnsiHelperIsIdempotent() {
            final String original = "ERROR [E0001] LEX: m";
            final String wrapped = "\u001B[1m\u001B[31m" + original + "\u001B[0m";

            assertThat(stripAnsiCodes(wrapped)).isEqualTo(original);
            assertThat(stripAnsiCodes(stripAnsiCodes(wrapped))).isEqualTo(original);
            assertThat(stripAnsiCodes(null)).isNull();
        }
    }

    // ---------------------------------------------------------------------
    // Multiple errors / ordering
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("multiple errors")
    public final class MultipleErrors {

        @Test
        @DisplayName("errors are concatenated in input order without any separator")
        void errorsConcatenatedInOrder() {
            final LineTracker tracker = LineTracker.fromLines(List.of("abc"));
            final ErrorReporter reporter = new ErrorReporter(tracker, SOURCE_FILE);
            final Span s1 = Span.point(SourceLocation.create(1, 1, 0L));
            final Span s2 = Span.point(SourceLocation.create(1, 2, 1L));
            final CompileError a = CompileError.lexerError(ErrorCode.E0001, "first", s1, null);
            final CompileError b = CompileError.lexerError(ErrorCode.E0002, "second", s2, null);

            final String rendered = stripAnsiCodes(reporter.reportErrors(List.of(a, b)));

            assertThat(rendered)
                    .contains("ERROR [E0001] LEX: first\n")
                    .contains("ERROR [E0002] LEX: second\n");
            final int idxFirst = rendered.indexOf("first");
            final int idxSecond = rendered.indexOf("second");
            assertThat(idxFirst).isLessThan(idxSecond);
        }

        @Test
        @DisplayName("single-error list renders exactly one block")
        void singleErrorRendersOneBlock() {
            final LineTracker tracker = LineTracker.fromLines(List.of("abc"));
            final ErrorReporter reporter = new ErrorReporter(tracker, SOURCE_FILE);
            final Span span = Span.point(SourceLocation.create(1, 1, 0L));

            final String rendered =
                    stripAnsiCodes(
                            reporter.reportErrors(
                                    List.of(
                                            CompileError.lexerError(
                                                    ErrorCode.E0001, "m", span, null))));

            assertThat(rendered.chars().filter(c -> c == '\n'))
                    .hasSize(3); // header + location + line + underline
        }
    }

    // ---------------------------------------------------------------------
    // source-file and message edge cases
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("edge cases")
    public final class EdgeCases {

        private final LineTracker tracker = LineTracker.fromLines(List.of("hello"));
        private final Span span = Span.point(SourceLocation.create(1, 1, 0L));

        @Test
        @DisplayName("empty sourceFile is preserved verbatim in the Location line")
        void emptySourceFilePreserved() {
            final ErrorReporter reporter = new ErrorReporter(tracker, "");
            final String rendered =
                    stripAnsiCodes(
                            reporter.reportErrors(
                                    List.of(
                                            CompileError.lexerError(
                                                    ErrorCode.E0001, "m", span, null))));

            assertThat(rendered).contains("Location: :line 1:column 1");
        }

        @Test
        @DisplayName("empty message is preserved verbatim")
        void emptyMessagePreserved() {
            final ErrorReporter reporter = new ErrorReporter(tracker, SOURCE_FILE);
            final String rendered =
                    stripAnsiCodes(
                            reporter.reportErrors(
                                    List.of(
                                            CompileError.lexerError(
                                                    ErrorCode.E0001, "", span, null))));

            assertThat(rendered).contains("ERROR [E0001] LEX: \n");
        }

        @Test
        @DisplayName(
                "source line containing tab characters still produces a correct underline column")
        void tabInSourceLine() {
            final LineTracker tabTracker = LineTracker.fromLines(List.of("\tabc"));
            final ErrorReporter reporter = new ErrorReporter(tabTracker, SOURCE_FILE);
            final Span tabSpan =
                    Span.create(SourceLocation.create(1, 2, 1L), SourceLocation.create(1, 4, 3L));

            final String rendered =
                    stripAnsiCodes(
                            reporter.reportErrors(
                                    List.of(
                                            CompileError.lexerError(
                                                    ErrorCode.E0001, "m", tabSpan, null))));

            // startColumn=2 -> startOffset=1; one space, then "^^" run
            assertThat(rendered).contains("     │  ^^");
        }

        @Test
        @DisplayName("unicode source line is rendered verbatim in the source context")
        void unicodeSourceLine() {
            final LineTracker unicodeTracker = LineTracker.fromLines(List.of("αβγ"));
            final ErrorReporter reporter = new ErrorReporter(unicodeTracker, SOURCE_FILE);

            final String rendered =
                    stripAnsiCodes(
                            reporter.reportErrors(
                                    List.of(
                                            CompileError.lexerError(
                                                    ErrorCode.E0001, "m", span, null))));

            assertThat(rendered).contains("   1 │ αβγ");
        }

        @Test
        @DisplayName("singleton List with AsmGeneratorError renders a trailing newline")
        void asmErrorTrailingNewline() {
            final ErrorReporter reporter = new ErrorReporter(tracker, SOURCE_FILE);
            final String rendered =
                    stripAnsiCodes(
                            reporter.reportErrors(
                                    List.of(CompileError.asmGeneratorError(ErrorCode.E4001, "m"))));

            assertThat(rendered).endsWith("\n");
        }

        @Test
        @DisplayName("unmodifiable Collections.emptyList() is also supported")
        void supportsCollectionsEmptyList() {
            final ErrorReporter reporter = new ErrorReporter(tracker, SOURCE_FILE);
            assertThat(reporter.reportErrors(Collections.<CompileError>emptyList())).isEmpty();
        }
    }

    // ---------------------------------------------------------------------
    // Parameterized round-trip across every phase-with-span variant
    // ---------------------------------------------------------------------

    @Nested
    @DisplayName("parameterized phase-with-span variants")
    public final class ParameterizedPhaseVariants {

        private static Stream<Arguments> phaseVariants() {
            final Span span =
                    Span.create(SourceLocation.create(1, 1, 0L), SourceLocation.create(1, 2, 1L));
            final ErrorReporter reporter =
                    new ErrorReporter(LineTracker.fromLines(List.of("ab")), SOURCE_FILE);
            return Stream.of(
                    Arguments.of(
                            "LEX",
                            CompileError.lexerError(ErrorCode.E0001, "m", span, null),
                            reporter),
                    Arguments.of(
                            "SYNTAX",
                            CompileError.syntaxError(ErrorCode.E1004, "m", span, null),
                            reporter),
                    Arguments.of(
                            "TYPE",
                            CompileError.typeError(ErrorCode.E2002, "m", span, null),
                            reporter),
                    Arguments.of(
                            "IR GEN",
                            CompileError.irGeneratorError(ErrorCode.E3003, "m", span, null),
                            reporter));
        }

        @ParameterizedTest(name = "{0} variant renders with the right category and caret")
        @MethodSource("phaseVariants")
        void phaseVariantRendersCategory(
                final String category, final CompileError error, final ErrorReporter reporter) {
            final String rendered = stripAnsiCodes(reporter.reportErrors(List.of(error)));

            assertThat(rendered)
                    .contains("ERROR [E")
                    .contains(" " + category + ": m")
                    .contains(LOCATION_LABEL + SOURCE_FILE + ":")
                    .contains("   1 │ ab")
                    .contains("     │ ^");
        }
    }
}
