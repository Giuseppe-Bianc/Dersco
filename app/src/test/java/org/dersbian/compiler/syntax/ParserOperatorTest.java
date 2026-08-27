package org.dersbian.compiler.syntax;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.dersbian.compiler.lexer.Lexer;
import org.dersbian.compiler.lexer.LexerResult;
import org.dersbian.compiler.syntax.ast.BinaryOp;
import org.dersbian.compiler.syntax.ast.Expr;
import org.dersbian.compiler.syntax.ast.Stmt;
import org.dersbian.compiler.syntax.ast.UnaryOp;
import org.dersbian.compiler.syntax.ast.UnaryOpSide;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestContainsTooManyAsserts"
})
class ParserOperatorTest {

    @Test
    void parsesCompoundAssignmentOperators() {
        final ParseResult addResult = parse("x += 1");
        final ParseResult andResult = parse("x &= 1");
        final ParseResult shiftResult = parse("x <<= 1");

        assertThat(addResult.errors()).isEmpty();
        assertThat(andResult.errors()).isEmpty();
        assertThat(shiftResult.errors()).isEmpty();
        assertThat(((Expr.Binary) ((Stmt.Expression) addResult.statements().get(0)).expr()).op())
                .isEqualTo(BinaryOp.ADD_EQUAL);
        assertThat(((Expr.Binary) ((Stmt.Expression) andResult.statements().get(0)).expr()).op())
                .isEqualTo(BinaryOp.BITWISE_AND_EQUAL);
        assertThat(((Expr.Binary) ((Stmt.Expression) shiftResult.statements().get(0)).expr()).op())
                .isEqualTo(BinaryOp.SHIFT_LEFT_EQUAL);
    }

    @Test
    void parsesBitwiseNotAndIncrementDecrement() {
        final ParseResult notResult = parse("~x");
        final ParseResult prefixResult = parse("++x");
        final ParseResult postfixResult = parse("x--");

        assertThat(notResult.errors()).isEmpty();
        assertThat(prefixResult.errors()).isEmpty();
        assertThat(postfixResult.errors()).isEmpty();
        assertThat(((Expr.Unary) ((Stmt.Expression) notResult.statements().get(0)).expr()).op())
                .isEqualTo(UnaryOp.BITWISE_NOT);
        assertThat(((Expr.Unary) ((Stmt.Expression) prefixResult.statements().get(0)).expr()).op())
                .isEqualTo(UnaryOp.INCREMENT);
        assertThat(
                        ((Expr.Unary) ((Stmt.Expression) prefixResult.statements().get(0)).expr())
                                .side())
                .isEqualTo(UnaryOpSide.PREFIX);
        assertThat(((Expr.Unary) ((Stmt.Expression) postfixResult.statements().get(0)).expr()).op())
                .isEqualTo(UnaryOp.DECREMENT);
        assertThat(
                        ((Expr.Unary) ((Stmt.Expression) postfixResult.statements().get(0)).expr())
                                .side())
                .isEqualTo(UnaryOpSide.POSTFIX);
    }

    @Test
    void prefixUnarySpanIncludesOperand() {
        final Expr.Unary unary = expression("-x", Expr.Unary.class);

        assertThat(unary.span().start()).isNotEqualTo(unary.expr().span().start());
        assertThat(unary.span().end()).isEqualTo(unary.expr().span().end());
    }

    @Test
    void chainedUnaryOperatorsRespectPrefixAndPostfixPrecedence() {
        final Expr.Unary unary = expression("-x++", Expr.Unary.class);

        assertThat(unary.op()).isEqualTo(UnaryOp.NEGATE);
        assertThat(unary.side()).isEqualTo(UnaryOpSide.PREFIX);
        assertThat(unary.expr()).isInstanceOf(Expr.Unary.class);
        final Expr.Unary postfix = (Expr.Unary) unary.expr();
        assertThat(postfix.op()).isEqualTo(UnaryOp.INCREMENT);
        assertThat(postfix.side()).isEqualTo(UnaryOpSide.POSTFIX);
    }

    @Test
    void missingPrefixOperandProducesSyntaxError() {
        final ParseResult result = parse("-");

        assertThat(result.errors()).isNotEmpty();
        assertThat(result.statements()).isEmpty();
    }

    @Test
    void parsesRadixLiteralAsUnaryOperand() {
        final Expr.Unary unary = expression("-#x2a", Expr.Unary.class);

        assertThat(unary.expr()).isInstanceOf(Expr.Literal.class);
    }

    private <T extends Expr> T expression(final String source, final Class<T> type) {
        final ParseResult result = parse(source);
        assertThat(result.errors()).isEmpty();
        assertThat(result.statements()).hasSize(1);
        return type.cast(((Stmt.Expression) result.statements().get(0)).expr());
    }

    private ParseResult parse(final String source) {
        final LexerResult lexed = new Lexer(Path.of("operator-test.dr"), source).tokenize();
        assertThat(lexed.errors()).isEmpty();
        return new Parser(lexed.tokens()).parse();
    }
}
