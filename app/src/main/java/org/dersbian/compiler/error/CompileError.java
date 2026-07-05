package org.dersbian.compiler.error;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.Span;

/**
 * Structured compiler errors translated from the Rust model.
 *
 * <p>Rust's {@code Arc<str>} maps to immutable {@link String} values here, and the Rust
 * {@code SourceSpan} equivalent is the existing {@link Span} type already used by the lexer.
 */
public sealed interface CompileError
    permits CompileError.LexerError,
        CompileError.SyntaxError,
        CompileError.TypeError,
        CompileError.IrGeneratorError,
        CompileError.AsmGeneratorError,
        CompileError.IoError {

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
        Objects.requireNonNull(message, "message must not be null"),
        Objects.requireNonNull(span, "span must not be null"),
        Optional.ofNullable(help));
  }

  /** Creates a syntax error. */
  static SyntaxError syntaxError(
      final ErrorCode code, final String message, final Span span, final String help) {
    return new SyntaxError(
        Optional.ofNullable(code),
        Objects.requireNonNull(message, "message must not be null"),
        Objects.requireNonNull(span, "span must not be null"),
        Optional.ofNullable(help));
  }

  /** Creates a type error. */
  static TypeError typeError(
      final ErrorCode code, final String message, final Span span, final String help) {
    return new TypeError(
        Optional.ofNullable(code),
        Objects.requireNonNull(message, "message must not be null"),
        Objects.requireNonNull(span, "span must not be null"),
        Optional.ofNullable(help));
  }

  /** Creates an IR generation error. */
  static IrGeneratorError irGeneratorError(
      final ErrorCode code, final String message, final Span span, final String help) {
    return new IrGeneratorError(
        Optional.ofNullable(code),
        Objects.requireNonNull(message, "message must not be null"),
        Objects.requireNonNull(span, "span must not be null"),
        Optional.ofNullable(help));
  }

  /** Creates an assembly generation error. */
  static AsmGeneratorError asmGeneratorError(final ErrorCode code, final String message) {
    return new AsmGeneratorError(
        Optional.ofNullable(code), Objects.requireNonNull(message, "message must not be null"));
  }

  /** Creates an I/O error wrapper. */
  static IoError ioError(final IOException cause) {
    return new IoError(Objects.requireNonNull(cause, "cause must not be null"));
  }

  /** Formats the optional error code prefix. */
  private static String codePrefix(final Optional<ErrorCode> code) {
    return code.map(value -> "[" + value.code() + "] ").orElse("");
  }

  /** Formats the optional help suffix. */
  private static String helpSuffix(final Optional<String> help) {
    return help.map(value -> "\nhelp: " + value).orElse("");
  }

  /** Formats the display string for errors that include a span. */
  private static String formatWithSpan(
      final String label,
      final Optional<ErrorCode> code,
      final String message,
      final Span span,
      final Optional<String> help) {
    return codePrefix(code) + label + message + " at " + span + helpSuffix(help);
  }

  /** Formats the display string for errors without span information. */
  private static String formatWithoutSpan(
      final String label, final Optional<ErrorCode> code, final String message) {
    return codePrefix(code) + label + message;
  }

  /** Lexical analysis error indicating invalid token sequences. */
  record LexerError(
      Optional<ErrorCode> errorCode,
      String errorMessage,
      Span errorSpan,
      Optional<String> errorHelp)
      implements CompileError {

    public LexerError(
        final Optional<ErrorCode> errorCode,
        final String errorMessage,
        final Span errorSpan,
        final Optional<String> errorHelp) {
      this.errorCode = Objects.requireNonNull(errorCode, "code must not be null");
      this.errorMessage = Objects.requireNonNull(errorMessage, "message must not be null");
      this.errorSpan = Objects.requireNonNull(errorSpan, "span must not be null");
      this.errorHelp = Objects.requireNonNull(errorHelp, "help must not be null");
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

    public SyntaxError(
        final Optional<ErrorCode> errorCode,
        final String errorMessage,
        final Span errorSpan,
        final Optional<String> errorHelp) {
      this.errorCode = Objects.requireNonNull(errorCode, "code must not be null");
      this.errorMessage = Objects.requireNonNull(errorMessage, "message must not be null");
      this.errorSpan = Objects.requireNonNull(errorSpan, "span must not be null");
      this.errorHelp = Objects.requireNonNull(errorHelp, "help must not be null");
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

    public TypeError(
        final Optional<ErrorCode> errorCode,
        final String errorMessage,
        final Span errorSpan,
        final Optional<String> errorHelp) {
      this.errorCode = Objects.requireNonNull(errorCode, "code must not be null");
      this.errorMessage = Objects.requireNonNull(errorMessage, "message must not be null");
      this.errorSpan = Objects.requireNonNull(errorSpan, "span must not be null");
      this.errorHelp = Objects.requireNonNull(errorHelp, "help must not be null");
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

    public IrGeneratorError(
        final Optional<ErrorCode> errorCode,
        final String errorMessage,
        final Span errorSpan,
        final Optional<String> errorHelp) {
      this.errorCode = Objects.requireNonNull(errorCode, "code must not be null");
      this.errorMessage = Objects.requireNonNull(errorMessage, "message must not be null");
      this.errorSpan = Objects.requireNonNull(errorSpan, "span must not be null");
      this.errorHelp = Objects.requireNonNull(errorHelp, "help must not be null");
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

    public AsmGeneratorError(
        final Optional<ErrorCode> errorCode, final String errorMessage) {
      this.errorCode = Objects.requireNonNull(errorCode, "code must not be null");
      this.errorMessage = Objects.requireNonNull(errorMessage, "message must not be null");
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
    public IoError(final IOException cause) {
      this.cause = Objects.requireNonNull(cause, "cause must not be null");
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