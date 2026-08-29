package org.dersbian.compiler.semantics.symbol;

import java.util.Objects;
import org.dersbian.compiler.syntax.ast.Type;

/** Immutable descriptor used to define a function parameter in a signature. */
public record ParameterDescriptor(String name, Type type, Mutability mutability) {
    public ParameterDescriptor {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(mutability, "mutability must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
    }
}
