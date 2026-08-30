package org.dersbian.compiler.semantics.symbol;

/** Result of a declaration attempt, including the existing binding on conflict. */
public sealed interface DeclarationResult permits DeclarationResult.Declared, DeclarationResult.AlreadyDeclared {
    /** A declaration accepted by the current scope. */
    record Declared(Symbol symbol) implements DeclarationResult {
        /** Validates the accepted symbol. */
        public Declared {
            if (symbol == null) {
                throw new NullPointerException("symbol must not be null");
            }
        }
    }

    /** A declaration rejected because the same name already exists locally. */
    record AlreadyDeclared(String name, Symbol existingSymbol) implements DeclarationResult {
        /** Validates the duplicate declaration result. */
        public AlreadyDeclared {
            if (name == null) {
                throw new NullPointerException("name must not be null");
            }
            if (existingSymbol == null) {
                throw new NullPointerException("existingSymbol must not be null");
            }
        }
    }
}
