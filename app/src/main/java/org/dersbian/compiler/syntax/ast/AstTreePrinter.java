package org.dersbian.compiler.syntax.ast;

import java.util.List;
import java.util.Optional;

/**
 * Pretty-prints an AST as a styled, tree-like string.
 *
 * <p>Java port of the Rust {@code pretty_print}/{@code pretty_print_stmt} module. The Rust code
 * uses a manually preallocated {@code String}; here a {@link StringBuilder} plays the same role.
 * The initial capacity heuristic (nodes × per-node estimate) is preserved.
 *
 * <p>Adaptation notes:
 *
 * <ul>
 *   <li>Rust's {@code Option<T>} maps to {@link Optional Optional&lt;T&gt;}.
 *   <li>The Rust AST uses parallel {@code variables}/{@code initializers} lists; the Java AST
 *       collapses these into {@link Stmt.VarBinding}. The printer iterates bindings once and prints
 *       names and initializers accordingly.
 *   <li>Rust {@code if}/{@code while}/{@code for} bodies are {@code Vec<Stmt>}; the Java model
 *       wraps them in {@link Stmt.Block}, so we iterate {@code block.statements()}.
 *   <li>The Rust {@code else_branch: Option<Vec<Stmt>>} maps to the sealed {@link ElseBranch}.
 *   <li>{@code type_annotation}'s {@code Display} in Rust corresponds here to a call to {@link
 *       AstPrinter#print(Type)}, which produces the canonical textual form.
 * </ul>
 */
@SuppressWarnings({"PMD.LongVariable", "PMD.ShortVariable"})
public final class AstTreePrinter {

    /** Estimated buffer capacity allocated per expression node during pretty-printing. */
    private static final int EXPR_CAPACITY_PER_NODE = 45;

    /** Estimated buffer capacity allocated per statement node during pretty-printing. */
    private static final int STMT_CAPACITY_PER_NODE = 50;

    private AstTreePrinter() {
        // static utility
    }

    /** Pretty-prints an expression AST into a styled, tree-like string. */
    public static String prettyPrint(final Expr expr) {
        final int nodeCount = NodeCounter.count(expr);
        final StringBuilder output = new StringBuilder(nodeCount * EXPR_CAPACITY_PER_NODE);
        final StyleManager styles = new StyleManager();
        printExpr(expr, "", BranchType.LAST, output, styles);
        return output.toString();
    }

    /** Pretty-prints a single statement AST into a styled, tree-like string. */
    public static String prettyPrintStmt(final Stmt stmt) {
        final int nodeCount = NodeCounter.count(stmt);
        final StringBuilder output = new StringBuilder(nodeCount * STMT_CAPACITY_PER_NODE);
        final StyleManager styles = new StyleManager();
        printStmt(stmt, "", BranchType.LAST, output, styles);
        return output.toString();
    }

    private static void printBranch(
            final String label,
            final Expr expr,
            final String parentIndent,
            final BranchConfig branchConfig,
            final StringBuilder output,
            final StyleManager styles) {
        final String indent = BranchPrinter.getIndent(parentIndent, branchConfig.parentType());
        BranchPrinter.appendLine(
                output, indent, branchConfig.currentType(), styles.structure(), label);
        printExpr(
                expr,
                BranchPrinter.getIndent(indent, branchConfig.currentType()),
                branchConfig.childType(),
                output,
                styles);
    }

    // -----------------------------------------------------------------------
    // Expr printing
    // -----------------------------------------------------------------------

