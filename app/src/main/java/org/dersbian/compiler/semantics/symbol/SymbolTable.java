package org.dersbian.compiler.semantics.symbol;

import java.util.List;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.Type;

/** Contract for lexical symbol binding and scope management. */
@SuppressWarnings("PMD.TooManyMethods")
public interface SymbolTable {
    /** @return the unique permanent global scope */
    Scope globalScope();
    /** @return the currently active innermost scope */
    Scope currentScope();
    /**
     * Enters a new BLOCK or LOOP scope.
     * @param kind scope kind
     * @return newly active scope
     * @throws NullPointerException when kind is null
     * @throws IllegalArgumentException when kind is GLOBAL or FUNCTION
     */
    Scope enterScope(ScopeKind kind);
    /**
     * Enters a function scope owned by a function symbol in the parent scope.
     * @param kind must be FUNCTION
     * @param ownerSymbolId function owner
     * @return newly active scope
     * @throws NullPointerException when an argument is null
     * @throws IllegalArgumentException when kind is not FUNCTION
     * @throws IllegalStateException when the owner is invalid
     */
    Scope enterScope(ScopeKind kind, SymbolId ownerSymbolId);
    /** Opens a structured BLOCK or LOOP scope. @param kind scope kind @return scope handle */
    default ScopeHandle openScope(final ScopeKind kind) {
        return new DefaultScopeHandle(this, enterScope(kind));
    }
    /** Exits the current non-global scope. @return parent scope @throws IllegalStateException on global */
    Scope exitScope();
    /** @return declaration result for a variable */
    DeclarationResult declareVariable(String name, Type type, Mutability mutability, Span declarationSpan);
    /** @return declaration result for a parameter */
    DeclarationResult declareParameter(
            String name, Type type, Mutability mutability, int ordinal, Span declarationSpan);
    /** @return declaration result for a function */
    DeclarationResult declareFunction(
            String name, List<ParameterDescriptor> parameters, Type returnType, Span declarationSpan);
    /** @return declaration result for the global main function */
    DeclarationResult declareMainFunction(Span declarationSpan);
    /** @return nearest lexical symbol, or empty */
    Optional<Symbol> lookup(String name);
    /** @return nearest lexical symbol from the supplied scope, or empty */
    Optional<Symbol> lookupFrom(ScopeId startScope, String name);
    /** @return local symbol in the current scope, or empty */
    Optional<Symbol> lookupLocal(String name);
    /** @return local symbol in the supplied scope, or empty */
    Optional<Symbol> lookupLocal(ScopeId scopeId, String name);
    /** @return symbol with the supplied identity, or empty */
    Optional<Symbol> find(SymbolId symbolId);
    /** @return scope with the supplied identity, or empty */
    Optional<Scope> findScope(ScopeId scopeId);
    /** @return immutable declaration-order snapshot of current symbols */
    List<Symbol> currentSymbols();
    /** @return immutable declaration-order snapshot of symbols directly in the scope */
    List<Symbol> symbolsInScope(ScopeId scopeId);

    /** Structured scope handle implementation. */
    final class DefaultScopeHandle implements ScopeHandle {
        private final SymbolTable table;
        private final Scope scope;
        private boolean closed;
        private DefaultScopeHandle(final SymbolTable table, final Scope scope) {
            this.table = table;
            this.scope = scope;
        }
        @Override public Scope scope() { return scope; }
        @Override public void close() {
            if (!closed) {
                if (!table.currentScope().id().equals(scope.id())) {
                    throw new IllegalStateException("scope handle must be closed while its scope is current");
                }
                table.exitScope();
                closed = true;
            }
        }
    }
}
