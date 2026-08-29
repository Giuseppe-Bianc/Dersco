package org.dersbian.compiler.semantics.symbol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.syntax.ast.Type;
import org.junit.jupiter.api.Test;

class DefaultSymbolTableTest {
    private static final Span SPAN = Span.create(
            SourceLocation.create(1, 1, 0), SourceLocation.create(1, 2, 1));

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
    void rejectsDuplicateDeclarationWithoutReplacingBindingOrConsumingAnId() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final var first = table.declareVariable("x", new Type.I32(), Mutability.MUTABLE, SPAN);
        final var duplicate = table.declareVariable("x", new Type.I64(), Mutability.IMMUTABLE, SPAN);
        final var next = table.declareVariable("y", new Type.I64(), Mutability.IMMUTABLE, SPAN);

        assertThat(first).isInstanceOf(DeclarationResult.Declared.class);
        assertThat(duplicate).isEqualTo(
                new DeclarationResult.AlreadyDeclared("x", table.globalScope().id()));
        assertThat(((DeclarationResult.Declared) next).symbol().id().value()).isEqualTo(2L);
        assertThat(table.lookup("x").orElseThrow()).isEqualTo(
                ((DeclarationResult.Declared) first).symbol());
    }

    @Test
    void resolvesNearestBindingAndRestoresParentAfterExit() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final var global = table.declareVariable("x", new Type.I32(), Mutability.IMMUTABLE, SPAN);
        final Scope child = table.enterScope(ScopeKind.BLOCK);
        final var local = table.declareVariable("x", new Type.I64(), Mutability.MUTABLE, SPAN);

        assertThat(child.parentId()).contains(table.globalScope().id());
        assertThat(child.depth()).isEqualTo(1);
        assertThat(table.lookup("x").orElseThrow()).isEqualTo(
                ((DeclarationResult.Declared) local).symbol());
        assertThat(table.exitScope()).isEqualTo(child);
        assertThat(table.lookup("x").orElseThrow()).isEqualTo(
                ((DeclarationResult.Declared) global).symbol());
    }

    @Test
    void retainsClosedScopesAndDeclarationOrder() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Scope child = table.enterScope(ScopeKind.BLOCK);
        table.declareVariable("a", new Type.I32(), Mutability.IMMUTABLE, SPAN);
        table.declareVariable("b", new Type.I64(), Mutability.MUTABLE, SPAN);
        table.exitScope();

        assertThat(table.findScope(child.id())).contains(child);
        assertThat(table.symbolsInScope(child.id())).extracting(Symbol::name)
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
        final var function = table.declareFunction(
                "f", new Type.VoidT(),
                List.of(new ParameterDescriptor("a", new Type.I32(), Mutability.IMMUTABLE)), SPAN);
        final Symbol.FunctionSymbol functionSymbol =
                (Symbol.FunctionSymbol) ((DeclarationResult.Declared) function).symbol();

        final Scope functionScope = table.enterFunctionScope(functionSymbol.id());
        table.declareParameter("a", new Type.I32(), Mutability.IMMUTABLE, 0, SPAN);

        assertThat(functionScope.ownerSymbolId()).contains(functionSymbol.id());
        assertThat(table.currentSymbols()).singleElement().satisfies(symbol -> {
            assertThat(symbol).isInstanceOf(Symbol.ParameterSymbol.class);
            assertThat(((Symbol.ParameterSymbol) symbol).ordinal()).isZero();
        });
    }

    @Test
    void rejectsOutOfOrderParameters() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final var function = table.declareFunction("f", new Type.VoidT(), List.of(), SPAN);
        final Symbol.FunctionSymbol owner =
                (Symbol.FunctionSymbol) ((DeclarationResult.Declared) function).symbol();
        table.enterFunctionScope(owner.id());

        assertThatThrownBy(() -> table.declareParameter(
                "a", new Type.I32(), Mutability.IMMUTABLE, 1, SPAN))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mainIsUniqueGlobalAndHasDedicatedKind() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final var first = table.declareMainFunction(SPAN);
        final var duplicate = table.declareMainFunction(SPAN);

        assertThat(first).isInstanceOf(DeclarationResult.Declared.class);
        assertThat(((DeclarationResult.Declared) first).symbol().kind())
                .isEqualTo(SymbolKind.MAIN_FUNCTION);
        assertThat(duplicate).isEqualTo(
                new DeclarationResult.AlreadyDeclared("main", table.globalScope().id()));
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

        assertThat(table.lookupFrom(child.id(), "x")).contains(
                ((DeclarationResult.Declared) global).symbol());
        assertThat(table.lookupFrom(child.id(), "y")).isPresent();
    }
}
