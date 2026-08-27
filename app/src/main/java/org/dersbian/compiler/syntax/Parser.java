package org.dersbian.compiler.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.dersbian.compiler.CompilerException;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.error.ErrorCode;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.lexer.token.Token;
import org.dersbian.compiler.lexer.token.TokenKind;
import org.dersbian.compiler.syntax.ast.BinaryOp;
import org.dersbian.compiler.syntax.ast.ElseBranch;
import org.dersbian.compiler.syntax.ast.Expr;
import org.dersbian.compiler.syntax.ast.Parameter;
import org.dersbian.compiler.syntax.ast.Stmt;
import org.dersbian.compiler.syntax.ast.Type;
import org.dersbian.compiler.syntax.ast.UnaryOp;
import org.dersbian.compiler.syntax.ast.UnaryOpSide;

/**
 * Recursive-descent / Pratt parser that turns a stream of {@link Token}s produced by the lexer into
 * an abstract syntax tree of {@link Stmt} nodes, collecting {@link CompileError}s for any syntax
 * problems encountered along the way.
 */
@SuppressWarnings({
    "PMD.LongVariable",
    "PMD.ShortVariable",
    "PMD.CyclomaticComplexity",
    "PMD.NPathComplexity",
    "PMD.OnlyOneReturn",
    "PMD.TooManyMethods",
    "PMD.CognitiveComplexity"
})
public final class Parser {
    /** Maximum allowed expression parsing recursion depth, used to guard against stack overflow. */
    private static final int MAX_RECURSION_DEPTH = 1000;

    /** The full stream of tokens to be parsed. */
    private final List<Token> tokens;

    /** Index of the token currently being examined in {@link #tokens}. */
    private int current;

    /** Syntax errors accumulated while parsing. */
    private final List<CompileError> errors;

    /**
     * Current depth of recursive expression parsing, used to enforce {@link #MAX_RECURSION_DEPTH}.
     */
    private int recursionDepth;

    /**
     * Creates a new parser over the given token stream.
     *
     * @param tokens the tokens to parse; must not be {@code null}
     */
    public Parser(final List<Token> tokens) {
        this.tokens = List.copyOf(tokens);
        this.current = 0;
        this.errors = new ArrayList<>(8);
        this.recursionDepth = 0;
    }

    /** Parses all statements and returns statements plus accumulated syntax errors. */
    public ParseResult parse() {
        final List<Stmt> statements = new ArrayList<>(tokens.size() / 4);
        while (!isAtEnd()) {
            final Stmt stmt = parseStmt();
            if (stmt != null) {
                statements.add(stmt);
            } else {
                // Error recovery: skip the problematic token.
                advance();
            }
        }
        return new ParseResult(statements, errors);
    }

    private boolean checkRecursionLimit() {
        if (recursionDepth > MAX_RECURSION_DEPTH) {
            final Token token = peek();
            if (token != null) {
                errors.add(
                        new CompileError.SyntaxError(
                                Optional.of(ErrorCode.E1001),
                                "Maximum recursion depth exceeded",
                                token.span(),
                                Optional.of(
                                        "Simplify the expression or break it into smaller parts")));
            }
            return true;
        }
        return false;
    }

    private void enterRecursion() {
        recursionDepth += 1;
    }

    private void exitRecursion() {
        if (recursionDepth > 0) {
            recursionDepth -= 1;
        }
    }

    private Stmt parseStmt() {
        final Token token = peek();
        if (token == null) {
            return null;
        }

        return switch (token.type()) {
            case TokenKind.Simple.Keyword.FUN -> parseFunction();
            case TokenKind.Simple.Keyword.MAIN -> parseMainFunction();
            case TokenKind.Simple.Keyword.IF -> parseIf();
            case TokenKind.Simple.Keyword.VAR, TokenKind.Simple.Keyword.CONST ->
                    parseVarDeclaration();
            case TokenKind.Simple.Keyword.RETURN -> parseReturn();
            case TokenKind.Simple.Keyword.WHILE -> parseWhile();
            case TokenKind.Simple.Keyword.FOR -> parseFor();
            case TokenKind.Simple.Keyword.BREAK -> parseBreak();
            case TokenKind.Simple.Keyword.CONTINUE -> parseContinue();
            case TokenKind.Simple.Delimiter.OPEN_BRACE -> parseBlockStmt();
            default -> parseExpressionStmt();
        };
    }

