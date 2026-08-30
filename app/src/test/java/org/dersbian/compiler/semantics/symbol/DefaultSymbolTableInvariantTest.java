package org.dersbian.compiler.semantics.symbol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.Type;
import org.junit.jupiter.api.Test;

/** Tests structural invariants and the structured scope API. */
class DefaultSymbolTableInvariantTest {
    private static final Span SPAN = Span.point(SourceLocation.create(1, 1, 0));

    @Test
    void structuredScopeIsLifoAndCloseIsIdempotent() {
        final DefaultSymbolTable table = new DefaultSymbolTable();

        final ScopeHandle handle = table.openScope(ScopeKind.BLOCK);
        final Scope opened = handle.scope();
        assertThat(table.currentScope()).isEqualTo(opened);

        handle.close();
        handle.close();
        assertThat(table.currentScope()).isEqualTo(table.globalScope());
        table.assertConsistent();
    }

    @Test
    void structuredScopeCannotCloseOutOfOrder() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final ScopeHandle outer = table.openScope(ScopeKind.BLOCK);
        final ScopeHandle inner = table.openScope(ScopeKind.BLOCK);

        assertThatThrownBy(outer::close).isInstanceOf(IllegalStateException.class);
        assertThat(table.currentScope()).isEqualTo(inner.scope());
        inner.close();
        outer.close();
        table.assertConsistent();
    }

    @Test
    void functionOwnerMustBeInImmediateParentScope() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final FunctionSymbol function = (FunctionSymbol) ((DeclarationResult.Declared) table
                .declareFunction("f", List.of(), new Type.VoidT(), SPAN)).symbol();
        table.enterScope(ScopeKind.BLOCK);

        assertThatThrownBy(() -> table.enterScope(ScopeKind.FUNCTION, function.id()))
                .isInstanceOf(IllegalStateException.class);
        table.assertConsistent();
    }

    @Test
    void nonFunctionScopesRejectOwners() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final FunctionSymbol function = (FunctionSymbol) ((DeclarationResult.Declared) table
                .declareFunction("f", List.of(), new Type.VoidT(), SPAN)).symbol();

        assertThatThrownBy(() -> table.enterScope(ScopeKind.BLOCK, function.id()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> table.enterScope(ScopeKind.GLOBAL)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> table.enterScope(ScopeKind.FUNCTION)).isInstanceOf(IllegalArgumentException.class);
        table.assertConsistent();
    }

    @Test
    void parameterOrdinalStateRemainsUnchangedAfterInvalidDeclaration() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final FunctionSymbol function = (FunctionSymbol) ((DeclarationResult.Declared) table
                .declareFunction("f", List.of(), new Type.VoidT(), SPAN)).symbol();
        table.enterScope(ScopeKind.FUNCTION, function.id());

        assertThatThrownBy(() -> table.declareParameter("bad", new Type.I32(), Mutability.IMMUTABLE, 1, SPAN))
                .isInstanceOf(IllegalArgumentException.class);
        final ParameterSymbol parameter = (ParameterSymbol) ((DeclarationResult.Declared) table
                .declareParameter("a", new Type.I32(), Mutability.IMMUTABLE, 0, SPAN)).symbol();
        assertThat(parameter.ordinal()).isZero();
        table.assertConsistent();
    }

    @Test
    void idsArePositiveAndIndependent() {
        final DefaultSymbolTable first = new DefaultSymbolTable();
        final DefaultSymbolTable second = new DefaultSymbolTable();
        assertThat(first.globalScope().id().value()).isPositive();
        assertThat(second.globalScope().id().value()).isPositive();

        final Symbol a = ((DeclarationResult.Declared) first
                .declareVariable("a", new Type.I32(), Mutability.IMMUTABLE, SPAN)).symbol();
        final Symbol b = ((DeclarationResult.Declared) first
                .declareVariable("b", new Type.I32(), Mutability.IMMUTABLE, SPAN)).symbol();
        assertThat(a.id().value()).isLessThan(b.id().value());
        assertThat(a.id()).isNotEqualTo(b.id());
        assertThat(first.find(a.id())).containsSame(a);
        first.assertConsistent();
        second.assertConsistent();
    }

    @Test
    void declarationOrderIsStableAfterShadowingAndScopeExit() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Symbol a = ((DeclarationResult.Declared) table
                .declareVariable("a", new Type.I32(), Mutability.IMMUTABLE, SPAN)).symbol();
        final Symbol b = ((DeclarationResult.Declared) table
                .declareVariable("b", new Type.I32(), Mutability.IMMUTABLE, SPAN)).symbol();
        final Scope block = table.enterScope(ScopeKind.BLOCK);
        final Symbol shadow = ((DeclarationResult.Declared) table
                .declareVariable("a", new Type.I64(), Mutability.MUTABLE, SPAN)).symbol();

        assertThat(table.symbolsInScope(block.id())).containsExactly(shadow);
        table.exitScope();
        assertThat(table.currentSymbols()).containsExactly(a, b);
        table.assertConsistent();
    }
}
