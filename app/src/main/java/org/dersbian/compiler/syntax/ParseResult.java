package org.dersbian.compiler.syntax;

import java.util.List;
import java.util.Objects;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.syntax.ast.Stmt;

/**
 * Result of a parse pass over a token stream.
 *
 * @param statements top-level statements produced by the parser
 * @param errors syntax errors collected during parsing; never {@code null}
 */
public record ParseResult(List<Stmt> statements, List<CompileError.SyntaxError> errors) {

    /**
     * Defensively copies both lists and rejects {@code null} values.
     *
     * @throws NullPointerException if {@code statements} or {@code errors} is {@code null}
     */
    public ParseResult {
        statements = List.copyOf(Objects.requireNonNull(statements, "statements must not be null"));
        errors = List.copyOf(Objects.requireNonNull(errors, "errors must not be null"));
    }

    /**
     * Returns {@code true} if at least one syntax error was collected.
     *
     * @return whether errors is non-empty
     */
    public boolean hasErrors() {
        return !errors.isEmpty();
    }
}
