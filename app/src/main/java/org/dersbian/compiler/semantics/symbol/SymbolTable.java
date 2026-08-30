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
     * @param kind scope kind, other than GLOBAL
     * @return the newly active scope
     * @throws NullPointerException if kind is null
     * @throws IllegalArgumentException if kind is GLOBAL
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

    /**
     * Performs innermost-first lexical lookup from the current scope.
     * @param name name to resolve
     * @return the nearest symbol, or empty when absent
     */
    Optional<Symbol> lookup(String name);

    /**
     * Performs innermost-first lexical lookup from a historical scope.
     * @param startScope starting scope identifier
     * @param name name to resolve
     * @return the nearest symbol, or empty when absent
     */
    Optional<Symbol> lookupFrom(ScopeId startScope, String name);

    /**
     * Looks only in the current scope.
     * @param name name to resolve
     * @return local symbol, or empty when absent
     */
    Optional<Symbol> lookupLocal(String name);

    /**
     * Looks only in the specified scope.
     * @param scopeId scope to inspect
     * @param name name to resolve
     * @return local symbol, or empty when absent
     */
    Optional<Symbol> lookupLocal(ScopeId scopeId, String name);

    /**
     * Finds a symbol by stable identity.
     * @param symbolId symbol identifier
     * @return symbol, or empty when unknown
     */
    Optional<Symbol> find(SymbolId symbolId);

    /**
     * Finds a scope by stable identity.
     * @param scopeId scope identifier
     * @return scope, or empty when unknown
     */
    Optional<Scope> findScope(ScopeId scopeId);

    /** @return immutable declaration-order snapshot of current-scope symbols */
    List<Symbol> currentSymbols();

    /**
     * Returns symbols declared directly in a scope, in declaration order.
     * @param scopeId scope to inspect
     * @return immutable symbol snapshot
     */
    List<Symbol> symbolsInScope(ScopeId scopeId);
}
