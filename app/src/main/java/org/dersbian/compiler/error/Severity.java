package org.dersbian.compiler.error;

/**
 * Severity levels assigned to compiler diagnostics.
 *
 * <p>The enum values are rendered by {@link #toString()} using the lowercase labels used in
 * diagnostics.
 */
public enum Severity {
    /** Informational note that does not affect compilation. */
    NOTE,
    /** Warning that might indicate a problem. */
    WARNING,
    /** Error that prevents successful compilation. */
    ERROR,
    /** Fatal error that stops compilation immediately. */
    FATAL;

    /**
     * Returns the lowercase diagnostic label corresponding to this severity.
     *
     * @return the diagnostic label for this severity
     */
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
