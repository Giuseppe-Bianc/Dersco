package org.dersbian.compiler.ast;

import java.util.List;
import org.dersbian.compiler.lexer.token.Span;

/** A named function declaration. */
public record FunctionDecl(
        String name, List<ParamDecl> params, TypeNode returnType, Block body, Span range)
        implements Decl {

    /** Validates and defensively copies the function declaration. */
    public FunctionDecl {
        name = AstValidation.name(name);
        params = AstValidation.list(params, "params");
        returnType = AstValidation.required(returnType, "returnType");
        body = AstValidation.required(body, "body");
        range = AstValidation.range(range);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.FUNCTION_DECL;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitFunctionDecl(this);
    }
}
