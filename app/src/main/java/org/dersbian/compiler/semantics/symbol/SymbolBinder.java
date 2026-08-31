package org.dersbian.compiler.semantics.symbol;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.dersbian.compiler.syntax.ast.ElseBranch;
import org.dersbian.compiler.syntax.ast.Parameter;
import org.dersbian.compiler.syntax.ast.Stmt;

/** Binds declarations from the current statement AST into a symbol table. */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.LongVariable"})
public final class SymbolBinder {
    /** The symbol table populated during symbol binding. */
    private final SymbolTable symbols;

    /** Accumulated declaration results across the bound statements. */
    private final List<DeclarationResult> declarations = new ArrayList<>();

    /** AST statement to lexical scope association for the current binding operation. */
    private final Map<Stmt, ScopeId> statementScopes = new LinkedHashMap<>();

    /** Creates a binder backed by the supplied symbol table. */
    public SymbolBinder(final SymbolTable symbols) {
        this.symbols = Objects.requireNonNull(symbols, "symbols must not be null");
    }

    /**
     * Binds a statement list in source order using the explicitly supplied parameter mutability.
     *
     * @param statements statements to bind
     * @param parameterMutability mutability assigned to every AST parameter
     * @return immutable binding context in source order
     */
    public BindingContext bind(
            final List<Stmt> statements, final Mutability parameterMutability) {
        Objects.requireNonNull(statements, "statements must not be null");
        Objects.requireNonNull(parameterMutability, "parameterMutability must not be null");
        declarations.clear();
        statementScopes.clear();
        for (final Stmt statement : List.copyOf(statements)) {
            bindStatement(
                    Objects.requireNonNull(statement, "statements must not contain null"),
                    parameterMutability);
        }
        return new BindingContext(statementScopes, declarations);
    }

    private void bindStatement(final Stmt statement, final Mutability parameterMutability) {
        statementScopes.put(statement, symbols.currentScope().id());
        switch (statement) {
            case Stmt.Expression ignored -> {
                // Expression references are resolved by a later semantic analysis phase.
            }
            case Stmt.VarDeclaration declaration -> bindVariableDeclaration(declaration);
            case Stmt.Function function -> bindFunction(function, parameterMutability);
            case Stmt.If conditional -> bindIf(conditional, parameterMutability);
            case Stmt.While loop -> bindWhile(loop, parameterMutability);
            case Stmt.For loop -> bindFor(loop, parameterMutability);
            case Stmt.Block block -> bindBlock(block, parameterMutability);
            case Stmt.Return ignored -> {
                // Return legality is checked by the semantic analyzer, not by symbol binding.
            }
            case Stmt.Break ignored -> {
                // Break legality is checked by the semantic analyzer, not by symbol binding.
            }
            case Stmt.Continue ignored -> {
                // Continue legality is checked by the semantic analyzer, not by symbol binding.
            }
            case Stmt.MainFunction main -> bindMain(main, parameterMutability);
        }
    }

    private void bindVariableDeclaration(final Stmt.VarDeclaration declaration) {
        for (final Stmt.VarBinding binding : declaration.bindings()) {
            declarations.add(
                    symbols.declareVariable(
                            binding.name(),
                            declaration.typeAnnotation(),
                            declaration.isMutable() ? Mutability.MUTABLE : Mutability.IMMUTABLE,
                            declaration.span()));
        }
    }

    private void bindFunction(final Stmt.Function function, final Mutability parameterMutability) {
        final DeclarationResult result =
                symbols.declareFunction(
                        function.name(),
                        function.parameters().stream()
                                .map(
                                        parameter ->
                                                new ParameterDescriptor(
                                                        parameter.name(),
                                                        parameter.typeAnnotation(),
                                                        parameterMutability))
                                .toList(),
                        function.returnType(),
                        function.span());
        declarations.add(result);
        if (result instanceof DeclarationResult.Declared declared) {
            symbols.enterScope(ScopeKind.FUNCTION, declared.symbol().id());
            try {
                bindParameters(function.parameters(), parameterMutability);
                bindBlock(function.body(), parameterMutability);
            } finally {
                symbols.exitScope();
            }
        }
    }

    private void bindParameters(
            final List<Parameter> parameters, final Mutability parameterMutability) {
        for (int ordinal = 0; ordinal < parameters.size(); ordinal++) {
            final Parameter parameter = parameters.get(ordinal);
            declarations.add(
                    symbols.declareParameter(
                            parameter.name(),
                            parameter.typeAnnotation(),
                            parameterMutability,
                            ordinal,
                            parameter.span()));
        }
    }

    private void bindMain(final Stmt.MainFunction main, final Mutability parameterMutability) {
        final DeclarationResult result = symbols.declareMainFunction(main.span());
        declarations.add(result);
        if (result instanceof DeclarationResult.Declared declared) {
            symbols.enterScope(ScopeKind.FUNCTION, declared.symbol().id());
            try {
                bindBlock(main.body(), parameterMutability);
            } finally {
                symbols.exitScope();
            }
        }
    }

    private void bindIf(final Stmt.If conditional, final Mutability parameterMutability) {
        bindBlock(conditional.thenBranch(), parameterMutability);
        bindElseBranch(conditional.elseBranch(), parameterMutability);
    }

    private void bindElseBranch(final ElseBranch branch, final Mutability parameterMutability) {
        switch (branch) {
            case ElseBranch.None ignored -> {
                // No scope exists for an absent else branch.
            }
            case ElseBranch.Block block -> bindBlock(block.block(), parameterMutability);
            case ElseBranch.ElseIf elseIf -> bindIf(elseIf.ifStmt(), parameterMutability);
        }
    }

    private void bindWhile(final Stmt.While loop, final Mutability parameterMutability) {
        bindBlock(loop.body(), parameterMutability);
    }

    private void bindFor(final Stmt.For loop, final Mutability parameterMutability) {
        symbols.enterScope(ScopeKind.LOOP);
        try {
            loop.initializer()
                    .ifPresent(statement -> bindStatement(statement, parameterMutability));
            bindBlock(loop.body(), parameterMutability);
        } finally {
            symbols.exitScope();
        }
    }

    private void bindBlock(final Stmt.Block block, final Mutability parameterMutability) {
        symbols.enterScope(ScopeKind.BLOCK);
        try {
            statementScopes.put(block, symbols.currentScope().id());
            for (final Stmt statement : block.statements()) {
                bindStatement(statement, parameterMutability);
            }
        } finally {
            symbols.exitScope();
        }
    }
}
