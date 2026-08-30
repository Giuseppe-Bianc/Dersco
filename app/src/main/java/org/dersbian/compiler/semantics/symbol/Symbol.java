package org.dersbian.compiler.semantics.symbol;

import org.dersbian.compiler.lexer.token.Span;

/** Common immutable identity and source metadata of a declared symbol. */
@SuppressWarnings("PMD.ShortMethodName")
public interface Symbol {
    /**
     * Returns the stable identity of this symbol.
     *
     * @return stable identity of this symbol
     */
    SymbolId id();

    /**
     * Returns the declared name.
     *
     * @return declared name
     */
    String name();

    /**
     * Returns the symbol kind.
     *
     * @return symbol kind
     */
    SymbolKind kind();

    /**
     * Returns the scope containing this declaration.
     *
     * @return scope containing this declaration
     */
    ScopeId scopeId();

    /**
     * Returns the source span of the declaration.
     *
     * @return source span of the declaration
     */
    Span declarationSpan();
}
