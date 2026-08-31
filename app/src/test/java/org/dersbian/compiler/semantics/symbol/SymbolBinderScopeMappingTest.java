package org.dersbian.compiler.semantics.symbol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        assertTrue(table.lookup("thenOnly").isEmpty(), "then branch symbol must not escape its block");
        assertTrue(table.lookup("elseOnly").isEmpty(), "else branch symbol must not escape its block");
        assertEquals(ScopeKind.GLOBAL, table.currentScope().kind(), "binding must restore global scope");
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

        assertTrue(table.lookup("inside").isEmpty(), "while body symbol must not escape its block");
        assertEquals(ScopeKind.GLOBAL, table.currentScope().kind(), "binding must restore global scope");
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

        assertEquals(ScopeKind.GLOBAL, table.currentScope().kind(), "binding must restore global scope");
        assertTrue(table.lookup("i").isEmpty(), "loop initializer must not escape the loop");
        assertTrue(table.lookup("bodyOnly").isEmpty(), "loop body symbol must not escape its block");
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

        assertEquals(5, result.declarations().size(), "all declarations must be retained in source order");
        assertTrue(table.lookup("outer").isPresent(), "outer function must remain globally visible");
        assertTrue(table.lookup("inner").isEmpty(), "nested function must not escape its enclosing function");
        assertEquals(ScopeKind.GLOBAL, table.currentScope().kind(), "binding must restore global scope");
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

        assertEquals(2, result.declarations().size(), "both declaration attempts must be represented");
        assertInstanceOf(DeclarationResult.Declared.class, result.declarations().get(0));
        assertInstanceOf(DeclarationResult.AlreadyDeclared.class, result.declarations().get(1));
        final DeclarationResult.Declared first =
                (DeclarationResult.Declared) result.declarations().get(0);
        final DeclarationResult.AlreadyDeclared duplicate =
                (DeclarationResult.AlreadyDeclared) result.declarations().get(1);
        assertTrue(duplicate.existingSymbol() == first.symbol(), "duplicate must preserve the original symbol");
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

        assertEquals(2, result.declarations().size(), "main and its local declaration must be bound");
        final Symbol mainSymbol = ((DeclarationResult.Declared) result.declarations().get(0)).symbol();
        assertInstanceOf(MainFunctionSymbol.class, mainSymbol);
        assertTrue(table.lookup("main").orElseThrow() == mainSymbol, "main lookup must return the registered symbol");
        assertTrue(table.lookup("local").isEmpty(), "main body local must not escape its block");
    }

    private static Stmt.VarDeclaration variable(
            final String name, final Type type, final boolean mutable) {
        return new Stmt.VarDeclaration(
                List.of(new Stmt.VarBinding(name, Optional.empty())), type, mutable, SPAN);
    }
}
