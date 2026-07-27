package org.dersbian.compiler.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.io.IOException;
import java.util.Optional;
import org.dersbian.compiler.CompilerException;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CompileError} and its sealed variant hierarchy.
 *
 * <p>Coverage:
 *
 * <ul>
 *   <li>Every sealed variant ({@link CompileError.LexerError}, {@link CompileError.SyntaxError},
 *       {@link CompileError.TypeError}, {@link CompileError.IrGeneratorError}, {@link
 *       CompileError.AsmGeneratorError}, {@link CompileError.IoError}).
 *   <li>Factory null-arg behavior ({@link java.util.Objects#requireNonNull} on {@code message},
 *       {@code span}, {@code cause}).
 *   <li>Record compact-constructor null-arg behavior on every field.
 *   <li>Accessor wrapping in {@link Optional}.
 *   <li>{@code toString()} formatting with and without code/span/help.
 *   <li>Edge cases: empty help, point (zero-length) spans, {@link IOException} with a null message,
 *       fallback to {@link Throwable#toString()}.
 *   <li>{@link CompilerException} propagation from {@link CompileError.IoError}.
 * </ul>
 */
@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.TooManyMethods",
    "PMD.GodClass",
    "PMD.AvoidDuplicateLiterals",
    "checkstyle:AbbreviationAsWordInName"
})
class CompileErrorTest {

    /** Default message used by tests that mirror the Rust {@code make_error!} macro default. */
    private static final String DEFAULT_MESSAGE = "Unexpected token \"@\"";

    /** Builds a span covering columns 1..2 at the given 1-based line. */
    private static Span spanAt(final int line) {
        final SourceLocation start = SourceLocation.create(line, 1, 0L);
        final SourceLocation end = SourceLocation.create(line, 2, 1L);
        return Span.create(start, end);
    }

    // ---------------------------------------------------------------------
    // LexerError
    // ---------------------------------------------------------------------

    @Test
    void lexerErrorInterfaceAccessorsExposeAllData() {
        final Span span = spanAt(1);
        final CompileError.LexerError error =
                CompileError.lexerError(ErrorCode.E0001, "bad token", span, "check it");

        assertThat(error.code()).contains(ErrorCode.E0001);
        assertThat(error.message()).contains("bad token");
        assertThat(error.span()).contains(span);
        assertThat(error.help()).contains("check it");
    }

    @Test
    void lexerErrorRecordFieldsExposeAllData() {
        final Span span = spanAt(1);
        final CompileError.LexerError error =
                CompileError.lexerError(ErrorCode.E0001, "bad token", span, "check it");

        assertThat(error.errorCode()).contains(ErrorCode.E0001);
        assertThat(error.errorMessage()).isEqualTo("bad token");
        assertThat(error.errorSpan()).isEqualTo(span);
        assertThat(error.errorHelp()).contains("check it");
    }

    @Test
    void lexerErrorWithNullCodeReportsEmptyOptional() {
        final CompileError.LexerError error =
                CompileError.lexerError(null, "bad token", spanAt(1), "check it");

        assertThat(error.code()).isEmpty();
        assertThat(error.errorCode()).isEmpty();
    }

    @Test
    void lexerErrorWithNullHelpReportsEmptyOptional() {
        final CompileError.LexerError error =
                CompileError.lexerError(ErrorCode.E0001, "bad token", spanAt(1), null);

        assertThat(error.help()).isEmpty();
        assertThat(error.errorHelp()).isEmpty();
    }

    @Test
    void lexerErrorToStringIncludesCodeMessageSpanAndHelp() {
        final Span span =
                Span.create(SourceLocation.create(1, 1, 0L), SourceLocation.create(1, 6, 5L));
        final CompileError.LexerError error =
                CompileError.lexerError(ErrorCode.E0001, "Invalid token", span, "Check the input");

        assertThat(error.toString())
                .isEqualTo(
                        "[E0001] Invalid token at line 1:column 1-line 1:column 6"
                                + "\nhelp: Check the input");
    }

