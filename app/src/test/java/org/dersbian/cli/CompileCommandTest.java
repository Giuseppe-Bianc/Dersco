package org.dersbian.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.dersbian.compiler.CompilationRequest;
import org.dersbian.compiler.CompilerException;
import org.dersbian.compiler.ICompilerService;
import org.dersbian.compiler.OptimizationLevel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;
import picocli.CommandLine.ParameterException;

/**
 * Tests for {@link CompileCommand}.
 *
 * <p>Verifies picocli wiring (parameter index, options with defaults) and the call() contract:
 * success returns {@code 0}, {@link CompilerException} returns {@code 1}, missing/unreadable input
 * file throws {@link ParameterException}. The {@link ICompilerService} dependency is replaced with
 * a recording fake so we never touch the file system or the lexer.
 */
@SuppressWarnings({
    "PMD.AtLeastOneConstructor",
    "PMD.CommentRequired",
    "PMD.UnitTestContainsTooManyAsserts",
    "PMD.TooManyMethods"
})
class CompileCommandTest {

    private static final String SOURCE_FILE_NAME = "source.dr";

    @Test
    void callSucceedsWhenServiceCompletes(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.createFile(tempDir.resolve(SOURCE_FILE_NAME));
        final RecordingService service = new RecordingService();
        final CompileCommand command = new CompileCommand(service);

        final Integer exit = new CommandLine(command).execute(source.toString());

        assertThat(exit).isEqualTo(0);
        assertThat(service.requestCount()).isEqualTo(1);
    }

    @Test
    void callForwardsSourcePathToService(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.createFile(tempDir.resolve(SOURCE_FILE_NAME));
        final RecordingService service = new RecordingService();
        final CompileCommand command = new CompileCommand(service);

        new CommandLine(command).execute(source.toString());

        assertThat(service.firstSource()).isEqualTo(source);
    }

    @Test
    void callForwardsCustomOutputPath(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.createFile(tempDir.resolve(SOURCE_FILE_NAME));
        final Path output = tempDir.resolve("out.bin");
        final RecordingService service = new RecordingService();
        final CompileCommand command = new CompileCommand(service);

        new CommandLine(command).execute(source.toString(), "-o", output.toString());

        assertThat(service.firstOutput()).isEqualTo(output);
    }

    @Test
    void callDefaultsOutputToDefaultExecutableName(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.createFile(tempDir.resolve(SOURCE_FILE_NAME));
        final RecordingService service = new RecordingService();
        final CompileCommand command = new CompileCommand(service);

        new CommandLine(command).execute(source.toString());

        assertThat(service.firstOutputFileName()).isEqualTo("a.exe");
    }

    @Test
    void callForwardsOptimizationLevel(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.createFile(tempDir.resolve(SOURCE_FILE_NAME));
        final RecordingService service = new RecordingService();
        final CompileCommand command = new CompileCommand(service);

        new CommandLine(command).execute(source.toString(), "-O", "AGGRESSIVE");

        assertThat(service.firstOptimizationLevel()).isEqualTo(OptimizationLevel.AGGRESSIVE);
    }

    @Test
    void callDefaultsOptimizationToNone(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.createFile(tempDir.resolve(SOURCE_FILE_NAME));
        final RecordingService service = new RecordingService();
        final CompileCommand command = new CompileCommand(service);

        new CommandLine(command).execute(source.toString());

        assertThat(service.firstOptimizationLevel()).isEqualTo(OptimizationLevel.NONE);
    }

    @Test
    void callForwardsEmitIrFlag(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.createFile(tempDir.resolve(SOURCE_FILE_NAME));
        final RecordingService service = new RecordingService();
        final CompileCommand command = new CompileCommand(service);

        new CommandLine(command).execute(source.toString(), "--emit-ir");

        assertThat(service.firstEmitIntermediateCode()).isTrue();
    }

    @Test
    void emitIrDefaultsToFalse(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.createFile(tempDir.resolve(SOURCE_FILE_NAME));
        final RecordingService service = new RecordingService();
        final CompileCommand command = new CompileCommand(service);

        new CommandLine(command).execute(source.toString());

        assertThat(service.firstEmitIntermediateCode()).isFalse();
    }

