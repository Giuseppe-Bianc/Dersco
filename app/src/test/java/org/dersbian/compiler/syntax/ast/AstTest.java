package org.dersbian.compiler.syntax.ast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Optional;
import org.dersbian.compiler.CompilerException;
import org.dersbian.compiler.lexer.token.SourceId;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.lexer.token.Token;
import org.dersbian.compiler.lexer.token.TokenKind;
import org.dersbian.compiler.lexer.token.number.INumber;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestContainsTooManyAsserts"
})
class AstTest {

    private final SourceId sourceId = new SourceId.VirtualResource("test");
    private final SourceLocation loc1 = SourceLocation.create(1, 1, 0);
    private final SourceLocation loc2 = SourceLocation.create(1, 5, 4);
    private final Span span = Span.create(loc1, loc2);

    @Test
    void testExprHelpers() {
        final Expr nullExpr = Expr.nullExpr(span);
        assertThat(nullExpr).isInstanceOf(Expr.Literal.class);
        final Expr.Literal literalNull = (Expr.Literal) nullExpr;
        assertThat(literalNull.value()).isInstanceOf(LiteralValue.NullPtr.class);
        assertThat(nullExpr.span()).isEqualTo(span);

        final INumber num = new INumber.I32(42);
        final Optional<Expr> numExpr = Expr.newNumberLiteral(num, span);
        assertThat(numExpr).isPresent();
        assertThat(numExpr.get()).isEqualTo(new Expr.Literal(new LiteralValue.Numeric(num), span));

        final Optional<Expr> boolExpr = Expr.newBoolLiteral(true, span);
        assertThat(boolExpr).isPresent();
        assertThat(boolExpr.get()).isEqualTo(new Expr.Literal(new LiteralValue.Bool(true), span));

        final Optional<Expr> nullLitExpr = Expr.newNullptrLiteral(span);
        assertThat(nullLitExpr).isPresent();
        assertThat(nullLitExpr.get()).isEqualTo(nullExpr);

        final Optional<Expr> strExpr = Expr.newStringLiteral("hello", span);
        assertThat(strExpr).isPresent();
        assertThat(strExpr.get())
                .isEqualTo(new Expr.Literal(new LiteralValue.StringLit("hello"), span));

        final Optional<Expr> charExpr = Expr.newCharLiteral("a", span);
        assertThat(charExpr).isPresent();
        assertThat(charExpr.get()).isEqualTo(new Expr.Literal(new LiteralValue.CharLit("a"), span));
    }

    @Test
    void testExprVariants() {
        final Expr varX = new Expr.Variable("x", span);
        final Expr lit1 = new Expr.Literal(new LiteralValue.Numeric(new INumber.I32(1)), span);
        final Expr.Binary binary = new Expr.Binary(varX, BinaryOp.ADD, lit1, span);

        assertThat(binary.left()).isEqualTo(varX);
        assertThat(binary.op()).isEqualTo(BinaryOp.ADD);
        assertThat(binary.right()).isEqualTo(lit1);
        assertThat(binary.span()).isEqualTo(span);

        final Expr.Unary unary = new Expr.Unary(UnaryOp.NEGATE, UnaryOpSide.POSTFIX, varX, span);
        assertThat(unary.op()).isEqualTo(UnaryOp.NEGATE);
        assertThat(unary.side()).isEqualTo(UnaryOpSide.POSTFIX);
        final Expr.Grouping grouping = new Expr.Grouping(binary, span);
        assertThat(grouping.expr()).isEqualTo(binary);

        final Expr arrayLit = new Expr.ArrayLiteral(List.of(varX, lit1), span);
        assertThat(arrayLit).isInstanceOf(Expr.ArrayLiteral.class);

        final Expr.Assign assign = new Expr.Assign(varX, lit1, span);
        assertThat(assign.target()).isEqualTo(varX);

        final Expr.Call call = new Expr.Call(varX, List.of(lit1), span);
        assertThat(call.callee()).isEqualTo(varX);

        final Expr.ArrayAccess arrayAccess = new Expr.ArrayAccess(varX, lit1, span);
        assertThat(arrayAccess.array()).isEqualTo(varX);
    }

