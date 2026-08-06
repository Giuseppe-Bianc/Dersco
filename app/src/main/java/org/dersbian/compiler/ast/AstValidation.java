package org.dersbian.compiler.ast;

import java.util.List;
import java.util.Objects;
import org.dersbian.compiler.lexer.token.Span;

/** Shared constructor validation for AST value objects. */
public final class AstValidation {

    private AstValidation() {}

    /**
     * Returns {@code value} if non-null, otherwise throws with a message derived from {@code name}.
     */
    public static <T> T required(final T value, final String name) {
        return Objects.requireNonNull(value, name + " must not be null");
    }

    /** Validates that {@code value} is non-null and non-blank, returning it unchanged. */
    public static String name(final String value) {
        final String nonNullValue = required(value, "name");
        if (nonNullValue.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        return nonNullValue;
    }

    /** Returns {@code value} after a non-null check. */
    public static Span range(final Span value) {
        return required(value, "range");
    }

    /** Returns an unmodifiable copy of {@code values} after a non-null check. */
    public static <T> List<T> list(final List<T> values, final String name) {
        return List.copyOf(required(values, name));
    }
}
