package org.dersbian.compiler.error;

/** Compiler phase where an error occurred. */
public enum CompilerPhase {
    /** Lexical analysis phase. */
    LEXER,
    /** Parsing phase. */
    PARSER,
    /** Semantic analysis phase. */
    SEMANTIC,
    /** Intermediate representation generation. */
    IR_GENERATION,
    /** Assembly code generation. */
    CODE_GENERATION,
    /** I/O and system operations. */
    SYSTEM;

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
