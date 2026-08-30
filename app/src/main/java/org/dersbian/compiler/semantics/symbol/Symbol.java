package org.dersbian.compiler.semantics.symbol;

import org.dersbian.compiler.lexer.token.Span;

/** Common immutable identity and source metadata of a declared symbol. */
public interface Symbol {
    /** @return stable identity of this symbol */
    SymbolId id();

    /** @return declared name */
    String name();

    /** @return symbol kind */
    SymbolKind kind();

    /** @return scope containing this declaration */
    ScopeId scopeId();

    /** @return source span of the declaration */
    Span declarationSpan();
}
