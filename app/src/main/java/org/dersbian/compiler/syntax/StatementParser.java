package org.dersbian.compiler.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.error.ErrorCode;
import org.dersbian.compiler.lexer.token.Token;
import org.dersbian.compiler.lexer.token.TokenKind;
import org.dersbian.compiler.syntax.ast.ElseBranch;
import org.dersbian.compiler.syntax.ast.Expr;
import org.dersbian.compiler.syntax.ast.Parameter;
import org.dersbian.compiler.syntax.ast.Stmt;
import org.dersbian.compiler.syntax.ast.Type;

/**
 * Recursive-descent statement parser.
 *
 * <p>Owns top-level dispatch by keyword and delegates expression parsing to {@link
 * ExpressionParser}. Each parse method advances the cursor and returns a {@link Stmt} variant from
 * {@link org.dersbian.compiler.syntax.ast.Stmt}. Errors are reported through the shared error list;
 * the cursor is left positioned for the next statement when possible.
 */
@SuppressWarnings({
    "PMD.CyclomaticComplexity",
    "PMD.CognitiveComplexity",
    "PMD.TooManyMethods",
    "PMD.ExcessiveImports",
    "PMD.CommentDefaultAccessModifier",
    "PMD.ShortVariable"
})
final class StatementParser {

    private final TokenCursor cursor;
    private final ExpressionParser exprParser;
    private final List<CompileError.SyntaxError> errors;

    /**
     * Creates a statement parser sharing cursor, expression parser, and error sink.
     *
     * @param cursor token cursor (not {@code null})
     * @param exprParser expression parser sharing the same cursor (not {@code null})
     * @param errors mutable error sink (not {@code null})
     */
    StatementParser(
            final TokenCursor cursor,
            final ExpressionParser exprParser,
            final List<CompileError.SyntaxError> errors) {
        this.cursor = Objects.requireNonNull(cursor, "cursor must not be null");
        this.exprParser = Objects.requireNonNull(exprParser, "exprParser must not be null");
        this.errors = Objects.requireNonNull(errors, "errors must not be null");
    }

    /**
     * Parses a single statement according to the current leading token.
     *
     * @return parsed statement; never {@code null}
     */
    @SuppressWarnings("PMD.CyclomaticComplexity")
    Stmt parseStatement() {
        final Token token = cursor.peek();
        final TokenKind kind = token.type();
        if (kind == TokenKind.Simple.Keyword.FUN) {
            return parseFunDecl();
        }
        if (kind == TokenKind.Simple.Keyword.VAR) {
            return parseVarDecl(true);
        }
        if (kind == TokenKind.Simple.Keyword.CONST) {
            return parseVarDecl(false);
        }
        if (kind == TokenKind.Simple.Keyword.IF) {
            return parseIf();
        }
        if (kind == TokenKind.Simple.Keyword.WHILE) {
            return parseWhile();
        }
        if (kind == TokenKind.Simple.Keyword.FOR) {
            return parseFor();
        }
        if (kind == TokenKind.Simple.Keyword.RETURN) {
            return parseReturn();
        }
        if (kind == TokenKind.Simple.Keyword.BREAK) {
            return parseBreak();
        }
        if (kind == TokenKind.Simple.Keyword.CONTINUE) {
            return parseContinue();
        }
        if (kind == TokenKind.Simple.Keyword.MAIN) {
            return parseMainBlock();
        }
        if (kind == TokenKind.Simple.Delimiter.OPEN_BRACE) {
            return parseBlock();
        }
        return parseExpressionStatement();
    }

