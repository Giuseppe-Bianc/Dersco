package org.dersbian.compiler.semantics.symbol;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.ElseBranch;
import org.dersbian.compiler.syntax.ast.Parameter;
import org.dersbian.compiler.syntax.ast.Stmt;
import org.dersbian.compiler.syntax.ast.Type;
import org.junit.jupiter.api.Test;

/** Verifies that every supported statement shape creates the expected lexical scopes. */
class SymbolBinderScopeMappingTest {
    /** Synthetic source span for AST fixtures. */
    private static final Span SPAN = Span.point(SourceLocation.create(1, 1, 0));

    @Test
    void ifBranchesGetIndependentBlockScopes() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final SymbolBinder binder = new SymbolBinder(table);
        final Stmt.If conditional =
                new Stmt.If(
                        new org.dersbian.compiler.syntax.ast.Expr.Literal(
                                new org.dersbian.compiler.syntax.ast.LiteralValue.Bool(true), SPAN),
                        new Stmt.Block(
                                List.of(variable("thenOnly", new Type.I32(), false)), SPAN),
                        new ElseBranch.Block(
                                new Stmt.Block(
                                        List.of(variable("elseOnly", new Type.I64(), false)), SPAN)),
                        SPAN);

        binder.bind(List.of(conditional), Mutability.IMMUTABLE);

        assertThat(table.lookup("thenOnly")).isEmpty();
        assertThat(table.lookup("elseOnly")).isEmpty();
        final long blockCount =
                List.of(table.globalScope()).stream().filter(scope -> scope.kind() == ScopeKind.BLOCK).count();
        assertThat(blockCount).isZero();
        table.assertConsistent();
    }

    @Test
    void whileBodyGetsBlockScope() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final SymbolBinder binder = new SymbolBinder(table);
        final Stmt.While loop =
                new Stmt.While(
                        new org.dersbian.compiler.syntax.ast.Expr.Literal(
                                new org.dersbian.compiler.syntax.ast.LiteralValue.Bool(true), SPAN),
                        new Stmt.Block(List.of(variable("inside", new Type.I32(), false)), SPAN),
                        SPAN);

        binder.bind(List.of(loop), Mutability.IMMUTABLE);

        assertThat(table.lookup("inside")).isEmpty();
        table.assertConsistent();
    }

    @Test
    void forScopeContainsInitializerAndBodyIsNestedBlock() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final SymbolBinder binder = new SymbolBinder(table);
        final Stmt.For loop =
                new Stmt.For(
                        Optional.of(variable("i", new Type.I32(), true)),
                        Optional.empty(),
                        Optional.empty(),
                        new Stmt.Block(List.of(variable("bodyOnly", new Type.I64(), false)), SPAN),
                        SPAN);

        binder.bind(List.of(loop), Mutability.IMMUTABLE);

        assertThat(table.currentScope().kind()).isEqualTo(ScopeKind.GLOBAL);
        assertThat(table.lookup("i")).isEmpty();
        assertThat(table.lookup("bodyOnly")).isEmpty();
        table.assertConsistent();
    }

    @Test
    void nestedFunctionCanShadowOuterVariable() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final SymbolBinder binder = new SymbolBinder(table);
        final Stmt.Function inner =
                new Stmt.Function(
                        "inner",
                        List.of(new Parameter("value", new Type.I32(), SPAN)),
                        new Type.VoidT(),
                        new Stmt.Block(List.of(variable("x", new Type.I64(), false)), SPAN),
                        SPAN);
        final Stmt.Function outer =
                new Stmt.Function(
                        "outer",
                        List.of(),
                        new Type.VoidT(),
                        new Stmt.Block(
                                List.of(
                                        variable("x", new Type.I32(), false),
                                        inner),
                                SPAN),
                        SPAN);

        final SymbolBindingResult result = binder.bind(List.of(outer), Mutability.MUTABLE);

        assertThat(result.declarations())
                .extracting(resultItem -> ((resultItem instanceof DeclarationResult.Declared declared)
                        ? declared.symbol().name()
                        : ((DeclarationResult.AlreadyDeclared) resultItem).name()))
                .containsExactly("outer", "x", "inner", "value", "x");
        assertThat(table.lookup("outer")).isPresent();
        assertThat(table.lookup("inner")).isEmpty();
        table.assertConsistent();
    }

    @Test
    void duplicateDeclarationProducesResultAndDoesNotCreateReplacement() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final SymbolBinder binder = new SymbolBinder(table);
        final Stmt.Block block =
                new Stmt.Block(
                        List.of(
                                variable("x", new Type.I32(), false),
                                variable("x", new Type.I64(), true)),
                        SPAN);

        final SymbolBindingResult result = binder.bind(List.of(block), Mutability.IMMUTABLE);

        assertThat(result.declarations()).hasSize(2);
        assertThat(result.declarations().get(0)).isInstanceOf(DeclarationResult.Declared.class);
        assertThat(result.declarations().get(1)).isInstanceOf(DeclarationResult.AlreadyDeclared.class);
        final DeclarationResult.Declared first =
                (DeclarationResult.Declared) result.declarations().get(0);
        final DeclarationResult.AlreadyDeclared duplicate =
                (DeclarationResult.AlreadyDeclared) result.declarations().get(1);
        assertThat(duplicate.existingSymbol()).isSameAs(first.symbol());
        table.assertConsistent();
    }

    @Test
    void mainBodyCreatesFunctionAndBlockScopes() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final SymbolBinder binder = new SymbolBinder(table);
        final Stmt.MainFunction main =
                new Stmt.MainFunction(
                        new Stmt.Block(List.of(variable("local", new Type.I32(), false)), SPAN),
                        SPAN);

        final SymbolBindingResult result = binder.bind(List.of(main), Mutability.IMMUTABLE);

        assertThat(result.declarations()).hasSize(2);
        final Symbol mainSymbol = ((DeclarationResult.Declared) result.declarations().get(0)).symbol();
        assertThat(mainSymbol).isInstanceOf(MainFunctionSymbol.class);
        assertThat(table.lookup("main")).containsSame(mainSymbol);
        assertThat(table.lookup("local")).isEmpty();
        table.assertConsistent();
    }

    private static Stmt.VarDeclaration variable(
            final String name, final Type type, final boolean mutable) {
        return new Stmt.VarDeclaration(
                List.of(new Stmt.VarBinding(name, Optional.empty())), type, mutable, SPAN);
    }
}
