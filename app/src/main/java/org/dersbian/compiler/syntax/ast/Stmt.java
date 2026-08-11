package org.dersbian.compiler.syntax.ast;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.Span;

/** Abstract syntax tree node representing a statement. */
@SuppressWarnings({"PMD.ShortClassName", "PMD.AvoidDuplicateLiterals"})
public sealed interface Stmt
        permits Stmt.Expression,
                Stmt.VarDeclaration,
                Stmt.Function,
                Stmt.If,
                Stmt.While,
                Stmt.For,
                Stmt.Block,
                Stmt.Return,
                Stmt.Break,
                Stmt.Continue,
                Stmt.MainFunction {

    /**
     * Returns the source span for this statement.
     *
     * @return source span
     */
    Span span();

    /**
     * Expression statement.
     *
     * @param expr wrapped expression
     */
    record Expression(Expr expr) implements Stmt {
        public Expression {
            Objects.requireNonNull(expr, "expr must not be null");
        }

        @Override
        public Span span() {
            return expr.span();
        }
    }

    /**
     * Pairs a variable name with an optional initializer expression.
     *
     * @param name variable name
     * @param initializer optional initializer expression
     */
    record VarBinding(String name, Optional<Expr> initializer) {
        public VarBinding {
            Objects.requireNonNull(name, "name must not be null");
            Objects.requireNonNull(initializer, "initializer must not be null");
        }
    }

    /**
     * Variable declaration statement.
     *
     * @param bindings list of variable bindings declared
     * @param typeAnnotation type annotation for declared variables
     * @param isMutable whether variables are mutable
     * @param span source extent
     */
    record VarDeclaration(
            List<VarBinding> bindings, Type typeAnnotation, boolean isMutable, Span span)
            implements Stmt {
        public VarDeclaration {
            bindings = List.copyOf(Objects.requireNonNull(bindings, "bindings must not be null"));
            Objects.requireNonNull(typeAnnotation, "typeAnnotation must not be null");
            Objects.requireNonNull(span, "span must not be null");
        }
    }

    /**
     * Function declaration statement.
     *
     * @param name function name
     * @param parameters function parameter list
     * @param returnType declared return type
     * @param body statement list forming function body
     * @param span source extent
     */
    record Function(String name, List<Parameter> parameters, Type returnType, Block body, Span span)
            implements Stmt {
        public Function {
            Objects.requireNonNull(name, "name must not be null");
            parameters =
                    List.copyOf(Objects.requireNonNull(parameters, "parameters must not be null"));
            Objects.requireNonNull(returnType, "returnType must not be null");
            Objects.requireNonNull(body, "body must not be null");
            Objects.requireNonNull(span, "span must not be null");
        }
    }

    /**
     * Conditional if statement.
     *
     * @param condition boolean condition expression
     * @param thenBranch statement list executed if condition is true
     * @param elseBranch optional statement list executed if condition is false
     * @param span source extent
     */
    record If(Expr condition, Block thenBranch, ElseBranch elseBranch, Span span) implements Stmt {
        public If {
            Objects.requireNonNull(condition, "condition must not be null");
            Objects.requireNonNull(thenBranch, "thenBranch must not be null");
            Objects.requireNonNull(elseBranch, "elseBranch must not be null");
            Objects.requireNonNull(span, "span must not be null");
        }
    }

    /**
     * While loop statement.
     *
     * @param condition loop condition expression
     * @param body loop body statements
     * @param span source extent
     */
    record While(Expr condition, Block body, Span span) implements Stmt {
        public While {
            Objects.requireNonNull(condition, "condition must not be null");
            Objects.requireNonNull(body, "body must not be null");
            Objects.requireNonNull(span, "span must not be null");
        }
    }

    /**
     * For loop statement.
     *
     * @param initializer optional loop initialization statement
     * @param condition optional loop condition expression
     * @param increment optional loop increment expression
     * @param body loop body statements
     * @param span source extent
     */
    record For(
            Optional<Stmt> initializer,
            Optional<Expr> condition,
            Optional<Expr> increment,
            Block body,
            Span span)
            implements Stmt {
        public For {
            Objects.requireNonNull(initializer, "initializer must not be null");
            Objects.requireNonNull(condition, "condition must not be null");
            Objects.requireNonNull(increment, "increment must not be null");
            Objects.requireNonNull(body, "body must not be null");
            Objects.requireNonNull(span, "span must not be null");
        }
    }

    /**
     * Block statement enclosing zero or more statements.
     *
     * @param statements list of statements in block
     * @param span source extent
     */
    record Block(List<Stmt> statements, Span span) implements Stmt {
        public Block {
            statements =
                    List.copyOf(Objects.requireNonNull(statements, "statements must not be null"));
            Objects.requireNonNull(span, "span must not be null");
        }
    }

    /**
     * Return statement.
     *
     * @param value optional return value expression
     * @param span source extent
     */
    record Return(Optional<Expr> value, Span span) implements Stmt {
        public Return {
            Objects.requireNonNull(value, "value must not be null");
            Objects.requireNonNull(span, "span must not be null");
        }
    }

    /**
     * Break statement.
     *
     * @param span source extent
     */
    record Break(Span span) implements Stmt {
        public Break {
            Objects.requireNonNull(span, "span must not be null");
        }
    }

    /**
     * Continue statement.
     *
     * @param span source extent
     */
    record Continue(Span span) implements Stmt {
        public Continue {
            Objects.requireNonNull(span, "span must not be null");
        }
    }

    /**
     * Main function statement.
     *
     * @param body statement list forming main function body
     * @param span source extent
     */
    record MainFunction(Block body, Span span) implements Stmt {
        public MainFunction {
            Objects.requireNonNull(body, "body must not be null");
            Objects.requireNonNull(span, "span must not be null");
        }
    }
}
