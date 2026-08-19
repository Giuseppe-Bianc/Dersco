# CLI reference

The Dersco command-line application is named `dersco`. It is implemented with Picocli and exposes two project commands, `check` and `compile`, plus the standard help, version, and built-in help command.

## Global command

```text
dersco [OPTIONS] COMMAND
```

When no subcommand is supplied, Dersco prints the root command usage information.

Standard Picocli options include:

```bash
dersco --help
dersco --version
```

The application entry point also configures case-insensitive enum parsing. This affects enum-valued options such as `--optimize`.

## Logging options

Both `check` and `compile` include the same logging mixin. The mixin may be placed with either command and accepts repeated verbosity flags.

| Option | Effect |
| --- | --- |
| `-q`, `--quiet` | Set the Logback root logger to `ERROR`. Takes precedence over verbosity. |
| `-v`, `--verbose` | One verbosity occurrence sets the root logger to `INFO`. |
| `-vv` | Two verbosity occurrences set the root logger to `DEBUG`. |
| `-vvv` or more | Three or more verbosity occurrences set the root logger to `TRACE`. |

Without a logging flag, the root logger uses `WARN`.

Examples:

```bash
dersco check source.der
dersco check -v source.der
dersco compile -vv source.der
dersco compile -q source.der
dersco -v compile source.der
```

## `check`

`check` is the front-end validation command.

```text
dersco check [OPTIONS] FILE
```

Example:

```bash
./gradlew run --args="check examples/hello.der"
```

The command performs these operations:

1. validates that `FILE` is a readable regular file;
2. loads it as UTF-8;
3. tokenizes it with the Dersco lexer;
4. renders lexer errors with source context when errors are found;
5. returns a command status based on whether the compiler service reported an error.

The current implementation does not invoke a parser. The service method is named `checkSyntax`, but the active check is lexical analysis plus diagnostic reporting. It should therefore not be treated as complete language-level syntax validation.

### Exit status

| Code | Meaning |
| ---: | --- |
| `0` | Front-end check completed without detected lexer/compiler errors. |
| `1` | A `CompilerException` was raised while checking the source. |

A missing, non-regular, or unreadable input file is rejected by Picocli with a parameter error before the compiler service is invoked.

## `compile`

`compile` is the current compilation entry point.

```text
dersco compile [OPTIONS] FILE
```

### Options

| Option | Default | Meaning | Active backend behavior |
| --- | --- | --- | --- |
| `-o`, `--output FILE` | `a.exe` | Requested output path | Stored in `CompilationRequest`, not written |
| `-O`, `--optimize LEVEL` | `NONE` | Requested optimization level: `NONE`, `BASIC`, or `AGGRESSIVE` | Stored in `CompilationRequest`, not applied |
| `--emit-ir` | disabled | Request intermediate representation output | Stored in `CompilationRequest`, not emitted |
| `--diagnostics` | disabled | Request advanced diagnostics | Stored in `CompilationRequest`, not consumed |

Example:

```bash
./gradlew run --args="compile examples/hello.der --output build/hello.exe --optimize BASIC"
```

The current command validates the input file, constructs a `CompilationRequest`, and calls `DefaultCompilerService.compile(...)`. That service currently calls `checkSyntax(request.source())` and then stops.

### Current limitation

The `compile` command does **not** currently:

- parse the source into an AST;
- perform semantic analysis;
- apply optimization;
- generate IR;
- generate native or executable code;
- create the requested `--output` file;
- enable additional diagnostic processing from `--diagnostics`.

The options are part of the public request model so that the CLI can already expose the intended compilation contract, but the backend implementation is not connected yet.

The command logs a successful compilation message containing the requested output path after the service returns. That message does not mean that the output file exists.

### Exit status

| Code | Meaning |
| ---: | --- |
| `0` | The current compilation entry point completed without a `CompilerException`. |
| `1` | The compiler service raised a `CompilerException`. |

An invalid or unreadable input file is reported as a Picocli parameter error.

## Input file encoding

The compiler service reads the complete source file with `Files.readString(..., StandardCharsets.UTF_8)`. Dersco source files should therefore be encoded as UTF-8.

If reading the source fails after command-level validation, the compiler service wraps the `IOException` in `CompilerException`, which causes the command to return exit status `1`.

## Diagnostics

Lexer errors are collected by `LexerResult` and passed to `ErrorReporter` together with the lexer's `LineTracker`. The reporter renders source context and error information to standard output before the compiler service raises `CompilerException`.

At normal log level, token details are not printed. With `-vv` or `-vvv`, `DefaultCompilerService` logs the recognized tokens at `DEBUG` or `TRACE` level respectively.

## Running from the repository

During development, invoke the application through Gradle:

```bash
./gradlew run --args="--help"
./gradlew run --args="check path/to/source.der"
./gradlew run --args="compile path/to/source.der"
```

On Windows, use `gradlew.bat`:

```bat
gradlew.bat run --args="check path/to/source.der"
```

After building the shaded JAR:

```bash
./gradlew shadowJar
java -jar app/build/libs/Dersco-0.1.0.jar --help
```

The JAR manifest declares `org.dersbian.App` as its main class.

## Optimization levels

`OptimizationLevel` currently defines three enum values:

```text
NONE
BASIC
AGGRESSIVE
```

The application enables case-insensitive enum parsing, so the CLI accepts values without requiring a specific letter case. No optimization is currently performed by `DefaultCompilerService`.

## Command implementation map

| CLI component | Responsibility |
| --- | --- |
| `org.dersbian.App` | Bootstrap Picocli and map command result to process exit code |
| `RootCommand` | Root command, subcommand registration, help, version |
| `CheckCommand` | Input validation, front-end check invocation, exit status mapping |
| `CompileCommand` | Input validation, compilation-request construction, compilation invocation, exit status mapping |
| `LoggingMixin` | Runtime Logback root-level configuration |
| `ManifestVersionProvider` | Application version exposed by `--version` |
| `CliExecutionExceptionHandler` | Mapping uncaught CLI execution exceptions to CLI output and status |

For compiler internals and the exact current execution path, see [architecture.md](architecture.md).
