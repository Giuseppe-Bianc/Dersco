package org.dersbian.compiler;

import java.nio.file.Path;

/**
 * Defines the compiler operations exposed to the CLI layer.
 *
 * <p>The service separates source checking from the compilation entry point. Implementations are
 * responsible for coordinating the compiler phases and reporting failures through {@link
 * CompilerException}.
 */
@SuppressWarnings({
    "PMD.AvoidUncheckedExceptionsInSignatures",
    "checkstyle:AbbreviationAsWordInName"
})
public interface ICompilerService {

    /**
     * Checks only the syntactic correctness of the source file, without producing any output.
     *
     * @param source source file to analyze.
     * @throws CompilerException if the source cannot be processed or contains syntax errors.
     */
    void checkSyntax(Path source) throws CompilerException;

    /**
     * Compiles the source file according to the parameters contained in the request.
     *
     * <p>The request determines the source and output paths together with compilation options such
     * as the optimization level, intermediate-code emission, and diagnostics.
     *
     * @param request compilation parameters.
     * @throws CompilerException if the compilation fails.
     */
    void compile(CompilationRequest request) throws CompilerException;
}
