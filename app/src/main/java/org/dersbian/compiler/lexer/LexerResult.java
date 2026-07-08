package org.dersbian.compiler.lexer;

import java.util.List;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.lexer.token.Token;

/**
 * Represents the result of a lexical analysis (tokenization) process.
 *
 * <p>Contains the list of tokens produced by the lexer along with any compilation errors
 * encountered during scanning.
 *
 * @param tokens the list of tokens produced by the lexer
 * @param errors the list of compile errors encountered during lexing
 */
public record LexerResult(List<Token> tokens, List<CompileError> errors) {
    /**
     * Compact canonical constructor that defensively copies both lists to prevent external mutation
     * of internal representation.
     *
     * @param tokens the list of tokens produced by the lexer
     * @param errors the list of compile errors encountered during lexing
     */
    public LexerResult {
        tokens = List.copyOf(tokens);
        errors = List.copyOf(errors);
    }

    /**
     * Returns an unmodifiable view of the token list.
     *
     * @return unmodifiable list of tokens
     */
    @Override
    public List<Token> tokens() {
        return tokens; // already unmodifiable from constructor
    }

    /**
     * Returns an unmodifiable view of the errors list.
     *
     * @return unmodifiable list of compile errors
     */
    @Override
    public List<CompileError> errors() {
        return errors; // already unmodifiable from constructor
    }
}
