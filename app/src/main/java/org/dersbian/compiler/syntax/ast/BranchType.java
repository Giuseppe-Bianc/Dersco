package org.dersbian.compiler.syntax.ast;

/** Represents the visual branch connector types used when formatting tree structures. */
@SuppressWarnings("PMD.LongVariable")
public enum BranchType {

    /** The last branch in a group, rendering with a terminal corner. */
    LAST("└── ", "    "),

    /** A middle branch in a group, rendering with a tee connector. */
    MIDDLE("├── ", "│   ");

    /** The string symbol for the branch connector. */
    private final String symboli;

    /** The indentation continuation string for child nodes. */
    private final String indentContinuationi;

    /**
     * Constructs a {@code BranchType} with the given symbol and continuation string.
     *
     * @param symbol the string symbol for the branch connector
     * @param indentContinuation the indentation continuation string for child nodes
     */
    BranchType(final String symbol, final String indentContinuation) {
        this.symboli = symbol;
        this.indentContinuationi = indentContinuation;
    }

    /**
     * Returns the branch connector symbol.
     *
     * @return the string representation of the branch connector
     */
    public String symbol() {
        return symboli;
    }

    /**
     * Returns the indentation continuation string for child nodes under this branch.
     *
     * @return the indentation continuation string
     */
    public String indentContinuation() {
        return indentContinuationi;
    }
}
