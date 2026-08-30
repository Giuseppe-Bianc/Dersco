package org.dersbian.compiler.semantics.symbol;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.Type;

/** Default single-threaded lexical symbol table with historical scope retention. */
@SuppressWarnings({
    "PMD.ShortVariable",
    "PMD.LongVariable",
    "PMD.OnlyOneReturn",
    "PMD.UseConcurrentHashMap",
    "PMD.LawOfDemeter",
    "PMD.GodClass",
    "PMD.TooManyMethods",
    "PMD.CouplingBetweenObjects",
    "PMD.AvoidFieldNameMatchingMethodName",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.CyclomaticComplexity",
    "PMD.CognitiveComplexity"
})
public final class DefaultSymbolTable implements SymbolTable {
    /** Canonical name of the main function entry point. */
    private static final String MAIN_NAME = "main";

    /** Stack depth that indicates only the global scope is present. */
    private static final int GLOBAL_SCOPE_ONLY = 1;

    /** Retained mutable state for every scope ever created, keyed by scope identifier. */
    private final Map<ScopeId, ScopeState> scopes = new LinkedHashMap<>();

    /** Global index of every symbol ever declared, keyed by symbol identifier. */
    private final Map<SymbolId, Symbol> symbols = new LinkedHashMap<>();

    /** LIFO stack of active scope identifiers; the top is the current scope. */
    private final Deque<ScopeId> scopeStack = new ArrayDeque<>();

    /** Monotonically increasing counter for scope identifier allocation. */
    private long nextScopeId = 1L;

    /** Monotonically increasing counter for symbol identifier allocation. */
    private long nextSymbolId = 1L;

    /** Identifier of the permanent root scope created at construction time. */
    private final ScopeId globalScope;

    /** Creates a table containing exactly one permanent global scope. */
    public DefaultSymbolTable() {
        globalScope = allocateScopeId();
        scopes.put(
                globalScope,
                new ScopeState(
                        globalScope, ScopeKind.GLOBAL, Optional.empty(), 0, Optional.empty()));
        scopeStack.push(globalScope);
        assertInvariants();
    }

    @Override
    public Scope globalScope() {
        return snapshot(globalScope);
    }

