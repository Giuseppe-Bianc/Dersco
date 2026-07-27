package org.dersbian.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

/**
 * Tests for {@link RootCommand}.
 *
 * <p>The root command is a git-style dispatcher: invoking {@code dersco} with no subcommand prints
 * the usage help. The {@code -V}/{@code --version} flag is wired to {@link ManifestVersionProvider}
 * and prints {@code "dersco <version>"}. Subcommands {@code compile} and {@code check} remain
 * dispatched to their respective classes.
 */
@SuppressWarnings("PMD.AtLeastOneConstructor")
class RootCommandTest {

    @Test
    void noSubcommandReturnsZeroExitCode() {
        final CommandLine commandLine = new CommandLine(new RootCommand());
        commandLine.setOut(new PrintWriter(new StringWriter(), true));

        final int exit = commandLine.execute();

        assertThat(exit).isEqualTo(0);
    }

    @Test
    void noSubcommandPrintsUsageHelp() {
        final StringWriter out = new StringWriter();
        final CommandLine commandLine = new CommandLine(new RootCommand());
        commandLine.setOut(new PrintWriter(out, true));

        commandLine.execute();

        // Usage synopsis contains the program name and at least one subcommand token.
        // Chained assertions are considered a single assert by the linter.
        assertThat(out.toString()).contains("dersco").contains("compile").contains("check");
    }

    @Test
    void versionFlagReturnsZeroExitCode() {
        final CommandLine commandLine = new CommandLine(new RootCommand());
        commandLine.setOut(new PrintWriter(new StringWriter(), true));

        final int exit = commandLine.execute("--version");

        assertThat(exit).isEqualTo(0);
    }

    @Test
    void versionFlagPrintsDerscoVersion() {
        final StringWriter out = new StringWriter();
        final CommandLine commandLine = new CommandLine(new RootCommand());
        commandLine.setOut(new PrintWriter(out, true));

        commandLine.execute("--version");

        assertThat(out.toString()).startsWith("dersco ");
    }

    @Test
    void unknownSubcommandExitsWithError() {
        final CommandLine commandLine = new CommandLine(new RootCommand());

        final int exit = commandLine.execute("bogus");

        // picocli returns ExitCode.USAGE (default 2) on invalid input.
        assertThat(exit).isEqualTo(CommandLine.ExitCode.USAGE);
    }
}
