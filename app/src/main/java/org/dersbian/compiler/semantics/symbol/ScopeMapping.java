package org.dersbian.compiler.semantics.symbol;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.dersbian.compiler.syntax.ast.Stmt;

/** Immutable mapping from AST statements to their lexical scopes. */
public record ScopeMapping(Map<Stmt, ScopeId> statementScopes, List<Scope> scopes) {
    /** Validates and defensively copies scope mappings. */
    public ScopeMapping {
        Objects.requireNonNull(statementScopes, "statementScopes must not be null");
        Objects.requireNonNull(scopes, "scopes must not be null");
        for (final Map.Entry<Stmt, ScopeId> entry : statementScopes.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                throw new IllegalArgumentException("statementScopes must not contain null entries");
            }
        }
        if (scopes.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("scopes must not contain null elements");
        }
        final Map<Stmt, ScopeId> copy = new LinkedHashMap<>(statementScopes);
        statementScopes = Collections.unmodifiableMap(copy);
        scopes = List.copyOf(scopes);
    }

    /** Returns the lexical scope associated with a statement. */
    public Optional<ScopeId> scopeOf(final Stmt statement) {
        Objects.requireNonNull(statement, "statement must not be null");
        return Optional.ofNullable(statementScopes.get(statement));
    }

    /** Returns the scope with the requested identity, if present. */
    public Optional<Scope> find(final ScopeId scopeId) {
        Objects.requireNonNull(scopeId, "scopeId must not be null");
        return scopes.stream().filter(scope -> scope.id().equals(scopeId)).findFirst();
    }
}
