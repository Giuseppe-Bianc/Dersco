package org.dersbian.compiler.semantics.symbol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.Type;
import org.junit.jupiter.api.Test;

/** Unit tests for {@link DefaultSymbolTable}. */
@SuppressWarnings({
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.AtLeastOneConstructor",
    "PMD.TooManyMethods",
    "PMD.UseExplicitTypes",
    "PMD.CommentRequired",
    "PMD.CloseResource"
})
class DefaultSymbolTableTest {
    private static final Span SPAN =
            Span.create(SourceLocation.create(1, 1, 0), SourceLocation.create(1, 2, 1));

    @Test
    void startsWithOnePermanentGlobalScopeAndPositiveIds() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        assertThat(table.currentScope()).isEqualTo(table.globalScope());
        assertThat(table.globalScope().id().value()).isEqualTo(1L);
        assertThat(table.globalScope().parentId()).isEmpty();
        assertThat(table.globalScope().depth()).isZero();
        assertThat(table.globalScope().ownerSymbolId()).isEmpty();
    }

    @Test
    void rejectsDuplicateWithoutReplacingBindingOrConsumingId() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final var first = table.declareVariable("x", new Type.I32(), Mutability.MUTABLE, SPAN);
        final var duplicate =
                table.declareVariable("x", new Type.I64(), Mutability.IMMUTABLE, SPAN);
        final var next = table.declareVariable("y", new Type.I64(), Mutability.IMMUTABLE, SPAN);
        assertThat(duplicate)
                .isEqualTo(
                        new DeclarationResult.AlreadyDeclared(
                                "x", ((DeclarationResult.Declared) first).symbol()));
        assertThat(((DeclarationResult.Declared) next).symbol().id().value()).isEqualTo(2L);
    }

    @Test
    void resolvesNearestBindingAndRestoresParentAfterExit() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final var global = table.declareVariable("x", new Type.I32(), Mutability.IMMUTABLE, SPAN);
        final Scope child = table.enterScope(ScopeKind.BLOCK);
        final var local = table.declareVariable("x", new Type.I64(), Mutability.MUTABLE, SPAN);
        assertThat(table.lookup("x").orElseThrow())
                .isEqualTo(((DeclarationResult.Declared) local).symbol());
        assertThat(table.exitScope()).isEqualTo(child);
        assertThat(table.lookup("x").orElseThrow())
                .isEqualTo(((DeclarationResult.Declared) global).symbol());
    }

    @Test
    void retainsClosedScopesAndDeclarationOrder() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Scope child = table.enterScope(ScopeKind.BLOCK);
        table.declareVariable("a", new Type.I32(), Mutability.IMMUTABLE, SPAN);
        table.declareVariable("b", new Type.I64(), Mutability.MUTABLE, SPAN);
        table.exitScope();
        assertThat(table.findScope(child.id())).contains(child);
        assertThat(table.symbolsInScope(child.id()))
                .extracting(Symbol::name)
                .containsExactly("a", "b");
    }

    @Test
    void cannotExitGlobalScope() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        assertThatThrownBy(table::exitScope).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void createsFunctionScopeWithOwnerAndTracksParameterOrdinals() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final var function =
                table.declareFunction(
                        "f",
                        new Type.VoidT(),
                        List.of(new ParameterDescriptor("a", new Type.I32(), Mutability.IMMUTABLE)),
                        SPAN);
        final Symbol.FunctionSymbol owner =
                (Symbol.FunctionSymbol) ((DeclarationResult.Declared) function).symbol();
        final Scope functionScope = table.enterFunctionScope(owner.id());
        table.declareParameter("a", new Type.I32(), Mutability.IMMUTABLE, 0, SPAN);
        assertThat(functionScope.ownerSymbolId()).contains(owner.id());
        assertThat(table.currentSymbols())
                .singleElement()
                .isInstanceOf(Symbol.ParameterSymbol.class);
        assertThat(((Symbol.ParameterSymbol) table.currentSymbols().getFirst()).ordinal()).isZero();
    }

    @Test
    void rejectsOutOfOrderParameters() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final var function = table.declareFunction("f", new Type.VoidT(), List.of(), SPAN);
        final Symbol.FunctionSymbol owner =
                (Symbol.FunctionSymbol) ((DeclarationResult.Declared) function).symbol();
        table.enterFunctionScope(owner.id());
        assertThatThrownBy(
                        () ->
                                table.declareParameter(
                                        "a", new Type.I32(), Mutability.IMMUTABLE, 1, SPAN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mainIsUniqueGlobalAndHasDedicatedKind() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final var first = table.declareMainFunction(SPAN);
        final var duplicate = table.declareMainFunction(SPAN);
        assertThat(((DeclarationResult.Declared) first).symbol().kind())
                .isEqualTo(SymbolKind.MAIN_FUNCTION);
        assertThat(duplicate)
                .isEqualTo(
                        new DeclarationResult.AlreadyDeclared(
                                "main", ((DeclarationResult.Declared) first).symbol()));
    }

    @Test
    void mainCannotBeDeclaredAsNormalFunction() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        assertThatThrownBy(() -> table.declareFunction("main", new Type.VoidT(), List.of(), SPAN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void lookupFromHistoricalScopeDoesNotDependOnCurrentScope() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final var global = table.declareVariable("x", new Type.I32(), Mutability.IMMUTABLE, SPAN);
        final Scope child = table.enterScope(ScopeKind.BLOCK);
        table.declareVariable("y", new Type.I64(), Mutability.IMMUTABLE, SPAN);
        table.exitScope();
        assertThat(table.lookupFrom(child.id(), "x"))
                .contains(((DeclarationResult.Declared) global).symbol());
        assertThat(table.lookupFrom(child.id(), "y")).isPresent();
    }

    @Test
    void findsSymbolsByStableIdentity() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Symbol symbol =
                ((DeclarationResult.Declared)
                                table.declareVariable(
                                        "x", new Type.I32(), Mutability.IMMUTABLE, SPAN))
                        .symbol();
        assertThat(table.find(symbol.id())).contains(symbol);
        assertThat(table.find(new SymbolId(symbol.id().value() + 1))).isEmpty();
    }

    @Test
    void loopScopeIsAvailableAndHasNoFunctionOwner() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Scope loop = table.enterScope(ScopeKind.LOOP);
        assertThat(loop.kind()).isEqualTo(ScopeKind.LOOP);
        assertThat(loop.ownerSymbolId()).isEmpty();
        assertThat(loop.depth()).isEqualTo(1);
    }

    @Test
    void scopeHandleEnforcesLifoLifecycle() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final ScopeHandle outer = table.openScope(ScopeKind.BLOCK);
        final ScopeHandle inner = table.openScope(ScopeKind.LOOP);
        assertThatThrownBy(outer::close).isInstanceOf(IllegalStateException.class);
        inner.close();
        outer.close();
        assertThat(table.currentScope()).isEqualTo(table.globalScope());
        assertThatThrownBy(outer::close).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void lookupLocalCanReadHistoricalScope() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Scope child = table.enterScope(ScopeKind.BLOCK);
        final Symbol symbol =
                ((DeclarationResult.Declared)
                                table.declareVariable(
                                        "x", new Type.I32(), Mutability.IMMUTABLE, SPAN))
                        .symbol();
        table.exitScope();
        assertThat(table.lookupLocal(child.id(), "x")).contains(symbol);
        assertThat(table.lookupLocal(child.id(), "missing")).isEmpty();
    }
}
