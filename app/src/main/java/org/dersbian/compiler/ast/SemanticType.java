package org.dersbian.compiler.ast;

import java.util.List;

/** A canonical type produced by semantic analysis rather than source parsing. */
public sealed interface SemanticType
        permits SemanticType.Primitive,
                SemanticType.Array,
                SemanticType.ClassReference,
                SemanticType.Function {

    /** A resolved primitive type. */
    record Primitive(PrimitiveTypeName name) implements SemanticType {

        /** Validates the primitive type name. */
        public Primitive {
            name = AstValidation.required(name, "name");
        }
    }

    /** A resolved array type. */
    record Array(SemanticType elementType) implements SemanticType {

        /** Validates the array element type. */
        public Array {
            elementType = AstValidation.required(elementType, "elementType");
        }
    }

    /** A resolved class type. */
    record ClassReference(ClassDecl declaration) implements SemanticType {

        /** Validates the declared class. */
        public ClassReference {
            declaration = AstValidation.required(declaration, "declaration");
        }
    }

    /** A resolved function signature. */
    record Function(List<SemanticType> parameterTypes, SemanticType returnType)
            implements SemanticType {

        /** Validates and defensively copies the signature. */
        public Function {
            parameterTypes = AstValidation.list(parameterTypes, "parameterTypes");
            returnType = AstValidation.required(returnType, "returnType");
        }
    }
}
