package org.dersbian.compiler;

import java.nio.file.Path;

/** Compiler service interface. */
@SuppressWarnings({
    "PMD.CommentSize",
    "PMD.AvoidUncheckedExceptionsInSignatures",
    "checkstyle:AbbreviationAsWordInName"
})
public interface ICompilerService {

    /**
     * Checks only the syntactic correctness of the source file, without producing any output.
     *
     * @param source source file to analyze.
     * @throws CompilerException if the source contains syntax errors.
     */
    void checkSyntax(Path source) throws CompilerException;

    /**
     * Compiles the source file producing the requested output file.
     *
     * @param request compilation parameters.
     * @throws CompilerException if the compilation fails.
     */
    void compile(CompilationRequest request) throws CompilerException;
}
