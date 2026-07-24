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

## Red-Green-Refactor cycle (TDD)

Every code change in this repository **must** follow the Red-Green-Refactor discipline. Claude Code must execute each phase explicitly and sequentially; do not skip or collapse phases. The cycle is the primary mechanism for ensuring correctness before, during, and after implementation.

### Phase 1 — Red: write a failing test first

**Definition.** Before writing any production code, create one or more test methods that exercise the behavior you are about to implement. Run the test suite and **confirm the new tests fail** (compilation error or assertion failure). A test that passes immediately is either redundant or testing the wrong thing -- investigate before proceeding.

**What to do concretely:**

1. Identify the smallest behavior increment (one parser branch, one new `TokenKind` variant, one validation rule).
2. Add the test in the correct package-private test class. Follow the existing naming convention (`parseHexWithUnsignedSuffix`, `createSpanWithInvertedOffsetsThrows` -- descriptive, no `test` prefix).
3. Reference production types and methods that **do not exist yet**. The code will not compile; that is the expected Red state.
4. Run the test to record the failure:

```bash
./gradlew test --tests "*BaseParsersTest*"
```

5. Verify the output shows a **compilation error** or **assertion failure** for the new test only. Existing tests must remain green.

**Example -- adding a `parseQuaternary` parser (hypothetical):**

```java
// Red: this test references BaseParsers.parseQuaternary, which does not exist yet.
@Test
void parseQuaternarySimpleValue() {
    INumber result = BaseParsers.parseQuaternary("#q123");
    assertThat(result).isInstanceOf(INumber.IntegerValue.class);
    assertThat(((INumber.IntegerValue) result).value()).isEqualTo(27L); // 1*16 + 2*4 + 3
    assertThat(((INumber.IntegerValue) result).suffix()).isEmpty();
}

@Test
void parseQuaternaryWithUnsignedSuffix() {
    INumber result = BaseParsers.parseQuaternary("#q123u");
    assertThat(((INumber.IntegerValue) result).value()).isEqualTo(27L);
    assertThat(((INumber.IntegerValue) result).suffix()).isEqualTo("u");
}

@Test
void parseQuaternaryInvalidDigitThrows() {
    assertThatThrownBy(() -> BaseParsers.parseQuaternary("#q149"))
            .isInstanceOf(NumberFormatException.class);
}
```

Running `./gradlew test --tests "*BaseParsersTest*"` at this point **must** fail because `BaseParsers.parseQuaternary` does not exist. That failure is the Red signal -- proceed to Green.

### Phase 2 — Green: make the tests pass with the minimum code

**Definition.** Write the smallest amount of production code that turns every Red test green. Do not generalize, do not optimize, do not refactor. Hardcoded values are acceptable if they satisfy the test; the Refactor phase addresses design.

**What to do concretely:**

1. Implement only the production code required by the failing tests. Follow the existing patterns exactly (e.g., `parseWithRadix` for non-decimal parsers, sealed-interface records for `TokenKind` variants).
2. Run the targeted tests:

```bash
./gradlew test --tests "*BaseParsersTest*"
```

3. Once those are green, run the full quality gate to catch style / lint regressions:

```bash
./gradlew check
```

4. If `check` fails on formatting, run `./gradlew spotlessApply` and re-run `check`. If it fails on Checkstyle / PMD / SpotBugs / Error Prone, fix the violation in the production code -- do **not** weaken the global config.
5. All tests green + `check` passes = Green phase complete.

**Example -- minimum implementation:**

```java
// Green: just enough to pass the three tests above.
public static INumber parseQuaternary(String raw) {
    // follows the existing parseWithRadix shape
    String body = raw.substring(2); // strip "#q"
    String suffix = "";
    if (body.endsWith("u") || body.endsWith("U")) {
        suffix = "u";
        body = body.substring(0, body.length() - 1);
    }
    long value = Long.parseLong(body, 4);
    return new INumber.IntegerValue(value, suffix);
}
```

This is intentionally unsophisticated. It does not handle edge cases beyond what the three tests demand. That is correct -- uncovered edge cases belong to the next Red phase.

### Phase 3 — Refactor: improve design under a green bar

**Definition.** With all tests passing, restructure the production code and the test code for clarity, duplication removal, and consistency with the codebase conventions. **No new behavior** is added; the tests must stay green throughout.

**What to do concretely:**

1. Look for duplication against existing parsers (`parseBinary`, `parseOctal`, `parseHex`). Extract shared logic into `parseWithRadix` if a clear pattern emerges.
2. Align naming, parameter order, Javadoc, and `@SuppressWarnings` annotations with the rest of the codebase.
3. If the `TokenKind` hierarchy was touched, verify `isType()` and `toString()` follow the existing sealed-interface pattern.
4. Run the full gate after every structural change:

```bash
./gradlew check
```

5. Commit only when `check` is green. The bar must never go red during Refactor.

**Example -- refactor toward `parseWithRadix`:**

```java
// Refactor: quaternary now delegates to the shared helper,
// just like parseBinary, parseOctal, parseHex.
public static INumber parseQuaternary(String raw) {
    return parseWithRadix(raw, "#q", 4);
}
```

If `parseWithRadix` did not exist before, this refactor creates it and migrates all four radix parsers to use it. The existing tests for binary/octal/hex **must** stay green -- they are the safety net that proves the extraction is correct.

### Iteration

After Refactor, return to Red with the next behavioral increment. Typical follow-up Red tests for the example above:

- Empty body after prefix (`"#q"` → should throw).
- Leading zeros (`"#q0012"`).
- Overflow beyond `Long.MAX_VALUE`.
- Integration with the `TokenKind.Quaternary` record (once defined).

Each increment is its own Red-Green-Refactor micro-cycle. Keep cycles small -- ideally one to three test methods per Red phase.

### Mandatory checklist for Claude Code

Before marking any task as complete, verify every item:

| # | Gate | Command |
|---|------|---------|
| 1 | New tests were written **before** the production code | (review git diff order) |
| 2 | Tests failed in Red | `./gradlew test --tests "*XxxTest*"` showed failures |
| 3 | Tests pass in Green | `./gradlew test --tests "*XxxTest*"` all green |
| 4 | Full quality gate passes | `./gradlew check` exit 0 |
| 5 | No global config was weakened | Checkstyle / PMD / SpotBugs configs unchanged |
| 6 | Formatting is Spotless-clean | `./gradlew spotlessCheck` exit 0 |
| 7 | Refactor did not add behavior | No new assertions appeared during Refactor |

If any gate is red, loop back to the appropriate phase. Do **not** commit with a broken gate.

### Anti-patterns to avoid

- **Writing production code before its test exists.** This inverts the cycle and removes the Red safety signal.
- **Writing a test that passes immediately on the first run.** Either the behavior already exists (test is redundant) or the assertion is wrong (test is vacuous). Investigate.
- **Large Red phases.** If more than three or four tests are red simultaneously, the increment is too big. Split it.
- **Refactoring while tests are red.** Refactor only happens under a green bar. If a test is failing, you are still in Green phase.
- **Skipping `./gradlew check` after Refactor.** Style and lint regressions introduced during refactoring are caught here. Never skip it.
- **Suppressing a static-analysis rule globally to make a refactoring pass.** Use a local `@SuppressWarnings` on the specific method or field instead.

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
