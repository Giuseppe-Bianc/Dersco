package org.dersbian.compiler.semantics.symbol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.error.ErrorCode;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.BinaryOp;
import org.dersbian.compiler.syntax.ast.ElseBranch;
import org.dersbian.compiler.syntax.ast.Expr;
import org.dersbian.compiler.syntax.ast.Parameter;
import org.dersbian.compiler.syntax.ast.Stmt;
import org.dersbian.compiler.syntax.ast.Type;
import org.dersbian.compiler.syntax.ast.UnaryOp;
import org.dersbian.compiler.syntax.ast.UnaryOpSide;
import org.junit.jupiter.api.Test;

/** Normative tests for lexical name resolution. */
@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.LongVariable",
    "PMD.ShortVariable",
    "PMD.TooManyMethods",
})
class NameResolverTest {
    /** Reusable single-point source span for test fixtures. */
    private static final Span SPAN = Span.point(SourceLocation.create(1, 1, 0));

    @Test
    void resolvesGlobalAndShadowedReferencesByStableIdentity() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Stmt.VarDeclaration global = variable("x", 0);
        final Expr.Variable outerReference = reference("x", 20);
        final Stmt.Block block =
                new Stmt.Block(List.of(variable("x", 30), expression(reference("x", 50))), SPAN);
        final NameResolutionResult result =
                new NameResolver(table)
                        .resolve(
                                List.of(global, expression(outerReference), block),
                                Mutability.IMMUTABLE);

