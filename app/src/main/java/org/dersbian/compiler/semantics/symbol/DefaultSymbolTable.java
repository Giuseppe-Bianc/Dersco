package org.dersbian.compiler.semantics.symbol;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.Type;

/** Default single-threaded lexical symbol table with historical scope retention. */
public final class DefaultSymbolTable implements SymbolTable {
    private static final String MAIN_NAME = "main";

    private final Map<ScopeId, ScopeState> scopes = new LinkedHashMap<>();
    private final Map<SymbolId, Symbol> symbols = new LinkedHashMap<>();
    private final Deque<ScopeId> scopeStack = new ArrayDeque<>();
    private long nextScopeId = 1L;
    private long nextSymbolId = 1L;
    private final ScopeId globalScope;

    /** Creates a table containing exactly one permanent global scope. */
    public DefaultSymbolTable() {
        globalScope = allocateScopeId();
        scopes.put(globalScope, new ScopeState(
                globalScope, ScopeKind.GLOBAL, Optional.empty(), 0, Optional.empty()));
        scopeStack.push(globalScope);
    }

    @Override
    public Scope globalScope() {
        return snapshot(globalScope);
    }

    @Override
    public Scope currentScope() {
        return snapshot(scopeStack.peek());
    }

    @Override
    public Scope enterScope(final ScopeKind kind) {
        Objects.requireNonNull(kind, "kind must not be null");
        if (kind == ScopeKind.GLOBAL || kind == ScopeKind.FUNCTION) {
            throw new IllegalArgumentException("use enterFunctionScope for function scopes");
        }
        return createScope(kind, Optional.empty());
    }

    @Override
    public Scope enterFunctionScope(final SymbolId ownerSymbolId) {
        Objects.requireNonNull(ownerSymbolId, "ownerSymbolId must not be null");
        final Symbol owner = symbols.get(ownerSymbolId);
        if (!(owner instanceof Symbol.FunctionSymbol) && !(owner instanceof Symbol.MainFunctionSymbol)) {
            throw new IllegalArgumentException("function scope owner must be a function symbol");
        }
        if (!owner.scopeId().equals(scopeStack.peek())) {
            throw new IllegalArgumentException("function scope owner must be declared in the parent scope");
        }
        return createScope(ScopeKind.FUNCTION, Optional.of(ownerSymbolId));
    }

    @Override
    public Scope exitScope() {
        if (scopeStack.size() == 1) {
            throw new IllegalStateException("global scope cannot be exited");
        }
        final ScopeId exiting = scopeStack.pop();
        return snapshot(exiting);
    }

    @Override
    public Optional<Scope> findScope(final ScopeId id) {
        Objects.requireNonNull(id, "id must not be null");
        return scopes.containsKey(id) ? Optional.of(snapshot(id)) : Optional.empty();
    }

    @Override
    public DeclarationResult declareVariable(
            final String name, final Type type, final Mutability mutability, final Span declarationSpan) {
        validateDeclarationArguments(name, type, mutability, declarationSpan);
        return declare(new Symbol.VariableSymbol(
                nextSymbol(), name, type, mutability, currentScopeId(), declarationSpan));
    }

    @Override
    public DeclarationResult declareFunction(
            final String name,
            final Type returnType,
            final List<ParameterDescriptor> parameters,
            final Span declarationSpan) {
        Objects.requireNonNull(returnType, "returnType must not be null");
        Objects.requireNonNull(parameters, "parameters must not be null");
        Objects.requireNonNull(declarationSpan, "declarationSpan must not be null");
        validateName(name);
        if (MAIN_NAME.equals(name)) {
            throw new IllegalArgumentException("use declareMainFunction for main");
        }
        final List<ParameterDescriptor> copy = List.copyOf(parameters);
        validateParameterNames(copy);
        if (lookupLocal(name).isPresent()) {
            return new DeclarationResult.AlreadyDeclared(name, currentScopeId());
        }
        return insert(new Symbol.FunctionSymbol(
                nextSymbol(), name, returnType, copy, currentScopeId(), declarationSpan));
    }

    @Override
    public DeclarationResult declareMainFunction(final Span declarationSpan) {
        Objects.requireNonNull(declarationSpan, "declarationSpan must not be null");
        if (!currentScopeId().equals(globalScope)) {
            throw new IllegalStateException("main must be declared in the global scope");
        }
        if (lookupLocal(MAIN_NAME).isPresent()) {
            return new DeclarationResult.AlreadyDeclared(MAIN_NAME, globalScope);
        }
        return insert(new Symbol.MainFunctionSymbol(
                nextSymbol(), MAIN_NAME, new Type.VoidT(), globalScope, declarationSpan));
    }

