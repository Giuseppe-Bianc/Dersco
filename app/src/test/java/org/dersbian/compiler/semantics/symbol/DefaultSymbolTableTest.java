package org.dersbian.compiler.semantics.symbol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.dersbian.compiler.syntax.ast.Type;
import org.junit.jupiter.api.Test;

class DefaultSymbolTableTest {
    @Test
    void startsWithOnePermanentGlobalScope() {
        final DefaultSymbolTable table = new DefaultSymbolTable();

        assertThat(table.currentScope()).isEqualTo(table.globalScope());
        assertThat(table.scope(table.globalScope())).isPresent();
        assertThat(table.scope(table.globalScope()).orElseThrow().parentId()).isNull();
    }

    @Test
    void rejectsDuplicateDeclarationWithoutReplacingBinding() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final var first = table.declareVariable("x", new Type.I32(), Mutability.MUTABLE);
        final var duplicate = table.declareVariable("x", new Type.I64(), Mutability.IMMUTABLE);

        assertThat(first).isInstanceOf(DeclarationResult.Declared.class);
        assertThat(duplicate).isEqualTo(
                new DeclarationResult.AlreadyDeclared("x", table.globalScope()));
        assertThat(table.lookup("x").orElseThrow().type()).isEqualTo(new Type.I32());
    }

    @Test
    void resolvesNearestBindingAndRestoresParentAfterExit() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final var global = table.declareVariable("x", new Type.I32(), Mutability.IMMUTABLE);
        table.enterScope(ScopeKind.BLOCK);
        final var local = table.declareVariable("x", new Type.I64(), Mutability.MUTABLE);

        assertThat(table.lookup("x").orElseThrow()).isEqualTo(
                ((DeclarationResult.Declared) local).symbol());
        table.exitScope();
        assertThat(table.lookup("x").orElseThrow()).isEqualTo(
                ((DeclarationResult.Declared) global).symbol());
    }

    @Test
    void retainsClosedScopesAndProtectsTheirCollections() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final ScopeId child = table.enterScope(ScopeKind.BLOCK);
        table.declareVariable("x", new Type.I32(), Mutability.IMMUTABLE);
        table.exitScope();

        final Scope scope = table.scope(child).orElseThrow();
        assertThat(scope.lookupLocal("x")).isPresent();
        assertThatThrownBy(() -> scope.symbols().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void cannotExitGlobalScope() {
        final DefaultSymbolTable table = new DefaultSymbolTable();

        assertThatThrownBy(table::exitScope).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validatesParameterOrdinalsAndDuplicates() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final var parameters = List.of(
                new ParameterDescriptor("a", new Type.I32(), 0, Mutability.IMMUTABLE),
                new ParameterDescriptor("a", new Type.I32(), 1, Mutability.IMMUTABLE));

        assertThatThrownBy(() -> table.declareFunction("f", new Type.VoidT(), parameters))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(table.lookup("f")).isEmpty();
    }
}
