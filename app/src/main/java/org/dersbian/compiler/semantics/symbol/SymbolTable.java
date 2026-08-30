package org.dersbian.compiler.semantics.symbol;

import java.util.List;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.Type;

/** Contract for lexical symbol binding and scope management. */
@SuppressWarnings("PMD.TooManyMethods")
public interface SymbolTable {
    /**
     * Returns the unique permanent global scope.
     *
     * @return the unique permanent global scope
     */
    Scope globalScope();

    /**
     * Returns the currently active innermost scope.
     *
     * @return the currently active innermost scope
     */
    Scope currentScope();

    /**
     * Enters a new BLOCK or LOOP scope.
     *
     * @param kind scope kind
     * @return newly active scope
     * @throws NullPointerException when kind is null
     * @throws IllegalArgumentException when kind is GLOBAL or FUNCTION
     */
    Scope enterScope(ScopeKind kind);

    /**
     * Enters a function scope owned by a function symbol in the parent scope.
     *
     * @param kind must be FUNCTION
     * @param ownerSymbolId function owner
     * @return newly active scope
     * @throws NullPointerException when an argument is null
     * @throws IllegalArgumentException when kind is not FUNCTION
     * @throws IllegalStateException when the owner is invalid
     */
    Scope enterScope(ScopeKind kind, SymbolId ownerSymbolId);

    /**
     * Opens a structured BLOCK or LOOP scope.
     *
     * @param kind scope kind
     * @return scope handle
     */
    default ScopeHandle openScope(final ScopeKind kind) {
        return new DefaultScopeHandle(this, enterScope(kind));
    }

    /**
     * Exits the current non-global scope.
     *
     * @return parent scope
     * @throws IllegalStateException on global
     */
    Scope exitScope();

    /**
     * Declares a variable in the current scope.
     *
     * @return declaration result for a variable
     */
    DeclarationResult declareVariable(
            String name, Type type, Mutability mutability, Span declarationSpan);

    /**
     * Declares a parameter in the current scope.
     *
     * @return declaration result for a parameter
     */
    DeclarationResult declareParameter(
            String name, Type type, Mutability mutability, int ordinal, Span declarationSpan);

    /**
     * Declares a function in the current scope.
     *
     * @return declaration result for a function
     */
    DeclarationResult declareFunction(
            String name,
            List<ParameterDescriptor> parameters,
            Type returnType,
            Span declarationSpan);

    /**
     * Declares the global main function.
     *
     * @return declaration result for the global main function
     */
    DeclarationResult declareMainFunction(Span declarationSpan);

    /**
     * Looks up the nearest lexical symbol matching the name.
     *
     * @return nearest lexical symbol, or empty
     */
    Optional<Symbol> lookup(String name);

    /**
     * Looks up the nearest lexical symbol from the supplied scope.
     *
     * @return nearest lexical symbol from the supplied scope, or empty
     */
    Optional<Symbol> lookupFrom(ScopeId startScope, String name);

    /**
     * Looks up a local symbol in the current scope.
     *
     * @return local symbol in the current scope, or empty
     */
    Optional<Symbol> lookupLocal(String name);

    /**
     * Looks up a local symbol in the supplied scope.
     *
     * @return local symbol in the supplied scope, or empty
     */
    Optional<Symbol> lookupLocal(ScopeId scopeId, String name);

    /**
     * Finds a symbol with the supplied identity.
     *
     * @return symbol with the supplied identity, or empty
     */
    Optional<Symbol> find(SymbolId symbolId);

    /**
     * Finds a scope with the supplied identity.
     *
     * @return scope with the supplied identity, or empty
     */
    Optional<Scope> findScope(ScopeId scopeId);

    /**
     * Returns an immutable snapshot of current symbols in declaration order.
     *
     * @return immutable declaration-order snapshot of current symbols
     */
    List<Symbol> currentSymbols();

    /**
     * Returns an immutable snapshot of symbols directly in the scope in declaration order.
     *
     * @return immutable declaration-order snapshot of symbols directly in the scope
     */
    List<Symbol> symbolsInScope(ScopeId scopeId);

    /** Structured scope handle implementation. */
    final class DefaultScopeHandle implements ScopeHandle {
        /** The symbol table this handle operates on. */
        private final SymbolTable table;

        /** The scope managed by this handle. */
        private final Scope scopei;

        /** Flag indicating whether this handle has been closed. */
        private boolean closed;

        private DefaultScopeHandle(final SymbolTable table, final Scope scope) {
            this.table = table;
            this.scopei = scope;
        }

        @Override
        public Scope scope() {
            return scopei;
        }

        @Override
        public void close() {
            if (!closed) {
                if (!table.currentScope().id().equals(scopei.id())) {
                    throw new IllegalStateException(
                            "scope handle must be closed while its scope is current");
                }
                table.exitScope();
                closed = true;
            }
        }
    }
}
