package org.dersbian.compiler.syntax.ast.visitor;

import java.util.List;
import java.util.Optional;
import lombok.NoArgsConstructor;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.BinaryOp;
import org.dersbian.compiler.syntax.ast.Expr;
import org.dersbian.compiler.syntax.ast.LiteralValue;
import org.dersbian.compiler.syntax.ast.Parameter;
import org.dersbian.compiler.syntax.ast.Stmt;
import org.dersbian.compiler.syntax.ast.Type;
import org.dersbian.compiler.syntax.ast.UnaryOp;
import org.dersbian.compiler.syntax.ast.UnaryOpSide;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Visitor Pattern implementation on the AST.
 *
 * <p>Each test builds a minimal AST fragment and verifies:
 *
 * <ol>
 *   <li>that the correct {@code visit} method is dispatched (dispatch correctness),
 *   <li>that child nodes are visited in the documented order (traversal order),
 *   <li>that return values are correctly propagated (result propagation).
 * </ol>
 */
@SuppressWarnings({
    "PMD.ShortVariable",
    "PMD.TooManyMethods",
    "PMD.LawOfDemeter",
    "PMD.UnitTestAssertionsShouldIncludeMessage",
    "checkstyle:OverloadMethodsDeclarationOrder"
})
@NoArgsConstructor
class AstVisitorTest {

    /** Sentinel span used wherever a real span is not relevant to the test. */
    private static final Span SPAN = Span.point(SourceLocation.create(2, 3, 4));

    /**
     * Reusable variable name for condition nodes used across traversal-order tests. Extracted to
     * satisfy PMD AvoidDuplicateLiterals: the literal would otherwise appear four times in this
     * file.
     */
    private static final String COND_VAR = "cond";

    /** Visitor that records which visit methods are called during dispatch. */
    private DispatchRecordingVisitor recorder;

    /** Visitor that counts every AST node encountered during traversal. */
    private final NodeCounterVisitor counter = new NodeCounterVisitor();

    /** Visitor under test that renders AST nodes as human-readable strings. */
    private final PrettyPrinterVisitor printer = new PrettyPrinterVisitor();

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Builds a simple boolean literal used as a leaf node in tests. */
    private Expr.Literal boolLit(final boolean value) {
        return new Expr.Literal(new LiteralValue.Bool(value), SPAN);
    }

    /** Builds a simple variable expression used as a leaf node in tests. */
    private Expr.Variable var(final String name) {
        return new Expr.Variable(name, SPAN);
    }

    /**
     * Builds the function-declaration AST node reused by the two split tests below.
     *
     * <p>The original single test was split to satisfy PMD UnitTestContainsTooManyAsserts, which
     * forbids more than one assertion per test method. Both tests share this factory so the AST
     * construction logic is not duplicated.
     */
    private Stmt.Function buildAddFunction() {
        return new Stmt.Function(
                "add",
                List.of(
                        new Parameter("x", new Type.I32(), SPAN),
                        new Parameter("y", new Type.I32(), SPAN)),
                new Type.I32(),
                new Stmt.Block(
                        List.of(
                                new Stmt.Return(
                                        Optional.of(
                                                new Expr.Binary(
                                                        var("x"), BinaryOp.ADD, var("y"), SPAN)),
                                        SPAN)),
                        SPAN),
                SPAN);
    }

    @BeforeEach
    void setUp() {
        recorder = new DispatchRecordingVisitor();
    }

    // -----------------------------------------------------------------------
    // Dispatch correctness
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Expr.Binary dispatches to visitBinary")
    void binaryDispatch() {
        final Expr.Binary node = new Expr.Binary(boolLit(true), BinaryOp.ADD, boolLit(false), SPAN);
        node.accept(recorder, null);
        Assertions.assertTrue(recorder.visited.contains("visitBinary"));
    }

    @Test
    @DisplayName("Expr.Unary dispatches to visitUnary")
    void unaryDispatch() {
        new Expr.Unary(UnaryOp.NEGATE, UnaryOpSide.PREFIX, boolLit(true), SPAN)
                .accept(recorder, null);
        Assertions.assertTrue(recorder.visited.contains("visitUnary"));
    }

