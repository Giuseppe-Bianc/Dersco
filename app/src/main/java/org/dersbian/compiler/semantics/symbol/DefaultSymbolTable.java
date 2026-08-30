package org.dersbian.compiler.semantics.symbol;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.Type;

/** Default in-memory implementation of the version-one lexical symbol table. */
@SuppressWarnings({
    "PMD.TooManyMethods",
    "PMD.OnlyOneReturn",
    "PMD.LongVariable",
    "PMD.ShortVariable",
    "PMD.LawOfDemeter",
    "PMD.AssignmentInOperand",
    "PMD.CyclomaticComplexity",
    "PMD.UseConcurrentHashMap",
    "PMD.GodClass",
    "PMD.CouplingBetweenObjects",
})
public final class DefaultSymbolTable implements SymbolTable {
    /** Fixed name of the global entry point function. */
    private static final String MAIN_NAME = "main";

    /** Null validation message for declaration span arguments. */
    private static final String DECLARATION_SPAN_NOT_NULL = "declarationSpan must not be null";

    /** Expected count of global scopes in a valid symbol table. */
    private static final long REQUIRED_GLOBAL_SCOPE_COUNT = 1L;

    /** Active and historical scopes by scope identifier. */
    private final Map<ScopeId, ScopeState> scopes = new LinkedHashMap<>();

    /** Declared symbols indexed by symbol identifier. */
    private final Map<SymbolId, Symbol> symbols = new LinkedHashMap<>();

    /** Stack of currently entered scopes, with global scope at the bottom. */
    private final Deque<ScopeId> scopeStack = new ArrayDeque<>();

    /** Monotonic sequence counter for scope identifiers. */
    private final MonotonicIdSequence scopeIdSequence = new MonotonicIdSequence();

    /** Monotonic sequence counter for symbol identifiers. */
    private final MonotonicIdSequence symbolIdSequence = new MonotonicIdSequence();

    /** Creates a symbol table containing one empty global scope. */
    public DefaultSymbolTable() {
        final ScopeId id = nextScopeId();
        final Scope global = new Scope(id, ScopeKind.GLOBAL, Optional.empty(), 0, Optional.empty());
        scopes.put(id, new ScopeState(global));
        scopeStack.push(id);
    }

    @Override
    public Scope globalScope() {
        return scopes.get(scopeStack.getLast()).scope;
    }

    @Override
    public Scope currentScope() {
        return scopes.get(scopeStack.peek()).scope;
    }

    @Override
    public Scope enterScope(final ScopeKind kind) {
        Objects.requireNonNull(kind, "kind must not be null");
        if (kind == ScopeKind.GLOBAL) {
            throw new IllegalArgumentException("GLOBAL cannot be entered explicitly");
        }
        if (kind == ScopeKind.FUNCTION) {
            throw new IllegalArgumentException("FUNCTION scope requires an owner symbol");
        }
        return createScope(kind, Optional.empty());
    }

    @Override
    public Scope enterScope(final ScopeKind kind, final SymbolId ownerSymbolId) {
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(ownerSymbolId, "ownerSymbolId must not be null");
        if (kind != ScopeKind.FUNCTION) {
            throw new IllegalArgumentException("an owner is valid only for FUNCTION scopes");
        }
        final Symbol owner = symbols.get(ownerSymbolId);
        if (owner == null) {
            throw new IllegalStateException("function scope owner is unknown");
        }
        if (owner.kind() != SymbolKind.FUNCTION && owner.kind() != SymbolKind.MAIN_FUNCTION) {
            throw new IllegalStateException("function scope owner must be a function symbol");
        }
        if (!owner.scopeId().equals(currentScope().id())) {
            throw new IllegalStateException(
                    "function scope owner must belong to the current parent scope");
        }
        return createScope(kind, Optional.of(ownerSymbolId));
    }

    @Override
    public Scope exitScope() {
        if (currentScope().kind() == ScopeKind.GLOBAL) {
            throw new IllegalStateException("global scope cannot be exited");
        }
        scopeStack.pop();
        return currentScope();
    }

    @Override
    public DeclarationResult declareVariable(
            final String name,
            final Type type,
            final Mutability mutability,
            final Span declarationSpan) {
        validateName(name);
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(mutability, "mutability must not be null");
        Objects.requireNonNull(declarationSpan, DECLARATION_SPAN_NOT_NULL);
        final DeclarationResult duplicate = duplicate(name);
        if (duplicate != null) {
            return duplicate;
        }
        final VariableSymbol symbol =
                new VariableSymbolImpl(
                        nextSymbolId(),
                        name,
                        currentScope().id(),
                        declarationSpan,
                        type,
                        mutability);
        register(symbol);
        return new DeclarationResult.Declared(symbol);
    }

