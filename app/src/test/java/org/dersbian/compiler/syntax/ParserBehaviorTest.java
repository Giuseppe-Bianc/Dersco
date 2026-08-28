package org.dersbian.compiler.syntax;

import java.nio.file.Path;
import java.util.List;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.error.ErrorCode;
import org.dersbian.compiler.lexer.Lexer;
import org.dersbian.compiler.lexer.LexerResult;
import org.dersbian.compiler.syntax.ast.BinaryOp;
import org.dersbian.compiler.syntax.ast.ElseBranch;
import org.dersbian.compiler.syntax.ast.Expr;
import org.dersbian.compiler.syntax.ast.Stmt;
import org.dersbian.compiler.syntax.ast.Type;
import org.dersbian.compiler.syntax.ast.UnaryOp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.UnitTestAssertionsShouldIncludeMessage",
    "PMD.TooManyMethods"
})
class ParserBehaviorTest {

    @Test
    void emptyTokenStreamProducesEmptyResult() {
        final ParseResult result = new Parser(List.of()).parse();

        Assertions.assertTrue(result.statements().isEmpty());
        Assertions.assertTrue(result.errors().isEmpty());
    }

    @Test
    void nullTokenStreamIsRejected() {
        Assertions.assertThrows(NullPointerException.class, () -> new Parser(null));
    }

    @Test
    void parsesLiteralAndVariableExpressions() {
        for (final String source :
                List.of("42", "true", "nullptr", "\"text\"", "'c'", "identifier")) {
            final ParseResult result = parse(source);

            Assertions.assertTrue(
                    result.errors().isEmpty(),
                    () -> "Unexpected parser errors: " + result.errors());
            Assertions.assertEquals(1, result.statements().size());
            Assertions.assertInstanceOf(Stmt.Expression.class, result.statements().get(0));
        }
    }

    @Test
    void parsesBinaryPrecedenceAndRightAssociativeAssignment() {
        final Expr.Binary arithmetic = expression("a + b * c", Expr.Binary.class);
        Assertions.assertEquals(BinaryOp.ADD, arithmetic.op());
        Assertions.assertInstanceOf(Expr.Binary.class, arithmetic.right());
        Assertions.assertEquals(BinaryOp.MULTIPLY, ((Expr.Binary) arithmetic.right()).op());

        final Expr.Assign assignment = expression("a = b = c", Expr.Assign.class);
        Assertions.assertInstanceOf(Expr.Assign.class, assignment.value());
    }

    @Test
    void parsesGroupingCallsArrayAccessAndArrayLiteral() {
        final Expr.Grouping grouping = expression("(value)", Expr.Grouping.class);
        Assertions.assertInstanceOf(Expr.Variable.class, grouping.expr());

        final Expr.Call call = expression("fn(1, 2)", Expr.Call.class);
        Assertions.assertEquals(2, call.arguments().size());

        final Expr.ArrayAccess access = expression("values[0]", Expr.ArrayAccess.class);
        Assertions.assertInstanceOf(Expr.Variable.class, access.array());

        final Expr.Call arrayArgument = expression("fn({1, 2})", Expr.Call.class);
        Assertions.assertInstanceOf(Expr.ArrayLiteral.class, arrayArgument.arguments().get(0));
    }

    @Test
    void parsesAllPrefixAndPostfixUnaryOperators() {
        Assertions.assertEquals(UnaryOp.NEGATE, expression("-x", Expr.Unary.class).op());
        Assertions.assertEquals(UnaryOp.NOT, expression("!x", Expr.Unary.class).op());
        Assertions.assertEquals(UnaryOp.BITWISE_NOT, expression("~x", Expr.Unary.class).op());
        Assertions.assertEquals(UnaryOp.INCREMENT, expression("++x", Expr.Unary.class).op());
        Assertions.assertEquals(UnaryOp.DECREMENT, expression("x--", Expr.Unary.class).op());
    }

    @Test
    void parsesMainFunctionAndControlFlowStatements() {
        final ParseResult mainResult = parse("main {}");
        Assertions.assertEquals(1, mainResult.statements().size());
        Assertions.assertInstanceOf(Stmt.MainFunction.class, mainResult.statements().get(0));

        final ParseResult ifResult = parse("if (condition) {} else if (other) {} else {}");
        final Stmt.If ifStatement =
                Assertions.assertInstanceOf(Stmt.If.class, ifResult.statements().get(0));
        Assertions.assertInstanceOf(ElseBranch.ElseIf.class, ifStatement.elseBranch());

        final ParseResult whileResult = parse("while (condition) {}");
        Assertions.assertInstanceOf(Stmt.While.class, whileResult.statements().get(0));

        final ParseResult forResult = parse("for (;;) {}");
        final Stmt.For forStatement =
                Assertions.assertInstanceOf(Stmt.For.class, forResult.statements().get(0));
        Assertions.assertTrue(forStatement.initializer().isEmpty());
        Assertions.assertTrue(forStatement.condition().isEmpty());
        Assertions.assertTrue(forStatement.increment().isEmpty());
    }

