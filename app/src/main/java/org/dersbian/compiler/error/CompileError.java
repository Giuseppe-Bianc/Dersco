package org.dersbian.compiler.error;

import static org.dersbian.compiler.error.CompilerErorFormater.formatWithSpan;
import static org.dersbian.compiler.error.CompilerErorFormater.formatWithoutSpan;

import java.util.Objects;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.Span;

/** Sealed hierarchy of compile-time errors for each compiler pipeline phase. */
public sealed interface CompileError
        permits CompileError.LexerError,
                CompileError.SyntaxError,
                CompileError.NameResolutionError,
                CompileError.TypeError,
                CompileError.IrGeneratorError,
                CompileError.AsmGeneratorError {

    String MSG_CODE = "code must not be null";
    String MSG_MESSAGE = "message must not be null";
    String MSG_SPAN = "span must not be null";
    String MSG_HELP = "help must not be null";

    Optional<ErrorCode> code();
    Optional<String> message();
    Optional<Span> span();
    Optional<String> help();

    static LexerError lexerError(
            final ErrorCode code, final String message, final Span span, final String help) {
        return new LexerError(Optional.ofNullable(code), Objects.requireNonNull(message, MSG_MESSAGE),
                Objects.requireNonNull(span, MSG_SPAN), Optional.ofNullable(help));
    }

    static SyntaxError syntaxError(
            final ErrorCode code, final String message, final Span span, final String help) {
        return new SyntaxError(Optional.ofNullable(code), Objects.requireNonNull(message, MSG_MESSAGE),
                Objects.requireNonNull(span, MSG_SPAN), Optional.ofNullable(help));
    }

    /** Creates a dedicated name-resolution diagnostic. */
    static NameResolutionError nameResolutionError(
            final ErrorCode code, final String message, final Span span, final String help) {
        return new NameResolutionError(Optional.ofNullable(code),
                Objects.requireNonNull(message, MSG_MESSAGE), Objects.requireNonNull(span, MSG_SPAN),
                Optional.ofNullable(help));
    }

    static TypeError typeError(
            final ErrorCode code, final String message, final Span span, final String help) {
        return new TypeError(Optional.ofNullable(code), Objects.requireNonNull(message, MSG_MESSAGE),
                Objects.requireNonNull(span, MSG_SPAN), Optional.ofNullable(help));
    }

    static IrGeneratorError irGeneratorError(
            final ErrorCode code, final String message, final Span span, final String help) {
        return new IrGeneratorError(Optional.ofNullable(code), Objects.requireNonNull(message, MSG_MESSAGE),
                Objects.requireNonNull(span, MSG_SPAN), Optional.ofNullable(help));
    }

    static AsmGeneratorError asmGeneratorError(final ErrorCode code, final String message) {
        return new AsmGeneratorError(Optional.ofNullable(code), Objects.requireNonNull(message, MSG_MESSAGE));
    }

    record LexerError(Optional<ErrorCode> errorCode, String errorMessage, Span errorSpan,
                      Optional<String> errorHelp) implements CompileError {
        public LexerError {
            errorCode = Objects.requireNonNull(errorCode, MSG_CODE);
            errorMessage = Objects.requireNonNull(errorMessage, MSG_MESSAGE);
            errorSpan = Objects.requireNonNull(errorSpan, MSG_SPAN);
            errorHelp = Objects.requireNonNull(errorHelp, MSG_HELP);
        }
        @Override public Optional<ErrorCode> code() { return errorCode; }
        @Override public Optional<String> message() { return Optional.of(errorMessage); }
        @Override public Optional<Span> span() { return Optional.of(errorSpan); }
        @Override public Optional<String> help() { return errorHelp; }
        @Override public String toString() { return formatWithSpan("", errorCode, errorMessage, errorSpan, errorHelp); }
    }

    record SyntaxError(Optional<ErrorCode> errorCode, String errorMessage, Span errorSpan,
                       Optional<String> errorHelp) implements CompileError {
        public SyntaxError {
            errorCode = Objects.requireNonNull(errorCode, MSG_CODE);
            errorMessage = Objects.requireNonNull(errorMessage, MSG_MESSAGE);
            errorSpan = Objects.requireNonNull(errorSpan, MSG_SPAN);
            errorHelp = Objects.requireNonNull(errorHelp, MSG_HELP);
        }
        @Override public Optional<ErrorCode> code() { return errorCode; }
        @Override public Optional<String> message() { return Optional.of(errorMessage); }
        @Override public Optional<Span> span() { return Optional.of(errorSpan); }
        @Override public Optional<String> help() { return errorHelp; }
        @Override public String toString() { return formatWithSpan("Syntax error: ", errorCode, errorMessage, errorSpan, errorHelp); }
    }

    /** Name-resolution error, kept distinct from type errors. */
    record NameResolutionError(Optional<ErrorCode> errorCode, String errorMessage, Span errorSpan,
                               Optional<String> errorHelp) implements CompileError {
        public NameResolutionError {
            errorCode = Objects.requireNonNull(errorCode, MSG_CODE);
            errorMessage = Objects.requireNonNull(errorMessage, MSG_MESSAGE);
            errorSpan = Objects.requireNonNull(errorSpan, MSG_SPAN);
            errorHelp = Objects.requireNonNull(errorHelp, MSG_HELP);
        }
        @Override public Optional<ErrorCode> code() { return errorCode; }
        @Override public Optional<String> message() { return Optional.of(errorMessage); }
        @Override public Optional<Span> span() { return Optional.of(errorSpan); }
        @Override public Optional<String> help() { return errorHelp; }
        @Override public String toString() { return formatWithSpan("Name resolution error: ", errorCode, errorMessage, errorSpan, errorHelp); }
    }

    record TypeError(Optional<ErrorCode> errorCode, String errorMessage, Span errorSpan,
                     Optional<String> errorHelp) implements CompileError {
        public TypeError {
            errorCode = Objects.requireNonNull(errorCode, MSG_CODE);
            errorMessage = Objects.requireNonNull(errorMessage, MSG_MESSAGE);
            errorSpan = Objects.requireNonNull(errorSpan, MSG_SPAN);
            errorHelp = Objects.requireNonNull(errorHelp, MSG_HELP);
        }
        @Override public Optional<ErrorCode> code() { return errorCode; }
        @Override public Optional<String> message() { return Optional.of(errorMessage); }
        @Override public Optional<Span> span() { return Optional.of(errorSpan); }
        @Override public Optional<String> help() { return errorHelp; }
        @Override public String toString() { return formatWithSpan("Type error: ", errorCode, errorMessage, errorSpan, errorHelp); }
    }

    record IrGeneratorError(Optional<ErrorCode> errorCode, String errorMessage, Span errorSpan,
                            Optional<String> errorHelp) implements CompileError {
        public IrGeneratorError {
            errorCode = Objects.requireNonNull(errorCode, MSG_CODE);
            errorMessage = Objects.requireNonNull(errorMessage, MSG_MESSAGE);
            errorSpan = Objects.requireNonNull(errorSpan, MSG_SPAN);
            errorHelp = Objects.requireNonNull(errorHelp, MSG_HELP);
        }
        @Override public Optional<ErrorCode> code() { return errorCode; }
        @Override public Optional<String> message() { return Optional.of(errorMessage); }
        @Override public Optional<Span> span() { return Optional.of(errorSpan); }
        @Override public Optional<String> help() { return errorHelp; }
        @Override public String toString() { return formatWithSpan("IR generator error: ", errorCode, errorMessage, errorSpan, errorHelp); }
    }

    record AsmGeneratorError(Optional<ErrorCode> errorCode, String errorMessage) implements CompileError {
        public AsmGeneratorError {
            errorCode = Objects.requireNonNull(errorCode, MSG_CODE);
            errorMessage = Objects.requireNonNull(errorMessage, MSG_MESSAGE);
        }
        @Override public Optional<ErrorCode> code() { return errorCode; }
        @Override public Optional<String> message() { return Optional.of(errorMessage); }
        @Override public Optional<Span> span() { return Optional.empty(); }
        @Override public Optional<String> help() { return Optional.empty(); }
        @Override public String toString() { return formatWithoutSpan("Assembly generation error: ", errorCode, errorMessage); }
    }
}