    @Override
    public DeclarationResult declareParameter(
            final String name,
            final Type type,
            final Mutability mutability,
            final int ordinal,
            final Span declarationSpan) {
        validateName(name);
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(mutability, "mutability must not be null");
        Objects.requireNonNull(declarationSpan, DECLARATION_SPAN_NOT_NULL);
        if (ordinal < 0) {
            throw new IllegalArgumentException("ordinal must not be negative");
        }
        if (currentScope().kind() != ScopeKind.FUNCTION) {
            throw new IllegalStateException("parameters can only be declared in a function scope");
        }
        final ScopeState state = currentState();
        if (ordinal != state.parameterCount) {
            throw new IllegalArgumentException(
                    "parameter ordinal must be consecutive starting at zero");
        }
        final DeclarationResult duplicate = duplicate(name);
        if (duplicate != null) {
            return duplicate;
        }
        final ParameterSymbol symbol =
                new ParameterSymbolImpl(
                        nextSymbolId(),
                        name,
                        currentScope().id(),
                        declarationSpan,
                        type,
                        mutability,
                        ordinal);
        register(symbol);
        state.parameterCount++;
        return new DeclarationResult.Declared(symbol);
    }

    @Override
    public DeclarationResult declareFunction(
            final String name,
            final List<ParameterDescriptor> parameters,
            final Type returnType,
            final Span declarationSpan) {
        validateName(name);
        Objects.requireNonNull(parameters, "parameters must not be null");
        Objects.requireNonNull(returnType, "returnType must not be null");
        Objects.requireNonNull(declarationSpan, DECLARATION_SPAN_NOT_NULL);
        final List<ParameterDescriptor> signature = validateParameters(parameters);
        final DeclarationResult duplicate = duplicate(name);
        if (duplicate != null) {
            return duplicate;
        }
        final FunctionSymbol symbol =
                new FunctionSymbolImpl(
                        nextSymbolId(),
                        name,
                        currentScope().id(),
                        declarationSpan,
                        signature,
                        returnType);
        register(symbol);
        return new DeclarationResult.Declared(symbol);
    }

    @Override
    public DeclarationResult declareMainFunction(final Span declarationSpan) {
        Objects.requireNonNull(declarationSpan, DECLARATION_SPAN_NOT_NULL);
        if (currentScope().kind() != ScopeKind.GLOBAL) {
            throw new IllegalStateException("main can only be declared in the global scope");
        }
        final DeclarationResult duplicate = duplicate(MAIN_NAME);
        if (duplicate != null) {
            return duplicate;
        }
        final MainFunctionSymbol symbol =
                new MainFunctionSymbolImpl(
                        nextSymbolId(),
                        MAIN_NAME,
                        currentScope().id(),
                        declarationSpan,
                        new Type.VoidT());
        register(symbol);
        return new DeclarationResult.Declared(symbol);
    }

    @Override
    public Optional<Symbol> lookup(final String name) {
        validateLookupName(name);
        return lookupFrom(currentScope().id(), name);
    }

    @Override
    public Optional<Symbol> lookupFrom(final ScopeId startScope, final String name) {
        Objects.requireNonNull(startScope, "startScope must not be null");
        validateLookupName(name);
        ScopeState state = scopes.get(startScope);
        if (state == null) {
            return Optional.empty();
        }
        while (true) {
            final Symbol symbol = state.symbolsByName.get(name);
            if (symbol != null) {
                return Optional.of(symbol);
            }
            final Optional<ScopeId> parentId = state.scope.parentId();
            if (parentId.isEmpty()) {
                return Optional.empty();
            }
            state = scopes.get(parentId.orElseThrow());
            if (state == null) {
                throw new IllegalStateException("scope tree contains an orphaned parent");
            }
        }
    }

    @Override
    public Optional<Symbol> lookupLocal(final String name) {
        validateLookupName(name);
        return Optional.ofNullable(currentState().symbolsByName.get(name));
    }

