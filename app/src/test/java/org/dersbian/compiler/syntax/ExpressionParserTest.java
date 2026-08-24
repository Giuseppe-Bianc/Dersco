package org.dersbian.compiler.syntax;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.lexer.Lexer;
import org.dersbian.compiler.lexer.LexerResult;
import org.dersbian.compiler.syntax.ast.BinaryOp;
import org.dersbian.compiler.syntax.ast.Expr;
import org.dersbian.compiler.syntax.ast.LiteralValue;
import org.dersbian.compiler.syntax.ast.UnaryOp;
import org.dersbian.compiler.syntax.ast.UnaryOpSide;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.TooManyMethods",
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.ExcessiveImports",
    "PMD.ExcessivePublicCount",
    "PMD.AvoidDuplicateLiterals"
})
class ExpressionParserTest {

    private final List<CompileError.SyntaxError> errors = new ArrayList<>();

    private ExpressionParser parserFor(final String source) {
        final Lexer lexer = new Lexer(Path.of("test.dr"), source + "\n");
        final LexerResult result = lexer.tokenize();
        final TokenCursor cursor = new TokenCursor(result.tokens());
        return new ExpressionParser(cursor, errors);
    }

    private Expr parse(final String source) {
        errors.clear();
        return parserFor(source).parseExpression(0);
    }

    // Literals

    @Test
    void integerLiteralProducesExprLiteral() {
        final Expr result = parse("1i32");
        assertThat(result).isInstanceOf(Expr.Literal.class);
        assertThat(((Expr.Literal) result).value()).isInstanceOf(LiteralValue.Numeric.class);
    }

    @Test
    void boolTrueLiteralProducesExprLiteralBoolTrue() {
        final Expr result = parse("true");
        assertThat(result).isInstanceOf(Expr.Literal.class);
        assertThat(((Expr.Literal) result).value()).isEqualTo(new LiteralValue.Bool(true));
    }

    @Test
    void boolFalseLiteralProducesExprLiteralBoolFalse() {
        final Expr result = parse("false");
        assertThat(result).isInstanceOf(Expr.Literal.class);
        assertThat(((Expr.Literal) result).value()).isEqualTo(new LiteralValue.Bool(false));
    }

    @Test
    void stringLiteralProducesStringLit() {
        final Expr result = parse("\"hello\"");
        assertThat(result).isInstanceOf(Expr.Literal.class);
        assertThat(((Expr.Literal) result).value()).isEqualTo(new LiteralValue.StringLit("hello"));
    }

    @Test
    void charLiteralProducesCharLit() {
        final Expr result = parse("'a'");
        assertThat(result).isInstanceOf(Expr.Literal.class);
        assertThat(((Expr.Literal) result).value()).isEqualTo(new LiteralValue.CharLit("a"));
    }

    @Test
    void nullptrLiteralProducesNullPtr() {
        final Expr result = parse("nullptr");
        assertThat(result).isInstanceOf(Expr.Literal.class);
        assertThat(((Expr.Literal) result).value()).isInstanceOf(LiteralValue.NullPtr.class);
    }

    @Test
    void identifierProducesExprVariable() {
        final Expr result = parse("myVar");
        assertThat(result).isInstanceOf(Expr.Variable.class);
        assertThat(((Expr.Variable) result).name()).isEqualTo("myVar");
    }

    @Test
    void groupingProducesExprGrouping() {
        final Expr result = parse("(1i32)");
        assertThat(result).isInstanceOf(Expr.Grouping.class);
    }

    // Additive

    @Test
    void additionProducesBinaryAdd() {
        final Expr result = parse("1i32 + 2i32");
        assertThat(result).isInstanceOf(Expr.Binary.class);
        assertThat(((Expr.Binary) result).op()).isEqualTo(BinaryOp.ADD);
    }

    @Test
    void subtractionProducesBinarySubtract() {
        final Expr result = parse("5i32 - 3i32");
        assertThat(result).isInstanceOf(Expr.Binary.class);
        assertThat(((Expr.Binary) result).op()).isEqualTo(BinaryOp.SUBTRACT);
    }

    // Multiplicative beats additive

