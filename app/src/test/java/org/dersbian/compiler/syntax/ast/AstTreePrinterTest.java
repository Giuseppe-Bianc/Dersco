package org.dersbian.compiler.syntax.ast;

import static org.junit.jupiter.api.Assertions.assertTrue;

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
class AstTreePrinterTest {

    private static final Span SPAN = Span.point(SourceLocation.create(1, 1, 0));

    @Test
    void printsExpressionVariantsAndChildren() {
        final Expr expression =
                new Expr.Call(
                        new Expr.Variable("compute", SPAN),
                        List.of(
                                new Expr.ArrayAccess(
                                        new Expr.Variable("values", SPAN),
                                        new Expr.Literal(new LiteralValue.Bool(true), SPAN),
                                        SPAN),
                                new Expr.ArrayLiteral(List.of(), SPAN)),
                        SPAN);

        final String output = AstTreePrinter.prettyPrint(expression);

        assertTrue(output.contains("Function Call"));
        assertTrue(output.contains("Array Access"));
        assertTrue(output.contains("Array Literal"));
        assertTrue(output.contains("Elements: (empty)"));
    }

    @Test
    void printsAllStatementBranchesAndOptionalParts() {
        final Stmt.If conditional =
                new Stmt.If(
                        new Expr.Variable("condition", SPAN),
                        new Stmt.Block(List.of(), SPAN),
                        new ElseBranch.Block(
                                new Stmt.Block(
                                        List.of(new Stmt.Continue(SPAN), new Stmt.Break(SPAN)),
                                        SPAN)),
                        SPAN);
        final Stmt.Function function =
                new Stmt.Function(
                        "work",
                        List.of(new Parameter("value", new Type.I32(), SPAN)),
                        new Type.VoidT(),
                        new Stmt.Block(List.of(new Stmt.Return(Optional.empty(), SPAN)), SPAN),
                        SPAN);
        final Stmt.For loop =
                new Stmt.For(
                        Optional.empty(),
                        Optional.empty(),
                        Optional.empty(),
                        new Stmt.Block(List.of(), SPAN),
                        SPAN);

        assertTrue(AstTreePrinter.prettyPrintStmt(conditional).contains("Then: (empty)"));
        assertTrue(AstTreePrinter.prettyPrintStmt(function).contains("Parameter 'value'"));
        assertTrue(AstTreePrinter.prettyPrintStmt(loop).contains("Body:"));
        assertTrue(
                AstTreePrinter.prettyPrintStmt(
                                new Stmt.MainFunction(new Stmt.Block(List.of(), SPAN), SPAN))
                        .contains("MainFunction"));
    }
}
