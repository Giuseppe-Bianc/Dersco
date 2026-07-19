package org.dersbian.compiler.error;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.location.LineTracker;

/** Enhanced error reporter with source context display. */
public final class ErrorReporter {
    /** ANSI escape code to reset console formatting. */
    private static final String RESET = "\u001B[0m";

    /** ANSI escape code for red text color. */
    private static final String RED = "\u001B[31m";

    /** ANSI escape code for green text color. */
    private static final String GREEN = "\u001B[32m";

    /** ANSI escape code for yellow text color. */
    private static final String YELLOW = "\u001B[33m";

    /** ANSI escape code for blue text color. */
    private static final String BLUE = "\u001B[34m";

    /** ANSI escape code for cyan text color. */
    private static final String CYAN = "\u001B[36m";

    /** ANSI escape code for bold text formatting. */
    private static final String BOLD = "\u001B[1m";

    /** Line tracker used to resolve source lines and spans for error reporting. */
    private final LineTracker lineTracker;

    /** Human-readable identifier of the source being reported (e.g. file path). */
    private final String sourceFile;

    /**
     * Creates an {@link ErrorReporter} backed by the given {@link LineTracker}.
     *
     * @param lineTracker used when rendering span information; must not be null.
     * @param sourceFile human-readable identifier of the source (e.g. file path); must not be null.
     */
    public ErrorReporter(final LineTracker lineTracker, final String sourceFile) {
        this.lineTracker = Objects.requireNonNull(lineTracker, "lineTracker must not be null");
        this.sourceFile = Objects.requireNonNull(sourceFile, "sourceFile must not be null");
    }

    /** Returns a formatted string containing all compile errors with source context. */
    public String reportErrors(final List<CompileError> errors) {
        final StringBuilder output = new StringBuilder(errors.size() * 500);
        for (final CompileError error : errors) {
            final String formatted =
                    switch (error) {
                        case CompileError.LexerError lexerError ->
                                formatError(
                                        "LEX",
                                        lexerError.errorMessage(),
                                        lexerError.errorSpan(),
                                        lexerError.errorHelp().orElse(null),
                                        lexerError.errorCode());
                        case CompileError.SyntaxError syntaxError ->
                                formatError(
                                        "SYNTAX",
                                        syntaxError.errorMessage(),
                                        syntaxError.errorSpan(),
                                        syntaxError.errorHelp().orElse(null),
                                        syntaxError.errorCode());
                        case CompileError.TypeError typeError ->
                                formatError(
                                        "TYPE",
                                        typeError.errorMessage(),
                                        typeError.errorSpan(),
                                        typeError.errorHelp().orElse(null),
                                        typeError.errorCode());
                        case CompileError.IrGeneratorError irGeneratorError ->
                                formatError(
                                        "IR GEN",
                                        irGeneratorError.errorMessage(),
                                        irGeneratorError.errorSpan(),
                                        irGeneratorError.errorHelp().orElse(null),
                                        irGeneratorError.errorCode());
                        case CompileError.AsmGeneratorError asmGeneratorError ->
                                formatSimpleError(
                                        "ASM GEN",
                                        asmGeneratorError.errorMessage(),
                                        asmGeneratorError.errorCode());
                        case CompileError.IoError ioError ->
                                formatSimpleError(
                                        "I/O", ioMessage(ioError.cause()), Optional.empty());
                    };
            output.append(formatted);
        }
        return output.toString();
    }

    private String formatError(
            final String category,
            final String message,
            final Span span,
            final String help,
            final Optional<ErrorCode> code) {
        final int startLine = span.start().line();
        final int startColumn = span.start().column();
        final int endLine = span.end().line();
        final int endColumn = span.end().column();

        final String sourceLine = lineTracker.getLine(startLine).orElse("");
        final int estimatedCapacity =
                100
                        + message.length()
                        + category.length()
                        + sourceLine.length()
                        + helpLength(help)
                        + 50;
        final StringBuilder output = new StringBuilder(estimatedCapacity);

        output.append(style("ERROR", RED, BOLD))
                .append(codePrefix(code))
                .append(style(category, RED))
                .append(": ")
                .append(style(message, YELLOW))
                .append('\n')
                .append(style("Location:", BLUE))
                .append(' ')
                .append(style(sourceFile + ":" + span.toString(), CYAN));

        if (!sourceLine.isEmpty()) {
            output.append('\n').append(String.format("%4d │ %s", startLine, sourceLine));
            final int startOffset = Math.max(startColumn - 1, 0);
            final String underline =
                    startLine == endLine
                            ? " ".repeat(startOffset)
                                    + "^".repeat(Math.max(endColumn - startColumn, 1))
                            : " ".repeat(startOffset) + "^";

            output.append("\n     │ ").append(style(underline, RED, BOLD));

            if (startLine != endLine) {
                output.append("\n     │ ")
                        .append(style("...", BLUE))
                        .append(" (error spans lines ")
                        .append(startLine)
                        .append('-')
                        .append(endLine)
                        .append(')');
            }
        }

        if (help != null) {
            output.append('\n')
                    .append(style("help:", BLUE, BOLD))
                    .append(' ')
                    .append(style(help, GREEN));
        }

        return output.toString();
    }

    private static String formatSimpleError(
            final String errorType, final String message, final Optional<ErrorCode> code) {
        return style("ERROR", RED, BOLD)
                + codePrefix(code)
                + style(errorType, RED)
                + ": "
                + style(message, YELLOW)
                + '\n';
    }

    private static String codePrefix(final Optional<ErrorCode> code) {
        return code.map(value -> " [" + style(value.code(), RED, BOLD) + "] ").orElse(" ");
    }

    private static String style(final String text, final String... ansiCodes) {
        final StringBuilder builder = new StringBuilder();
        for (final String code : ansiCodes) {
            builder.append(code);
        }
        return builder.append(text).append(RESET).toString();
    }

    private static String ioMessage(final Throwable throwable) {
        return throwable.getMessage() != null ? throwable.getMessage() : throwable.toString();
    }

    private static int helpLength(final String help) {
        return help == null ? 0 : help.length() + 20;
    }
}
