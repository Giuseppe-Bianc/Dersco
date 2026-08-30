package org.dersbian.compiler.semantics.symbol;

import org.dersbian.compiler.syntax.ast.Type;

/** Symbol representing a function parameter binding. */
public interface ParameterSymbol extends Symbol {
    /**
     * @return declared parameter type
     */
    Type type();

    /**
     * @return retained parameter mutability
     */
    Mutability mutability();

    /**
     * @return zero-based position in the function parameter list
     */
    int ordinal();
}
