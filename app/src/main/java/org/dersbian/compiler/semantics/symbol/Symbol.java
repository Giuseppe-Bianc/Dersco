package org.dersbian.compiler.semantics.symbol;

import java.util.List;
import java.util.Objects;
import org.dersbian.compiler.syntax.ast.Type;

/** Immutable binding stored by the symbol table. */
public record Symbol(
        SymbolId id,
        String name,
        SymbolKind kind,
        Type type,
        Mutability mutability,
        ScopeId scopeId,
        List<ParameterDescriptor> parameters) {
    public Symbol {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(mutability, "mutability must not be null");
        Objects.requireNonNull(scopeId, "scopeId must not be null");
        parameters = List.copyOf(Objects.requireNonNull(parameters, "parameters must not be null"));
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (kind != SymbolKind.FUNCTION && !parameters.isEmpty()) {
            throw new IllegalArgumentException("only functions may have parameters");
        }
    }
}
