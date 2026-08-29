package org.dersbian.cli;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.dersbian.compiler.CompilerException;
import org.dersbian.compiler.DefaultCompilerService;
import org.dersbian.compiler.ICompilerService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** Subcommand that performs lexical and syntax checking and prints the resulting AST. */
@Command(
        name = "check",
        mixinStandardHelpOptions = true,
        description = "Check the syntactic correctness of a source file without compiling it.")
@SuppressWarnings({
    "PMD.CommentDefaultAccessModifier",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.GuardLogStatement",
    "PMD.OnlyOneReturn"
})
public final class CheckCommand implements Callable<Integer> {
    /** Logger for this command. */
    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(CheckCommand.class);

    /** Exit code indicating successful execution without syntax errors. */
    private static final int EXIT_OK = 0;

    /** Exit code indicating that a syntax error was detected. */
    private static final int EXIT_SYNTAX_ERROR = 1;

    /** The Picocli command specification, used for error reporting. */
    @Spec private CommandSpec spec;

    /** Mixin for handling logging configuration via command line options. */
    @Mixin private LoggingMixin loggingMixin;

    /** The source file to be checked for syntactic correctness. */
    @Parameters(index = "0", paramLabel = "FILE", description = "Source file to check.")
    private Path inputFile;

    /** Service responsible for performing the compilation or syntax check. */
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
        final File inputFileFile = inputFile.toFile();
        if (!inputFileFile.isFile() || !inputFileFile.canRead()) {
            throw new ParameterException(
                    spec.commandLine(), "Invalid or unreadable input file: " + inputFile);
        }

        try {
            compilerService.checkSyntax(inputFile);
        } catch (CompilerException e) {
            LOG.error("Syntax error: {}", e.getMessage());
            return EXIT_SYNTAX_ERROR;
        }

        LOG.info("No syntax errors detected in {}", inputFile);
        return EXIT_OK;
    }
}
