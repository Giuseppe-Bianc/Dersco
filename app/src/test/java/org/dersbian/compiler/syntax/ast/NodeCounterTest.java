package org.dersbian.compiler.syntax.ast;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import lombok.NoArgsConstructor;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NodeCounter}.
 *
 * <p>Counts every distinct AST node reached in a depth-first traversal. Replaces the assertions
 * that previously relied on the visitor-based {@code NodeCounterVisitor}.
 */
@SuppressWarnings({"PMD.ShortVariable", "PMD.CommentRequired"})
@NoArgsConstructor
class NodeCounterTest {

    private static final Span SPAN = Span.point(SourceLocation.create(1, 1, 0));

    @Test
    void singleLiteralCountsLiteralAndValue() {
        final Expr node = new Expr.Literal(new LiteralValue.Bool(true), SPAN);
        assertThat(NodeCounter.count(node)).isEqualTo(2);
    }

    @Test
    void binaryOfTwoLiteralsCountsAllNodes() {
        final Expr b =
                new Expr.Binary(
                        new Expr.Literal(new LiteralValue.Bool(true), SPAN),
                        BinaryOp.ADD,
                        new Expr.Literal(new LiteralValue.Bool(false), SPAN),
                        SPAN);
        assertThat(NodeCounter.count(b)).isEqualTo(5);
    }

    @Test
    void blockWithTwoExpressionsCountsAllDescendants() {
        final Stmt.Block block =
                new Stmt.Block(
                        List.of(
                                new Stmt.Expression(
                                        new Expr.Literal(new LiteralValue.Bool(true), SPAN)),
                                new Stmt.Expression(
                                        new Expr.Literal(new LiteralValue.Bool(false), SPAN))),
                        SPAN);
        assertThat(NodeCounter.count(block)).isEqualTo(7);
    }

    @Test
    void emptyArrayLiteralCountsOne() {
        final Expr node = new Expr.ArrayLiteral(List.of(), SPAN);
        assertThat(NodeCounter.count(node)).isEqualTo(1);
    }

    @Test
    void ifWithElseBlockCountsConditionAndBranches() {
        final Stmt.If node =
                new Stmt.If(
                        new Expr.Variable("c", SPAN),
                        new Stmt.Block(List.of(new Stmt.Return(Optional.empty(), SPAN)), SPAN),
                        new ElseBranch.Block(
                                new Stmt.Block(
                                        List.of(new Stmt.Return(Optional.empty(), SPAN)), SPAN)),
                        SPAN);
        // var(1) + thenBlock(1) + return(1) + elseBlock(1) + return(1) + if(1) = 6
        assertThat(NodeCounter.count(node)).isEqualTo(6);
    }

    @Test
    void leafExprCountsOne() {
        assertThat(NodeCounter.count(new Expr.Variable("x", SPAN))).isEqualTo(1);
    }

    @Test
    void leafTypeCountsOne() {
        assertThat(NodeCounter.count(new Type.I32())).isEqualTo(1);
    }

    @Test
    void leafStmtCountsOne() {
        assertThat(NodeCounter.count(new Stmt.Break(SPAN))).isEqualTo(1);
    }
}
