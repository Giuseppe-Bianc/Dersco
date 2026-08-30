package org.dersbian.compiler.semantics.symbol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.Type;
import org.junit.jupiter.api.Test;

/** Verifies that invalid operations do not partially mutate symbol-table state. */
class DefaultSymbolTableFailureTest {
    private static final Span SPAN =
            Span.create(SourceLocation.create(1, 1, 0), SourceLocation.create(1, 2, 1));

    @Test
    void duplicateVariableDoesNotConsumeASecondSymbolId() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Symbol first =
                ((DeclarationResult.Declared)
                                table.declareVariable("x", new Type.I32(), Mutability.MUTABLE, SPAN))
                        .symbol();

        final DeclarationResult duplicate =
                table.declareVariable("x", new Type.I64(), Mutability.IMMUTABLE, SPAN);

        assertThat(duplicate).isInstanceOf(DeclarationResult.AlreadyDeclared.class);
        assertThat(((DeclarationResult.AlreadyDeclared) duplicate).existingSymbol()).isSameAs(first);

        final Symbol next =
                ((DeclarationResult.Declared)
                                table.declareVariable("y", new Type.I32(), Mutability.MUTABLE, SPAN))
                        .symbol();
        assertThat(next.id().value()).isEqualTo(first.id().value() + 1L);
        table.assertInvariants();
    }

    @Test
    void invalidParameterOrdinalDoesNotChangeNextOrdinal() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Symbol function =
                ((DeclarationResult.Declared)
                                table.declareFunction(
                                        "f", new Type.VoidT(), List.of(), SPAN))
                        .symbol();
        table.enterFunctionScope(function.id());

        assertThatThrownBy(
                        () ->
                                table.declareParameter(
                                        "x", new Type.I32(), Mutability.MUTABLE, 1, SPAN))
                .isInstanceOf(IllegalArgumentException.class);

        final DeclarationResult result =
                table.declareParameter("x", new Type.I32(), Mutability.MUTABLE, 0, SPAN);
        assertThat(result).isInstanceOf(DeclarationResult.Declared.class);
        table.exitScope();
        table.assertInvariants();
    }

    @Test
    void invalidFunctionOwnerDoesNotCreateAScope() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Scope before = table.currentScope();

        assertThatThrownBy(() -> table.enterFunctionScope(new SymbolId(99L)))
                .isInstanceOf(IllegalArgumentException.class);

        assertThat(table.currentScope()).isEqualTo(before);
        table.assertInvariants();
    }

    @Test
    void globalScopeCannotBeExited() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Scope before = table.currentScope();

        assertThatThrownBy(table::exitScope).isInstanceOf(IllegalStateException.class);

        assertThat(table.currentScope()).isEqualTo(before);
        table.assertInvariants();
    }

    @Test
    void duplicateParametersDoNotAdvanceOrdinal() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Symbol function =
                ((DeclarationResult.Declared)
                                table.declareFunction(
                                        "f",
                                        new Type.VoidT(),
                                        List.of(
                                                new ParameterDescriptor(
                                                        "x", new Type.I32(), Mutability.MUTABLE),
                                                new ParameterDescriptor(
                                                        "y", new Type.I64(), Mutability.IMMUTABLE)),
                                        SPAN))
                        .symbol();
        table.enterFunctionScope(function.id());
        table.declareParameter("x", new Type.I32(), Mutability.MUTABLE, 0, SPAN);

        final DeclarationResult duplicate =
                table.declareParameter("x", new Type.I64(), Mutability.IMMUTABLE, 1, SPAN);
        assertThat(duplicate).isInstanceOf(DeclarationResult.AlreadyDeclared.class);

        final DeclarationResult second =
                table.declareParameter("y", new Type.I64(), Mutability.IMMUTABLE, 1, SPAN);
        assertThat(second).isInstanceOf(DeclarationResult.Declared.class);

        table.exitScope();
        table.assertInvariants();
    }
}
