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
    "PMD.UnitTestAssertionsShouldIncludeMessage",
    "PMD.UnitTestContainsTooManyAsserts"
})
class CompileCommandTest {

    @Test
    void callSucceedsWhenServiceCompletes(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.createFile(tempDir.resolve("source.dr"));
        final RecordingService service = new RecordingService();
        final CompileCommand command = new CompileCommand(service);

        final Integer exit = new CommandLine(command).execute(source.toString());

        assertThat(exit).isEqualTo(0);
        assertThat(service.requests).hasSize(1);
    }

    @Test
    void callForwardsSourcePathToService(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.createFile(tempDir.resolve("source.dr"));
        final RecordingService service = new RecordingService();
        final CompileCommand command = new CompileCommand(service);

        new CommandLine(command).execute(source.toString());

        assertThat(service.requests.get(0).source()).isEqualTo(source);
    }

    @Test
    void callForwardsCustomOutputPath(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.createFile(tempDir.resolve("source.dr"));
        final Path output = tempDir.resolve("out.bin");
        final RecordingService service = new RecordingService();
        final CompileCommand command = new CompileCommand(service);

        new CommandLine(command).execute(source.toString(), "-o", output.toString());

        assertThat(service.requests.get(0).output()).isEqualTo(output);
    }

    @Test
    void callDefaultsOutputToAExe(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.createFile(tempDir.resolve("source.dr"));
        final RecordingService service = new RecordingService();
        final CompileCommand command = new CompileCommand(service);

        new CommandLine(command).execute(source.toString());

        assertThat(service.requests.get(0).output().getFileName().toString()).isEqualTo("a.exe");
    }

    @Test
    void callForwardsOptimizationLevel(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.createFile(tempDir.resolve("source.dr"));
        final RecordingService service = new RecordingService();
        final CompileCommand command = new CompileCommand(service);

        new CommandLine(command).execute(source.toString(), "-O", "AGGRESSIVE");

        assertThat(service.requests.get(0).optimizationLevel())
                .isEqualTo(OptimizationLevel.AGGRESSIVE);
    }

    @Test
    void callDefaultsOptimizationToNone(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.createFile(tempDir.resolve("source.dr"));
        final RecordingService service = new RecordingService();
        final CompileCommand command = new CompileCommand(service);

        new CommandLine(command).execute(source.toString());

        assertThat(service.requests.get(0).optimizationLevel()).isEqualTo(OptimizationLevel.NONE);
    }

    @Test
    void callForwardsEmitIrFlag(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.createFile(tempDir.resolve("source.dr"));
        final RecordingService service = new RecordingService();
        final CompileCommand command = new CompileCommand(service);

        new CommandLine(command).execute(source.toString(), "--emit-ir");

        assertThat(service.requests.get(0).emitIntermediateCode()).isTrue();
    }

    @Test
    void emitIrDefaultsToFalse(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.createFile(tempDir.resolve("source.dr"));
        final RecordingService service = new RecordingService();
        final CompileCommand command = new CompileCommand(service);

        new CommandLine(command).execute(source.toString());

        assertThat(service.requests.get(0).emitIntermediateCode()).isFalse();
    }

    @Test
    void callForwardsDiagnosticsFlag(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.createFile(tempDir.resolve("source.dr"));
        final RecordingService service = new RecordingService();
        final CompileCommand command = new CompileCommand(service);

        new CommandLine(command).execute(source.toString(), "--diagnostics");

        assertThat(service.requests.get(0).diagnostics()).isTrue();
    }

    @Test
    void diagnosticsDefaultsToFalse(@TempDir final Path tempDir) throws IOException {
        final Path source = Files.createFile(tempDir.resolve("source.dr"));
        final RecordingService service = new RecordingService();
        final CompileCommand command = new CompileCommand(service);

        new CommandLine(command).execute(source.toString());

        assertThat(service.requests.get(0).diagnostics()).isFalse();
    }

    @Test
    void callReturnsOneWhenServiceThrowsCompilerException(@TempDir final Path tempDir)
            throws IOException {
        final Path source = Files.createFile(tempDir.resolve("source.dr"));
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

        // picocli's default exit code on invalid input is 2; the ParameterException is logged and
        // not re-thrown across the execute() boundary.
        assertThat(exit).isEqualTo(commandLine.getCommandSpec().exitCodeOnInvalidInput());
    }

    @Test
    void callExitsWithInvalidInputCodeWhenInputIsDirectory(@TempDir final Path tempDir) {
        final CompileCommand command = new CompileCommand(new RecordingService());
        final CommandLine commandLine = new CommandLine(command);

        // tempDir itself exists and is a directory; not a readable file.
        final Integer exit = commandLine.execute(tempDir.toString());

        assertThat(exit).isEqualTo(commandLine.getCommandSpec().exitCodeOnInvalidInput());
    }

    @Test
    void callDoesNotInvokeServiceWhenInputIsInvalid(@TempDir final Path tempDir) {
        final Path missing = tempDir.resolve("does-not-exist.dr");
        final RecordingService service = new RecordingService();
        final CompileCommand command = new CompileCommand(service);

        new CommandLine(command).execute(missing.toString());

        assertThat(service.requests).isEmpty();
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
