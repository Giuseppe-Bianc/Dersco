package org.dersbian.compiler;

import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;

/** Default implementation. */
@Slf4j
@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.AvoidUncheckedExceptionsInSignatures"
})
public final class DefaultCompilerService implements ICompilerService {

    @Override
    public void checkSyntax(Path source) throws CompilerException {
        log.debug("Syntax check on {}", source);
        if (!source.toFile().exists()) {
            throw new CompilerException("File not found: " + source);
        }
        // TODO: wire up the real lexer/parser.
    }

    @Override
    public void compile(CompilationRequest request) throws CompilerException {
        log.debug("Starting compilation with parameters: {}", request);
        checkSyntax(request.source());
        // TODO: wire up real semantic analysis and code generation.
    }
}
