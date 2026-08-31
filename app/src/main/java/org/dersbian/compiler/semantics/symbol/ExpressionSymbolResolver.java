package org.dersbian.compiler.semantics.symbol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.dersbian.compiler.syntax.ast.Expr;

/** Resolves lexical symbol references occurring inside expressions. */
public final class ExpressionSymbolResolver {
    /** Symbol table used for lexical lookup. */
    private final SymbolTable symbolTable;

    /**
     * Creates an expression resolver backed by the supplied symbol table.
     *
     * @param symbolTable symbol table used for lexical lookup
     * @throws NullPointerException when symbolTable is null
     */
    public ExpressionSymbolResolver(final SymbolTable symbolTable) {
        this.symbolTable = Objects.requireNonNull(symbolTable, "symbolTable must not be null");
    }

    /**
     * Resolves every variable reference reachable from the supplied expression.
     *
     * <p>The current scope of the symbol table is used as the lexical starting scope. Only
     * successfully resolved references are present in the returned result. Missing references are
     * intentionally left unresolved so that the semantic analyzer can decide how to diagnose them.
     *
     * @param expression root expression to resolve
     * @return immutable expression-to-symbol binding result
     * @throws NullPointerException when expression is null
     */
    public ExpressionBindingResult resolve(final Expr expression) {
        Objects.requireNonNull(expression, "expression must not be null");
        final Map<Expr, SymbolId> bindings = new LinkedHashMap<>();
        resolveExpression(expression, bindings);
        return new ExpressionBindingResult(bindings);
    }

    /** Resolves one expression node and recursively processes its children. */
    private void resolveExpression(final Expr expression, final Map<Expr, SymbolId> bindings) {
        if (expression instanceof Expr.Variable variable) {
            symbolTable.lookup(variable.name()).map(Symbol::id).ifPresent(id -> bindings.put(variable, id));
            return;
        }
        if (expression instanceof Expr.Binary binary) {
            resolveExpression(binary.left(), bindings);
            resolveExpression(binary.right(), bindings);
            return;
        }
        if (expression instanceof Expr.Unary unary) {
            resolveExpression(unary.expr(), bindings);
            return;
        }
        if (expression instanceof Expr.Grouping grouping) {
            resolveExpression(grouping.expr(), bindings);
            return;
        }
        if (expression instanceof Expr.ArrayLiteral arrayLiteral) {
            for (final Expr element : arrayLiteral.elements()) {
                resolveExpression(element, bindings);
            }
            return;
        }
        if (expression instanceof Expr.Assign assign) {
            resolveExpression(assign.target(), bindings);
            resolveExpression(assign.value(), bindings);
            return;
        }
        if (expression instanceof Expr.Call call) {
            resolveExpression(call.callee(), bindings);
            for (final Expr argument : call.arguments()) {
                resolveExpression(argument, bindings);
            }
            return;
        }
        if (expression instanceof Expr.ArrayAccess arrayAccess) {
            resolveExpression(arrayAccess.array(), bindings);
            resolveExpression(arrayAccess.index(), bindings);
        }
    }
}