    private Stmt parseMainFunction() {
        final Token startToken = advance();
        if (startToken == null) {
            return null;
        }
        final Stmt body = parseBlockStmt();
        if (body == null) {
            return null;
        }

        final Span endSpan = body.span();
        final Span functionSpan = startToken.span().merge(endSpan);
        return new Stmt.MainFunction((Stmt.Block) body, functionSpan);
    }

    private Stmt parseBreak() {
        final Span span = advanceAndGetSpan();
        return span == null ? null : new Stmt.Break(span);
    }

    private Stmt parseContinue() {
        final Span span = advanceAndGetSpan();
        return span == null ? null : new Stmt.Continue(span);
    }

    private Span advanceAndGetSpan() {
        final Token token = advance();
        return token == null ? null : token.span();
    }

    private Stmt parseBlockStmt() {
        if (!matchToken(TokenKind.Simple.Delimiter.OPEN_BRACE)) {
            return null;
        }
        final Token startToken = previous();

        final List<Stmt> statements = new ArrayList<>();
        while (!check(TokenKind.Simple.Delimiter.CLOSE_BRACE) && !isAtEnd()) {
            final Stmt stmt = parseStmt();
            if (stmt != null) {
                statements.add(stmt);
            } else {
                advance();
            }
        }

        expect(TokenKind.Simple.Delimiter.CLOSE_BRACE, "end of block");
        return new Stmt.Block(statements, mergedSpan(startToken));
    }

    private Stmt parseReturn() {
        final Token startToken = advance();
        if (startToken == null) {
            return null;
        }

        final Expr returnValue = isEndOfStatement() ? null : parseExpr(0);
        final Span span = calculateReturnSpan(startToken, returnValue);
        return new Stmt.Return(Optional.ofNullable(returnValue), span);
    }

    private boolean isEndOfStatement() {
        final Token token = peek();
        if (token == null) {
            return false;
        }
        return switch (token.type()) {
            case TokenKind.Simple.Delimiter.CLOSE_BRACE,
                    TokenKind.Simple.Special.EOF,
                    TokenKind.Simple.Special.SEMICOLON ->
                    true;
            default -> false;
        };
    }

    private Span calculateReturnSpan(final Token start, final Expr value) {
        return value == null ? start.span() : start.span().merge(value.span());
    }

    private Stmt parseFunction() {
        final Token startToken = advance();
        if (startToken == null) {
            return null;
        }

        final String name = consumeIdentifier();
        if (name == null) {
            return null;
        }
        // final Token nameToken = previous();
        // final Span _ = nameToken == null ? startToken.span() : nameToken.span();

        expect(TokenKind.Simple.Delimiter.OPEN_PAREN, "after function name");
        final List<Parameter> params = new ArrayList<>();
        while (!check(TokenKind.Simple.Delimiter.CLOSE_PAREN) && !isAtEnd()) {
            final Token paramStart = peek();
            if (paramStart == null) {
                break;
            }

            final String parameterName = consumeIdentifier();
            if (parameterName == null) {
                return null;
            }
            final Token previousName = previous();
            final Span parameterNameSpan =
                    previousName == null ? paramStart.span() : previousName.span();

            expect(TokenKind.Simple.Operator.COLON, "after parameter name");
            final Type typeAnnotation = parseType();
            if (typeAnnotation == null) {
                return null;
            }
            final Token previousType = previous();
            final Span typeSpan = previousType == null ? parameterNameSpan : previousType.span();
            final Span parameterSpan = parameterNameSpan.merge(typeSpan);
            params.add(new Parameter(parameterName, typeAnnotation, parameterSpan));

            if (!matchToken(TokenKind.Simple.Operator.COMMA)) {
                break;
            }
        }
        expect(TokenKind.Simple.Delimiter.CLOSE_PAREN, "after parameter list");

        final Type returnType = matchToken(TokenKind.Simple.Operator.COLON) ? parseType() : null;
        final Stmt body = parseBlockStmt();
        if (body == null) {
            return null;
        }

        final Span functionSpan = startToken.span().merge(body.span());
        return new Stmt.Function(
                name,
                params,
                returnType == null ? new Type.VoidT() : returnType,
                (Stmt.Block) body,
                functionSpan);
    }

