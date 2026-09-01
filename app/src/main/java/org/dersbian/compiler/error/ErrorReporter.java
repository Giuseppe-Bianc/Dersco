package org.dersbian.compiler.error;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.location.LineTracker;

/** Enhanced error reporter with source context display. */
public final class ErrorReporter {
    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String BLUE = "\u001B[34m";
    private static final String CYAN = "\u001B[36m";
    private static final String BOLD = "\u001B[1m";
    private final LineTracker lineTracker;
    private final String sourceFile;

    public ErrorReporter(final LineTracker lineTracker, final String sourceFile) {
        this.lineTracker = Objects.requireNonNull(lineTracker, "lineTracker must not be null");
        this.sourceFile = Objects.requireNonNull(sourceFile, "sourceFile must not be null");
    }

    /** Returns a formatted string containing all compile errors with source context. */
    public String reportErrors(final List<CompileError> errors) {
        final StringBuilder output = new StringBuilder(errors.size() * 500);
        for (final CompileError error : errors) {
            final String formatted = switch (error) {
                case CompileError.LexerError value -> formatError("LEX", value.errorMessage(), value.errorSpan(), value.errorHelp().orElse(null), value.errorCode());
                case CompileError.SyntaxError value -> formatError("SYNTAX", value.errorMessage(), value.errorSpan(), value.errorHelp().orElse(null), value.errorCode());
                case CompileError.NameResolutionError value -> formatError("NAME", value.errorMessage(), value.errorSpan(), value.errorHelp().orElse(null), value.errorCode());
                case CompileError.TypeError value -> formatError("TYPE", value.errorMessage(), value.errorSpan(), value.errorHelp().orElse(null), value.errorCode());
                case CompileError.IrGeneratorError value -> formatError("IR GEN", value.errorMessage(), value.errorSpan(), value.errorHelp().orElse(null), value.errorCode());
                case CompileError.AsmGeneratorError value -> formatSimpleError("ASM GEN", value.errorMessage(), value.errorCode());
            };
            output.append(formatted);
        }
        return output.toString();
    }

    private String formatError(final String category, final String message, final Span span,
                               final String help, final Optional<ErrorCode> code) {
        final int startLine = span.start().line();
        final int startColumn = span.start().column();
        final int endLine = span.end().line();
        final int endColumn = span.end().column();
        final String sourceLine = lineTracker.getLine(startLine).orElse("");
        final int estimatedCapacity = 100 + message.length() + category.length()
                + sourceLine.length() + helpLength(help) + 50;
        final StringBuilder output = new StringBuilder(estimatedCapacity);
        output.append(style("ERROR", RED, BOLD)).append(codePrefix(code)).append(style(category, RED))
                .append(": ").append(style(message, YELLOW)).append('\n').append(style("Location:", BLUE))
                .append(' ').append(style(sourceFile + ":" + span, CYAN));
        if (!sourceLine.isEmpty()) {
            output.append('\n').append(String.format("%4d │ %s", startLine, sourceLine));
            final int startOffset = Math.max(startColumn - 1, 0);
            final String underline = startLine == endLine
                    ? " ".repeat(startOffset) + "^".repeat(Math.max(endColumn - startColumn, 1))
                    : " ".repeat(startOffset) + "^";
            output.append("\n     │ ").append(style(underline, RED, BOLD));
            if (startLine != endLine) {
                output.append("\n     │ ").append(style("...", BLUE)).append(" (error spans lines ")
                        .append(startLine).append('-').append(endLine).append(')');
            }
        }
        if (help != null) {
            output.append('\n').append(style("help:", BLUE, BOLD)).append(' ').append(style(help, GREEN));
        }
        output.append('\n');
        return output.toString();
    }

    private static String formatSimpleError(final String errorType, final String message,
                                             final Optional<ErrorCode> code) {
        return style("ERROR", RED, BOLD) + codePrefix(code) + style(errorType, RED) + ": "
                + style(message, YELLOW) + '\n';
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

    private static int helpLength(final String help) {
        return help == null ? 0 : help.length() + 20;
    }
}
