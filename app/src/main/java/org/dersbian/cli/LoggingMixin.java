package org.dersbian.cli;

// import ch.qos.logback.classic.Level;
// import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine.Option;

/**
 * Reusable mixin that adds verbosity options to every command/subcommand in which it is included
 * via {@code @Mixin}. As a mixin (rather than a global option on the root command only) it can be
 * placed by the user anywhere on the command line, e.g. {@code dersco compile -v file.der} or
 * {@code dersco -v compile file.der}.
 *
 * <p>Direct manipulation of {@link LoggerContext} is the pattern documented by picocli for
 * programmatic logging configuration from the command line (see picocli wiki: "Configuring logging
 * with picocli"). It involves a deliberate coupling to Logback, confined to this single class.
 */
@SuppressWarnings({
  "PMD.CommentSize",
  "PMD.AtLeastOneConstructor",
  "PMD.ImmutableField",
  "PMD.CommentRequired",
  "PMD.OnlyOneReturn"
})
public final class LoggingMixin {

  @Option(
      names = {"-v", "--verbose"},
      description = {
        "Increase the verbosity of the log output. Repeatable (e.g. -v, -vv, -vvv).",
        "  -v: INFO   -vv: DEBUG   -vvv: TRACE"
      })
  private boolean[] verbosity = {};

  @Option(
      names = {"-q", "--quiet"},
      description = "Suppress non-essential output.")
  private boolean quiet;

  /** Applies the resolved log level to the root logger. Call at the beginning of {@code call()}. */
  public void applyLogLevel() {
    final LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
    context.getLogger(Logger.ROOT_LOGGER_NAME).setLevel(resolveLevel());
  }

  private Level resolveLevel() {
    if (quiet) {
      return Level.ERROR;
    }
    return switch (verbosity.length) {
      case 0 -> Level.WARN;
      case 1 -> Level.INFO;
      case 2 -> Level.DEBUG;
      default -> Level.TRACE;
    };
  }
}
