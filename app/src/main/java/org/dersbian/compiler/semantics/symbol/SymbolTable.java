package org.dersbian.compiler.semantics.symbol;

import java.util.List;
import java.util.Optional;
import org.dersbian.compiler.syntax.ast.Type;

/** Lexical binding table used by semantic analysis. */
public interface SymbolTable {
    /** Returns the permanent global scope. */
    ScopeId globalScope();

    /** Returns the currently active scope. */
    ScopeId currentScope();

    /** Enters a child scope of the current scope. */
    ScopeId enterScope(ScopeKind kind);

    /** Exits the current scope, except for the global scope. */
    void exitScope();

    /** Returns a historical scope by id. */
    Optional<Scope> scope(ScopeId id);

    /** Declares a variable in the current scope. */
    DeclarationResult declareVariable(String name, Type type, Mutability mutability);

    /** Declares a function in the current scope. */
    DeclarationResult declareFunction(
            String name, Type returnType, List<ParameterDescriptor> parameters);

    /** Declares a parameter in the current function scope. */
    DeclarationResult declareParameter(ParameterDescriptor parameter);

    /** Resolves a name from the current scope toward the global scope. */
    Optional<Symbol> lookup(String name);

    /** Resolves a name from a specific scope toward the global scope. */
    Optional<Symbol> lookup(ScopeId fromScope, String name);
}
