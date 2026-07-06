package org.dersbian.compiler;

/** Exception for compiler errors. */
@SuppressWarnings("PMD.MethodArgumentCouldBeFinal")
public class CompilerException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructor.
     *
     * @param message message
     */
    public CompilerException(String message) {
        super(message);
    }

    /**
     * Constructor.
     *
     * @param message message
     * @param cause cause
     */
    public CompilerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructor.
     *
     * @param cause cause
     */
    public CompilerException(Throwable cause) {
        super(cause);
    }
}
