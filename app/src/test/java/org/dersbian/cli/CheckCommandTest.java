package org.dersbian.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.dersbian.compiler.CompilerException;
import org.dersbian.compiler.ICompilerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

/**
 * Tests for {@link CheckCommand}.
 *
 * <p>Verifies the picocli wiring and the call() contract:
 *
 * <ul>
 *   <li>On success (no syntax errors), {@code checkSyntax} is invoked with the source path and the
 *       command exits with code {@code 0}.
 *   <li>On a {@link CompilerException}, the command exits with code {@code 1}.
 *   <li>On a missing or unreadable input file, picocli exits with its configured "invalid input"
 *       code (default 2) and the service is not invoked.
 * </ul>
 */
@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.LongVariable"
})
class CheckCommandTest {

    /** Picocli's default exit code for invalid command-line input, per {@code CommandSpec}. */
    private static final int DEFAULT_INVALID_INPUT_EXIT_CODE = 2;

    @Test
    void callInvokesCheckSyntaxWithSourcePath(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.createFile(tempDir.resolve("source.dr"));
        final RecordingService service = new RecordingService();
        final CheckCommand command = new CheckCommand(service);

        final Integer exit = new CommandLine(command).execute(source.toString());

        assertThat(exit).isEqualTo(0);
        assertThat(service.checked).containsExactly(source);
    }

    @Test
    void callReturnsOneWhenServiceThrowsCompilerException(@TempDir final Path tempDir)
            throws IOException {
        final Path source = Files.createFile(tempDir.resolve("source.dr"));
        final CheckCommand command = new CheckCommand(new ThrowingService());

        final Integer exit = new CommandLine(command).execute(source.toString());

        assertThat(exit).isEqualTo(1);
    }

    @Test
    void callExitsWithInvalidInputCodeWhenFileMissing(@TempDir final Path tempDir) {
        final Path missing = tempDir.resolve("does-not-exist.dr");
        final RecordingService service = new RecordingService();
        final CheckCommand command = new CheckCommand(service);

        final Integer exit = new CommandLine(command).execute(missing.toString());

        assertThat(exit).isEqualTo(DEFAULT_INVALID_INPUT_EXIT_CODE);
        assertThat(service.checked).isEmpty();
    }

    @Test
    void callExitsWithInvalidInputCodeWhenInputIsDirectory(@TempDir final Path tempDir) {
        final RecordingService service = new RecordingService();
        final CheckCommand command = new CheckCommand(service);

        // tempDir itself is an existing directory; not a regular file.
        final Integer exit = new CommandLine(command).execute(tempDir.toString());

        assertThat(exit).isEqualTo(DEFAULT_INVALID_INPUT_EXIT_CODE);
        assertThat(service.checked).isEmpty();
    }

    @Test
    void helpOptionReturnsZero() {
        final Integer exit =
                new CommandLine(new CheckCommand(new RecordingService())).execute("--help");

        assertThat(exit).isEqualTo(0);
    }

    /** Recording fake: captures paths passed to {@link #checkSyntax}. */
    private static final class RecordingService implements ICompilerService {
        private final List<Path> checked = new ArrayList<>();

        @Override
        public void checkSyntax(final Path source) {
            checked.add(source);
        }

        @Override
        public void compile(final org.dersbian.compiler.CompilationRequest request) {
            // not exercised by CheckCommand
        }
    }

    /** Fake that always throws {@link CompilerException} from {@code checkSyntax}. */
    private static final class ThrowingService implements ICompilerService {
        @Override
        public void checkSyntax(final Path source) {
            throw new CompilerException("synthetic syntax failure");
        }

        @Override
        public void compile(final org.dersbian.compiler.CompilationRequest request) {
            // not exercised by CheckCommand
        }
    }
}
