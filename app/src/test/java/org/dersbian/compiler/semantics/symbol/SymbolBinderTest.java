package org.dersbian.compiler.semantics.symbol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.Parameter;
import org.dersbian.compiler.syntax.ast.Stmt;
import org.dersbian.compiler.syntax.ast.Type;
import org.junit.jupiter.api.Test;

class SymbolBinderTest {
    private static final Span SPAN = new Span(1, 1, 1, 1);

    @Test
    void bindsFunctionParametersAndBodyInNestedScopes() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final SymbolBinder binder = new SymbolBinder(table);
        final Stmt.Function function = new Stmt.Function(
                "f",
                List.of(new Parameter("x", new Type.IntT(), SPAN)),
                new Type.VoidT(),
                new Stmt.Block(List.of(new Stmt.VarDeclaration(
                        List.of(new Stmt.VarBinding("y", Optional.empty())),
                        new Type.IntT(),
                        false,
                        SPAN)), SPAN),
                SPAN);

        final SymbolBindingResult result = binder.bind(List.of(function), Mutability.MUTABLE);

        assertEquals(3, result.declarations().size());
        assertInstanceOf(DeclarationResult.Declared.class, result.declarations().get(0));
        assertInstanceOf(DeclarationResult.Declared.class, result.declarations().get(1));
        assertInstanceOf(DeclarationResult.Declared.class, result.declarations().get(2));
        assertEquals(table.globalScope(), table.currentScope());
        assertTrue(table.lookup("f").isPresent());
    }

    @Test
    void bindsForInitializerInsideLoopScope() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final SymbolBinder binder = new SymbolBinder(table);
        final Stmt.For loop = new Stmt.For(
                Optional.of(new Stmt.VarDeclaration(
                        List.of(new Stmt.VarBinding("i", Optional.empty())),
                        new Type.IntT(),
                        false,
                        SPAN)),
                Optional.empty(),
                Optional.empty(),
                new Stmt.Block(List.of(), SPAN),
                SPAN);

        binder.bind(List.of(loop), Mutability.IMMUTABLE);

        assertTrue(table.lookup("i").isEmpty());
        assertEquals(table.globalScope(), table.currentScope());
    }
}
