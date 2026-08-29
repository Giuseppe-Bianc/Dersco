package org.dersbian.compiler.semantics.symbol;

/** Semantic kind of lexical scope. */
public enum ScopeKind {
    GLOBAL,
    FUNCTION,
    BLOCK,
    LOOP
}
