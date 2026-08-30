package org.dersbian.compiler.semantics.symbol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.Type;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Additional edge-case and stress tests for the version-one symbol table contract. */
@SuppressWarnings({"PMD.UnitTestContainsTooManyAsserts", "PMD.AvoidDuplicateLiterals"})
class DefaultSymbolTableEdgeCaseTest {
    /** Synthetic span used across symbol test declarations. */
    private static final Span SPAN = Span.point(SourceLocation.create(1, 1, 0));

    @ParameterizedTest
    @ValueSource(strings = {"x", "_x", "X", "main"})
    void namesRemainCaseSensitiveAndUnchanged(final String name) {
        final DefaultSymbolTable table = new DefaultSymbolTable();

        final Symbol symbol = declaredVariable(table, name);

        assertThat(symbol.name()).isEqualTo(name);
        assertThat(table.lookup(name)).containsSame(symbol);
    }

    @Test
    void sameNameAcrossDifferentKindsIsDuplicateInOneNamespace() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        declaredVariable(table, "x");

        assertThat(table.declareFunction("x", List.of(), new Type.VoidT(), SPAN))
                .isInstanceOf(DeclarationResult.AlreadyDeclared.class);
        assertThat(table.currentSymbols()).hasSize(1);
        table.assertConsistent();
    }

    @Test
    void functionSignaturePreservesDescriptorOrderAndValues() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final List<ParameterDescriptor> descriptors = List.of(
                new ParameterDescriptor("first", new Type.I32(), Mutability.IMMUTABLE),
                new ParameterDescriptor("second", new Type.F64(), Mutability.MUTABLE));

        final FunctionSymbol function =
                (FunctionSymbol) declaredResult(table.declareFunction("f", descriptors, new Type.Bool(), SPAN));

        assertThat(function.parameters()).containsExactlyElementsOf(descriptors);
        assertThat(function.returnType()).isEqualTo(new Type.Bool());
        assertThat(function.scopeId()).isEqualTo(table.globalScope().id());
        assertThat(function.declarationSpan()).isSameAs(SPAN);
    }

    @Test
    void invalidFunctionSignatureIsAtomic() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final List<ParameterDescriptor> descriptors = new ArrayList<>();
        descriptors.add(new ParameterDescriptor("a", new Type.I32(), Mutability.IMMUTABLE));
        descriptors.add(new ParameterDescriptor("a", new Type.I64(), Mutability.MUTABLE));
        final List<Symbol> before = table.currentSymbols();

        assertThatThrownBy(() -> table.declareFunction("f", descriptors, new Type.VoidT(), SPAN))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(descriptors).hasSize(2);
        assertThat(table.currentSymbols()).containsExactlyElementsOf(before);
        table.assertConsistent();
    }

    @Test
    void unknownHistoricalScopeIsNotAVisibleScope() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Scope block = table.enterScope(ScopeKind.BLOCK);
        declaredVariable(table, "x");
        table.exitScope();

        assertThat(table.findScope(block.id())).contains(block);
        assertThat(table.lookupLocal(block.id(), "x")).containsMatch(symbol -> symbol.name().equals("x"));
        assertThat(table.lookupFrom(block.id(), "x")).isPresent();
        assertThat(table.lookupLocal(new ScopeId(Long.MAX_VALUE), "x")).isEmpty();
        assertThat(table.lookupFrom(new ScopeId(Long.MAX_VALUE), "x")).isEmpty();
    }

    @Test
    void childBindingIsNotVisibleFromParentAfterChildExit() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Scope parent = table.enterScope(ScopeKind.BLOCK);
        declaredVariable(table, "parent");
        final Scope child = table.enterScope(ScopeKind.BLOCK);
        declaredVariable(table, "child");
        table.exitScope();

        assertThat(table.currentScope()).isEqualTo(parent);
        assertThat(table.lookup("parent")).isPresent();
        assertThat(table.lookup("child")).isEmpty();
        assertThat(table.lookupFrom(child.id(), "child")).isPresent();
        table.exitScope();
        assertThat(table.lookup("parent")).isEmpty();
    }

    @Test
    void parameterDuplicateDoesNotAdvanceOrdinal() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final FunctionSymbol function =
                (FunctionSymbol)
                        declaredResult(table.declareFunction("f", List.of(), new Type.VoidT(), SPAN));
        table.enterScope(ScopeKind.FUNCTION, function.id());
        declaredParameter(table, "a", 0);

        final DeclarationResult duplicate =
                table.declareParameter("a", new Type.I64(), Mutability.MUTABLE, 1, SPAN);

        assertThat(duplicate).isInstanceOf(DeclarationResult.AlreadyDeclared.class);
        final ParameterSymbol next = declaredParameter(table, "b", 1);
        assertThat(next.ordinal()).isEqualTo(1);
        table.assertConsistent();
    }

    @Test
    void lookupDoesNotCreateBindings() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final int before = table.currentSymbols().size();

        assertThat(table.lookup("missing")).isEmpty();
        assertThat(table.lookupLocal("missing")).isEmpty();
        assertThat(table.currentSymbols()).hasSize(before);
        table.assertConsistent();
    }

    @Test
    void deepNestingUsesIterativeLookupAndHistoricalScopesRemainQueryable() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final int depth = 2_000;
        final List<ScopeId> scopes = new ArrayList<>(depth);
        final List<Symbol> symbols = new ArrayList<>(depth);
        declaredVariable(table, "root");

        IntStream.range(0, depth)
                .forEach(
                        index -> {
                            scopes.add(table.enterScope(ScopeKind.BLOCK).id());
                            symbols.add(declaredVariable(table, "s" + index));
                        });

        assertThat(table.lookup("root")).isPresent();
        assertThat(table.lookup("s0")).isPresent();
        assertThat(table.lookup("s1999")).containsSame(symbols.get(1999));
        assertThat(table.lookupFrom(scopes.get(0), "root")).isPresent();
        assertThat(table.lookupFrom(scopes.get(1_000), "s500")).isPresent();

        IntStream.range(0, depth).forEach(ignored -> table.exitScope());
        assertThat(table.currentScope()).isEqualTo(table.globalScope());
        assertThat(table.lookup("s0")).isEmpty();
        assertThat(table.lookup("root")).isPresent();
        table.assertConsistent();
    }

    @Test
    void manySymbolsRetainInsertionOrder() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final int count = 500;

        IntStream.range(0, count).forEach(index -> declaredVariable(table, "v" + index));

        assertThat(table.currentSymbols()).extracting(Symbol::name).containsExactlyElementsOf(
                IntStream.range(0, count).mapToObj(index -> "v" + index).toList());
        table.assertConsistent();
    }

    @Test
    void allPublicLookupOperationsRejectNullNames() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Scope scope = table.globalScope();
        final List<Executable> calls = List.of(
                () -> table.lookup(null),
                () -> table.lookupFrom(scope.id(), null),
                () -> table.lookupLocal(null),
                () -> table.lookupLocal(scope.id(), null));

        assertThat(calls).allSatisfy(call -> assertThatThrownBy(call).isInstanceOf(NullPointerException.class));
    }

    @Test
    void enterAndExitDoNotMutatePreviouslyReturnedScopeSnapshots() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Scope global = table.globalScope();
        final Scope block = table.enterScope(ScopeKind.BLOCK);
        table.exitScope();

        assertThat(global.kind()).isEqualTo(ScopeKind.GLOBAL);
        assertThat(global.depth()).isZero();
        assertThat(block.kind()).isEqualTo(ScopeKind.BLOCK);
        assertThat(block.depth()).isEqualTo(1);
        assertThat(table.currentScope()).isEqualTo(global);
    }

    @Test
    void mainConflictsWithOrdinaryFunctionAndVariableInGlobalNamespace() {
        final DefaultSymbolTable first = new DefaultSymbolTable();
        assertThat(first.declareFunction("main", List.of(), new Type.VoidT(), SPAN))
                .isInstanceOf(DeclarationResult.Declared.class);
        assertThat(first.declareMainFunction(SPAN)).isInstanceOf(DeclarationResult.AlreadyDeclared.class);

        final DefaultSymbolTable second = new DefaultSymbolTable();
        declaredVariable(second, "main");
        assertThat(second.declareMainFunction(SPAN)).isInstanceOf(DeclarationResult.AlreadyDeclared.class);
    }

    private static Symbol declaredVariable(final DefaultSymbolTable table, final String name) {
        return declaredResult(
                table.declareVariable(name, new Type.I32(), Mutability.IMMUTABLE, SPAN));
    }

    private static ParameterSymbol declaredParameter(
            final DefaultSymbolTable table, final String name, final int ordinal) {
        return (ParameterSymbol)
                declaredResult(
                        table.declareParameter(
                                name, new Type.I32(), Mutability.IMMUTABLE, ordinal, SPAN));
    }

    private static Symbol declaredResult(final DeclarationResult result) {
        assertThat(result).isInstanceOf(DeclarationResult.Declared.class);
        return ((DeclarationResult.Declared) result).symbol();
    }
}