    @Override
    public Optional<Symbol> lookupLocal(final ScopeId scopeId, final String name) {
        Objects.requireNonNull(scopeId, "scopeId must not be null");
        validateLookupName(name);
        final ScopeState state = scopes.get(scopeId);
        return state == null
                ? Optional.empty()
                : Optional.ofNullable(state.symbolsByName.get(name));
    }

    @Override
    public Optional<Symbol> find(final SymbolId symbolId) {
        Objects.requireNonNull(symbolId, "symbolId must not be null");
        return Optional.ofNullable(symbols.get(symbolId));
    }

    @Override
    public Optional<Scope> findScope(final ScopeId scopeId) {
        Objects.requireNonNull(scopeId, "scopeId must not be null");
        final ScopeState state = scopes.get(scopeId);
        return state == null ? Optional.empty() : Optional.of(state.scope);
    }

    @Override
    public List<Symbol> currentSymbols() {
        return symbolsInScope(currentScope().id());
    }

    @Override
    public List<Symbol> symbolsInScope(final ScopeId scopeId) {
        Objects.requireNonNull(scopeId, "scopeId must not be null");
        final ScopeState state = scopes.get(scopeId);
        return state == null ? List.of() : List.copyOf(state.symbolsByName.values());
    }

    /** Verifies all structural and cross-reference invariants for tests in this package. */
    public void assertConsistent() {
        if (scopeStack.isEmpty()) {
            throw new AssertionError("scope stack must not be empty");
        }
        for (final ScopeId id : scopeStack) {
            if (!scopes.containsKey(id)) {
                throw new AssertionError("scope stack contains an unknown scope");
            }
        }
        if (scopes.values().stream().filter(state -> state.scope.kind() == ScopeKind.GLOBAL).count()
                != REQUIRED_GLOBAL_SCOPE_COUNT) {
            throw new AssertionError("exactly one global scope is required");
        }
        for (final ScopeState state : scopes.values()) {
            assertValidScope(state);
            assertAcyclic(state.scope);
            assertSymbolCrossReferences(state);
        }
        for (final Symbol symbol : symbols.values()) {
            final ScopeState owner = scopes.get(symbol.scopeId());
            if (owner == null || !Objects.equals(owner.symbolsByName.get(symbol.name()), symbol)) {
                throw new AssertionError("symbol is not registered in its declaring scope");
            }
        }
        final ScopeId currentId = scopeStack.peek();
        if (!currentId.equals(currentScope().id())) {
            throw new AssertionError("scope stack/current scope mismatch");
        }
    }

    private void assertValidScope(final ScopeState state) {
        final Scope scope = state.scope;
        if (scope.kind() == ScopeKind.GLOBAL) {
            assertValidGlobalScope(scope);
            return;
        }
        assertValidNonGlobalScope(scope);
    }

    private void assertValidGlobalScope(final Scope scope) {
        if (scope.parentId().isPresent()
                || scope.depth() != 0
                || scope.ownerSymbolId().isPresent()) {
            throw new AssertionError("invalid global scope");
        }
    }

    private void assertValidNonGlobalScope(final Scope scope) {
        final ScopeId parentId = scope.parentId().orElseThrow();
        final ScopeState parent = scopes.get(parentId);
        if (parent == null || scope.depth() != parent.scope.depth() + 1) {
            throw new AssertionError("invalid scope parent/depth");
        }
        if (scope.kind() == ScopeKind.FUNCTION) {
            assertValidFunctionScopeOwner(scope, parentId);
            return;
        }
        if (scope.ownerSymbolId().isPresent()) {
            throw new AssertionError("non-function scope cannot have an owner");
        }
    }

    private void assertValidFunctionScopeOwner(final Scope scope, final ScopeId parentId) {
        final SymbolId ownerId = scope.ownerSymbolId().orElseThrow();
        final Symbol owner = symbols.get(ownerId);
        if (owner == null
                || (owner.kind() != SymbolKind.FUNCTION && owner.kind() != SymbolKind.MAIN_FUNCTION)
                || !owner.scopeId().equals(parentId)) {
            throw new AssertionError("invalid function scope owner");
        }
    }

    private void assertSymbolCrossReferences(final ScopeState state) {
        for (final Symbol symbol : state.symbolsByName.values()) {
            if (!symbol.scopeId().equals(state.scope.id())
                    || !Objects.equals(symbols.get(symbol.id()), symbol)) {
                throw new AssertionError("symbol cross-reference invariant violated");
            }
        }
    }

