package org.dersbian.compiler.semantics.symbol;

/** Structured handle that closes exactly one scope opened by a symbol table. */
public interface ScopeHandle extends AutoCloseable {
    /**
     * Returns the scope represented by this handle.
     *
     * @return immutable opened scope snapshot
     */
    Scope scope();

    /**
     * Closes the opened scope. Repeated calls are idempotent.
     *
     * @throws IllegalStateException if another scope is current when closing
     */
    @Override
    void close();
}
