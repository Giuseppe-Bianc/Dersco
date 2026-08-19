# Architecture

## Scope

Dersco is currently an executable compiler front end and command-line application under development. The active compiler flow loads a source file as UTF-8, tokenizes it, tracks source locations, and renders lexical diagnostics. The repository also contains structures for syntax and AST work, but they are not invoked by the current compiler service.

The most important distinction for understanding the current implementation is between **available models** and **active execution stages**. `CompilationRequest`, `OptimizationLevel`, the syntax/AST package, and token parsing infrastructure expose extension points for later compiler stages. They do not mean that parsing, semantic analysis, IR generation, optimization, or code generation currently occur.

## High-level execution flow

```text
App
 |
 v
RootCommand
 |
 +--> CheckCommand -----------------------------+
 |                                               |
 +--> CompileCommand --> CompilationRequest     |
                          |                      |
                          v                      v
                    ICompilerService      DefaultCompilerService
                                               |
                                               v
                                      checkSyntax(source)
                                               |
                         +---------------------+--------------------+
                         |                                          |
                  Files.readString(UTF-8)                         Lexer
                         |                                          |
                         |                                  SourceCursor
                         |                                          |
                         |                                  LexerResult
                         |                                    /       \
                         |                                 tokens    errors
                         |                                             |
                         |                                       ErrorReporter
                         |                                             |
                         +-----------------------------+---------------+
                                                       |
                                             CompilerException on errors
                                                       |
                                                       v
                                                CLI exit code
```

`check` calls `ICompilerService.checkSyntax(...)`. `compile` creates a `CompilationRequest` and calls `ICompilerService.compile(...)`. `DefaultCompilerService.compile(...)` currently delegates only to `checkSyntax(...)` and then returns.

## Application entry point

`org.dersbian.App` is the Java application entry point. It creates and executes the Picocli command tree rooted at `RootCommand`.

The application is configured by Gradle with `org.dersbian.App` as the main class. The same main class is written into the standard JAR and shaded JAR manifests.

## CLI layer

The `org.dersbian.cli` package owns command parsing, input validation, exit-code translation, logging configuration, and version presentation.

### `RootCommand`

`RootCommand` defines the `dersco` command and registers the `check` and `compile` subcommands. Picocli standard help and version support are enabled, and a built-in help command is available. When no subcommand is supplied, Picocli displays command usage information.

### `CheckCommand`

`CheckCommand`:

1. applies the requested logging level;
2. validates that the input path is a readable regular file;
3. calls `ICompilerService.checkSyntax(...)`;
4. translates `CompilerException` to exit code `1`;
5. returns exit code `0` when the service completes without an exception.

Input validation failures are raised as Picocli `ParameterException` instances so they use Picocli's parameter-error handling.

The command's current implementation checks the lexical front end through `DefaultCompilerService`. It does not invoke a parser despite the command's user-facing name and description referring to syntax checking.

### `CompileCommand`

`CompileCommand` performs the same input validation pattern and constructs a `CompilationRequest` containing:

- source path;
- requested output path;
- optimization level;
- intermediate representation emission flag;
- advanced diagnostics flag.

It then calls `ICompilerService.compile(...)` and maps `CompilerException` to exit code `1`.

The command defaults to `a.exe` for the output path and `OptimizationLevel.NONE` for optimization. These values are request defaults only. `DefaultCompilerService` currently consumes only `request.source()`.

### Dependency injection in commands

Both commands have a production constructor that creates `DefaultCompilerService` and a constructor used by tests to inject an `ICompilerService`. This keeps command tests independent from a real compiler implementation and makes the service boundary explicit.

### `LoggingMixin`

`LoggingMixin` centralizes CLI logging controls. The intended levels are:

| Flags | Root logger level |
| --- | --- |
| none | `WARN` |
| `-v` | `INFO` |
| `-vv` | `DEBUG` |
| `-vvv` or higher | `TRACE` |
| `-q` | `ERROR` |

Quiet mode takes precedence over verbosity. Logging is implemented through SLF4J and Logback.