    @Test
    void lexerErrorToStringOmitsCodePrefixWhenCodeIsNull() {
        final CompileError.LexerError error =
                CompileError.lexerError(null, "unexpected", spanAt(3), null);

        assertThat(error.toString()).isEqualTo("unexpected at line 3:column 1-line 3:column 2");
    }

    @Test
    void lexerErrorToStringOmitsHelpSuffixWhenHelpIsNull() {
        final CompileError.LexerError error =
                CompileError.lexerError(ErrorCode.E0001, "msg", spanAt(1), null);

        assertThat(error.toString()).isEqualTo("[E0001] msg at line 1:column 1-line 1:column 2");
    }

    @Test
    void lexerErrorToStringWithPointSpanRendersSingleLocation() {
        final Span point = Span.point(SourceLocation.create(7, 4, 30L));
        final CompileError.LexerError error =
                CompileError.lexerError(ErrorCode.E0001, "eof", point, null);

        assertThat(error.toString()).isEqualTo("[E0001] eof at line 7:column 4");
    }

    @Test
    void lexerErrorToStringUsesEmptyLabelDistinctFromOtherVariants() {
        final CompileError.LexerError error =
                CompileError.lexerError(ErrorCode.E0001, "msg", spanAt(1), null);

        assertThat(error.toString())
                .doesNotContain("Syntax error:")
                .doesNotContain("Type error:")
                .doesNotContain("IR generator error:")
                .doesNotContain("Assembly generation error:");
    }

    @Test
    void lexerErrorFactoryRejectsNullMessage() {
        assertThatNullPointerException()
                .isThrownBy(() -> CompileError.lexerError(ErrorCode.E0001, null, spanAt(1), null))
                .withMessageContaining("message");
    }

    @Test
    void lexerErrorFactoryRejectsNullSpan() {
        assertThatNullPointerException()
                .isThrownBy(() -> CompileError.lexerError(ErrorCode.E0001, "msg", null, null))
                .withMessageContaining("span");
    }

    @Test
    void lexerErrorRecordConstructorRejectsNullCodeOptional() {
        assertThatNullPointerException()
                .isThrownBy(
                        () -> new CompileError.LexerError(null, "msg", spanAt(1), Optional.empty()))
                .withMessageContaining("code");
    }