    @Test
    void parsesFunctionsAndTypedDeclarations() {
        final ParseResult functionResult =
                parse("fun combine(left: i32, right: vector<i64>): i64 {}");
        final Stmt.Function function =
                Assertions.assertInstanceOf(
                        Stmt.Function.class, functionResult.statements().get(0));
        Assertions.assertEquals("combine", function.name());
        Assertions.assertEquals(2, function.parameters().size());
        Assertions.assertInstanceOf(
                Type.Vector.class, function.parameters().get(1).typeAnnotation());
        Assertions.assertInstanceOf(Type.I64.class, function.returnType());

        final ParseResult declarationResult = parse("var first, second: i32 = 1, 2");
        final Stmt.VarDeclaration declaration =
                Assertions.assertInstanceOf(
                        Stmt.VarDeclaration.class, declarationResult.statements().get(0));
        Assertions.assertTrue(declaration.isMutable());
        Assertions.assertEquals(2, declaration.bindings().size());

        final ParseResult constantResult = parse("const answer: i32 = 42");
        final Stmt.VarDeclaration constant =
                Assertions.assertInstanceOf(
                        Stmt.VarDeclaration.class, constantResult.statements().get(0));
        Assertions.assertFalse(constant.isMutable());
    }

    @Test
    void parsesReturnBreakAndContinue() {
        final ParseResult result = parse("return");
        final Stmt.Return returnStatement =
                Assertions.assertInstanceOf(Stmt.Return.class, result.statements().get(0));
        Assertions.assertTrue(returnStatement.value().isEmpty());

        Assertions.assertInstanceOf(Stmt.Break.class, parse("break").statements().get(0));
        Assertions.assertInstanceOf(Stmt.Continue.class, parse("continue").statements().get(0));

        final Stmt.Return valuedReturn =
                Assertions.assertInstanceOf(
                        Stmt.Return.class, parse("return 7").statements().get(0));
        Assertions.assertInstanceOf(Expr.Literal.class, valuedReturn.value().orElseThrow());
    }

    @Test
    void malformedInputCollectsStructuredSyntaxErrors() {
        final ParseResult result = parse("var : i32 = 1");

        Assertions.assertTrue(result.hasErrors());
        Assertions.assertTrue(
                result.errors().stream().anyMatch(error -> hasCode(error, ErrorCode.E1008)));
    }

    @Test
    void invalidAssignmentTargetProducesError() {
        final ParseResult result = parse("1 = value");

        Assertions.assertTrue(
                result.errors().stream().anyMatch(error -> hasCode(error, ErrorCode.E1003)));
    }

    @Test
    void semicolonIsRejectedOutsideFor() {
        final ParseResult result = parse("return;");

        Assertions.assertTrue(result.hasErrors());
        Assertions.assertTrue(
                result.errors().stream().anyMatch(error -> hasCode(error, ErrorCode.E1004)));
    }

    @Test
    void semicolonsRemainValidInsideFor() {
        final ParseResult result = parse("for (i = 0; i < 10; i++) {}");

        Assertions.assertTrue(
                result.errors().isEmpty(), () -> "Unexpected parser errors: " + result.errors());
        Assertions.assertEquals(1, result.statements().size());
        final Stmt.For forStatement =
                Assertions.assertInstanceOf(Stmt.For.class, result.statements().get(0));
        Assertions.assertTrue(forStatement.initializer().isPresent());
        Assertions.assertTrue(forStatement.condition().isPresent());
        Assertions.assertTrue(forStatement.increment().isPresent());
    }

    private <T extends Expr> T expression(final String source, final Class<T> expectedType) {
        final ParseResult result = parse(source);
        Assertions.assertTrue(
                result.errors().isEmpty(), () -> "Unexpected parser errors: " + result.errors());
        Assertions.assertEquals(1, result.statements().size());
        return expectedType.cast(
                Assertions.assertInstanceOf(Stmt.Expression.class, result.statements().get(0))
                        .expr());
    }

    private ParseResult parse(final String source) {
        final LexerResult lexed = new Lexer(Path.of("parser-behavior.dr"), source).tokenize();
        Assertions.assertTrue(
                lexed.errors().isEmpty(), () -> "Unexpected lexer errors: " + lexed.errors());
        return new Parser(lexed.tokens()).parse();
    }

    private boolean hasCode(final CompileError error, final ErrorCode code) {
        return error instanceof CompileError.SyntaxError syntaxError
                && syntaxError.code().orElse(null) == code;
    }
}
