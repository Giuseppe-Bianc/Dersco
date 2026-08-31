package org.dersbian.compiler.semantics.symbol;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.dersbian.compiler.syntax.ast.Stmt;

/** Immutable association between AST statements and the scopes in which they were bound. */
public record BindingContext(
        Map<Stmt, ScopeId> statementScopes,
        List<DeclarationResult> declarations) {
    /** Validates and defensively copies binding context collections. */
    public BindingContext {
        Objects.requireNonNull(statementScopes, "statementScopes must not be null");
        Objects.requireNonNull(declarations, "declarations must not be null");
        if (statementScopes.entrySet().stream()
                .anyMatch(entry -> entry.getKey() == null || entry.getValue() == null)) {
            throw new NullPointerException("statementScopes must not contain null keys or values");
        }
        if (declarations.stream().anyMatch(Objects::isNull)) {
            throw new NullPointerException("declarations must not contain null");
        }
        statementScopes = Map.copyOf(statementScopes);
        declarations = List.copyOf(declarations);
    }

    /**
     * Returns the scope in which a statement was entered.
     *
     * @param statement AST statement
     * @return associated scope, or empty when the statement was not bound
     */
    public Optional<ScopeId> scopeOf(final Stmt statement) {
        Objects.requireNonNull(statement, "statement must not be null");
        return Optional.ofNullable(statementScopes.get(statement));
    }
}
