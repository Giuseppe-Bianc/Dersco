package org.dersbian.compiler.syntax;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.dersbian.compiler.error.CompileError;
import org.dersbian.compiler.error.ErrorCode;
import org.dersbian.compiler.lexer.token.SourceId;
import org.dersbian.compiler.lexer.token.Span;
import org.dersbian.compiler.lexer.token.Token;
import org.dersbian.compiler.lexer.token.TokenKind;

/**
 * Single forward-only cursor over a filtered token stream.
 *
 * <p>Comments are filtered at construction and an EOF token is appended when the supplied stream
 * does not already end with one. The cursor remains positioned at EOF after the end of the stream,
 * so {@link #peek()} and {@link #advance()} continue to return the EOF token instead of throwing.
 */
@SuppressWarnings({
    "PMD.CommentDefaultAccessModifier",
    "PMD.CommentRequired",
    "PMD.OnlyOneReturn",
    "PMD.ShortVariable",
    "PMD.EnumComparison"
})
final class TokenCursor {

    private static final List<TokenKind.Simple.Keyword> SYNC_KEYWORDS =
            List.of(
                    TokenKind.Simple.Keyword.FUN,
                    TokenKind.Simple.Keyword.VAR,
                    TokenKind.Simple.Keyword.CONST,
                    TokenKind.Simple.Keyword.IF,
                    TokenKind.Simple.Keyword.WHILE,
                    TokenKind.Simple.Keyword.FOR,
                    TokenKind.Simple.Keyword.RETURN,
                    TokenKind.Simple.Keyword.BREAK,
                    TokenKind.Simple.Keyword.CONTINUE,
                    TokenKind.Simple.Keyword.MAIN);

    private final List<Token> tokens;
    private int pos;

    /**
     * Creates a cursor from the supplied token stream.
     *
     * <p>Comment and multiline-comment tokens are removed. If the resulting stream is empty, an
     * EOF token is created with a generated source id and a point span at line 1, column 1. If the
     * stream does not end in EOF, an EOF token is appended at the end position of the last token.
     *
     * @param tokens tokens to traverse
     * @throws NullPointerException if {@code tokens} is {@code null}
     */
    TokenCursor(final List<Token> tokens) {
        Objects.requireNonNull(tokens, "tokens must not be null");
        final List<Token> filtered =
                tokens.stream()
                        .filter(
                                t ->
                                        t.type() != TokenKind.Simple.Special.COMMENT
                                                && t.type()
                                                        != TokenKind.Simple.Special
                                                                .MULTILINE_COMMENT)
                        .toList();
        final List<Token> withEof = new ArrayList<>(filtered);
        if (withEof.isEmpty()
                || withEof.get(withEof.size() - 1).type() != TokenKind.Simple.Special.EOF) {
            final SourceId sid =
                    withEof.isEmpty()
                            ? new SourceId.Generated("<empty>")
                            : withEof.get(withEof.size() - 1).sourceId();
            final Span endSpan =
                    withEof.isEmpty()
                            ? Span.point(
                                    org.dersbian.compiler.lexer.token.SourceLocation.create(
                                            1, 1, 0L))
                            : Span.point(withEof.get(withEof.size() - 1).span().end());
            withEof.add(Token.eof(sid, endSpan.end()));
        }
        this.tokens = List.copyOf(withEof);
    }

    /**
     * Returns the token at the current cursor position without advancing it.
     *
     * @return current token, including EOF when the cursor is at the end
     */
    Token peek() {
        return tokens.get(pos);
    }

    /**
     * Returns the current token and advances the cursor when it has not reached EOF.
     *
     * @return token at the current cursor position
     */
    Token advance() {
        final Token t = tokens.get(pos);
        if (pos < tokens.size() - 1) {
            pos++;
        }
        return t;
    }

    /**
     * Checks whether the current token has the expected kind.
     *
     * @param expected expected token kind
     * @return {@code true} when the current token matches {@code expected}
     */
    boolean check(final TokenKind expected) {
        return peek().type().equals(expected);
    }

    /**
     * Checks whether the current token matches at least one of the supplied kinds.
     *
     * @param expected token kinds to compare with the current token
     * @return {@code true} when the current token matches one of {@code expected}
     */
    boolean checkAny(final TokenKind... expected) {
        final TokenKind actual = peek().type();
        for (final TokenKind k : expected) {
            if (actual.equals(k)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Consumes the current token when it has the expected kind, otherwise records a syntax error
     * and leaves the cursor unchanged.
     *
     * @param expected expected token kind
     * @param errors mutable collection receiving a syntax error on mismatch
     * @return consumed token on match, otherwise the current token
     */
    Token expect(final TokenKind expected, final List<CompileError.SyntaxError> errors) {
        if (check(expected)) {
            return advance();
        }
        errors.add(
                CompileError.syntaxError(
                        ErrorCode.E1006,
                        "Expected " + expected + " but found " + peek().type(),
                        peek().span(),
                        null));
        return peek();
    }

    /**
     * Advances until a closing brace, a synchronization keyword, or EOF is reached.
     *
     * <p>A closing brace is consumed when it is the synchronization point. Synchronization
     * keywords remain current so the parser can handle the corresponding statement normally.
     *
     * @param errors mutable collection reserved for parser errors; not modified by this method
     */
    void synchronize(final List<CompileError.SyntaxError> errors) {
        while (!isAtEnd()
                && !check(TokenKind.Simple.Delimiter.CLOSE_BRACE)
                && !isSyncKeyword(peek().type())) {
            advance();
        }
        if (check(TokenKind.Simple.Delimiter.CLOSE_BRACE)) {
            advance();
        }
    }

    /**
     * Returns whether the current token is EOF.
     *
     * @return {@code true} when the cursor is positioned at EOF
     */
    boolean isAtEnd() {
        return peek().type().equals(TokenKind.Simple.Special.EOF);
    }

    /**
     * Returns the source span of the current token.
     *
     * @return current token span
     */
    Span currentSpan() {
        return peek().span();
    }

    private static boolean isSyncKeyword(final TokenKind kind) {
        return kind instanceof TokenKind.Simple.Keyword kw && SYNC_KEYWORDS.contains(kw);
    }
}
