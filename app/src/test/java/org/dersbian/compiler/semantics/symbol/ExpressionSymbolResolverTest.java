package org.dersbian.compiler.semantics.symbol;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.Expr;
import org.dersbian.compiler.syntax.ast.Type;
import org.junit.jupiter.api.Test;

/** Verifies lexical expression binding against current and historical scopes. */
@SuppressWarnings({"PMD.AtLeastOneConstructor", "PMD.UnitTestContainsTooManyAsserts"})
class ExpressionSymbolResolverTest {
    /** Synthetic source span for expression fixtures. */
    private static final Span SPAN = Span.point(SourceLocation.create(1, 1, 0));

    @Test
    void resolvesVariableFromCurrentScope() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Symbol symbol = declaredVariable(table, "x", new Type.I32());
        final Expr.Variable reference = new Expr.Variable("x", SPAN);
        final ExpressionSymbolResolver resolver = new ExpressionSymbolResolver(table);

        final ExpressionBindingResult result = resolver.resolve(reference);

        assertThat(result.symbolOf(reference)).contains(symbol.id());
        assertThat(result.bindings()).containsEntry(reference, symbol.id());
    }

    @Test
    void resolvesShadowedVariableFromExplicitScope() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Symbol global = declaredVariable(table, "x", new Type.I32());
        final Scope block = table.enterScope(ScopeKind.BLOCK);
        final Symbol local = declaredVariable(table, "x", new Type.I64());
        final Expr.Variable reference = new Expr.Variable("x", SPAN);
        final ExpressionSymbolResolver resolver = new ExpressionSymbolResolver(table);

        final ExpressionBindingResult current = resolver.resolve(reference);
        final ExpressionBindingResult globalResult =
                resolver.resolveFrom(table.globalScope().id(), reference);
        final ExpressionBindingResult historical = resolver.resolveFrom(block.id(), reference);

        assertThat(current.symbolOf(reference)).contains(local.id());
        assertThat(globalResult.symbolOf(reference)).contains(global.id());
        assertThat(historical.symbolOf(reference)).contains(local.id());
        table.exitScope();
        table.assertConsistent();
    }

    @Test
    void leavesMissingVariableUnresolved() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Expr.Variable reference = new Expr.Variable("missing", SPAN);
        final ExpressionSymbolResolver resolver = new ExpressionSymbolResolver(table);

        final ExpressionBindingResult result = resolver.resolve(reference);

        assertThat(result.symbolOf(reference)).isEmpty();
        assertThat(result.bindings()).isEmpty();
    }

    @Test
    void resolvesReferencesInsideGroupingArrayAndAssignment() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Symbol symbol = declaredVariable(table, "x", new Type.I32());
        final Expr.Variable target = new Expr.Variable("x", SPAN);
        final Expr.Variable value = new Expr.Variable("x", SPAN);
        final Expr.Assign assignment = new Expr.Assign(target, value, SPAN);
        final Expr.Grouping grouping = new Expr.Grouping(assignment, SPAN);
        final Expr.ArrayLiteral array = new Expr.ArrayLiteral(List.of(grouping), SPAN);
        final ExpressionSymbolResolver resolver = new ExpressionSymbolResolver(table);

        final ExpressionBindingResult result = resolver.resolve(array);

        assertThat(result.bindings()).containsEntry(target, symbol.id());
        assertThat(result.bindings()).containsEntry(value, symbol.id());
        assertThat(result.bindings()).hasSize(1);
    }

    @Test
    void historicalScopeResolutionRemainsValidAfterScopeExit() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Scope block = table.enterScope(ScopeKind.BLOCK);
        final Symbol symbol = declaredVariable(table, "x", new Type.I32());
        final Expr.Variable reference = new Expr.Variable("x", SPAN);
        table.exitScope();

        final ExpressionBindingResult result =
                new ExpressionSymbolResolver(table).resolveFrom(block.id(), reference);

        assertThat(result.symbolOf(reference)).contains(symbol.id());
        assertThat(table.currentScope()).isEqualTo(table.globalScope());
        assertThat(table.lookup("x")).isEmpty();
    }

    @Test
    void resolverDoesNotMutateSymbolTable() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        declaredVariable(table, "x", new Type.I32());
        final List<Symbol> before = table.currentSymbols();
        final Expr.Variable reference = new Expr.Variable("x", SPAN);

        new ExpressionSymbolResolver(table).resolve(reference);

        assertThat(table.currentSymbols()).containsExactlyElementsOf(before);
        table.assertConsistent();
    }

    private static Symbol declaredVariable(
            final DefaultSymbolTable table, final String name, final Type type) {
        final DeclarationResult result =
                table.declareVariable(name, type, Mutability.IMMUTABLE, SPAN);
        assertThat(result).isInstanceOf(DeclarationResult.Declared.class);
        return ((DeclarationResult.Declared) result).symbol();
    }
}
