package org.dersbian.compiler.ast;

/** A type reference as it appeared in source code. */
public sealed interface TypeNode extends Node
        permits PrimitiveTypeNode, ArrayTypeNode, ClassTypeNode {}
