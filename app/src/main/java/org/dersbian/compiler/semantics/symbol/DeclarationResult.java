package org.dersbian.compiler.semantics.symbol;

import java.util.Objects;

/** Result of a declaration attempt, including the existing binding on conflict. */
public sealed interface DeclarationResult
        permits DeclarationResult.Declared, DeclarationResult.AlreadyDeclared {
    /** A declaration accepted by the current scope. */
    record Declared(Symbol symbol) implements DeclarationResult {
        /** Validates the accepted symbol. */
        public Declared {
            Objects.requireNonNull(symbol, "symbol must not be null");
        }
    }

    /** A declaration rejected because the same name already exists locally. */
    record AlreadyDeclared(String name, Symbol existingSymbol) implements DeclarationResult {
        /** Validates the duplicate declaration result. */
        public AlreadyDeclared {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(existingSymbol, "existingSymbol must not be null");
        }
    }
}
