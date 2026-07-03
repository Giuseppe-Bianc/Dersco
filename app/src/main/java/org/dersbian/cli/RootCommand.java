package org.dersbian.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * Root {@code dersco} command. It performs no work of its own: it delegates to the {@code compile}
 * and {@code check} subcommands. This git-style structure is the pattern recommended by picocli for
 * CLIs meant to grow with new features (e.g. {@code dersco format}, {@code dersco repl}) without
 * breaking the backward compatibility of existing options.
 */
@Command(
    name = "dersco",
    mixinStandardHelpOptions = true,
    versionProvider = ManifestVersionProvider.class,
    description = "Modern compiler for the Dersco programming language.",
    subcommands = {CompileCommand.class, CheckCommand.class, HelpCommand.class},
    synopsisSubcommandLabel = "COMMAND")
@SuppressWarnings({"PMD.CommentSize", "PMD.AtLeastOneConstructor", "PMD.CommentRequired"})
public final class RootCommand implements Runnable {

  @Spec private CommandSpec spec;

  @Override
  public void run() {
    // No subcommand specified: show help instead of exiting silently.
    spec.commandLine().usage(spec.commandLine().getOut());
  }
}
