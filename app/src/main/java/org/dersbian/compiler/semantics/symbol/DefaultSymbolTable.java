package org.dersbian.compiler.semantics.symbol;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.dersbian.compiler.syntax.ast.Type;

/** Default single-threaded lexical symbol table with historical scope retention. */
public final class DefaultSymbolTable implements SymbolTable {
    private static final String MAIN_NAME = "main";

    private final Map<ScopeId, ScopeState> scopes = new HashMap<>();
    private final Deque<ScopeId> scopeStack = new ArrayDeque<>();
    private long nextScopeId;
    private long nextSymbolId;
    private final ScopeId globalScope;

    /** Creates the table with exactly one global scope. */
    public DefaultSymbolTable() {
        globalScope = new ScopeId(nextScopeId++);
        scopes.put(globalScope, new ScopeState(globalScope, null, ScopeKind.GLOBAL));
        scopeStack.push(globalScope);
    }

    @Override
    public ScopeId globalScope() {
        return globalScope;
    }

    @Override
    public ScopeId currentScope() {
        return scopeStack.peek();
    }

    @Override
    public ScopeId enterScope(final ScopeKind kind) {
        Objects.requireNonNull(kind, "kind must not be null");
        if (kind == ScopeKind.GLOBAL) {
            throw new IllegalArgumentException("cannot enter another global scope");
        }
        final ScopeId parent = currentScope();
        final ScopeId id = new ScopeId(nextScopeId++);
        scopes.put(id, new ScopeState(id, parent, kind));
        scopeStack.push(id);
        return id;
    }

    @Override
    public void exitScope() {
        if (scopeStack.size() == 1) {
            throw new IllegalStateException("global scope cannot be exited");
        }
        scopeStack.pop();
    }

    @Override
    public Optional<Scope> scope(final ScopeId id) {
        Objects.requireNonNull(id, "id must not be null");
        return Optional.ofNullable(scopes.get(id)).map(ScopeState::snapshot);
    }

    @Override
    public DeclarationResult declareVariable(
            final String name, final Type type, final Mutability mutability) {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(mutability, "mutability must not be null");
        validateName(name);
        return declare(new Symbol(
                new SymbolId(nextSymbolId++), name, SymbolKind.VARIABLE, type,
                mutability, currentScope(), List.of()));
    }

    @Override
    public DeclarationResult declareFunction(
            final String name, final Type returnType, final List<ParameterDescriptor> parameters) {
        Objects.requireNonNull(returnType, "returnType must not be null");
        Objects.requireNonNull(parameters, "parameters must not be null");
        validateName(name);
        if (MAIN_NAME.equals(name) && !currentScope().equals(globalScope)) {
            throw new IllegalArgumentException("main must be declared in the global scope");
        }
        final List<ParameterDescriptor> copy = List.copyOf(parameters);
        validateParameters(copy);
        return declare(new Symbol(
                new SymbolId(nextSymbolId++), name, SymbolKind.FUNCTION, returnType,
                Mutability.IMMUTABLE, currentScope(), copy));
    }

    @Override
    public DeclarationResult declareParameter(final ParameterDescriptor parameter) {
        Objects.requireNonNull(parameter, "parameter must not be null");
        if (scopes.get(currentScope()).kind != ScopeKind.FUNCTION) {
            throw new IllegalStateException("parameters may only be declared in function scopes");
        }
        return declare(new Symbol(
                new SymbolId(nextSymbolId++), parameter.name(), SymbolKind.PARAMETER,
                parameter.type(), parameter.mutability(), currentScope(), List.of()));
    }

    @Override
    public Optional<Symbol> lookup(final String name) {
        return lookup(currentScope(), name);
    }

    @Override
    public Optional<Symbol> lookup(final ScopeId fromScope, final String name) {
        Objects.requireNonNull(fromScope, "fromScope must not be null");
        validateName(name);
        ScopeId cursor = fromScope;
        while (cursor != null) {
            final ScopeState state = scopes.get(cursor);
            if (state == null) {
                return Optional.empty();
            }
            final Symbol symbol = state.symbols.get(name);
            if (symbol != null) {
                return Optional.of(symbol);
            }
            cursor = state.parentId;
        }
        return Optional.empty();
    }

    private DeclarationResult declare(final Symbol symbol) {
        final ScopeState state = scopes.get(symbol.scopeId());
        if (state.symbols.containsKey(symbol.name())) {
            return new DeclarationResult.AlreadyDeclared(symbol.name(), state.id);
        }
        state.symbols.put(symbol.name(), symbol);
        return new DeclarationResult.Declared(symbol);
    }

    private static void validateName(final String name) {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    private static void validateParameters(final List<ParameterDescriptor> parameters) {
        final Map<String, Boolean> names = new HashMap<>();
        for (int index = 0; index < parameters.size(); index++) {
            final ParameterDescriptor parameter = parameters.get(index);
            if (parameter.ordinal() != index) {
                throw new IllegalArgumentException("parameter ordinals must be contiguous from zero");
            }
            if (names.put(parameter.name(), Boolean.TRUE) != null) {
                throw new IllegalArgumentException("duplicate parameter name: " + parameter.name());
            }
        }
    }

    private static final class ScopeState {
        private final ScopeId id;
        private final ScopeId parentId;
        private final ScopeKind kind;
        private final Map<String, Symbol> symbols = new HashMap<>();

        private ScopeState(final ScopeId id, final ScopeId parentId, final ScopeKind kind) {
            this.id = id;
            this.parentId = parentId;
            this.kind = kind;
        }

        private Scope snapshot() {
            return new Scope(id, parentId, kind, Map.copyOf(symbols));
        }
    }
}
