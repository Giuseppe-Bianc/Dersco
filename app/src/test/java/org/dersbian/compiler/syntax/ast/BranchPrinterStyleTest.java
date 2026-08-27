package org.dersbian.compiler.syntax.ast;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.UnitTestAssertionsShouldIncludeMessage",
    "PMD.AvoidDuplicateLiterals"
})
class BranchPrinterStyleTest {

    @Test
    void branchTypesExposeSymbolsAndContinuationIndentation() {
        assertEquals("└── ", BranchType.LAST.symbol());
        assertEquals("    ", BranchType.LAST.indentContinuation());
        assertEquals("├── ", BranchType.MIDDLE.symbol());
        assertEquals("│   ", BranchType.MIDDLE.indentContinuation());
    }

    @Test
    void branchPrinterPrintsEmptyAndMultipleChildren() {
        final StringBuilder output = new StringBuilder();
        final int[] calls = {0};
        BranchPrinter.printChildren(
                List.of(),
                "",
                output,
                new StyleManager(),
                (child, indent, branch, text, styles) -> calls[0]++);
        assertEquals(0, calls[0]);

        BranchPrinter.printChildren(
                List.of("first", "last"),
                "  ",
                output,
                new StyleManager(),
                (child, indent, branch, text, styles) ->
                        BranchPrinter.appendLine(text, indent, branch, Style.newStyle(), child));
        assertEquals("  ├── first\n  └── last\n", output.toString());
        assertEquals("prefix    ", BranchPrinter.getIndent("prefix", BranchType.LAST));
    }

    @Test
    void styleIsImmutableAndAppliesAnsiCodes() {
        final Style plain = Style.newStyle();
        final Style blue = plain.blue();
        final Style combined = blue.italic();

        assertEquals("text", plain.applyTo("text"));
        assertEquals("\u001B[34mtext\u001B[0m", blue.applyTo("text"));
        assertEquals("\u001B[34;3mtext\u001B[0m", combined.applyTo("text"));
        assertEquals(blue, Style.newStyle().blue());
        assertThrows(NullPointerException.class, () -> plain.applyTo(null));
    }

    @Test
    void branchConfigRejectsNullTypes() {
        assertThrows(
                NullPointerException.class,
                () -> new BranchConfig(null, BranchType.MIDDLE, BranchType.LAST));
        assertThrows(
                NullPointerException.class,
                () -> new BranchConfig(BranchType.LAST, null, BranchType.LAST));
        assertThrows(
                NullPointerException.class,
                () -> new BranchConfig(BranchType.LAST, BranchType.MIDDLE, null));
    }
}