    @Test
    @DisplayName("Expr.Grouping dispatches to visitGrouping")
    void groupingDispatch() {
        new Expr.Grouping(boolLit(true), SPAN).accept(recorder, null);
        Assertions.assertTrue(recorder.visited.contains("visitGrouping"));
    }

    @Test
    @DisplayName("Expr.Literal dispatches to visitLiteral")
    void literalDispatch() {
        boolLit(true).accept(recorder, null);
        Assertions.assertTrue(recorder.visited.contains("visitLiteral"));
    }

    @Test
    @DisplayName("Expr.ArrayLiteral dispatches to visitArrayLiteral")
    void arrayLiteralDispatch() {
        new Expr.ArrayLiteral(List.of(boolLit(true)), SPAN).accept(recorder, null);
        Assertions.assertTrue(recorder.visited.contains("visitArrayLiteral"));
    }

    @Test
    @DisplayName("Expr.Variable dispatches to visitVariable")
    void variableDispatch() {
        var("x").accept(recorder, null);
        Assertions.assertTrue(recorder.visited.contains("visitVariable"));
    }

    @Test
    @DisplayName("Expr.Assign dispatches to visitAssign")
    void assignDispatch() {
        new Expr.Assign(var("x"), boolLit(true), SPAN).accept(recorder, null);
        Assertions.assertTrue(recorder.visited.contains("visitAssign"));
    }

    @Test
    @DisplayName("Expr.Call dispatches to visitCall")
    void callDispatch() {
        new Expr.Call(var("f"), List.of(boolLit(true)), SPAN).accept(recorder, null);
        Assertions.assertTrue(recorder.visited.contains("visitCall"));
    }

    @Test
    @DisplayName("Expr.ArrayAccess dispatches to visitArrayAccess")
    void arrayAccessDispatch() {
        new Expr.ArrayAccess(var("arr"), boolLit(true), SPAN).accept(recorder, null);
        Assertions.assertTrue(recorder.visited.contains("visitArrayAccess"));
    }

    @Test
    @DisplayName("Stmt.Break dispatches to visitBreak")
    void breakDispatch() {
        new Stmt.Break(SPAN).accept(recorder, null);
        Assertions.assertTrue(recorder.visited.contains("visitBreak"));
    }

    @Test
    @DisplayName("Stmt.Continue dispatches to visitContinue")
    void continueDispatch() {
        new Stmt.Continue(SPAN).accept(recorder, null);
        Assertions.assertTrue(recorder.visited.contains("visitContinue"));
    }

    @Test
    @DisplayName("LiteralValue.Bool dispatches to visitBool")
    void literalBoolDispatch() {
        new LiteralValue.Bool(true).accept(recorder, null);
        Assertions.assertTrue(recorder.visited.contains("visitBoolLiteral"));
    }

    @Test
    @DisplayName("LiteralValue.NullPtr dispatches to visitNullPtr")
    void literalNullPtrDispatch() {
        new LiteralValue.NullPtr().accept(recorder, null);
        Assertions.assertTrue(recorder.visited.contains("visitNullPtrLiteral"));
    }

    @Test
    @DisplayName("Type.I32 dispatches to visitI32")
    void typeI32Dispatch() {
        new Type.I32().accept(recorder, null);
        Assertions.assertTrue(recorder.visited.contains("visitI32"));
    }

    // -----------------------------------------------------------------------
    // Traversal order
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Expr.Binary: left child visited before right child")
    void binaryTraversalOrder() {
        final Expr.Variable left = var("a");
        final Expr.Variable right = var("b");
        final Expr.Binary binary = new Expr.Binary(left, BinaryOp.ADD, right, SPAN);

        final List<String> order = new java.util.ArrayList<>();
        final ExprVisitor<Void, Void> orderRecorder =
                new AbstractAstVisitor<>() {
                    @Override
                    protected Void defaultResult() {
                        return null;
                    }

                    @Override
                    public Void visitVariable(final Expr.Variable e, final Void c) {
                        order.add(e.name());
                        return null;
                    }
                };

        binary.accept(orderRecorder, null);
        Assertions.assertEquals(List.of("a", "b"), order);
    }

