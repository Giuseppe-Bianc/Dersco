package org.dersbian.compiler.syntax.ast;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.dersbian.compiler.lexer.token.SourceLocation;
import org.dersbian.compiler.lexer.token.Span;
import org.junit.jupiter.api.Test;

/**
 * Tests for the sealed {@link ElseBranch} hierarchy.
 *
 * <p>Locks the three-variant contract: {@code None}, {@code Block(Stmt.Block)}, {@code
 * ElseIf(Stmt.If)}. Exhaustiveness is verified by a visitor that switches over all three variants
 * -- adding a fourth variant would fail compilation.
 */
@SuppressWarnings({
    "PMD.CommentRequired",
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.AtLeastOneConstructor"
})
class ElseBranchTest {

    private static final Span SPAN = Span.point(SourceLocation.create(1, 1, 0));

    private static Stmt.If wrap(
            final Expr cond, final Stmt.Block then, final ElseBranch elseBranch) {
        return new Stmt.If(cond, then, elseBranch, SPAN);
    }

    private static Stmt.Block block(final Stmt... stmts) {
        return new Stmt.Block(List.of(stmts), SPAN);
    }

    private static Expr.Variable var(final String name) {
        return new Expr.Variable(name, SPAN);
    }

    @Test
    void ifWithoutElseExposesNoneBranch() {
        final Stmt.If ifStmt =
                wrap(
                        var("c"),
                        block(new Stmt.Return(java.util.Optional.empty(), SPAN)),
                        new ElseBranch.None());
        assertThat(ifStmt.elseBranch()).isInstanceOf(ElseBranch.None.class);
    }

    @Test
    void ifWithElseBlockExposesBlockBranch() {
        final Stmt.Block elseBlock = block(new Stmt.Return(java.util.Optional.empty(), SPAN));
        final Stmt.If ifStmt = wrap(var("c"), block(), new ElseBranch.Block(elseBlock));
        assertThat(ifStmt.elseBranch()).isInstanceOf(ElseBranch.Block.class);
        assertThat(((ElseBranch.Block) ifStmt.elseBranch()).block()).isEqualTo(elseBlock);
    }

    @Test
    void ifWithElseIfExposesElseIfBranch() {
        final Stmt.If inner = wrap(var("d"), block(), new ElseBranch.None());
        final Stmt.If ifStmt = wrap(var("c"), block(), new ElseBranch.ElseIf(inner));
        assertThat(ifStmt.elseBranch()).isInstanceOf(ElseBranch.ElseIf.class);
        assertThat(((ElseBranch.ElseIf) ifStmt.elseBranch()).ifStmt()).isEqualTo(inner);
    }

    @Test
    void prettyPrinterOmitsElseForNone() {
        final Stmt.If ifStmt = wrap(var("c"), block(), new ElseBranch.None());
        final String out = AstPrinter.print(ifStmt);
        assertThat(out).doesNotContain("else");
    }

    @Test
    void prettyPrinterRendersElseForBlock() {
        final Stmt.If ifStmt =
                wrap(
                        var("c"),
                        block(),
                        new ElseBranch.Block(
                                block(new Stmt.Return(java.util.Optional.empty(), SPAN))));
        final String out = AstPrinter.print(ifStmt);
        assertThat(out).contains("else {");
    }

    @Test
    void prettyPrinterRendersElseIfForElseIf() {
        final Stmt.If inner = wrap(var("d"), block(), new ElseBranch.None());
        final Stmt.If outer = wrap(var("c"), block(), new ElseBranch.ElseIf(inner));
        final String out = AstPrinter.print(outer);
        assertThat(out).contains("else if (d)");
    }

    /**
     * Compile-time exhaustiveness check: if a fourth {@link ElseBranch} variant is added, the
     * pattern-matching switch in this test will fail to compile and the test will not even start.
     */
    @Test
    void visitElseBranchExhaustivelyHandled() {
        final java.util.Set<String> visited = new java.util.LinkedHashSet<>();
        final java.util.function.Consumer<Stmt.If> classify =
                stmt -> {
                    switch (stmt.elseBranch()) {
                        case ElseBranch.None _ -> visited.add("none");
                        case ElseBranch.Block _ -> visited.add("block");
                        case ElseBranch.ElseIf _ -> visited.add("elseif");
                    }
                };

        classify.accept(wrap(var("c"), block(), new ElseBranch.None()));
        classify.accept(wrap(var("c"), block(), new ElseBranch.Block(block())));
        classify.accept(
                wrap(
                        var("c"),
                        block(),
                        new ElseBranch.ElseIf(wrap(var("d"), block(), new ElseBranch.None()))));

        assertThat(visited).containsExactlyInAnyOrder("none", "block", "elseif");
    }
}