    @Test
    void multiplicationBindsTighterThanAddition() {
        final Expr result = parse("1i32 + 2i32 * 3i32");
        assertThat(result).isInstanceOf(Expr.Binary.class);
        final Expr.Binary outer = (Expr.Binary) result;
        assertThat(outer.op()).isEqualTo(BinaryOp.ADD);
        assertThat(outer.right()).isInstanceOf(Expr.Binary.class);
        assertThat(((Expr.Binary) outer.right()).op()).isEqualTo(BinaryOp.MULTIPLY);
    }

    // Left-associativity

    @Test
    void subtractionIsLeftAssociative() {
        final Expr result = parse("a - b - c");
        assertThat(result).isInstanceOf(Expr.Binary.class);
        final Expr.Binary outer = (Expr.Binary) result;
        assertThat(outer.op()).isEqualTo(BinaryOp.SUBTRACT);
        assertThat(outer.left()).isInstanceOf(Expr.Binary.class);
        assertThat(((Expr.Binary) outer.left()).op()).isEqualTo(BinaryOp.SUBTRACT);
        assertThat(outer.right()).isInstanceOf(Expr.Variable.class);
    }

    @Test
    void additionIsLeftAssociative() {
        final Expr result = parse("a + b + c");
        assertThat(result).isInstanceOf(Expr.Binary.class);
        final Expr.Binary outer = (Expr.Binary) result;
        assertThat(outer.op()).isEqualTo(BinaryOp.ADD);
        assertThat(outer.left()).isInstanceOf(Expr.Binary.class);
        assertThat(((Expr.Binary) outer.left()).op()).isEqualTo(BinaryOp.ADD);
    }

    // Right-associativity - assignments (SC-003)

    @Test
    void simpleAssignmentIsRightAssociative() {
        final Expr result = parse("a = b = c");
        assertThat(result).isInstanceOf(Expr.Assign.class);
        final Expr.Assign outer = (Expr.Assign) result;
        assertThat(outer.target()).isInstanceOf(Expr.Variable.class);
        assertThat(outer.value()).isInstanceOf(Expr.Assign.class);
    }

    @Test
    void compoundAssignPlusDesugarsToBinaryAdd() {
        final Expr result = parse("a += 5i32");
        assertThat(result).isInstanceOf(Expr.Assign.class);
        final Expr.Assign assign = (Expr.Assign) result;
        assertThat(assign.value()).isInstanceOf(Expr.Binary.class);
        assertThat(((Expr.Binary) assign.value()).op()).isEqualTo(BinaryOp.ADD);
    }

    @Test
    void compoundAssignMinusDesugarsToBinarySubtract() {
        final Expr result = parse("a -= 5i32");
        final Expr.Assign assign = (Expr.Assign) result;
        assertThat(((Expr.Binary) assign.value()).op()).isEqualTo(BinaryOp.SUBTRACT);
    }

    @Test
    void compoundAssignStarDesugarsToBinaryMultiply() {
        final Expr result = parse("a *= 5i32");
        final Expr.Assign assign = (Expr.Assign) result;
        assertThat(((Expr.Binary) assign.value()).op()).isEqualTo(BinaryOp.MULTIPLY);
    }

    @Test
    void compoundAssignSlashDesugarsToBinaryDivide() {
        final Expr result = parse("a /= 5i32");
        final Expr.Assign assign = (Expr.Assign) result;
        assertThat(((Expr.Binary) assign.value()).op()).isEqualTo(BinaryOp.DIVIDE);
    }

    @Test
    void compoundAssignPercentDesugarsToBinaryModulo() {
        final Expr result = parse("a %= 5i32");
        final Expr.Assign assign = (Expr.Assign) result;
        assertThat(((Expr.Binary) assign.value()).op()).isEqualTo(BinaryOp.MODULO);
    }

    @Test
    void compoundAssignAndEqualDesugarsToBitwiseAnd() {
        final Expr result = parse("a &= 5i32");
        final Expr.Assign assign = (Expr.Assign) result;
        assertThat(((Expr.Binary) assign.value()).op()).isEqualTo(BinaryOp.BITWISE_AND);
    }

    @Test
    void compoundAssignOrEqualDesugarsToBitwiseOr() {
        final Expr result = parse("a |= 5i32");
        final Expr.Assign assign = (Expr.Assign) result;
        assertThat(((Expr.Binary) assign.value()).op()).isEqualTo(BinaryOp.BITWISE_OR);
    }

