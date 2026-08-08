package org.dersbian.compiler.syntax.ast;

import java.util.Objects;
import org.dersbian.compiler.lexer.token.number.INumber;
import org.dersbian.compiler.syntax.ast.visitor.LiteralValueVisitor;

/** Value of a literal expression. */
public sealed interface LiteralValue
        permits LiteralValue.Numeric,
                LiteralValue.StringLit,
                LiteralValue.CharLit,
                LiteralValue.Bool,
                LiteralValue.NullPtr {

    /**
     * Accepts a {@link LiteralValueVisitor}, dispatching to the method that corresponds to the
     * concrete variant of this literal value.
     *
     * @param <R> result type of the visitor
     * @param <C> context type threaded through the traversal
     * @param visitor visitor instance to dispatch to
     * @param ctx traversal context
     * @return result produced by the visitor for this value
     */
    <R, C> R accept(LiteralValueVisitor<R, C> visitor, C ctx);

    /**
     * Numeric literal value wrapper.
     *
     * @param value underlying number representation
     */
    record Numeric(INumber value) implements LiteralValue {
        public Numeric {
            Objects.requireNonNull(value, "value must not be null");
        }

        @Override
        public <R, C> R accept(final LiteralValueVisitor<R, C> visitor, final C ctx) {
            return visitor.visitNumeric(this, ctx);
        }
    }

    /**
     * String literal value wrapper.
     *
     * @param value string payload
     */
    record StringLit(String value) implements LiteralValue {
        public StringLit {
            Objects.requireNonNull(value, "value must not be null");
        }

        @Override
        public <R, C> R accept(final LiteralValueVisitor<R, C> visitor, final C ctx) {
            return visitor.visitStringLit(this, ctx);
        }
    }

    /**
     * Character literal value wrapper.
     *
     * @param value character payload
     */
    record CharLit(String value) implements LiteralValue {
        public CharLit {
            Objects.requireNonNull(value, "value must not be null");
        }

        @Override
        public <R, C> R accept(final LiteralValueVisitor<R, C> visitor, final C ctx) {
            return visitor.visitCharLit(this, ctx);
        }
    }

    /**
     * Boolean literal value wrapper.
     *
     * @param value boolean payload
     */
    record Bool(boolean value) implements LiteralValue {
        @Override
        public <R, C> R accept(final LiteralValueVisitor<R, C> visitor, final C ctx) {
            return visitor.visitBool(this, ctx);
        }
    }

    /** Null pointer literal value representation. */
    record NullPtr() implements LiteralValue {
        @Override
        public <R, C> R accept(final LiteralValueVisitor<R, C> visitor, final C ctx) {
            return visitor.visitNullPtr(this, ctx);
        }
    }
}