    @Test
    void testBinaryOpMapping() {
        final Token plusToken = Token.create(sourceId, TokenKind.Simple.Operator.PLUS, span);
        assertThat(BinaryOp.getOp(plusToken)).isEqualTo(BinaryOp.ADD);

        final Token minusToken = Token.create(sourceId, TokenKind.Simple.Operator.MINUS, span);
        assertThat(BinaryOp.getOp(minusToken)).isEqualTo(BinaryOp.SUBTRACT);

        final Token starToken = Token.create(sourceId, TokenKind.Simple.Operator.STAR, span);
        assertThat(BinaryOp.getOp(starToken)).isEqualTo(BinaryOp.MULTIPLY);

        final Token slashToken = Token.create(sourceId, TokenKind.Simple.Operator.SLASH, span);
        assertThat(BinaryOp.getOp(slashToken)).isEqualTo(BinaryOp.DIVIDE);

        final Token percentToken = Token.create(sourceId, TokenKind.Simple.Operator.PERCENT, span);
        assertThat(BinaryOp.getOp(percentToken)).isEqualTo(BinaryOp.MODULO);

        final Token eqToken = Token.create(sourceId, TokenKind.Simple.Operator.EQUAL_EQUAL, span);
        assertThat(BinaryOp.getOp(eqToken)).isEqualTo(BinaryOp.EQUAL);

        final Token neqToken = Token.create(sourceId, TokenKind.Simple.Operator.NOT_EQUAL, span);
        assertThat(BinaryOp.getOp(neqToken)).isEqualTo(BinaryOp.NOT_EQUAL);

        final Token ltToken = Token.create(sourceId, TokenKind.Simple.Operator.LESS, span);
        assertThat(BinaryOp.getOp(ltToken)).isEqualTo(BinaryOp.LESS);

        final Token leToken = Token.create(sourceId, TokenKind.Simple.Operator.LESS_EQUAL, span);
        assertThat(BinaryOp.getOp(leToken)).isEqualTo(BinaryOp.LESS_EQUAL);

        final Token gtToken = Token.create(sourceId, TokenKind.Simple.Operator.GREATER, span);
        assertThat(BinaryOp.getOp(gtToken)).isEqualTo(BinaryOp.GREATER);

        final Token geToken = Token.create(sourceId, TokenKind.Simple.Operator.GREATER_EQUAL, span);
        assertThat(BinaryOp.getOp(geToken)).isEqualTo(BinaryOp.GREATER_EQUAL);

        final Token andAndToken = Token.create(sourceId, TokenKind.Simple.Operator.AND_AND, span);
        assertThat(BinaryOp.getOp(andAndToken)).isEqualTo(BinaryOp.AND);

        final Token orOrToken = Token.create(sourceId, TokenKind.Simple.Operator.OR_OR, span);
        assertThat(BinaryOp.getOp(orOrToken)).isEqualTo(BinaryOp.OR);

        final Token andToken = Token.create(sourceId, TokenKind.Simple.Operator.AND, span);
        assertThat(BinaryOp.getOp(andToken)).isEqualTo(BinaryOp.BITWISE_AND);

        final Token orToken = Token.create(sourceId, TokenKind.Simple.Operator.OR, span);
        assertThat(BinaryOp.getOp(orToken)).isEqualTo(BinaryOp.BITWISE_OR);

        final Token xorToken = Token.create(sourceId, TokenKind.Simple.Operator.XOR, span);
        assertThat(BinaryOp.getOp(xorToken)).isEqualTo(BinaryOp.BITWISE_XOR);

        final Token shlToken = Token.create(sourceId, TokenKind.Simple.Operator.SHIFT_LEFT, span);
        assertThat(BinaryOp.getOp(shlToken)).isEqualTo(BinaryOp.SHIFT_LEFT);

        final Token shrToken = Token.create(sourceId, TokenKind.Simple.Operator.SHIFT_RIGHT, span);
        assertThat(BinaryOp.getOp(shrToken)).isEqualTo(BinaryOp.SHIFT_RIGHT);

        final Token invalidToken = Token.create(sourceId, TokenKind.Simple.Keyword.IF, span);
        assertThatThrownBy(() -> BinaryOp.getOp(invalidToken))
                .isInstanceOf(CompilerException.class)
                .hasMessageContaining("E1005");
    }

    @Test
    void testTypeVariants() {
        final Type i32 = new Type.I32();
        final Type custom = new Type.Custom("MyStruct");
        final Expr sizeExpr = new Expr.Literal(new LiteralValue.Numeric(new INumber.I32(10)), span);
        final Type array = new Type.Array(i32, sizeExpr);
        final Type vector = new Type.Vector(custom);

        assertThat(i32).isInstanceOf(Type.I32.class);
        assertThat(custom).isEqualTo(new Type.Custom("MyStruct"));
        assertThat(array).isInstanceOf(Type.Array.class);
        assertThat(vector).isInstanceOf(Type.Vector.class);
    }

    @Test
    void testStmtVariants() {
        final Expr varX = new Expr.Variable("x", span);
        final Stmt exprStmt = new Stmt.Expression(varX);
        assertThat(exprStmt.span()).isEqualTo(span);

        final Parameter param = new Parameter("x", new Type.I32(), span);
        final Stmt funcStmt =
                new Stmt.Function(
                        "foo",
                        List.of(param),
                        new Type.VoidT(),
                        new Stmt.Block(List.of(exprStmt), span),
                        span);
        assertThat(funcStmt.span()).isEqualTo(span);

        final Stmt ifStmt =
                new Stmt.If(varX, new Stmt.Block(List.of(exprStmt), span), Optional.empty(), span);
        assertThat(ifStmt.span()).isEqualTo(span);

        final Stmt whileStmt = new Stmt.While(varX, new Stmt.Block(List.of(exprStmt), span), span);
        assertThat(whileStmt.span()).isEqualTo(span);

        final Stmt forStmt =
                new Stmt.For(
                        Optional.empty(),
                        Optional.of(varX),
                        Optional.empty(),
                        new Stmt.Block(List.of(exprStmt), span),
                        span);
        assertThat(forStmt.span()).isEqualTo(span);

        final Stmt blockStmt = new Stmt.Block(List.of(exprStmt), span);
        assertThat(blockStmt.span()).isEqualTo(span);

        final Stmt returnStmt = new Stmt.Return(Optional.of(varX), span);
        assertThat(returnStmt.span()).isEqualTo(span);

        final Stmt breakStmt = new Stmt.Break(span);
        assertThat(breakStmt.span()).isEqualTo(span);

        final Stmt continueStmt = new Stmt.Continue(span);
        assertThat(continueStmt.span()).isEqualTo(span);

        final Stmt mainStmt = new Stmt.MainFunction(new Stmt.Block(List.of(exprStmt), span), span);
        assertThat(mainStmt.span()).isEqualTo(span);

        final Stmt varDecl =
                new Stmt.VarDeclaration(
                        List.of(new Stmt.VarBinding("x", Optional.of(varX))),
                        new Type.I32(),
                        true,
                        span);
        assertThat(varDecl.span()).isEqualTo(span);
    }
}
