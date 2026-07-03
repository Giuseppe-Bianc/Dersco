package org.dersbian.cli;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import org.dersbian.compiler.CompilerException;
import org.dersbian.compiler.DefaultCompilerService;
import org.dersbian.compiler.ICompilerService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** Subcommand that performs only syntax checking, without producing any output. */
@Slf4j
@Command(
    name = "check",
    mixinStandardHelpOptions = true,
    description = "Check the syntactic correctness of a source file without compiling it.")
@SuppressWarnings({
  "PMD.CommentRequired",
  "PMD.CommentDefaultAccessModifier",
  "PMD.MethodArgumentCouldBeFinal",
  "PMD.GuardLogStatement",
  "PMD.OnlyOneReturn"
})
public final class CheckCommand implements Callable<Integer> {

  private static final int EXIT_OK = 0;
  private static final int EXIT_SYNTAX_ERROR = 1;

  @Spec private CommandSpec spec;

  @Mixin private LoggingMixin loggingMixin;

  @Parameters(index = "0", paramLabel = "FILE", description = "Source file to check.")
  private Path inputFile;

  private final ICompilerService compilerService;

  /** Default constructor. */
  public CheckCommand() {
    this(new DefaultCompilerService());
  }

  CheckCommand(ICompilerService compilerService) {
    this.compilerService = compilerService;
  }

  @Override
  public Integer call() {
    loggingMixin.applyLogLevel();
    File inputFileFile = inputFile.toFile();
    if (!inputFileFile.isFile() || !inputFileFile.canRead()) {
      throw new ParameterException(
          spec.commandLine(), "Invalid or unreadable input file: " + inputFile);
    }

    try {
      compilerService.checkSyntax(inputFile);
    } catch (CompilerException e) {
      log.error("Syntax error: {}", e.getMessage());
      return EXIT_SYNTAX_ERROR;
    }

    log.info("No syntax errors detected in {}", inputFile);
    return EXIT_OK;
  }
}
