package org.dersbian.compiler.lexer.token;

import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestAssertionsShouldIncludeMessage"
})
class SourceIdTest {

    @Test
    void filePathUsesPathAsIdentifierAndDescription() {
        final SourceId sourceId = new SourceId.FilePath(Path.of("src", "main.ds"));

        Assertions.assertAll(
                () ->
                        Assertions.assertEquals(
                                Path.of("src", "main.ds").toString(), sourceId.identifier()),
                () ->
                        Assertions.assertEquals(
                                "file: " + Path.of("src", "main.ds"), sourceId.describe()));
    }

    @Test
    void generatedSourceWrapsIdentifierAndDescription() {
        final SourceId sourceId = new SourceId.Generated("macro expansion");

        Assertions.assertAll(
                () -> Assertions.assertEquals("<generated:macro expansion>", sourceId.identifier()),
                () -> Assertions.assertEquals("generated: macro expansion", sourceId.describe()));
    }

    @Test
    void textualSourceIdsRejectBlankValues() {
        Assertions.assertAll(
                () ->
                        Assertions.assertThrows(
                                IllegalArgumentException.class,
                                () -> new SourceId.VirtualResource(" ")),
                () ->
                        Assertions.assertThrows(
                                IllegalArgumentException.class,
                                () -> new SourceId.InMemoryModule("")),
                () ->
                        Assertions.assertThrows(
                                IllegalArgumentException.class,
                                () -> new SourceId.Generated("\t")));
    }
}
