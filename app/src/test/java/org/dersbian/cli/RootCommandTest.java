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
@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestAssertionsShouldIncludeMessage"
})
class RootCommandTest {

    @Test
    void noSubcommandPrintsUsageHelp() {
        final StringWriter out = new StringWriter();
        final CommandLine commandLine = new CommandLine(new RootCommand());
        commandLine.setOut(new PrintWriter(out, true));

        final int exit = commandLine.execute();

        assertThat(exit).isEqualTo(0);
        // Usage synopsis contains the program name and at least one subcommand token.
        final String printed = out.toString();
        assertThat(printed).contains("dersco").contains("compile").contains("check");
    }

    @Test
    void versionFlagPrintsDerscoVersion() {
        final StringWriter out = new StringWriter();
        final CommandLine commandLine = new CommandLine(new RootCommand());
        commandLine.setOut(new PrintWriter(out, true));

        final int exit = commandLine.execute("--version");

        assertThat(exit).isEqualTo(0);
        assertThat(out.toString()).startsWith("dersco ");
    }

    @Test
    void unknownSubcommandExitsWithError() {
        final CommandLine commandLine = new CommandLine(new RootCommand());

        final int exit = commandLine.execute("bogus");

        // picocli returns its configured exit code on invalid input (default 2).
        assertThat(exit).isEqualTo(commandLine.getCommandSpec().exitCodeOnInvalidInput());
    }
}
