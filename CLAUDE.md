# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build, test, and quality gates

The build, test, and static-analysis commands are documented in [AGENTS.md](AGENTS.md). Read it first; the summary below does not replace it.

- Full quality-gate run: `./gradlew check` (chains `checkstyleMain`, `pmdMain`, `spotbugsMain`, `spotlessCheck`, `test`).
- Tests only: `./gradlew test`.
- Single test class: `./gradlew test --tests "*NumericParsersTest*"`. Wildcard around the simple class name is required.
- Format only: `./gradlew spotlessApply`. Never hand-format; Spotless with Google Java Format AOSP (4 spaces) is the source of truth.
- Run the compiler: `./gradlew run --args="compile path/to/source.dr"` or build the fat jar with `./gradlew shadowJar` then `java -jar app/build/libs/Dersco-0.1.0.jar ...`.
- The build is intentionally strict: Checkstyle `maxWarnings=0`, Error Prone `-Werror`, SpotBugs `MAX` effort / `LOW` confidence. The build fails on any violation, so a passing build already implies style + lint + tests are green.

## Project shape

Dersco is a Java 25 compiler for the Dersco language. Top-level layout under `app/src/main/java/org/dersbian`:

- `App.java` -- picocli bootstrap only. Builds the `CommandLine`, wires ANSI (Jansi), and exits with the picocli return code. No domain logic.
- `cli/` -- picocli subcommands: `RootCommand` (git-style, delegates to subcommands), `CompileCommand`, `CheckCommand`, `LoggingMixin`, `ManifestVersionProvider`, `CliExecutionExceptionHandler`. Subcommands construct `DefaultCompilerService` by default; a package-private constructor accepts an `ICompilerService` for tests.
- `compiler/` -- the compiler service surface. `ICompilerService` defines `checkSyntax(Path)` and `compile(CompilationRequest)`. `DefaultCompilerService` is currently a scaffold (`checkSyntax` only validates the file exists; `compile` is `TODO`-wired). Treat it as a placeholder -- do not add new features here until the lexer/parser pipeline is implemented.
- `compiler/lexer/` -- lexer package. `Lexer.java` is a stub (empty `@NoArgsConstructor` class). The real value is in the subpackages below; the lexer class itself will be wired up later.
- `compiler/lexer/token/` -- the token model. The interesting types here are:
  - `Token` -- immutable record `(SourceId, TokenKind, Span)` with factory methods (`create`, `eof`, `point`) and a `BY_POSITION` comparator.
  - `TokenKind` -- sealed interface with two layers. Unit variants (operators, keywords, brackets, type keywords, `EOF`, comments) live in the nested `TokenKind.Simple` enum. Payload-bearing variants (literals) are top-level records: `Numeric(INumber)`, `Binary(INumber)`, `Octal(INumber)`, `Hexadecimal(INumber)`, `StringLiteral(String)`, `CharLiteral(String)`, `IdentifierAscii(String)`, `IdentifierUnicode(String)`, `KeywordBool(boolean)`. The numeric literal variants all carry the same `INumber` payload type.
  - `SourceLocation` -- 1-based `(line, column)`, with `offset` (UTF-16, matches `String#length`), `index`, and optional `utf8Offset` / `codePointOffset` (sentinel `UNKNOWN = -1L`). Validated in the compact constructor.
  - `Span` -- `(start, end)`; validated so `end.offset() >= start.offset()`. `extractFrom(CharSequence)` slices source text by offset. `point(location)` for zero-length spans (e.g. EOF).
  - `SourceId` -- sealed interface with four variants: `FilePath(Path)`, `VirtualResource(uri)`, `InMemoryModule(name)`, `Generated(description)`. `identifier()` is the stable id used in diagnostics.
