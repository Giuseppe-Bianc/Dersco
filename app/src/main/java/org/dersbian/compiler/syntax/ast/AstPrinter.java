package org.dersbian.compiler.syntax.ast;

import java.util.List;

/**
 * Pattern-matching based pretty printer for the AST.
 *
 * <p>Replaces the visitor-based {@code PrettyPrinterVisitor} with exhaustive {@code switch}
 * expressions over sealed types. Exhaustiveness is enforced by the compiler: adding a new variant
 * to {@link Expr}, {@link Stmt}, {@link Type}, or {@link LiteralValue} without updating this class
 * is a compile error.
 *
 * <p>Indentation lives in a mutable {@link PrintContext} passed explicitly to recursive calls, so
 * the printer is reentrant: the same instance can print disjoint subtrees concurrently, and reuse
 * after a partial failure yields a fresh context.
 */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.ShortVariable", "PMD.CognitiveComplexity"})
public final class AstPrinter {

    /**
     * Mutable per-print state threaded through the recursion.
     *
     * <p>Prevents the stateful-instance anti-pattern of the previous visitor: a fresh context is
     * allocated per {@code print} call, so concurrent or repeated invocations cannot interfere.
     */
    private static final class PrintContext {

        /** Two-space indentation unit. */
        private static final String INDENT_UNIT = "  ";

        /** Current indentation level. */
        private int indentLevel;

        /** Returns the indentation string for the current level. */
        private String indent() {
            return INDENT_UNIT.repeat(indentLevel);
        }
    }

    private AstPrinter() {
        // static utility
    }

    // -----------------------------------------------------------------------
    // Entry points
    // -----------------------------------------------------------------------

    /** Prints an expression with default (zero) indentation. */
    public static String print(final Expr expr) {
        return printExpr(expr, new PrintContext());
    }

    /** Prints a statement with default indentation. */
    public static String print(final Stmt stmt) {
        return printStmt(stmt, new PrintContext());
    }

    /** Prints a type with default indentation. */
    public static String print(final Type type) {
        return printType(type, new PrintContext());
    }

    // -----------------------------------------------------------------------
    // Expr
    // -----------------------------------------------------------------------

