package org.dersbian.compiler.syntax;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.lexer.token.Token;

/**
 * Top-level parser. Combines {@link TokenCursor}, {@link ExpressionParser}, and {@link
 * StatementParser} to turn a token stream into a {@link ParseResult}.
 *
 * <p>Parsing never throws for recoverable syntax errors: every error is recorded in the {@link
 * ParseResult} and the cursor is synchronized so that subsequent top-level declarations can still
 * be parsed in a single pass (FR-008).
 */
@SuppressWarnings({
    "PMD.CommentDefaultAccessModifier",
    "PMD.ShortVariable",
    "PMD.AvoidDuplicateLiterals"
})
public final class Parser {

    private final TokenCursor cursor;
    private final List<CompileError.SyntaxError> errors = new ArrayList<>();
    private final ExpressionParser exprParser;
    private final StatementParser stmtParser;

    /**
     * Creates a parser over the given token list and source path.
     *
     * @param tokens tokens from the lexer (not {@code null})
     * @param source source file path used for error reporting (not {@code null})
     * @throws NullPointerException if either parameter is {@code null}
     */
    public Parser(final List<Token> tokens, final Path source) {
        Objects.requireNonNull(tokens, "tokens must not be null");
        Objects.requireNonNull(source, "source must not be null");
        this.cursor = new TokenCursor(tokens);
        this.exprParser = new ExpressionParser(cursor, errors);
        this.stmtParser = new StatementParser(cursor, exprParser, errors);
    }

    /**
     * Parses the entire token stream and returns a {@link ParseResult} carrying the top-level
     * statements and all syntax errors collected.
     *
     * <p>This method does not throw for recoverable syntax errors; the cursor is synchronized after
     * each error so that following declarations are still visited.
     *
     * @return parse result containing statements and errors
     */
    public ParseResult parse() {
        final List<org.dersbian.compiler.syntax.ast.Stmt> statements = new ArrayList<>();
        while (!cursor.isAtEnd()) {
            try {
                statements.add(stmtParser.parseStatement());
            } catch (final RuntimeException ex) {
                errors.add(
                        CompileError.syntaxError(
                                null,
                                "Internal parse error: " + ex.getMessage(),
                                cursor.currentSpan(),
                                null));
                cursor.synchronize(errors);
            }
        }
        return new ParseResult(List.copyOf(statements), List.copyOf(errors));
    }
}
