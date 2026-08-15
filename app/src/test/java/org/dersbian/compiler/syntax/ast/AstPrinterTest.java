package org.dersbian.compiler.syntax.ast;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.lexer.token.number.INumber;
import org.junit.jupiter.api.Test;

/**
 * Golden tests for {@link AstPrinter}.
 *
 * <p>Each test captures the exact textual output for a representative AST shape. The output
 * contract is locked: changing the printer's format requires updating these tests deliberately, not
 * by accident. The expected strings mirror those historically produced by {@code
 * PrettyPrinterVisitor} so the pattern-matching rewrite is behaviour-preserving.
 */
@SuppressWarnings({
    "PMD.CommentRequired",
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.TooManyMethods",
    "PMD.AtLeastOneConstructor"
})
class AstPrinterTest {

    private static final Span SPAN = Span.point(SourceLocation.create(1, 1, 0));

    // --- Expr ---------------------------------------------------------------

    @Test
    void printsBinaryAddition() {
        final Expr node = new Expr.Binary(var("x"), BinaryOp.ADD, var("y"), SPAN);
        assertThat(AstPrinter.print(node)).isEqualTo("(x ADD y)");
    }

    @Test
    void printsNestedGroupingWithMultiply() {
        final Expr inner = new Expr.Grouping(var("a"), SPAN);
        final Expr binary = new Expr.Binary(inner, BinaryOp.MULTIPLY, var("b"), SPAN);
        assertThat(AstPrinter.print(binary)).isEqualTo("((a) MULTIPLY b)");
    }

    @Test
    void printsUnaryPrefix() {
        final Expr node = new Expr.Unary(UnaryOp.NEGATE, UnaryOpSide.PREFIX, var("x"), SPAN);
        assertThat(AstPrinter.print(node)).isEqualTo("(NEGATE x)");
    }

    @Test
    void printsArrayLiteral() {
        final Expr node = new Expr.ArrayLiteral(List.of(var("a"), var("b")), SPAN);
        assertThat(AstPrinter.print(node)).isEqualTo("[a, b]");
    }

    @Test
    void printsAssign() {
        final Expr node = new Expr.Assign(var("x"), var("y"), SPAN);
        assertThat(AstPrinter.print(node)).isEqualTo("x = y");
    }

    @Test
    void printsCall() {
        final Expr node = new Expr.Call(var("f"), List.of(var("a"), var("b")), SPAN);
        assertThat(AstPrinter.print(node)).isEqualTo("f(a, b)");
    }

    @Test
    void printsArrayAccess() {
        final Expr node = new Expr.ArrayAccess(var("arr"), var("i"), SPAN);
        assertThat(AstPrinter.print(node)).isEqualTo("arr[i]");
    }

    @Test
    void printsNullptrLiteral() {
        assertThat(AstPrinter.print(Expr.nullExpr(SPAN))).isEqualTo("nullptr");
    }

    @Test
    void printsBoolLiteral() {
        final Expr node = new Expr.Literal(new LiteralValue.Bool(true), SPAN);
        assertThat(AstPrinter.print(node)).isEqualTo("true");
    }

    @Test
    void printsNumericLiteral() {
        final Expr node = new Expr.Literal(new LiteralValue.Numeric(new INumber.I32(42)), SPAN);
        assertThat(AstPrinter.print(node)).isEqualTo("42i32");
    }

    @Test
    void printsStringLiteral() {
        final Expr node = new Expr.Literal(new LiteralValue.StringLit("hi"), SPAN);
        assertThat(AstPrinter.print(node)).isEqualTo("\"hi\"");
    }

    @Test
    void printsCharLiteral() {
        final Expr node = new Expr.Literal(new LiteralValue.CharLit("a"), SPAN);
        assertThat(AstPrinter.print(node)).isEqualTo("'a'");
    }

    // --- Stmt ---------------------------------------------------------------

    @Test
    void printsExpressionStatement() {
        final Stmt node = new Stmt.Expression(var("x"));
        assertThat(AstPrinter.print(node)).isEqualTo("x;");
    }

    @Test
    void printsReturnWithoutValue() {
        final Stmt node = new Stmt.Return(Optional.empty(), SPAN);
        assertThat(AstPrinter.print(node)).isEqualTo("return;");
    }

    @Test
    void printsReturnWithValue() {
        final Stmt node = new Stmt.Return(Optional.of(var("x")), SPAN);
        assertThat(AstPrinter.print(node)).isEqualTo("return x;");
    }

    @Test
    void printsBreak() {
        final Stmt node = new Stmt.Break(SPAN);
        assertThat(AstPrinter.print(node)).isEqualTo("break;");
    }

    @Test
    void printsContinue() {
        final Stmt node = new Stmt.Continue(SPAN);
        assertThat(AstPrinter.print(node)).isEqualTo("continue;");
    }

    @Test
    void printsIfWithoutElse() {
        final Stmt node =
                new Stmt.If(
                        var("c"),
                        new Stmt.Block(List.<Stmt>of(), SPAN),
                        new ElseBranch.None(),
                        SPAN);
        final String out = AstPrinter.print(node);
        assertThat(out).contains("if (c) {");
        assertThat(out).doesNotContain("else");
    }

    @Test
    void printsIfWithElseBlock() {
        final Stmt node =
                new Stmt.If(
                        var("c"),
                        new Stmt.Block(List.<Stmt>of(), SPAN),
                        new ElseBranch.Block(new Stmt.Block(List.<Stmt>of(), SPAN)),
                        SPAN);
        assertThat(AstPrinter.print(node)).contains("else {");
    }

