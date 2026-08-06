package org.dersbian.compiler.ast;

import java.util.List;
import org.dersbian.compiler.lexer.token.Span;

/** A class declaration with optional single inheritance. */
public record ClassDecl(
        String name,
        String superclass,
        List<VariableDecl> fields,
        List<FunctionDecl> methods,
        Span range)
        implements Decl {

    /** Validates and defensively copies the class declaration. */
    public ClassDecl {
        name = AstValidation.name(name);
        fields = AstValidation.list(fields, "fields");
        methods = AstValidation.list(methods, "methods");
        range = AstValidation.range(range);
    }

    @Override
    public NodeKind kind() {
        return NodeKind.CLASS_DECL;
    }

    @Override
    public <R> R accept(final AstVisitor<R> visitor) {
        return AstValidation.required(visitor, "visitor").visitClassDecl(this);
    }
}
