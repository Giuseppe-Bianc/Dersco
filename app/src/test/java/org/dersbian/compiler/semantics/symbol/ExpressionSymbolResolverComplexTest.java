package org.dersbian.compiler.semantics.symbol;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.BinaryOp;
import org.dersbian.compiler.syntax.ast.Expr;
import org.dersbian.compiler.syntax.ast.LiteralValue;
import org.dersbian.compiler.syntax.ast.Type;
import org.dersbian.compiler.syntax.ast.UnaryOp;
import org.dersbian.compiler.syntax.ast.UnaryOpSide;
import org.junit.jupiter.api.Test;

/** Covers recursive lexical resolution for compound expression forms. */
class ExpressionSymbolResolverComplexTest {
    /** Synthetic source span for expression fixtures. */
    private static final Span SPAN = Span.point(SourceLocation.create(1, 1, 0));

    @Test
    void resolvesBinaryAndUnaryOperands() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Symbol leftSymbol = declare(table, "left", new Type.I32());
        final Symbol rightSymbol = declare(table, "right", new Type.I32());
        final Expr.Variable left = new Expr.Variable("left", SPAN);
        final Expr.Variable right = new Expr.Variable("right", SPAN);
        final Expr.Binary binary = new Expr.Binary(left, BinaryOp.ADD, right, SPAN);
        final Expr.Unary unary = new Expr.Unary(UnaryOp.NEGATE, UnaryOpSide.PREFIX, binary, SPAN);

        final ExpressionBindingResult result = new ExpressionSymbolResolver(table).resolve(unary);

        assertThat(result.symbolOf(left)).contains(leftSymbol.id());
        assertThat(result.symbolOf(right)).contains(rightSymbol.id());
        assertThat(result.bindings()).hasSize(2);
    }

    @Test
    void resolvesCallCalleeAndArguments() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Symbol function = declare(table, "callee", new Type.I32());
        final Symbol argument = declare(table, "argument", new Type.I32());
        final Expr.Variable callee = new Expr.Variable("callee", SPAN);
        final Expr.Variable argumentReference = new Expr.Variable("argument", SPAN);
        final Expr.Call call = new Expr.Call(callee, List.of(argumentReference), SPAN);

        final ExpressionBindingResult result = new ExpressionSymbolResolver(table).resolve(call);

        assertThat(result.symbolOf(callee)).contains(function.id());
        assertThat(result.symbolOf(argumentReference)).contains(argument.id());
        assertThat(result.bindings()).hasSize(2);
    }

    @Test
    void resolvesArrayAndIndexExpressions() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Symbol arraySymbol = declare(table, "array", new Type.I32());
        final Symbol indexSymbol = declare(table, "index", new Type.I32());
        final Expr.Variable array = new Expr.Variable("array", SPAN);
        final Expr.Variable index = new Expr.Variable("index", SPAN);
        final Expr.ArrayAccess access = new Expr.ArrayAccess(array, index, SPAN);

        final ExpressionBindingResult result = new ExpressionSymbolResolver(table).resolve(access);

        assertThat(result.symbolOf(array)).contains(arraySymbol.id());
        assertThat(result.symbolOf(index)).contains(indexSymbol.id());
        assertThat(result.bindings()).hasSize(2);
    }

    @Test
    void literalsProduceNoBindings() {
        final DefaultSymbolTable table = new DefaultSymbolTable();
        final Expr literal = new Expr.Literal(new LiteralValue.Bool(true), SPAN);

        final ExpressionBindingResult result = new ExpressionSymbolResolver(table).resolve(literal);

        assertThat(result.bindings()).isEmpty();
    }

    private static Symbol declare(
            final DefaultSymbolTable table, final String name, final Type type) {
        final DeclarationResult result =
                table.declareVariable(name, type, Mutability.IMMUTABLE, SPAN);
        return ((DeclarationResult.Declared) result).symbol();
    }
}
