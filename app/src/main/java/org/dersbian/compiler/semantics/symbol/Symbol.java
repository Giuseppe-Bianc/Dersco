package org.dersbian.compiler.semantics.symbol;

import java.util.List;
import java.util.Objects;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.Type;

/** Immutable semantic binding. */
public sealed interface Symbol
        permits Symbol.VariableSymbol,
                Symbol.ParameterSymbol,
                Symbol.FunctionSymbol,
                Symbol.MainFunctionSymbol {
    /** Returns the unique identifier assigned to this symbol. */
    SymbolId id();

    /** Returns the declared name of this symbol. */
    String name();

    /** Returns the semantic kind of this symbol. */
    SymbolKind kind();

    /** Returns the owning lexical scope identifier. */
    ScopeId scopeId();

    /** Returns the source span covering the declaration. */
    Span declarationSpan();

    /** Immutable variable binding. */
    record VariableSymbol(
            SymbolId id,
            String name,
            Type type,
            Mutability mutability,
            ScopeId scopeId,
            Span declarationSpan)
            implements Symbol {
        public VariableSymbol {
            validateCommon(id, name, scopeId, declarationSpan);
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(mutability, "mutability must not be null");
        }

        @Override
        public SymbolKind kind() {
            return SymbolKind.VARIABLE;
        }
    }

    /** Immutable parameter binding with its zero-based function ordinal. */
    record ParameterSymbol(
            SymbolId id,
            String name,
            Type type,
            Mutability mutability,
            int ordinal,
            ScopeId scopeId,
            Span declarationSpan)
            implements Symbol {
        public ParameterSymbol {
            validateCommon(id, name, scopeId, declarationSpan);
            Objects.requireNonNull(type, "type must not be null");
            Objects.requireNonNull(mutability, "mutability must not be null");
            if (ordinal < 0) {
                throw new IllegalArgumentException("ordinal must be non-negative");
            }
        }

        @Override
        public SymbolKind kind() {
            return SymbolKind.PARAMETER;
        }
    }

    /** Immutable function binding containing its ordered signature descriptors. */
    record FunctionSymbol(
            SymbolId id,
            String name,
            Type returnType,
            List<ParameterDescriptor> parameters,
            ScopeId scopeId,
            Span declarationSpan)
            implements Symbol {
        public FunctionSymbol {
            validateCommon(id, name, scopeId, declarationSpan);
            Objects.requireNonNull(returnType, "returnType must not be null");
            parameters =
                    List.copyOf(Objects.requireNonNull(parameters, "parameters must not be null"));
            validateParameters(parameters);
        }

        @Override
        public SymbolKind kind() {
            return SymbolKind.FUNCTION;
        }
    }

    /** Immutable global entry-point binding. */
    record MainFunctionSymbol(
            SymbolId id, String name, Type returnType, ScopeId scopeId, Span declarationSpan)
            implements Symbol {
        public MainFunctionSymbol {
            validateCommon(id, name, scopeId, declarationSpan);
            Objects.requireNonNull(returnType, "returnType must not be null");
            if (!"main".equals(name)) {
                throw new IllegalArgumentException("main function must be named main");
            }
            if (!(returnType instanceof Type.VoidT)) {
                throw new IllegalArgumentException("main function must return void");
            }
        }

        @Override
        public SymbolKind kind() {
            return SymbolKind.MAIN_FUNCTION;
        }
    }

    private static void validateCommon(
            final SymbolId id,
            final String name,
            final ScopeId scopeId,
            final Span declarationSpan) {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(scopeId, "scopeId must not be null");
        Objects.requireNonNull(declarationSpan, "declarationSpan must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }

    private static void validateParameters(final List<ParameterDescriptor> parameters) {
        for (int index = 0; index < parameters.size(); index++) {
            final ParameterDescriptor parameter = parameters.get(index);
            for (int previous = 0; previous < index; previous++) {
                if (parameters.get(previous).name().equals(parameter.name())) {
                    throw new IllegalArgumentException(
                            "duplicate parameter name: " + parameter.name());
                }
            }
        }
    }
}