    private Expr parseCondition(final String keyword) {
        expect(TokenKind.Simple.Delimiter.OPEN_PAREN, "after '" + keyword + "'");
        final Expr condition = parseExpr(0);
        if (condition == null) {
            return null;
        }
        expect(TokenKind.Simple.Delimiter.CLOSE_PAREN, "after the condition");
        return condition;
    }

    private Stmt parseIf() {
        final Token startToken = advance();
        if (startToken == null) {
            return null;
        }
        final Expr condition = parseCondition("if");
        if (condition == null) {
            return null;
        }
        final Stmt thenBranch = parseBlockStmt();
        if (thenBranch == null) {
            return null;
        }

        final List<Stmt> elseBranch;
        if (matchToken(TokenKind.Simple.Keyword.ELSE)) {
            final Stmt elseStmt = parseStmt();
            if (elseStmt == null) {
                return null;
            }
            elseBranch = List.of(elseStmt);
        } else {
            elseBranch = null;
        }

        final ElseBranch parsedElse;
        if (elseBranch == null) {
            parsedElse = new ElseBranch.None();
        } else {
            final Stmt elseStmt = elseBranch.get(0);
            parsedElse =
                    switch (elseStmt) {
                        case Stmt.Block block -> new ElseBranch.Block(block);
                        case Stmt.If ifStmt -> new ElseBranch.ElseIf(ifStmt);
                        default -> {
                            errors.add(
                                    new CompileError.SyntaxError(
                                            Optional.of(ErrorCode.E1004),
                                            "Invalid else branch",
                                            elseStmt.span(),
                                            Optional.of(
                                                    "The else branch must be a block or else-if")));
                            yield new ElseBranch.None();
                        }
                    };
        }

        return new Stmt.If(condition, (Stmt.Block) thenBranch, parsedElse, mergedSpan(startToken));
    }

    private Stmt parseWhile() {
        final Token startToken = advance();
        if (startToken == null) {
            return null;
        }
        final Expr condition = parseCondition("while");
        if (condition == null) {
            return null;
        }
        final Stmt body = parseBlockStmt();
        if (body == null) {
            return null;
        }
        return new Stmt.While(condition, (Stmt.Block) body, startToken.span().merge(body.span()));
    }

    private Stmt parseFor() {
        final Token startToken = advance();
        if (startToken == null) {
            return null;
        }
        expect(TokenKind.Simple.Delimiter.OPEN_PAREN, "after 'for'");

        final Stmt initializer = parseForInitializer();
        final Expr condition;
        if (check(TokenKind.Simple.Special.SEMICOLON)) {
            advance();
            condition = null;
        } else {
            condition = parseExpr(0);
            expect(TokenKind.Simple.Special.SEMICOLON, "after for loop condition");
        }

        final Expr increment = check(TokenKind.Simple.Delimiter.CLOSE_PAREN) ? null : parseExpr(0);
        expect(TokenKind.Simple.Delimiter.CLOSE_PAREN, "after for loop clauses");

        final Stmt bodyStmt = parseStmt();
        if (bodyStmt == null) {
            return null;
        }

        final List<Stmt> body;
        if (bodyStmt instanceof Stmt.Block block) {
            body = block.statements();
        } else {
            body = List.of(bodyStmt);
        }

        Span endSpan = null;
        if (!body.isEmpty()) {
            endSpan = body.get(body.size() - 1).span();
        }
        if (endSpan == null) {
            final Token previous = previous();
            endSpan = previous == null ? startToken.span() : previous.span();
        }
        final Span span = startToken.span().merge(endSpan);
        final Stmt.Block forBody =
                bodyStmt instanceof Stmt.Block block
                        ? block
                        : new Stmt.Block(body, bodyStmt.span());
        return new Stmt.For(
                Optional.ofNullable(initializer),
                Optional.ofNullable(condition),
                Optional.ofNullable(increment),
                forBody,
                span);
    }

