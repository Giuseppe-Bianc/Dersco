package org.dersbian.compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.dersbian.compiler.lexer.Lexer;
import org.dersbian.util.FileSizeInfo;
import org.dersbian.util.FileSizeReport;
import org.dersbian.util.SizeSystems;

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
        final FileSizeReport sizeReport =
                new FileSizeReport(
                        new FileSizeInfo(source.toFile().length()),
                        SizeSystems.SI_SYSTEM,
                        SizeSystems.IEC);
        if (log.isDebugEnabled()) {
            log.debug("Read file size:\n{}", sizeReport);
        }
        final String sourceContent;
        try {
            sourceContent = Files.readString(source);
        } catch (IOException e) {
            throw new CompilerException("Failed to read source file: " + source, e);
        }
        final Lexer lexer = new Lexer(source, sourceContent);
        final int nLines = lexer.lineCount();
        if (log.isDebugEnabled()) {
            log.debug("Line count: {}", nLines);
        }
        // TODO: wire up the real parser.
    }

    @Override
    public void compile(CompilationRequest request) throws CompilerException {
        log.trace("Starting compilation with parameters: {}", request);
        checkSyntax(request.source());
        // TODO: wire up real semantic analysis and code generation.
    }
}