    @Test
    void compoundAssignXorEqualDesugarsToBitwiseXor() {
        final Expr result = parse("a ^= 5i32");
        final Expr.Assign assign = (Expr.Assign) result;
        assertThat(((Expr.Binary) assign.value()).op()).isEqualTo(BinaryOp.BITWISE_XOR);
    }

    @Test
    void compoundAssignShiftLeftEqualDesugarsToShiftLeft() {
        final Expr result = parse("a <<= 1i32");
        final Expr.Assign assign = (Expr.Assign) result;
        assertThat(((Expr.Binary) assign.value()).op()).isEqualTo(BinaryOp.SHIFT_LEFT);
    }

    @Test
    void compoundAssignShiftRightEqualDesugarsToShiftRight() {
        final Expr result = parse("a >>= 1i32");
        final Expr.Assign assign = (Expr.Assign) result;
        assertThat(((Expr.Binary) assign.value()).op()).isEqualTo(BinaryOp.SHIFT_RIGHT);
    }

    // Cross-level precedence (SC-002)

    @Test
    void andBindsTighterThanOr() {
        final Expr result = parse("a && b || c");
        final Expr.Binary outer = (Expr.Binary) result;
        assertThat(outer.op()).isEqualTo(BinaryOp.OR);
        assertThat(outer.left()).isInstanceOf(Expr.Binary.class);
        assertThat(((Expr.Binary) outer.left()).op()).isEqualTo(BinaryOp.AND);
    }

    @Test
    void bitwiseOrBindsTighterThanLogicalOr() {
        final Expr result = parse("a | b || c");
        final Expr.Binary outer = (Expr.Binary) result;
        assertThat(outer.op()).isEqualTo(BinaryOp.OR);
        assertThat(((Expr.Binary) outer.left()).op()).isEqualTo(BinaryOp.BITWISE_OR);
    }

    @Test
    void bitwiseXorBindsTighterThanBitwiseOr() {
        final Expr result = parse("a ^ b | c");
        final Expr.Binary outer = (Expr.Binary) result;
        assertThat(outer.op()).isEqualTo(BinaryOp.BITWISE_OR);
        assertThat(((Expr.Binary) outer.left()).op()).isEqualTo(BinaryOp.BITWISE_XOR);
    }

    @Test
    void bitwiseAndBindsTighterThanBitwiseXor() {
        final Expr result = parse("a & b ^ c");
        final Expr.Binary outer = (Expr.Binary) result;
        assertThat(outer.op()).isEqualTo(BinaryOp.BITWISE_XOR);
        assertThat(((Expr.Binary) outer.left()).op()).isEqualTo(BinaryOp.BITWISE_AND);
    }

    @Test
    void equalityBindsTighterThanBitwiseAnd() {
        final Expr result = parse("a == b & c");
        final Expr.Binary outer = (Expr.Binary) result;
        assertThat(outer.op()).isEqualTo(BinaryOp.BITWISE_AND);
        assertThat(((Expr.Binary) outer.left()).op()).isEqualTo(BinaryOp.EQUAL);
    }

    @Test
    void relationalBindsTighterThanEquality() {
        final Expr result = parse("a < b == c");
        final Expr.Binary outer = (Expr.Binary) result;
        assertThat(outer.op()).isEqualTo(BinaryOp.EQUAL);
        assertThat(((Expr.Binary) outer.left()).op()).isEqualTo(BinaryOp.LESS);
    }

    @Test
    void shiftBindsTighterThanAdditive() {
        final Expr result = parse("a + b << c");
        final Expr.Binary outer = (Expr.Binary) result;
        assertThat(outer.op()).isEqualTo(BinaryOp.SHIFT_LEFT);
        assertThat(((Expr.Binary) outer.left()).op()).isEqualTo(BinaryOp.ADD);
    }

    // Unary

    @Test
    void prefixNegateProducesUnaryNegate() {
        final Expr result = parse("-a");
        assertThat(result).isInstanceOf(Expr.Unary.class);
        final Expr.Unary u = (Expr.Unary) result;
        assertThat(u.op()).isEqualTo(UnaryOp.NEGATE);
        assertThat(u.side()).isEqualTo(UnaryOpSide.PREFIX);
    }

    @Test
    void prefixNotProducesUnaryNot() {
        final Expr result = parse("!a");
        final Expr.Unary u = (Expr.Unary) result;
        assertThat(u.op()).isEqualTo(UnaryOp.NOT);
        assertThat(u.side()).isEqualTo(UnaryOpSide.PREFIX);
    }

