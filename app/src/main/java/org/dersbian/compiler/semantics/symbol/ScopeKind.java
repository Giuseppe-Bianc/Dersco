package org.dersbian.compiler.semantics.symbol;

/** Kinds of lexical scopes supported by symbol table version one. */
public enum ScopeKind {
    GLOBAL,
    FUNCTION,
    BLOCK,
    LOOP
}
