# Architecture

## Scope

Dersco is currently an executable Java CLI and a compiler front end under active development. The codebase contains the infrastructure for source loading, source-location tracking, lexical analysis, token modeling, structured diagnostics, and AST data structures. The parser, semantic analysis, IR generation, and code generation are not connected to the default compilation service.

This distinction is important when reading the code. Some classes describe future compiler stages, but their presence in the repository does not mean those stages are currently executed by `dersco compile`.

## High-level execution flow

The active runtime path is:

```text
App
 |
 v
RootCommand
 |
 +--------------------+
 |                    |
 v                    v
check               compile
 |                    |
 v                    v
ICompilerService   CompilationRequest
 |                    |
 +---------+----------+
           |
           v
DefaultCompilerService
           |
           v
Files.readString(UTF-8)
           |
           v
Lexer(source, content)
           |
     +-----+-----+
     |           |
     v           v
 tokens       lexer errors
     |           |
     |           v
     |      ErrorReporter
     |           |
     +-----+-----+
           |
           v
        CLI result
```

`compile` currently calls `checkSyntax(request.source())` and then returns. The requested output path, optimization level, IR flag, and advanced diagnostics flag are not consumed by a backend.

## Application bootstrap

`org.dersbian.App` contains only process bootstrap logic. It creates the Picocli `CommandLine` instance from `RootCommand`, installs the CLI execution exception handler, enables case-insensitive enum values, enables automatic usage width, executes the parsed command, and terminates the process with the resulting exit code.

No compiler-domain logic belongs in `App`.

## CLI layer

The `org.dersbian.cli` package owns command-line parsing, input validation, exit-code mapping, and runtime logging configuration.

### `RootCommand`

`RootCommand` defines the `dersco` command and registers:

- `compile`;
- `check`;
- Picocli's built-in `help` command;
- standard help and version options.

When no subcommand is supplied, it prints command usage information. Version information is supplied by `ManifestVersionProvider`.

### `CheckCommand`

`CheckCommand` validates that the supplied path is a readable regular file and calls `ICompilerService.checkSyntax(...)`.

A `CompilerException` is mapped to exit code `1`. A successful front-end pass returns exit code `0`.

The name `checkSyntax` is retained in the service API, but the current implementation does not invoke a parser. It loads the source and tokenizes it, so the command currently detects lexical errors rather than providing complete language-level syntax validation.

### `CompileCommand`

`CompileCommand` validates the input file and constructs a `CompilationRequest` containing:

- source path;
- requested output path;
- optimization level;
- IR emission flag;
- advanced diagnostics flag.

It then calls `ICompilerService.compile(...)`. `CompilerException` is mapped to exit code `1`.

The command logs a successful compilation using the requested output path even though no output file is currently created. This is a consequence of the CLI being ahead of the backend implementation and should not be interpreted as proof that the output exists.

### `CompilationRequest`

`CompilationRequest` is an immutable record and the data boundary between the CLI and compiler service. Its fields are:

| Field | Meaning | Currently consumed by backend |
| --- | --- | --- |
| `source` | Input Dersco source path | Yes |
| `output` | Requested output path | No |
| `optimizationLevel` | Requested optimization level | No |
| `emitIntermediateCode` | Request IR emission | No |
| `diagnostics` | Request advanced diagnostics | No |

### `LoggingMixin`

`LoggingMixin` is included independently by the `check` and `compile` commands. It changes the Logback root logger at command execution time.

| Flags | Root log level |
| --- | --- |
| none | `WARN` |
| `-v` | `INFO` |
| `-vv` | `DEBUG` |
| `-vvv` or more | `TRACE` |
| `-q` | `ERROR` |

Quiet mode takes precedence over verbosity because `resolveLevel()` checks it first. The mixin accepts repeated `-v` occurrences and therefore supports forms such as `-vv`.

## Compiler service layer

`ICompilerService` isolates the CLI from the compiler implementation. `DefaultCompilerService` is the current production implementation.

### `checkSyntax(Path)` current behavior

The method performs these operations:

1. logs the requested source path;
2. records and logs the source file size using `FileSizeInfo`, `FileSizeReport`, and the SI/IEC size systems;
3. reads the entire file as UTF-8 using `Files.readString`;
4. creates a `Lexer` with the source path and source content;
5. obtains the lexer line count for diagnostics/logging;
6. tokenizes the source;
7. creates an `ErrorReporter` from the lexer's line tracker;
8. renders all lexer errors;
9. prints the rendered diagnostic report when errors exist and throws `CompilerException`;
10. logs each token at debug level when no lexer errors exist;
11. stops at the parser placeholder.

A source-read `IOException` is wrapped in `CompilerException`. Lexer errors are also converted into `CompilerException` after their diagnostics are printed.

### `compile(CompilationRequest)` current behavior

The method currently performs only:

```text
CompilationRequest
       |
       v
checkSyntax(request.source())
       |
       v
return
```

The method contains an explicit placeholder for semantic analysis and code generation. No output path is created, no IR is emitted, and no optimization is performed.

## Lexical analysis

The `org.dersbian.compiler.lexer` package provides the active front-end tokenizer.

### `Lexer`

