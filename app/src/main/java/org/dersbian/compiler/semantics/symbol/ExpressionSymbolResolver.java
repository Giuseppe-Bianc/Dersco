package org.dersbian.compiler.semantics.symbol;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.Expr;

/** Resolves lexical symbol references occurring inside expressions. */
@SuppressWarnings({
    "PMD.OnlyOneReturn",
    "PMD.CyclomaticComplexity",
    "PMD.CognitiveComplexity",
    "PMD.UseConcurrentHashMap"
})
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
        return resolveFrom(symbolTable.currentScope().id(), expression);
    }

    /**
     * Resolves every variable reference reachable from the supplied expression and scope.
     *
     * <p>The historical scope is queried directly, so the operation does not require that the scope
     * remain current. This is useful for deferred semantic analysis after AST traversal.
     *
     * @param scopeId lexical starting scope
     * @param expression root expression to resolve
     * @return immutable expression-to-symbol binding result
     * @throws NullPointerException when scopeId or expression is null
     */
    public ExpressionBindingResult resolveFrom(final ScopeId scopeId, final Expr expression) {
        Objects.requireNonNull(scopeId, "scopeId must not be null");
        Objects.requireNonNull(expression, "expression must not be null");
        final Map<Expr, SymbolId> bindings = new LinkedHashMap<>();
        resolveExpression(scopeId, expression, bindings);
        return new ExpressionBindingResult(bindings);
    }

    /** Resolves one expression node and recursively processes its children. */
    private void resolveExpression(
            final ScopeId scopeId, final Expr expression, final Map<Expr, SymbolId> bindings) {
        if (expression instanceof Expr.Variable variable) {
            lookupVisibleAt(scopeId, variable.name(), variable.span())
                    .map(Symbol::id)
                    .ifPresent(id -> bindings.put(variable, id));
            return;
        }
        if (expression instanceof Expr.Binary binary) {
            resolveExpression(scopeId, binary.left(), bindings);
            resolveExpression(scopeId, binary.right(), bindings);
            return;
        }
        if (expression instanceof Expr.Unary unary) {
            resolveExpression(scopeId, unary.expr(), bindings);
            return;
        }
        if (expression instanceof Expr.Grouping grouping) {
            resolveExpression(scopeId, grouping.expr(), bindings);
            return;
        }
        if (expression instanceof Expr.ArrayLiteral arrayLiteral) {
            for (final Expr element : arrayLiteral.elements()) {
                resolveExpression(scopeId, element, bindings);
            }
            return;
        }
        if (expression instanceof Expr.Assign assign) {
            resolveExpression(scopeId, assign.target(), bindings);
            resolveExpression(scopeId, assign.value(), bindings);
            return;
        }
        if (expression instanceof Expr.Call call) {
            resolveExpression(scopeId, call.callee(), bindings);
            for (final Expr argument : call.arguments()) {
                resolveExpression(scopeId, argument, bindings);
            }
            return;
        }
        if (expression instanceof Expr.ArrayAccess arrayAccess) {
            resolveExpression(scopeId, arrayAccess.array(), bindings);
            resolveExpression(scopeId, arrayAccess.index(), bindings);
        }
    }

    /** Performs lexical lookup while enforcing declaration-order visibility. */
    private Optional<Symbol> lookupVisibleAt(
            final ScopeId startScope, final String name, final Span referenceSpan) {
        Scope scope = symbolTable.findScope(startScope).orElse(null);
        while (scope != null) {
            final Optional<Symbol> local = symbolTable.lookupLocal(scope.id(), name);
            if (local.isPresent()) {
                final Symbol symbol = local.orElseThrow();
                if (symbol.declarationSpan().start().offset() <= referenceSpan.start().offset()) {
                    return local;
                }
            }
            scope =
                    scope.parentId().isEmpty()
                            ? null
                            : symbolTable.findScope(scope.parentId().orElseThrow()).orElse(null);
        }
        return Optional.empty();
    }
}
