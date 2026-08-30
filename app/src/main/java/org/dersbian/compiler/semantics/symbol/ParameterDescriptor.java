package org.dersbian.compiler.semantics.symbol;

import java.util.List;
import java.util.Objects;
import org.dersbian.compiler.syntax.ast.Type;

/** Immutable descriptor of one parameter in a function signature. */
public record ParameterDescriptor(String name, Type type, Mutability mutability) {
    /** Validates and defensively normalizes the descriptor values. */
    public ParameterDescriptor {
        Objects.requireNonNull(name, "name must not be null");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name must not be empty");
        }
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(mutability, "mutability must not be null");
    }

    /** Copies a parameter descriptor list into an immutable list. */
    public static List<ParameterDescriptor> immutableCopy(
            final List<ParameterDescriptor> parameters) {
        Objects.requireNonNull(parameters, "parameters must not be null");
        return List.copyOf(parameters);
    }
}