`Lexer` scans the complete source string and produces a `LexerResult` containing tokens and lexical errors. It owns the lexical state and connects source traversal, token construction, and line tracking.

### `SourceCursor`

`SourceCursor` encapsulates traversal of the source text. It provides the cursor abstraction used by the lexer instead of making tokenization logic manage raw string indexes directly.

### `CodePoints`

`CodePoints` centralizes character and Unicode code-point related classification and operations used by lexical scanning.

### `LexerResult`

`LexerResult` is the lexer boundary object. It carries both the successfully recognized token sequence and the collected lexical errors so the compiler service can render diagnostics without coupling `ErrorReporter` to the scanner's internal state.

### Source locations and tokens

The token model includes `Token`, `TokenKind`, `Span`, `SourceId`, and `SourceLocation`. Tokens carry source span information, while the location types provide the data required to map diagnostics back to the original source.

Numeric token parsing is further separated into `INumber`, `BaseNumberParser`, `NumericParser`, and `SuffixParser`.

## Source locations and diagnostics

The location infrastructure contains `LineTracker`, which maps source positions to line information used by diagnostics.

The `compiler.error` package provides:

- `CompileError`, the structured compiler error representation;
- `CompilerPhase`, which classifies the compiler stage associated with an error;
- `ErrorCode`, the repository's error-code catalog;
- `Severity`, the diagnostic severity model;
- `ErrorReporter`, which converts structured errors into user-facing text;
- `CompilerErorFormater`, the formatting helper used by the diagnostic system.

The active CLI path uses the lexer line tracker and `ErrorReporter` to render lexical errors with source context.

## Syntax and AST model

The `org.dersbian.compiler.syntax.ast` package contains AST model types such as `Expr`, `Stmt`, `Type`, `BinaryOp`, `UnaryOp`, `LiteralValue`, `Parameter`, and `ElseBranch`, plus traversal and inspection helpers such as `AstPrinter` and `NodeCounter`.

These classes form a model boundary for later parsing and semantic work. The current `DefaultCompilerService` does not instantiate an AST from source because a parser is not connected to the compilation flow.

## Utility layer

The `org.dersbian.util` package provides reusable support code. In the current compiler service, the relevant classes are:

- `FileSizeInfo`, which represents the source file size;
- `FileSizeReport`, which formats size information for logging;
- `SizeSystem` and `SizeSystems`, which define size-system behavior, including SI and IEC systems;
- `FormattedSize` and `FormattedSizePair`, which represent formatted size values;
- `PathUtils`, which contains shared path handling utilities.

The compiler service uses the file-size utilities only for debug logging. They are not part of the compilation semantics.

## Build architecture

The repository is a Gradle multi-project build containing the `app` module.

The application module selects Java 25 and uses the Gradle wrapper pinned to 9.7.0. The root settings apply the Foojay toolchain resolver convention plugin and include only the `app` project.

The application dependencies are managed through `gradle/libs.versions.toml`. The runtime and test stack includes Picocli, SLF4J, Logback, Jansi, Guava, JUnit Jupiter, and AssertJ. Build-time quality tooling includes Checkstyle, PMD, SpotBugs, Error Prone, Spotless, JaCoCo, and Shadow.

`app/build.gradle.kts` configures Java compilation as UTF-8, enables compiler lint diagnostics, treats compiler warnings as errors, runs Error Prone, and configures the static-analysis and formatting tasks. The `check` task explicitly depends on Checkstyle, PMD, SpotBugs, Spotless verification, and `jacocoTestReport`.

The shaded JAR is produced by Shadow and declares `org.dersbian.App` as its main class.

## Test architecture

Unit tests are under `app/src/test/java` and use JUnit Jupiter and AssertJ. Compiler services and CLI commands expose constructor injection points so tests can substitute `ICompilerService` implementations without requiring the real compiler service.

The repository also contains `.dr` fixtures under `dr_files`, which cover valid programs and front-end error cases such as type mismatches, invalid indexes, loop control, returns, arrays, SSA-related examples, and numeric behavior.

## CI/CD architecture

`.github/workflows/ci.yml` defines two jobs.

### Build and verify

The `build-and-check` job:

1. checks out full repository history;
2. installs Temurin JDK 25;
3. configures Gradle;
4. runs `./gradlew check`;
5. builds the shaded JAR;
6. uploads JAR artifacts;
7. generates the JaCoCo XML report;
8. uploads the report to Codecov.

The Codecov action currently uses `fail_ci_if_error: true`, so a Codecov upload failure fails the workflow.

### Release

The `release` job depends on `build-and-check` and runs only when the triggering ref is a tag beginning with `v`. It installs JDK 25, builds the shaded JAR, and attaches the resulting JAR to a GitHub Release.

## Design boundaries and future stages

The intended expansion boundary is:

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
 +--> lexical analysis
 +--> diagnostics
 +--> parser                 future connection
 +--> semantic analysis      future connection
 +--> IR generation          future connection
 +--> code generation       future connection
```

The existing AST and compilation-request types make some future boundaries explicit, but they are not evidence of an active end-to-end compiler pipeline. Any documentation claiming that Dersco currently parses, performs semantic analysis, optimizes, emits IR, or generates an executable would be inconsistent with the implementation.