### `CliExecutionExceptionHandler`

The CLI package also contains a dedicated exception handler for Picocli execution failures. It is part of the command-line error-handling infrastructure and should be considered separately from `CompilerException`, which represents failures reported by the compiler service.

### `ManifestVersionProvider`

`ManifestVersionProvider` supplies the application version exposed through Picocli version support. The Gradle build writes `Implementation-Version` into the application manifest from the project version.

## Compiler service layer

### `ICompilerService`

`ICompilerService` is the abstraction consumed by the CLI. It separates command handling from the compiler implementation and defines the two current entry points:

- `checkSyntax(Path source)`;
- `compile(CompilationRequest request)`.

The interface is also the primary seam used to inject test doubles into CLI commands.

### `DefaultCompilerService`

`DefaultCompilerService` is the current production implementation. Its responsibilities are currently limited to source loading, lexical analysis, lexical error reporting, and token debug logging.

It also records the source file size for debug logging through `FileSizeInfo`, `FileSizeReport`, and the configured size systems. This information is diagnostic logging only and does not affect compilation behavior.

### `checkSyntax(...)` behavior

The method executes the following steps:

1. logs the requested source path;
2. reads the file with `Files.readString(..., StandardCharsets.UTF_8)`;
3. converts I/O failures into `CompilerException`;
4. creates a `Lexer` with the source path and source content;
5. obtains the line count from the lexer for debug logging;
6. tokenizes the source;
7. creates an `ErrorReporter` using the lexer's line tracker and source path;
8. renders lexer errors;
9. prints the rendered report when errors exist;
10. throws `CompilerException` containing the number of reported lexer errors;
11. logs generated tokens at debug level when no lexer errors exist.

The method ends with an explicit parser TODO. Therefore, it does not currently validate grammar-level syntax.

### `compile(...)` behavior

The current implementation is intentionally small:

```text
CompilationRequest
       |
       v
checkSyntax(request.source())
       |
       v
return
```

The following request fields are currently not consumed by the implementation:

- `output`;
- `optimizationLevel`;
- `emitIntermediateCode`;
- `diagnostics`.

Consequently, the current compile command does not create an executable or any other requested output artifact, does not emit IR, and does not apply optimization.

## Lexical analysis

The `org.dersbian.compiler.lexer` package is the active compiler stage after source loading.

### `Lexer`

`Lexer` scans the source text and produces a `LexerResult`. It is responsible for recognizing the token categories implemented by the current token model and collecting lexical errors instead of forcing the compiler service to handle individual scanner failures.

The lexer is constructed with both the source path and source content so that produced locations can refer to the originating source.

### `SourceCursor`

`SourceCursor` provides the lexer with controlled traversal of source text. It encapsulates cursor movement and character/code-point inspection so the main lexer does not have to manage raw string indexes everywhere.

### `CodePoints`

`CodePoints` centralizes character and code-point classification helpers used during lexing. It is an implementation utility of the lexical stage, not a separate compiler phase.

### `LexerResult`

`LexerResult` groups the two principal outputs of lexical analysis:

- the generated token sequence;
- the collected lexical errors.

`DefaultCompilerService` consumes both outputs. Errors are rendered for the user, while tokens are currently logged at debug level when lexing succeeds.

## Token model

The token model under `org.dersbian.compiler.lexer.token` provides the data boundary between lexical analysis and future parsing.

Key concepts include:

- `Token`, the token value and associated source information;
- `TokenKind`, the enumeration of supported token categories;
- `Span`, the source range occupied by a token;
- `SourceId`, which identifies the source associated with a location;
- `SourceLocation`, which represents a source position.

The token package also contains numeric token infrastructure under `token.number` and parsing helpers under `token.parser`. These components support token payload construction and conversion, but they do not constitute the compiler's grammar parser.

## Source locations and line tracking

Source location data is required by diagnostics and is propagated through lexical analysis. The lexer maintains a line tracker, which is passed to `ErrorReporter` after tokenization.