    private static void printExpr(
            final Expr expr,
            final String indent,
            final BranchType branchType,
            final StringBuilder output,
            final StyleManager styles) {
        switch (expr) {
            case Expr.Binary b -> {
                BranchPrinter.appendLine(
                        output, indent, branchType, styles.operator(), "BinaryOp " + b.op().name());
                printBranch(
                        "Left:",
                        b.left(),
                        indent,
                        new BranchConfig(branchType, BranchType.MIDDLE, BranchType.LAST),
                        output,
                        styles);
                printBranch(
                        "Right:",
                        b.right(),
                        indent,
                        new BranchConfig(branchType, BranchType.LAST, BranchType.LAST),
                        output,
                        styles);
            }
            case Expr.Unary u -> {
                BranchPrinter.appendLine(
                        output, indent, branchType, styles.operator(), "UnaryOp " + u.op().name());
                printBranch(
                        "Expr:",
                        u.expr(),
                        indent,
                        new BranchConfig(branchType, BranchType.LAST, BranchType.LAST),
                        output,
                        styles);
            }
            case Expr.Grouping g -> {
                BranchPrinter.appendLine(
                        output, indent, branchType, styles.punctuation(), "Grouping");
                printBranch(
                        "Expr:",
                        g.expr(),
                        indent,
                        new BranchConfig(branchType, BranchType.LAST, BranchType.LAST),
                        output,
                        styles);
            }
            case Expr.Literal l ->
                    BranchPrinter.appendLine(
                            output,
                            indent,
                            branchType,
                            styles.literal(),
                            "Literal " + formatLiteral(l.value()));
            case Expr.Variable v ->
                    BranchPrinter.appendLine(
                            output,
                            indent,
                            branchType,
                            styles.variable(),
                            "Variable '" + v.name() + "'");
            case Expr.Assign a -> {
                BranchPrinter.appendLine(
                        output, indent, branchType, styles.variable(), "Assignment");
                final String newIndent = BranchPrinter.getIndent(indent, branchType);
                BranchPrinter.appendLine(
                        output, newIndent, BranchType.MIDDLE, styles.structure(), "Target:");
                printExpr(
                        a.target(),
                        BranchPrinter.getIndent(newIndent, BranchType.MIDDLE),
                        BranchType.LAST,
                        output,
                        styles);
                BranchPrinter.appendLine(
                        output, newIndent, BranchType.LAST, styles.structure(), "Value:");
                printExpr(
                        a.value(),
                        BranchPrinter.getIndent(newIndent, BranchType.LAST),
                        BranchType.LAST,
                        output,
                        styles);
            }
            case Expr.Call c -> {
                BranchPrinter.appendLine(
                        output, indent, branchType, styles.punctuation(), "Function Call");
                final String newIndent = BranchPrinter.getIndent(indent, branchType);
                BranchPrinter.appendLine(
                        output, newIndent, BranchType.MIDDLE, styles.structure(), "Callee:");
                printExpr(
                        c.callee(),
                        BranchPrinter.getIndent(newIndent, BranchType.MIDDLE),
                        BranchType.LAST,
                        output,
                        styles);
                BranchPrinter.appendLine(
                        output, newIndent, BranchType.LAST, styles.structure(), "Arguments:");
                final String argsIndent = BranchPrinter.getIndent(newIndent, BranchType.LAST);
                BranchPrinter.printChildren(
                        c.arguments(),
                        argsIndent,
                        output,
                        styles,
                        (arg, childIndent, bt, out, st) -> {
                            BranchPrinter.appendLine(out, childIndent, bt, st.structure(), "Arg:");
                            printExpr(
                                    arg,
                                    BranchPrinter.getIndent(childIndent, bt),
                                    BranchType.LAST,
                                    out,
                                    st);
                        });
            }
            case Expr.ArrayAccess aa -> {
                BranchPrinter.appendLine(
                        output, indent, branchType, styles.punctuation(), "Array Access");
                printBranch(
                        "Array:",
                        aa.array(),
                        indent,
                        new BranchConfig(branchType, BranchType.MIDDLE, BranchType.LAST),
                        output,
                        styles);
                printBranch(
                        "Index:",
                        aa.index(),
                        indent,
                        new BranchConfig(branchType, BranchType.LAST, BranchType.LAST),
                        output,
                        styles);
            }
            case Expr.ArrayLiteral al -> {
                BranchPrinter.appendLine(
                        output, indent, branchType, styles.punctuation(), "Array Literal");
                final String newIndent = BranchPrinter.getIndent(indent, branchType);
                final List<Expr> elements = al.elements();
                if (elements.isEmpty()) {
                    BranchPrinter.appendLine(
                            output,
                            newIndent,
                            BranchType.LAST,
                            styles.structure(),
                            "Elements: (empty)");
                } else {
                    BranchPrinter.appendLine(
                            output, newIndent, BranchType.LAST, styles.structure(), "Elements:");
                    BranchPrinter.printChildren(
                            elements,
                            BranchPrinter.getIndent(newIndent, BranchType.LAST),
                            output,
                            styles,
                            AstTreePrinter::printExpr);
                }
            }
        }
    }

