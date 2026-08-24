package org.dersbian.compiler.syntax;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.dersbian.compiler.lexer.Lexer;
import org.dersbian.compiler.lexer.LexerResult;
import org.dersbian.compiler.syntax.ast.BinaryOp;
import org.dersbian.compiler.syntax.ast.Expr;
import org.dersbian.compiler.syntax.ast.LiteralValue;
import org.dersbian.compiler.syntax.ast.Stmt;
import org.dersbian.compiler.syntax.ast.Type;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.TooManyMethods",
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.ExcessiveImports"
})
class StatementParserTest {

    private Stmt parseStatement(final String source) {
        final ParseResult result = parseAll(source);
        assertThat(result.statements()).isNotEmpty();
        return result.statements().get(0);
    }

    private ParseResult parseAll(final String source) {
        final Lexer lexer = new Lexer(Path.of("test.dr"), source);
        final LexerResult result = lexer.tokenize();
        return new Parser(result.tokens(), Path.of("test.dr")).parse();
    }

    // Expression statements

    @Test
    void expressionStatementWrapsExpression() {
        final Stmt s = parseStatement("a\n");
        assertThat(s).isInstanceOf(Stmt.Expression.class);
        assertThat(((Stmt.Expression) s).expr()).isInstanceOf(Expr.Variable.class);
    }

    // var declarations

    @Test
    void varDeclarationIsMutable() {
        final Stmt s = parseStatement("var x: i32 = 5i32\n");
        assertThat(s).isInstanceOf(Stmt.VarDeclaration.class);
        final Stmt.VarDeclaration decl = (Stmt.VarDeclaration) s;
        assertThat(decl.isMutable()).isTrue();
        assertThat(decl.bindings()).hasSize(1);
        assertThat(decl.bindings().get(0).name()).isEqualTo("x");
        assertThat(decl.bindings().get(0).initializer()).isPresent();
        assertThat(decl.typeAnnotation()).isEqualTo(new Type.I32());
    }

    @Test
    void constDeclarationIsNotMutable() {
        final Stmt s = parseStatement("const y: bool = true\n");
        final Stmt.VarDeclaration decl = (Stmt.VarDeclaration) s;
        assertThat(decl.isMutable()).isFalse();
        assertThat(decl.typeAnnotation()).isEqualTo(new Type.Bool());
    }

    @Test
    void varDeclarationWithoutInitializer() {
        final Stmt s = parseStatement("var i: i32\n");
        final Stmt.VarDeclaration decl = (Stmt.VarDeclaration) s;
        assertThat(decl.isMutable()).isTrue();
        assertThat(decl.bindings().get(0).initializer()).isEqualTo(Optional.empty());
    }

    @Test
    void varDeclarationMultipleBindings() {
        final Stmt s = parseStatement("var a, b: i64 = 12, 21\n");
        final Stmt.VarDeclaration decl = (Stmt.VarDeclaration) s;
        assertThat(decl.bindings()).hasSize(2);
        assertThat(decl.bindings().get(0).name()).isEqualTo("a");
        assertThat(decl.bindings().get(1).name()).isEqualTo("b");
        assertThat(decl.bindings().get(0).initializer()).isPresent();
        assertThat(decl.bindings().get(1).initializer()).isPresent();
    }

    @Test
    void varDeclarationArrayType() {
        final Stmt s = parseStatement("var arr: i64[5] = {1i64, 2i64, 3i64, 4i64, 5i64}\n");
        final Stmt.VarDeclaration decl = (Stmt.VarDeclaration) s;
        assertThat(decl.typeAnnotation()).isInstanceOf(Type.Array.class);
    }

    // fun declarations

    @Test
    void funDeclarationNoParams() {
        final Stmt s = parseStatement("fun f(): void { }\n");
        assertThat(s).isInstanceOf(Stmt.Function.class);
        final Stmt.Function fn = (Stmt.Function) s;
        assertThat(fn.name()).isEqualTo("f");
        assertThat(fn.parameters()).isEmpty();
        assertThat(fn.returnType()).isEqualTo(new Type.VoidT());
        assertThat(fn.body().statements()).isEmpty();
    }

    @Test
    void funDeclarationWithParams() {
        final Stmt s = parseStatement("fun add(a: i32, b: i32): i32 { return a }\n");
        final Stmt.Function fn = (Stmt.Function) s;
        assertThat(fn.parameters()).hasSize(2);
        assertThat(fn.parameters().get(0).name()).isEqualTo("a");
        assertThat(fn.parameters().get(0).typeAnnotation()).isEqualTo(new Type.I32());
        assertThat(fn.parameters().get(1).name()).isEqualTo("b");
        assertThat(fn.parameters().get(1).typeAnnotation()).isEqualTo(new Type.I32());
        assertThat(fn.returnType()).isEqualTo(new Type.I32());
    }