    private Stmt parseForInitializer() {
        if (matchToken(TokenKind.Simple.Special.SEMICOLON)) {
            return null;
        }

        final Stmt stmt =
                check(TokenKind.Simple.Keyword.VAR) || check(TokenKind.Simple.Keyword.CONST)
                        ? parseVarDeclaration()
                        : parseExpressionStmt();
        expect(TokenKind.Simple.Special.SEMICOLON, "after for loop initializer");
        return stmt;
    }

    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    private Type parseType() {
        final Token token = advance();
        if (token == null) {
            return null;
        }

        Type baseType =
                switch (token.type()) {
                    case TokenKind.Simple.TypeKeyword.I8 -> new Type.I8();
                    case TokenKind.Simple.TypeKeyword.I16 -> new Type.I16();
                    case TokenKind.Simple.TypeKeyword.I32 -> new Type.I32();
                    case TokenKind.Simple.TypeKeyword.I64 -> new Type.I64();
                    case TokenKind.Simple.TypeKeyword.U8 -> new Type.U8();
                    case TokenKind.Simple.TypeKeyword.U16 -> new Type.U16();
                    case TokenKind.Simple.TypeKeyword.U32 -> new Type.U32();
                    case TokenKind.Simple.TypeKeyword.U64 -> new Type.U64();
                    case TokenKind.Simple.TypeKeyword.F32 -> new Type.F32();
                    case TokenKind.Simple.TypeKeyword.F64 -> new Type.F64();
                    case TokenKind.Simple.TypeKeyword.CHAR -> new Type.Char();
                    case TokenKind.Simple.TypeKeyword.STRING -> new Type.StringT();
                    case TokenKind.Simple.TypeKeyword.BOOL -> new Type.Bool();
                    case TokenKind.IdentifierAscii identifier ->
                            new Type.Custom(identifier.value());
                    case TokenKind.IdentifierUnicode identifier ->
                            new Type.Custom(identifier.value());
                    default -> {
                        syntaxError(
                                "Invalid type specification, expected primitive type or custom"
                                        + " identifier",
                                token,
                                "Try using a primitive type (like i32, f64) or a custom type"
                                        + " identifier",
                                ErrorCode.E1002);
                        yield null;
                    }
                };

        if (baseType == null) {
            return null;
        }

        final List<Expr> dimensions = new ArrayList<>();
        while (matchToken(TokenKind.Simple.Delimiter.OPEN_BRACKET)) {
            final Expr sizeExpr = parseExpr(0);
            if (sizeExpr == null) {
                return null;
            }
            expect(TokenKind.Simple.Delimiter.CLOSE_BRACKET, "after array size");
            dimensions.add(sizeExpr);
        }

        for (int i = dimensions.size() - 1; i >= 0; i--) {
            baseType = new Type.Array(baseType, dimensions.get(i));
        }

        if (baseType instanceof Type.Custom custom
                && "vector".equals(custom.name())
                && matchToken(TokenKind.Simple.Operator.LESS)) {
            final Type innerType = parseType();
            if (innerType == null) {
                return null;
            }
            expect(TokenKind.Simple.Operator.GREATER, "after vector inner type");
            baseType = new Type.Vector(innerType);
        }

        return baseType;
    }

