package org.dersbian.compiler.syntax.ast;

import java.util.List;
import java.util.Objects;

/**
 * Helper functions for tree-like output: {@code printChildren}, {@code getIndent}, {@code
 * appendLine}. Mirrors the free functions in the Rust {@code branch_type} module.
 */
public final class BranchPrinter {

    private BranchPrinter() {
        // static utility
    }

    /**
     * Functional interface corresponding to the Rust closure {@code FnMut(&T, &str, BranchType,
     * &mut String, &StyleManager)}.
     *
     * @param <T> the type of element being printed
     */
    @FunctionalInterface
    public interface ChildPrinter<T> {
        /**
         * Formats and prints a single child node to the specified output builder.
         *
         * @param child the child element to print
         * @param indent the current indentation string
         * @param branchType the branch type indicating the position of the child
         * @param output the output buffer to append the formatted child to
         * @param styles the style manager for formatting
         */
        void print(
                T child,
                String indent,
                BranchType branchType,
                StringBuilder output,
                StyleManager styles);
    }

    /**
     * Prints a list of children with correct branch symbols and indentation. All but the last child
     * receive {@link BranchType#MIDDLE}; the last child receives {@link BranchType#LAST}. Returns
     * immediately without invoking {@code printFn} if {@code children} is empty.
     *
     * @param <T> the element type of the children
     * @param children the list of children to print
     * @param indent the current indentation string
     * @param output the output buffer to append to
     * @param styles the style manager for formatting
     * @param printFn the function used to print each child
     */
    public static <T> void printChildren(
            final List<T> children,
            final String indent,
            final StringBuilder output,
            final StyleManager styles,
            final ChildPrinter<T> printFn) {
        Objects.requireNonNull(children, "children must not be null");
        if (children.isEmpty()) {
            return;
        }
        final int lastIdx = children.size() - 1;
        for (int i = 0; i < lastIdx; i++) {
            printFn.print(children.get(i), indent, BranchType.MIDDLE, output, styles);
        }
        printFn.print(children.get(lastIdx), indent, BranchType.LAST, output, styles);
    }

    /**
     * Concatenates {@code indent} with the branch's continuation indent.
     *
     * @param indent the current indentation string
     * @param branchType the branch type to get continuation indentation for
     * @return the combined indentation string
     */
    public static String getIndent(final String indent, final BranchType branchType) {
        final String continuation = branchType.indentContinuation();
        final StringBuilder result = new StringBuilder(indent.length() + continuation.length());
        result.append(indent).append(continuation);
        return result.toString();
    }

    /**
     * Appends a formatted, styled line to {@code output}.
     *
     * @param output the output buffer to append to
     * @param indent the current indentation string
     * @param branchType the branch type for the line prefix
     * @param style the style to apply to {@code text}
     * @param text the text content to append
     */
    public static void appendLine(
            final StringBuilder output,
            final String indent,
            final BranchType branchType,
            final Style style,
            final String text) {
        final String branch = branchType.symbol();
        final String styledText = style.applyTo(text);
        output.append(indent).append(branch).append(styledText).append('\n');
    }
}
