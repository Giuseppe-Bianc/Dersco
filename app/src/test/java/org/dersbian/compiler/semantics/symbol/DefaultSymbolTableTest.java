package org.dersbian.compiler.semantics.symbol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.Type;
import org.junit.jupiter.api.Test;

/** Contract tests for the default version-one symbol table implementation. */
class DefaultSymbolTableTest {
    private static final Span SPAN = Span.point(SourceLocation.create(1, 1, 0));

    @Test
    void constructionCreatesOnlyGlobalScope() {
        final DefaultSymbolTable table = new DefaultSymbolTable();

        assertThat(table.currentScope()).isEqualTo(table.globalScope());
        assertThat(table.globalScope().kind()).isEqualTo(ScopeKind.GLOBAL);
        assertThat(table.globalScope().depth()).isZero();
        assertThat(table.globalScope().parentId()).isEmpty();
        assertThat(table.globalScope().ownerSymbolId()).isEmpty();
    }

    @Test
    void scopesAreNestedAndExitedInLifoOrder() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Scope block = table.enterScope(ScopeKind.BLOCK);
        final Scope loop = table.enterScope(ScopeKind.LOOP);

        assertThat(loop.parentId()).contains(block.id());
        assertThat(loop.depth()).isEqualTo(block.depth() + 1);
        assertThat(table.exitScope()).isEqualTo(block);
        assertThat(table.exitScope()).isEqualTo(table.globalScope());
        assertThatThrownBy(table::exitScope).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void declarationPreservesIdentityAndRejectsDuplicatesWithoutMutation() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final DeclarationResult first =
                table.declareVariable("x", new Type.I32(), Mutability.MUTABLE, SPAN);
        final List<Symbol> before = table.currentSymbols();
        final DeclarationResult duplicate =
                table.declareVariable("x", new Type.I64(), Mutability.IMMUTABLE, SPAN);

        assertThat(first).isInstanceOf(DeclarationResult.Declared.class);
        assertThat(duplicate).isInstanceOf(DeclarationResult.AlreadyDeclared.class);
        assertThat(((DeclarationResult.AlreadyDeclared) duplicate).existingSymbol())
                .isEqualTo(((DeclarationResult.Declared) first).symbol());
        assertThat(table.currentSymbols()).containsExactlyElementsOf(before);
    }

    @Test
    void shadowingResolvesNearestBinding() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Symbol global =
                ((DeclarationResult.Declared)
                                table.declareVariable(
                                        "x", new Type.I32(), Mutability.MUTABLE, SPAN))
                        .symbol();
        final Scope block = table.enterScope(ScopeKind.BLOCK);
        final Symbol local =
                ((DeclarationResult.Declared)
                                table.declareVariable(
                                        "x", new Type.I64(), Mutability.IMMUTABLE, SPAN))
                        .symbol();

        assertThat(table.lookup("x")).contains(local);
        assertThat(table.lookupLocal(block.id())).contains(local);
        assertThat(table.lookupFrom(table.globalScope().id(), "x")).contains(global);
        table.exitScope();
        assertThat(table.lookup("x")).contains(global);
        assertThat(table.lookupFrom(block.id(), "x")).contains(local);
    }

    @Test
    void lookupMissingIsEmptyAndParentIsNotVisibleDownward() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        table.declareVariable("x", new Type.I32(), Mutability.MUTABLE, SPAN);
        final Scope block = table.enterScope(ScopeKind.BLOCK);
        table.declareVariable("y", new Type.I32(), Mutability.MUTABLE, SPAN);

        assertThat(table.lookup("x")).isPresent();
        assertThat(table.lookupLocal("x")).isEmpty();
        assertThat(table.lookupFrom(block.id(), "missing")).isEmpty();
        table.exitScope();
        assertThat(table.lookup("y")).isEmpty();
    }

    @Test
    void parametersRequireConsecutiveOrdinalsAndUniqueNames() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Symbol function =
                ((DeclarationResult.Declared)
                                table.declareFunction("f", List.of(), new Type.VoidT(), SPAN))
                        .symbol();
        table.enterScope(ScopeKind.FUNCTION, function.id());

        final DeclarationResult first =
                table.declareParameter("a", new Type.I32(), Mutability.IMMUTABLE, 0, SPAN);
        final DeclarationResult second =
                table.declareParameter("b", new Type.I64(), Mutability.MUTABLE, 1, SPAN);
        assertThat(first).isInstanceOf(DeclarationResult.Declared.class);
        assertThat(second).isInstanceOf(DeclarationResult.Declared.class);
        assertThatThrownBy(
                        () ->
                                table.declareParameter(
                                        "c", new Type.I32(), Mutability.IMMUTABLE, 3, SPAN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(table.currentSymbols()).extracting(Symbol::name).containsExactly("a", "b");
    }

    @Test
    void mainIsGlobalUniqueVoidAndOwnerOfFunctionScope() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final DeclarationResult result = table.declareMainFunction(SPAN);
        final Symbol main = ((DeclarationResult.Declared) result).symbol();
        final DeclarationResult duplicate = table.declareMainFunction(SPAN);

        assertThat(main).isInstanceOf(MainFunctionSymbol.class);
        assertThat(main.name()).isEqualTo("main");
        assertThat(((MainFunctionSymbol) main).returnType()).isEqualTo(new Type.VoidT());
        assertThat(duplicate).isInstanceOf(DeclarationResult.AlreadyDeclared.class);
        final Scope functionScope = table.enterScope(ScopeKind.FUNCTION, main.id());
        assertThat(functionScope.ownerSymbolId()).contains(main.id());
        assertThatThrownBy(() -> table.enterScope(ScopeKind.FUNCTION, new SymbolId(999)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void invalidDeclarationsDoNotMutateStateAndCollectionsAreDefensive() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final List<Symbol> before = table.currentSymbols();

        assertThatThrownBy(
                        () -> table.declareVariable("", new Type.I32(), Mutability.MUTABLE, SPAN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> table.declareVariable("x", null, Mutability.MUTABLE, SPAN))
                .isInstanceOf(NullPointerException.class);
        assertThat(table.currentSymbols()).containsExactlyElementsOf(before);

        final List<Symbol> returned = table.currentSymbols();
        assertThatThrownBy(() -> returned.add(null))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void functionSignatureIsImmutableAndParametersAreCopied() {
        final var parameters =
                new java.util.ArrayList<>(
                        List.of(
                                new ParameterDescriptor("a", new Type.I32(), Mutability.IMMUTABLE),
                                new ParameterDescriptor("b", new Type.I64(), Mutability.MUTABLE)));
        final FunctionSymbol function =
                (FunctionSymbol)
                        ((DeclarationResult.Declared) tableWithFunction(parameters)).symbol();
        parameters.clear();

        assertThat(function.parameters()).hasSize(2);
        assertThatThrownBy(() -> function.parameters().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static DeclarationResult tableWithFunction(final List<ParameterDescriptor> parameters) {
        return new DefaultSymbolTable().declareFunction("f", parameters, new Type.I32(), SPAN);
    }
}
