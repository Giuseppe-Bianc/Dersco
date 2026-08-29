package org.dersbian.compiler.semantics.symbol;

import java.util.Objects;

/** Result of attempting to declare a symbol in the current scope. */
public sealed interface DeclarationResult
        permits DeclarationResult.Declared, DeclarationResult.AlreadyDeclared {
    /** Successful declaration. */
    record Declared(Symbol symbol) implements DeclarationResult {
        public Declared {
            Objects.requireNonNull(symbol, "symbol must not be null");
        }
    }

    /** Declaration rejected because the name already exists in the current scope. */
    record AlreadyDeclared(String name, ScopeId scopeId) implements DeclarationResult {
        public AlreadyDeclared {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(scopeId, "scopeId must not be null");
        }
    }
}
