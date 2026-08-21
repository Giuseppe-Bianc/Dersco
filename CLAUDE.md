# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build, test, and quality gates

The build, test, and static-analysis commands are documented in [AGENTS.md](AGENTS.md). Read it first; the summary below does not replace it.

- Full quality-gate run: `./gradlew check` (or `./gradlew :app:check`; chains `checkstyleMain`, `pmdMain`, `spotbugsMain`, `spotlessCheck`, `test`, `jacocoTestReport`).
- Tests only: `./gradlew test` (or `./gradlew :app:test`).
- Single test class: `./gradlew test --tests "*NumericParsersTest*"` or `./gradlew test --tests "*BaseParsersTest*"`. Wildcard around the simple class name is required.
- Format only: `./gradlew spotlessApply`. Never hand-format; Spotless with Google Java Format AOSP (4 spaces) is the source of truth.
- Run the compiler CLI: `./gradlew :app:run --args="check dr_files/simple_test.dr"` or build the fat jar with `./gradlew shadowJar` then `java -jar app/build/libs/Dersco-0.1.0-all.jar check path/to/source.dr`.
- The build is intentionally strict: Checkstyle `maxWarnings=0`, Error Prone `-Werror`, SpotBugs `MAX` effort / `LOW` confidence, PMD ruleset. The build fails on any violation, so a passing build already implies style + lint + tests are green.

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
// Red: this test references BaseNumberParser.parseQuaternary, which does not exist yet.
@Test
void parseQuaternarySimpleValue() {
    INumber result = BaseNumberParser.parseQuaternary("#q123");
    assertThat(result).isInstanceOf(INumber.Integer.class);
    assertThat(((INumber.Integer) result).value()).isEqualTo(27L); // 1*16 + 2*4 + 3
}

@Test
void parseQuaternaryWithUnsignedSuffix() {
    INumber result = BaseNumberParser.parseQuaternary("#q123u");
    assertThat(result).isInstanceOf(INumber.UnsignedInteger.class);
    assertThat(((INumber.UnsignedInteger) result).value()).isEqualTo(27L);
}

@Test
void parseQuaternaryInvalidDigitReturnsNull() {
    INumber result = BaseNumberParser.parseQuaternary("#q149");
    assertThat(result).isNull();
}
```

Running `./gradlew test --tests "*BaseParsersTest*"` at this point **must** fail because `BaseNumberParser.parseQuaternary` does not exist. That failure is the Red signal -- proceed to Green.

### Phase 2 — Green: make the tests pass with the minimum code

**Definition.** Write the smallest amount of production code that turns every Red test green. Do not generalize, do not optimize, do not refactor. Hardcoded values are acceptable if they satisfy the test; the Refactor phase addresses design.

**What to do concretely:**

1. Implement only the production code required by the failing tests. Follow the existing patterns exactly (e.g., `parseBaseNumber` in `BaseNumberParser`, sealed-interface records for `TokenKind` variants).
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
// Green: just enough to pass the tests above.
public static INumber parseQuaternary(final String slice) {
    return parseBaseNumber(4, slice);
}
```

### Phase 3 — Refactor: improve design under a green bar

**Definition.** With all tests passing, restructure the production code and the test code for clarity, duplication removal, and consistency with the codebase conventions. **No new behavior** is added; the tests must stay green throughout.

**What to do concretely:**

1. Look for duplication against existing parsers (`parseBinary`, `parseOctal`, `parseHex`). Extract shared constants or helpers if needed.
2. Align naming, parameter order, Javadoc, and `@SuppressWarnings` annotations with the rest of the codebase.
3. If the `TokenKind` hierarchy was touched, verify `isType()` and `toString()` follow the existing sealed-interface pattern.
4. Run the full gate after every structural change:

```bash
./gradlew check
```

5. Commit only when `check` is green. The bar must never go red during Refactor.

### Iteration

After Refactor, return to Red with the next behavioral increment. Typical follow-up Red tests for the example above:

- Empty body after prefix (`"#q"` -> returns `null`).
- Leading zeros (`"#q0012"`).
- Overflow beyond `Long.MAX_VALUE`.
- Integration with lexer radix scanning (`Lexer.scanRadixLiteral`).

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

- `App.java` -- picocli bootstrap only. Builds the `CommandLine`, configures `CliExecutionExceptionHandler`, enables case-insensitive enum parsing and auto-width usage help, then maps the result to `System.exit`.

