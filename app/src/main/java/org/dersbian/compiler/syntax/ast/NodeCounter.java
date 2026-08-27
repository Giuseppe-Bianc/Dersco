package org.dersbian.compiler.syntax.ast;

/**
 * Counts the total number of AST nodes in a subtree via exhaustive pattern matching over sealed
 * types.
 *
 * <p>Replaces the visitor-based {@code NodeCounterVisitor}. Each call returns 1 for the node itself
 * plus the sum of {@code count(child)} for every direct child reached in the traversal. The
 * compilation phase is irrelevant: this is a pure structural count, not a semantic one.
 *
 * <p>Exhaustiveness is enforced by the compiler: adding a new variant to {@link Expr}, {@link
 * Stmt}, {@link Type}, or {@link LiteralValue} without updating this class is a compile error.
 */
@SuppressWarnings({"PMD.ShortVariable"})
public final class NodeCounter {

    private NodeCounter() {
        // static utility
    }

    // -----------------------------------------------------------------------
    // Entry points
    // -----------------------------------------------------------------------

    /** Counts nodes in an expression subtree. */
    public static int count(final Expr expr) {
        return 1 + countExpr(expr);
    }

    /** Counts nodes in a statement subtree. */
    public static int count(final Stmt stmt) {
        return 1 + countStmt(stmt);
    }

    /** Counts nodes in a type subtree. */
    public static int count(final Type type) {
        return 1 + countType(type);
    }

    /** Counts nodes in a literal value subtree. */
    public static int count(final LiteralValue value) {
        return 1;
    }

    // -----------------------------------------------------------------------
    // Expr
    // -----------------------------------------------------------------------

    private static int countExpr(final Expr expr) {
        return switch (expr) {
            case Expr.Binary b -> count(b.left()) + count(b.right());
            case Expr.Unary u -> count(u.expr());
            case Expr.Grouping g -> count(g.expr());
            case Expr.Literal l -> count(l.value());
            case Expr.ArrayLiteral a -> {
                int sum = 0;
                for (final Expr element : a.elements()) {
                    sum += count(element);
                }
                yield sum;
            }
            case Expr.Variable _ -> 0;
            case Expr.Assign a -> count(a.target()) + count(a.value());
            case Expr.Call c -> {
                int sum = count(c.callee());
                for (final Expr arg : c.arguments()) {
                    sum += count(arg);
                }
                yield sum;
            }
            case Expr.ArrayAccess aa -> count(aa.array()) + count(aa.index());
        };
    }

    // -----------------------------------------------------------------------
    // Stmt
    // -----------------------------------------------------------------------

    private static int countStmt(final Stmt stmt) {
        return switch (stmt) {
            case Stmt.Expression e -> count(e.expr());
            case Stmt.VarDeclaration v -> {
                int sum = count(v.typeAnnotation());
                for (final Stmt.VarBinding binding : v.bindings()) {
                    if (binding.initializer().isPresent()) {
                        sum += count(binding.initializer().get());
                    }
                }
                yield sum;
            }
            case Stmt.Function f -> {
                int sum = count(f.returnType());
                for (final Parameter param : f.parameters()) {
                    sum += count(param.typeAnnotation());
                }
                yield sum + count(f.body());
            }
            case Stmt.If i -> {
                int sum = count(i.condition()) + count(i.thenBranch());
                switch (i.elseBranch()) {
                    case ElseBranch.None _ -> {
                        // no else
                    }
                    case ElseBranch.Block b -> sum += count(b.block());
                    case ElseBranch.ElseIf e -> sum += count(e.ifStmt());
                }
                yield sum;
            }
            case Stmt.While w -> count(w.condition()) + count(w.body());
            case Stmt.For f -> {
                int sum = 0;
                if (f.initializer().isPresent()) {
                    sum += count(f.initializer().get());
                }
                if (f.condition().isPresent()) {
                    sum += count(f.condition().get());
                }
                if (f.increment().isPresent()) {
                    sum += count(f.increment().get());
                }
                yield sum + count(f.body());
            }
            case Stmt.Block b -> {
                int sum = 0;
                for (final Stmt s : b.statements()) {
                    sum += count(s);
                }
                yield sum;
            }
            case Stmt.Return r -> r.value().map(NodeCounter::count).orElse(0);
            case Stmt.Break _ -> 0;
            case Stmt.Continue _ -> 0;
            case Stmt.MainFunction m -> count(m.body());
        };
    }

    // -----------------------------------------------------------------------
    // Type
    // -----------------------------------------------------------------------

    private static int countType(final Type type) {
        return switch (type) {
            case Type.I8 _ -> 0;
            case Type.I16 _ -> 0;
            case Type.I32 _ -> 0;
            case Type.I64 _ -> 0;
            case Type.U8 _ -> 0;
            case Type.U16 _ -> 0;
            case Type.U32 _ -> 0;
            case Type.U64 _ -> 0;
            case Type.F32 _ -> 0;
            case Type.F64 _ -> 0;
            case Type.Char _ -> 0;
            case Type.StringT _ -> 0;
            case Type.Bool _ -> 0;
            case Type.VoidT _ -> 0;
            case Type.NullPtr _ -> 0;
            case Type.Custom _ -> 0;
            case Type.Array a -> count(a.elementType()) + count(a.size());
            case Type.Vector v -> count(v.elementType());
        };
    }
}
