package org.dersbian.compiler.ast;

/** Identifies every concrete syntax-tree node. */
@SuppressWarnings("PMD.LongVariable")
public enum NodeKind {
    PROGRAM,
    FUNCTION_DECL,
    PARAM_DECL,
    VARIABLE_DECL,
    CLASS_DECL,
    BLOCK,
    IF_STMT,
    WHILE_STMT,
    FOR_STMT,
    RETURN_STMT,
    BREAK_STMT,
    CONTINUE_STMT,
    DECL_STMT,
    EXPR_STMT,
    INT_LITERAL,
    FLOAT_LITERAL,
    BOOL_LITERAL,
    STRING_LITERAL,
    NULL_LITERAL,
    NAME_EXPR,
    BINARY_EXPR,
    UNARY_EXPR,
    ASSIGN_EXPR,
    CALL_EXPR,
    MEMBER_ACCESS_EXPR,
    INDEX_EXPR,
    NEW_EXPR,
    CAST_EXPR,
    PRIMITIVE_TYPE_NODE,
    ARRAY_TYPE_NODE,
    CLASS_TYPE_NODE
}