- `cli/` -- picocli subcommands and CLI helpers: `RootCommand` (git-style root command registering `CompileCommand`, `CheckCommand`, `HelpCommand`), `CompileCommand` (options `-o`/`--output`, `-O`/`--optimize`, `--emit-ir`, `--diagnostics`), `CheckCommand` (syntax checking only), `LoggingMixin`, `ManifestVersionProvider`, `CliExecutionExceptionHandler`. Subcommands instantiate `DefaultCompilerService` by default; package-private constructors accept an `ICompilerService` for testing.
- `compiler/` -- compiler service surface and orchestration: `ICompilerService` defines `checkSyntax(Path)` and `compile(CompilationRequest)`, `CompilationRequest`, `OptimizationLevel`, `CompilerException`, `Constants`, and `DefaultCompilerService`.
- `compiler/lexer/` -- lexical analysis:
    - `Lexer.java` -- full tokenizer reading source code, handling comments (single/multi-line), radix numbers, numeric literals with suffixes, string/char literals with escape sequences, operators, delimiters, and Unicode identifiers, returning `LexerResult(tokens, errors)`.
    - `SourceCursor.java` -- Unicode code-point traversal tracking 1-based line/column numbers, UTF-16 offset, index, UTF-8 offset, and code-point offset.
    - `CodePoints.java` -- Unicode character classification, BOM stripping, identifier start/part validation, and keyword/identifier kind resolution.
    - `LexerResult.java` -- container record for produced `List<Token>` and collected `List<CompileError>`.
- `compiler/lexer/token/` -- the token model:
    - `Token` -- immutable record `(SourceId, TokenKind, Span)` with factory methods (`create`, `eof`, `point`) and a `BY_POSITION` comparator.
    - `TokenKind` -- sealed interface. Unit variants (operators, keywords, delimiters, type keywords, `EOF`, comments) live in nested enums within the sealed `TokenKind.Simple` interface (`Operator`, `Keyword`, `TypeKeyword`, `Delimiter`, `Special`). Payload-bearing variants are top-level records: `Numeric(INumber)`, `Binary(INumber)`, `Octal(INumber)`, `Hexadecimal(INumber)`, `StringLiteral(String)`, `CharLiteral(String)`, `IdentifierAscii(String)`, `IdentifierUnicode(String)`, `KeywordBool(boolean)`. Helper `isType()` returns `true` for primitive type keywords.
    - `SourceLocation` -- 1-based `(line, column)`, with `offset` (UTF-16), `index`, `utf8Offset`, and `codePointOffset`. Validated in compact constructor.
    - `Span` -- `(start, end)` validated bounds (`end.offset() >= start.offset()`), with `extractFrom(CharSequence)` and `point(location)` for zero-length spans.
    - `SourceId` -- sealed interface with four variants: `FilePath(Path)`, `VirtualResource(URI)`, `InMemoryModule(String)`, `Generated(String)`. `identifier()` provides the stable id used in diagnostics.
- `compiler/lexer/token/number/` -- `INumber` sealed interface representing typed numeric values:
    - Signed integer records: `I8(byte)`, `I16(short)`, `I32(int)`, `Integer(long)`.
    - Unsigned integer records: `U8(short)`, `U16(int)`, `U32(long)`, `UnsignedInteger(long)`.
    - Floating-point records: `Float32(float)`, `Float64(double)`.
    - Scientific notation records: `Scientific32(float base, int exponent)`, `Scientific64(double base, int exponent)`.
- `compiler/lexer/token/parser/numeric/` -- static parsers for numeric literals:
    - `BaseNumberParser` -- static methods `parseBinary`, `parseOctal`, `parseHex`, and `parseBaseNumber` for non-decimal literals with `#b`, `#o`, `#x` prefixes and optional `u`/`U` suffix, returning `INumber.Integer`, `INumber.UnsignedInteger`, or `null` on failure.
    - `NumericParser` -- static method `parseNumber(String)` for decimal integers, floats, scientific notations, and type suffixes.
    - `SuffixParser` -- helper for scanning and resolving type suffixes.