    @Override
    public Scope currentScope() {
        return snapshot(currentScopeId());
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
    public ScopeHandle openScope(final ScopeKind kind) {
        return new ScopeHandle(this, enterScope(kind));
    }

    /** Opens a function scope and returns a closeable handle for structured analysis. */
    public ScopeHandle openFunctionScope(final SymbolId ownerSymbolId) {
        return new ScopeHandle(this, enterFunctionScope(ownerSymbolId));
    }

    @Override
    public Scope enterFunctionScope(final SymbolId ownerSymbolId) {
        Objects.requireNonNull(ownerSymbolId, "ownerSymbolId must not be null");
        final Symbol owner = symbols.get(ownerSymbolId);
        if (!(owner instanceof Symbol.FunctionSymbol)
                && !(owner instanceof Symbol.MainFunctionSymbol)) {
            throw new IllegalArgumentException("function scope owner must be a function symbol");
        }
        if (!owner.scopeId().equals(currentScopeId())) {
            throw new IllegalArgumentException(
                    "function scope owner must be declared in the parent scope");
        }
        return createScope(ScopeKind.FUNCTION, Optional.of(ownerSymbolId));
    }

    @Override
    public Scope exitScope() {
        if (scopeStack.size() == GLOBAL_SCOPE_ONLY) {
            throw new IllegalStateException("global scope cannot be exited");
        }
        final ScopeId exiting = scopeStack.peek();
        final ScopeState state = scopes.get(exiting);
        if (state.kind == ScopeKind.FUNCTION) {
            validateFunctionScope(state);
        }
        scopeStack.pop();
        assertInvariants();
        return state.snapshot();
    }

    @Override
    public Optional<Scope> findScope(final ScopeId id) {
        Objects.requireNonNull(id, "id must not be null");
        return Optional.ofNullable(scopes.get(id)).map(ScopeState::snapshot);
    }

    @Override
    public DeclarationResult declareVariable(
            final String name,
            final Type type,
            final Mutability mutability,
            final Span declarationSpan) {
        validateDeclarationArguments(name, type, mutability, declarationSpan);
        return declare(
                name,
                () ->
                        new Symbol.VariableSymbol(
                                nextSymbol(),
                                name,
                                type,
                                mutability,
                                currentScopeId(),
                                declarationSpan));
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
        return declare(
                name,
                () ->
                        new Symbol.FunctionSymbol(
                                nextSymbol(),
                                name,
                                returnType,
                                copy,
                                currentScopeId(),
                                declarationSpan));
    }

    @Override
    public DeclarationResult declareMainFunction(final Span declarationSpan) {
        Objects.requireNonNull(declarationSpan, "declarationSpan must not be null");
        if (!currentScopeId().equals(globalScope)) {
            throw new IllegalStateException("main must be declared in the global scope");
        }
        return declare(
                MAIN_NAME,
                () ->
                        new Symbol.MainFunctionSymbol(
                                nextSymbol(),
                                MAIN_NAME,
                                new Type.VoidT(),
                                globalScope,
                                declarationSpan));
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
            return duplicate(name);
        }
        if (ordinal != state.nextParameterOrdinal) {
            throw new IllegalArgumentException(
                    "parameter ordinal must be " + state.nextParameterOrdinal);
        }
        final Symbol.ParameterSymbol symbol =
                new Symbol.ParameterSymbol(
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
        return lookupLocal(currentScopeId(), name);
    }

    @Override
    public Optional<Symbol> lookupLocal(final ScopeId scopeId, final String name) {
        Objects.requireNonNull(scopeId, "scopeId must not be null");
        validateName(name);
        final ScopeState state = scopes.get(scopeId);
        return state == null ? Optional.empty() : Optional.ofNullable(state.symbols.get(name));
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
        return state == null ? List.of() : List.copyOf(state.symbols.values());
    }

    /** Performs a full internal consistency check; package-private for invariant tests. */
    private void assertInvariants() {
        if (scopes.isEmpty() || !scopes.containsKey(globalScope)) {
            throw new IllegalStateException("global scope registry is invalid");
        }
        if (scopeStack.isEmpty() || !scopeStack.contains(globalScope)) {
            throw new IllegalStateException("scope stack does not contain global scope");
        }
        if (!scopeStack.peek().equals(currentScopeId())) {
            throw new IllegalStateException("scope stack top is not the current scope");
        }
        for (ScopeState state : scopes.values()) {
            validateScopeState(state);
            for (Symbol symbol : state.symbols.values()) {
                if (!symbol.scopeId().equals(state.id)) {
                    throw new IllegalStateException("symbol points to the wrong owning scope");
                }
                final Symbol indexed = symbols.get(symbol.id());
                if (!Objects.equals(indexed, symbol)) {
                    throw new IllegalStateException("scope and symbol indices disagree");
                }
            }
        }
        for (Map.Entry<SymbolId, Symbol> entry : symbols.entrySet()) {
            final Symbol symbol = entry.getValue();
            if (!entry.getKey().equals(symbol.id())) {
                throw new IllegalStateException("symbol index key does not match symbol id");
            }
            final ScopeState state = scopes.get(symbol.scopeId());
            if (state == null || !Objects.equals(state.symbols.get(symbol.name()), symbol)) {
                throw new IllegalStateException("symbol index contains an unreachable symbol");
            }
        }
        if (scopes.get(globalScope).kind != ScopeKind.GLOBAL) {
            throw new IllegalStateException("global scope has non-global kind");
        }
    }

    private Scope createScope(final ScopeKind kind, final Optional<SymbolId> owner) {
        final ScopeId parent = currentScopeId();
        final ScopeState parentState = currentState();
        final ScopeId id = allocateScopeId();
        final ScopeState state =
                new ScopeState(id, kind, Optional.of(parent), parentState.depth + 1, owner);
        scopes.put(id, state);
        scopeStack.push(id);
        assertInvariants();
        return state.snapshot();
    }

    private DeclarationResult declare(final String name, final SymbolFactory factory) {
        final Symbol existing = currentState().symbols.get(name);
        if (existing != null) {
            return new DeclarationResult.AlreadyDeclared(name, existing);
        }
        return insert(factory.create());
    }

    private DeclarationResult duplicate(final String name) {
        return new DeclarationResult.AlreadyDeclared(name, currentState().symbols.get(name));
    }

    private DeclarationResult insert(final Symbol symbol) {
        final ScopeState state = currentState();
        if (state.symbols.put(symbol.name(), symbol) != null) {
            throw new IllegalStateException("symbol index insertion collision");
        }
        if (symbols.put(symbol.id(), symbol) != null) {
            state.symbols.remove(symbol.name());
            throw new IllegalStateException("symbol identifier collision");
        }
        assertInvariants();
        return new DeclarationResult.Declared(symbol);
    }

    private void validateFunctionScope(final ScopeState state) {
        if (state.ownerSymbolId.isEmpty()) {
            throw new IllegalStateException("function scope has no owner");
        }
        final Symbol owner = symbols.get(state.ownerSymbolId.orElseThrow());
        if (owner == null) {
            throw new IllegalStateException("function scope owner is missing from symbol index");
        }
        if (!owner.scopeId().equals(state.parentId.orElseThrow())) {
            throw new IllegalStateException(
                    "function scope owner is not declared in its parent scope");
        }
        if (owner instanceof Symbol.FunctionSymbol function) {
            validateFunctionParameters(function.parameters(), state.symbols, state.id);
            return;
        }
        if (!(owner instanceof Symbol.MainFunctionSymbol)) {
            throw new IllegalStateException("unsupported function scope owner");
        }
        if (!state.symbols.isEmpty()) {
            throw new IllegalStateException("main function scope cannot contain parameters");
        }
    }

    private static void validateFunctionParameters(
            final List<ParameterDescriptor> descriptors,
            final Map<String, Symbol> parameterScope,
            final ScopeId functionScopeId) {
        if (parameterScope.size() != descriptors.size()) {
            throw new IllegalStateException(
                    "function parameter count does not match its signature");
        }
        for (int ordinal = 0; ordinal < descriptors.size(); ordinal++) {
            final ParameterDescriptor descriptor = descriptors.get(ordinal);
            final Symbol symbol = parameterScope.get(descriptor.name());
            if (!(symbol instanceof Symbol.ParameterSymbol parameter)) {
                throw new IllegalStateException("missing parameter symbol: " + descriptor.name());
            }
            if (!parameter.scopeId().equals(functionScopeId)) {
                throw new IllegalStateException("parameter symbol has an invalid scope");
            }
            if (parameter.ordinal() != ordinal
                    || !parameter.type().equals(descriptor.type())
                    || parameter.mutability() != descriptor.mutability()) {
                throw new IllegalStateException(
                        "parameter symbol does not match function signature: " + descriptor.name());
            }
        }
    }

    private static void validateScopeState(final ScopeState state) {
        if (state.kind == ScopeKind.GLOBAL) {
            if (state.parentId.isPresent() || state.ownerSymbolId.isPresent() || state.depth != 0) {
                throw new IllegalStateException("global scope invariants are broken");
            }
            return;
        }
        if (state.parentId.isEmpty()) {
            throw new IllegalStateException("non-global scope has no parent");
        }
        if (state.depth <= 0) {
            throw new IllegalStateException("non-global scope has invalid depth");
        }
        if (state.kind == ScopeKind.FUNCTION && state.ownerSymbolId.isEmpty()) {
            throw new IllegalStateException("function scope has no owner");
        }
        if (state.kind != ScopeKind.FUNCTION && state.ownerSymbolId.isPresent()) {
            throw new IllegalStateException("non-function scope has an owner");
        }
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
            final String name,
            final Type type,
            final Mutability mutability,
            final Span declarationSpan) {
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

    /** Factory callback that lazily constructs a {@link Symbol} during declaration. */
    @FunctionalInterface
    private interface SymbolFactory {
        /**
         * Creates and returns a new symbol instance.
         *
         * @return newly created symbol
         */
        Symbol create();
    }

    /** Mutable internal bookkeeping for one scope. */
    private static final class ScopeState {
        /** Unique identifier for this scope. */
        private final ScopeId id;

        /** Lexical kind of this scope (global, function, block, etc.). */
        private final ScopeKind kind;

        /** Identifier of the enclosing parent scope, empty for the global scope. */
        private final Optional<ScopeId> parentId;

        /** Zero-based nesting depth from the global scope. */
        private final int depth;

        /** Symbol that owns this scope, present only for function scopes. */
        private final Optional<SymbolId> ownerSymbolId;

        /** Symbols declared directly in this scope, keyed by name for fast local lookup. */
        private final Map<String, Symbol> symbols = new LinkedHashMap<>();

        /** Ordinal counter for the next parameter to be declared in this scope. */
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