    private Stmt parseVarDeclaration() {
        final Token startToken;
        final boolean isMutable;
        if (matchToken(TokenKind.Simple.Keyword.CONST)) {
            startToken = previous();
            isMutable = false;
        } else if (matchToken(TokenKind.Simple.Keyword.VAR)) {
            startToken = previous();
            isMutable = true;
        } else {
            return null;
        }

        if (startToken == null) {
            return null;
        }

        final List<String> variables = new ArrayList<>();
        while (true) {
            final String name = consumeIdentifier();
            if (name == null) {
                break;
            }
            variables.add(name);
            if (!matchToken(TokenKind.Simple.Operator.COMMA)) {
                break;
            }
        }

        if (variables.isEmpty()) {
            syntaxError(
                    "Expected at least one variable name",
                    startToken,
                    "Provide at least one variable name after 'var' or 'const'",
                    ErrorCode.E1008);
            return null;
        }

        expect(TokenKind.Simple.Operator.COLON, "after variable name(s)");
        Type typeAnnotation = parseType();
        if (typeAnnotation == null) {
            reportPeekError(
                    "Invalid type specification",
                    "Try using a primitive type or a custom type identifier");
            typeAnnotation = new Type.VoidT();
        }

        expect(TokenKind.Simple.Operator.EQUAL, "after type annotation");
        final List<Expr> initializers = new ArrayList<>(variables.size());
        while (true) {
            final Expr expr = parseExpr(0);
            if (expr != null) {
                initializers.add(expr);
            } else {
                reportPeekError(
                        "Expected initializer expression",
                        "Provide an expression to initialize the variable (e.g., 42, \"text\","
                                + " variable_name)");
                break;
            }
            if (!matchToken(TokenKind.Simple.Operator.COMMA)) {
                break;
            }
        }

        if (variables.size() != initializers.size()) {
            syntaxError(
                    "Declaration mismatch: %d variables but %d initializers"
                            .formatted(variables.size(), initializers.size()),
                    startToken,
                    "Each variable must have exactly one initializer expression",
                    ErrorCode.E2001);
        }

        final List<Stmt.VarBinding> bindings = new ArrayList<>(variables.size());
        for (int i = 0; i < variables.size(); i++) {
            final Expr initializer = i < initializers.size() ? initializers.get(i) : null;
            bindings.add(new Stmt.VarBinding(variables.get(i), Optional.ofNullable(initializer)));
        }
        return new Stmt.VarDeclaration(bindings, typeAnnotation, isMutable, mergedSpan(startToken));
    }

    private void reportPeekError(final String message, final String help) {
        final Token token = peek();
        if (token != null) {
            syntaxError(message, token, help, ErrorCode.E1004);
        }
    }

    private String consumeIdentifier() {
        final Token token = peek();
        if (token == null) {
            return null;
        }

        return switch (token.type()) {
            case TokenKind.IdentifierAscii identifier -> {
                advance();
                yield identifier.value();
            }
            case TokenKind.IdentifierUnicode identifier -> {
                advance();
                yield identifier.value();
            }
            default -> {
                syntaxError(
                        "Expected identifier",
                        token,
                        "An identifier must start with a letter/underscore and contain only"
                                + " alphanumeric characters",
                        ErrorCode.E1008);
                yield null;
            }
        };
    }

    private Stmt parseExpressionStmt() {
        final Expr expr = parseExpr(0);
        return expr == null ? null : new Stmt.Expression(expr);
    }

    private Expr parseExpr(final int minBp) {
        if (checkRecursionLimit()) {
            return null;
        }

        enterRecursion();
        final Expr result = parseExprInner(minBp);
        exitRecursion();
        return result;
    }

    private Expr parseExprInner(final int minBp) {
        Expr left = nud();
        if (left == null) {
            return null;
        }

        while (true) {
            final Token token = peek();
            if (token == null) {
                break;
            }
            final int lbp = Precendence.bindingPower(token).left();
            if (lbp <= minBp) {
                break;
            }
            left = led(left);
            if (left == null) {
                return null;
            }
        }
        return left;
    }