    // Textual form of a literal value (mirrors Rust's {value} formatting).
    private static String formatLiteral(final LiteralValue value) {
        return switch (value) {
            case LiteralValue.Numeric n -> n.value().toString();
            case LiteralValue.StringLit s -> "\"" + s.value() + "\"";
            case LiteralValue.CharLit c -> "'" + c.value() + "'";
            case LiteralValue.Bool b -> String.valueOf(b.value());
            case LiteralValue.NullPtr _ -> "nullptr";
        };
    }

    // -----------------------------------------------------------------------
    // Stmt printing
    // -----------------------------------------------------------------------

    private static void printStmt(
            final Stmt stmt,
            final String indent,
            final BranchType branchType,
            final StringBuilder output,
            final StyleManager styles) {
        switch (stmt) {
            case Stmt.Expression e -> {
                BranchPrinter.appendLine(
                        output, indent, branchType, styles.keyword(), "Expression");
                final String newIndent = BranchPrinter.getIndent(indent, branchType);
                BranchPrinter.appendLine(
                        output, newIndent, BranchType.LAST, styles.structure(), "Expr:");
                printExpr(
                        e.expr(),
                        BranchPrinter.getIndent(newIndent, BranchType.LAST),
                        BranchType.LAST,
                        output,
                        styles);
            }
            case Stmt.VarDeclaration v ->
                    printVarDeclaration(v, indent, branchType, output, styles);
            case Stmt.Function f -> printFunction(f, indent, branchType, output, styles);
            case Stmt.If i -> printIf(i, indent, branchType, output, styles);
            case Stmt.MainFunction m -> {
                BranchPrinter.appendLine(
                        output, indent, branchType, styles.keyword(), "MainFunction");
                BranchPrinter.printChildren(
                        m.body().statements(),
                        BranchPrinter.getIndent(indent, branchType),
                        output,
                        styles,
                        AstTreePrinter::printStmt);
            }
            case Stmt.Block b -> {
                if (b.statements().isEmpty()) {
                    BranchPrinter.appendLine(
                            output, indent, branchType, styles.keyword(), "Block: (empty)");
                } else {
                    BranchPrinter.appendLine(output, indent, branchType, styles.keyword(), "Block");
                    BranchPrinter.printChildren(
                            b.statements(),
                            BranchPrinter.getIndent(indent, branchType),
                            output,
                            styles,
                            AstTreePrinter::printStmt);
                }
            }
            case Stmt.Return r -> {
                BranchPrinter.appendLine(output, indent, branchType, styles.keyword(), "Return");
                if (r.value().isPresent()) {
                    final String newIndent = BranchPrinter.getIndent(indent, branchType);
                    BranchPrinter.appendLine(
                            output, newIndent, BranchType.LAST, styles.structure(), "Value:");
                    printExpr(
                            r.value().get(),
                            BranchPrinter.getIndent(newIndent, BranchType.LAST),
                            BranchType.LAST,
                            output,
                            styles);
                }
            }
            case Stmt.While w -> {
                BranchPrinter.appendLine(output, indent, branchType, styles.keyword(), "While");
                final String newIndent = BranchPrinter.getIndent(indent, branchType);
                BranchPrinter.appendLine(
                        output, newIndent, BranchType.MIDDLE, styles.structure(), "Condition:");
                printExpr(
                        w.condition(),
                        BranchPrinter.getIndent(newIndent, BranchType.MIDDLE),
                        BranchType.LAST,
                        output,
                        styles);
                BranchPrinter.appendLine(
                        output, newIndent, BranchType.LAST, styles.structure(), "Body:");
                BranchPrinter.printChildren(
                        w.body().statements(),
                        BranchPrinter.getIndent(newIndent, BranchType.LAST),
                        output,
                        styles,
                        AstTreePrinter::printStmt);
            }
            case Stmt.For fr -> printFor(fr, indent, branchType, output, styles);
            case Stmt.Break _ ->
                    BranchPrinter.appendLine(output, indent, branchType, styles.keyword(), "Break");
            case Stmt.Continue _ ->
                    BranchPrinter.appendLine(
                            output, indent, branchType, styles.keyword(), "Continue");
        }
    }

