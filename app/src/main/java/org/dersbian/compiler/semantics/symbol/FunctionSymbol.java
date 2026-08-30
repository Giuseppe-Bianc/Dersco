package org.dersbian.compiler.semantics.symbol;

import java.util.List;
import org.dersbian.compiler.syntax.ast.Type;

/** Symbol representing a named function and its declared signature. */
public interface FunctionSymbol extends Symbol {
    /**
     * Returns the immutable declared parameter descriptors.
     *
     * @return immutable declared parameter descriptors
     */
    List<ParameterDescriptor> parameters();

    /**
     * Returns the declared return type.
     *
     * @return declared return type
     */
    Type returnType();
}