    private static String printExpr(final Expr expr, final PrintContext ctx) {
        return switch (expr) {
            case Expr.Binary b -> {
                final String left = printExpr(b.left(), ctx);
                final String right = printExpr(b.right(), ctx);
                yield "(" + left + " " + b.op().name() + " " + right + ")";
            }
            case Expr.Unary u -> {
                final String inner = printExpr(u.expr(), ctx);
                yield switch (u.side()) {
                    case PREFIX -> "(" + u.op().name() + " " + inner + ")";
                    case POSTFIX -> "(" + inner + " " + u.op().name() + ")";
                };
            }
            case Expr.Grouping g -> "(" + printExpr(g.expr(), ctx) + ")";
            case Expr.Literal l -> printLiteral(l.value());
            case Expr.ArrayLiteral a -> {
                final StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < a.elements().size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(printExpr(a.elements().get(i), ctx));
                }
                yield sb.append(']').toString();
            }
            case Expr.Variable v -> v.name();
            case Expr.Assign a -> printExpr(a.target(), ctx) + " = " + printExpr(a.value(), ctx);
            case Expr.Call c -> {
                final StringBuilder sb = new StringBuilder(printExpr(c.callee(), ctx)).append('(');
                for (int i = 0; i < c.arguments().size(); i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(printExpr(c.arguments().get(i), ctx));
                }
                yield sb.append(')').toString();
            }
            case Expr.ArrayAccess aa ->
                    printExpr(aa.array(), ctx) + "[" + printExpr(aa.index(), ctx) + "]";
        };
    }

    // -----------------------------------------------------------------------
    // LiteralValue
    // -----------------------------------------------------------------------

    private static String printLiteral(final LiteralValue value) {
        return switch (value) {
            case LiteralValue.Numeric n -> n.value().toString();
            case LiteralValue.StringLit s -> "\"" + s.value() + "\"";
            case LiteralValue.CharLit c -> "'" + c.value() + "'";
            case LiteralValue.Bool b -> String.valueOf(b.value());
            case LiteralValue.NullPtr ignored -> "nullptr";
        };
    }

    // -----------------------------------------------------------------------
    // Stmt
    // -----------------------------------------------------------------------

    private static String printStmt(final Stmt stmt, final PrintContext ctx) {
        return switch (stmt) {
            case Stmt.Expression e -> ctx.indent() + printExpr(e.expr(), ctx) + ";";
            case Stmt.VarDeclaration v -> printVarDeclaration(v, ctx);
            case Stmt.Function f -> printFunction(f, ctx);
            case Stmt.If i -> printIf(i, ctx);
            case Stmt.While w -> printWhile(w, ctx);
            case Stmt.For f -> printFor(f, ctx);
            case Stmt.Block b -> printBlock(b, ctx);
            case Stmt.Return r ->
                    r.value()
                            .map(v -> ctx.indent() + "return " + printExpr(v, ctx) + ";")
                            .orElseGet(() -> ctx.indent() + "return;");
            case Stmt.Break ignored -> ctx.indent() + "break;";
            case Stmt.Continue ignored -> ctx.indent() + "continue;";
            case Stmt.MainFunction m -> printMainFunction(m, ctx);
        };
    }

    private static String printVarDeclaration(final Stmt.VarDeclaration v, final PrintContext ctx) {
        final String mutability = v.isMutable() ? "mut " : "";
        final String typeName = printType(v.typeAnnotation(), ctx);
        final StringBuilder sb = new StringBuilder(ctx.indent()).append(mutability).append("var ");
        final List<Stmt.VarBinding> bindings = v.bindings();
        for (int i = 0; i < bindings.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(bindings.get(i).name());
        }
        sb.append(": ").append(typeName);
        final boolean anyInitializers =
                bindings.stream().anyMatch(b -> b.initializer().isPresent());
        if (anyInitializers) {
            sb.append(" = ");
            for (int i = 0; i < bindings.size(); i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                bindings.get(i)
                        .initializer()
                        .ifPresentOrElse(
                                expr -> sb.append(printExpr(expr, ctx)), () -> sb.append('_'));
            }
        }
        return sb.append(';').toString();
    }

    private static String printFunction(final Stmt.Function f, final PrintContext ctx) {
        final StringBuilder sb =
                new StringBuilder(ctx.indent()).append("fn ").append(f.name()).append('(');
        for (int i = 0; i < f.parameters().size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            final Parameter parameter = f.parameters().get(i);
            sb.append(parameter.name())
                    .append(": ")
                    .append(printType(parameter.typeAnnotation(), ctx));
        }
        sb.append("): ").append(printType(f.returnType(), ctx)).append(" {\n");
        ctx.indentLevel++;
        final String body = printStmt(f.body(), ctx);
        ctx.indentLevel--;
        sb.append(body).append('\n').append(ctx.indent()).append('}');
        return sb.toString();
    }

    private static String printIf(final Stmt.If i, final PrintContext ctx) {
        final StringBuilder sb =
                new StringBuilder(ctx.indent())
                        .append("if (")
                        .append(printExpr(i.condition(), ctx))
                        .append(") {\n");
        ctx.indentLevel++;
        final String thenBranch = printStmt(i.thenBranch(), ctx);
        ctx.indentLevel--;
        sb.append(thenBranch).append('\n').append(ctx.indent()).append('}');
        switch (i.elseBranch()) {
            case ElseBranch.None ignored -> {
                // no else
            }
            case ElseBranch.Block eb -> {
                sb.append(" else {\n");
                ctx.indentLevel++;
                final String elseBody = printStmt(eb.block(), ctx);
                ctx.indentLevel--;
                sb.append(elseBody).append('\n').append(ctx.indent()).append('}');
            }
            case ElseBranch.ElseIf ei -> sb.append(" else ").append(printStmt(ei.ifStmt(), ctx));
        }
        return sb.toString();
    }

    private static String printWhile(final Stmt.While w, final PrintContext ctx) {
        final StringBuilder sb =
                new StringBuilder(ctx.indent())
                        .append("while (")
                        .append(printExpr(w.condition(), ctx))
                        .append(") {\n");
        ctx.indentLevel++;
        final String body = printStmt(w.body(), ctx);
        ctx.indentLevel--;
        sb.append(body).append('\n').append(ctx.indent()).append('}');
        return sb.toString();
    }

    private static String printFor(final Stmt.For f, final PrintContext ctx) {
        final StringBuilder sb = new StringBuilder(ctx.indent()).append("for (");
        f.initializer().ifPresent(i -> sb.append(printStmt(i, ctx).stripLeading()));
        sb.append("; ");
        f.condition().ifPresent(c -> sb.append(printExpr(c, ctx)));
        sb.append("; ");
        f.increment().ifPresent(inc -> sb.append(printExpr(inc, ctx)));
        sb.append(") {\n");
        ctx.indentLevel++;
        final String body = printStmt(f.body(), ctx);
        ctx.indentLevel--;
        sb.append(body).append('\n').append(ctx.indent()).append('}');
        return sb.toString();
    }

    private static String printBlock(final Stmt.Block b, final PrintContext ctx) {
        final StringBuilder sb = new StringBuilder(ctx.indent()).append("{\n");
        ctx.indentLevel++;
        for (final Stmt s : b.statements()) {
            sb.append(printStmt(s, ctx)).append('\n');
        }
        ctx.indentLevel--;
        sb.append(ctx.indent()).append('}');
        return sb.toString();
    }

    private static String printMainFunction(final Stmt.MainFunction m, final PrintContext ctx) {
        final StringBuilder sb = new StringBuilder(ctx.indent()).append("main {\n");
        ctx.indentLevel++;
        final String body = printStmt(m.body(), ctx);
        ctx.indentLevel--;
        sb.append(body).append('\n').append(ctx.indent()).append('}');
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Type
    // -----------------------------------------------------------------------

    private static String printType(final Type type, final PrintContext ctx) {
        return switch (type) {
            case Type.I8 ignored -> "i8";
            case Type.I16 ignored -> "i16";
            case Type.I32 ignored -> "i32";
            case Type.I64 ignored -> "i64";
            case Type.U8 ignored -> "u8";
            case Type.U16 ignored -> "u16";
            case Type.U32 ignored -> "u32";
            case Type.U64 ignored -> "u64";
            case Type.F32 ignored -> "f32";
            case Type.F64 ignored -> "f64";
            case Type.Char ignored -> "char";
            case Type.StringT ignored -> "string";
            case Type.Bool ignored -> "bool";
            case Type.VoidT ignored -> "void";
            case Type.NullPtr ignored -> "nullptr";
            case Type.Custom c -> c.name();
            case Type.Array a ->
                    "[" + printType(a.elementType(), ctx) + "; " + printExpr(a.size(), ctx) + "]";
            case Type.Vector v -> "Vec<" + printType(v.elementType(), ctx) + ">";
        };
    }
}
