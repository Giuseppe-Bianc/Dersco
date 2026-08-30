package org.dersbian.compiler.semantics.symbol;

import java.util.List;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.Type;

/** Contract for lexical symbol binding and scope management. */
public interface SymbolTable {
    /** @return the unique permanent global scope */
    Scope globalScope();

    /** @return the currently active innermost scope */
    Scope currentScope();

    /**
     * Enters a new non-global scope owned by no symbol.
     * @param kind scope kind, other than GLOBAL and FUNCTION
     * @return the newly active scope
     * @throws NullPointerException if kind is null
     * @throws IllegalArgumentException if kind is GLOBAL or FUNCTION
     */
    Scope enterScope(ScopeKind kind);

    /**
     * Enters a function scope owned by an already declared function symbol.
     * @param kind scope kind, which must be FUNCTION
     * @param ownerSymbolId function or main-function symbol declared in the parent scope
     * @return the newly active scope
     * @throws NullPointerException if an argument is null
     * @throws IllegalArgumentException if the kind is not FUNCTION
     * @throws IllegalStateException if the owner is invalid
     */
    Scope enterScope(ScopeKind kind, SymbolId ownerSymbolId);

    /**
     * Opens a structured non-function scope.
     * @param kind BLOCK or LOOP scope kind
     * @return handle whose close operation exits the opened scope
     * @throws NullPointerException if kind is null
     * @throws IllegalArgumentException if kind is GLOBAL or FUNCTION
     */
    default ScopeHandle openScope(final ScopeKind kind) {
        return new DefaultScopeHandle(this, enterScope(kind));
    }

    /**
     * Exits the current non-global scope.
     * @return the parent scope, now active
     * @throws IllegalStateException if the global scope is current
     */
    Scope exitScope();

    /**
     * Declares a variable in the current scope.
     * @param name declared name
     * @param type declared type
     * @param mutability retained mutability
     * @param declarationSpan source declaration span
     * @return Declared or AlreadyDeclared
     */
    DeclarationResult declareVariable(String name, Type type, Mutability mutability, Span declarationSpan);

    /**
     * Declares a parameter in the current function scope.
     * @param name declared name
     * @param type declared type
     * @param mutability retained mutability
     * @param ordinal zero-based parameter position
     * @param declarationSpan source declaration span
     * @return Declared or AlreadyDeclared
     */
    DeclarationResult declareParameter(String name, Type type, Mutability mutability, int ordinal, Span declarationSpan);

    /**
     * Declares a function in the current scope.
     * @param name function name
     * @param parameters declared immutable signature descriptors
     * @param returnType return type
     * @param declarationSpan source declaration span
     * @return Declared or AlreadyDeclared
     */
    DeclarationResult declareFunction(String name, List<ParameterDescriptor> parameters, Type returnType, Span declarationSpan);

    /**
     * Declares the unique global main function.
     * @param declarationSpan source declaration span
     * @return Declared or AlreadyDeclared
     */
    DeclarationResult declareMainFunction(Span declarationSpan);

    /** @param name name to resolve @return nearest lexical symbol, or empty */
    Optional<Symbol> lookup(String name);

    /** @param startScope starting scope @param name name to resolve @return nearest symbol, or empty */
    Optional<Symbol> lookupFrom(ScopeId startScope, String name);

    /** @param name name to resolve @return local symbol, or empty */
    Optional<Symbol> lookupLocal(String name);

    /** @param scopeId scope to inspect @param name name to resolve @return local symbol, or empty */
    Optional<Symbol> lookupLocal(ScopeId scopeId, String name);

    /** @param symbolId symbol identifier @return symbol, or empty when unknown */
    Optional<Symbol> find(SymbolId symbolId);

    /** @param scopeId scope identifier @return scope, or empty when unknown */
    Optional<Scope> findScope(ScopeId scopeId);

    /** @return immutable declaration-order snapshot of current-scope symbols */
    List<Symbol> currentSymbols();

    /** @param scopeId scope to inspect @return immutable declaration-order snapshot */
    List<Symbol> symbolsInScope(ScopeId scopeId);

    /** Structured scope-handle implementation. */
    final class DefaultScopeHandle implements ScopeHandle {
        private final SymbolTable table;
        private final Scope scope;
        private boolean closed;

        private DefaultScopeHandle(final SymbolTable table, final Scope scope) {
            this.table = table;
            this.scope = scope;
        }

        @Override
        public Scope scope() {
            return scope;
        }

        @Override
        public void close() {
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
