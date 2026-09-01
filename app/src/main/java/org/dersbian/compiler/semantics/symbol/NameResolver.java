package org.dersbian.compiler.semantics.symbol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.error.ErrorCode;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.ElseBranch;
import org.dersbian.compiler.syntax.ast.Expr;
import org.dersbian.compiler.syntax.ast.Stmt;

/** Performs declaration binding and deterministic lexical name resolution. */
public final class NameResolver {
    /** Symbol table used by the resolution operation. */
    private final SymbolTable symbols;

    /** Creates a resolver backed by the supplied symbol table. */
    public NameResolver(final SymbolTable symbols) {
        this.symbols = Objects.requireNonNull(symbols, "symbols must not be null");
    }

    /** Binds declarations and resolves every name reference in source order. */
    public NameResolutionResult resolve(
            final List<Stmt> statements, final Mutability parameterMutability) {
        Objects.requireNonNull(statements, "statements must not be null");
        Objects.requireNonNull(parameterMutability, "parameterMutability must not be null");

        final SymbolBinder binder = new SymbolBinder(symbols);
        final BindingContext binding = binder.bindContext(statements, parameterMutability);
        final Map<Expr, SymbolId> referenceBindings = new LinkedHashMap<>();
        final List<CompileError> diagnostics = new ArrayList<>();
        for (final Stmt statement : List.copyOf(statements)) {
            resolveStatement(statement, binding, referenceBindings, diagnostics);
        }
        symbols.findScope(symbols.globalScope().id())
                .orElseThrow(() -> new IllegalStateException("global scope disappeared during resolution"));
        return new NameResolutionResult(
                new SymbolBindingResult(binding.declarations()),
                scopeMapping(binding),
                referenceBindings,
                diagnostics);
    }

    private ScopeMapping scopeMapping(final BindingContext binding) {
        final List<Scope> scopes = new ArrayList<>();
        for (final ScopeId scopeId : binding.statementScopes().values()) {
            addScopeAndAncestors(scopeId, scopes);
        }
        for (final DeclarationResult declaration : binding.declarations()) {
            if (declaration instanceof DeclarationResult.Declared declared) {
                addScopeAndAncestors(declared.symbol().scopeId(), scopes);
            }
        }
        addScopeAndAncestors(symbols.globalScope().id(), scopes);
        return new ScopeMapping(binding.statementScopes(), scopes);
    }

    private void addScopeAndAncestors(final ScopeId scopeId, final List<Scope> scopes) {
        Scope scope = symbols.findScope(scopeId)
                .orElseThrow(() -> new IllegalStateException("scope mapping contains an unknown scope"));
        while (true) {
            if (!scopes.contains(scope)) {
                scopes.add(scope);
            }
            if (scope.parentId().isEmpty()) {
                return;
            }
            scope = symbols.findScope(scope.parentId().orElseThrow())
                    .orElseThrow(() -> new IllegalStateException("scope tree contains an orphaned parent"));
        }
    }

    private void resolveStatement(
            final Stmt statement,
            final BindingContext binding,
            final Map<Expr, SymbolId> referenceBindings,
            final List<CompileError> diagnostics) {
        final ScopeId scopeId = binding.scopeOf(statement)
                .orElseThrow(() -> new IllegalStateException("statement has no lexical scope"));
        switch (statement) {
            case Stmt.Expression expression ->
                    resolveExpression(scopeId, expression.expr(), referenceBindings, diagnostics);
            case Stmt.VarDeclaration declaration -> {
                for (final Stmt.VarBinding variable : declaration.bindings()) {
                    variable.initializer().ifPresent(initializer ->
                            resolveExpression(scopeId, initializer, referenceBindings, diagnostics));
                }
            }
            case Stmt.Function function ->
                    resolveStatement(function.body(), binding, referenceBindings, diagnostics);
            case Stmt.MainFunction main ->
                    resolveStatement(main.body(), binding, referenceBindings, diagnostics);
            case Stmt.If conditional -> {
                resolveExpression(scopeId, conditional.condition(), referenceBindings, diagnostics);
                resolveStatement(conditional.thenBranch(), binding, referenceBindings, diagnostics);
                resolveElse(conditional.elseBranch(), binding, referenceBindings, diagnostics);
            }
            case Stmt.While loop -> {
                resolveExpression(scopeId, loop.condition(), referenceBindings, diagnostics);
                resolveStatement(loop.body(), binding, referenceBindings, diagnostics);
            }
            case Stmt.For loop -> {
                loop.initializer().ifPresent(initializer ->
                        resolveStatement(initializer, binding, referenceBindings, diagnostics));
                loop.condition().ifPresent(condition ->
                        resolveExpression(scopeId, condition, referenceBindings, diagnostics));
                loop.increment().ifPresent(increment ->
                        resolveExpression(scopeId, increment, referenceBindings, diagnostics));
                resolveStatement(loop.body(), binding, referenceBindings, diagnostics);
            }
            case Stmt.Block block -> {
                for (final Stmt nested : block.statements()) {
                    resolveStatement(nested, binding, referenceBindings, diagnostics);
                }
            }
            case Stmt.Return result ->
                    result.value().ifPresent(value ->
                            resolveExpression(scopeId, value, referenceBindings, diagnostics));
            case Stmt.Break ignored -> { }
            case Stmt.Continue ignored -> { }
        }
    }

