package org.dersbian.compiler.ast;

/** Operators accepted by a binary expression. */
@SuppressWarnings("PMD.LongVariable")
public enum BinaryOperator {
    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE,
    MODULO,
    EQUAL,
    NOT_EQUAL,
    LESS_THAN,
    LESS_THAN_OR_EQUAL,
    GREATER_THAN,
    GREATER_THAN_OR_EQUAL,
    LOGICAL_AND,
    LOGICAL_OR
}
