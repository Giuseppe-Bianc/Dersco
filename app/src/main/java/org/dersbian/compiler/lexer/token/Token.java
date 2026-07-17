package org.dersbian.compiler.lexer.token;

import java.util.Comparator;
import org.dersbian.util.PathUtils;

/**
 * Fundamental lexical unit produced by the lexer.
 *
 * @param sourceId stable identifier of the source.
 * @param type point-like lexical kind of the token.
 * @param span extent in the source.
 */
public record Token(SourceId sourceId, TokenKind type, Span span) {

    /** Comparator by position. Not consistent with equals. */
    public static final Comparator<Token> BY_POSITION =
            Comparator.comparing(token -> token.span().start());

    /** Creates a token from an already built span. */
    public static Token create(final SourceId sourceId, final TokenKind type, final Span span) {
        return new Token(sourceId, type, span);
    }

    /** Creates a token from explicit start and end positions. */
    public static Token create(
            final SourceId sourceId,
            final TokenKind type,
            final SourceLocation start,
            final SourceLocation end) {
        return new Token(sourceId, type, Span.create(start, end));
    }

    /** Creates a synthetic end-of-source (EOF) token, with zero length. */
    public static Token eof(final SourceId sourceId, final SourceLocation location) {
        return new Token(sourceId, TokenKind.Simple.Special.EOF, Span.point(location));
    }

    /** Checks whether the token is of the given kind. */
    public boolean isKind(final TokenKind candidate) {
        return type.equals(candidate);
    }

    /** Whether the token comes from an automatically generated source. */
    public boolean isSynthetic() {
        return sourceId instanceof SourceId.Generated;
    }

    @Override
    public String toString() {
        final String result;
        if (sourceId instanceof SourceId.FilePath(var path)) {
            result = PathUtils.truncatePath(path, 2);
        } else {
            result = sourceId.identifier();
        }
        return "%s %s:%s".formatted(type, result, span);
    }
}
