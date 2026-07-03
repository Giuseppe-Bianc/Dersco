package org.dersbian.cli;

import lombok.extern.slf4j.Slf4j;
import picocli.CommandLine;
import picocli.CommandLine.ExecutionException;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

/**
 * Handler for unexpected exceptions (bugs, IO errors, etc.) raised during command execution. {@link
 * CommandLine.ParameterException} (input validation errors) remain handled by picocli's default
 * handler, which prints the error plus usage help and exits with code 2: they should not be
 * duplicated here.
 */
@Slf4j
@SuppressWarnings({
  "PMD.CommentSize",
  "PMD.AtLeastOneConstructor",
  "PMD.LongVariable",
  "PMD.ShortVariable",
  "PMD.MethodArgumentCouldBeFinal",
  "PMD.LawOfDemeter",
  "PMD.OnlyOneReturn"
})
public final class CliExecutionExceptionHandler implements IExecutionExceptionHandler {

  /** BSD sysexits.h convention: EX_SOFTWARE. */
  private static final int EXIT_SOFTWARE_ERROR = 70;

  @Override
  public int handleExecutionException(
      Exception ex, CommandLine commandLine, ParseResult parseResult) {
    commandLine
        .getErr()
        .println(commandLine.getColorScheme().errorText("Error: " + ex.getMessage()));
    log.error("Unhandled exception during command execution", ex);

    if (ex instanceof ExecutionException) {
      return commandLine.getCommandSpec().exitCodeOnExecutionException();
    }
    return EXIT_SOFTWARE_ERROR;
  }
}
