package org.dersbian.compiler.lexer.token;

import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.TooManyMethods",
    "PMD.UnitTestContainsTooManyAsserts",
})
class SourceIdTest {

    @Test
    void describeShouldReturnCorrectFormatForFilePath() {
        final Path path = Path.of("src", "main.ds");
        final SourceId sourceId = new SourceId.FilePath(path);

        Assertions.assertEquals(
                "file: " + path.toString(),
                sourceId.describe(),
                "Il formato della descrizione per FilePath non è corretto");
    }

    @Test
    void describeShouldReturnCorrectFormatForVirtualResource() {
        final SourceId sourceId = new SourceId.VirtualResource("http://localhost/script.ds");

        Assertions.assertEquals(
                "virtual: http://localhost/script.ds",
                sourceId.describe(),
                "Il formato della descrizione per VirtualResource non è corretto");
    }

    @Test
    void describeShouldReturnCorrectFormatForInMemoryModule() {
        final SourceId sourceId = new SourceId.InMemoryModule("ReplSession_1");

        Assertions.assertEquals(
                "in-memory module: ReplSession_1",
                sourceId.describe(),
                "Il formato della descrizione per InMemoryModule non è corretto");
    }

    @Test
    void describeShouldReturnCorrectFormatForGenerated() {
        final SourceId sourceId = new SourceId.Generated("macro_expansion");

        Assertions.assertEquals(
                "generated: macro_expansion",
                sourceId.describe(),
                "Il formato della descrizione per Generated non è corretto");
    }

    @Test
    void virtualResourceShouldReturnUriAsIdentifier() {
        final String expectedUri = "jar:file:/app.jar!/module.ds";
        final SourceId sourceId = new SourceId.VirtualResource(expectedUri);

        Assertions.assertEquals(
                expectedUri,
                sourceId.identifier(),
                "L'identifier deve restituire esattamente l'URI fornito");
    }

    @ParameterizedTest(name = "VirtualResource con stringa non vuota ma con spazi: ''{0}''")
    @ValueSource(strings = {" uri ", "a", "  a  ", "\turi\n", "\u00A0"})
    void virtualResourceShouldAcceptValidUrisIncludingCornerCases(final String cornerCaseUri) {
        // Corner case: stringhe che contengono spazi, o caratteri speciali come \u00A0
        final SourceId sourceId = new SourceId.VirtualResource(cornerCaseUri);

        Assertions.assertEquals(
                cornerCaseUri,
                sourceId.identifier(),
                "Non deve alterare l'URI, anche se contiene spazi bianchi all'inizio o alla fine o"
                        + " caratteri non-breaking");
    }

    @ParameterizedTest(
            name = "VirtualResource deve lanciare IllegalArgumentException se blank: ''{0}''")
    @ValueSource(strings = {"", " ", "   ", "\t", "\n", "\r\n"})
    void virtualResourceShouldThrowExceptionOnBlankUri(final String blankUri) {
        // Edge case: testiamo stringhe vuote o contenenti solo caratteri di spaziatura
        // standard
        final IllegalArgumentException exception =
                Assertions.assertThrows(
                        IllegalArgumentException.class,
                        () -> new SourceId.VirtualResource(blankUri),
                        "Deve lanciare IllegalArgumentException per URI blank");

        Assertions.assertEquals(
                "uri must not be blank",
                exception.getMessage(),
                "Il messaggio di errore per stringhe blank non è corretto");
    }

    @ParameterizedTest(name = "VirtualResource deve lanciare NullPointerException se null")
    @NullSource
    void virtualResourceShouldThrowExceptionOnNullUri(final String nullUri) {
        // Edge case: testiamo il caso in cui viene passato un valore null
        final NullPointerException exception =
                Assertions.assertThrows(
                        NullPointerException.class,
                        () -> new SourceId.VirtualResource(nullUri),
                        "Deve lanciare NullPointerException per URI null");

        Assertions.assertEquals(
                "uri must not be null",
                exception.getMessage(),
                "Il messaggio di errore per URI null non è corretto");
    }

    @Test
    void filePathUsesPathAsIdentifier() {
        final Path path = Path.of("src", "main.ds");
        final SourceId sourceId = new SourceId.FilePath(path);

        Assertions.assertEquals(
                path.toString(),
                sourceId.identifier(),
                "FilePath deve usare il toString() del Path come identifier");
    }

    @Test
    void generatedSourceWrapsIdentifier() {
        final SourceId sourceId = new SourceId.Generated("macro expansion");

        Assertions.assertEquals(
                "<generated:macro expansion>",
                sourceId.identifier(),
                "L'identifier di Generated deve avere un prefisso e suffisso specifici");
    }

    @Test
    void otherTextualSourceIdsRejectBlankValues() {
        // Questo test raggruppa le validazioni base degli altri record (per
        // completezza)
        Assertions.assertAll(
                "Verifica blank su altri costrutti",
                () ->
                        Assertions.assertThrows(
                                IllegalArgumentException.class,
                                () -> new SourceId.InMemoryModule(""),
                                "InMemoryModule non deve accettare blank"),
                () ->
                        Assertions.assertThrows(
                                IllegalArgumentException.class,
                                () -> new SourceId.Generated("\t"),
                                "Generated non deve accettare blank"));
    }
}