    @Test
    void callForwardsDiagnosticsFlag(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.createFile(tempDir.resolve(SOURCE_FILE_NAME));
        final RecordingService service = new RecordingService();
        final CompileCommand command = new CompileCommand(service);

        new CommandLine(command).execute(source.toString(), "--diagnostics");

        assertThat(service.firstDiagnostics()).isTrue();
    }

    @Test
    void diagnosticsDefaultsToFalse(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.createFile(tempDir.resolve(SOURCE_FILE_NAME));
        final RecordingService service = new RecordingService();
        final CompileCommand command = new CompileCommand(service);

        new CommandLine(command).execute(source.toString());

        assertThat(service.firstDiagnostics()).isFalse();
    }

    @Test
    void callReturnsOneWhenServiceThrowsCompilerException(@TempDir final Path tempDir)
            throws IOException {
        final Path source = Files.createFile(tempDir.resolve(SOURCE_FILE_NAME));
        final CompileCommand command = new CompileCommand(new ThrowingService());

        final Integer exit = new CommandLine(command).execute(source.toString());

        assertThat(exit).isEqualTo(1);
    }

    @Test
    void callExitsWithInvalidInputCodeWhenFileMissing(@TempDir final Path tempDir) {
        final Path missing = tempDir.resolve("does-not-exist.dr");
        final CompileCommand command = new CompileCommand(new RecordingService());
        final CommandLine commandLine = new CommandLine(command);

        final Integer exit = commandLine.execute(missing.toString());

        assertThat(exit).isEqualTo(CommandLine.ExitCode.USAGE);
    }

    @Test
    void callExitsWithInvalidInputCodeWhenInputIsDirectory(@TempDir final Path tempDir) {
        final CompileCommand command = new CompileCommand(new RecordingService());
        final CommandLine commandLine = new CommandLine(command);

        // tempDir itself exists and is a directory; not a readable file.
        final Integer exit = commandLine.execute(tempDir.toString());

        assertThat(exit).isEqualTo(CommandLine.ExitCode.USAGE);
    }

    @Test
    void callDoesNotInvokeServiceWhenInputIsInvalid(@TempDir final Path tempDir) {
        final Path missing = tempDir.resolve("does-not-exist.dr");
        final RecordingService service = new RecordingService();
        final CompileCommand command = new CompileCommand(service);

        new CommandLine(command).execute(missing.toString());

        assertThat(service.hasNoRequests()).isTrue();
    }

    @Test
    void helpOptionReturnsZero() {
        final Integer exit =
                new CommandLine(new CompileCommand(new RecordingService())).execute("--help");

        assertThat(exit).isEqualTo(0);
    }

    /** Recording fake: captures the {@link CompilationRequest} passed to {@link #compile}. */
    private static final class RecordingService implements ICompilerService {
        private final java.util.List<CompilationRequest> requests = new java.util.ArrayList<>();

        @Override
        public void checkSyntax(final Path source) {
            // no-op
        }

        @Override
        public void compile(final CompilationRequest request) {
            requests.add(request);
        }

        private int requestCount() {
            return requests.size();
        }

        private boolean hasNoRequests() {
            return requests.isEmpty();
        }

        private Path firstSource() {
            return firstRequest().source();
        }

        private Path firstOutput() {
            return firstRequest().output();
        }

        private String firstOutputFileName() {
            final Path fileName = firstOutput().getFileName();
            return java.util.Objects.requireNonNull(
                            fileName, "output path has no file name: " + firstOutput())
                    .toString();
        }

        private OptimizationLevel firstOptimizationLevel() {
            return firstRequest().optimizationLevel();
        }

        private boolean firstEmitIntermediateCode() {
            return firstRequest().emitIntermediateCode();
        }

        private boolean firstDiagnostics() {
            return firstRequest().diagnostics();
        }

        private CompilationRequest firstRequest() {
            return requests.get(0);
        }
    }

    /** Fake that always throws {@link CompilerException}. */
    private static final class ThrowingService implements ICompilerService {
        @Override
        public void checkSyntax(final Path source) {
            // no-op
        }

        @Override
        public void compile(final CompilationRequest request) {
            throw new CompilerException("synthetic failure");
        }
    }
}
