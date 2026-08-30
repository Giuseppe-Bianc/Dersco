package org.dersbian.compiler.semantics.symbol;

import org.dersbian.compiler.syntax.ast.Type;

/** Symbol representing a function parameter binding. */
public interface ParameterSymbol extends Symbol {
    /**
     * Returns the declared parameter type.
     *
     * @return declared parameter type
     */
    Type type();

    /**
     * Returns the retained parameter mutability.
     *
     * @return retained parameter mutability
     */
    Mutability mutability();

    /**
     * Returns the zero-based position in the function parameter list.
     *
     * @return zero-based position in the function parameter list
     */
    int ordinal();
}