    @Test
    @DisplayName("Stmt.If: condition, then-branch, else-branch visited in order")
    void ifTraversalOrder() {
        final List<String> order = new java.util.ArrayList<>();

        final Expr condition = var(COND_VAR);
        final Stmt thenStmt = new Stmt.Expression(var("then"));
        final Stmt elseStmt = new Stmt.Expression(var("else"));

        final Stmt.If ifNode =
                new Stmt.If(
                        condition,
                        new Stmt.Block(List.of(thenStmt), SPAN),
                        Optional.of(List.of(elseStmt)),
                        SPAN);

        final AstVisitor<Void, Void> orderRecorder =
                new AbstractAstVisitor<>() {
                    @Override
                    protected Void defaultResult() {
                        return null;
                    }

                    @Override
                    public Void visitVariable(final Expr.Variable e, final Void c) {
                        order.add(e.name());
                        return null;
                    }
                };

        ifNode.accept(orderRecorder, null);
        Assertions.assertEquals(List.of(COND_VAR, "then", "else"), order);
    }

    @Test
    @DisplayName("Stmt.For: initializer, condition, increment, body visited in order")
    void forTraversalOrder() {
        final List<String> order = new java.util.ArrayList<>();

        final Stmt.VarDeclaration init =
                new Stmt.VarDeclaration(
                        List.of(new Stmt.VarBinding("i", Optional.of(boolLit(true)))),
                        new Type.I32(),
                        true,
                        SPAN);
        final Expr condition = var(COND_VAR);
        final Expr increment = var("inc");
        final Stmt body = new Stmt.Expression(var("body"));

        final Stmt.For forNode =
                new Stmt.For(
                        Optional.of(init),
                        Optional.of(condition),
                        Optional.of(increment),
                        new Stmt.Block(List.of(body), SPAN),
                        SPAN);

        final AstVisitor<Void, Void> orderRecorder =
                new AbstractAstVisitor<>() {
                    @Override
                    protected Void defaultResult() {
                        return null;
                    }

                    @Override
                    public Void visitVariable(final Expr.Variable e, final Void c) {
                        order.add(e.name());
                        return null;
                    }
                };

        forNode.accept(orderRecorder, null);
        Assertions.assertEquals(List.of(COND_VAR, "inc", "body"), order);
    }

    @Test
    @DisplayName("Expr.Call: callee visited before arguments, arguments left to right")
    void callTraversalOrder() {
        final List<String> order = new java.util.ArrayList<>();
        final Expr.Call call = new Expr.Call(var("fn"), List.of(var("a1"), var("a2")), SPAN);

        final ExprVisitor<Void, Void> orderRecorder =
                new AbstractAstVisitor<>() {
                    @Override
                    protected Void defaultResult() {
                        return null;
                    }

                    @Override
                    public Void visitVariable(final Expr.Variable e, final Void c) {
                        order.add(e.name());
                        return null;
                    }
                };

        call.accept(orderRecorder, null);
        Assertions.assertEquals(List.of("fn", "a1", "a2"), order);
    }

    // -----------------------------------------------------------------------
    // Result propagation
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Single literal = 2 nodes (Literal + Bool)")
    void singleLiteralCount() {
        Assertions.assertEquals(2, boolLit(true).accept(counter, null));
    }

    @Test
    @DisplayName("Binary(lit, lit) = 1 (Binary) + 2 + 2 = 5 nodes")
    void binaryOfTwoLiteralsCount() {
        final Expr.Binary b = new Expr.Binary(boolLit(true), BinaryOp.ADD, boolLit(false), SPAN);
        Assertions.assertEquals(5, b.accept(counter, null));
    }

    @Test
    @DisplayName("Block with two expressions counts all descendant nodes")
    void blockCount() {
        final Stmt.Block block =
                new Stmt.Block(
                        List.of(
                                new Stmt.Expression(boolLit(true)),
                                new Stmt.Expression(boolLit(false))),
                        SPAN);
        Assertions.assertEquals(7, block.accept(counter, null));
    }

    @Test
    @DisplayName("Empty ArrayLiteral = 1 node (ArrayLiteral only)")
    void emptyArrayLiteralCount() {
        Assertions.assertEquals(1, new Expr.ArrayLiteral(List.of(), SPAN).accept(counter, null));
    }