    private Expr nud() {
        final Token next = peek();
        if (next == null || next.type() == TokenKind.Simple.Special.EOF) {
            if (next != null) {
                syntaxError(
                        "Expected an operand",
                        next,
                        "Provide an expression after the unary operator",
                        ErrorCode.E1006);
            }
            return null;
        }
        final Token token = advance();
        if (token == null) {
            return null;
        }

        return switch (token.type()) {
            case TokenKind.Numeric numeric ->
                    Expr.newNumberLiteral(numeric.value(), token.span()).orElseThrow();
            case TokenKind.Binary binary ->
                    Expr.newNumberLiteral(binary.value(), token.span()).orElseThrow();
            case TokenKind.Octal octal ->
                    Expr.newNumberLiteral(octal.value(), token.span()).orElseThrow();
            case TokenKind.Hexadecimal hexadecimal ->
                    Expr.newNumberLiteral(hexadecimal.value(), token.span()).orElseThrow();
            case TokenKind.KeywordBool bool ->
                    Expr.newBoolLiteral(bool.value(), token.span()).orElseThrow();
            case TokenKind.Simple.Keyword.NULLPTR ->
                    Expr.newNullptrLiteral(token.span()).orElseThrow();
            case TokenKind.StringLiteral stringLiteral ->
                    Expr.newStringLiteral(stringLiteral.value(), token.span()).orElseThrow();
            case TokenKind.CharLiteral charLiteral ->
                    Expr.newCharLiteral(charLiteral.value(), token.span()).orElseThrow();
            case TokenKind.Simple.Operator.MINUS -> parseUnary(UnaryOp.NEGATE, token);
            case TokenKind.Simple.Operator.NOT -> parseUnary(UnaryOp.NOT, token);
            case TokenKind.Simple.Operator.BITWISE_NOT -> parseUnary(UnaryOp.BITWISE_NOT, token);
            case TokenKind.Simple.Operator.PLUS_PLUS -> parseUnary(UnaryOp.INCREMENT, token);
            case TokenKind.Simple.Operator.MINUS_MINUS -> parseUnary(UnaryOp.DECREMENT, token);
            case TokenKind.Simple.Delimiter.OPEN_BRACE -> parseArrayLiteral(token);
            case TokenKind.Simple.Delimiter.OPEN_PAREN -> parseGrouping(token);
            case TokenKind.IdentifierAscii identifier ->
                    new Expr.Variable(identifier.value(), token.span());
            case TokenKind.IdentifierUnicode identifier ->
                    new Expr.Variable(identifier.value(), token.span());
            default -> {
                syntaxError(
                        "Unexpected token",
                        token,
                        "Expected an expression (number, string, variable, or operator)",
                        ErrorCode.E1004);
                yield null;
            }
        };
    }