    @Test
    void printsElseIf() {
        final Stmt.If inner =
                new Stmt.If(
                        var("d"),
                        new Stmt.Block(List.<Stmt>of(), SPAN),
                        new ElseBranch.None(),
                        SPAN);
        final Stmt.If outer =
                new Stmt.If(
                        var("c"),
                        new Stmt.Block(List.<Stmt>of(), SPAN),
                        new ElseBranch.ElseIf(inner),
                        SPAN);
        assertThat(AstPrinter.print(outer)).contains("else if (d)");
    }

    @Test
    void printsWhile() {
        final Stmt node = new Stmt.While(var("c"), new Stmt.Block(List.<Stmt>of(), SPAN), SPAN);
        assertThat(AstPrinter.print(node)).contains("while (c)");
    }

    @Test
    void printsFor() {
        final Stmt.VarDeclaration init =
                new Stmt.VarDeclaration(
                        List.of(new Stmt.VarBinding("i", Optional.of(var("0")))),
                        new Type.I32(),
                        true,
                        SPAN);
        final Stmt node =
                new Stmt.For(
                        Optional.of(init),
                        Optional.of(var("c")),
                        Optional.of(var("i")),
                        new Stmt.Block(List.<Stmt>of(), SPAN),
                        SPAN);
        assertThat(AstPrinter.print(node)).contains("for (");
        assertThat(AstPrinter.print(node)).contains("; c; i)");
    }

    @Test
    void printsBlock() {
        final Stmt node = new Stmt.Block(List.<Stmt>of(), SPAN);
        assertThat(AstPrinter.print(node)).isEqualTo("{\n}");
    }

    @Test
    void printsFunction() {
        final Stmt node =
                new Stmt.Function(
                        "add",
                        List.of(
                                new Parameter("x", new Type.I32(), SPAN),
                                new Parameter("y", new Type.I32(), SPAN)),
                        new Type.I32(),
                        new Stmt.Block(List.of(new Stmt.Return(Optional.of(var("x")), SPAN)), SPAN),
                        SPAN);
        final String out = AstPrinter.print(node);
        assertThat(out).startsWith("fn add(x: i32, y: i32): i32 {");
        assertThat(out).contains("return x;");
    }

    @Test
    void printsMain() {
        final Stmt node = new Stmt.MainFunction(new Stmt.Block(List.<Stmt>of(), SPAN), SPAN);
        assertThat(AstPrinter.print(node)).startsWith("main {");
    }

    @Test
    void printsVarDeclarationNoInitializers() {
        final Stmt node =
                new Stmt.VarDeclaration(
                        List.of(new Stmt.VarBinding("x", Optional.empty())),
                        new Type.I32(),
                        false,
                        SPAN);
        final String out = AstPrinter.print(node);
        assertThat(out).contains("var x: i32;");
        assertThat(out).doesNotContain("mut");
    }

    @Test
    void printsVarDeclarationMutable() {
        final Stmt node =
                new Stmt.VarDeclaration(
                        List.of(new Stmt.VarBinding("x", Optional.of(var("1")))),
                        new Type.I32(),
                        true,
                        SPAN);
        assertThat(AstPrinter.print(node)).contains("mut var x: i32 = 1;");
    }

    // --- Type ---------------------------------------------------------------

    @Test
    void printsPrimitiveTypes() {
        assertThat(AstPrinter.print(new Type.I8())).isEqualTo("i8");
        assertThat(AstPrinter.print(new Type.I16())).isEqualTo("i16");
        assertThat(AstPrinter.print(new Type.I32())).isEqualTo("i32");
        assertThat(AstPrinter.print(new Type.I64())).isEqualTo("i64");
        assertThat(AstPrinter.print(new Type.U8())).isEqualTo("u8");
        assertThat(AstPrinter.print(new Type.U16())).isEqualTo("u16");
        assertThat(AstPrinter.print(new Type.U32())).isEqualTo("u32");
        assertThat(AstPrinter.print(new Type.U64())).isEqualTo("u64");
        assertThat(AstPrinter.print(new Type.F32())).isEqualTo("f32");
        assertThat(AstPrinter.print(new Type.F64())).isEqualTo("f64");
        assertThat(AstPrinter.print(new Type.Char())).isEqualTo("char");
        assertThat(AstPrinter.print(new Type.StringT())).isEqualTo("string");
        assertThat(AstPrinter.print(new Type.Bool())).isEqualTo("bool");
        assertThat(AstPrinter.print(new Type.VoidT())).isEqualTo("void");
        assertThat(AstPrinter.print(new Type.NullPtr())).isEqualTo("nullptr");
    }

    @Test
    void printsCustomType() {
        assertThat(AstPrinter.print(new Type.Custom("MyStruct"))).isEqualTo("MyStruct");
    }

    @Test
    void printsArrayType() {
        final Type node =
                new Type.Array(
                        new Type.I32(),
                        new Expr.Literal(new LiteralValue.Numeric(new INumber.I32(10)), SPAN));
        assertThat(AstPrinter.print(node)).isEqualTo("[i32; 10i32]");
    }

    @Test
    void printsVectorType() {
        final Type node = new Type.Vector(new Type.I32());
        assertThat(AstPrinter.print(node)).isEqualTo("Vec<i32>");
    }

    // --- Helper -------------------------------------------------------------

    private static Expr.Variable var(final String name) {
        return new Expr.Variable(name, SPAN);
    }
}
