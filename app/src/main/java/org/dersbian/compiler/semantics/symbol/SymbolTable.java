package org.dersbian.compiler.semantics.symbol;

import java.util.List;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.Type;

/** Mutable semantic symbol table with lexical scope management. */
@SuppressWarnings({"PMD.ShortVariable", "PMD.TooManyMethods"})
public interface SymbolTable {
    /** Returns the immutable global scope snapshot. */
    Scope globalScope();

    /** Returns the immutable snapshot of the active scope. */
    Scope currentScope();

    /**
     * Enters a non-function child scope below the current scope.
     *
     * @param kind scope kind, either {@link ScopeKind#BLOCK} or {@link ScopeKind#LOOP}
     * @return immutable snapshot of the newly active scope
     */
    Scope enterScope(ScopeKind kind);

    /** Enters a function scope owned by the given function symbol. */
    Scope enterFunctionScope(SymbolId ownerSymbolId);

    /** Exits the active scope and returns its final immutable snapshot. */
    Scope exitScope();

    /**
     * Opens a block or loop scope whose close operation exits exactly that scope.
     *
     * @param kind scope kind, either {@link ScopeKind#BLOCK} or {@link ScopeKind#LOOP}
     * @return structured scope handle
     */
    ScopeHandle openScope(ScopeKind kind);

    /** Returns a historical immutable scope snapshot. */
    Optional<Scope> findScope(ScopeId id);

    /** Declares a variable in the current scope. */
    DeclarationResult declareVariable(
            String name, Type type, Mutability mutability, Span declarationSpan);

    /** Declares a function in the current scope. */
    DeclarationResult declareFunction(
            String name,
            Type returnType,
            List<ParameterDescriptor> parameters,
            Span declarationSpan);

    /** Declares the unique main function in the global scope. */
    DeclarationResult declareMainFunction(Span declarationSpan);

    /** Declares a parameter with the given ordinal in the active function scope. */
    DeclarationResult declareParameter(
            String name, Type type, Mutability mutability, int ordinal, Span declarationSpan);

    /** Resolves a name from the active scope toward the global scope. */
    Optional<Symbol> lookup(String name);

    /** Resolves a name from a specific scope toward the global scope. */
    Optional<Symbol> lookupFrom(ScopeId fromScope, String name);

    /** Resolves a name only in the active scope. */
    Optional<Symbol> lookupLocal(String name);

    /** Resolves a name only in the specified scope. */
    Optional<Symbol> lookupLocal(ScopeId scopeId, String name);

    /** Resolves a symbol by stable identity. */
    Optional<Symbol> find(SymbolId id);

    /** Returns symbols declared in the active scope, in declaration order. */
    List<Symbol> currentSymbols();

    /** Returns symbols declared in the specified scope, in declaration order. */
    List<Symbol> symbolsInScope(ScopeId scopeId);
}
