package org.dersbian.compiler.syntax.ast.visitor;

import java.util.List;
import lombok.NoArgsConstructor;
import org.dersbian.compiler.syntax.ast.ElseBranch;
import org.dersbian.compiler.syntax.ast.Expr;
import org.dersbian.compiler.syntax.ast.LiteralValue;
import org.dersbian.compiler.syntax.ast.Parameter;
import org.dersbian.compiler.syntax.ast.Stmt;
import org.dersbian.compiler.syntax.ast.Stmt.VarBinding;
import org.dersbian.compiler.syntax.ast.Type;

/**
 * A concrete visitor that pretty-prints the AST as a human-readable string.
 *
 * <p>Context {@code C} is unused ({@link Void}); pass {@code null} at call sites. The result type
 * {@code R} is {@link String}: every visit method returns the textual representation of the subtree
 * rooted at the visited node.
 *
 * <p>This visitor demonstrates the pattern where child traversal is explicit inside each visit
 * method: each method calls {@code child.accept(this, null)} and incorporates the returned string
 * into its own output.
 */
@SuppressWarnings({
    "PMD.ShortVariable",
    "PMD.TooManyMethods",
    "PMD.OnlyOneReturn",
    "PMD.CouplingBetweenObjects",
    "checkstyle:OverloadMethodsDeclarationOrder"
})
@NoArgsConstructor
public final class PrettyPrinterVisitor extends AbstractAstVisitor<String, Void> {

    /** Current indentation depth, incremented on block entry and decremented on block exit. */
    private int indentLevel;

    /** Single indentation unit appended once per indentation level. */
    private static final String INDENT_UNIT = "  ";

    @Override
    protected String defaultResult() {
        return "";
    }

    @Override
    protected String aggregateResult(final String aggregate, final String nextResult) {
        if (aggregate.isEmpty()) {
            return nextResult;
        }
        if (nextResult.isEmpty()) {
            return aggregate;
        }
        return aggregate + "\n" + nextResult;
    }

    // -----------------------------------------------------------------------
    // Expr
    // -----------------------------------------------------------------------

    @Override
    public String visitBinary(final Expr.Binary expr, final Void ctx) {
        final String left = expr.left().accept(this, null);
        final String right = expr.right().accept(this, null);
        return "(" + left + " " + expr.op().name() + " " + right + ")";
    }

    @Override
    public String visitUnary(final Expr.Unary expr, final Void ctx) {
        return "(" + expr.op().name() + " " + expr.expr().accept(this, null) + ")";
    }

    @Override
    public String visitGrouping(final Expr.Grouping expr, final Void ctx) {
        return "(" + expr.expr().accept(this, null) + ")";
    }

    @Override
    public String visitLiteral(final Expr.Literal expr, final Void ctx) {
        return expr.value().accept(this, null);
    }

    @Override
    public String visitArrayLiteral(final Expr.ArrayLiteral expr, final Void ctx) {
        final StringBuilder sb = new StringBuilder().append('[');
        for (int i = 0; i < expr.elements().size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(expr.elements().get(i).accept(this, null));
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public String visitVariable(final Expr.Variable expr, final Void ctx) {
        return expr.name();
    }

    @Override
    public String visitAssign(final Expr.Assign expr, final Void ctx) {
        return expr.target().accept(this, null) + " = " + expr.value().accept(this, null);
    }

    @Override
    public String visitCall(final Expr.Call expr, final Void ctx) {
        final StringBuilder sb = new StringBuilder(expr.callee().accept(this, null));
        sb.append('(');
        for (int i = 0; i < expr.arguments().size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(expr.arguments().get(i).accept(this, null));
        }
        sb.append(')');
        return sb.toString();
    }

    @Override
    public String visitArrayAccess(final Expr.ArrayAccess expr, final Void ctx) {
        return expr.array().accept(this, null) + "[" + expr.index().accept(this, null) + "]";
    }

    // -----------------------------------------------------------------------
    // Stmt
    // -----------------------------------------------------------------------

    @Override
    public String visitExpression(final Stmt.Expression stmt, final Void ctx) {
        return indent() + stmt.expr().accept(this, null) + ";";
    }

    @Override
    public String visitVarDeclaration(final Stmt.VarDeclaration stmt, final Void ctx) {
        final String mutability = stmt.isMutable() ? "mut " : "";
        final String typeName = stmt.typeAnnotation().accept(this, null);
        final StringBuilder sb = new StringBuilder(indent());
        sb.append(mutability).append("var ");

        final List<VarBinding> bindings = stmt.bindings();
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
                                expr -> sb.append(expr.accept(this, null)), () -> sb.append('_'));
            }
        }

