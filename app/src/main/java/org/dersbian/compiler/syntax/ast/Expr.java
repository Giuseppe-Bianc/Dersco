package org.dersbian.compiler.syntax.ast;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.lexer.token.number.INumber;

/** Abstract syntax tree node representing an expression. */
@SuppressWarnings({"PMD.ShortClassName", "PMD.ShortVariable", "PMD.AvoidDuplicateLiterals"})
public sealed interface Expr
        permits Expr.Binary,
                Expr.Unary,
                Expr.Grouping,
                Expr.Literal,
                Expr.ArrayLiteral,
                Expr.Variable,
                Expr.Assign,
                Expr.Call,
                Expr.ArrayAccess {

    /**
     * Returns the source span for this expression.
     *
     * @return source span
     */
    Span span();

    /**
     * Binary operation expression.
     *
     * @param left left operand
     * @param op binary operator
     * @param right right operand
     * @param span source extent
     */
    record Binary(Expr left, BinaryOp op, Expr right, Span span) implements Expr {
        public Binary {
            Objects.requireNonNull(left, "left must not be null");
            Objects.requireNonNull(op, "op must not be null");
            Objects.requireNonNull(right, "right must not be null");
            Objects.requireNonNull(span, "span must not be null");
        }
    }

    /**
     * Unary operation expression.
     *
     * @param op unary operator
     * @param side whether the operator is applied in prefix or postfix position
     * @param expr operand expression
     * @param span source extent
     */
    record Unary(UnaryOp op, UnaryOpSide side, Expr expr, Span span) implements Expr {
        public Unary {
            Objects.requireNonNull(op, "op must not be null");
            Objects.requireNonNull(expr, "expr must not be null");
            Objects.requireNonNull(span, "span must not be null");
        }
    }

    /**
     * Parenthesized/grouped expression.
     *
     * @param expr inner expression
     * @param span source extent
     */
    record Grouping(Expr expr, Span span) implements Expr {
        public Grouping {
            Objects.requireNonNull(expr, "expr must not be null");
            Objects.requireNonNull(span, "span must not be null");
        }
    }

    /**
     * Literal value expression.
     *
     * @param value literal value payload
     * @param span source extent
     */
    record Literal(LiteralValue value, Span span) implements Expr {
        public Literal {
            Objects.requireNonNull(value, "value must not be null");
            Objects.requireNonNull(span, "span must not be null");
        }
    }

    /**
     * Array literal expression containing zero or more element expressions.
     *
     * @param elements list of element expressions
     * @param span source extent
     */
    record ArrayLiteral(List<Expr> elements, Span span) implements Expr {
        public ArrayLiteral {
            elements = List.copyOf(Objects.requireNonNull(elements, "elements must not be null"));
            Objects.requireNonNull(span, "span must not be null");
        }
    }

    /**
     * Variable reference expression.
     *
     * @param name identifier name
     * @param span source extent
     */
    record Variable(String name, Span span) implements Expr {
        public Variable {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(span, "span must not be null");
        }
    }

    /**
     * Assignment expression.
     *
     * @param target expression representing assignment target
     * @param value expression representing right-hand side value
     * @param span source extent
     */
    record Assign(Expr target, Expr value, Span span) implements Expr {
        public Assign {
            Objects.requireNonNull(target, "target must not be null");
            Objects.requireNonNull(value, "value must not be null");
            Objects.requireNonNull(span, "span must not be null");
        }
    }

    /**
     * Function or method call expression.
     *
     * @param callee expression producing the callable value
     * @param arguments argument expressions passed to the call
     * @param span source extent
     */
    record Call(Expr callee, List<Expr> arguments, Span span) implements Expr {
        public Call {
            Objects.requireNonNull(callee, "callee must not be null");
            arguments =
                    List.copyOf(Objects.requireNonNull(arguments, "arguments must not be null"));
            Objects.requireNonNull(span, "span must not be null");
        }
    }

    /**
     * Array indexing expression.
     *
     * @param array expression producing the indexed array
     * @param index expression producing the element index
     * @param span source extent
     */
    record ArrayAccess(Expr array, Expr index, Span span) implements Expr {
        public ArrayAccess {
            Objects.requireNonNull(array, "array must not be null");
            Objects.requireNonNull(index, "index must not be null");
            Objects.requireNonNull(span, "span must not be null");
        }
    }

    /**
     * Creates a null literal expression.
     *
     * @param span source extent
     * @return null literal expression
     */
    static Expr nullExpr(final Span span) {
        return new Literal(new LiteralValue.NullPtr(), span);
    }

    /**
     * Helper method for creating a numeric literal expression.
     *
     * @param value number payload
     * @param span source extent
     * @return optional containing the literal expression
     */
    static Optional<Expr> newNumberLiteral(final INumber value, final Span span) {
        return Optional.of(new Literal(new LiteralValue.Numeric(value), span));
    }

    /**
     * Helper method for creating a boolean literal expression.
     *
     * @param value boolean payload
     * @param span source extent
     * @return optional containing the literal expression
     */
    static Optional<Expr> newBoolLiteral(final boolean value, final Span span) {
        return Optional.of(new Literal(new LiteralValue.Bool(value), span));
    }

    /**
     * Helper method for creating a nullptr literal expression.
     *
     * @param span source extent
     * @return optional containing the literal expression
     */
    static Optional<Expr> newNullptrLiteral(final Span span) {
        return Optional.of(nullExpr(span));
    }

    /**
     * Helper method for creating a string literal expression.
     *
     * @param value string payload
     * @param span source extent
     * @return optional containing the literal expression
     */
    static Optional<Expr> newStringLiteral(final String value, final Span span) {
        return Optional.of(new Literal(new LiteralValue.StringLit(value), span));
    }

    /**
     * Helper method for creating a character literal expression.
     *
     * @param value character payload
     * @param span source extent
     * @return optional containing the literal expression
     */
    static Optional<Expr> newCharLiteral(final String value, final Span span) {
        return Optional.of(new Literal(new LiteralValue.CharLit(value), span));
    }
}
