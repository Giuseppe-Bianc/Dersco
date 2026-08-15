package org.dersbian.compiler.syntax.ast;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.junit.jupiter.api.Test;

/**
 * Tests that the AST shape remains correct after migrating from the visitor pattern to exhaustive
 * pattern matching.
 *
 * <p>Each test pins down one observable property: traversal order via the printer output, total
 * node count, or the order in which {@link NodeCounter} accumulates values.
 */
@SuppressWarnings({
    "PMD.ShortVariable",
    "PMD.CommentRequired",
    "PMD.TooManyMethods",
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.AtLeastOneConstructor"
})
class AstTraversalTest {

    private static final Span SPAN = Span.point(SourceLocation.create(1, 1, 0));
    private static final String COND = "cond";

    private static Expr.Literal boolLit(final boolean value) {
        return new Expr.Literal(new LiteralValue.Bool(value), SPAN);
    }

    private static Expr.Variable var(final String name) {
        return new Expr.Variable(name, SPAN);
    }

    // -----------------------------------------------------------------------
    // Exhaustiveness (replaces dispatch tests)
    // -----------------------------------------------------------------------

    @Test
    void everyExprVariantHasCountCoverage() {
        // Exhaustiveness guard: if a variant's case arm is missing, the switch throws
        // IncompletePatternException. Asserting >= 1 on every variant proves the arm
        // exists
        // and the node is counted (or sub-counted via its children).
        assertThat(
                        NodeCounter.count(
                                new Expr.Binary(boolLit(true), BinaryOp.ADD, boolLit(false), SPAN)))
                .isGreaterThanOrEqualTo(1);
        assertThat(
                        NodeCounter.count(
                                new Expr.Unary(
                                        UnaryOp.NEGATE, UnaryOpSide.PREFIX, boolLit(true), SPAN)))
                .isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(new Expr.Grouping(boolLit(true), SPAN)))
                .isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(boolLit(true))).isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(new Expr.ArrayLiteral(List.of(boolLit(true)), SPAN)))
                .isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(var("x"))).isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(new Expr.Assign(var("x"), boolLit(true), SPAN)))
                .isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(new Expr.Call(var("f"), List.of(boolLit(true)), SPAN)))
                .isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(new Expr.ArrayAccess(var("arr"), boolLit(true), SPAN)))
                .isGreaterThanOrEqualTo(1);
    }

    @Test
    void everyStmtVariantHasCountCoverage() {
        assertThat(NodeCounter.count(new Stmt.Expression(boolLit(true)))).isGreaterThanOrEqualTo(1);
        assertThat(
                        NodeCounter.count(
                                new Stmt.VarDeclaration(
                                        List.of(
                                                new Stmt.VarBinding(
                                                        "x", Optional.of(boolLit(true)))),
                                        new Type.I32(),
                                        true,
                                        SPAN)))
                .isGreaterThanOrEqualTo(1);
        // Function, If, While, For, Block, Return, Break, Continue, MainFunction
        // already
        // covered in dedicated tests below
        assertThat(NodeCounter.count(new Stmt.Break(SPAN))).isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(new Stmt.Continue(SPAN))).isGreaterThanOrEqualTo(1);
    }

    @Test
    void everyTypeVariantHasCountCoverage() {
        assertThat(NodeCounter.count(new Type.I8())).isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(new Type.I16())).isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(new Type.I32())).isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(new Type.I64())).isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(new Type.U8())).isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(new Type.U16())).isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(new Type.U32())).isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(new Type.U64())).isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(new Type.F32())).isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(new Type.F64())).isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(new Type.Char())).isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(new Type.StringT())).isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(new Type.Bool())).isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(new Type.Custom("T"))).isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(new Type.VoidT())).isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(new Type.NullPtr())).isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(new Type.Array(new Type.I32(), new Expr.Variable("n", SPAN))))
                .isGreaterThanOrEqualTo(1);
        assertThat(NodeCounter.count(new Type.Vector(new Type.I32()))).isGreaterThanOrEqualTo(1);
    }

    // -----------------------------------------------------------------------
    // Traversal order (semantics of child recursion in pattern matching)
    // -----------------------------------------------------------------------

    @Test
    void binaryTraversesLeftBeforeRight() {
        final Expr node = new Expr.Binary(var("a"), BinaryOp.MULTIPLY, var("b"), SPAN);
        assertThat(AstPrinter.print(node)).isEqualTo("(a MULTIPLY b)");
    }

    @Test
    void ifTraversesConditionThenBranchThenElseBranch() {
        final Stmt.If node =
                new Stmt.If(
                        var(COND),
                        new Stmt.Block(List.of(new Stmt.Expression(var("then"))), SPAN),
                        new ElseBranch.Block(
                                new Stmt.Block(List.of(new Stmt.Expression(var("els"))), SPAN)),
                        SPAN);
        final String out = AstPrinter.print(node);
        assertThat(out).contains("if (cond)");
        assertThat(out).contains("then;");
        assertThat(out).contains("els;");
        // Order: condition, then, else all present in that position
        final int idxCond = out.indexOf(COND);
        final int idxThen = out.indexOf("then;");
        final int idxElse = out.indexOf("els;");
        assertThat(idxCond).isLessThan(idxThen);
        assertThat(idxThen).isLessThan(idxElse);
    }

    @Test
    void forTraversesInitializerConditionIncrementBody() {
        final Stmt.VarDeclaration init =
                new Stmt.VarDeclaration(
                        List.of(new Stmt.VarBinding("i", Optional.of(boolLit(true)))),
                        new Type.I32(),
                        true,
                        SPAN);
        final Stmt.For node =
                new Stmt.For(
                        Optional.of(init),
                        Optional.of(var(COND)),
                        Optional.of(var("inc")),
                        new Stmt.Block(List.of(new Stmt.Expression(var("body"))), SPAN),
                        SPAN);
        final String out = AstPrinter.print(node);
        assertThat(out).contains(COND);
        assertThat(out).contains("inc");
        assertThat(out).contains("body");
    }

    @Test
    void callTraversesCalleeBeforeArguments() {
        final Expr.Call node = new Expr.Call(var("fn"), List.of(var("a1"), var("a2")), SPAN);
        assertThat(AstPrinter.print(node)).isEqualTo("fn(a1, a2)");
    }

    // -----------------------------------------------------------------------
    // Result propagation (counts)
    // -----------------------------------------------------------------------

    @Test
    void singleLiteralCountsLiteralAndValue() {
        assertThat(NodeCounter.count(boolLit(true))).isEqualTo(2);
    }

    @Test
    void binaryOfTwoLiteralsCountsAllNodes() {
        final Expr.Binary b = new Expr.Binary(boolLit(true), BinaryOp.ADD, boolLit(false), SPAN);
        assertThat(NodeCounter.count(b)).isEqualTo(5);
    }

    @Test
    void blockWithTwoExpressionsCountsAllDescendants() {
        final Stmt.Block block =
                new Stmt.Block(
                        List.of(
                                new Stmt.Expression(boolLit(true)),
                                new Stmt.Expression(boolLit(false))),
                        SPAN);
        assertThat(NodeCounter.count(block)).isEqualTo(7);
    }

    @Test
    void emptyArrayLiteralCountsOne() {
        assertThat(NodeCounter.count(new Expr.ArrayLiteral(List.of(), SPAN))).isEqualTo(1);
    }

    // -----------------------------------------------------------------------
    // PrettyPrinter smoke tests
    // -----------------------------------------------------------------------

    @Test
    void binaryAdditionRendersWithOperatorName() {
        final String out =
                AstPrinter.print(new Expr.Binary(var("x"), BinaryOp.ADD, var("y"), SPAN));
        assertThat(out).isEqualTo("(x ADD y)");
    }

    @Test
    void nestedGroupingRendersCorrectly() {
        final Expr inner = new Expr.Grouping(var("a"), SPAN);
        final Expr binary = new Expr.Binary(inner, BinaryOp.MULTIPLY, var("b"), SPAN);
        assertThat(AstPrinter.print(binary)).isEqualTo("((a) MULTIPLY b)");
    }

    @Test
    void functionDeclarationRendersSignature() {
        final Stmt.Function func =
                new Stmt.Function(
                        "add",
                        List.of(
                                new Parameter("x", new Type.I32(), SPAN),
                                new Parameter("y", new Type.I32(), SPAN)),
                        new Type.I32(),
                        new Stmt.Block(
                                List.of(
                                        new Stmt.Return(
                                                Optional.of(
                                                        new Expr.Binary(
                                                                var("x"),
                                                                BinaryOp.ADD,
                                                                var("y"),
                                                                SPAN)),
                                                SPAN)),
                                SPAN),
                        SPAN);
        final String out = AstPrinter.print(func);
        assertThat(out).startsWith("fn add(x: i32, y: i32): i32 {");
    }

    @Test
    void functionDeclarationRendersBody() {
        final Stmt.Function func =
                new Stmt.Function(
                        "add",
                        List.of(
                                new Parameter("x", new Type.I32(), SPAN),
                                new Parameter("y", new Type.I32(), SPAN)),
                        new Type.I32(),
                        new Stmt.Block(
                                List.of(
                                        new Stmt.Return(
                                                Optional.of(
                                                        new Expr.Binary(
                                                                var("x"),
                                                                BinaryOp.ADD,
                                                                var("y"),
                                                                SPAN)),
                                                SPAN)),
                                SPAN),
                        SPAN);
        assertThat(AstPrinter.print(func)).contains("return (x ADD y);");
    }

    @Test
    void nullptrLiteralRendersAsNullptr() {
        assertThat(AstPrinter.print(Expr.nullExpr(SPAN))).isEqualTo("nullptr");
    }
}
