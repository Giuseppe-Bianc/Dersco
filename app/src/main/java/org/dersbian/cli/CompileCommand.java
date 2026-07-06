package org.dersbian.cli;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import org.dersbian.compiler.CompilationRequest;
import org.dersbian.compiler.CompilerException;
import org.dersbian.compiler.DefaultCompilerService;
import org.dersbian.compiler.ICompilerService;
import org.dersbian.compiler.OptimizationLevel;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

/** Subcommand that performs the full compilation of a Dersco source file. */
@Slf4j
@Command(
        name = "compile",
        mixinStandardHelpOptions = true,
        description = "Compile a Dersco source file.")
@SuppressWarnings({
    "PMD.CommentRequired",
    "PMD.LongVariable",
    "PMD.CommentSize",
    "PMD.CommentDefaultAccessModifier",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.GuardLogStatement",
    "PMD.OnlyOneReturn"
})
public final class CompileCommand implements Callable<Integer> {

    private static final int EXIT_OK = 0;
    private static final int EXIT_COMPILATION_ERROR = 1;

    @Spec private CommandSpec spec;

    @Mixin private LoggingMixin loggingMixin;

    @Parameters(index = "0", paramLabel = "FILE", description = "Source file to compile.")
    private Path inputFile;

    @Option(
            names = {"-o", "--output"},
            paramLabel = "FILE",
            description = "Output file (default: ${DEFAULT-VALUE}).",
            defaultValue = "a.dr")
    private Path outputFile;

    @Option(
            names = {"-O", "--optimize"},
            paramLabel = "LEVEL",
            description =
                    "Optimization level: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).",
            defaultValue = "NONE")
    private OptimizationLevel optimizationLevel;

    @Option(names = "--emit-ir", description = "Also emit the intermediate code (IR).")
    private boolean emitIntermediateCode;

    @Option(
            names = "--diagnostics",
            description = "Enable advanced diagnostics (extended warnings, statistics).")
    private boolean diagnostics;

    /** Compilation engine. Injected via constructor to ease testing (mocks). */
    private final ICompilerService compilerService;

    /** Constructor used by picocli in production. */
    public CompileCommand() {
        this(new DefaultCompilerService());
    }

    /**
     * Test constructor: allows injecting a fake {@link ICompilerService} without touching the file
     * system or depending on a real compilation engine.
     *
     * @param compilerService the implementation to use.
     */
    CompileCommand(ICompilerService compilerService) {
        this.compilerService = compilerService;
    }

    @Override
    public Integer call() {
        loggingMixin.applyLogLevel();
        validateInputFile();

        log.info("Compiling {} -> {} (optimize={})", inputFile, outputFile, optimizationLevel);
        final CompilationRequest request =
                new CompilationRequest(
                        inputFile,
                        outputFile,
                        optimizationLevel,
                        emitIntermediateCode,
                        diagnostics);

        try {
            compilerService.compile(request);
        } catch (CompilerException e) {
            log.error("Compilation failed: {}", e.getMessage());
            return EXIT_COMPILATION_ERROR;
        }

        log.info("Compilation completed successfully: {}", outputFile);
        return EXIT_OK;
    }

    /**
     * Validates the existence/readability of the input file at execution time (it is not possible
     * to do so during parsing with a pure {@code ITypeConverter}, because that would require file
     * system access). Using {@link ParameterException} guarantees the same error format and usage
     * help as standard picocli parsing errors.
     */
    private void validateInputFile() {
        final File inputFileFile = inputFile.toFile();
        if (!inputFileFile.isFile() || !inputFileFile.canRead()) {
            throw new ParameterException(
                    spec.commandLine(), "Invalid or unreadable input file: " + inputFileFile);
        }
    }
}
