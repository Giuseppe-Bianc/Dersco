package org.dersbian.compiler.lexer.token;

import java.util.Comparator;

/**
 * Unità lessicale fondamentale prodotta dall'analisi lessicale.
 *
 * @param sourceId identificativo stabile della sorgente.
 * @param type tipo lessicale puntuale del token.
 * @param span estensione nella sorgente.
 */
public record Token(SourceId sourceId, TokenKind type, Span span) {

    /** Comparatore per posizione. Non coerente con equals. */
    public static final Comparator<Token> BY_POSITION =
            Comparator.comparing(token -> token.span().start());

    /** Crea un token a partire da uno span già costruito. */
    public static Token create(final SourceId sourceId, final TokenKind type, final Span span) {
        return new Token(sourceId, type, span);
    }

    /** Crea un token a partire da posizione iniziale e finale esplicite. */
    public static Token create(
            final SourceId sourceId,
            final TokenKind type,
            final SourceLocation start,
            final SourceLocation end) {
        return new Token(sourceId, type, Span.create(start, end));
    }

    /** Crea un token sintetico di fine sorgente (EOF), di lunghezza zero. */
    public static Token eof(final SourceId sourceId, final SourceLocation location) {
        return new Token(sourceId, TokenKind.Simple.EOF, Span.point(location));
    }

    /** Verifica se il token è del tipo specificato. */
    public boolean isType(final TokenKind candidate) {
        return type.equals(candidate);
    }

    /** Indica se il token proviene da una sorgente generata automaticamente. */
    public boolean isSynthetic() {
        return sourceId instanceof SourceId.Generated;
    }

    @Override
    public String toString() {
        return "Token[type=%s, span=%s, source=%s]".formatted(type, span, sourceId.identifier());
    }
}
