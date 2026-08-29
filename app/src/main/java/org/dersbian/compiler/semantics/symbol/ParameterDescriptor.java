package org.dersbian.compiler.semantics.symbol;

import java.util.Objects;
import org.dersbian.compiler.syntax.ast.Type;

/** Immutable semantic descriptor for a function parameter. */
public record ParameterDescriptor(String name, Type type, int ordinal, Mutability mutability) {
    public ParameterDescriptor {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(mutability, "mutability must not be null");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        if (ordinal < 0) {
            throw new IllegalArgumentException("ordinal must be non-negative");
        }
    }
}
