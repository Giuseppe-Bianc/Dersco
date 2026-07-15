package org.dersbian.compiler.lexer.token.number;

/** Represents a numeric literal value. */
@SuppressWarnings({"AvoidCommonTypeNames", "checkstyle:AbbreviationAsWordInName"})
public sealed interface INumber {

    /** Integer value. */
    record IntegerValue(long value, String suffix) implements INumber {

        @Override
        public String toString() {
            return suffix == null ? Long.toString(value) : value + suffix;
        }
    }

    /** Floating-point value. */
    record FloatingValue(double value, String suffix) implements INumber {

        @Override
        public String toString() {
            return suffix == null ? Double.toString(value) : value + suffix;
        }
    }
}
