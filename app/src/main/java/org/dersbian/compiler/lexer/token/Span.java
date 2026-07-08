package org.dersbian.compiler.lexer.token;

import java.util.Objects;

/**
 * Estensione di un token.
 *
 * @param start posizione iniziale.
 * @param end posizione finale.
 */
public record Span(SourceLocation start, SourceLocation end) {

    /** Costruttore compatto per la validazione. */
    public Span {
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");
        if (end.offset() < start.offset()) {
            throw new IllegalArgumentException(
                    "end offset (%d) non può precedere start offset (%d)"
                            .formatted(end.offset(), start.offset()));
        }
    }

    /**
     * Crea un nuovo Span.
     *
     * @param start posizione iniziale
     * @param end posizione finale
     * @return il nuovo span
     */
    public static Span create(final SourceLocation start, final SourceLocation end) {
        return new Span(start, end);
    }

    /** Span puntuale. */
    public static Span point(final SourceLocation location) {
        Objects.requireNonNull(location, "location must not be null");
        return new Span(location, location);
    }

    /** Lunghezza in unità UTF-16 (coerente con {@link String#length()}). */
    public long length() {
        return end.offset() - start.offset();
    }

    /**
     * Verifica se lo span ha lunghezza zero.
     *
     * @return true se vuoto
     */
    public boolean isEmpty() {
        return length() == 0L;
    }

    /**
     * Verifica se lo span si estende su più righe.
     *
     * @return true se multilinea
     */
    public boolean isMultiline() {
        return start.line() != end.line();
    }

    /** Verifica se la posizione ricade nell'intervallo [start, end). */
    public boolean contains(final SourceLocation location) {
        Objects.requireNonNull(location, "location must not be null");
        return location.offset() >= start.offset() && location.offset() < end.offset();
    }

    /** Verifica se due span condividono almeno un carattere. */
    public boolean overlaps(final Span other) {
        Objects.requireNonNull(other, "other must not be null");
        return this.start.offset() < other.end.offset() && other.start.offset() < this.end.offset();
    }

    /** Restituisce lo span minimo che contiene entrambi gli span. */
    public Span merge(final Span other) {
        Objects.requireNonNull(other, "other must not be null");
        final SourceLocation mergedStart =
                this.start.offset() <= other.start.offset() ? this.start : other.start;
        final SourceLocation mergedEnd =
                this.end.offset() >= other.end.offset() ? this.end : other.end;
        return new Span(mergedStart, mergedEnd);
    }

    /** Estrae il testo dallo span. */
    public String extractFrom(final CharSequence source) {
        Objects.requireNonNull(source, "source must not be null");
        return source.subSequence(Math.toIntExact(start.offset()), Math.toIntExact(end.offset()))
                .toString();
    }

    @Override
    public String toString() {
        return "%s-%s".formatted(start, end);
    }
}
