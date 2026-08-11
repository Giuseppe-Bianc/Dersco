package org.dersbian.compiler.syntax.ast;

import java.util.Objects;

/** Representation of primitive, user-defined, composite, and special types. */
@SuppressWarnings({"PMD.ShortClassName"})
public sealed interface Type
        permits Type.I8,
                Type.I16,
                Type.I32,
                Type.I64,
                Type.U8,
                Type.U16,
                Type.U32,
                Type.U64,
                Type.F32,
                Type.F64,
                Type.Char,
                Type.StringT,
                Type.Bool,
                Type.Custom,
                Type.Array,
                Type.Vector,
                Type.VoidT,
                Type.NullPtr {

    /** Signed 8-bit integer type. */
    record I8() implements Type {}

    /** Signed 16-bit integer type. */
    record I16() implements Type {}

    /** Signed 32-bit integer type. */
    record I32() implements Type {}

    /** Signed 64-bit integer type. */
    record I64() implements Type {}

    /** Unsigned 8-bit integer type. */
    record U8() implements Type {}

    /** Unsigned 16-bit integer type. */
    record U16() implements Type {}

    /** Unsigned 32-bit integer type. */
    record U32() implements Type {}

    /** Unsigned 64-bit integer type. */
    record U64() implements Type {}

    /** 32-bit floating point type. */
    record F32() implements Type {}

    /** 64-bit floating point type. */
    record F64() implements Type {}

    /** Character type. */
    record Char() implements Type {}

    /** String type. */
    record StringT() implements Type {}

    /** Boolean type. */
    record Bool() implements Type {}

    /**
     * User-defined custom type.
     *
     * @param name type identifier
     */
    record Custom(String name) implements Type {
        public Custom {
            Objects.requireNonNull(name, "name must not be null");
        }
    }

    /**
     * Fixed-size array type.
     *
     * @param elementType array element type
     * @param size expression evaluating to array capacity
     */
    record Array(Type elementType, Expr size) implements Type {
        public Array {
            Objects.requireNonNull(elementType, "elementType must not be null");
            Objects.requireNonNull(size, "size must not be null");
        }
    }

    /**
     * Dynamic vector type.
     *
     * @param elementType vector element type
     */
    record Vector(Type elementType) implements Type {
        public Vector {
            Objects.requireNonNull(elementType, "elementType must not be null");
        }
    }

    /** Void type. */
    record VoidT() implements Type {}

    /** Null pointer type. */
    record NullPtr() implements Type {}
}
