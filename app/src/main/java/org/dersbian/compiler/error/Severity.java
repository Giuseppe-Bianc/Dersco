package org.dersbian.compiler.error;

/** Severity levels for compiler diagnostics. */
public enum Severity {
    /** Informational note, does not affect compilation. */
    NOTE,
    /** Warning that might indicate a problem. */
    WARNING,
    /** Error that prevents successful compilation. */
    ERROR,
    /** Fatal error that stops compilation immediately. */
    FATAL;

    @Override
    public String toString() {
        return switch (this) {
            case NOTE -> "note";
            case WARNING -> "warning";
            case ERROR -> "error";
            case FATAL -> "fatal";
        };
    }
}