    private static void printVarDeclaration(
            final Stmt.VarDeclaration v,
            final String indent,
            final BranchType branchType,
            final StringBuilder output,
            final StyleManager styles) {
        BranchPrinter.appendLine(output, indent, branchType, styles.keyword(), "VarDeclaration");
        final String newIndent = BranchPrinter.getIndent(indent, branchType);

        // Variables / Constants (name list)
        final String varsLabel = v.isMutable() ? "Variables:" : "Constants:";
        BranchPrinter.appendLine(
                output, newIndent, BranchType.MIDDLE, styles.variable(), varsLabel);
        final String varsIndent = BranchPrinter.getIndent(newIndent, BranchType.MIDDLE);
        final List<Stmt.VarBinding> bindings = v.bindings();
        for (int i = 0; i < bindings.size(); i++) {
            final BranchType varBranchType =
                    (i == bindings.size() - 1) ? BranchType.LAST : BranchType.MIDDLE;
            BranchPrinter.appendLine(
                    output, varsIndent, varBranchType, styles.variable(), bindings.get(i).name());
        }

        // Type
        BranchPrinter.appendLine(output, newIndent, BranchType.MIDDLE, styles.structure(), "Type:");
        final String typeIndent = BranchPrinter.getIndent(newIndent, BranchType.MIDDLE);
        BranchPrinter.appendLine(
                output,
                typeIndent,
                BranchType.LAST,
                styles.typeStyle(),
                AstPrinter.print(v.typeAnnotation()));

        // Initializers (collected from bindings, matching the Rust field semantics)
        BranchPrinter.appendLine(
                output, newIndent, BranchType.LAST, styles.structure(), "Initializers:");
        final String initIndent = BranchPrinter.getIndent(newIndent, BranchType.LAST);
        final List<Expr> initializers =
                bindings.stream()
                        .map(Stmt.VarBinding::initializer)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .toList();
        BranchPrinter.printChildren(
                initializers, initIndent, output, styles, AstTreePrinter::printExpr);
    }

    private static void printFunction(
            final Stmt.Function f,
            final String indent,
            final BranchType branchType,
            final StringBuilder output,
            final StyleManager styles) {
        BranchPrinter.appendLine(output, indent, branchType, styles.keyword(), "Function");
        final String newIndent = BranchPrinter.getIndent(indent, branchType);

        // Name
        BranchPrinter.appendLine(output, newIndent, BranchType.MIDDLE, styles.structure(), "Name:");
        BranchPrinter.appendLine(
                output,
                BranchPrinter.getIndent(newIndent, BranchType.MIDDLE),
                BranchType.LAST,
                styles.variable(),
                f.name());

        // Parameters
        BranchPrinter.appendLine(
                output, newIndent, BranchType.MIDDLE, styles.structure(), "Parameters:");
        final String paramsIndent = BranchPrinter.getIndent(newIndent, BranchType.MIDDLE);
        final List<Parameter> parameters = f.parameters();
        for (int i = 0; i < parameters.size(); i++) {
            final BranchType paramBranchType =
                    (i == parameters.size() - 1) ? BranchType.LAST : BranchType.MIDDLE;
            final Parameter param = parameters.get(i);
            BranchPrinter.appendLine(
                    output,
                    paramsIndent,
                    paramBranchType,
                    styles.structure(),
                    "Parameter '" + param.name() + "'");
            BranchPrinter.appendLine(
                    output,
                    BranchPrinter.getIndent(paramsIndent, paramBranchType),
                    BranchType.LAST,
                    styles.typeStyle(),
                    "Type: " + AstPrinter.print(param.typeAnnotation()));
        }

        // Return Type
        BranchPrinter.appendLine(
                output, newIndent, BranchType.MIDDLE, styles.structure(), "Return Type:");
        BranchPrinter.appendLine(
                output,
                BranchPrinter.getIndent(newIndent, BranchType.MIDDLE),
                BranchType.LAST,
                styles.typeStyle(),
                AstPrinter.print(f.returnType()));

        // Body
        BranchPrinter.appendLine(output, newIndent, BranchType.LAST, styles.structure(), "Body:");
        BranchPrinter.printChildren(
                f.body().statements(),
                BranchPrinter.getIndent(newIndent, BranchType.LAST),
                output,
                styles,
                AstTreePrinter::printStmt);
    }

