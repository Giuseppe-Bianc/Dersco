package org.dersbian.compiler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.CloseResource"
})
class DefaultCompilerServiceTest {

    @Test
    void checkSyntaxAcceptsEmptyUtf8File(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.createFile(tempDir.resolve("empty.dr"));
        final DefaultCompilerService service = new DefaultCompilerService();

        assertThatCode(() -> service.checkSyntax(source)).doesNotThrowAnyException();
    }

    @Test
    void checkSyntaxWrapsMissingFileAsCompilerException(@TempDir final Path tempDir) {
        final Path missingSource = tempDir.resolve("missing.dr");
        final DefaultCompilerService service = new DefaultCompilerService();

        assertThatThrownBy(() -> service.checkSyntax(missingSource))
                .isInstanceOf(CompilerException.class)
                .hasMessageContaining("Failed to read source file")
                .hasMessageContaining(missingSource.toString());
    }

    @Test
    void checkSyntaxWrapsMalformedUtf8AsCompilerException(@TempDir final Path tempDir)
            throws IOException {
        final Path source = tempDir.resolve("invalid-utf8.dr");
        Files.write(source, new byte[] {(byte) 0xC3, (byte) 0x28});
        final DefaultCompilerService service = new DefaultCompilerService();

        assertThatThrownBy(() -> service.checkSyntax(source))
                .isInstanceOf(CompilerException.class)
                .hasCauseInstanceOf(IOException.class)
                .hasMessageContaining("Failed to read source file")
                .hasMessageContaining(source.toString());
    }

    @Test
    void checkSyntaxFailsOnInvalidSource(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.writeString(tempDir.resolve("invalid-source.dr"), "@");
        final DefaultCompilerService service = new DefaultCompilerService();

        assertThatThrownBy(() -> service.checkSyntax(source))
                .isInstanceOf(CompilerException.class)
                .hasMessageContaining("Compilation failed with 1 error(s)");
    }

    @Test
    void checkSyntaxPrintsErrorReportOnInvalidSource(@TempDir final Path tempDir)
            throws IOException {
        final Path source = Files.writeString(tempDir.resolve("invalid-source.dr"), "@");
        final DefaultCompilerService service = new DefaultCompilerService();
        final ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        final PrintStream originalOut = System.out;

        try (PrintStream capturedOut = new PrintStream(stdout, true, StandardCharsets.UTF_8)) {
            System.setOut(capturedOut);
            try {
                service.checkSyntax(source);
            } catch (final CompilerException ignored) {
                // expected: the test is only interested in the emitted report
            } finally {
                System.setOut(originalOut);
            }
        }

        assertThat(stdout.toString(StandardCharsets.UTF_8))
                .contains("ERROR")
                .contains("Unrecognized character: '@'")
                .contains(source.toString());
    }

    @Test
    void compileAcceptsEmptyUtf8File(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.createFile(tempDir.resolve("program.dr"));
        final Path output = tempDir.resolve("program.bin");
        final DefaultCompilerService service = new DefaultCompilerService();

        assertThatCode(
                        () ->
                                service.compile(
                                        new CompilationRequest(
                                                source,
                                                output,
                                                OptimizationLevel.NONE,
                                                false,
                                                false)))
                .doesNotThrowAnyException();
    }
}