The result is that lexical errors can be associated with source context rather than reported as plain messages without a location.

## Diagnostics and compiler errors

The `org.dersbian.compiler.error` package defines structured error metadata and rendering support.

Important types include:

- `CompileError`, the common error model;
- `CompilerPhase`, which identifies the compiler phase associated with an error;
- `ErrorCode`, which provides stable error identifiers;
- `Severity`, which represents diagnostic severity;
- `ErrorReporter`, which renders errors with source context;
- `CompilerErorFormater`, the current formatter implementation.

The current active path feeds lexer errors into `ErrorReporter`. When at least one lexer error is present, the service prints the rendered report and throws `CompilerException`, allowing the CLI command to return a non-zero exit code.

## Syntax and AST structure

The repository contains `compiler/syntax/ast`, but this package currently has no active implementation in the execution flow documented above. It should be treated as reserved architecture for future parser and AST work.

This distinction matters because the current codebase has a lexical token parser infrastructure, but it does not yet have a grammar parser connected to `DefaultCompilerService`.

## Build architecture

The repository is a Gradle multi-project build with one application module, `app`.

`settings.gradle.kts` names the root project `Dersco`, includes `app`, and enables the Foojay toolchain resolver. The Gradle wrapper is configured for Gradle 9.7.0.

The application module configures Java 25, application packaging, testing, static analysis, formatting, coverage, and the shaded JAR.

### Runtime and application dependencies

The application uses:

- Picocli 4.7.7 for CLI parsing;
- SLF4J 2.0.18 for the logging API;
- Logback 1.6.3 for the logging implementation;
- Jansi 2.4.3 at runtime for terminal support;
- Guava 33.6.0-jre;
- Picocli code generation at annotation-processing time.

Error Prone 2.50.0 is configured as a Java compilation-time analysis dependency.

### Test dependencies

The test suite uses JUnit Jupiter 6.1.3 and AssertJ 3.27.7. Tests run on the JUnit Platform. Gradle configures multiple test forks based on available processors, with at least one fork.

### Quality gates

The application build configures:

- Checkstyle 13.7.0, with warnings treated as failures;
- PMD 7.26.0 using `app/config/pmd/ruleset.xml`;
- SpotBugs 4.10.2 at maximum effort and low confidence reporting;
- Error Prone with compiler warnings treated as errors;
- Spotless 8.9.0 with Google Java Format 1.35.0 in AOSP mode;
- JaCoCo 0.8.14 for coverage;
- Shadow 9.6.1 for the shaded application JAR.

The `check` task explicitly depends on Checkstyle, PMD, SpotBugs, Spotless verification, and `jacocoTestReport`. The JaCoCo report task depends on the test task.

## Packaging

The application main class is `org.dersbian.App`. Both the normal JAR and shaded JAR receive manifest attributes for the main class, implementation version, implementation title, and native access.

The application and tests are configured with `--enable-native-access=ALL-UNNAMED`.

The shaded artifact uses the root project name as its base name, so the configured version produces `app/build/libs/Dersco-0.1.0.jar`.

## CI/CD

The repository defines CI and release automation in `.github/workflows/ci.yml`. The workflow is responsible for verification, packaging, coverage reporting, and tagged release publication.

The local commands that reproduce the principal build stages are:

```bash
./gradlew check
./gradlew shadowJar
./gradlew jacocoTestReport
```

JaCoCo's XML report is generated under `app/build/reports/jacoco/test/jacocoTestReport.xml` and is consumed by the Codecov integration.

## Architectural boundaries and future work

The current architecture can be summarized as:

```text
CLI
 |
 v
ICompilerService
 |
 v
DefaultCompilerService
 |
 +--> source loading
 +--> lexer
 +--> diagnostics
 |
 +--> future parser
 +--> future semantic analysis
 +--> future IR
 +--> future optimizer
 +--> future code generator
```

The key boundary for future compiler stages is `DefaultCompilerService`. Until that service invokes the parser and subsequent stages, documentation must describe those stages as planned or structurally prepared rather than implemented.
