package org.dersbian.compiler.error;

/**
 * Identifies the compiler phase associated with a diagnostic.
 *
 * <p>The enum values are rendered by {@link #toString()} using the short labels used in compiler
 * diagnostics.
 */
public enum CompilerPhase {
    /** Lexical analysis phase. */
    LEXER,
    /** Parsing phase. */
    PARSER,
    /** Semantic analysis phase. */
    SEMANTIC,
    /** Intermediate representation generation phase. */
    IR_GENERATION,
    /** Assembly code generation phase. */
    CODE_GENERATION,
    /** I/O and other system operations. */
    SYSTEM;

    /**
     * Returns the short diagnostic label corresponding to this compiler phase.
     *
     * @return the lowercase diagnostic label for this phase
     */
    @Override
    public String toString() {
        return switch (this) {
            case LEXER -> "lexer";
            case PARSER -> "parser";
            case SEMANTIC -> "semantic";
            case IR_GENERATION -> "ir-gen";
            case CODE_GENERATION -> "codegen";
            case SYSTEM -> "system";
        };
    }
}
