package org.dersbian.compiler.semantics.symbol;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.dersbian.compiler.syntax.ast.ElseBranch;
import org.dersbian.compiler.syntax.ast.Parameter;
import org.dersbian.compiler.syntax.ast.Stmt;

/** Binds declarations from the current statement AST into a symbol table. */
public final class SymbolBinder {
    private static final Mutability DEFAULT_PARAMETER_MUTABILITY = Mutability.IMMUTABLE;

    private final SymbolTable symbols;
    private final List<DeclarationResult> declarations = new ArrayList<>();

    /** Creates a binder backed by the supplied symbol table. */
    public SymbolBinder(final SymbolTable symbols) {
        this.symbols = Objects.requireNonNull(symbols, "symbols must not be null");
    }

    /**
     * Binds a statement list in source order.
     *
     * @param statements statements to bind
     * @return immutable declaration results in source order
     */
    public SymbolBindingResult bind(final List<Stmt> statements) {
        Objects.requireNonNull(statements, "statements must not be null");
        declarations.clear();
        for (final Stmt statement : List.copyOf(statements)) {
            bindStatement(Objects.requireNonNull(statement, "statements must not contain null"));
        }
        return new SymbolBindingResult(declarations);
    }

    private void bindStatement(final Stmt statement) {
        switch (statement) {
            case Stmt.Expression ignored -> {
                // Expression references are resolved by a later semantic analysis phase.
            }
            case Stmt.VarDeclaration declaration -> bindVariableDeclaration(declaration);
            case Stmt.Function function -> bindFunction(function);
            case Stmt.If conditional -> bindIf(conditional);
            case Stmt.While loop -> bindWhile(loop);
            case Stmt.For loop -> bindFor(loop);
            case Stmt.Block block -> bindBlock(block);
            case Stmt.Return ignored -> {
                // Return legality is checked by the semantic analyzer, not by symbol binding.
            }
            case Stmt.Break ignored -> {
                // Break legality is checked by the semantic analyzer, not by symbol binding.
            }
            case Stmt.Continue ignored -> {
                // Continue legality is checked by the semantic analyzer, not by symbol binding.
            }
            case Stmt.MainFunction main -> bindMain(main);
        }
    }

    private void bindVariableDeclaration(final Stmt.VarDeclaration declaration) {
        for (final Stmt.VarBinding binding : declaration.bindings()) {
            declarations.add(
                    symbols.declareVariable(
                            binding.name(),
                            declaration.typeAnnotation(),
                            declaration.isMutable()
                                    ? Mutability.MUTABLE
                                    : Mutability.IMMUTABLE,
                            declaration.span()));
        }
    }

    private void bindFunction(final Stmt.Function function) {
        final DeclarationResult result =
                symbols.declareFunction(
                        function.name(),
                        function.parameters().stream()
                                .map(
                                        parameter ->
                                                new ParameterDescriptor(
                                                        parameter.name(),
                                                        parameter.typeAnnotation(),
                                                        DEFAULT_PARAMETER_MUTABILITY))
                                .toList(),
                        function.returnType(),
                        function.span());
        declarations.add(result);
        if (result instanceof DeclarationResult.Declared declared) {
            symbols.enterScope(ScopeKind.FUNCTION, declared.symbol().id());
            try {
                bindParameters(function.parameters());
                bindBlock(function.body());
            } finally {
                symbols.exitScope();
            }
        }
    }

    private void bindParameters(final List<Parameter> parameters) {
        for (int ordinal = 0; ordinal < parameters.size(); ordinal++) {
            final Parameter parameter = parameters.get(ordinal);
            declarations.add(
                    symbols.declareParameter(
                            parameter.name(),
                            parameter.typeAnnotation(),
                            DEFAULT_PARAMETER_MUTABILITY,
                            ordinal,
                            parameter.span()));
        }
    }

    private void bindMain(final Stmt.MainFunction main) {
        final DeclarationResult result = symbols.declareMainFunction(main.span());
        declarations.add(result);
        if (result instanceof DeclarationResult.Declared declared) {
            symbols.enterScope(ScopeKind.FUNCTION, declared.symbol().id());
            try {
                bindBlock(main.body());
            } finally {
                symbols.exitScope();
            }
        }
    }

    private void bindIf(final Stmt.If conditional) {
        bindBlock(conditional.thenBranch());
        bindElseBranch(conditional.elseBranch());
    }

    private void bindElseBranch(final ElseBranch branch) {
        switch (branch) {
            case ElseBranch.None ignored -> {
                // No scope exists for an absent else branch.
            }
            case ElseBranch.Block block -> bindBlock(block.block());
            case ElseBranch.ElseIf elseIf -> bindIf(elseIf.ifStmt());
        }
    }

    private void bindWhile(final Stmt.While loop) {
        bindBlock(loop.body());
    }

    private void bindFor(final Stmt.For loop) {
        symbols.enterScope(ScopeKind.LOOP);
        try {
            loop.initializer().ifPresent(this::bindStatement);
            bindBlock(loop.body());
        } finally {
            symbols.exitScope();
        }
    }

    private void bindBlock(final Stmt.Block block) {
        symbols.enterScope(ScopeKind.BLOCK);
        try {
            for (final Stmt statement : block.statements()) {
                bindStatement(statement);
            }
        } finally {
            symbols.exitScope();
        }
    }
}
