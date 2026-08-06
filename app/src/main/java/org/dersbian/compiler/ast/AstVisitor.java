package org.dersbian.compiler.ast;

/** Visitor for all concrete AST nodes, with a shared default dispatch. */
@SuppressWarnings("PMD.TooManyMethods")
public interface AstVisitor<R> {

    /** Handles a node whose concrete visitor method was not overridden. */
    default R visitNode(final Node node) {
        throw new UnsupportedOperationException("No visitor method implemented for " + node.kind());
    }

    /** Visits a program root. */
    default R visitProgram(final Program node) {
        return visitNode(node);
    }

    /** Visits a function declaration. */
    default R visitFunctionDecl(final FunctionDecl node) {
        return visitNode(node);
    }

    /** Visits a parameter declaration. */
    default R visitParamDecl(final ParamDecl node) {
        return visitNode(node);
    }

    /** Visits a variable declaration. */
    default R visitVariableDecl(final VariableDecl node) {
        return visitNode(node);
    }

    /** Visits a class declaration. */
    default R visitClassDecl(final ClassDecl node) {
        return visitNode(node);
    }

    /** Visits a block. */
    default R visitBlock(final Block node) {
        return visitNode(node);
    }

    /** Visits a conditional statement. */
    default R visitIfStmt(final IfStmt node) {
        return visitNode(node);
    }

    /** Visits a while statement. */
    default R visitWhileStmt(final WhileStmt node) {
        return visitNode(node);
    }

    /** Visits a for statement. */
    default R visitForStmt(final ForStmt node) {
        return visitNode(node);
    }

    /** Visits a return statement. */
    default R visitReturnStmt(final ReturnStmt node) {
        return visitNode(node);
    }

    /** Visits a break statement. */
    default R visitBreakStmt(final BreakStmt node) {
        return visitNode(node);
    }

    /** Visits a continue statement. */
    default R visitContinueStmt(final ContinueStmt node) {
        return visitNode(node);
    }

    /** Visits a declaration statement. */
    default R visitDeclStmt(final DeclStmt node) {
        return visitNode(node);
    }

    /** Visits an expression statement. */
    default R visitExprStmt(final ExprStmt node) {
        return visitNode(node);
    }

    /** Visits an integer literal. */
    default R visitIntLiteral(final IntLiteral node) {
        return visitNode(node);
    }

    /** Visits a floating-point literal. */
    default R visitFloatLiteral(final FloatLiteral node) {
        return visitNode(node);
    }

    /** Visits a Boolean literal. */
    default R visitBoolLiteral(final BoolLiteral node) {
        return visitNode(node);
    }

    /** Visits a string literal. */
    default R visitStringLiteral(final StringLiteral node) {
        return visitNode(node);
    }

    /** Visits a null literal. */
    default R visitNullLiteral(final NullLiteral node) {
        return visitNode(node);
    }

    /** Visits a name expression. */
    default R visitNameExpr(final NameExpr node) {
        return visitNode(node);
    }

    /** Visits a binary expression. */
    default R visitBinaryExpr(final BinaryExpr node) {
        return visitNode(node);
    }

    /** Visits a unary expression. */
    default R visitUnaryExpr(final UnaryExpr node) {
        return visitNode(node);
    }

    /** Visits an assignment expression. */
    default R visitAssignExpr(final AssignExpr node) {
        return visitNode(node);
    }

    /** Visits a call expression. */
    default R visitCallExpr(final CallExpr node) {
        return visitNode(node);
    }

    /** Visits a member-access expression. */
    default R visitMemberAccessExpr(final MemberAccessExpr node) {
        return visitNode(node);
    }

    /** Visits an index expression. */
    default R visitIndexExpr(final IndexExpr node) {
        return visitNode(node);
    }

    /** Visits a construction expression. */
    default R visitNewExpr(final NewExpr node) {
        return visitNode(node);
    }

    /** Visits a cast expression. */
    default R visitCastExpr(final CastExpr node) {
        return visitNode(node);
    }

    /** Visits a primitive type node. */
    default R visitPrimitiveTypeNode(final PrimitiveTypeNode node) {
        return visitNode(node);
    }

    /** Visits an array type node. */
    default R visitArrayTypeNode(final ArrayTypeNode node) {
        return visitNode(node);
    }

    /** Visits a class type node. */
    default R visitClassTypeNode(final ClassTypeNode node) {
        return visitNode(node);
    }
}
