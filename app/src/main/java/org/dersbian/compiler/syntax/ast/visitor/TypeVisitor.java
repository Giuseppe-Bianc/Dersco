package org.dersbian.compiler.syntax.ast.visitor;

import org.dersbian.compiler.syntax.ast.Type;

/**
 * Visitor contract for {@link Type} nodes.
 *
 * <p>{@link Type} appears as a structural annotation within {@link
 * org.dersbian.compiler.syntax.ast.Stmt.VarDeclaration}, {@link
 * org.dersbian.compiler.syntax.ast.Stmt.Function}, {@link
 * org.dersbian.compiler.syntax.ast.Parameter}, and {@link Type.Array} / {@link Type.Vector}. It is
 * visited separately from the expression and statement hierarchies because it represents a
 * type-level sub-tree, not a value-level one.
 *
 * @param <R> result type produced by each visit method
 * @param <C> context type threaded through the traversal
 */
@SuppressWarnings("PMD.TooManyMethods")
public interface TypeVisitor<R, C> {

    /**
     * Visits a {@link Type.I8} node.
     *
     * @param type the node
     * @param ctx traversal context
     * @return visit result
     */
    R visitI8(Type.I8 type, C ctx);

    /**
     * Visits a {@link Type.I16} node.
     *
     * @param type the node
     * @param ctx traversal context
     * @return visit result
     */
    R visitI16(Type.I16 type, C ctx);

    /**
     * Visits a {@link Type.I32} node.
     *
     * @param type the node
     * @param ctx traversal context
     * @return visit result
     */
    R visitI32(Type.I32 type, C ctx);

    /**
     * Visits a {@link Type.I64} node.
     *
     * @param type the node
     * @param ctx traversal context
     * @return visit result
     */
    R visitI64(Type.I64 type, C ctx);

    /**
     * Visits a {@link Type.U8} node.
     *
     * @param type the node
     * @param ctx traversal context
     * @return visit result
     */
    R visitU8(Type.U8 type, C ctx);

    /**
     * Visits a {@link Type.U16} node.
     *
     * @param type the node
     * @param ctx traversal context
     * @return visit result
     */
    R visitU16(Type.U16 type, C ctx);

    /**
     * Visits a {@link Type.U32} node.
     *
     * @param type the node
     * @param ctx traversal context
     * @return visit result
     */
    R visitU32(Type.U32 type, C ctx);

    /**
     * Visits a {@link Type.U64} node.
     *
     * @param type the node
     * @param ctx traversal context
     * @return visit result
     */
    R visitU64(Type.U64 type, C ctx);

    /**
     * Visits a {@link Type.F32} node.
     *
     * @param type the node
     * @param ctx traversal context
     * @return visit result
     */
    R visitF32(Type.F32 type, C ctx);

    /**
     * Visits a {@link Type.F64} node.
     *
     * @param type the node
     * @param ctx traversal context
     * @return visit result
     */
    R visitF64(Type.F64 type, C ctx);

    /**
     * Visits a {@link Type.Char} node.
     *
     * @param type the node
     * @param ctx traversal context
     * @return visit result
     */
    R visitChar(Type.Char type, C ctx);

    /**
     * Visits a {@link Type.StringT} node.
     *
     * @param type the node
     * @param ctx traversal context
     * @return visit result
     */
    R visitStringT(Type.StringT type, C ctx);

    /**
     * Visits a {@link Type.Bool} node.
     *
     * @param type the node
     * @param ctx traversal context
     * @return visit result
     */
    R visitBool(Type.Bool type, C ctx);

    /**
     * Visits a {@link Type.Custom} node.
     *
     * @param type the node
     * @param ctx traversal context
     * @return visit result
     */
    R visitCustom(Type.Custom type, C ctx);

    /**
     * Visits a {@link Type.Array} node.
     *
     * <p>Implementations that need to recurse into the element type or the size expression must do
     * so explicitly by calling {@link Type#accept(TypeVisitor, Object)} on {@link
     * Type.Array#elementType()} and {@link
     * org.dersbian.compiler.syntax.ast.Expr#accept(ExprVisitor, Object)} on {@link
     * Type.Array#size()}.
     *
     * @param type the array type node
     * @param ctx traversal context
     * @return visit result
     */
    R visitArray(Type.Array type, C ctx);

    /**
     * Visits a {@link Type.Vector} node.
     *
     * <p>Implementations that need to recurse into the element type must call {@link
     * Type#accept(TypeVisitor, Object)} on {@link Type.Vector#elementType()} explicitly.
     *
     * @param type the vector type node
     * @param ctx traversal context
     * @return visit result
     */
    R visitVector(Type.Vector type, C ctx);

    /**
     * Visits a {@link Type.VoidT} node.
     *
     * @param type the node
     * @param ctx traversal context
     * @return visit result
     */
    R visitVoidT(Type.VoidT type, C ctx);

    /**
     * Visits a {@link Type.NullPtr} node.
     *
     * @param type the node
     * @param ctx traversal context
     * @return visit result
     */
    R visitNullPtr(Type.NullPtr type, C ctx);
}
