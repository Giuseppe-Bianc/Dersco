package org.dersbian.compiler.ast;

/** An executable statement. */
@SuppressWarnings("PMD.ShortClassName")
public sealed interface Stmt extends Node
        permits Block,
                IfStmt,
                WhileStmt,
                ForStmt,
                ReturnStmt,
                BreakStmt,
                ContinueStmt,
                DeclStmt,
                ExprStmt {}
