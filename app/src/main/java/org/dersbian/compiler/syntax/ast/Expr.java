package org.dersbian.compiler.syntax.ast;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.lexer.token.number.INumber;
import org.dersbian.compiler.syntax.ast.visitor.ExprVisitor;

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
     * Accepts an {@link ExprVisitor}, dispatching to the method that corresponds to the concrete
     * type of this node (double dispatch).
     *
     * <p>Each permitted record implements this method by calling the specific {@code visit}
     * overload on {@code visitor}, passing itself as the first argument. No {@code instanceof}
     * chain is involved: the concrete type is known at compile time within each record body.
     *
     * @param <R> result type of the visitor
     * @param <C> context type threaded through the traversal
     * @param visitor visitor instance to dispatch to
     * @param ctx traversal context, may be {@code null} if the visitor does not use it
     * @return result produced by the visitor for this node
     */
    <R, C> R accept(ExprVisitor<R, C> visitor, C ctx);

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

        /**
         * {@inheritDoc}
         *
         * <p>Does <em>not</em> automatically recurse into {@link #left()} or {@link #right()}.
         * Child traversal is the responsibility of the visitor implementation, which calls {@code
         * left().accept(visitor, ctx)} and {@code right().accept(visitor, ctx)} as needed. This
         * separation keeps control flow in the visitor, not scattered across node classes.
         */
        @Override
        public <R, C> R accept(final ExprVisitor<R, C> visitor, final C ctx) {
            return visitor.visitBinary(this, ctx);
        }
    }

    /**
     * Unary operation expression.
     *
     * @param op unary operator
     * @param expr operand expression
     * @param span source extent
     */
    record Unary(UnaryOp op, UnaryOpSide side, Expr expr, Span span) implements Expr {
        public Unary {
            Objects.requireNonNull(op, "op must not be null");
            Objects.requireNonNull(expr, "expr must not be null");
            Objects.requireNonNull(span, "span must not be null");
        }

        @Override
        public <R, C> R accept(final ExprVisitor<R, C> visitor, final C ctx) {
            return visitor.visitUnary(this, ctx);
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

        @Override
        public <R, C> R accept(final ExprVisitor<R, C> visitor, final C ctx) {
            return visitor.visitGrouping(this, ctx);
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

        @Override
        public <R, C> R accept(final ExprVisitor<R, C> visitor, final C ctx) {
            return visitor.visitLiteral(this, ctx);
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

        @Override
        public <R, C> R accept(final ExprVisitor<R, C> visitor, final C ctx) {
            return visitor.visitArrayLiteral(this, ctx);
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

        @Override
        public <R, C> R accept(final ExprVisitor<R, C> visitor, final C ctx) {
            return visitor.visitVariable(this, ctx);
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

        @Override
        public <R, C> R accept(final ExprVisitor<R, C> visitor, final C ctx) {
            return visitor.visitAssign(this, ctx);
        }
    }

    /**
     * Function or method call expression.
     *
     * @param callee expression representing function being called
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

        @Override
        public <R, C> R accept(final ExprVisitor<R, C> visitor, final C ctx) {
            return visitor.visitCall(this, ctx);
        }
    }

    /**
     * Array indexing expression.
     *
     * @param array expression representing array container
     * @param index expression representing element index
     * @param span source extent
     */
    record ArrayAccess(Expr array, Expr index, Span span) implements Expr {
        public ArrayAccess {
            Objects.requireNonNull(array, "array must not be null");
            Objects.requireNonNull(index, "index must not be null");
            Objects.requireNonNull(span, "span must not be null");
        }

        @Override
        public <R, C> R accept(final ExprVisitor<R, C> visitor, final C ctx) {
            return visitor.visitArrayAccess(this, ctx);
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
