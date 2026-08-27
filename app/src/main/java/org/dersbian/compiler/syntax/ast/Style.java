package org.dersbian.compiler.syntax.ast;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Immutable utility for constructing and applying ANSI escape styling to console text. */
@SuppressWarnings({"PMD.TooManyMethods", "PMD.OnlyOneReturn", "PMD.ShortVariable"})
public final class Style {

    /** The ANSI escape sequence prefix. */
    private static final String ESC = "\u001B[";

    /** The ANSI sequence used to reset text formatting. */
    private static final String RESET = ESC + "0m";

    /** The accumulated ANSI styling code parameters. */
    private final List<String> codes;

    private Style(final List<String> codes) {
        this.codes = List.copyOf(codes);
    }

    /**
     * Creates a new, unstyled {@code Style} instance.
     *
     * @return a new empty {@code Style}
     */
    public static Style newStyle() {
        return new Style(List.of());
    }

    private Style with(final String code) {
        final List<String> next = new ArrayList<>(codes);
        next.add(code);
        return new Style(next);
    }

    /**
     * Applies the blue ANSI foreground color to this style.
     *
     * @return a new {@code Style} instance with blue color applied
     */
    public Style blue() {
        return with("34");
    }

    /**
     * Applies the green ANSI foreground color to this style.
     *
     * @return a new {@code Style} instance with green color applied
     */
    public Style green() {
        return with("32");
    }

    /**
     * Applies the yellow ANSI foreground color to this style.
     *
     * @return a new {@code Style} instance with yellow color applied
     */
    public Style yellow() {
        return with("33");
    }

    /**
     * Applies the cyan ANSI foreground color to this style.
     *
     * @return a new {@code Style} instance with cyan color applied
     */
    public Style cyan() {
        return with("36");
    }

    /**
     * Applies the magenta ANSI foreground color to this style.
     *
     * @return a new {@code Style} instance with magenta color applied
     */
    public Style magenta() {
        return with("35");
    }

    /**
     * Applies the dim (faint) ANSI text attribute to this style.
     *
     * @return a new {@code Style} instance with dim styling applied
     */
    public Style dim() {
        return with("2");
    }

    /**
     * Applies the italic ANSI text attribute to this style.
     *
     * @return a new {@code Style} instance with italic styling applied
     */
    public Style italic() {
        return with("3");
    }

    /**
     * Wraps text with the accumulated ANSI codes and resets styling at the end.
     *
     * @param text the plain text string to style
     * @return the styled text string with ANSI escape codes, or the original text if no styles are
     *     configured
     */
    public String applyTo(final String text) {
        Objects.requireNonNull(text, "text must not be null");
        if (codes.isEmpty()) {
            return text;
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(ESC);
        for (int i = 0; i < codes.size(); i++) {
            if (i > 0) {
                sb.append(';');
            }
            sb.append(codes.get(i));
        }
        sb.append('m').append(text).append(RESET);
        return sb.toString();
    }

    @Override
    public boolean equals(final Object other) {
        return other instanceof Style s && codes.equals(s.codes);
    }

    @Override
    public int hashCode() {
        return codes.hashCode();
    }
}
