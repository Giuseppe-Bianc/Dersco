package org.dersbian.compiler.error;

import static org.dersbian.compiler.error.CompilerErorFormater.formatWithSpan;
import static org.dersbian.compiler.error.CompilerErorFormater.formatWithoutSpan;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.Span;

/**
 * Sealed hierarchy of compile-time errors for each compiler pipeline phase.
 *
 * @see #code()
 * @see #message()
 * @see #span()
 * @see #help()
 */
public sealed interface CompileError
    permits CompileError.LexerError,
        CompileError.SyntaxError,
        CompileError.TypeError,
        CompileError.IrGeneratorError,
        CompileError.AsmGeneratorError,
        CompileError.IoError {

  /** Reusable null-check message for the error-code field. */
  String MSG_CODE = "code must not be null";

  /** Reusable null-check message for the human-readable message field. */
  String MSG_MESSAGE = "message must not be null";

  /** Reusable null-check message for the source-span field. */
  String MSG_SPAN = "span must not be null";

  /** Reusable null-check message for the help field. */
  String MSG_HELP = "help must not be null";

  /** Returns the standardized error code, if one exists. */
  Optional<ErrorCode> code();

  /** Returns the primary human-readable message, if one exists. */
  Optional<String> message();

  /** Returns the source span, if one exists. */
  Optional<Span> span();

  /** Returns help guidance, if one exists. */
  Optional<String> help();

  /** Creates a lexer error. */
  static LexerError lexerError(
      final ErrorCode code, final String message, final Span span, final String help) {
    return new LexerError(
        Optional.ofNullable(code),
        Objects.requireNonNull(message, MSG_MESSAGE),
        Objects.requireNonNull(span, MSG_SPAN),
        Optional.ofNullable(help));
  }

  /** Creates a syntax error. */
  static SyntaxError syntaxError(
      final ErrorCode code, final String message, final Span span, final String help) {
    return new SyntaxError(
        Optional.ofNullable(code),
        Objects.requireNonNull(message, MSG_MESSAGE),
        Objects.requireNonNull(span, MSG_SPAN),
        Optional.ofNullable(help));
  }

  /** Creates a type error. */
  static TypeError typeError(
      final ErrorCode code, final String message, final Span span, final String help) {
    return new TypeError(
        Optional.ofNullable(code),
        Objects.requireNonNull(message, MSG_MESSAGE),
        Objects.requireNonNull(span, MSG_SPAN),
        Optional.ofNullable(help));
  }

  /** Creates an IR generation error. */
  static IrGeneratorError irGeneratorError(
      final ErrorCode code, final String message, final Span span, final String help) {
    return new IrGeneratorError(
        Optional.ofNullable(code),
        Objects.requireNonNull(message, MSG_MESSAGE),
        Objects.requireNonNull(span, MSG_SPAN),
        Optional.ofNullable(help));
  }

  /** Creates an assembly generation error. */
  static AsmGeneratorError asmGeneratorError(final ErrorCode code, final String message) {
    return new AsmGeneratorError(
        Optional.ofNullable(code), Objects.requireNonNull(message, MSG_MESSAGE));
  }

  /** Creates an I/O error wrapper. */
  static IoError ioError(final IOException cause) {
    return new IoError(Objects.requireNonNull(cause, "cause must not be null"));
  }

  /** Lexical analysis error indicating invalid token sequences. */
  record LexerError(
      Optional<ErrorCode> errorCode,
      String errorMessage,
      Span errorSpan,
      Optional<String> errorHelp)
      implements CompileError {

    /**
     * Creates a {@link LexerError} with validated fields.
     *
     * @param errorCode optional standardized error code
     * @param errorMessage human-readable description of the error
     * @param errorSpan source location associated with this error
     * @param errorHelp optional guidance for the developer
     */
    public LexerError(
        final Optional<ErrorCode> errorCode,
        final String errorMessage,
        final Span errorSpan,
        final Optional<String> errorHelp) {
      this.errorCode = Objects.requireNonNull(errorCode, MSG_CODE);
      this.errorMessage = Objects.requireNonNull(errorMessage, MSG_MESSAGE);
      this.errorSpan = Objects.requireNonNull(errorSpan, MSG_SPAN);
      this.errorHelp = Objects.requireNonNull(errorHelp, MSG_HELP);
    }

    @Override
    public Optional<ErrorCode> code() {
      return errorCode;
    }

    @Override
    public Optional<String> message() {
      return Optional.of(errorMessage);
    }

    @Override
    public Optional<Span> span() {
      return Optional.of(errorSpan);
    }

    @Override
    public Optional<String> help() {
      return errorHelp;
    }

    @Override
    public String toString() {
      return formatWithSpan("", errorCode, errorMessage, errorSpan, errorHelp);
    }
  }

  /** Syntax error indicating invalid program structure. */
  record SyntaxError(
      Optional<ErrorCode> errorCode,
      String errorMessage,
      Span errorSpan,
      Optional<String> errorHelp)
      implements CompileError {

    /**
     * Creates a {@link SyntaxError} with validated fields.
     *
     * @param errorCode optional standardized error code
     * @param errorMessage human-readable description of the error
     * @param errorSpan source location associated with this error
     * @param errorHelp optional guidance for the developer
     */
    public SyntaxError(
        final Optional<ErrorCode> errorCode,
        final String errorMessage,
        final Span errorSpan,
        final Optional<String> errorHelp) {
      this.errorCode = Objects.requireNonNull(errorCode, MSG_CODE);
      this.errorMessage = Objects.requireNonNull(errorMessage, MSG_MESSAGE);
      this.errorSpan = Objects.requireNonNull(errorSpan, MSG_SPAN);
      this.errorHelp = Objects.requireNonNull(errorHelp, MSG_HELP);
    }

    @Override
    public Optional<ErrorCode> code() {
      return errorCode;
    }

    @Override
    public Optional<String> message() {
      return Optional.of(errorMessage);
    }

    @Override
    public Optional<Span> span() {
      return Optional.of(errorSpan);
    }

    @Override
    public Optional<String> help() {
      return errorHelp;
    }

    @Override
    public String toString() {
      return formatWithSpan("Syntax error: ", errorCode, errorMessage, errorSpan, errorHelp);
    }
  }

  /** Type checking error indicating type mismatches or unsupported operations. */
  record TypeError(
      Optional<ErrorCode> errorCode,
      String errorMessage,
      Span errorSpan,
      Optional<String> errorHelp)
      implements CompileError {

    /**
     * Creates a {@link TypeError} with validated fields.
     *
     * @param errorCode optional standardized error code
     * @param errorMessage human-readable description of the error
     * @param errorSpan source location associated with this error
     * @param errorHelp optional guidance for the developer
     */
    public TypeError(
        final Optional<ErrorCode> errorCode,
        final String errorMessage,
        final Span errorSpan,
        final Optional<String> errorHelp) {
      this.errorCode = Objects.requireNonNull(errorCode, MSG_CODE);
      this.errorMessage = Objects.requireNonNull(errorMessage, MSG_MESSAGE);
      this.errorSpan = Objects.requireNonNull(errorSpan, MSG_SPAN);
      this.errorHelp = Objects.requireNonNull(errorHelp, MSG_HELP);
    }

    @Override
    public Optional<ErrorCode> code() {
      return errorCode;
    }

    @Override
    public Optional<String> message() {
      return Optional.of(errorMessage);
    }

    @Override
    public Optional<Span> span() {
      return Optional.of(errorSpan);
    }

    @Override
    public Optional<String> help() {
      return errorHelp;
    }

    @Override
    public String toString() {
      return formatWithSpan("Type error: ", errorCode, errorMessage, errorSpan, errorHelp);
    }
  }

  /** Error during intermediate representation generation. */
  record IrGeneratorError(
      Optional<ErrorCode> errorCode,
      String errorMessage,
      Span errorSpan,
      Optional<String> errorHelp)
      implements CompileError {

    /**
     * Creates an {@link IrGeneratorError} with validated fields.
     *
     * @param errorCode optional standardized error code
     * @param errorMessage human-readable description of the error
     * @param errorSpan source location associated with this error
     * @param errorHelp optional guidance for the developer
     */
    public IrGeneratorError(
        final Optional<ErrorCode> errorCode,
        final String errorMessage,
        final Span errorSpan,
        final Optional<String> errorHelp) {
      this.errorCode = Objects.requireNonNull(errorCode, MSG_CODE);
      this.errorMessage = Objects.requireNonNull(errorMessage, MSG_MESSAGE);
      this.errorSpan = Objects.requireNonNull(errorSpan, MSG_SPAN);
      this.errorHelp = Objects.requireNonNull(errorHelp, MSG_HELP);
    }

    @Override
    public Optional<ErrorCode> code() {
      return errorCode;
    }

    @Override
    public Optional<String> message() {
      return Optional.of(errorMessage);
    }

    @Override
    public Optional<Span> span() {
      return Optional.of(errorSpan);
    }

    @Override
    public Optional<String> help() {
      return errorHelp;
    }

    @Override
    public String toString() {
      return formatWithSpan("IR generator error: ", errorCode, errorMessage, errorSpan, errorHelp);
    }
  }

  /** Error during assembly code generation. */
  record AsmGeneratorError(Optional<ErrorCode> errorCode, String errorMessage)
      implements CompileError {

    /**
     * Creates an {@link AsmGeneratorError} with validated fields.
     *
     * @param errorCode optional standardized error code
     * @param errorMessage human-readable description of the error
     */
    public AsmGeneratorError(final Optional<ErrorCode> errorCode, final String errorMessage) {
      this.errorCode = Objects.requireNonNull(errorCode, MSG_CODE);
      this.errorMessage = Objects.requireNonNull(errorMessage, MSG_MESSAGE);
    }

    @Override
    public Optional<ErrorCode> code() {
      return errorCode;
    }

    @Override
    public Optional<String> message() {
      return Optional.of(errorMessage);
    }

    @Override
    public Optional<Span> span() {
      return Optional.empty();
    }

    @Override
    public Optional<String> help() {
      return Optional.empty();
    }

    @Override
    public String toString() {
      return formatWithoutSpan("Assembly generation error: ", errorCode, errorMessage);
    }
  }

  /** I/O operation failure during compilation. */
  record IoError(IOException cause) implements CompileError {

    /**
     * Creates an {@link IoError} wrapping the given {@link IOException}.
     *
     * @param cause the underlying I/O failure; must not be {@code null}
     */
    public IoError(final IOException cause) {
      this.cause = Objects.requireNonNull(cause, "cause must not be null");
    }

    @Override
    @SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification =
            "IOException is an exception wrapper; defensive copying would "
                + "discard the stack trace, cause chain and suppressed exceptions, which are "
                + "required for accurate error reporting. IoError is an internal, immutable "
                + "compiler-diagnostics value not exposed to untrusted callers.")
    public IOException cause() {
      return cause;
    }

    @Override
    public Optional<ErrorCode> code() {
      return Optional.empty();
    }

    @Override
    public Optional<String> message() {
      return Optional.empty();
    }

    @Override
    public Optional<Span> span() {
      return Optional.empty();
    }

    @Override
    public Optional<String> help() {
      return Optional.empty();
    }

    @Override
    public String toString() {
      final String detail = cause.getMessage() != null ? cause.getMessage() : cause.toString();
      return "I/O error: " + detail;
    }
  }
}
