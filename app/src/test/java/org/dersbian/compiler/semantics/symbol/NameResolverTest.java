package org.dersbian.compiler.semantics.symbol;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.error.ErrorCode;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.Expr;
import org.dersbian.compiler.syntax.ast.Stmt;
import org.dersbian.compiler.syntax.ast.Type;
import org.junit.jupiter.api.Test;

/** Normative tests for lexical name resolution. */
class NameResolverTest {
    private static final Span SPAN = Span.point(SourceLocation.create(1, 1, 0));

    @Test
    void resolvesGlobalAndShadowedReferencesByStableIdentity() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Stmt.VarDeclaration global = variable("x", 0);
        final Expr.Variable outerReference = reference("x", 20);
        final Stmt.Block block = new Stmt.Block(
                List.of(variable("x", 30), expression(reference("x", 50))), SPAN);
        final NameResolutionResult result = new NameResolver(table).resolve(
                List.of(global, expression(outerReference), block), Mutability.IMMUTABLE);

        final Symbol globalSymbol = declared(result, 0);
        final Symbol innerSymbol = declared(result, 1);
        assertThat(result.bindingOf(outerReference)).contains(globalSymbol.id());
        final Expr.Variable innerReference = (Expr.Variable) ((Stmt.Expression) block.statements().get(1)).expr();
        assertThat(result.bindingOf(innerReference)).contains(innerSymbol.id());
        assertThat(globalSymbol.id()).isNotEqualTo(innerSymbol.id());
        table.assertConsistent();
    }

    @Test
    void rejectsForwardReferenceWithoutFallingBackToOuterScope() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        declaredVariable(table, "x");
        final Stmt.Block block = new Stmt.Block(
                List.of(expression(reference("x", 10)), variable("x", 20)), SPAN);
        final NameResolutionResult result = new NameResolver(table).resolve(
                List.of(block), Mutability.IMMUTABLE);

        final Expr.Variable reference = (Expr.Variable) ((Stmt.Expression) block.statements().get(0)).expr();
        assertThat(result.bindingOf(reference)).isEmpty();
        assertThat(result.diagnostics()).singleElement().satisfies(error -> {
            assertThat(error).isInstanceOf(CompileError.NameResolutionError.class);
            assertThat(error.code()).contains(ErrorCode.E2023);
            assertThat(error.span()).contains(reference.span());
        });
    }

    @Test
    void supportsDirectFunctionRecursionButRejectsIndirectForwardReference() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Expr.Variable recursiveReference = reference("a", 30);
        final Stmt.Function a = function("a", 0,
                expression(new Expr.Call(recursiveReference, List.of(), SPAN)));
        final Expr.Variable forwardReference = reference("b", 10);
        final Stmt.Function b = function("b", 40,
                expression(new Expr.Call(reference("a", 60), List.of(), SPAN)));

        final NameResolutionResult result = new NameResolver(table).resolve(
                List.of(expression(new Expr.Call(forwardReference, List.of(), SPAN)), a, b),
                Mutability.IMMUTABLE);

        assertThat(result.bindingOf(recursiveReference)).isNotEmpty();
        assertThat(result.bindingOf(forwardReference)).isEmpty();
        assertThat(result.diagnostics()).hasSize(1);
    }

    @Test
    void resolvesAllExpressionReferencePositions() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Symbol x = declaredVariable(table, "x");
        final Symbol y = declaredVariable(table, "y");
        final Expr.Variable target = reference("x", 10);
        final Expr.Variable value = reference("y", 12);
        final Expr.Variable index = reference("x", 14);
        final Expr.ArrayAccess access = new Expr.ArrayAccess(target, index, SPAN);
        final Stmt.Expression statement = expression(new Expr.Assign(access, value, SPAN));

        final NameResolutionResult result = new NameResolver(table).resolve(
                List.of(statement), Mutability.IMMUTABLE);

        assertThat(result.bindingOf(target)).contains(x.id());
        assertThat(result.bindingOf(index)).contains(x.id());
        assertThat(result.bindingOf(value)).contains(y.id());
        assertThat(result.diagnostics()).isEmpty();
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

    private static Stmt.Function function(final String name, final long offset, final Stmt bodyStatement) {
        final Span span = Span.point(SourceLocation.create(1, Math.toIntExact(offset + 1), offset));
        final Stmt.Block body = new Stmt.Block(List.of(bodyStatement), span);
        return new Stmt.Function(name, List.of(), new Type.VoidT(), body, span);
    }

    private static Symbol declaredVariable(final DefaultSymbolTable table, final String name) {
        final DeclarationResult result = table.declareVariable(name, new Type.I32(), Mutability.IMMUTABLE, SPAN);
        assertThat(result).isInstanceOf(DeclarationResult.Declared.class);
        return ((DeclarationResult.Declared) result).symbol();
    }

    private static Symbol declared(final NameResolutionResult result, final int index) {
        return ((DeclarationResult.Declared) result.declarations().declarations().get(index)).symbol();
    }
}