        return sb.append(';').toString();
    }

    @Override
    public String visitFunction(final Stmt.Function stmt, final Void ctx) {
        final StringBuilder sb = new StringBuilder(indent());
        sb.append("fn ").append(stmt.name()).append('(');
        for (int i = 0; i < stmt.parameters().size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            final Parameter parameter = stmt.parameters().get(i);
            sb.append(parameter.name())
                    .append(": ")
                    .append(parameter.typeAnnotation().accept(this, null));
        }
        sb.append("): ").append(stmt.returnType().accept(this, null)).append(" {\n");
        indentLevel++;
        sb.append(stmt.body().accept(this, null)).append('\n');
        indentLevel--;
        sb.append(indent()).append('}');
        return sb.toString();
    }

    @Override
    public String visitIf(final Stmt.If stmt, final Void ctx) {
        final StringBuilder sb = new StringBuilder(indent());
        sb.append("if (").append(stmt.condition().accept(this, null)).append(") {\n");
        indentLevel++;
        sb.append(stmt.thenBranch().accept(this, null)).append('\n');
        indentLevel--;
        sb.append(indent()).append('}');
        switch (stmt.elseBranch()) {
            case ElseBranch.None _ -> {
                /* nothing */
            }
            case ElseBranch.Block b ->
                    sb.append(" else ").append(b.block().accept(this, null)).append('\n');
            case ElseBranch.ElseIf e ->
                    sb.append(" else ").append(e.ifStmt().accept(this, null)).append('\n');
        }
        return sb.toString();
    }

    @Override
    public String visitWhile(final Stmt.While stmt, final Void ctx) {
        final StringBuilder sb = new StringBuilder(indent());
        sb.append("while (").append(stmt.condition().accept(this, null)).append(") {\n");
        indentLevel++;
        sb.append(stmt.body().accept(this, null)).append('\n');
        indentLevel--;
        sb.append(indent()).append('}');
        return sb.toString();
    }

    @Override
    public String visitFor(final Stmt.For stmt, final Void ctx) {
        final StringBuilder sb = new StringBuilder(indent()).append("for (");
        stmt.initializer().ifPresent(i -> sb.append(i.accept(this, null).stripLeading()));
        sb.append("; ");
        stmt.condition().ifPresent(c -> sb.append(c.accept(this, null)));
        sb.append("; ");
        stmt.increment().ifPresent(inc -> sb.append(inc.accept(this, null)));
        sb.append(") {\n");
        indentLevel++;
        sb.append(stmt.body().accept(this, null)).append('\n');
        indentLevel--;
        sb.append(indent()).append('}');
        return sb.toString();
    }

    @Override
    public String visitBlock(final Stmt.Block stmt, final Void ctx) {
        final StringBuilder sb = new StringBuilder(indent()).append("{\n");
        indentLevel++;
        for (final Stmt s : stmt.statements()) {
            sb.append(s.accept(this, null)).append('\n');
        }
        indentLevel--;
        sb.append(indent()).append('}');
        return sb.toString();
    }

    @Override
    public String visitReturn(final Stmt.Return stmt, final Void ctx) {
        return stmt.value()
                .map(v -> indent() + "return " + v.accept(this, null) + ";")
                .orElseGet(() -> indent() + "return;");
    }

    @Override
    public String visitBreak(final Stmt.Break stmt, final Void ctx) {
        return indent() + "break;";
    }

    @Override
    public String visitContinue(final Stmt.Continue stmt, final Void ctx) {
        return indent() + "continue;";
    }

    @Override
    public String visitMainFunction(final Stmt.MainFunction stmt, final Void ctx) {
        final StringBuilder sb = new StringBuilder(indent()).append("main {\n");
        indentLevel++;
        sb.append(stmt.body().accept(this, null)).append('\n');
        indentLevel--;
        sb.append(indent()).append('}');
        return sb.toString();
    }

    // -----------------------------------------------------------------------
    // Type
    // -----------------------------------------------------------------------

    @Override
    public String visitI8(final Type.I8 t, final Void ctx) {
        return "i8";
    }

    @Override
    public String visitI16(final Type.I16 t, final Void ctx) {
        return "i16";
    }

    @Override
    public String visitI32(final Type.I32 t, final Void ctx) {
        return "i32";
    }

    @Override
    public String visitI64(final Type.I64 t, final Void ctx) {
        return "i64";
    }

    @Override
    public String visitU8(final Type.U8 t, final Void ctx) {
        return "u8";
    }

    @Override
    public String visitU16(final Type.U16 t, final Void ctx) {
        return "u16";
    }

    @Override
    public String visitU32(final Type.U32 t, final Void ctx) {
        return "u32";
    }

    @Override
    public String visitU64(final Type.U64 t, final Void ctx) {
        return "u64";
    }

    @Override
    public String visitF32(final Type.F32 t, final Void ctx) {
        return "f32";
    }

    @Override
    public String visitF64(final Type.F64 t, final Void ctx) {
        return "f64";
    }

    @Override
    public String visitChar(final Type.Char t, final Void ctx) {
        return "char";
    }

    @Override
    public String visitStringT(final Type.StringT t, final Void ctx) {
        return "string";
    }

    @Override
    public String visitBool(final Type.Bool t, final Void ctx) {
        return "bool";
    }

    @Override
    public String visitVoidT(final Type.VoidT t, final Void ctx) {
        return "void";
    }

    @Override
    public String visitNullPtr(final Type.NullPtr t, final Void ctx) {
        return "nullptr";
    }

    @Override
    public String visitCustom(final Type.Custom t, final Void ctx) {
        return t.name();
    }

    @Override
    public String visitArray(final Type.Array t, final Void ctx) {
        return "[" + t.elementType().accept(this, null) + "; " + t.size().accept(this, null) + "]";
    }

    @Override
    public String visitVector(final Type.Vector t, final Void ctx) {
        return "Vec<" + t.elementType().accept(this, null) + ">";
    }

    // -----------------------------------------------------------------------
    // LiteralValue
    // -----------------------------------------------------------------------

    @Override
    public String visitNumeric(final LiteralValue.Numeric value, final Void ctx) {
        return value.value().toString();
    }

    @Override
    public String visitStringLit(final LiteralValue.StringLit value, final Void ctx) {
        return "\"" + value.value() + "\"";
    }

    @Override
    public String visitCharLit(final LiteralValue.CharLit value, final Void ctx) {
        return "'" + value.value() + "'";
    }

    @Override
    public String visitBool(final LiteralValue.Bool value, final Void ctx) {
        return String.valueOf(value.value());
    }

    @Override
    public String visitNullPtr(final LiteralValue.NullPtr value, final Void ctx) {
        return "nullptr";
    }

    // -----------------------------------------------------------------------
    // Helper
    // -----------------------------------------------------------------------

    private String indent() {
        return INDENT_UNIT.repeat(indentLevel);
    }
}
