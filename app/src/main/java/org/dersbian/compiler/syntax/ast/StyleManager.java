package org.dersbian.compiler.syntax.ast;

/**
 * Console styling configuration for different element types. The immutable fields mirror the Rust
 * {@code pub}-field struct; getters expose the styles. Instances are thread-safe.
 */
public final class StyleManager {

    /** The style used for operators. */
    private final Style operatori;

    /** The style used for literal values. */
    private final Style literali;

    /** The style used for variables and identifiers. */
    private final Style variablei;

    /** The style used for structural tree elements. */
    private final Style structurei;

    /** The style used for punctuation symbols. */
    private final Style punctuationi;

    /** The style used for language keywords. */
    private final Style keywordi;

    /** The style used for type names. */
    private final Style typeStylei;

    /** The style used for metadata and auxiliary information. */
    private final Style metadatai;

    /**
     * Constructs a new {@code StyleManager} initialized with default syntax highlighting styles.
     */
    public StyleManager() {
        this.operatori = Style.newStyle().blue();
        this.literali = Style.newStyle().green();
        this.variablei = Style.newStyle().yellow();
        this.structurei = Style.newStyle().cyan();
        this.punctuationi = Style.newStyle().magenta();
        this.keywordi = Style.newStyle().blue();
        this.typeStylei = Style.newStyle().green();
        this.metadatai = Style.newStyle().dim().italic();
    }

    /**
     * Returns the style used for operators.
     *
     * @return the operator {@link Style}
     */
    public Style operator() {
        return operatori;
    }

    /**
     * Returns the style used for literal values.
     *
     * @return the literal {@link Style}
     */
    public Style literal() {
        return literali;
    }

    /**
     * Returns the style used for variables and identifiers.
     *
     * @return the variable {@link Style}
     */
    public Style variable() {
        return variablei;
    }

    /**
     * Returns the style used for structural tree elements.
     *
     * @return the structure {@link Style}
     */
    public Style structure() {
        return structurei;
    }

    /**
     * Returns the style used for punctuation symbols.
     *
     * @return the punctuation {@link Style}
     */
    public Style punctuation() {
        return punctuationi;
    }

    /**
     * Returns the style used for language keywords.
     *
     * @return the keyword {@link Style}
     */
    public Style keyword() {
        return keywordi;
    }

    /**
     * Returns the style used for type names.
     *
     * @return the type {@link Style}
     */
    public Style typeStyle() {
        return typeStylei;
    }

    /**
     * Returns the style used for metadata and auxiliary information.
     *
     * @return the metadata {@link Style}
     */
    public Style metadata() {
        return metadatai;
    }
}
