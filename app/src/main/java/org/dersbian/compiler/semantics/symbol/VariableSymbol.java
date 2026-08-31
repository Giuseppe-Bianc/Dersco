package org.dersbian.compiler.semantics.symbol;

import org.dersbian.compiler.syntax.ast.Type;

/** Symbol representing a variable binding. */
public interface VariableSymbol extends Symbol {
    /**
     * Returns the declared type.
     *
     * @return declared type
     */
    Type type();

    /**
     * Returns the retained mutability.
     *
     * @return retained mutability
     */
    Mutability mutability();
}
