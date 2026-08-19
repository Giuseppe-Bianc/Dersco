# Architecture

## Scope

Dersco is currently a compiler front end and CLI under development. The repository contains the infrastructure needed to load source text, track source locations, tokenize input, and render diagnostics. Parsing, semantic analysis, and code generation are planned stages, but they are not connected to the default compiler service yet.

## High-level flow

```text
CLI
 |
 +--> check --------------------+
 |                              |
 +--> compile --> CompilationRequest
                                |
                                v
                       ICompilerService
                                |
                                v
                     DefaultCompilerService
                                |
                    +-----------+-----------+
                    |                       |
              read UTF-8 file          source metadata
                    |                       |
                    +-----------+-----------+
                                v
                              Lexer
                                |
                 +--------------+--------------+
                 |                             |
              Tokens                         Errors
                 |                             |
                 +--------------+--------------+
                                v
                         ErrorReporter
                                |
                                v
                         CLI diagnostics
```

The `compile` path currently reaches the same front end as `check` and then stops. The future backend is not part of the execution flow yet.

## Application entry point

`org.dersbian.App` starts the Picocli command tree. `RootCommand` defines the `dersco` command and delegates work to `CompileCommand` and `CheckCommand`.

The root command also enables Picocli's standard help options and obtains the application version through `ManifestVersionProvider`.

## CLI layer

The CLI package is responsible for argument parsing, validation, exit codes, and logging configuration.

### `RootCommand`

The root command provides:

- command name `dersco`;
- standard help and version options;
- `compile` and `check` subcommands;
- built-in help command.

If no subcommand is supplied, the command prints usage information.

### `CheckCommand`

`CheckCommand` validates the input path and calls `ICompilerService.checkSyntax(...)`. A `CompilerException` is converted to exit code `1`.

### `CompileCommand`

`CompileCommand` validates the input path and constructs a `CompilationRequest` containing:

- source path;
- requested output path;
- optimization level;
- IR emission flag;
- advanced diagnostics flag.

It then calls `ICompilerService.compile(...)`.

The request model is already designed for a future backend, but the current `DefaultCompilerService` does not consume the backend-related options after the front-end check.

### `LoggingMixin`

Logging is configured through Logback at runtime:

| Flags | Level |
| --- | --- |
| none | `WARN` |
| `-v` | `INFO` |
| `-vv` | `DEBUG` |
| `-vvv` or more | `TRACE` |
| `-q` | `ERROR` |

Quiet mode takes precedence over verbosity flags.

## Compiler service layer

`ICompilerService` separates CLI code from compiler implementation. `DefaultCompilerService` is the current implementation.

### Syntax checking

The current `checkSyntax(...)` implementation:

1. reads the source file as UTF-8;
2. creates a `Lexer` with the source path and source content;
3. obtains the source line count;
4. tokenizes the source;
5. passes lexer errors to `ErrorReporter`;
6. prints a rendered diagnostic report when errors are present;
7. logs tokens at debug level when no lexer errors are reported.

The implementation currently contains an explicit placeholder for the real parser.

### Compilation

`compile(...)` currently performs:

```text
CompilationRequest
       |
       v
checkSyntax(source)
       |
       v
return
```

Semantic analysis and code generation are not connected. Consequently, the output path, optimization level, IR flag, and diagnostics flag are currently request-level API surface rather than active backend functionality.

## Lexer

The lexer package contains the source scanning infrastructure. Its main components include:

- `Lexer`, the main tokenizer;
- `SourceCursor`, which manages traversal of source text;
- `LexerResult`, which groups tokens and lexical errors;
- `CodePoints`, which centralizes character classification and related code-point operations;
- source location and token packages for precise diagnostics.

The lexer tracks source positions so diagnostics can point back to the original file and line.

## Token model

Tokens are modeled as typed values and carry source span information. The token package contains the token kinds and payload-bearing representations used by the lexer.

This model is intended to provide a stable boundary between lexical analysis and the parser that will be added later.

## Diagnostics

The compiler error package provides structured compiler errors and an `ErrorReporter`. Diagnostics are rendered with source context so users can identify the affected location directly from CLI output.

The location package provides source-position primitives and line tracking used by the lexer and diagnostic renderer.

## Build and quality architecture

The project is a Gradle multi-project build with the `app` module.

The application build uses:

- Java 25 toolchain;
- JUnit 6 and AssertJ for tests;
- Picocli for the CLI;
- SLF4J and Logback for logging;
- Jansi for terminal support;
- Checkstyle;
- PMD;
- SpotBugs at maximum effort and low confidence threshold;
- Error Prone with compiler warnings treated as errors;
- Spotless with Google Java Format in AOSP mode;
- JaCoCo for test coverage;
- Shadow for the packaged application JAR.

The `check` task explicitly depends on the main static-analysis tasks, formatting verification, and the JaCoCo test report.

## CI/CD architecture

The GitHub Actions workflow provisions JDK 25, configures Gradle, runs verification, builds the shaded JAR, uploads the artifact, regenerates the JaCoCo XML report, and uploads coverage to Codecov.

Release publication is separated into a second job that runs for tags matching `v*` and depends on the successful build job.

## Design boundaries

The current code has useful boundaries for future expansion:

```text
CLI -> ICompilerService -> front-end components
                         |
                         +-> future parser
                         +-> future semantic analysis
                         +-> future IR
                         +-> future code generator
```

The main architectural constraint is that the backend must be wired into `DefaultCompilerService` before `compile` can be documented as a real compiler command. Until then, documentation should describe it as a compilation entry point that currently performs front-end validation.
