package org.dersbian.compiler.syntax.ast;

import java.util.Objects;
import org.dersbian.compiler.lexer.token.number.INumber;

/** Value of a literal expression. */
public sealed interface LiteralValue
        permits LiteralValue.Numeric,
                LiteralValue.StringLit,
                LiteralValue.CharLit,
                LiteralValue.Bool,
                LiteralValue.NullPtr {

    /**
     * Numeric literal value wrapper.
     *
     * @param value underlying number representation
     */
    record Numeric(INumber value) implements LiteralValue {
        public Numeric {
            Objects.requireNonNull(value, "value must not be null");
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
    }

    /**
     * Boolean literal value wrapper.
     *
     * @param value boolean payload
     */
    record Bool(boolean value) implements LiteralValue {}

    /** Null pointer literal value representation. */
    record NullPtr() implements LiteralValue {}
}
