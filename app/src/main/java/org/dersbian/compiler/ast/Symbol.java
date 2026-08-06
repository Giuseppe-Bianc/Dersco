package org.dersbian.compiler.ast;

/** A declaration resolved during semantic analysis. */
public record Symbol(String name, SymbolKind kind, Decl declaration) {

    /** Validates the resolved symbol. */
    public Symbol {
        name = AstValidation.name(name);
        kind = AstValidation.required(kind, "kind");
        declaration = AstValidation.required(declaration, "declaration");
    }
}