    @Override
    public DeclarationResult declareParameter(
            final String name,
            final Type type,
            final Mutability mutability,
            final int ordinal,
            final Span declarationSpan) {
        validateDeclarationArguments(name, type, mutability, declarationSpan);
        if (ordinal < 0) {
            throw new IllegalArgumentException("ordinal must be non-negative");
        }
        final ScopeState state = currentState();
        if (state.kind != ScopeKind.FUNCTION) {
            throw new IllegalStateException("parameters may only be declared in function scopes");
        }
        if (lookupLocal(name).isPresent()) {
            return new DeclarationResult.AlreadyDeclared(name, state.id);
        }
        final int expected = state.nextParameterOrdinal;
        if (ordinal != expected) {
            throw new IllegalArgumentException("parameter ordinal must be " + expected);
        }
        final Symbol.ParameterSymbol symbol = new Symbol.ParameterSymbol(
                nextSymbol(), name, type, mutability, ordinal, state.id, declarationSpan);
        state.nextParameterOrdinal++;
        return insert(symbol);
    }

    @Override
    public Optional<Symbol> lookup(final String name) {
        return lookupFrom(currentScopeId(), name);
    }

    @Override
    public Optional<Symbol> lookupFrom(final ScopeId fromScope, final String name) {
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
            cursor = state.parentId.orElse(null);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Symbol> lookupLocal(final String name) {
        validateName(name);
        return Optional.ofNullable(currentState().symbols.get(name));
    }

    @Override
    public Optional<Symbol> find(final SymbolId id) {
        Objects.requireNonNull(id, "id must not be null");
        return Optional.ofNullable(symbols.get(id));
    }

    @Override
    public List<Symbol> currentSymbols() {
        return symbolsInScope(currentScopeId());
    }

    @Override
    public List<Symbol> symbolsInScope(final ScopeId scopeId) {
        Objects.requireNonNull(scopeId, "scopeId must not be null");
        final ScopeState state = scopes.get(scopeId);
        if (state == null) {
            return List.of();
        }
        return List.copyOf(state.symbols.values());
    }

    private Scope createScope(final ScopeKind kind, final Optional<SymbolId> owner) {
        final ScopeId parent = currentScopeId();
        final ScopeState parentState = currentState();
        final ScopeId id = allocateScopeId();
        final ScopeState state = new ScopeState(
                id, kind, Optional.of(parent), parentState.depth + 1, owner);
        scopes.put(id, state);
        scopeStack.push(id);
        return state.snapshot();
    }

    private DeclarationResult declare(final Symbol symbol) {
        if (lookupLocal(symbol.name()).isPresent()) {
            return new DeclarationResult.AlreadyDeclared(symbol.name(), currentScopeId());
        }
        return insert(symbol);
    }

    private DeclarationResult insert(final Symbol symbol) {
        final ScopeState state = currentState();
        state.symbols.put(symbol.name(), symbol);
        symbols.put(symbol.id(), symbol);
        return new DeclarationResult.Declared(symbol);
    }

    private SymbolId nextSymbol() {
        final long value = nextSymbolId;
        nextSymbolId = nextPositiveId(nextSymbolId, "symbol");
        return new SymbolId(value);
    }

    private ScopeId allocateScopeId() {
        final long value = nextScopeId;
        nextScopeId = nextPositiveId(nextScopeId, "scope");
        return new ScopeId(value);
    }

    private static long nextPositiveId(final long current, final String kind) {
        if (current == Long.MAX_VALUE) {
            throw new IllegalStateException(kind + " identifier space exhausted");
        }
        return current + 1L;
    }

    private Scope snapshot(final ScopeId id) {
        final ScopeState state = scopes.get(id);
        if (state == null) {
            throw new IllegalArgumentException("unknown scope: " + id);
        }
        return state.snapshot();
    }

    private ScopeId currentScopeId() {
        return scopeStack.peek();
    }

    private ScopeState currentState() {
        return scopes.get(currentScopeId());
    }

    private static void validateDeclarationArguments(
            final String name, final Type type, final Mutability mutability, final Span declarationSpan) {
        validateName(name);
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(mutability, "mutability must not be null");
        Objects.requireNonNull(declarationSpan, "declarationSpan must not be null");
    }

    private static void validateName(final String name) {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    private static void validateParameterNames(final List<ParameterDescriptor> parameters) {
        final Map<String, Boolean> names = new LinkedHashMap<>();
        for (ParameterDescriptor parameter : parameters) {
            if (names.put(parameter.name(), Boolean.TRUE) != null) {
                throw new IllegalArgumentException("duplicate parameter name: " + parameter.name());
            }
        }
    }

    private static final class ScopeState {
        private final ScopeId id;
        private final ScopeKind kind;
        private final Optional<ScopeId> parentId;
        private final int depth;
        private final Optional<SymbolId> ownerSymbolId;
        private final Map<String, Symbol> symbols = new LinkedHashMap<>();
        private int nextParameterOrdinal;

        private ScopeState(
                final ScopeId id,
                final ScopeKind kind,
                final Optional<ScopeId> parentId,
                final int depth,
                final Optional<SymbolId> ownerSymbolId) {
            this.id = id;
            this.kind = kind;
            this.parentId = parentId;
            this.depth = depth;
            this.ownerSymbolId = ownerSymbolId;
        }

        private Scope snapshot() {
            return new Scope(id, kind, parentId, depth, ownerSymbolId);
        }
    }
}
