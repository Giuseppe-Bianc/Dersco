# AGENTS.md

## Project Layout

This is a Gradle 9.7.1 multi-project build with one application module, `app`. Run the wrapper from the repository root and scope module-specific work to `:app`.

- Build layout and Java 25 toolchain: [settings.gradle.kts](settings.gradle.kts) and [app/build.gradle.kts](app/build.gradle.kts).
- Dependency versions: [gradle/libs.versions.toml](gradle/libs.versions.toml).
- Static-analysis configurations: [app/config](app/config) (Checkstyle and PMD).
- Production code: [app/src/main/java/org/dersbian](app/src/main/java/org/dersbian).
- Runtime logging configuration: [app/src/main/resources/logback.xml](app/src/main/resources/logback.xml).
- Unit tests: [app/src/test](app/src/test), mirroring production packages where applicable.
- Manual Dersco source samples: [dr_files](dr_files). These are useful for CLI smoke tests; do not treat them as automated test fixtures unless a test explicitly uses them.

## Code Architecture

- `org.dersbian.App` is bootstrap only: it constructs the picocli command line, installs `CliExecutionExceptionHandler`, executes it, and maps the result to `System.exit`.
- `org.dersbian.cli` owns picocli commands, logging options, version reporting, input validation, and CLI exception-to-exit-code handling. New commands belong here and must be registered by `RootCommand`.
- `CheckCommand` calls `ICompilerService.checkSyntax(Path)`. `CompileCommand` creates `CompilationRequest` from `-o`/`--output`, `-O`/`--optimize`, `--emit-ir`, and `--diagnostics`, then calls `ICompilerService.compile`.
- `org.dersbian.compiler` defines the public service boundary (`ICompilerService`), request/options types, `CompilerException`, constants, and `DefaultCompilerService` orchestration.
- `org.dersbian.compiler.lexer` owns Unicode code-point traversal (`SourceCursor`, `CodePoints`), BOM stripping, line tracking setup, and tokenization (`Lexer`, `LexerResult`). Keep lexical rules and recovery here.
- `org.dersbian.compiler.lexer.token` owns `Token`, `TokenKind`, `SourceId`, `SourceLocation`, and `Span`. Numeric value records belong in `lexer.token.number` (`INumber`); numeric-literal parsing belongs in `lexer.token.parser.numeric` (`BaseNumberParser`, `NumericParser`, `SuffixParser`).
- `org.dersbian.compiler.syntax` owns the recursive-descent/Pratt parser and `ParseResult`; `org.dersbian.compiler.syntax.ast` defines the abstract syntax tree (`Expr`, `Stmt`, `Type`, `LiteralValue`, `BinaryOp`, `UnaryOp`, `UnaryOpSide`, `Parameter`, `ElseBranch`), AST printers (`AstPrinter`, `AstTreePrinter`), and node metrics (`NodeCounter`).
- `org.dersbian.compiler.error` owns the sealed `CompileError` hierarchy, error codes/phases (`ErrorCode`, `CompilerPhase`, `Severity`), and ANSI source-context rendering in `ErrorReporter`. `org.dersbian.compiler.location.LineTracker` resolves source lines for that rendering. Errors are data: do not print diagnostics from token, lexer, or error-model classes.
- `org.dersbian.util` contains general-purpose path and file-size formatting helpers only (`PathUtils`, `FileSizeInfo`, `FileSizeReport`, `SizeSystems`); compiler-domain behavior belongs under `org.dersbian.compiler`.

The implemented pipeline reads UTF-8 source, strips a leading BOM, tokenizes it via `Lexer`, parses it via `Parser`, prints the AST for valid input, and renders lexical or syntax diagnostics with `ErrorReporter`. `DefaultCompilerService.compile` delegates to that front end: it does not create the requested output, emit IR, apply optimization, perform semantic/type analysis, or generate code. Those later phases are not connected in the compiler pipeline. Do not present them as working end-to-end behavior or couple lexer changes to them prematurely.

## Working Rules

- Use Java 25 semantics and the configured Gradle toolchain; do not introduce preview features without an explicit build change.
- Prefer small, targeted edits that retain the package boundaries above and add tests in the corresponding package under `app/src/test/java`.
- Follow the existing conventions: immutable records and sealed hierarchies for small closed value models, `final` concrete classes when extension is not intended, and package-private JUnit test classes with descriptive test names.
- Tests use JUnit Jupiter and AssertJ. Use `@TempDir` for filesystem cases instead of repository-local temporary files.
- Preserve UTF-8 handling for source files and diagnostic output.
- Keep production code compatible with the enforced quality gates: Checkstyle, PMD, SpotBugs, Error Prone (`-Werror`), Spotless, and JaCoCo.
- The Java formatter is Spotless with Google Java Format in AOSP style (four-space indentation). Let Spotless format Java rather than formatting by hand.
- When Java or platform behavior matters, prefer official Java SE documentation, JEPs, and standard-library documentation as the source of truth.

## Validation

- Run focused tests with `./gradlew :app:test` (or `./gradlew :app:test --tests "*ClassNameTest*"`). On Windows, use `./gradlew.bat` if the shell does not resolve the script wrapper.
- Run `./gradlew :app:spotlessCheck` after editing Java; use `./gradlew :app:spotlessApply` to apply the configured formatter.
- Run `./gradlew :app:check` for all tests and quality gates: Checkstyle, PMD, SpotBugs, Spotless, and JaCoCo reporting.
- For CLI wiring, exercise `./gradlew :app:run --args="check ..\\dr_files\\simple_test.dr"` from the repository root, or use an absolute source path. A successful `compile` invocation currently performs the same lexer/parser front-end check, prints the AST, and does not create its configured output file.
- For distributable changes, verify `./gradlew :app:shadowJar`; the executable JAR is written under `app/build/libs/`.

## Ongoing Refinement

- Use `/chronicle improve` to capture recurring friction and keep these instructions aligned with the repository as it evolves.