    private Expr led(final Expr left) {
        final Token token = advance();
        if (token == null) {
            return null;
        }

        return switch (token.type()) {
            case TokenKind.Simple.Operator.PLUS,
                    TokenKind.Simple.Operator.MINUS,
                    TokenKind.Simple.Operator.STAR,
                    TokenKind.Simple.Operator.SLASH,
                    TokenKind.Simple.Operator.PERCENT,
                    TokenKind.Simple.Operator.EQUAL_EQUAL,
                    TokenKind.Simple.Operator.NOT_EQUAL,
                    TokenKind.Simple.Operator.LESS,
                    TokenKind.Simple.Operator.LESS_EQUAL,
                    TokenKind.Simple.Operator.GREATER,
                    TokenKind.Simple.Operator.GREATER_EQUAL,
                    TokenKind.Simple.Operator.AND_AND,
                    TokenKind.Simple.Operator.OR_OR,
                    TokenKind.Simple.Operator.AND,
                    TokenKind.Simple.Operator.OR,
                    TokenKind.Simple.Operator.XOR,
                    TokenKind.Simple.Operator.SHIFT_LEFT,
                    TokenKind.Simple.Operator.SHIFT_RIGHT,
                    TokenKind.Simple.Operator.PLUS_EQUAL,
                    TokenKind.Simple.Operator.MINUS_EQUAL,
                    TokenKind.Simple.Operator.AND_EQUAL,
                    TokenKind.Simple.Operator.OR_EQUAL,
                    TokenKind.Simple.Operator.PERCENT_EQUAL,
                    TokenKind.Simple.Operator.XOR_EQUAL,
                    TokenKind.Simple.Operator.STAR_EQUAL,
                    TokenKind.Simple.Operator.SLASH_EQUAL,
                    TokenKind.Simple.Operator.SHIFT_LEFT_EQUAL,
                    TokenKind.Simple.Operator.SHIFT_RIGHT_EQUAL ->
                    parseBinary(left, token);
            case TokenKind.Simple.Operator.EQUAL -> parseAssignment(left, token);
            case TokenKind.Simple.Delimiter.OPEN_PAREN -> parseCall(left, token);
            case TokenKind.Simple.Delimiter.OPEN_BRACKET -> parseArrayAccess(left, token);
            case TokenKind.Simple.Operator.PLUS_PLUS ->
                    new Expr.Unary(
                            UnaryOp.INCREMENT,
                            UnaryOpSide.POSTFIX,
                            left,
                            left.span().merge(token.span()));
            case TokenKind.Simple.Operator.MINUS_MINUS ->
                    new Expr.Unary(
                            UnaryOp.DECREMENT,
                            UnaryOpSide.POSTFIX,
                            left,
                            left.span().merge(token.span()));
            default -> {
                syntaxError(
                        "Unexpected operator",
                        token,
                        "This operator is not supported in this context",
                        ErrorCode.E1004);
                yield null;
            }
        };
    }

    private Expr parseUnary(final UnaryOp op, final Token token) {
        final int rbp = Precendence.unaryBindingPower(token).right();
        final Expr expr = parseExpr(rbp);
        if (expr == null) {
            return null;
        }
        return new Expr.Unary(op, UnaryOpSide.PREFIX, expr, token.span().merge(expr.span()));
    }

    private Expr parseArrayLiteral(final Token startToken) {
        final List<Expr> elements = new ArrayList<>();
        extractElements(TokenKind.Simple.Delimiter.CLOSE_BRACE, elements);
        if (!expect(TokenKind.Simple.Delimiter.CLOSE_BRACE, "end of array literal")) {
            return null;
        }
        return new Expr.ArrayLiteral(elements, mergedSpan(startToken));
    }

    private void extractElements(final TokenKind kind, final List<Expr> elements) {
        while (!check(kind) && !isAtEnd()) {
            final Expr expr = parseExpr(0);
            if (expr != null) {
                elements.add(expr);
            }
            if (!matchToken(TokenKind.Simple.Operator.COMMA)) {
                break;
            }
        }
    }

    private Expr parseBinary(final Expr left, final Token token) {
        final BinaryOp op;
        try {
            op = BinaryOp.getOp(token);
        } catch (CompilerException exception) {
            errors.add(
                    new CompileError.SyntaxError(
                            Optional.of(ErrorCode.E1004),
                            exception.getMessage() == null
                                    ? "Unsupported binary operator"
                                    : exception.getMessage(),
                            token.span(),
                            Optional.of("This operator is not supported in this context")));
            return null;
        }

        final Expr right = parseExpr(Precendence.bindingPower(token).right());
        final Expr safeRight = right == null ? Expr.nullExpr(token.span()) : right;
        return new Expr.Binary(left, op, safeRight, token.span());
    }

    private Expr parseGrouping(final Token startToken) {
        final Expr expr = parseExpr(0);
        if (!expect(TokenKind.Simple.Delimiter.CLOSE_PAREN, "end of grouping")) {
            return null;
        }
        if (expr == null) {
            return null;
        }
        return new Expr.Grouping(expr, mergedSpan(startToken));
    }

