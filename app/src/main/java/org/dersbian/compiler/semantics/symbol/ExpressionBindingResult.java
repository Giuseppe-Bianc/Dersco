package org.dersbian.compiler.semantics.symbol;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.dersbian.compiler.syntax.ast.Expr;

/** Immutable lexical binding result for expression references. */
public record ExpressionBindingResult(Map<Expr, SymbolId> bindings) {
    /** Validates and defensively copies expression bindings. */
    public ExpressionBindingResult {
        Objects.requireNonNull(bindings, "bindings must not be null");
        if (bindings.entrySet().stream()
                .anyMatch(entry -> entry.getKey() == null || entry.getValue() == null)) {
            throw new NullPointerException("bindings must not contain null keys or values");
        }
        bindings = Map.copyOf(bindings);
    }

    /**
     * Returns the symbol bound to an expression reference.
     *
     * @param expression expression to query
     * @return bound symbol identifier, or empty when the expression has no binding
     */
    public Optional<SymbolId> symbolOf(final Expr expression) {
        Objects.requireNonNull(expression, "expression must not be null");
        return Optional.ofNullable(bindings.get(expression));
    }
}
