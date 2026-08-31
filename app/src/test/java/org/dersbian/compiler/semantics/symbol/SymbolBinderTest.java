package org.dersbian.compiler.semantics.symbol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.Parameter;
import org.dersbian.compiler.syntax.ast.Stmt;
import org.dersbian.compiler.syntax.ast.Type;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.CommentRequired",
    "PMD.AtLeastOneConstructor"
})
class SymbolBinderTest {
    private static final Span SPAN = Span.point(SourceLocation.create(1, 1, 0));

    @Test
    void bindsFunctionParametersAndBodyInNestedScopes() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final SymbolBinder binder = new SymbolBinder(table);
        final Stmt.Function function =
                new Stmt.Function(
                        "f",
                        List.of(new Parameter("x", new Type.I32(), SPAN)),
                        new Type.VoidT(),
                        new Stmt.Block(
                                List.of(
                                        new Stmt.VarDeclaration(
                                                List.of(new Stmt.VarBinding("y", Optional.empty())),
                                                new Type.I32(),
                                                false,
                                                SPAN)),
                                SPAN),
                        SPAN);

        final SymbolBindingResult result = binder.bind(List.of(function), Mutability.MUTABLE);

        assertEquals(3, result.declarations().size(), "Expected 3 declarations to be bound");
        assertInstanceOf(DeclarationResult.Declared.class, result.declarations().get(0));
        assertInstanceOf(DeclarationResult.Declared.class, result.declarations().get(1));
        assertInstanceOf(DeclarationResult.Declared.class, result.declarations().get(2));
        assertEquals(
                table.globalScope(),
                table.currentScope(),
                "Current scope should be reset to global scope after binding function");
        assertTrue(table.lookup("f").isPresent(), "Function 'f' should be present in symbol table");
    }

    @Test
    void bindsForInitializerInsideLoopScope() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final SymbolBinder binder = new SymbolBinder(table);
        final Stmt.For loop =
                new Stmt.For(
                        Optional.of(
                                new Stmt.VarDeclaration(
                                        List.of(new Stmt.VarBinding("i", Optional.empty())),
                                        new Type.I32(),
                                        false,
                                        SPAN)),
                        Optional.empty(),
                        Optional.empty(),
                        new Stmt.Block(List.of(), SPAN),
                        SPAN);

        binder.bind(List.of(loop), Mutability.IMMUTABLE);

        assertTrue(
                table.lookup("i").isEmpty(),
                "Loop variable 'i' should not be present in global scope");
        assertEquals(
                table.globalScope(),
                table.currentScope(),
                "Current scope should be reset to global scope after binding loop");
    }
}