        final Symbol globalSymbol = declared(result, 0);
        final Symbol innerSymbol = declared(result, 1);
        assertThat(result.bindingOf(outerReference)).contains(globalSymbol.id());
        final Expr.Variable innerReference =
                (Expr.Variable) ((Stmt.Expression) block.statements().get(1)).expr();
        assertThat(result.bindingOf(innerReference)).contains(innerSymbol.id());
        assertThat(globalSymbol.id()).isNotEqualTo(innerSymbol.id());
        table.assertConsistent();
    }

    @Test
    void fallsBackToOuterDeclarationBeforeInnerShadowingDeclaration() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Symbol outer = declaredVariable(table, "x");
        final Expr.Variable reference = reference("x", 10);
        final Stmt.Block block =
                new Stmt.Block(List.of(expression(reference), variable("x", 20)), SPAN);
        final NameResolutionResult result =
                new NameResolver(table).resolve(List.of(block), Mutability.IMMUTABLE);

        assertThat(result.bindingOf(reference)).contains(outer.id());
        assertThat(result.diagnostics()).isEmpty();
    }

    @Test
    void rejectsReferenceWhenNoDeclarationExists() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Expr.Variable reference = reference("missing", 10);
        final NameResolutionResult result =
                new NameResolver(table)
                        .resolve(List.of(expression(reference)), Mutability.IMMUTABLE);

        assertThat(result.bindingOf(reference)).isEmpty();
        assertThat(result.diagnostics())
                .singleElement()
                .satisfies(
                        error -> {
                            assertThat(error).isInstanceOf(CompileError.TypeError.class);
                            assertThat(error.code()).contains(ErrorCode.E2023);
                            assertThat(error.span()).contains(reference.span());
                        });
    }

    @Test
    void reportsDuplicateDeclarationsWithoutReplacingCanonicalSymbol() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Stmt.VarDeclaration first = variable("x", 0);
        final Stmt.VarDeclaration duplicate = variable("x", 20);
        final Expr.Variable reference = reference("x", 30);

        final NameResolutionResult result =
                new NameResolver(table)
                        .resolve(
                                List.of(first, duplicate, expression(reference)),
                                Mutability.IMMUTABLE);

        final Symbol canonical = declared(result, 0);
        assertThat(result.declarations().declarations()).hasSize(2);
        assertThat(result.declarations().declarations().get(1))
                .isInstanceOf(DeclarationResult.AlreadyDeclared.class);
        assertThat(result.bindingOf(reference)).contains(canonical.id());
        assertThat(result.diagnostics())
                .singleElement()
                .satisfies(
                        error -> {
                            assertThat(error).isInstanceOf(CompileError.TypeError.class);
                            assertThat(error.code()).contains(ErrorCode.E2032);
                            assertThat(error.span()).contains(first.span());
                        });
        assertThat(table.symbolsInScope(table.globalScope().id())).containsExactly(canonical);
    }

    @Test
    void resolvesParametersInsideFunctionAndPreservesFunctionScopeHistory() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Expr.Variable parameterReference = reference("value", 30);
        final Parameter parameter = new Parameter("value", new Type.I32(), reference("p", 5).span());
        final Stmt.Function function =
                new Stmt.Function(
                        "identity",
                        List.of(parameter),
                        new Type.I32(),
                        new Stmt.Block(List.of(expression(parameterReference)), SPAN),
                        reference("f", 0).span());

        final NameResolutionResult result =
                new NameResolver(table).resolve(List.of(function), Mutability.IMMUTABLE);

        final Symbol parameterSymbol = declared(result, 1);
        assertThat(result.bindingOf(parameterReference)).contains(parameterSymbol.id());
        final ScopeId bodyScope = result.scopeOf(function.body()).orElseThrow();
        assertThat(result.scopes().find(bodyScope)).isPresent();
        assertThat(result.scopes().find(table.globalScope().id())).isPresent();
        assertThat(table.currentScope()).isEqualTo(table.globalScope());
        table.assertConsistent();
    }

    @Test
    void traversesNestedExpressionFormsAndEveryReferencePosition() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Symbol x = declaredVariable(table, "x");
        final Symbol y = declaredVariable(table, "y");
        final Expr.Variable left = reference("x", 10);
        final Expr.Variable right = reference("y", 12);
        final Expr.Variable unaryReference = reference("x", 14);
        final Expr.Variable arrayElement = reference("y", 16);
        final Expr.ArrayLiteral array = new Expr.ArrayLiteral(List.of(arrayElement), SPAN);
        final Expr composite =
                new Expr.Grouping(
                        new Expr.Binary(
                                new Expr.Unary(UnaryOp.NEGATE, UnaryOpSide.PREFIX, unaryReference, SPAN),
                                BinaryOp.ADD,
                                array,
                                SPAN),
                        SPAN);
        final Expr.Variable target = reference("x", 18);
        final Expr.Variable callee = reference("y", 20);
        final Expr.Variable argument = reference("x", 22);
        final Expr.Variable index = reference("x", 24);
        final Stmt.Expression statement =
                expression(
                        new Expr.Assign(
                                new Expr.ArrayAccess(target, index, SPAN),
                                new Expr.Call(callee, List.of(argument), SPAN),
                                SPAN));
        final Stmt.Expression nested = expression(composite);

        final NameResolutionResult result =
                new NameResolver(table)
                        .resolve(List.of(nested, statement), Mutability.IMMUTABLE);

        assertThat(result.bindingOf(left)).contains(x.id());
        assertThat(result.bindingOf(right)).contains(y.id());
        assertThat(result.bindingOf(unaryReference)).contains(x.id());
        assertThat(result.bindingOf(arrayElement)).contains(y.id());
        assertThat(result.bindingOf(target)).contains(x.id());
        assertThat(result.bindingOf(callee)).contains(y.id());
        assertThat(result.bindingOf(argument)).contains(x.id());
        assertThat(result.bindingOf(index)).contains(x.id());
        assertThat(result.diagnostics()).isEmpty();
    }

    @Test
    void traversesControlFlowBranchesAndLoopExpressions() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Symbol condition = declaredVariable(table, "condition");
        final Symbol value = declaredVariable(table, "value");
        final Expr.Variable ifCondition = reference("condition", 10);
        final Expr.Variable thenReference = reference("value", 20);
        final Expr.Variable elseReference = reference("condition", 30);
        final Expr.Variable whileCondition = reference("condition", 40);
        final Expr.Variable forCondition = reference("condition", 50);
        final Expr.Variable increment = reference("value", 60);
        final Stmt.If nestedElseIf =
                new Stmt.If(
                        elseReference,
                        new Stmt.Block(List.of(expression(thenReference)), SPAN),
                        new ElseBranch.None(),
                        SPAN);
        final Stmt.If conditional =
                new Stmt.If(
                        ifCondition,
                        new Stmt.Block(List.of(expression(thenReference)), SPAN),
                        new ElseBranch.ElseIf(nestedElseIf),
                        SPAN);
        final Stmt.While loop =
                new Stmt.While(
                        whileCondition,
                        new Stmt.Block(List.of(new Stmt.Continue(SPAN), new Stmt.Break(SPAN)), SPAN),
                        SPAN);
        final Stmt.For forLoop =
                new Stmt.For(
                        Optional.of(variable("i", 45)),
                        Optional.of(forCondition),
                        Optional.of(increment),
                        new Stmt.Block(List.of(expression(reference("condition", 70))), SPAN),
                        SPAN);

        final NameResolutionResult result =
                new NameResolver(table)
                        .resolve(
                                List.of(
                                        conditional,
                                        loop,
                                        forLoop,
                                        new Stmt.Return(Optional.empty(), SPAN)),
                                Mutability.IMMUTABLE);

        assertThat(result.bindingOf(ifCondition)).contains(condition.id());
        assertThat(result.bindingOf(thenReference)).contains(value.id());
        assertThat(result.bindingOf(elseReference)).contains(condition.id());
        assertThat(result.bindingOf(whileCondition)).contains(condition.id());
        assertThat(result.bindingOf(forCondition)).contains(condition.id());
        assertThat(result.bindingOf(increment)).contains(value.id());
        assertThat(result.diagnostics()).isEmpty();
    }

    @Test
    void resolvesMainBodyAndKeepsDeclarationOrderInDiagnostics() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Expr.Variable missingFirst = reference("missingFirst", 10);
        final Expr.Variable missingSecond = reference("missingSecond", 20);
        final Stmt.MainFunction main =
                new Stmt.MainFunction(
                        new Stmt.Block(
                                List.of(expression(missingFirst), expression(missingSecond)), SPAN),
                        SPAN);

        final NameResolutionResult result =
                new NameResolver(table).resolve(List.of(main), Mutability.IMMUTABLE);

        assertThat(result.diagnostics()).hasSize(2);
        assertThat(result.diagnostics().get(0).span()).contains(missingFirst.span());
        assertThat(result.diagnostics().get(1).span()).contains(missingSecond.span());
        assertThat(table.currentScope()).isEqualTo(table.globalScope());
        table.assertConsistent();
    }

    @Test
    void exposesImmutableResolutionSnapshots() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Expr.Variable reference = reference("x", 10);
        final NameResolutionResult result =
                new NameResolver(table).resolve(List.of(expression(reference)), Mutability.IMMUTABLE);

        assertThatThrownBy(() -> result.referenceBindings().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.diagnostics().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> result.scopes().scopes().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void validatesResolverAndResultInputs() {
        assertThatThrownBy(() -> new NameResolver(null)).isInstanceOf(NullPointerException.class);
        final DefaultSymbolTable table = new DefaultSymbolTable();
        assertThatThrownBy(() -> new NameResolver(table).resolve(null, Mutability.IMMUTABLE))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new NameResolver(table).resolve(List.of(), null))
                .isInstanceOf(NullPointerException.class);

        final NameResolutionResult empty =
                new NameResolver(table).resolve(List.of(), Mutability.IMMUTABLE);
        assertThat(empty.referenceBindings()).isEmpty();
        assertThat(empty.diagnostics()).isEmpty();
        assertThat(empty.scopeOf(new Stmt.Break(SPAN))).isEmpty();
        assertThatThrownBy(() -> empty.scopeOf(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> empty.bindingOf(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void validatesScopeMappingAndResultCollections() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Stmt statement = expression(reference("x", 10));
        final Scope global = table.globalScope();
        assertThatThrownBy(
                        () -> new ScopeMapping(Map.of(statement, null), List.of(global)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () -> new ScopeMapping(Map.of(), List.of((Scope) null)))
                .isInstanceOf(IllegalArgumentException.class);
        final ScopeMapping mapping = new ScopeMapping(Map.of(statement, global.id()), List.of(global));
        assertThat(mapping.scopeOf(statement)).contains(global.id());
        assertThat(mapping.find(global.id())).contains(global);
        assertThatThrownBy(() -> mapping.scopeOf(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> mapping.find(null)).isInstanceOf(NullPointerException.class);
    }

    private static Stmt.VarDeclaration variable(final String name, final long offset) {
        final Span span = Span.point(SourceLocation.create(1, Math.toIntExact(offset + 1), offset));
        return new Stmt.VarDeclaration(
                List.of(new Stmt.VarBinding(name, Optional.empty())), new Type.I32(), false, span);
    }

    private static Stmt.Expression expression(final Expr expr) {
        return new Stmt.Expression(expr);
    }

    private static Expr.Variable reference(final String name, final long offset) {
        final Span span = Span.point(SourceLocation.create(1, Math.toIntExact(offset + 1), offset));
        return new Expr.Variable(name, span);
    }

    private static Stmt.Function function(
            final String name, final long offset, final Stmt bodyStatement) {
        final Span span = Span.point(SourceLocation.create(1, Math.toIntExact(offset + 1), offset));
        final Stmt.Block body = new Stmt.Block(List.of(bodyStatement), span);
        return new Stmt.Function(name, List.of(), new Type.VoidT(), body, span);
    }

    private static Symbol declaredVariable(final DefaultSymbolTable table, final String name) {
        final DeclarationResult result =
                table.declareVariable(name, new Type.I32(), Mutability.IMMUTABLE, SPAN);
        assertThat(result).isInstanceOf(DeclarationResult.Declared.class);
        return ((DeclarationResult.Declared) result).symbol();
    }

    private static Symbol declared(final NameResolutionResult result, final int index) {
        return ((DeclarationResult.Declared) result.declarations().declarations().get(index))
                .symbol();
    }
}
