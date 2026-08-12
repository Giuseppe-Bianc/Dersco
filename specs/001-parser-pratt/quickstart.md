# Quickstart Validation Guide: Recursive Descent + Pratt Parser

**Feature**: `001-parser-pratt` | **Branch**: `001-parser-pratt` | **Date**: 2026-08-12

This guide describes how to validate that the parser is correctly wired and working
end-to-end once implementation is complete. It covers prerequisites, build commands,
test commands, and expected outcomes. For implementation details see `tasks.md`; for
the grammar contract see [`contracts/grammar.md`](contracts/grammar.md); for the
data model see [`data-model.md`](data-model.md).

---

## Prerequisites

- Java 25 SDK on `PATH` (or let Gradle's Foojay resolver provision it)
- Repository cloned and on branch `001-parser-pratt`
- Working directory: repository root (`C:\dev\intellij\Dersco`)
- On Windows, use `.\gradlew.bat` if the shell does not resolve `./gradlew`

---

## 1. Build

```powershell
.\gradlew.bat :app:classes
```

**Expected**: `BUILD SUCCESSFUL`. No compilation errors in any new parser class or the
modified `DefaultCompilerService`.

---

## 2. Code Formatting

```powershell
.\gradlew.bat :app:spotlessApply
.\gradlew.bat :app:spotlessCheck
```

**Expected**: `BUILD SUCCESSFUL` with no formatting violations. All new `.java` files
comply with Google Java Format AOSP style (4-space indent).

---

## 3. Focused Parser Unit Tests

```powershell
# Run all parser tests
.\gradlew.bat :app:test --tests "org.dersbian.compiler.syntax.*"

# Run only expression-level tests (precedence + associativity)
.\gradlew.bat :app:test --tests "*ExpressionParserTest*"

# Run only statement-level tests
.\gradlew.bat :app:test --tests "*StatementParserTest*"

# Run error recovery tests
.\gradlew.bat :app:test --tests "*ParserErrorRecoveryTest*"
```

**Expected**: All tests pass. No failures, no unexpected skips.

Key scenarios that MUST pass:

- `1 + 2 * 3` → `Binary(Literal(1), ADD, Binary(Literal(2), MULTIPLY, Literal(3)))`
  (multiplicative binds tighter than additive — SC-002)
- `a - b - c` → `Binary(Binary(Variable(a), SUBTRACT, Variable(b)), SUBTRACT, Variable(c))`
  (left-associativity — SC-002)
- `a = b = c` → `Assign(Variable(a), Assign(Variable(b), Variable(c)))`
  (right-associativity — SC-003)
- `a += 5` → `Assign(Variable(a), Binary(Variable(a), ADD, Literal(5)))`
  (compound assignment desugaring — FR-004b)
- `!-a` → `Unary(NOT, PREFIX, Unary(NEGATE, PREFIX, Variable(a)))` (chained prefix)
- `a++` → `Unary(INCREMENT, POSTFIX, Variable(a))` (postfix)
- `a && b || c` → `Binary(Binary(Variable(a), AND, Variable(b)), OR, Variable(c))`
  (`&&` tighter than `||` — SC-002)

---

## 4. Full Quality Gate

```powershell
.\gradlew.bat :app:check
```

**Expected**: `BUILD SUCCESSFUL`. All gates pass:

- Checkstyle — no rule violations in new parser classes
- PMD — no PMD violations
- SpotBugs — no bug findings
- Error Prone — no errors with `-Werror`
- Spotless — formatting clean
- JUnit — all tests pass
- JaCoCo — branch coverage ≥ 80% on `org.dersbian.compiler.syntax.*` parser classes (SC-004)

---

## 5. CLI Smoke Tests (end-to-end)

Run the compiler CLI against the existing `dr_files/` samples. The parser is now wired into
`DefaultCompilerService.checkSyntax`, so this exercises the full lex → parse pipeline.

```powershell
# Should succeed (valid syntax):
.\gradlew.bat :app:run --args="check dr_files/simple_test.dr"
.\gradlew.bat :app:run --args="check dr_files/large_toy_program.dr"
.\gradlew.bat :app:run --args="check dr_files/break_continue_loops.dr"
.\gradlew.bat :app:run --args="check dr_files/test_for.dr"
.\gradlew.bat :app:run --args="check dr_files/float_test.dr"
```

**Expected outcome for valid files**: Exit code 0. No error output. Log may show token
debug output at TRACE level; no parse errors are printed.

```powershell
# These files have deliberate semantic errors but should be syntactically valid:
.\gradlew.bat :app:run --args="check dr_files/binary_type_mismatch.dr"
.\gradlew.bat :app:run --args="check dr_files/return_type_mismatch.dr"
```

**Expected outcome**: Exit code 0 (no parse errors; semantic errors are not yet detected
because the semantic phase is not implemented). No exception is thrown.

**SC-001 verification**: All 29 `.dr` files in `dr_files/` produce a non-empty `List<Stmt>`
without a `CompilerException` caused by a parse error.

**SC-005 verification**: No file causes an unhandled exception. Any syntax-invalid input
surfaces `CompileError.SyntaxError` entries, not a stack trace.

---

## 6. Performance Spot-Check (SC-007)

```powershell
.\gradlew.bat :app:run --args="check dr_files/large_toy_program.dr"
```

**Expected**: Completes in well under 100 ms (JVM startup dominates; the actual parse
of a 200-line file is in the low milliseconds). If the run time exceeds 5 seconds,
investigate; the JVM startup alone should keep wall-clock time below 3 s.

Alternatively, verify with a dedicated JUnit benchmark in `ParserTest`:

```java
@Test void parseLargeToyProgramUnder100ms() {
    long start = System.nanoTime();
    ParseResult result = parser.parse(); // pre-loaded token list
    long elapsed = System.nanoTime() - start;
    assertThat(elapsed).isLessThan(100_000_000L); // 100 ms
}
```

---

## 7. Shadow JAR Smoke Test

```powershell
.\gradlew.bat :app:shadowJar
java -jar app/build/libs/Dersco-0.1.0.jar check dr_files/simple_test.dr
```

**Expected**: Exit code 0. Output identical to the Gradle `run` invocation above.
