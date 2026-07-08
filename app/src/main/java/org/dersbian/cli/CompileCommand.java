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
@SuppressWarnings({"PMD.LongVariable", "PMD.GuardLogStatement", "PMD.OnlyOneReturn"})
public final class CompileCommand implements Callable<Integer> {

    /** Exit code indicating successful compilation. */
    private static final int EXIT_OK = 0;

    /** Exit code indicating a compilation error occurred. */
    private static final int EXIT_COMPILATION_ERROR = 1;

    /** Picocli command specification, injected at parse time for error reporting. */
    @Spec private CommandSpec spec;

    /** Mixin that provides verbose/quiet logging level options to this command. */
    @Mixin private LoggingMixin loggingMixin;

    /** Path to the Dersco source file to compile. */
    @Parameters(index = "0", paramLabel = "FILE", description = "Source file to compile.")
    private Path inputFile;

    /** Path to the output file produced by the compiler. */
    @Option(
            names = {"-o", "--output"},
            paramLabel = "FILE",
            description = "Output file (default: ${DEFAULT-VALUE}).",
            defaultValue = "a.exe")
    private Path outputFile;

    /** Optimization level applied during compilation. */
    @Option(
            names = {"-O", "--optimize"},
            paramLabel = "LEVEL",
            description =
                    "Optimization level: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).",
            defaultValue = "NONE")
    private OptimizationLevel optimizationLevel;

    /** Whether to emit the intermediate representation (IR) alongside the compiled output. */
    @Option(names = "--emit-ir", description = "Also emit the intermediate code (IR).")
    private boolean emitIntermediateCode;

    /** Whether to enable advanced diagnostics such as extended warnings and statistics. */
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
    public CompileCommand(final ICompilerService compilerService) {
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
        log.info("Validating input file: {}", inputFile);
        final File inputFileFile = inputFile.toFile();
        if (!inputFileFile.isFile() || !inputFileFile.canRead()) {
            throw new ParameterException(
                    spec.commandLine(), "Invalid or unreadable input file: " + inputFileFile);
        }
    }
}
