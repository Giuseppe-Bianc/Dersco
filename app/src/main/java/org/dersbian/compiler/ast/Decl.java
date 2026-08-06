package org.dersbian.compiler.ast;

/** A declaration that introduces a named program entity. */
@SuppressWarnings("PMD.ShortClassName")
public sealed interface Decl extends Node
        permits FunctionDecl, ParamDecl, VariableDecl, ClassDecl {}
