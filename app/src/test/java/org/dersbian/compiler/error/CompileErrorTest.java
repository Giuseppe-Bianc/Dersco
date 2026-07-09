package org.dersbian.compiler.error;

import java.io.IOException;
import org.dersbian.compiler.CompilerException;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestAssertionsShouldIncludeMessage"
})
class CompileErrorTest {

    @Test
    void lexerErrorCarriesStructuredDataAndFormatsLikeRust() {
        final Span span =
                Span.create(SourceLocation.create(1, 1, 0L), SourceLocation.create(1, 6, 5L));
        final CompileError.LexerError error =
                CompileError.lexerError(ErrorCode.E0001, "Invalid token", span, "Check the input");

        Assertions.assertAll(
                () -> Assertions.assertEquals(ErrorCode.E0001, error.code().orElseThrow()),
                () -> Assertions.assertEquals("Invalid token", error.message().orElseThrow()),
                () -> Assertions.assertEquals(span, error.span().orElseThrow()),
                () -> Assertions.assertEquals("Check the input", error.help().orElseThrow()),
                () ->
                        Assertions.assertEquals(
                                "[E0001] Invalid token at line 1:column 1-line 1:column 6\n"
                                        + "help: Check the input",
                                error.toString()));
    }

    @Test
    void syntaxAndAssemblyErrorsRenderWithTheirSpecificLabels() {
        final Span span = Span.point(SourceLocation.create(2, 3, 4L));
        final CompileError.SyntaxError syntaxError =
                CompileError.syntaxError(ErrorCode.E1004, "Unexpected token", span, null);
        final CompileError.AsmGeneratorError asmError =
                CompileError.asmGeneratorError(ErrorCode.E4001, "Invalid instruction");

        Assertions.assertAll(
                () ->
                        Assertions.assertEquals(
                                "[E1004] Syntax error: Unexpected token at line 2:column 3",
                                syntaxError.toString()),
                () ->
                        Assertions.assertEquals(
                                "[E4001] Assembly generation error: Invalid instruction",
                                asmError.toString()));
    }

    @Test
    void ioErrorsPreserveTheWrappedCauseAndCanFeedCompilerException() {
        final IOException ioException = new IOException("disk full");
        final CompileError.IoError ioError = CompileError.ioError(ioException);
        final CompilerException compilerException =
                new CompilerException(ioError.toString(), ioError.cause());

        Assertions.assertAll(
                () -> Assertions.assertSame(ioException, ioError.cause()),
                () -> Assertions.assertEquals("I/O error: disk full", ioError.toString()),
                () ->
                        Assertions.assertEquals(
                                "I/O error: disk full", compilerException.getMessage()),
                () -> Assertions.assertSame(ioException, compilerException.getCause()));
    }
}
