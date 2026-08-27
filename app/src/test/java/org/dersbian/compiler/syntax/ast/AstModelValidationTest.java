package org.dersbian.compiler.syntax.ast;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.UnitTestAssertionsShouldIncludeMessage"
})
class AstModelValidationTest {

    private static final Span SPAN = Span.point(SourceLocation.create(1, 1, 0));
    private static final Expr VARIABLE = new Expr.Variable("x", SPAN);
    private static final Type TYPE = new Type.I32();

    @Test
    void expressionCollectionsAreDefensivelyCopied() {
        final List<Expr> elements = new ArrayList<>(List.of(VARIABLE));
        final Expr.ArrayLiteral array = new Expr.ArrayLiteral(elements, SPAN);
        final Expr.Call call = new Expr.Call(VARIABLE, elements, SPAN);
        elements.clear();

        assertTrue(array.elements().contains(VARIABLE));
        assertTrue(call.arguments().contains(VARIABLE));
        assertThrows(UnsupportedOperationException.class, () -> array.elements().add(VARIABLE));
    }

    @Test
    void statementCollectionsAreDefensivelyCopied() {
        final Stmt.VarBinding binding = new Stmt.VarBinding("x", Optional.of(VARIABLE));
        final List<Stmt.VarBinding> bindings = new ArrayList<>(List.of(binding));
        final Stmt.VarDeclaration declaration = new Stmt.VarDeclaration(bindings, TYPE, true, SPAN);
        bindings.clear();

        assertTrue(declaration.bindings().contains(binding));
        assertThrows(
                UnsupportedOperationException.class, () -> declaration.bindings().add(binding));
    }

    @Test
    void constructorsRejectNullRequiredFields() {
        assertThrows(NullPointerException.class, () -> new Expr.Variable(null, SPAN));
        assertThrows(NullPointerException.class, () -> new Expr.Variable("x", null));
        assertThrows(NullPointerException.class, () -> new Type.Custom(null));
        assertThrows(NullPointerException.class, () -> new Type.Array(null, VARIABLE));
        assertThrows(NullPointerException.class, () -> new Type.Array(TYPE, null));
        assertThrows(NullPointerException.class, () -> new Parameter(null, TYPE, SPAN));
        assertThrows(NullPointerException.class, () -> new ElseBranch.Block(null));
        assertThrows(NullPointerException.class, () -> new ElseBranch.ElseIf(null));
    }

    @Test
    void optionalFieldsMustContainOptionalInstances() {
        assertThrows(NullPointerException.class, () -> new Stmt.VarBinding("x", null));
        assertThrows(NullPointerException.class, () -> new Stmt.Return(null, SPAN));
        assertThrows(
                NullPointerException.class,
                () -> new Stmt.For(null, Optional.empty(), Optional.empty(), block(), SPAN));
        assertThrows(
                NullPointerException.class,
                () -> new Stmt.For(Optional.empty(), null, Optional.empty(), block(), SPAN));
    }

    private static Stmt.Block block() {
        return new Stmt.Block(List.of(), SPAN);
    }
}