    private static void printIf(
            final Stmt.If i,
            final String indent,
            final BranchType branchType,
            final StringBuilder output,
            final StyleManager styles) {
        BranchPrinter.appendLine(output, indent, branchType, styles.keyword(), "If");
        final String newIndent = BranchPrinter.getIndent(indent, branchType);

        // Condition
        BranchPrinter.appendLine(
                output, newIndent, BranchType.MIDDLE, styles.structure(), "Condition:");
        printExpr(
                i.condition(),
                BranchPrinter.getIndent(newIndent, BranchType.MIDDLE),
                BranchType.LAST,
                output,
                styles);

        final boolean hasElse = !(i.elseBranch() instanceof ElseBranch.None);
        final List<Stmt> thenStmts = i.thenBranch().statements();

        if (thenStmts.isEmpty()) {
            BranchPrinter.appendLine(
                    output, newIndent, BranchType.LAST, styles.structure(), "Then: (empty)");
        } else {
            final BranchType thenBranchType = hasElse ? BranchType.MIDDLE : BranchType.LAST;
            BranchPrinter.appendLine(
                    output, newIndent, thenBranchType, styles.structure(), "Then:");
            BranchPrinter.printChildren(
                    thenStmts,
                    BranchPrinter.getIndent(newIndent, thenBranchType),
                    output,
                    styles,
                    AstTreePrinter::printStmt);
        }

        // Else branch: adapt sealed ElseBranch to the Rust Option<Vec<Stmt>> shape.
        final List<Stmt> elseStmts =
                switch (i.elseBranch()) {
                    case ElseBranch.None _ -> null;
                    case ElseBranch.Block eb -> eb.block().statements();
                    case ElseBranch.ElseIf ei -> List.of(ei.ifStmt());
                };
        if (elseStmts != null) {
            BranchPrinter.appendLine(
                    output, newIndent, BranchType.LAST, styles.structure(), "Else:");
            BranchPrinter.printChildren(
                    elseStmts,
                    BranchPrinter.getIndent(newIndent, BranchType.LAST),
                    output,
                    styles,
                    AstTreePrinter::printStmt);
        }
    }

    private static void printFor(
            final Stmt.For fr,
            final String indent,
            final BranchType branchType,
            final StringBuilder output,
            final StyleManager styles) {
        BranchPrinter.appendLine(output, indent, branchType, styles.keyword(), "For");
        final String newIndent = BranchPrinter.getIndent(indent, branchType);

        // Initializer
        if (fr.initializer().isPresent()) {
            BranchPrinter.appendLine(
                    output, newIndent, BranchType.MIDDLE, styles.structure(), "Initializer:");
            printStmt(
                    fr.initializer().get(),
                    BranchPrinter.getIndent(newIndent, BranchType.MIDDLE),
                    BranchType.LAST,
                    output,
                    styles);
        }
        // Condition
        if (fr.condition().isPresent()) {
            BranchPrinter.appendLine(
                    output, newIndent, BranchType.MIDDLE, styles.structure(), "Condition:");
            printExpr(
                    fr.condition().get(),
                    BranchPrinter.getIndent(newIndent, BranchType.MIDDLE),
                    BranchType.LAST,
                    output,
                    styles);
        }
        // Increment
        if (fr.increment().isPresent()) {
            BranchPrinter.appendLine(
                    output, newIndent, BranchType.MIDDLE, styles.structure(), "Increment:");
            printExpr(
                    fr.increment().get(),
                    BranchPrinter.getIndent(newIndent, BranchType.MIDDLE),
                    BranchType.LAST,
                    output,
                    styles);
        }
        // Body
        BranchPrinter.appendLine(output, newIndent, BranchType.LAST, styles.structure(), "Body:");
        BranchPrinter.printChildren(
                fr.body().statements(),
                BranchPrinter.getIndent(newIndent, BranchType.LAST),
                output,
                styles,
                AstTreePrinter::printStmt);
    }
}
