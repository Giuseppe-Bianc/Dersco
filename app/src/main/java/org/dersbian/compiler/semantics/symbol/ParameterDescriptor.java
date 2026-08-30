package org.dersbian.compiler.semantics.symbol;

import java.util.List;
import org.dersbian.compiler.syntax.ast.Type;

/** Immutable descriptor of one parameter in a function signature. */
public record ParameterDescriptor(String name, Type type, Mutability mutability) {
    /** Validates and defensively normalizes the descriptor values. */
    public ParameterDescriptor {
        if (name == null) {
            throw new NullPointerException("name must not be null");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name must not be empty");
        }
        if (type == null) {
            throw new NullPointerException("type must not be null");
        }
        if (mutability == null) {
            throw new NullPointerException("mutability must not be null");
        }
    }

    /** Copies a parameter descriptor list into an immutable list. */
    public static List<ParameterDescriptor> immutableCopy(final List<ParameterDescriptor> parameters) {
        if (parameters == null) {
            throw new NullPointerException("parameters must not be null");
        }
        return List.copyOf(parameters);
    }
}
