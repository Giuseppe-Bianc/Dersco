package org.dersbian.compiler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.dersbian.compiler.error.ErrorReporter;
import org.dersbian.compiler.lexer.Lexer;
import org.dersbian.compiler.lexer.LexerResult;
import org.dersbian.compiler.lexer.token.Token;
import org.dersbian.compiler.lexer.token.TokenKind;
import org.dersbian.compiler.syntax.ParseResult;
import org.dersbian.compiler.syntax.Parser;
import org.dersbian.compiler.syntax.ast.AstTreePrinter;
import org.dersbian.compiler.syntax.ast.Stmt;
import org.dersbian.util.FileSizeInfo;
import org.dersbian.util.FileSizeReport;
import org.dersbian.util.SizeSystems;

/** Default implementation of the compiler service. */
@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.AvoidUncheckedExceptionsInSignatures"
})
public final class DefaultCompilerService implements ICompilerService {
    /** Logger for this service. */
    private static final org.slf4j.Logger LOG =
            org.slf4j.LoggerFactory.getLogger(DefaultCompilerService.class);

    @Override
    public void checkSyntax(Path source) throws CompilerException {
        LOG.debug("Syntax check on {}", source);
        final FileSizeReport sizeReport =
                new FileSizeReport(
                        new FileSizeInfo(source.toFile().length()),
                        SizeSystems.SI_SYSTEM,
                        SizeSystems.IEC);
        LOG.debug("Read file size:\n{}", sizeReport);
        final String sourceContent;
        try {
            sourceContent = Files.readString(source, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new CompilerException("Failed to read source file: " + source, e);
        }
        final Lexer lexer = new Lexer(source, sourceContent);
        final int nLines = lexer.lineCount();
        LOG.debug("Line count: {}", nLines);

        final LexerResult result = lexer.tokenize();
        final ErrorReporter errorReporter =
                new ErrorReporter(lexer.getLineTracker(), source.toString());
        final String lexErrorReport = errorReporter.reportErrors(result.errors());
        if (!lexErrorReport.isEmpty()) {
            IO.println(lexErrorReport);
            throw new CompilerException(
                    "Compilation failed with " + result.errors().size() + " error(s)");
        }
        /*for (final Token token : result.tokens()) {
            LOG.debug("Token: {}", token);
        }*/
        final List<Token> tokens =
                result.tokens().stream()
                        .filter(
                                token ->
                                        switch (token.type()) {
                                            case TokenKind.Simple.Special.MULTILINE_COMMENT,
                                                    TokenKind.Simple.Special.COMMENT ->
                                                    false;
                                            default -> true;
                                        })
                        .toList();
        final Parser parser = new Parser(tokens);
        final ParseResult parseResult = parser.parse();
        final String errorReport = errorReporter.reportErrors(parseResult.errors());
        if (!errorReport.isEmpty()) {
            IO.println(errorReport);
            throw new CompilerException(
                    "Compilation failed with " + parseResult.errors().size() + " error(s)");
        }
        for (final Stmt stmt : parseResult.statements()) {
            IO.println(AstTreePrinter.prettyPrintStmt(stmt));
        }
    }

    @Override
    public void compile(CompilationRequest request) throws CompilerException {
        LOG.trace("Starting compilation with parameters: {}", request);
        checkSyntax(request.source());
        // TODO: wire up real semantic analysis and code generation.
    }
}
