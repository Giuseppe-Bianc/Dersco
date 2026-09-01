package org.dersbian.compiler.semantics.symbol;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.syntax.ast.Expr;
import org.dersbian.compiler.syntax.ast.Stmt;

/** Immutable result of declaration binding and lexical name resolution. */
public record NameResolutionResult(
        SymbolBindingResult declarations,
        BindingContext scopes,
        Map<Expr, SymbolId> referenceBindings,
        List<CompileError> diagnostics) {
    /** Validates and defensively copies the resolution result. */
    public NameResolutionResult {
        Objects.requireNonNull(declarations, "declarations must not be null");
        Objects.requireNonNull(scopes, "scopes must not be null");
        Objects.requireNonNull(referenceBindings, "referenceBindings must not be null");
        Objects.requireNonNull(diagnostics, "diagnostics must not be null");
        if (referenceBindings.entrySet().stream()
                .anyMatch(entry -> entry.getKey() == null || entry.getValue() == null)) {
            throw new IllegalArgumentException("referenceBindings must not contain null entries");
        }
        if (diagnostics.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("diagnostics must not contain null elements");
        }
        referenceBindings = Map.copyOf(referenceBindings);
        diagnostics = List.copyOf(diagnostics);
    }

    /** Returns the resolved symbol for a reference, if one exists. */
    public java.util.Optional<SymbolId> bindingOf(final Expr reference) {
        Objects.requireNonNull(reference, "reference must not be null");
        return java.util.Optional.ofNullable(referenceBindings.get(reference));
    }

    /** Returns the lexical scope associated with a statement, if known. */
    public java.util.Optional<ScopeId> scopeOf(final Stmt statement) {
        return scopes.scopeOf(statement);
    }
}
