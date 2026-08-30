package org.dersbian.compiler.semantics.symbol;

import org.dersbian.compiler.syntax.ast.Type;

/** Symbol representing a variable binding. */
public interface VariableSymbol extends Symbol {
    /** @return declared type */
    Type type();

    /** @return retained mutability */
    Mutability mutability();
}
