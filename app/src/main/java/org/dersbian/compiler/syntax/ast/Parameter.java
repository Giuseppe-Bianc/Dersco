package org.dersbian.compiler.syntax.ast;

import java.util.Objects;
import org.dersbian.compiler.lexer.token.Span;

/**
 * Parameter definition in a function declaration.
 *
 * @param name parameter name
 * @param typeAnnotation annotated type of the parameter
 * @param span source location extent
 */
public record Parameter(String name, Type typeAnnotation, Span span) {
    /** Compact constructor that validates all fields. */
    public Parameter {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(typeAnnotation, "typeAnnotation must not be null");
        Objects.requireNonNull(span, "span must not be null");
    }
}
