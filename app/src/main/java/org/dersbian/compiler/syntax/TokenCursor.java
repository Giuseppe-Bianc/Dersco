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
 * <p>Comments are filtered at construction; the cursor always ends in an EOF token so {@link
 * #peek()} never throws.
 */
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
                            ? new SourceId.Generated("")
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

    Token peek() {
        return tokens.get(pos);
    }

    Token advance() {
        final Token t = tokens.get(pos);
        if (pos < tokens.size() - 1) {
            pos++;
        }
        return t;
    }

    boolean check(final TokenKind expected) {
        return peek().type().equals(expected);
    }

    boolean checkAny(final TokenKind... expected) {
        final TokenKind actual = peek().type();
        for (final TokenKind k : expected) {
            if (actual.equals(k)) {
                return true;
            }
        }
        return false;
    }

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

    boolean isAtEnd() {
        return peek().type().equals(TokenKind.Simple.Special.EOF);
    }

    Span currentSpan() {
        return peek().span();
    }

    private static boolean isSyncKeyword(final TokenKind kind) {
        return kind instanceof TokenKind.Simple.Keyword kw && SYNC_KEYWORDS.contains(kw);
    }
}
