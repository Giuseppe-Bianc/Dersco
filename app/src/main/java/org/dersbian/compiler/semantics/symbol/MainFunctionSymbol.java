package org.dersbian.compiler.semantics.symbol;

import org.dersbian.compiler.syntax.ast.Type;

/** Symbol representing the unique global {@code main} function. */
public interface MainFunctionSymbol extends Symbol {
    /**
     * Returns the {@code void} return type.
     *
     * @return the {@code void} return type
     */
    Type returnType();
}
