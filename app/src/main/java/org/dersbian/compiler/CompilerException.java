package org.dersbian.compiler;

/**
 * Unchecked exception used to report failures encountered during compilation.
 *
 * <p>The exception can preserve an underlying cause when a lower-level operation, such as source
 * file access, fails.
 */
@SuppressWarnings("PMD.MethodArgumentCouldBeFinal")
public class CompilerException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with the specified message.
     *
     * @param message message describing the compilation failure
     */
    public CompilerException(String message) {
        super(message);
    }

    /**
     * Creates an exception with the specified message and underlying cause.
     *
     * @param message message describing the compilation failure
     * @param cause underlying cause of the failure
     */
    public CompilerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates an exception with the specified underlying cause.
     *
     * @param cause underlying cause of the failure
     */
    public CompilerException(Throwable cause) {
        super(cause);
    }
}
