package org.dersbian.compiler.syntax.ast;

/** Unary operators supported in syntax expressions. */
public enum UnaryOp {
    /** Negation operator (-). */
    NEGATE,

    /** Logical NOT operator (!). */
    NOT,

    /** Bitwise complement operator (~). */
    BITWISE_NOT,

    /** Increment operator (++). */
    INCREMENT,

    /** Decrement operator (--). */
    DECREMENT
}