    private void assertAcyclic(final Scope start) {
        final Set<ScopeId> visited = new HashSet<>();
        ScopeState state = scopes.get(start.id());
        while (state != null) {
            if (!visited.add(state.scope.id())) {
                throw new AssertionError("scope tree contains a cycle");
            }
            final Optional<ScopeId> parentId = state.scope.parentId();
            if (parentId.isEmpty()) {
                return;
            }
            state = scopes.get(parentId.orElseThrow());
        }
        throw new AssertionError("scope tree contains an orphaned parent");
    }

    private Scope createScope(final ScopeKind kind, final Optional<SymbolId> ownerSymbolId) {
        final Scope parent = currentScope();
        final ScopeId id = nextScopeId();
        final Scope scope =
                new Scope(id, kind, Optional.of(parent.id()), parent.depth() + 1, ownerSymbolId);
        scopes.put(id, new ScopeState(scope));
        scopeStack.push(id);
        return scope;
    }

    private ScopeState currentState() {
        return scopes.get(currentScope().id());
    }

    private DeclarationResult duplicate(final String name) {
        final Symbol existing = currentState().symbolsByName.get(name);
        return existing == null ? null : new DeclarationResult.AlreadyDeclared(name, existing);
    }

    private void register(final Symbol symbol) {
        final ScopeState state = currentState();
        if (state.symbolsByName.containsKey(symbol.name())) {
            throw new IllegalStateException("attempted to register a duplicate symbol");
        }
        state.symbolsByName.put(symbol.name(), symbol);
        symbols.put(symbol.id(), symbol);
    }

    private List<ParameterDescriptor> validateParameters(
            final List<ParameterDescriptor> parameters) {
        final List<ParameterDescriptor> copy = List.copyOf(parameters);
        final Map<String, Boolean> names = new LinkedHashMap<>();
        for (final ParameterDescriptor parameter : copy) {
            if (names.put(parameter.name(), Boolean.TRUE) != null) {
                throw new IllegalArgumentException("function parameters must have unique names");
            }
        }
        return copy;
    }

    private SymbolId nextSymbolId() {
        return new SymbolId(symbolIdSequence.next());
    }

    private ScopeId nextScopeId() {
        return new ScopeId(scopeIdSequence.next());
    }

    private static void validateName(final String name) {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name must not be empty");
        }
    }

    private static void validateLookupName(final String name) {
        validateName(name);
    }

    /** Internal state tracked for an individual scope. */
    private static final class ScopeState {
        /** Immutable scope metadata. */
        private final Scope scope;

        /** Symbols declared in this scope, preserved in declaration order. */
        private final Map<String, Symbol> symbolsByName = new LinkedHashMap<>();

        /** Number of successfully declared parameters in this function scope. */
        private int parameterCount;

        private ScopeState(final Scope scope) {
            this.scope = scope;
        }
    }

    /** Concrete immutable variable symbol. */
    private record VariableSymbolImpl(
            SymbolId id,
            String name,
            ScopeId scopeId,
            Span declarationSpan,
            Type type,
            Mutability mutability)
            implements VariableSymbol {
        @Override
        public SymbolKind kind() {
            return SymbolKind.VARIABLE;
        }
    }

    /** Concrete immutable parameter symbol. */
    private record ParameterSymbolImpl(
            SymbolId id,
            String name,
            ScopeId scopeId,
            Span declarationSpan,
            Type type,
            Mutability mutability,
            int ordinal)
            implements ParameterSymbol {
        @Override
        public SymbolKind kind() {
            return SymbolKind.PARAMETER;
        }
    }

    /** Concrete implementation of a named function symbol. */
    private record FunctionSymbolImpl(
            SymbolId id,
            String name,
            ScopeId scopeId,
            Span declarationSpan,
            List<ParameterDescriptor> parameters,
            Type returnType)
            implements FunctionSymbol {

        private FunctionSymbolImpl {
            parameters = List.copyOf(parameters);
        }

        @Override
        public List<ParameterDescriptor> parameters() {
            return List.copyOf(parameters);
        }

        @Override
        public SymbolKind kind() {
            return SymbolKind.FUNCTION;
        }
    }

    /** Concrete implementation of the unique main function symbol. */
    private record MainFunctionSymbolImpl(
            SymbolId id, String name, ScopeId scopeId, Span declarationSpan, Type returnType)
            implements MainFunctionSymbol {
        @Override
        public SymbolKind kind() {
            return SymbolKind.MAIN_FUNCTION;
        }
    }
}
