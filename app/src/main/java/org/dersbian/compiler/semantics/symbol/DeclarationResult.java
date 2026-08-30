package org.dersbian.compiler.semantics.symbol;

import java.util.Objects;

/** Result of an attempt to declare a symbol in the current lexical scope. */
public sealed interface DeclarationResult
        permits DeclarationResult.Declared, DeclarationResult.AlreadyDeclared {
    /** Successful declaration containing the exact registered symbol instance. */
    record Declared(Symbol symbol) implements DeclarationResult {
        public Declared {
            Objects.requireNonNull(symbol, "symbol must not be null");
        }
    }

    /**
     * Declaration rejected because an existing symbol already owns the name in this scope.
     *
     * @param name requested declaration name
     * @param existingSymbol symbol that prevented the declaration
     */
    record AlreadyDeclared(String name, Symbol existingSymbol) implements DeclarationResult {
        public AlreadyDeclared {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(existingSymbol, "existingSymbol must not be null");
            if (name.isEmpty()) {
                throw new IllegalArgumentException("name must not be empty");
            }
        }
    }
}
