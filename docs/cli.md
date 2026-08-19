# CLI reference

The Dersco command-line application is named `dersco` and is implemented with Picocli. The current command tree exposes `check` and `compile`, plus Picocli standard help and version support.

## Global command

```text
dersco [OPTIONS] COMMAND
```

If no subcommand is supplied, Picocli displays the command usage information.

Built-in help and version support are available through:

```bash
dersco --help
dersco --version
```

The version is supplied from the application manifest by `ManifestVersionProvider`.

## Logging options

Both project commands include the logging mixin.

| Option | Effect |
| --- | --- |
| `-q`, `--quiet` | Set the root logger to `ERROR`. |
| `-v`, `--verbose` | Set the root logger to `INFO`. |
| `-vv` | Set the root logger to `DEBUG`. |
| `-vvv` and higher | Set the root logger to `TRACE`. |

Without a verbosity flag, the root logger uses `WARN`. Quiet mode takes precedence when combined with verbosity flags.

Examples:

```bash
dersco check source.der
dersco check -v source.der
dersco compile -vv source.der
dersco compile -q source.der
```

## `check`

Checks a source file through the compiler service's current front-end validation path.

```text
dersco check [OPTIONS] FILE
```

Example:

```bash
./gradlew run --args="check examples/hello.der"
```

### Processing performed

The command:

1. verifies that `FILE` is a readable regular file;
2. reads the source as UTF-8;
3. tokenizes it with `Lexer`;
4. reports lexical errors with source context;
5. returns success when no lexer errors are detected.

The current implementation does **not** invoke a grammar parser. Therefore `check` should currently be understood as a lexical front-end check, even though its command description refers to syntactic correctness.

### Exit codes

| Code | Meaning |
| ---: | --- |
| `0` | The compiler service completed without a `CompilerException`, and no lexer errors were reported. |
| `1` | The compiler service raised `CompilerException`, including lexical errors and source-reading failures. |

A missing, non-regular, or unreadable input file raises a Picocli `ParameterException` during command execution and is handled as a CLI parameter error.

## `compile`

Runs the current compilation entry point for a source file.

```text
dersco compile [OPTIONS] FILE
```

### Options

| Option | Default | Meaning | Current effect |
| --- | --- | --- | --- |
| `-o`, `--output FILE` | `a.exe` | Requested output path. | Stored in `CompilationRequest`; no output is currently created. |
| `-O`, `--optimize LEVEL` | `NONE` | Optimization level. | Stored in `CompilationRequest`; currently unused by the service. |
| `--emit-ir` | disabled | Request IR output. | Stored in `CompilationRequest`; IR is not generated. |
| `--diagnostics` | disabled | Request advanced diagnostics. | Stored in `CompilationRequest`; no separate advanced diagnostic phase is currently executed. |

The supported optimization levels are the values of `OptimizationLevel`: `NONE`, `BASIC`, and `AGGRESSIVE`.

Example:

```bash
./gradlew run --args="compile examples/hello.der --output build/hello.exe --optimize BASIC"
```

### Current execution

The current implementation is:

```text
compile FILE
    |
    v
validate FILE
    |
    v
CompilationRequest
    |
    v
DefaultCompilerService.compile(...)
    |
    v
checkSyntax(source)
    |
    v
return
```

`DefaultCompilerService.compile(...)` currently uses only the source path from the request. It does not consume the requested output path, optimization level, IR flag, or diagnostics flag.

As a result, a successful `compile` command currently means that the active front-end validation completed. It does **not** mean that an executable or other output artifact was generated.

### Exit codes

| Code | Meaning |
| ---: | --- |
| `0` | The current compilation entry point completed without a `CompilerException`. No output artifact is guaranteed to exist. |
| `1` | The compiler service raised `CompilerException`. |

## Input file handling

Both commands validate the input path before calling the compiler service. The path must identify a regular file and the process must be able to read it.

The compiler service reads source files with `StandardCharsets.UTF_8`. The current CLI does not expose an encoding option.

## Error handling

There are two relevant error paths:

1. **Picocli parameter errors.** Invalid command-line arguments or an unreadable input file are represented by Picocli's parameter error mechanism.
2. **Compiler errors.** `DefaultCompilerService` reports lexical errors through `ErrorReporter`, then throws `CompilerException`. The `check` and `compile` commands convert this exception to exit code `1`.

The rendered compiler diagnostic is printed by the compiler service before the exception reaches the CLI command.

## Running from the repository

During development, invoke the application through Gradle:

```bash
./gradlew run --args="--help"
./gradlew run --args="check path/to/source.der"
./gradlew run --args="compile path/to/source.der"
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

After creating the shaded JAR:

```bash
./gradlew shadowJar
java -jar app/build/libs/Dersco-0.1.0.jar --help
```

The configured project version is `0.1.0`, and the Shadow task uses the root project name as the artifact base name.

## Source encoding

The compiler service reads source files as UTF-8. This is the effective source encoding of the current implementation.

## CLI architecture for developers

The command classes depend on `ICompilerService` rather than directly coupling command execution to compiler internals. In production, the default constructors instantiate `DefaultCompilerService`. Tests can inject another `ICompilerService` implementation.

`CompileCommand` maps its CLI options into `CompilationRequest`. The request is deliberately broader than the currently active implementation because it defines the API surface needed by future compilation stages.

For component responsibilities and compiler execution flow, see [architecture.md](architecture.md).
