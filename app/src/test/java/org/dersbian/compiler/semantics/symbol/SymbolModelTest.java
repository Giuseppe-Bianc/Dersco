package org.dersbian.compiler.semantics.symbol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.Type;
import org.junit.jupiter.api.Test;

/** Tests immutable symbol and scope model invariants. */
class SymbolModelTest {
    private static final Span SPAN =
            Span.create(SourceLocation.create(1, 1, 0), SourceLocation.create(1, 2, 1));

    @Test
    void idsMustBeStrictlyPositive() {
        assertThatThrownBy(() -> new SymbolId(0L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SymbolId(-1L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ScopeId(0L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ScopeId(-1L)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new SymbolId(1L).value()).isEqualTo(1L);
        assertThat(new ScopeId(1L).value()).isEqualTo(1L);
    }

    @Test
    void globalScopeHasNoParentAndNoOwner() {
        final Scope scope =
                new Scope(new ScopeId(1L), ScopeKind.GLOBAL, Optional.empty(), 0, Optional.empty());

        assertThat(scope.parentId()).isEmpty();
        assertThat(scope.ownerSymbolId()).isEmpty();
        assertThat(scope.depth()).isZero();
    }

    @Test
    void nonGlobalScopesRequireParents() {
        assertThatThrownBy(
                        () ->
                                new Scope(
                                        new ScopeId(1L),
                                        ScopeKind.BLOCK,
                                        Optional.empty(),
                                        1,
                                        Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void onlyFunctionScopesMayHaveOwners() {
        final SymbolId owner = new SymbolId(7L);

        assertThatThrownBy(
                        () ->
                                new Scope(
                                        new ScopeId(2L),
                                        ScopeKind.BLOCK,
                                        Optional.of(new ScopeId(1L)),
                                        1,
                                        Optional.of(owner)))
                .isInstanceOf(IllegalArgumentException.class);

        final Scope function =
                new Scope(
                        new ScopeId(2L),
                        ScopeKind.FUNCTION,
                        Optional.of(new ScopeId(1L)),
                        1,
                        Optional.of(owner));
        assertThat(function.ownerSymbolId()).contains(owner);
    }

    @Test
    void functionSymbolCopiesItsParameterList() {
        final List<ParameterDescriptor> descriptors =
                List.of(
                        new ParameterDescriptor("a", new Type.I32(), Mutability.IMMUTABLE),
                        new ParameterDescriptor("b", new Type.I64(), Mutability.MUTABLE));
        final Symbol.FunctionSymbol function =
                new Symbol.FunctionSymbol(
                        new SymbolId(1L),
                        "f",
                        new Type.VoidT(),
                        descriptors,
                        new ScopeId(1L),
                        SPAN);

        assertThat(function.parameters()).containsExactlyElementsOf(descriptors);
        assertThatThrownBy(() -> function.parameters().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void mainSymbolIsStrictlyTypedAndNamed() {
        final Symbol.MainFunctionSymbol main =
                new Symbol.MainFunctionSymbol(
                        new SymbolId(1L), "main", new Type.VoidT(), new ScopeId(1L), SPAN);

        assertThat(main.kind()).isEqualTo(SymbolKind.MAIN_FUNCTION);
        assertThatThrownBy(
                        () ->
                                new Symbol.MainFunctionSymbol(
                                        new SymbolId(2L),
                                        "entry",
                                        new Type.VoidT(),
                                        new ScopeId(1L),
                                        SPAN))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                new Symbol.MainFunctionSymbol(
                                        new SymbolId(3L),
                                        "main",
                                        new Type.I32(),
                                        new ScopeId(1L),
                                        SPAN))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