- `compiler/syntax/ast/` -- abstract syntax tree model and tools:
    - `Expr` -- sealed interface: `Binary`, `Unary`, `Grouping`, `Literal`, `ArrayLiteral`, `Variable`, `Assign`, `Call`, `ArrayAccess`.
    - `Stmt` -- sealed interface: `Expression`, `VarDeclaration` (with `VarBinding`), `Function`, `If`, `While`, `For`, `Block`, `Return`, `Break`, `Continue`, `MainFunction`.
    - `Type` -- sealed interface: `I8`, `I16`, `I32`, `I64`, `U8`, `U16`, `U32`, `U64`, `F32`, `F64`, `Char`, `StringT`, `Bool`, `VoidT`, `NullPtr`, `Custom`, `Array`, `Vector`.
    - `LiteralValue` -- sealed interface: `Numeric(INumber)`, `StringLit(String)`, `CharLit(String)`, `Bool(boolean)`, `NullPtr()`.
    - `BinaryOp`, `UnaryOp`, `UnaryOpSide`, `Parameter`, `ElseBranch`.
    - `AstPrinter` -- pattern-matching pretty printer using switch expressions for AST visualization.
    - `NodeCounter` -- recursive AST node counter utility.
- `compiler/error/` -- diagnostic model:
    - `CompileError` -- sealed hierarchy for compiler diagnostics: `LexerError`, `SyntaxError`, `TypeError`, `IrGeneratorError`, `AsmGeneratorError`. Records carry `(errorCode, errorMessage, errorSpan, errorHelp)` except `AsmGeneratorError` (no span/help).
    - `ErrorCode` -- standardized error codes enum categorized by phase: `E0001`..`E0010` (LEXER), `E1001`..`E1015` (PARSER), `E2001`..`E2032` (SEMANTIC), `E3001`..`E3008` (IR_GENERATION), `E4001`..`E4005` (CODE_GENERATION), `E5001`..`E5005` (SYSTEM).
    - `CompilerPhase` -- enum identifying pipeline phase (`LEXER`, `PARSER`, `SEMANTIC`, `IR_GENERATION`, `CODE_GENERATION`, `SYSTEM`).
    - `Severity` -- diagnostic level (`ERROR`, `WARNING`, `INFO`).
    - `ErrorReporter` -- ANSI source-context diagnostic renderer with caret underlines and multi-line formatting.
    - `CompilerErorFormater` -- error text formatting utility.
- `compiler/location/LineTracker` -- splits source text into 1-based-addressable lines for diagnostic rendering.
- `util/` -- general utility helpers: `PathUtils`, `FileSizeInfo`, `FileSizeReport`, `FormattedSize`, `FormattedSizePair`, `SizeSystem`, `SizeSystems`.

## Adding a new numeric token type

When introducing a new numeric token representation, touch exactly these places:

1. Add a `TokenKind` record carrying `INumber` (mirroring `Binary` / `Octal` / `Hexadecimal`) and a `toString()` that prints `"<kind> '<value>'"`.
2. Add a static parser method in `BaseNumberParser` (or `NumericParser` if decimal) returning the appropriate `INumber` variant or `null` on failure.
3. Mirror the change in test packages (`BaseParsersTest` for non-decimal, `NumericParsersTest` for decimal).
4. Update `Lexer.scanRadixLiteral` (or decimal scanner) to dispatch to the new parser.
5. If it changes the kind-test surface on `TokenKind`, update `isType()` (and any new helper) in lockstep.

Numeric literal records (`Numeric`, `Binary`, `Octal`, `Hexadecimal`) all carry the `INumber` interface payload; do not introduce separate number base interfaces.

## Conventions

- Final classes, private no-arg constructors on static-utility classes, Lombok `@Slf4j` or SLF4J loggers.
- Tests are package-private (`class XxxTest`, not `public`) with descriptive method names (`parseIntegerWithMultiCharacterSuffix`, not `test1`).
- Error messages in validation paths are constants on the record or interface (`MSG_CODE`, `MSG_MESSAGE`, ...).
- Suppress static-analysis rules locally with `@SuppressWarnings` annotations on the specific method/field; do not weaken the global config.
- Preserve existing source comment styles (mixed English and Italian comments exist in the codebase).

## Dev loop: probing the compiler

`App.java` is wired to picocli. The CLI can be exercised via Gradle or the fat jar produced by `./gradlew :app:shadowJar`:

```bash
./gradlew :app:run --args="check dr_files/simple_test.dr"
./gradlew :app:run --args="compile dr_files/simple_test.dr"
```

Or using the generated fat jar under `app/build/libs/`:

```bash
java -jar app/build/libs/Dersco-0.1.0-all.jar check dr_files/simple_test.dr
java -jar app/build/libs/Dersco-0.1.0-all.jar compile dr_files/simple_test.dr
```

`check` runs syntax check (currently UTF-8 source reading, lexing, and error reporting); `compile` runs the compiler service. Exit codes are 0 on success, 1 on a `CompilerException`. The AST and error models are defined, while parser wiring and code generation into the CLI execution remain in development.
