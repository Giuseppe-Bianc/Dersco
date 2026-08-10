package org.dersbian.compiler.syntax.ast;

import java.util.Objects;

/**
 * The {@code else} branch of an {@link Stmt.If} statement.
 *
 * <p>Three cases are representable:
 *
 * <ul>
 *   <li>{@link None} -- the {@code if} has no {@code else};
 *   <li>{@link Block} -- the {@code else} runs a block of statements;
 *   <li>{@link ElseIf} -- the {@code else} chains to another {@code if}, modelling {@code else if}
 *       without nesting.
 * </ul>
 *
 * <p>Constrained to {@code Block} or {@code If} by design -- the language does not permit a bare
 * non-block statement as an {@code else} body, so the type system rules out illegal shapes.
 */
public sealed interface ElseBranch permits ElseBranch.None, ElseBranch.Block, ElseBranch.ElseIf {

    /** Absence of an {@code else} branch. */
    record None() implements ElseBranch {

        /** Sentinel canonical instance -- {@code None} carries no data. */
        public None {
            // no fields to validate
        }
    }

    /**
     * An {@code else} branch running a block of statements.
     *
     * @param block the block executed when the {@code if} condition is false
     */
    record Block(Stmt.Block block) implements ElseBranch {
        public Block {
            Objects.requireNonNull(block, "block must not be null");
        }
    }

    /**
     * An {@code else if} chain: the {@code else} branch is itself another {@link Stmt.If}.
     *
     * @param ifStmt the chained {@code if}
     */
    record ElseIf(Stmt.If ifStmt) implements ElseBranch {
        public ElseIf {
            Objects.requireNonNull(ifStmt, "ifStmt must not be null");
        }
    }
}
