package org.dersbian.compiler.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.error.ErrorCode;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.lexer.token.Token;
import org.dersbian.compiler.lexer.token.TokenKind;
import org.dersbian.compiler.syntax.ast.BinaryOp;
import org.dersbian.compiler.syntax.ast.Expr;
import org.dersbian.compiler.syntax.ast.LiteralValue;
import org.dersbian.compiler.syntax.ast.UnaryOp;
import org.dersbian.compiler.syntax.ast.UnaryOpSide;

/**
 * Pratt expression parser.
 *
 * <p>Combines a null-denotation (nud) primary parser with a left-denotation (led) infix loop driven
 * by {@link BindingPower}. Compound assignment operators desugar to {@code Expr.Assign} wrapping
 * {@code Expr.Binary}. Postfix operators, calls and indexing are handled at the highest
 * binding-power level (130).
 */
@SuppressWarnings({
    "PMD.CyclomaticComplexity",
    "PMD.CognitiveComplexity",
    "PMD.ExcessiveImports",
    "PMD.CommentDefaultAccessModifier",
    "PMD.ShortVariable",
    "PMD.TooManyMethods"
})
final class ExpressionParser {

    private final TokenCursor cursor;
    private final List<CompileError.SyntaxError> errors;

    /**
     * Creates an expression parser sharing the given cursor and error sink.
     *
     * @param cursor token cursor (not {@code null})
     * @param errors mutable error sink (not {@code null})
     */
    ExpressionParser(final TokenCursor cursor, final List<CompileError.SyntaxError> errors) {
        this.cursor = Objects.requireNonNull(cursor, "cursor must not be null");
        this.errors = Objects.requireNonNull(errors, "errors must not be null");
    }