    @Test
    void funDeclarationBodyHasStatements() {
        final Stmt s = parseStatement("fun f(): void { var x: i32 = 1i32 }\n");
        final Stmt.Function fn = (Stmt.Function) s;
        assertThat(fn.body().statements()).hasSize(1);
        assertThat(fn.body().statements().get(0)).isInstanceOf(Stmt.VarDeclaration.class);
    }

    // main block

    @Test
    void mainBlockProducesStmtMainFunction() {
        final Stmt s = parseStatement("main { }\n");
        assertThat(s).isInstanceOf(Stmt.MainFunction.class);
        final Stmt.MainFunction main = (Stmt.MainFunction) s;
        assertThat(main.body().statements()).isEmpty();
    }

    @Test
    void mainBlockWithBody() {
        final Stmt s = parseStatement("main { var x: i32 = 1i32 }\n");
        final Stmt.MainFunction main = (Stmt.MainFunction) s;
        assertThat(main.body().statements()).hasSize(1);
    }

    // if / else if / else

    @Test
    void ifWithNoElseProducesElseBranchNone() {
        final Stmt s = parseStatement("if (true) { }\n");
        assertThat(s).isInstanceOf(Stmt.If.class);
        final Stmt.If ifStmt = (Stmt.If) s;
        assertThat(ifStmt.elseBranch())
                .isInstanceOf(org.dersbian.compiler.syntax.ast.ElseBranch.None.class);
    }

    @Test
    void ifWithElseBlockProducesElseBranchBlock() {
        final Stmt s = parseStatement("if (true) { } else { }\n");
        final Stmt.If ifStmt = (Stmt.If) s;
        assertThat(ifStmt.elseBranch())
                .isInstanceOf(org.dersbian.compiler.syntax.ast.ElseBranch.Block.class);
    }

    @Test
    void ifWithElseIfProducesElseBranchElseIf() {
        final Stmt s = parseStatement("if (a == 1) { } else if (a == 2) { }\n");
        final Stmt.If ifStmt = (Stmt.If) s;
        assertThat(ifStmt.elseBranch())
                .isInstanceOf(org.dersbian.compiler.syntax.ast.ElseBranch.ElseIf.class);
        final org.dersbian.compiler.syntax.ast.ElseBranch.ElseIf elseIf =
                (org.dersbian.compiler.syntax.ast.ElseBranch.ElseIf) ifStmt.elseBranch();
        final Stmt.If nested = elseIf.ifStmt();
        assertThat(nested).isInstanceOf(Stmt.If.class);
        assertThat(nested.elseBranch())
                .isInstanceOf(org.dersbian.compiler.syntax.ast.ElseBranch.None.class);
    }

    @Test
    void ifElseIfElseChain() {
        final Stmt s = parseStatement("if (a == 1) { } else if (a == 2) { } else { }\n");
        final Stmt.If ifStmt = (Stmt.If) s;
        assertThat(ifStmt.elseBranch())
                .isInstanceOf(org.dersbian.compiler.syntax.ast.ElseBranch.ElseIf.class);
        final org.dersbian.compiler.syntax.ast.ElseBranch.ElseIf elseIf =
                (org.dersbian.compiler.syntax.ast.ElseBranch.ElseIf) ifStmt.elseBranch();
        assertThat(elseIf.ifStmt().elseBranch())
                .isInstanceOf(org.dersbian.compiler.syntax.ast.ElseBranch.Block.class);
    }

    // while

    @Test
    void whileLoopProducesStmtWhile() {
        final Stmt s = parseStatement("while (true) { break }\n");
        assertThat(s).isInstanceOf(Stmt.While.class);
        final Stmt.While w = (Stmt.While) s;
        assertThat(w.condition()).isInstanceOf(Expr.Literal.class);
        assertThat(((Expr.Literal) w.condition()).value()).isEqualTo(new LiteralValue.Bool(true));
        assertThat(w.body().statements()).hasSize(1);
        assertThat(w.body().statements().get(0)).isInstanceOf(Stmt.Break.class);
    }

    // for

    @Test
    void forLoopAllClausesPresent() {
        final Stmt s =
                parseStatement("for (var i: i32 = 0i32; i < 10i32; i = i + 1i32) { break }\n");
        assertThat(s).isInstanceOf(Stmt.For.class);
        final Stmt.For f = (Stmt.For) s;
        assertThat(f.initializer()).isPresent();
        assertThat(f.initializer().get()).isInstanceOf(Stmt.VarDeclaration.class);
        assertThat(f.condition()).isPresent();
        final Expr cond = f.condition().get();
        assertThat(cond).isInstanceOf(Expr.Binary.class);
        assertThat(((Expr.Binary) cond).op()).isEqualTo(BinaryOp.LESS);
        assertThat(f.increment()).isPresent();
        assertThat(f.increment().get()).isInstanceOf(Expr.Assign.class);
    }

