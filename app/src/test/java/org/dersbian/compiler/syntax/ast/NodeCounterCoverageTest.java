package org.dersbian.compiler.syntax.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
class NodeCounterCoverageTest {

    private static final Span SPAN = Span.point(SourceLocation.create(1, 1, 0));

    @Test
    void countsCallArrayAccessAndArrayLiteralDescendants() {
        final Expr expression =
                new Expr.Call(
                        new Expr.Variable("fn", SPAN),
                        List.of(
                                new Expr.ArrayAccess(
                                        new Expr.ArrayLiteral(
                                                List.of(new Expr.Variable("x", SPAN)), SPAN),
                                        new Expr.Variable("i", SPAN),
                                        SPAN)),
                        SPAN);

        assertEquals(6, NodeCounter.count(expression));
    }

    @Test
    void countsFunctionParametersReturnTypeAndBody() {
        final Stmt.Function function =
                new Stmt.Function(
                        "f",
                        List.of(new Parameter("value", new Type.Vector(new Type.I32()), SPAN)),
                        new Type.Array(new Type.I64(), new Expr.Variable("size", SPAN)),
                        new Stmt.Block(
                                List.of(
                                        new Stmt.Return(
                                                Optional.of(new Expr.Variable("value", SPAN)),
                                                SPAN)),
                                SPAN),
                        SPAN);

        assertEquals(9, NodeCounter.count(function));
    }

    @Test
    void countsForOptionalClausesAndMainBody() {
        final Stmt.For loop =
                new Stmt.For(
                        Optional.of(new Stmt.Break(SPAN)),
                        Optional.of(new Expr.Variable("condition", SPAN)),
                        Optional.of(
                                new Expr.Unary(
                                        UnaryOp.INCREMENT,
                                        UnaryOpSide.POSTFIX,
                                        new Expr.Variable("i", SPAN),
                                        SPAN)),
                        new Stmt.Block(List.of(new Stmt.Continue(SPAN)), SPAN),
                        SPAN);
        final Stmt.MainFunction main =
                new Stmt.MainFunction(new Stmt.Block(List.of(loop), SPAN), SPAN);

        assertEquals(9, NodeCounter.count(main));
    }

    @Test
    void countsAllElseBranchShapes() {
        final Stmt.If inner =
                new Stmt.If(
                        new Expr.Variable("inner", SPAN),
                        new Stmt.Block(List.of(), SPAN),
                        new ElseBranch.None(),
                        SPAN);
        final Stmt.If withElseIf =
                new Stmt.If(
                        new Expr.Variable("outer", SPAN),
                        new Stmt.Block(List.of(), SPAN),
                        new ElseBranch.ElseIf(inner),
                        SPAN);
        final Stmt.If withElseBlock =
                new Stmt.If(
                        new Expr.Variable("condition", SPAN),
                        new Stmt.Block(List.of(), SPAN),
                        new ElseBranch.Block(new Stmt.Block(List.of(new Stmt.Break(SPAN)), SPAN)),
                        SPAN);

        assertEquals(6, NodeCounter.count(withElseIf));
        assertEquals(5, NodeCounter.count(withElseBlock));
    }
}