    /**
     * Parses a single expression whose binary operator precedence must exceed {@code minBp}.
     *
     * @param minBp minimum left binding power for continuation
     * @return parsed expression; never {@code null} (a null literal is returned on primary error)
     */
    Expr parseExpression(final int minBp) {
        Expr lhs = parsePrimary();
        while (true) {
            final TokenKind kind = cursor.peek().type();
            final Optional<BindingPower> maybeBp = BindingPower.infix(kind);
            if (maybeBp.isEmpty() || maybeBp.get().left() < minBp) {
                break;
            }
            final BindingPower bp = maybeBp.get();
            final Token opToken = cursor.advance();
            final Span opSpan = opToken.span();
            lhs = handleInfix(lhs, opToken, opSpan, bp);
        }
        return lhs;
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    private Expr handleInfix(
            final Expr lhs, final Token opToken, final Span opSpan, final BindingPower bp) {
        final TokenKind kind = opToken.type();
        if (kind == TokenKind.Simple.Operator.PLUS_PLUS
                || kind == TokenKind.Simple.Operator.MINUS_MINUS) {
            return wrapPostfix(lhs, opToken, opSpan);
        }
        if (kind == TokenKind.Simple.Delimiter.OPEN_PAREN) {
            return parseCall(lhs, opSpan);
        }
        if (kind == TokenKind.Simple.Delimiter.OPEN_BRACKET) {
            return parseIndex(lhs, opSpan);
        }
        final Expr rhs = parseExpression(bp.right());
        final Span span = lhs.span().merge(rhs.span());
        if (kind == TokenKind.Simple.Operator.EQUAL) {
            return new Expr.Assign(lhs, rhs, span);
        }
        if (isCompoundAssignment(kind)) {
            final BinaryOp compound = BinaryOp.getOp(opToken);
            final BinaryOp base = baseOp(compound);
            final Expr desugared = new Expr.Binary(lhs, base, rhs, span);
            return new Expr.Assign(lhs, desugared, span);
        }
        return new Expr.Binary(lhs, BinaryOp.getOp(opToken), rhs, span);
    }

    private static boolean isCompoundAssignment(final TokenKind kind) {
        return kind == TokenKind.Simple.Operator.PLUS_EQUAL
                || kind == TokenKind.Simple.Operator.MINUS_EQUAL
                || kind == TokenKind.Simple.Operator.STAR_EQUAL
                || kind == TokenKind.Simple.Operator.SLASH_EQUAL
                || kind == TokenKind.Simple.Operator.PERCENT_EQUAL
                || kind == TokenKind.Simple.Operator.AND_EQUAL
                || kind == TokenKind.Simple.Operator.OR_EQUAL
                || kind == TokenKind.Simple.Operator.XOR_EQUAL
                || kind == TokenKind.Simple.Operator.SHIFT_LEFT_EQUAL
                || kind == TokenKind.Simple.Operator.SHIFT_RIGHT_EQUAL;
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    private static BinaryOp baseOp(final BinaryOp compound) {
        return switch (compound) {
            case ADD_EQUAL -> BinaryOp.ADD;
            case SUBTRACT_EQUAL -> BinaryOp.SUBTRACT;
            case MULTIPLY_EQUAL -> BinaryOp.MULTIPLY;
            case DIVIDE_EQUAL -> BinaryOp.DIVIDE;
            case MODULO_EQUAL -> BinaryOp.MODULO;
            case BITWISE_AND_EQUAL -> BinaryOp.BITWISE_AND;
            case BITWISE_OR_EQUAL -> BinaryOp.BITWISE_OR;
            case BITWISE_XOR_EQUAL -> BinaryOp.BITWISE_XOR;
            case SHIFT_LEFT_EQUAL -> BinaryOp.SHIFT_LEFT;
            case SHIFT_RIGHT_EQUAL -> BinaryOp.SHIFT_RIGHT;
            default -> throw new IllegalStateException("Not a compound assignment: " + compound);
        };
    }

    private Expr wrapPostfix(final Expr lhs, final Token opToken, final Span opSpan) {
        final UnaryOp op =
                opToken.type() == TokenKind.Simple.Operator.PLUS_PLUS
                        ? UnaryOp.INCREMENT
                        : UnaryOp.DECREMENT;
        return new Expr.Unary(op, UnaryOpSide.POSTFIX, lhs, lhs.span().merge(opSpan));
    }

    private Expr parseCall(final Expr callee, final Span openSpan) {
        final List<Expr> args = new ArrayList<>();
        if (!cursor.check(TokenKind.Simple.Delimiter.CLOSE_PAREN)) {
            args.add(parseExpression(0));
            while (cursor.check(TokenKind.Simple.Operator.COMMA)) {
                cursor.advance();
                args.add(parseExpression(0));
            }
        }
        final Token close = cursor.expect(TokenKind.Simple.Delimiter.CLOSE_PAREN, errors);
        return new Expr.Call(callee, args, openSpan.merge(close.span()));
    }

    private Expr parseIndex(final Expr array, final Span openSpan) {
        final Expr index = parseExpression(0);
        final Token close = cursor.expect(TokenKind.Simple.Delimiter.CLOSE_BRACKET, errors);
        return new Expr.ArrayAccess(array, index, openSpan.merge(close.span()));
    }

    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.CognitiveComplexity"})
    private Expr parsePrimary() {
        final Token token = cursor.peek();
        final TokenKind kind = token.type();
        return switch (kind) {
            case TokenKind.Numeric n -> wrapNumeric(token, n.value());
            case TokenKind.Binary b -> wrapNumeric(token, b.value());
            case TokenKind.Octal o -> wrapNumeric(token, o.value());
            case TokenKind.Hexadecimal h -> wrapNumeric(token, h.value());
            case TokenKind.KeywordBool b -> wrapBool(token, b.value());
            case TokenKind.StringLiteral s -> wrapString(token, s.value());
            case TokenKind.CharLiteral c -> wrapChar(token, c.value());
            case TokenKind.Simple.Keyword kw when kw == TokenKind.Simple.Keyword.NULLPTR ->
                    wrapNullptr(token);
            case TokenKind.IdentifierAscii id -> wrapVariable(token, id.value());
            case TokenKind.IdentifierUnicode id -> wrapVariable(token, id.value());
            case TokenKind.Simple.Delimiter.OPEN_PAREN -> parseGrouping(token);
            case TokenKind.Simple.Delimiter.OPEN_BRACE -> parseArrayLiteral(token);
            default -> parsePrefixOrError();
        };
    }

    private Expr wrapNumeric(
            final Token token, final org.dersbian.compiler.lexer.token.number.INumber v) {
        cursor.advance();
        return new Expr.Literal(new LiteralValue.Numeric(v), token.span());
    }

    private Expr wrapBool(final Token token, final boolean v) {
        cursor.advance();
        return new Expr.Literal(new LiteralValue.Bool(v), token.span());
    }

    private Expr wrapString(final Token token, final String v) {
        cursor.advance();
        return new Expr.Literal(new LiteralValue.StringLit(v), token.span());
    }

    private Expr wrapChar(final Token token, final String v) {
        cursor.advance();
        return new Expr.Literal(new LiteralValue.CharLit(v), token.span());
    }

    private Expr wrapNullptr(final Token token) {
        cursor.advance();
        return new Expr.Literal(new LiteralValue.NullPtr(), token.span());
    }

    private Expr wrapVariable(final Token token, final String name) {
        cursor.advance();
        return new Expr.Variable(name, token.span());
    }

    private Expr parseGrouping(final Token openToken) {
        cursor.advance();
        final Expr inner = parseExpression(0);
        final Token close = cursor.expect(TokenKind.Simple.Delimiter.CLOSE_PAREN, errors);
        return new Expr.Grouping(inner, openToken.span().merge(close.span()));
    }

    private Expr parseArrayLiteral(final Token openToken) {
        cursor.advance();
        final List<Expr> elements = new ArrayList<>();
        if (!cursor.check(TokenKind.Simple.Delimiter.CLOSE_BRACE)) {
            elements.add(parseExpression(0));
            while (cursor.check(TokenKind.Simple.Operator.COMMA)) {
                cursor.advance();
                if (cursor.check(TokenKind.Simple.Delimiter.CLOSE_BRACE)) {
                    break;
                }
                elements.add(parseExpression(0));
            }
        }
        final Token close = cursor.expect(TokenKind.Simple.Delimiter.CLOSE_BRACE, errors);
        return new Expr.ArrayLiteral(elements, openToken.span().merge(close.span()));
    }

    private Expr parsePrefixOrError() {
        final Token token = cursor.peek();
        final TokenKind kind = token.type();
        final OptionalInt prefixBp = BindingPower.prefix(kind);
        if (prefixBp.isPresent()) {
            cursor.advance();
            final UnaryOp op = prefixOp(kind);
            final Expr operand = parseExpression(prefixBp.getAsInt());
            return new Expr.Unary(
                    op, UnaryOpSide.PREFIX, operand, token.span().merge(operand.span()));
        }
        errors.add(
                CompileError.syntaxError(
                        ErrorCode.E1006,
                        "Unexpected token in expression: " + kind,
                        token.span(),
                        null));
        cursor.advance();
        return Expr.nullExpr(token.span());
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    private static UnaryOp prefixOp(final TokenKind kind) {
        if (kind == TokenKind.Simple.Operator.MINUS) {
            return UnaryOp.NEGATE;
        }
        if (kind == TokenKind.Simple.Operator.NOT) {
            return UnaryOp.NOT;
        }
        if (kind == TokenKind.Simple.Operator.BITWISE_NOT) {
            return UnaryOp.BITWISE_NOT;
        }
        if (kind == TokenKind.Simple.Operator.PLUS_PLUS) {
            return UnaryOp.INCREMENT;
        }
        if (kind == TokenKind.Simple.Operator.MINUS_MINUS) {
            return UnaryOp.DECREMENT;
        }
        throw new IllegalStateException("Not a prefix operator: " + kind);
    }

    @SuppressWarnings("unused")
    private static Optional<Expr> unused() {
        return Optional.empty();
    }
}
