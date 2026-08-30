package org.dersbian.compiler.semantics.symbol;

import java.util.Objects;

/**
 * Structured handle for a lexical scope opened through {@link SymbolTable#openScope(ScopeKind)}. A
 * handle may be closed exactly once and only while its scope is the active scope.
 */
@SuppressWarnings({"PMD.AvoidFieldNameMatchingMethodName"})
public final class ScopeHandle implements AutoCloseable {
    /** The symbol table in which the scope was opened. */
    private final SymbolTable table;

    /** The scope managed by this handle. */
    private final Scope scope;

    /** Whether this handle has already been closed. */
    private boolean closed;

    /**
     * Constructs a handle for the given scope in the symbol table.
     *
     * @param table the symbol table managing the scope
     * @param scope the active scope to manage
     * @throws NullPointerException if either parameter is null
     */
    public ScopeHandle(final SymbolTable table, final Scope scope) {
        this.table = Objects.requireNonNull(table, "table must not be null");
        this.scope = Objects.requireNonNull(scope, "scope must not be null");
    }

    /** Returns the scope represented by this handle. */
    public Scope scope() {
        return scope;
    }

    /**
     * Closes this scope handle.
     *
     * @throws IllegalStateException if the handle has already been closed or its scope is no longer
     *     active
     */
    @Override
    public void close() {
        if (closed) {
            throw new IllegalStateException("scope handle is already closed");
        }
        if (!table.currentScope().id().equals(scope.id())) {
            throw new IllegalStateException("scope handle is not the active scope");
        }
        table.exitScope();
        closed = true;
    }
}