    @Test
    void prefixBitwiseNotProducesUnaryBitwiseNot() {
        final Expr result = parse("~a");
        final Expr.Unary u = (Expr.Unary) result;
        assertThat(u.op()).isEqualTo(UnaryOp.BITWISE_NOT);
        assertThat(u.side()).isEqualTo(UnaryOpSide.PREFIX);
    }

    @Test
    void prefixIncrementProducesUnaryIncrement() {
        final Expr result = parse("++a");
        final Expr.Unary u = (Expr.Unary) result;
        assertThat(u.op()).isEqualTo(UnaryOp.INCREMENT);
        assertThat(u.side()).isEqualTo(UnaryOpSide.PREFIX);
    }

    @Test
    void prefixDecrementProducesUnaryDecrement() {
        final Expr result = parse("--a");
        final Expr.Unary u = (Expr.Unary) result;
        assertThat(u.op()).isEqualTo(UnaryOp.DECREMENT);
        assertThat(u.side()).isEqualTo(UnaryOpSide.PREFIX);
    }

    @Test
    void postfixIncrementProducesUnaryIncrementPostfix() {
        final Expr result = parse("a++");
        final Expr.Unary u = (Expr.Unary) result;
        assertThat(u.op()).isEqualTo(UnaryOp.INCREMENT);
        assertThat(u.side()).isEqualTo(UnaryOpSide.POSTFIX);
    }

    @Test
    void postfixDecrementProducesUnaryDecrementPostfix() {
        final Expr result = parse("a--");
        final Expr.Unary u = (Expr.Unary) result;
        assertThat(u.op()).isEqualTo(UnaryOp.DECREMENT);
        assertThat(u.side()).isEqualTo(UnaryOpSide.POSTFIX);
    }

    @Test
    void chainedPrefixUnaryRightAssociative() {
        final Expr result = parse("!-a");
        final Expr.Unary outer = (Expr.Unary) result;
        assertThat(outer.op()).isEqualTo(UnaryOp.NOT);
        assertThat(outer.expr()).isInstanceOf(Expr.Unary.class);
        final Expr.Unary inner = (Expr.Unary) outer.expr();
        assertThat(inner.op()).isEqualTo(UnaryOp.NEGATE);
        assertThat(inner.expr()).isInstanceOf(Expr.Variable.class);
    }

    // Call and Index

    @Test
    void functionCallProducesExprCall() {
        final Expr result = parse("f()");
        assertThat(result).isInstanceOf(Expr.Call.class);
        assertThat(((Expr.Call) result).arguments()).isEmpty();
    }

    @Test
    void functionCallWithArgsProducesExprCall() {
        final Expr result = parse("f(1i32, 2i32)");
        assertThat(result).isInstanceOf(Expr.Call.class);
        assertThat(((Expr.Call) result).arguments()).hasSize(2);
    }

    @Test
    void arrayIndexProducesExprArrayAccess() {
        final Expr result = parse("a[0i32]");
        assertThat(result).isInstanceOf(Expr.ArrayAccess.class);
    }

    @Test
    void emptyArrayLiteralProducesExprArrayLiteralEmpty() {
        final Expr result = parse("{}");
        assertThat(result).isInstanceOf(Expr.ArrayLiteral.class);
        assertThat(((Expr.ArrayLiteral) result).elements()).isEmpty();
    }

    @Test
    void nonEmptyArrayLiteralProducesExprArrayLiteralWithElements() {
        final Expr result = parse("{1i32, 2i32, 3i32}");
        assertThat(result).isInstanceOf(Expr.ArrayLiteral.class);
        assertThat(((Expr.ArrayLiteral) result).elements()).hasSize(3);
    }

    // Edge cases

    @Test
    void eofMidExpressionAddsError() {
        errors.clear();
        final ExpressionParser p = parserFor("+");
        p.parseExpression(0);
        assertThat(errors).isNotEmpty();
    }

    @Test
    void deeplyNestedGroupingDoesNotStackOverflow() {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append('(');
        }
        sb.append("1i32");
        for (int i = 0; i < 50; i++) {
            sb.append(')');
        }
        final Expr result = parse(sb.toString());
        assertThat(result).isInstanceOf(Expr.Grouping.class);
    }
}
