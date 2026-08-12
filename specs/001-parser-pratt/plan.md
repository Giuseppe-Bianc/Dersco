# Implementation Plan: Recursive Descent + Pratt Parser

**Branch**: `001-parser-pratt` | **Date**: 2026-08-12 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-parser-pratt/spec.md`

## Summary

Implement a parser in `org.dersbian.compiler.syntax` that consumes a `List<Token>` produced by
the existing `Lexer` and returns a `ParseResult` carrying a `List<Stmt>` (the top-level program
structure) and a `List<CompileError.SyntaxError>` (all recoverable parse errors). The parser
combines Recursive Descent for statement-level constructs with Pratt Parsing for expressions,
using a numeric binding-power table to drive precedence and associativity. All nine existing
`Expr` variants and eleven `Stmt` variants defined in `org.dersbian.compiler.syntax.ast` are
used without modification. `DefaultCompilerService.checkSyntax` is updated to call the new parser
after lexing and surface parse errors through `ErrorReporter`.

---

## Technical Context

**Language/Version**: Java 25 (stable features only — sealed interfaces, records, pattern matching
for switch; no preview features)

**Primary Dependencies**:
- `org.dersbian.compiler.lexer.token.Token` / `TokenKind` / `Span` — token model already defined
- `org.dersbian.compiler.syntax.ast.*` — `Expr`, `Stmt`, `BinaryOp`, `UnaryOp`, `UnaryOpSide`,
  `Type`, `LiteralValue`, `Parameter`, `ElseBranch` — all already defined
- `org.dersbian.compiler.error.CompileError.SyntaxError` — error type already defined
- `org.dersbian.compiler.error.ErrorReporter` — diagnostic rendering already defined
- JUnit Jupiter 9.1.3 + AssertJ — test stack already configured
- Gradle 9.7.0 wrapper — build already configured

**Storage**: N/A (in-memory token list → AST)

**Testing**: JUnit Jupiter 9.1.3 (via `./gradlew :app:test`); JaCoCo for branch coverage gate

**Target Platform**: JVM / Java 25

**Project Type**: Compiler (parser phase)

**Performance Goals**: Parse a 200-line `.dr` file in < 100 ms on a development machine (SC-007)

**Constraints**:
- Zero preview features (Constitution I)
- All quality gates must pass: Checkstyle, PMD, SpotBugs, Error Prone `-Werror`, Spotless
  (Google Java Format AOSP, 4-space indent), JaCoCo ≥ 80% branch coverage on parser classes
- No logging from any parser class (FR-012); all output via `ParseResult`
- No new `Expr` or `Stmt` variants (FR-005, FR-006); compound assignment as `Expr.Assign`
  wrapping `Expr.Binary` (FR-004b)
- `parse()` MUST NOT throw for recoverable syntax errors (FR-001)

**Scale/Scope**: Single-module Gradle project; parser handles all `.dr` files in `dr_files/`

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-checked after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| **I. Java 25 / Gradle 9.7.0 Baseline** | ✅ PASS | Implementation uses only Java 25 stable features; no preview APIs introduced; Gradle wrapper unchanged |
| **II. Explicit Compiler Boundaries** | ✅ PASS | Parser lives in `org.dersbian.compiler.syntax`; no lexer, diagnostic, or utility code is modified except `DefaultCompilerService` wiring; `ErrorReporter` is called from the service layer, never from inside parser classes |
| **III. JUnit Conformance** | ✅ PASS | Tests use JUnit Jupiter 9.1.3 annotations only; no deprecated or version-incompatible APIs |
| **IV. Complete Behavioral Coverage** | ✅ PASS | Spec mandates unit tests for every precedence level, every right-associative operator, every statement variant, every error-recovery path, and every edge case (EOF mid-expression, empty lists, etc.) |
| **V. Red-Green-Refactor (NON-NEGOTIABLE)** | ✅ PASS | TDD cycle applies: write failing test for each FR/SC item, implement minimal code to pass, then refactor; no deviation justified |

*Post-Phase 1 re-check*: Design artifacts introduce no new violations. `ParseResult` is a pure
immutable record; `TokenCursor` is package-private with no side-effects on external state;
`BindingPower` is a plain enum/record with no I/O. All class boundaries map 1-to-1 to FR-010.

---

## Project Structure

### Documentation (this feature)

```text
specs/001-parser-pratt/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   └── grammar.md
└── tasks.md             # Phase 2 output (/speckit.tasks command — NOT created here)
```

### Source Code (repository root)

```text
app/src/main/java/org/dersbian/
├── compiler/
│   ├── DefaultCompilerService.java          # modified: wire parser after lexer
│   └── syntax/
│       ├── ast/                             # unchanged — all AST types already exist
│       │   ├── Expr.java
│       │   ├── Stmt.java
│       │   ├── BinaryOp.java
│       │   ├── UnaryOp.java
│       │   ├── UnaryOpSide.java
│       │   ├── Type.java
│       │   ├── LiteralValue.java
│       │   ├── Parameter.java
│       │   └── ElseBranch.java
│       ├── ParseResult.java                 # new: immutable record (statements + errors)
│       ├── TokenCursor.java                 # new: stateful view over List<Token>
│       ├── BindingPower.java                # new: left/right bp per operator
│       ├── Parser.java                      # new: top-level entry point, owns parse()
│       ├── StatementParser.java             # new: recursive-descent statement rules
│       └── ExpressionParser.java           # new: Pratt expression parser

app/src/test/java/org/dersbian/
└── compiler/
    └── syntax/
        ├── ParseResultTest.java
        ├── TokenCursorTest.java
        ├── BindingPowerTest.java
        ├── ExpressionParserTest.java        # precedence, associativity, unary, edge cases
        ├── StatementParserTest.java         # all Stmt variants
        ├── ParserTest.java                  # integration: full parse of dr_files/ samples
        └── ParserErrorRecoveryTest.java     # error recovery and SyntaxError location
```

**Structure Decision**: Single-module Gradle project (Option 1). Parser classes are added to the
existing `org.dersbian.compiler.syntax` package and sub-packages as mandated by FR-009.
`DefaultCompilerService` gains a minimal wiring change; all other existing classes are untouched.

---

## Complexity Tracking

No constitution violations requiring justification. All new classes are within the existing
module and package boundaries.