- `compiler/lexer/token/number/INumber` -- sealed interface for numeric literal values: `IntegerValue(long, String suffix)` and `FloatingValue(double, String suffix)`. Suffix is the raw type-annotation text (`u`, `i32`, `f`, ...).
- `compiler/lexer/token/parser/numeric/` -- static parsers for numeric literals.
  - `NumericParsers.parseNumber(String)` -- decimal integers and floats, with optional suffixes (`u`, `f`, `d`, `i8`..`i64`, `u8`..`u64`). Decides int vs float by `.`, `e`/`E`, or a floating suffix.
  - `BaseParsers.parseBinary / parseOctal / parseHex` -- non-decimal literals with the `#b` / `#o` / `#x` prefixes. Strip a single `u`/`U` suffix and parse with `Long.parseLong(body, radix)`.
- `compiler/error/` -- diagnostic surface. `CompileError` is a sealed hierarchy keyed by compiler phase: `LexerError`, `SyntaxError`, `TypeError`, `IrGeneratorError`, `AsmGeneratorError`, `IoError`. Each variant carries `(code, message, span, help)` except `AsmGeneratorError` (no span) and `IoError` (wraps an `IOException`). `ErrorCode` is the standardized code enum; `Severity` classifies it; `CompilerPhase` names the pipeline stage. `ErrorReporter` renders errors with ANSI color and a source-context underline (caret run on the same line, `...` continuation across lines), backed by a `LineTracker` from `compiler.location`.
- `compiler/location/LineTracker` -- splits source text into 1-based-addressable lines for the reporter.
- `util/` -- unrelated helpers (`FileSizeInfo`, `FileSizeReport`, `FormattedSize`, `FormattedSizePair`, `SizeSystem`, `SizeSystems`). They have no dependency on the compiler packages.

## Adding a new numeric token type

This is the recurring 5-file pattern visible in the recent commit history. Touch exactly these places; do not invent new abstractions:

1. Add a `TokenKind` record carrying `INumber` (mirror `Binary` / `Octal` / `Hexadecimal`) and a `toString()` that prints `"<kind> '<value>'"`.
2. Add a static parser to `BaseParsers` (or `NumericParsers` if it is decimal) following the existing `parseWithRadix` shape: strip prefix, optionally strip a single `u`/`U` suffix, `Long.parseLong`, return `new INumber.IntegerValue(value, suffix)`.
3. Mirror the change in test packages -- the test class lives next to the parser (`compiler.lexer.token.parser.numeric.BaseParsersTest` for non-decimal, `NumericParsersTest` for decimal).
4. If the kind affects dispatch in the lexer (once the lexer is implemented), add a `Simple` enum constant or extend the sealed hierarchy at the same level as the existing variants.
5. If it changes the kind-test surface on `TokenKind`, update `isType()` (and any new helper) in lockstep.

Numeric literal records (`Numeric`, `Binary`, `Octal`, `Hexadecimal`) all carry the same `INumber` payload; do not introduce per-kind number subtypes.

## Conventions

- Final classes, private no-arg constructors on static-utility classes (`@NoArgsConstructor(access = AccessLevel.PRIVATE)`), Lombok `@Slf4j` for loggers.
- Tests are package-private (`class XxxTest`, not `public`) with descriptive method names (`parseIntegerWithMultiCharacterSuffix`, not `test1`).
- Error messages in validation paths are constants on the record or interface (`MSG_CODE`, `MSG_MESSAGE`, ...).
- Suppress the specific PMD / SpotBugs / Checkstyle rule locally with a `@SuppressWarnings` annotation; do not weaken the global config to make a file pass.
- Source comments in this repo mix English and Italian; preserve existing style rather than normalizing.

## Dev loop: probing the compiler

`App.java` is wired to picocli. After `./gradlew shadowJar`, the fat jar at `app/build/libs/Dersco-0.1.0.jar` is the fastest way to run a snippet through the CLI:

```
java -jar app/build/libs/Dersco-0.1.0.jar compile path/to/source.dr
java -jar app/build/libs/Dersco-0.1.0.jar check path/to/source.dr
```

`check` runs the syntax stage only; `compile` runs the full pipeline. Exit codes are 0 on success, 1 on a `CompilerException`. The compiler is currently a scaffold -- meaningful feedback only comes from the diagnostic surface, not from generated output.
