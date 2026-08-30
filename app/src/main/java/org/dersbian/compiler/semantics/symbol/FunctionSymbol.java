package org.dersbian.compiler.semantics.symbol;

import java.util.List;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.syntax.ast.Type;

/** Symbol representing a named function and its declared signature. */
public interface FunctionSymbol extends Symbol {
    /** @return immutable declared parameter descriptors */
    List<ParameterDescriptor> parameters();

    /** @return declared return type */
    Type returnType();
}