    @Test
    void forLoopNoClausesInfiniteLoop() {
        final Stmt s = parseStatement("for (;;) { break }\n");
        final Stmt.For f = (Stmt.For) s;
        assertThat(f.initializer()).isEqualTo(Optional.empty());
        assertThat(f.condition()).isEqualTo(Optional.empty());
        assertThat(f.increment()).isEqualTo(Optional.empty());
    }

    @Test
    void forLoopConditionOnlyPresent() {
        final Stmt s = parseStatement("for (; i < 10i32;) { break }\n");
        final Stmt.For f = (Stmt.For) s;
        assertThat(f.initializer()).isEqualTo(Optional.empty());
        assertThat(f.condition()).isPresent();
        assertThat(f.increment()).isEqualTo(Optional.empty());
    }

    // return / break / continue

    @Test
    void returnWithValueProducesStmtReturn() {
        final ParseResult r = parseAll("fun f(): i32 { return 42i32 }\n");
        final Stmt.Function fn = (Stmt.Function) r.statements().get(0);
        final Stmt ret = fn.body().statements().get(0);
        assertThat(ret).isInstanceOf(Stmt.Return.class);
        assertThat(((Stmt.Return) ret).value()).isPresent();
    }

    @Test
    void returnWithNoValueProducesStmtReturnEmpty() {
        final ParseResult r = parseAll("fun f(): void { return }\n");
        final Stmt.Function fn = (Stmt.Function) r.statements().get(0);
        final Stmt ret = fn.body().statements().get(0);
        assertThat(ret).isInstanceOf(Stmt.Return.class);
        assertThat(((Stmt.Return) ret).value()).isEqualTo(Optional.empty());
    }

    @Test
    void breakProducesStmtBreak() {
        final ParseResult r = parseAll("fun f(): void { while(true) { break } }\n");
        final Stmt.Function fn = (Stmt.Function) r.statements().get(0);
        final Stmt.While w = (Stmt.While) fn.body().statements().get(0);
        assertThat(w.body().statements().get(0)).isInstanceOf(Stmt.Break.class);
    }

    @Test
    void continueProducesStmtContinue() {
        final ParseResult r = parseAll("fun f(): void { while(true) { continue } }\n");
        final Stmt.Function fn = (Stmt.Function) r.statements().get(0);
        final Stmt.While w = (Stmt.While) fn.body().statements().get(0);
        assertThat(w.body().statements().get(0)).isInstanceOf(Stmt.Continue.class);
    }

    // standalone block

    @Test
    void standaloneBlockProducesStmtBlock() {
        final ParseResult r = parseAll("main { { } }\n");
        final Stmt.MainFunction main = (Stmt.MainFunction) r.statements().get(0);
        final List<Stmt> body = main.body().statements();
        assertThat(body.stream().anyMatch(s -> s instanceof Stmt.Block)).isTrue();
        final Stmt.Block block =
                (Stmt.Block) body.stream().filter(s -> s instanceof Stmt.Block).findFirst().get();
        assertThat(block.statements()).isEmpty();
    }

    // multi-dimensional array

    @Test
    void multiDimensionalArrayTypeParses() {
        final Stmt s =
                parseStatement("var matrix: i8[2][3] = {{1i8, 2i8, 3i8}, {4i8, 5i8, 6i8}}\n");
        final Stmt.VarDeclaration decl = (Stmt.VarDeclaration) s;
        final Type.Array outer = (Type.Array) decl.typeAnnotation();
        assertThat(outer.elementType()).isInstanceOf(Type.Array.class);
    }

    // type keywords round-trip

    @Test
    void allPrimitiveTypeKeywordsParsed() {
        assertTypeKeyword("i8", new Type.I8());
        assertTypeKeyword("i16", new Type.I16());
        assertTypeKeyword("i32", new Type.I32());
        assertTypeKeyword("i64", new Type.I64());
        assertTypeKeyword("u8", new Type.U8());
        assertTypeKeyword("u16", new Type.U16());
        assertTypeKeyword("u32", new Type.U32());
        assertTypeKeyword("u64", new Type.U64());
        assertTypeKeyword("f32", new Type.F32());
        assertTypeKeyword("f64", new Type.F64());
        assertTypeKeyword("char", new Type.Char());
        assertTypeKeyword("string", new Type.StringT());
        assertTypeKeyword("bool", new Type.Bool());
    }

    private void assertTypeKeyword(final String kw, final Type expected) {
        final Stmt s = parseStatement("var x: " + kw + " = null\n");
        final Stmt.VarDeclaration decl = (Stmt.VarDeclaration) s;
        assertThat(decl.typeAnnotation()).isEqualTo(expected);
    }
}
