package org.dersbian.compiler.semantics.symbol;

import java.util.List;
import java.util.Objects;

/** Immutable result of binding declarations from an AST traversal. */
public record SymbolBindingResult(List<DeclarationResult> declarations) {
    /** Validates and defensively copies declaration results. */
    public SymbolBindingResult {
        Objects.requireNonNull(declarations, "declarations must not be null");
        if (declarations.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("declarations must not contain null");
        }
        declarations = List.copyOf(declarations);
    }
}
