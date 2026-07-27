package org.dersbian.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.Help;
import picocli.CommandLine.ParseResult;

/**
 * Tests for {@link CliExecutionExceptionHandler}.
 *
 * <p>Behavior contract verified here:
 *
 * <ul>
 *   <li>{@link RuntimeException} -> BSD EX_SOFTWARE (70).
 *   <li>{@link ExecutionException} -> picocli's configured exit code on execution exception.
 *   <li>Both branches write a single line containing {@code "Error: " + message} to stderr.
 * </ul>
 */
@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestAssertionsShouldIncludeMessage"
})
class CliExecutionExceptionHandlerTest {

    private static final int EXIT_SOFTWARE_ERROR = 70;

    @Test
    void runtimeExceptionReturnsExSoftware() {
        final Capturing commandLine = new Capturing();
        final ParseResult parseResult = new CommandLine(new RootCommand()).parseArgs("--help");
        final CliExecutionExceptionHandler handler = new CliExecutionExceptionHandler();

        final int exit =
                handler.handleExecutionException(
                        new IllegalStateException("boom"), commandLine, parseResult);

        assertThat(exit).isEqualTo(EXIT_SOFTWARE_ERROR);
    }

    @Test
    void runtimeExceptionPrintsErrorPrefixToStderr() {
        final Capturing commandLine = new Capturing();
        final ParseResult parseResult = new CommandLine(new RootCommand()).parseArgs("--help");
        final CliExecutionExceptionHandler handler = new CliExecutionExceptionHandler();

        handler.handleExecutionException(
                new IllegalStateException("disk on fire"), commandLine, parseResult);

        assertThat(commandLine.errWriter.toString()).contains("Error: ").contains("disk on fire");
    }

    @Test
    void executionExceptionUsesPicocliConfiguredExitCode() {
        final Capturing commandLine = new Capturing();
        // Configure a non-default exit code on execution exceptions.
        commandLine.getCommandSpec().exitCodeOnExecutionException(42);
        final ParseResult parseResult = new CommandLine(new RootCommand()).parseArgs("--help");
        final CliExecutionExceptionHandler handler = new CliExecutionExceptionHandler();

        final int exit =
                handler.handleExecutionException(
                        new ExecutionException(new CommandLine(new RootCommand()), "io broke"),
                        commandLine,
                        parseResult);

        assertThat(exit).isEqualTo(42);
    }

    @Test
    void executionExceptionStillPrintsErrorPrefixToStderr() {
        final Capturing commandLine = new Capturing();
        final ParseResult parseResult = new CommandLine(new RootCommand()).parseArgs("--help");
        final CliExecutionExceptionHandler handler = new CliExecutionExceptionHandler();

        handler.handleExecutionException(
                new ExecutionException(new CommandLine(new RootCommand()), "io broke"),
                commandLine,
                parseResult);

        assertThat(commandLine.errWriter.toString()).contains("Error: ").contains("io broke");
    }

    /**
     * Minimal {@link CommandLine} stand-in that captures stderr writes without spinning up a real
     * picocli subcommand tree.
     */
    private static final class Capturing extends CommandLine {
        private final StringWriter errWriter = new StringWriter();

        private Capturing() {
            super(new RootCommand());
            setOut(new PrintWriter(new StringWriter(), true));
            setErr(new PrintWriter(errWriter, true));
            setColorScheme(Help.defaultColorScheme(Help.Ansi.OFF));
        }
    }
}
