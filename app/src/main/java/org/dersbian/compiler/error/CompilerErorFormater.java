package org.dersbian.compiler.error;

import java.util.Optional;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.dersbian.compiler.lexer.token.Span;

/** Utility class responsible for formatting compiler error messages. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CompilerErorFormater {

  /** Formats the optional error code prefix. */
  private static String codePrefix(final Optional<ErrorCode> code) {
    return code.map(value -> "[" + value.code() + "] ").orElse("");
  }

  /** Formats the optional help suffix. */
  private static String helpSuffix(final Optional<String> help) {
    return help.map(value -> "\nhelp: " + value).orElse("");
  }

  /** Formats the display string for errors that include a span. */
  public static String formatWithSpan(
      final String label,
      final Optional<ErrorCode> code,
      final String message,
      final Span span,
      final Optional<String> help) {
    return codePrefix(code) + label + message + " at " + span + helpSuffix(help);
  }

  /** Formats the display string for errors without span information. */
  public static String formatWithoutSpan(
      final String label, final Optional<ErrorCode> code, final String message) {
    return codePrefix(code) + label + message;
  }
}
