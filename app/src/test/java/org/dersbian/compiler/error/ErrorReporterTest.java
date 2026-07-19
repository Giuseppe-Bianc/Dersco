package org.dersbian.compiler.error;

import java.util.List;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.location.LineTracker;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestAssertionsShouldIncludeMessage"
})
class ErrorReporterTest {

    @Test
    void reportsSingleLineErrorsWithSourceContextAndHelp() {
        final LineTracker lineTracker = LineTracker.fromLines(List.of("let value = 42;"));
        final ErrorReporter reporter = new ErrorReporter(lineTracker, "test.vn");
        final Span span =
                Span.create(SourceLocation.create(1, 5, 4L), SourceLocation.create(1, 10, 9L));
        final CompileError.LexerError error =
                CompileError.lexerError(ErrorCode.E0001, "Invalid token", span, "Check the input");

        final String expected =
                String.join(
                        "\n",
                        List.of(
                                "\u001B[31m\u001B[1mERROR\u001B[0m"
                                    + " [\u001B[31m\u001B[1mE0001\u001B[0m] \u001B[31mLEX\u001B[0m:"
                                    + " \u001B[33mInvalid token\u001B[0m",
                                "\u001B[34mLocation:\u001B[0m"
                                        + " \u001B[36mtest.vn:line 1:column 5-line 1:column"
                                        + " 10\u001B[0m",
                                "   1 │ let value = 42;",
                                "     │ \u001B[31m\u001B[1m    ^^^^^\u001B[0m",
                                "\u001B[34m\u001B[1mhelp:\u001B[0m \u001B[32mCheck the"
                                        + " input\u001B[0m"));

        Assertions.assertEquals(expected, reporter.reportErrors(List.of(error)));
    }

    @Test
    void reportsMultiLineErrorsAndSimpleFailures() {
        final LineTracker lineTracker = LineTracker.fromLines(List.of("first line", "second line"));
        final ErrorReporter reporter = new ErrorReporter(lineTracker, "test.vn");
        final Span span =
                Span.create(SourceLocation.create(1, 3, 2L), SourceLocation.create(2, 4, 14L));
        final CompileError.SyntaxError syntaxError =
                CompileError.syntaxError(ErrorCode.E1004, "Unexpected token", span, null);
        final CompileError.AsmGeneratorError asmError =
                CompileError.asmGeneratorError(ErrorCode.E4001, "Invalid instruction");

        final String expectedSyntax =
                String.join(
                        "\n",
                        List.of(
                                "\u001B[31m\u001B[1mERROR\u001B[0m"
                                        + " [\u001B[31m\u001B[1mE1004\u001B[0m]"
                                        + " \u001B[31mSYNTAX\u001B[0m: \u001B[33mUnexpected"
                                        + " token\u001B[0m",
                                "\u001B[34mLocation:\u001B[0m"
                                        + " \u001B[36mtest.vn:line 1:column 3-line 2:column"
                                        + " 4\u001B[0m",
                                "   1 │ first line",
                                "     │ \u001B[31m\u001B[1m  ^\u001B[0m",
                                "     │ \u001B[34m...\u001B[0m (error spans lines 1-2)"));
        final String expectedAsm =
                String.join(
                        "\n",
                        List.of(
                                "\u001B[31m\u001B[1mERROR\u001B[0m"
                                        + " [\u001B[31m\u001B[1mE4001\u001B[0m] \u001B[31mASM"
                                        + " GEN\u001B[0m: \u001B[33mInvalid instruction\u001B[0m",
                                ""));

        Assertions.assertEquals(
                expectedSyntax + expectedAsm,
                reporter.reportErrors(List.of(syntaxError, asmError)));
    }
}
