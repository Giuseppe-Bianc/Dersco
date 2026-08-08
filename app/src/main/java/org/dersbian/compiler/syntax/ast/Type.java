package org.dersbian.compiler.syntax.ast;

import java.util.Objects;
import org.dersbian.compiler.syntax.ast.visitor.TypeVisitor;

/** Representation of primitive, user-defined, composite, and special types. */
@SuppressWarnings({"PMD.ShortClassName", "PMD.ShortVariable"})
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

    /**
     * Accepts a {@link TypeVisitor}, dispatching to the method that corresponds to the concrete
     * type of this node.
     *
     * @param <R> result type of the visitor
     * @param <C> context type threaded through the traversal
     * @param visitor visitor instance to dispatch to
     * @param ctx traversal context
     * @return result produced by the visitor for this node
     */
    <R, C> R accept(TypeVisitor<R, C> visitor, C ctx);

    /** Signed 8-bit integer type. */
    record I8() implements Type {
        @Override
        public <R, C> R accept(final TypeVisitor<R, C> v, final C ctx) {
            return v.visitI8(this, ctx);
        }
    }

    /** Signed 16-bit integer type. */
    record I16() implements Type {
        @Override
        public <R, C> R accept(final TypeVisitor<R, C> v, final C ctx) {
            return v.visitI16(this, ctx);
        }
    }

    /** Signed 32-bit integer type. */
    record I32() implements Type {
        @Override
        public <R, C> R accept(final TypeVisitor<R, C> v, final C ctx) {
            return v.visitI32(this, ctx);
        }
    }

    /** Signed 64-bit integer type. */
    record I64() implements Type {
        @Override
        public <R, C> R accept(final TypeVisitor<R, C> v, final C ctx) {
            return v.visitI64(this, ctx);
        }
    }

    /** Unsigned 8-bit integer type. */
    record U8() implements Type {
        @Override
        public <R, C> R accept(final TypeVisitor<R, C> v, final C ctx) {
            return v.visitU8(this, ctx);
        }
    }

    /** Unsigned 16-bit integer type. */
    record U16() implements Type {
        @Override
        public <R, C> R accept(final TypeVisitor<R, C> v, final C ctx) {
            return v.visitU16(this, ctx);
        }
    }

    /** Unsigned 32-bit integer type. */
    record U32() implements Type {
        @Override
        public <R, C> R accept(final TypeVisitor<R, C> v, final C ctx) {
            return v.visitU32(this, ctx);
        }
    }

    /** Unsigned 64-bit integer type. */
    record U64() implements Type {
        @Override
        public <R, C> R accept(final TypeVisitor<R, C> v, final C ctx) {
            return v.visitU64(this, ctx);
        }
    }

    /** 32-bit floating point type. */
    record F32() implements Type {
        @Override
        public <R, C> R accept(final TypeVisitor<R, C> v, final C ctx) {
            return v.visitF32(this, ctx);
        }
    }

    /** 64-bit floating point type. */
    record F64() implements Type {
        @Override
        public <R, C> R accept(final TypeVisitor<R, C> v, final C ctx) {
            return v.visitF64(this, ctx);
        }
    }

    /** Character type. */
    record Char() implements Type {
        @Override
        public <R, C> R accept(final TypeVisitor<R, C> v, final C ctx) {
            return v.visitChar(this, ctx);
        }
    }

    /** String type. */
    record StringT() implements Type {
        @Override
        public <R, C> R accept(final TypeVisitor<R, C> v, final C ctx) {
            return v.visitStringT(this, ctx);
        }
    }

    /** Boolean type. */
    record Bool() implements Type {
        @Override
        public <R, C> R accept(final TypeVisitor<R, C> v, final C ctx) {
            return v.visitBool(this, ctx);
        }
    }

    /**
     * User-defined custom type.
     *
     * @param name type identifier
     */
    record Custom(String name) implements Type {
        public Custom {
            Objects.requireNonNull(name, "name must not be null");
        }

        @Override
        public <R, C> R accept(final TypeVisitor<R, C> v, final C ctx) {
            return v.visitCustom(this, ctx);
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

        @Override
        public <R, C> R accept(final TypeVisitor<R, C> v, final C ctx) {
            return v.visitArray(this, ctx);
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

        @Override
        public <R, C> R accept(final TypeVisitor<R, C> v, final C ctx) {
            return v.visitVector(this, ctx);
        }
    }

    /** Void type. */
    record VoidT() implements Type {
        @Override
        public <R, C> R accept(final TypeVisitor<R, C> v, final C ctx) {
            return v.visitVoidT(this, ctx);
        }
    }

    /** Null pointer type. */
    record NullPtr() implements Type {
        @Override
        public <R, C> R accept(final TypeVisitor<R, C> v, final C ctx) {
            return v.visitNullPtr(this, ctx);
        }
    }
}