    private void resolveElse(
            final ElseBranch branch,
            final BindingContext binding,
            final Map<Expr, SymbolId> referenceBindings,
            final List<CompileError> diagnostics) {
        switch (branch) {
            case ElseBranch.None ignored -> { }
            case ElseBranch.Block block ->
                    resolveStatement(block.block(), binding, referenceBindings, diagnostics);
            case ElseBranch.ElseIf elseIf ->
                    resolveStatement(elseIf.ifStmt(), binding, referenceBindings, diagnostics);
        }
    }

    private void resolveExpression(
            final ScopeId scopeId,
            final Expr expression,
            final Map<Expr, SymbolId> referenceBindings,
            final List<CompileError> diagnostics) {
        if (expression instanceof Expr.Variable variable) {
            resolveVariable(scopeId, variable, referenceBindings, diagnostics);
            return;
        }
        if (expression instanceof Expr.Binary binary) {
            resolveExpression(scopeId, binary.left(), referenceBindings, diagnostics);
            resolveExpression(scopeId, binary.right(), referenceBindings, diagnostics);
            return;
        }
        if (expression instanceof Expr.Unary unary) {
            resolveExpression(scopeId, unary.expr(), referenceBindings, diagnostics);
            return;
        }
        if (expression instanceof Expr.Grouping grouping) {
            resolveExpression(scopeId, grouping.expr(), referenceBindings, diagnostics);
            return;
        }
        if (expression instanceof Expr.Literal) {
            return;
        }
        if (expression instanceof Expr.ArrayLiteral arrayLiteral) {
            for (final Expr element : arrayLiteral.elements()) {
                resolveExpression(scopeId, element, referenceBindings, diagnostics);
            }
            return;
        }
        if (expression instanceof Expr.Assign assign) {
            resolveExpression(scopeId, assign.target(), referenceBindings, diagnostics);
            resolveExpression(scopeId, assign.value(), referenceBindings, diagnostics);
            return;
        }
        if (expression instanceof Expr.Call call) {
            resolveExpression(scopeId, call.callee(), referenceBindings, diagnostics);
            for (final Expr argument : call.arguments()) {
                resolveExpression(scopeId, argument, referenceBindings, diagnostics);
            }
            return;
        }
        if (expression instanceof Expr.ArrayAccess access) {
            resolveExpression(scopeId, access.array(), referenceBindings, diagnostics);
            resolveExpression(scopeId, access.index(), referenceBindings, diagnostics);
        }
    }

    private void resolveVariable(
            final ScopeId scopeId,
            final Expr.Variable variable,
            final Map<Expr, SymbolId> referenceBindings,
            final List<CompileError> diagnostics) {
        final Optional<Symbol> visible = lookupVisibleAt(scopeId, variable.name(), variable.span());
        if (visible.isPresent()) {
            referenceBindings.put(variable, visible.orElseThrow().id());
            return;
        }
        diagnostics.add(unresolvedName(variable));
    }

    /** Performs lexical lookup while enforcing declaration-order visibility. */
    private Optional<Symbol> lookupVisibleAt(
            final ScopeId startScope, final String name, final Span referenceSpan) {
        Scope scope = symbols.findScope(startScope)
                .orElseThrow(() -> new IllegalStateException("reference starts in an unknown scope"));
        while (true) {
            final Optional<Symbol> local = symbols.lookupLocal(scope.id(), name);
            if (local.isPresent()) {
                final Symbol symbol = local.orElseThrow();
                if (symbol.declarationSpan().start().offset() <= referenceSpan.start().offset()) {
                    return local;
                }
                return Optional.empty();
            }
            if (scope.parentId().isEmpty()) {
                return Optional.empty();
            }
            scope = symbols.findScope(scope.parentId().orElseThrow())
                    .orElseThrow(() -> new IllegalStateException("scope tree contains an orphaned parent"));
        }
    }

    private static CompileError unresolvedName(final Expr.Variable variable) {
        return CompileError.nameResolutionError(
                ErrorCode.E2023,
                "unresolved name '%s'".formatted(variable.name()),
                variable.span(),
                "Declare the name before using it or ensure it is in scope.");
    }
}