    /**
     * Parses a block delimited by braces; missing close brace is reported but consumed.
     *
     * @return parsed block
     */
    Stmt.Block parseBlock() {
        final Token open = cursor.expect(TokenKind.Simple.Delimiter.OPEN_BRACE, errors);
        final List<Stmt> stmts = new ArrayList<>();
        while (!cursor.check(TokenKind.Simple.Delimiter.CLOSE_BRACE) && !cursor.isAtEnd()) {
            stmts.add(parseStatement());
        }
        final Token close = cursor.expect(TokenKind.Simple.Delimiter.CLOSE_BRACE, errors);
        return new Stmt.Block(stmts, open.span().merge(close.span()));
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    Stmt parseFunDecl() {
        final Token funToken = cursor.advance();
        final Token nameToken = cursor.peek();
        String name = "";
        if (nameToken.type() instanceof TokenKind.IdentifierAscii id) {
            name = id.value();
            cursor.advance();
        } else if (nameToken.type() instanceof TokenKind.IdentifierUnicode id) {
            name = id.value();
            cursor.advance();
        } else {
            errors.add(
                    CompileError.syntaxError(
                            ErrorCode.E1001,
                            "Expected function name, found " + nameToken.type(),
                            nameToken.span(),
                            null));
        }
        cursor.expect(TokenKind.Simple.Delimiter.OPEN_PAREN, errors);
        final List<Parameter> params = parseParamList();
        cursor.expect(TokenKind.Simple.Delimiter.CLOSE_PAREN, errors);
        cursor.expect(TokenKind.Simple.Operator.COLON, errors);
        final Type returnType = parseType();
        final Stmt.Block body = parseBlock();
        return new Stmt.Function(
                name, params, returnType, body, funToken.span().merge(body.span()));
    }

    private List<Parameter> parseParamList() {
        final List<Parameter> params = new ArrayList<>();
        if (cursor.check(TokenKind.Simple.Delimiter.CLOSE_PAREN)) {
            return params;
        }
        params.add(parseParam());
        while (cursor.check(TokenKind.Simple.Operator.COMMA)) {
            cursor.advance();
            params.add(parseParam());
        }
        return params;
    }

    private Parameter parseParam() {
        final Token nameToken = cursor.peek();
        String name = "";
        if (nameToken.type() instanceof TokenKind.IdentifierAscii id) {
            name = id.value();
            cursor.advance();
        } else if (nameToken.type() instanceof TokenKind.IdentifierUnicode id) {
            name = id.value();
            cursor.advance();
        } else {
            errors.add(
                    CompileError.syntaxError(
                            ErrorCode.E1001,
                            "Expected parameter name, found " + nameToken.type(),
                            nameToken.span(),
                            null));
        }
        cursor.expect(TokenKind.Simple.Operator.COLON, errors);
        final Type type = parseType();
        return new Parameter(name, type, nameToken.span());
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    Stmt parseVarDecl(final boolean isMutable) {
        final Token kwToken = cursor.advance();
        final List<Stmt.VarBinding> bindings = new ArrayList<>();
        bindings.add(parseBindingName());
        while (cursor.check(TokenKind.Simple.Operator.COMMA)) {
            cursor.advance();
            bindings.add(parseBindingName());
        }
        Type typeAnnotation;
        if (cursor.check(TokenKind.Simple.Operator.COLON)) {
            cursor.advance();
            typeAnnotation = parseType();
        } else {
            typeAnnotation = new Type.Custom("");
        }
        if (cursor.check(TokenKind.Simple.Operator.EQUAL)) {
            cursor.advance();
            parseInitializers(bindings);
        }
        return new Stmt.VarDeclaration(bindings, typeAnnotation, isMutable, kwToken.span());
    }

    private Stmt.VarBinding parseBindingName() {
        final Token nameToken = cursor.peek();
        String name = "";
        if (nameToken.type() instanceof TokenKind.IdentifierAscii id) {
            name = id.value();
            cursor.advance();
        } else if (nameToken.type() instanceof TokenKind.IdentifierUnicode id) {
            name = id.value();
            cursor.advance();
        } else {
            errors.add(
                    CompileError.syntaxError(
                            ErrorCode.E1001,
                            "Expected variable name, found " + nameToken.type(),
                            nameToken.span(),
                            null));
        }
        return new Stmt.VarBinding(name, Optional.empty());
    }

    private void parseInitializers(final List<Stmt.VarBinding> bindings) {
        final List<Expr> exprs = new ArrayList<>();
        exprs.add(exprParser.parseExpression(0));
        while (cursor.check(TokenKind.Simple.Operator.COMMA)) {
            cursor.advance();
            exprs.add(exprParser.parseExpression(0));
        }
        for (int i = 0; i < bindings.size(); i++) {
            final Optional<Expr> value =
                    i < exprs.size() ? Optional.of(exprs.get(i)) : Optional.empty();
            bindings.set(i, new Stmt.VarBinding(bindings.get(i).name(), value));
        }
    }

    Stmt parseIf() {
        final Token ifToken = cursor.advance();
        cursor.expect(TokenKind.Simple.Delimiter.OPEN_PAREN, errors);
        final Expr condition = exprParser.parseExpression(0);
        cursor.expect(TokenKind.Simple.Delimiter.CLOSE_PAREN, errors);
        final Stmt.Block thenBranch = parseBlock();
        final ElseBranch elseBranch;
        if (cursor.check(TokenKind.Simple.Keyword.ELSE)) {
            cursor.advance();
            if (cursor.check(TokenKind.Simple.Keyword.IF)) {
                elseBranch = new ElseBranch.ElseIf((Stmt.If) parseIf());
            } else {
                elseBranch = new ElseBranch.Block(parseBlock());
            }
        } else {
            elseBranch = new ElseBranch.None();
        }
        return new Stmt.If(
                condition, thenBranch, elseBranch, ifToken.span().merge(thenBranch.span()));
    }

    Stmt parseWhile() {
        final Token whileToken = cursor.advance();
        cursor.expect(TokenKind.Simple.Delimiter.OPEN_PAREN, errors);
        final Expr condition = exprParser.parseExpression(0);
        cursor.expect(TokenKind.Simple.Delimiter.CLOSE_PAREN, errors);
        final Stmt.Block body = parseBlock();
        return new Stmt.While(condition, body, whileToken.span().merge(body.span()));
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    Stmt parseFor() {
        final Token forToken = cursor.advance();
        cursor.expect(TokenKind.Simple.Delimiter.OPEN_PAREN, errors);
        final Optional<Stmt> initializer;
        if (cursor.check(TokenKind.Simple.Special.SEMICOLON)) {
            initializer = Optional.empty();
        } else if (cursor.check(TokenKind.Simple.Keyword.VAR)) {
            initializer = Optional.of(parseVarDecl(true));
        } else if (cursor.check(TokenKind.Simple.Keyword.CONST)) {
            initializer = Optional.of(parseVarDecl(false));
        } else {
            initializer = Optional.of(parseExpressionStatement());
        }
        cursor.expect(TokenKind.Simple.Special.SEMICOLON, errors);
        final Optional<Expr> condition;
        if (cursor.check(TokenKind.Simple.Special.SEMICOLON)) {
            condition = Optional.empty();
        } else {
            condition = Optional.of(exprParser.parseExpression(0));
        }
        cursor.expect(TokenKind.Simple.Special.SEMICOLON, errors);
        final Optional<Expr> increment;
        if (cursor.check(TokenKind.Simple.Delimiter.CLOSE_PAREN)) {
            increment = Optional.empty();
        } else {
            increment = Optional.of(exprParser.parseExpression(0));
        }
        cursor.expect(TokenKind.Simple.Delimiter.CLOSE_PAREN, errors);
        final Stmt.Block body = parseBlock();
        return new Stmt.For(
                initializer, condition, increment, body, forToken.span().merge(body.span()));
    }

    Stmt parseReturn() {
        final Token retToken = cursor.advance();
        final Optional<Expr> value;
        if (cursor.check(TokenKind.Simple.Delimiter.CLOSE_BRACE) || cursor.isAtEnd()) {
            value = Optional.empty();
        } else {
            value = Optional.of(exprParser.parseExpression(0));
        }
        return new Stmt.Return(value, retToken.span());
    }

    Stmt parseBreak() {
        final Token brkToken = cursor.advance();
        return new Stmt.Break(brkToken.span());
    }

    Stmt parseContinue() {
        final Token contToken = cursor.advance();
        return new Stmt.Continue(contToken.span());
    }

    Stmt parseMainBlock() {
        final Token mainToken = cursor.advance();
        final Stmt.Block body = parseBlock();
        return new Stmt.MainFunction(body, mainToken.span().merge(body.span()));
    }

    Stmt parseExpressionStatement() {
        final Expr expr = exprParser.parseExpression(0);
        return new Stmt.Expression(expr);
    }

    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.CognitiveComplexity"})
    Type parseType() {
        final Token token = cursor.peek();
        final TokenKind kind = token.type();
        Type base = null;
        if (kind instanceof TokenKind.Simple.TypeKeyword tk) {
            cursor.advance();
            base =
                    switch (tk) {
                        case I8 -> new Type.I8();
                        case I16 -> new Type.I16();
                        case I32 -> new Type.I32();
                        case I64 -> new Type.I64();
                        case U8 -> new Type.U8();
                        case U16 -> new Type.U16();
                        case U32 -> new Type.U32();
                        case U64 -> new Type.U64();
                        case F32 -> new Type.F32();
                        case F64 -> new Type.F64();
                        case CHAR -> new Type.Char();
                        case STRING -> new Type.StringT();
                        case BOOL -> new Type.Bool();
                    };
        } else if (kind instanceof TokenKind.IdentifierAscii id) {
            base = new Type.Custom(id.value());
            cursor.advance();
        } else if (kind instanceof TokenKind.IdentifierUnicode id) {
            base = new Type.Custom(id.value());
            cursor.advance();
        } else {
            errors.add(
                    CompileError.syntaxError(
                            ErrorCode.E1003,
                            "Expected type annotation, found " + kind,
                            token.span(),
                            null));
            cursor.advance();
            return new Type.Custom("?");
        }
        Type result = base;
        while (cursor.check(TokenKind.Simple.Delimiter.OPEN_BRACKET)) {
            cursor.advance();
            final Expr size = exprParser.parseExpression(0);
            cursor.expect(TokenKind.Simple.Delimiter.CLOSE_BRACKET, errors);
            result = new Type.Array(result, size);
        }
        return result;
    }
}