    // -----------------------------------------------------------------------
    // PrettyPrinter smoke tests
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("Binary addition renders with operator name")
    void prettyBinaryAddition() {
        final String out =
                new Expr.Binary(var("x"), BinaryOp.ADD, var("y"), SPAN).accept(printer, null);
        Assertions.assertEquals("(x ADD y)", out);
    }

    @Test
    @DisplayName("Nested grouping renders correctly")
    void prettyNestedGrouping() {
        final Expr inner = new Expr.Grouping(var("a"), SPAN);
        final Expr binary = new Expr.Binary(inner, BinaryOp.MULTIPLY, var("b"), SPAN);
        Assertions.assertEquals("((a) MULTIPLY b)", binary.accept(printer, null));
    }

    @Test
    @DisplayName("Function declaration renders signature with parameters and return type")
    void prettyFunctionDeclarationSignature() {
        final String out = buildAddFunction().accept(printer, null);
        Assertions.assertTrue(out.startsWith("fn add(x: i32, y: i32): i32 {"));
    }

    @Test
    @DisplayName("Function declaration renders body with return expression")
    void prettyFunctionDeclarationBody() {
        final String out = buildAddFunction().accept(printer, null);
        Assertions.assertTrue(out.contains("return (x ADD y);"));
    }

    @Test
    @DisplayName("nullptr literal renders as 'nullptr'")
    void prettyNullptrLiteral() {
        Assertions.assertEquals("nullptr", Expr.nullExpr(SPAN).accept(printer, null));
    }

    // -----------------------------------------------------------------------
    // Support types
    // -----------------------------------------------------------------------

    /**
     * Records the name of each visit method called, so dispatch tests can assert without depending
     * on side effects or return values.
     */
    private static final class DispatchRecordingVisitor extends AbstractAstVisitor<Void, Void> {

        /** Set of visit method names that have been invoked on this visitor. */
        private final java.util.Set<String> visited = new java.util.LinkedHashSet<>();

        @Override
        protected Void defaultResult() {
            return null;
        }

        @Override
        public Void visitBinary(final Expr.Binary e, final Void c) {
            visited.add("visitBinary");
            return super.visitBinary(e, null);
        }

        @Override
        public Void visitUnary(final Expr.Unary e, final Void c) {
            visited.add("visitUnary");
            return super.visitUnary(e, null);
        }

        @Override
        public Void visitGrouping(final Expr.Grouping e, final Void c) {
            visited.add("visitGrouping");
            return super.visitGrouping(e, null);
        }

        @Override
        public Void visitLiteral(final Expr.Literal e, final Void c) {
            visited.add("visitLiteral");
            return super.visitLiteral(e, null);
        }

        @Override
        public Void visitArrayLiteral(final Expr.ArrayLiteral e, final Void c) {
            visited.add("visitArrayLiteral");
            return super.visitArrayLiteral(e, null);
        }

        @Override
        public Void visitVariable(final Expr.Variable e, final Void c) {
            visited.add("visitVariable");
            return null;
        }

        @Override
        public Void visitAssign(final Expr.Assign e, final Void c) {
            visited.add("visitAssign");
            return super.visitAssign(e, null);
        }

        @Override
        public Void visitCall(final Expr.Call e, final Void c) {
            visited.add("visitCall");
            return super.visitCall(e, null);
        }

        @Override
        public Void visitArrayAccess(final Expr.ArrayAccess e, final Void c) {
            visited.add("visitArrayAccess");
            return super.visitArrayAccess(e, null);
        }

        @Override
        public Void visitBreak(final Stmt.Break s, final Void c) {
            visited.add("visitBreak");
            return null;
        }

        @Override
        public Void visitContinue(final Stmt.Continue s, final Void c) {
            visited.add("visitContinue");
            return null;
        }

        @Override
        public Void visitI32(final Type.I32 t, final Void c) {
            visited.add("visitI32");
            return null;
        }

        @Override
        public Void visitBool(final LiteralValue.Bool v, final Void c) {
            visited.add("visitBoolLiteral");
            return null;
        }

        @Override
        public Void visitNullPtr(final LiteralValue.NullPtr v, final Void c) {
            visited.add("visitNullPtrLiteral");
            return null;
        }
    }
}