    private Expr parseAssignment(final Expr left, final Token token) {
        final Expr value = parseExpr(1);
        final Expr safeValue = value == null ? Expr.nullExpr(token.span()) : value;
        final Span span = left.span().merge(safeValue.span());

        final boolean valid = left instanceof Expr.Variable || left instanceof Expr.ArrayAccess;
        if (!valid) {
            errors.add(
                    new CompileError.SyntaxError(
                            Optional.of(ErrorCode.E1003),
                            "Invalid left-hand side in assignment",
                            left.span(),
                            Optional.of(
                                    "Only variables and array elements can be assigned to. Consider"
                                            + " using a variable name or an array access"
                                            + " expression.")));
            return null;
        }

        return new Expr.Assign(left, safeValue, span);
    }

    private Expr parseCall(final Expr callee, final Token startToken) {
        final List<Expr> arguments = new ArrayList<>();
        extractElements(TokenKind.Simple.Delimiter.CLOSE_PAREN, arguments);
        if (!expect(TokenKind.Simple.Delimiter.CLOSE_PAREN, "end of function call arguments")) {
            return null;
        }
        return new Expr.Call(callee, arguments, mergedSpan(startToken));
    }

    private Expr parseArrayAccess(final Expr array, final Token startToken) {
        final Expr index = parseExpr(0);
        final Expr safeIndex = index == null ? Expr.nullExpr(startToken.span()) : index;
        if (!expect(TokenKind.Simple.Delimiter.CLOSE_BRACKET, "end of array access")) {
            return null;
        }
        return new Expr.ArrayAccess(array, safeIndex, mergedSpan(startToken));
    }

    private Span mergedSpan(final Token startToken) {
        final Token end = previous();
        return end == null ? startToken.span() : startToken.span().merge(end.span());
    }

    private void syntaxError(
            final String message, final Token token, final String help, final ErrorCode errorCode) {
        final String messageText = message + ": " + token.type();
        errors.add(
                new CompileError.SyntaxError(
                        Optional.ofNullable(errorCode),
                        messageText,
                        token.span(),
                        Optional.ofNullable(help)));
    }

    private boolean expect(final TokenKind kind, final String context) {
        if (matchToken(kind)) {
            return true;
        }

        final Token currentToken = peek();
        final String foundStr =
                currentToken == null ? "end of input" : currentToken.type().toString();
        final Span span = resolveErrorSpan(currentToken);
        final String errorMessage =
                "Expected %s in %s, found %s.".formatted(kind, context, foundStr);
        final String helpMessage = "Try adding a " + kind;
        errors.add(
                new CompileError.SyntaxError(
                        Optional.of(ErrorCode.E1004),
                        errorMessage,
                        span,
                        Optional.of(helpMessage)));
        return false;
    }

    /**
     * Resolves a non-null {@link Span} to attach to a syntax error when the current token is
     * unavailable (e.g. the parser has run past the end of the token stream). Falls back to the
     * previously consumed token's span, and finally to the last token in the stream (which should
     * always be an EOF token supplied by the lexer), so we never pass {@code null} to {@link
     * CompileError.SyntaxError}, which requires a non-null {@link Span}.
     */
    private Span resolveErrorSpan(final Token currentToken) {
        if (currentToken != null) {
            return currentToken.span();
        }
        final Token last = previous();
        if (last != null) {
            return last.span();
        }
        if (!tokens.isEmpty()) {
            return tokens.get(tokens.size() - 1).span();
        }
        throw new IllegalStateException(
                "Cannot report syntax error: parser has no tokens to derive a span from");
    }

    private boolean matchToken(final TokenKind kind) {
        if (check(kind)) {
            advance();
            return true;
        }
        return false;
    }

    private Token advance() {
        if (isAtEnd()) {
            return null;
        }
        current += 1;
        return previous();
    }

    private Token previous() {
        final int index = Math.max(0, current - 1);
        return index < tokens.size() ? tokens.get(index) : null;
    }

    private Token peek() {
        return current < tokens.size() ? tokens.get(current) : null;
    }

    private boolean check(final TokenKind kind) {
        final Token token = peek();
        return token != null && token.type().equals(kind);
    }

    private boolean isAtEnd() {
        final Token token = peek();
        return token == null || token.type() == TokenKind.Simple.Special.EOF;
    }
}
