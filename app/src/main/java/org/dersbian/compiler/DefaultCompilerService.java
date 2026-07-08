package org.dersbian.compiler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.extern.slf4j.Slf4j;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.lexer.Lexer;
import org.dersbian.compiler.lexer.LexerResult;
import org.dersbian.compiler.lexer.token.Token;
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
            sourceContent = Files.readString(source, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CompilerException("Failed to read source file: " + source, e);
        }
        final Lexer lexer = new Lexer(source, sourceContent);
        final int nLines = lexer.lineCount();
        if (log.isDebugEnabled()) {
            log.debug("Line count: {}", nLines);
        }
        final LexerResult result = lexer.tokenize();
        if (result.errors().isEmpty()) {
            log.debug("No syntax errors found.");
        } else {
            if (log.isDebugEnabled()) {
                log.debug("Found {} syntax errors.", result.errors().size());
            }
            for (final CompileError error : result.errors()) {
                log.error("Syntax error: {}", error);
            }
        }
        for (final Token token : result.tokens()) {
            log.info("Token: {}", token);
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