    @Test
    void lexerErrorRecordConstructorRejectsNullMessage() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                new CompileError.LexerError(
                                        Optional.of(ErrorCode.E0001),
                                        null,
                                        spanAt(1),
                                        Optional.empty()))
                .withMessageContaining("message");
    }

    @Test
    void lexerErrorRecordConstructorRejectsNullSpan() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                new CompileError.LexerError(
                                        Optional.of(ErrorCode.E0001),
                                        "msg",
                                        null,
                                        Optional.empty()))
                .withMessageContaining("span");
    }

    @Test
    void lexerErrorRecordConstructorRejectsNullHelpOptional() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                new CompileError.LexerError(
                                        Optional.of(ErrorCode.E0001), "msg", spanAt(1), null))
                .withMessageContaining("help");
    }

    // ---------------------------------------------------------------------
    // SyntaxError
    // ---------------------------------------------------------------------

    @Test
    void syntaxErrorInterfaceAccessorsExposeAllData() {
        final Span span = spanAt(2);
        final CompileError.SyntaxError error =
                CompileError.syntaxError(ErrorCode.E1004, "Unexpected token", span, "check token");

        assertThat(error.code()).contains(ErrorCode.E1004);
        assertThat(error.message()).contains("Unexpected token");
        assertThat(error.span()).contains(span);
        assertThat(error.help()).contains("check token");
    }

    @Test
    void syntaxErrorWithNullCodeReportsEmptyOptional() {
        final CompileError.SyntaxError error =
                CompileError.syntaxError(null, "msg", spanAt(1), null);

        assertThat(error.code()).isEmpty();
    }

    @Test
    void syntaxErrorWithNullHelpReportsEmptyOptional() {
        final CompileError.SyntaxError error =
                CompileError.syntaxError(ErrorCode.E1004, "msg", spanAt(1), null);

        assertThat(error.help()).isEmpty();
    }

    @Test
    void syntaxErrorToStringUsesSyntaxLabel() {
        final Span span = Span.point(SourceLocation.create(2, 3, 4L));
        final CompileError.SyntaxError error =
                CompileError.syntaxError(ErrorCode.E1004, "Unexpected token", span, null);

        assertThat(error.toString())
                .isEqualTo("[E1004] Syntax error: Unexpected token at line 2:column 3");
    }

    @Test
    void syntaxErrorToStringIncludesHelpWhenProvided() {
        final CompileError.SyntaxError error =
                CompileError.syntaxError(ErrorCode.E1010, "unmatched", spanAt(5), "check parens");

        assertThat(error.toString())
                .isEqualTo(
                        "[E1010] Syntax error: unmatched at line 5:column 1-line 5:column 2"
                                + "\nhelp: check parens");
    }

    @Test
    void syntaxErrorToStringOmitsCodePrefixWhenCodeIsNull() {
        final CompileError.SyntaxError error =
                CompileError.syntaxError(null, "bad", spanAt(1), null);

        assertThat(error.toString())
                .isEqualTo("Syntax error: bad at line 1:column 1-line 1:column 2");
    }

    @Test
    void syntaxErrorFactoryRejectsNullMessage() {
        assertThatNullPointerException()
                .isThrownBy(() -> CompileError.syntaxError(ErrorCode.E1004, null, spanAt(1), null))
                .withMessageContaining("message");
    }

    @Test
    void syntaxErrorFactoryRejectsNullSpan() {
        assertThatNullPointerException()
                .isThrownBy(() -> CompileError.syntaxError(ErrorCode.E1004, "msg", null, null))
                .withMessageContaining("span");
    }

    @Test
    void syntaxErrorRecordConstructorRejectsNullCodeOptional() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                new CompileError.SyntaxError(
                                        null, "msg", spanAt(1), Optional.empty()))
                .withMessageContaining("code");
    }

    @Test
    void syntaxErrorRecordConstructorRejectsNullHelpOptional() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                new CompileError.SyntaxError(
                                        Optional.of(ErrorCode.E1004), "msg", spanAt(1), null))
                .withMessageContaining("help");
    }

    // ---------------------------------------------------------------------
    // TypeError
    // ---------------------------------------------------------------------

    @Test
    void typeErrorInterfaceAccessorsExposeAllData() {
        final Span span = spanAt(10);
        final CompileError.TypeError error =
                CompileError.typeError(ErrorCode.E2002, "string is not i32", span, null);

        assertThat(error.code()).contains(ErrorCode.E2002);
        assertThat(error.message()).contains("string is not i32");
        assertThat(error.span()).contains(span);
        assertThat(error.help()).isEmpty();
    }

    @Test
    void typeErrorToStringUsesTypeErrorLabel() {
        final CompileError.TypeError error =
                CompileError.typeError(ErrorCode.E2002, "string is not i32", spanAt(10), null);

        assertThat(error.toString())
                .isEqualTo(
                        "[E2002] Type error: string is not i32 at line 10:column 1-line 10:column"
                                + " 2");
    }

    @Test
    void typeErrorToStringIncludesHelpWhenProvided() {
        final CompileError.TypeError error =
                CompileError.typeError(
                        ErrorCode.E2014, "1 < \"hello\"", spanAt(8), "check operand types");

        assertThat(error.toString())
                .isEqualTo(
                        "[E2014] Type error: 1 < \"hello\" at line 8:column 1-line 8:column 2"
                                + "\nhelp: check operand types");
    }

    @Test
    void typeErrorToStringOmitsCodePrefixWhenCodeIsNull() {
        final CompileError.TypeError error =
                CompileError.typeError(null, "type mismatch", spanAt(10), null);

        assertThat(error.toString())
                .isEqualTo("Type error: type mismatch at line 10:column 1-line 10:column 2");
    }

    @Test
    void typeErrorFactoryRejectsNullMessage() {
        assertThatNullPointerException()
                .isThrownBy(() -> CompileError.typeError(ErrorCode.E2002, null, spanAt(1), null))
                .withMessageContaining("message");
    }

    @Test
    void typeErrorFactoryRejectsNullSpan() {
        assertThatNullPointerException()
                .isThrownBy(() -> CompileError.typeError(ErrorCode.E2002, "msg", null, null))
                .withMessageContaining("span");
    }

    @Test
    void typeErrorRecordConstructorRejectsNullCodeOptional() {
        assertThatNullPointerException()
                .isThrownBy(
                        () -> new CompileError.TypeError(null, "msg", spanAt(1), Optional.empty()))
                .withMessageContaining("code");
    }

    @Test
    void typeErrorRecordConstructorRejectsNullHelpOptional() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                new CompileError.TypeError(
                                        Optional.of(ErrorCode.E2002), "msg", spanAt(1), null))
                .withMessageContaining("help");
    }

    // ---------------------------------------------------------------------
    // IrGeneratorError (method names spell "Ir" to satisfy Checkstyle)
    // ---------------------------------------------------------------------

    @Test
    void irGeneratorErrorInterfaceAccessorsExposeAllData() {
        final Span span = spanAt(42);
        final CompileError.IrGeneratorError error =
                CompileError.irGeneratorError(ErrorCode.E3007, "ssa failed", span, null);

        assertThat(error.code()).contains(ErrorCode.E3007);
        assertThat(error.message()).contains("ssa failed");
        assertThat(error.span()).contains(span);
        assertThat(error.help()).isEmpty();
    }

    @Test
    void irGeneratorErrorToStringUsesIrGeneratorLabel() {
        final CompileError.IrGeneratorError error =
                CompileError.irGeneratorError(ErrorCode.E3007, "ssa failed", spanAt(42), null);

        assertThat(error.toString())
                .isEqualTo(
                        "[E3007] IR generator error: ssa failed at line 42:column 1-line 42:column"
                                + " 2");
    }

    @Test
    void irGeneratorErrorToStringIncludesHelpWhenProvided() {
        final CompileError.IrGeneratorError error =
                CompileError.irGeneratorError(
                        ErrorCode.E3005, "bad block", spanAt(1), "review CFG");

        assertThat(error.toString())
                .isEqualTo(
                        "[E3005] IR generator error: bad block at line 1:column 1-line 1:column 2"
                                + "\nhelp: review CFG");
    }

    @Test
    void irGeneratorErrorToStringOmitsCodePrefixWhenCodeIsNull() {
        final CompileError.IrGeneratorError error =
                CompileError.irGeneratorError(null, "ir failure", spanAt(1), null);

        assertThat(error.toString())
                .isEqualTo("IR generator error: ir failure at line 1:column 1-line 1:column 2");
    }

    @Test
    void irGeneratorErrorFactoryRejectsNullMessage() {
        assertThatNullPointerException()
                .isThrownBy(
                        () -> CompileError.irGeneratorError(ErrorCode.E3007, null, spanAt(1), null))
                .withMessageContaining("message");
    }

    @Test
    void irGeneratorErrorFactoryRejectsNullSpan() {
        assertThatNullPointerException()
                .isThrownBy(() -> CompileError.irGeneratorError(ErrorCode.E3007, "msg", null, null))
                .withMessageContaining("span");
    }

    @Test
    void irGeneratorErrorRecordConstructorRejectsNullCodeOptional() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                new CompileError.IrGeneratorError(
                                        null, "msg", spanAt(1), Optional.empty()))
                .withMessageContaining("code");
    }

    @Test
    void irGeneratorErrorRecordConstructorRejectsNullHelpOptional() {
        assertThatNullPointerException()
                .isThrownBy(
                        () ->
                                new CompileError.IrGeneratorError(
                                        Optional.of(ErrorCode.E3007), "msg", spanAt(1), null))
                .withMessageContaining("help");
    }

    // ---------------------------------------------------------------------
    // AsmGeneratorError (span-free by design)
    // ---------------------------------------------------------------------

    @Test
    void asmGeneratorErrorInterfaceAccessorsExposeCode() {
        final CompileError.AsmGeneratorError error =
                CompileError.asmGeneratorError(ErrorCode.E4001, "Invalid instruction");

        assertThat(error.code()).contains(ErrorCode.E4001);
    }

    @Test
    void asmGeneratorErrorInterfaceAccessorsExposeMessage() {
        final CompileError.AsmGeneratorError error =
                CompileError.asmGeneratorError(ErrorCode.E4001, "Invalid instruction");

        assertThat(error.message()).contains("Invalid instruction");
    }

    @Test
    void asmGeneratorErrorRecordFieldsExposeCode() {
        final CompileError.AsmGeneratorError error =
                CompileError.asmGeneratorError(ErrorCode.E4001, "Invalid instruction");

        assertThat(error.errorCode()).contains(ErrorCode.E4001);
    }

    @Test
    void asmGeneratorErrorRecordFieldsExposeMessage() {
        final CompileError.AsmGeneratorError error =
                CompileError.asmGeneratorError(ErrorCode.E4001, "Invalid instruction");

        assertThat(error.errorMessage()).isEqualTo("Invalid instruction");
    }

    @Test
    void asmGeneratorErrorSpanIsAlwaysEmptyByDesign() {
        final CompileError.AsmGeneratorError error =
                CompileError.asmGeneratorError(ErrorCode.E4001, "Invalid instruction");

        assertThat(error.span()).isEmpty();
    }

    @Test
    void asmGeneratorErrorHelpIsAlwaysEmptyByDesign() {
        final CompileError.AsmGeneratorError error =
                CompileError.asmGeneratorError(ErrorCode.E4001, "Invalid instruction");

        assertThat(error.help()).isEmpty();
    }

    @Test
    void asmGeneratorErrorToStringUsesAssemblyLabelWithoutSpan() {
        final CompileError.AsmGeneratorError error =
                CompileError.asmGeneratorError(ErrorCode.E4001, "Invalid instruction");

        assertThat(error.toString())
                .isEqualTo("[E4001] Assembly generation error: Invalid instruction");
    }

    @Test
    void asmGeneratorErrorToStringOmitsCodePrefixWhenCodeIsNull() {
        final CompileError.AsmGeneratorError error =
                CompileError.asmGeneratorError(null, "bad reg");

        assertThat(error.toString()).isEqualTo("Assembly generation error: bad reg");
    }

    @Test
    void asmGeneratorErrorFactoryRejectsNullMessage() {
        assertThatNullPointerException()
                .isThrownBy(() -> CompileError.asmGeneratorError(ErrorCode.E4001, null))
                .withMessageContaining("message");
    }

    @Test
    void asmGeneratorErrorRecordConstructorRejectsNullCodeOptional() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CompileError.AsmGeneratorError(null, "msg"))
                .withMessageContaining("code");
    }

    // ---------------------------------------------------------------------
    // IoError (wraps IOException; carries no code/message/span/help)
    // ---------------------------------------------------------------------

    @Test
    void ioErrorFactoryPreservesCauseReference() {
        final IOException cause = new IOException("disk full");
        final CompileError.IoError error = CompileError.ioError(cause);

        assertThat(error.cause()).isSameAs(cause);
    }

    @Test
    void ioErrorInterfaceAccessorsAreAlwaysEmpty() {
        final CompileError.IoError error = CompileError.ioError(new IOException("x"));

        assertThat(error.code()).isEmpty();
        assertThat(error.message()).isEmpty();
        assertThat(error.span()).isEmpty();
        assertThat(error.help()).isEmpty();
    }

    @Test
    void ioErrorToStringUsesCauseMessageWhenPresent() {
        final CompileError.IoError error = CompileError.ioError(new IOException("disk full"));

        assertThat(error.toString()).isEqualTo("I/O error: disk full");
    }

    @Test
    void ioErrorToStringFallsBackToCauseToStringWhenMessageIsNull() {
        final IOException cause = new IOException();
        final CompileError.IoError error = CompileError.ioError(cause);

        assertThat(cause.getMessage()).isNull();
        assertThat(error.toString()).isEqualTo("I/O error: " + cause.toString());
    }

    @Test
    void ioErrorFactoryRejectsNullCause() {
        assertThatNullPointerException()
                .isThrownBy(() -> CompileError.ioError(null))
                .withMessageContaining("cause");
    }

    @Test
    void ioErrorRecordConstructorRejectsNullCause() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CompileError.IoError(null))
                .withMessageContaining("cause");
    }

    // ---------------------------------------------------------------------
    // CompilerException propagation
    // ---------------------------------------------------------------------

    @Test
    void compilerExceptionCarriesIoErrorMessageAndCause() {
        final IOException cause = new IOException("disk full");
        final CompileError.IoError error = CompileError.ioError(cause);
        final CompilerException exception = new CompilerException(error.toString(), error.cause());

        assertThat(exception.getMessage()).isEqualTo("I/O error: disk full");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void compilerExceptionPreservesLexerErrorToString() {
        final CompileError.LexerError error =
                CompileError.lexerError(ErrorCode.E0001, "bad token", spanAt(7), "check it");
        final CompilerException exception = new CompilerException(error.toString());

        assertThat(exception.getMessage())
                .isEqualTo("[E0001] bad token at line 7:column 1-line 7:column 2\nhelp: check it");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void compilerExceptionWithCauseOnlyWrapsTheUnderlyingIoException() {
        final IOException cause = new IOException("disk full");
        final CompileError.IoError error = CompileError.ioError(cause);
        final CompilerException exception = new CompilerException(error.cause());

        assertThat(exception.getCause()).isSameAs(cause);
        assertThat(exception.getCause().getMessage()).isEqualTo("disk full");
    }

    // ---------------------------------------------------------------------
    // Default-message contract (Rust make_error! mirror, inlined)
    // ---------------------------------------------------------------------

    @Test
    void defaultMessageIsAppliedWhenFactoryIsCalledDirectlyWithNullCode() {
        final CompileError.SyntaxError error =
                CompileError.syntaxError(null, DEFAULT_MESSAGE, spanAt(11), null);

        assertThat(error.errorMessage()).isEqualTo(DEFAULT_MESSAGE);
        assertThat(error.errorCode()).isEmpty();
        assertThat(error.errorHelp()).isEmpty();
        assertThat(error.errorSpan()).isEqualTo(spanAt(11));
    }

    @Test
    void defaultMessageIsAppliedWithProvidedHelpWhenFactoryIsCalledDirectly() {
        final CompileError.LexerError error =
                CompileError.lexerError(null, DEFAULT_MESSAGE, spanAt(4), "review the input");

        assertThat(error.errorMessage()).isEqualTo(DEFAULT_MESSAGE);
        assertThat(error.errorCode()).isEmpty();
        assertThat(error.errorHelp()).contains("review the input");
    }

    @Test
    void spanAtBuildsNonEmptySpanAtGivenLine() {
        final Span span = spanAt(13);

        assertThat(span.start().line()).isEqualTo(13);
        assertThat(span.start().column()).isEqualTo(1);
        assertThat(span.start().offset()).isEqualTo(0L);
        assertThat(span.end().line()).isEqualTo(13);
        assertThat(span.end().column()).isEqualTo(2);
        assertThat(span.end().offset()).isEqualTo(1L);
        assertThat(span.isEmpty()).isFalse();
    }

    @Test
    void spanAtAtLineZeroThrowsBecauseSourceLocationRejectsIt() {
        org.assertj.core.api.Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> spanAt(0));
    }

    @Test
    void spanAtAtNegativeLineThrowsBecauseSourceLocationRejectsIt() {
        org.assertj.core.api.Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> spanAt(-1));
    }
}
