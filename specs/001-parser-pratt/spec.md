# Feature Specification: Recursive Descent + Pratt Parser

**Feature Branch**: `001-parser-pratt`

**Created**: 2026-08-12

**Status**: Draft

**Input**: User description: "Implement a parser using a combined Recursive Descent + Pratt Parsing
architecture, producing a correct AST for all syntactic constructs defined by the Dersco language
grammar, with deterministic operator precedence and associativity."

---

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Parse a complete Dersco source file (Priority: P1)

A developer invokes the compiler on a valid `.dr` source file. The compiler tokenizes the source
and feeds the token stream to the parser. The parser produces a structurally correct, fully-formed
AST that faithfully represents every top-level declaration and statement in the file.

**Why this priority**: This is the minimal viable capability of the parser. Without it, no
downstream phase (semantic analysis, code generation) can run. It is also the broadest integration
path for the existing lexer infrastructure.

**Independent Test**: Given the token stream produced by the existing `Lexer` for
`large_toy_program.dr`, the parser returns a `Stmt.MainFunction` and one or more `Stmt.Function`
nodes whose structures match the source. Can be verified entirely without a semantic phase.

**Acceptance Scenarios**:

1. **Given** a valid `.dr` file containing function declarations, variable declarations, and a
   `main` block, **When** the parser processes the token stream, **Then** it returns a list of
   `Stmt` nodes matching every top-level construct without errors.
2. **Given** a source file containing only a `main { }` block with no statements, **When** the
   parser processes the token stream, **Then** it returns a `Stmt.MainFunction` with an empty
   `Block`.
3. **Given** a source file that begins with a valid function declaration followed by an invalid
   token, **When** the parser processes the token stream, **Then** it emits at least one structured
   parse error that identifies the offending position and the expected construct.

---

### User Story 2 - Correct operator precedence and associativity in expressions (Priority: P1)

A developer writes an arithmetic or logical expression in a `.dr` source file. The parser groups
the operands according to the language's defined precedence table and associativity rules, producing
an `Expr.Binary` tree whose shape matches the intended evaluation order.

**Why this priority**: Precedence and associativity correctness is a safety-critical property. A
parser that silently mis-groups `a + b * c` as `(a + b) * c` would produce incorrect compiled
output without any visible error.

**Independent Test**: Given the token stream for the expression `1 + 2 * 3`, the parser produces
`Binary(Literal(1), ADD, Binary(Literal(2), MULTIPLY, Literal(3)))`. Each precedence level and each
associativity direction can be verified by a dedicated unit test without running the full pipeline.

**Acceptance Scenarios**:

1. **Given** the expression `a + b * c`, **When** parsed, **Then** the AST is
   `Binary(Variable(a), ADD, Binary(Variable(b), MULTIPLY, Variable(c)))`.
2. **Given** the expression `a - b - c` (left-associative subtraction), **When** parsed, **Then**
   the AST is `Binary(Binary(Variable(a), SUBTRACT, Variable(b)), SUBTRACT, Variable(c))`.
3. **Given** the expression `a = b = c` (right-associative assignment), **When** parsed, **Then**
   the AST is `Assign(Variable(a), Assign(Variable(b), Variable(c)))`.
4. **Given** the expression `!-a` (chained prefix unary operators), **When** parsed, **Then** the
   AST is `Unary(NOT, PREFIX, Unary(NEGATE, PREFIX, Variable(a)))`.
5. **Given** the expression `a++` (postfix increment), **When** parsed, **Then** the AST is
   `Unary(INCREMENT, POSTFIX, Variable(a))`.
6. **Given** the expression `a && b || c`, **When** parsed, **Then** the AST reflects that `&&`
   binds tighter than `||`:
   `Binary(Binary(Variable(a), AND, Variable(b)), OR, Variable(c))`.

---

### User Story 3 - Parse all statement constructs (Priority: P2)

A developer writes any statement construct supported by the Dersco language: variable declarations
(`var`/`const`), `if`/`else if`/`else`, `while`, `for`, `return`, `break`, `continue`, function
declarations (`fun`), and the `main` block. The parser produces the correct `Stmt` variant for each.

**Why this priority**: Statement parsing uses Recursive Descent and is independent of expression
precedence. It can be implemented, tested, and merged incrementally before or alongside the
expression layer.

**Independent Test**: Given a source fragment containing each statement kind in isolation, the
parser returns the matching `Stmt` record with correct fields (condition, body, bindings, return
type, etc.).

**Acceptance Scenarios**:

1. **Given** `var x: i32 = 5i32`, **When** parsed, **Then** the result is
   `Stmt.VarDeclaration` with `isMutable=true`, binding `("x", Literal(5))`, type `Type.I32`.
2. **Given** `const y: bool = true`, **When** parsed, **Then** the result is
   `Stmt.VarDeclaration` with `isMutable=false`, binding `("y", Literal(true))`, type `Type.Bool`.
3. **Given** a `fun` declaration with parameters and a return type, **When** parsed, **Then** the
   result is `Stmt.Function` with the correct name, parameter list, return type, and body block.
4. **Given** an `if` with an `else if` and a trailing `else`, **When** parsed, **Then** the result
   is a `Stmt.If` whose `ElseBranch` is a nested `Stmt.If` terminating in a plain `Stmt.Block`.
5. **Given** a `for` loop with all three clauses present, **When** parsed, **Then** the result is
   `Stmt.For` with `initializer`, `condition`, and `increment` all present.

---

### User Story 4 - Structured parse errors with source location (Priority: P2)

A developer submits a source file containing a syntax error. Instead of a crash or an opaque
exception, the compiler emits a structured `CompileError.SyntaxError` that includes the source
span of the offending token and a human-readable message indicating what was expected.

**Why this priority**: Developer experience and debuggability. The existing diagnostic engine
(`ErrorReporter`) is already capable of rendering located errors; the parser must feed it
correctly-located errors.

**Independent Test**: Given a token stream with a deliberate syntax error (e.g., a missing closing
parenthesis after a function call), the parser produces a `CompileError.SyntaxError` whose `span`
points to the erroneous token and whose message names the expected token or construct.

**Acceptance Scenarios**:

1. **Given** a function call with a missing `)`, **When** parsed, **Then** the error message
   references the expected `)` and the span points to the token that was found instead.
2. **Given** a `var` declaration with a missing `:` before the type, **When** parsed, **Then**
   the error message references the expected `:` and the span is correct.
3. **Given** multiple errors in a single source file, **When** the parser operates in
   error-recovery mode, **Then** it produces one `CompileError.SyntaxError` per error and
   continues parsing so that subsequent errors are also reported.

---

### Edge Cases

- What happens when the token stream ends mid-expression (e.g., `1 +` with no right operand)?
  → The parser emits a `CompileError.SyntaxError` indicating an unexpected EOF and returns a
  partial or null right operand, depending on the chosen recovery strategy.
- What happens when an operator appears where a primary expression is expected?
  → The parser emits a structured error and attempts to skip to a synchronization point
  (the next `}` or the next statement-opening keyword).
- How does the parser handle deeply nested expressions (e.g., 100+ levels of parentheses)?
  → The Pratt loop is iterative, not recursive on precedence climbs, so stack depth is bounded
  by the depth of syntactic nesting (blocks, function calls), not by operator chaining.
- What happens when `++` or `--` appears on a non-lvalue (e.g., `5++`)?
  → The parser accepts it structurally (it is syntactically valid) and defers the lvalue check
  to the semantic phase.
- What happens with an empty array literal `{}`?
  → The parser produces an `Expr.ArrayLiteral` with an empty element list.
- What happens with a function call with no arguments `f()`?
  → The parser produces an `Expr.Call` with an empty argument list.

---

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The parser MUST consume a `List<Token>` (or equivalent token iterator) produced by
  the existing `Lexer` and produce a `ParseResult` value carrying both a `List<Stmt>` representing
  the top-level program structure and a `List<CompileError.SyntaxError>` collecting all parse
  errors encountered during the run. `parse()` MUST NOT throw for recoverable syntax errors.
- **FR-002**: The parser MUST implement Recursive Descent for all statement-level constructs:
  function declarations (`fun`), the `main` block, variable/constant declarations (`var`/`const`),
  `if`/`else if`/`else`, `while`, `for`, `return`, `break`, `continue`, and expression statements.
- **FR-003**: The parser MUST implement Pratt Parsing for all expression-level constructs, using
  a numeric binding-power (precedence) table and per-operator associativity to drive the
  parse loop.
- **FR-004**: The precedence table MUST reflect the following ordering, from lowest to highest
  binding power:
  1. Assignment operators (`=`, `+=`, `-=`, `*=`, `/=`, `%=`, `&=`, `|=`, `^=`, `<<=`, `>>=`) — right-associative
  2. Logical OR (`||`) — left-associative (`TokenKind.Simple.Operator.OR_OR`)
  3. Logical AND (`&&`) — left-associative (`TokenKind.Simple.Operator.AND_AND`)
  4. Bitwise OR (`|`) — left-associative (`TokenKind.Simple.Operator.OR`)
  5. Bitwise XOR (`^`) — left-associative (`TokenKind.Simple.Operator.XOR`)
  6. Bitwise AND (`&`) — left-associative (`TokenKind.Simple.Operator.AND`)
  7. Equality (`==`, `!=`) — left-associative
  8. Relational (`<`, `<=`, `>`, `>=`) — left-associative
  9. Shift (`<<`, `>>`) — left-associative
  10. Additive (`+`, `-`) — left-associative
  11. Multiplicative (`*`, `/`, `%`) — left-associative
  12. Prefix unary (`-`, `!`, `~`, `++`, `--`) — right-associative (applied to the right operand)
  13. Postfix unary (`++`, `--`) and call/index (`(…)`, `[…]`) — left-associative (highest)
- **FR-004b**: Compound assignment operators (`+=`, `-=`, `*=`, `/=`, `%=`, `&=`, `|=`, `^=`,
  `<<=`, `>>=`) MUST be parsed as `Expr.Assign(target, Expr.Binary(target, op, rhs))` where `op`
  is the corresponding non-compound `BinaryOp` (e.g., `ADD` for `+=`). No new `Expr` variant is
  introduced; the target sub-expression is re-evaluated syntactically on both sides.
- **FR-005**: The parser MUST produce `Expr.Binary`, `Expr.Unary`, `Expr.Grouping`,
  `Expr.Literal`, `Expr.ArrayLiteral`, `Expr.Variable`, `Expr.Assign`, `Expr.Call`, and
  `Expr.ArrayAccess` nodes using the existing sealed `Expr` hierarchy without modification.
- **FR-006**: The parser MUST produce `Stmt.Expression`, `Stmt.VarDeclaration`, `Stmt.Function`,
  `Stmt.If`, `Stmt.While`, `Stmt.For`, `Stmt.Block`, `Stmt.Return`, `Stmt.Break`,
  `Stmt.Continue`, and `Stmt.MainFunction` nodes using the existing sealed `Stmt` hierarchy
  without modification.
- **FR-007**: Every parse error MUST be represented as a `CompileError.SyntaxError` carrying
  the correct `Span` of the offending token and a message naming the expected token or construct.
- **FR-008**: The parser MUST support basic error recovery: upon encountering a syntax error it
  MUST advance to the nearest synchronization point — the next `}` (consumed) or the next
  statement-opening keyword (`fun`, `var`, `const`, `if`, `while`, `for`, `return`, `break`,
  `continue`, `main`) (not consumed) — and continue parsing so that all errors in one file are
  reported in a single pass. There is no cap on the number of errors collected; the parser MUST
  NOT abort early due to error count alone.
- **FR-009**: The parser MUST reside in the package `org.dersbian.compiler.syntax` (or a
  sub-package thereof) and MUST NOT introduce dependencies on semantic or code-generation phases.
- **FR-010**: The tokenization, expression parsing, statement parsing, and error collection
  responsibilities MUST be held by distinct classes or methods; no single class may own more than
  one of these responsibilities.
- **FR-011**: The token cursor MUST silently discard all `TokenKind.Simple.Special.COMMENT` and
  `TokenKind.Simple.Special.MULTILINE_COMMENT` tokens before any grammar rule or Pratt loop step
  processes them. Comment tokens MUST never reach a parse function or trigger a parse error.
- **FR-012**: The parser MUST emit no log output during a parse run. All results — parsed
  statements and collected errors — MUST flow exclusively through `ParseResult`. No SLF4J or
  other logging calls are permitted inside parser classes.

### Key Entities

- **Parser**: The top-level class that accepts a token stream and exposes a `parse()` method
  returning a `ParseResult` record carrying `List<Stmt> statements` and
  `List<CompileError.SyntaxError> errors`. Never throws for recoverable syntax errors.
- **ParseResult**: An immutable value record bundling the parsed statement list and the collected
  error list. Callers (e.g., `DefaultCompilerService`) inspect `errors()` to determine whether the
  parse succeeded cleanly.
- **PrattExpressionParser** (or equivalent inner structure): The component responsible for
  parsing expressions using binding-power arithmetic. Delegates to the token stream for lookahead
  and consumption.
- **BindingPower**: A value object or enum encoding the left-binding-power and right-binding-power
  of each operator, used by the Pratt loop to decide when to stop consuming the right-hand side.
- **ParseError** (mapped to `CompileError.SyntaxError`): The structured error representation
  produced when a token does not match the expected grammar rule, including position and
  expected-token information.
- **Token stream / cursor**: A stateful view over the `List<Token>` produced by the lexer,
  providing `peek()`, `advance()`, `expect(TokenKind)`, and `synchronize()` operations.

---

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All valid sample `.dr` files in `dr_files/` are parsed without errors, producing a
  non-empty `List<Stmt>` whose top-level node types match the file's declared structure.
- **SC-002**: For every precedence level in the operator table (FR-004), at least one dedicated
  unit test confirms the correct AST shape for both a left-associative grouping example and a
  cross-level precedence example.
- **SC-003**: For every right-associative operator (assignment operators), at least one dedicated
  unit test confirms that chained uses group to the right.
- **SC-004**: The parser test suite achieves branch coverage ≥ 80% on all parser classes as
  reported by JaCoCo, consistent with the project's coverage gate.
- **SC-005**: No single parse of any `dr_files/` sample produces an unhandled exception;
  errors in invalid input files are surfaced as `CompileError.SyntaxError` entries, not as
  thrown exceptions.
- **SC-006**: All quality gates (`./gradlew :app:check`) pass with the parser code in place:
  Checkstyle, PMD, SpotBugs, Error Prone (`-Werror`), Spotless, and JaCoCo.
- **SC-007**: A developer can parse a 200-line `.dr` file end-to-end in under 100 ms on a
  development machine, confirming the parser introduces no observable startup or throughput
  regression for the existing test suite.

---

## Assumptions

- The `Lexer` already produces a complete, correct token stream for all `.dr` source files in
  `dr_files/`. The parser will consume that stream as-is without re-tokenizing.
- The existing `Expr`, `Stmt`, `BinaryOp`, `UnaryOp`, `Type`, `LiteralValue`, and `Span`
  types are sufficient to represent all syntactic constructs currently defined by the language;
  no new AST node types are required for this feature.
- Compound assignment operators are represented as `Expr.Assign(target, Expr.Binary(target, op, rhs))`
  per FR-004b; no new `Expr` variant is needed. This is now a requirement, not merely an assumption.
- The `main` block is syntactically distinct from a `fun` declaration and is parsed into a
  `Stmt.MainFunction`; the parser does not treat it as a regular function.
- Multi-dimensional array literals (e.g., `{{1i8, 2i8}, {3i8, 4i8}}`) are represented as
  nested `Expr.ArrayLiteral` nodes; the parser does not need to enforce dimension consistency
  (that is a semantic concern).
- Error recovery targets synchronization at `}` (consumed) and statement-opening keywords
  (`fun`, `var`, `const`, `if`, `while`, `for`, `return`, `break`, `continue`, `main`) (not
  consumed) only; more sophisticated recovery strategies (e.g., inserting missing tokens) are
  out of scope for this feature.
- The `DefaultCompilerService` integration (wiring the parser into `checkSyntax`/`compile`) is
  within scope for this feature: the parser must be invokable from the service layer, but the
  service does not need to pass the resulting AST to any downstream phase.
- Preview Java features are not used; the implementation relies solely on Java 25 stable features
  (sealed interfaces, records, pattern matching for switch) already present in the codebase.

---

## Clarifications

### Session 2026-08-12

- Q: When a compound assignment operator like `+=` or `<<=` appears in an expression, how should the parser represent it in the AST? → A: Option A — `Expr.Assign(target, Expr.Binary(target, op, rhs))`; no new AST variant introduced.
- Q: How should the parser treat `COMMENT` and `MULTILINE_COMMENT` tokens that the lexer emits — should they be silently skipped during parsing, or preserved? → A: Option A — silently filtered by the token cursor before any grammar rule sees them.
- Q: How should the parser surface collected errors to its caller — result object, throw on first error, or dual API? → A: Option A — `parse()` returns a `ParseResult` carrying both `List<Stmt>` and `List<CompileError.SyntaxError>`; no exception thrown for recoverable syntax errors.
- Q: Should the parser impose a maximum number of syntax errors before aborting, or collect all errors? → A: Option A — unbounded; the parser always attempts to collect every error and MUST NOT abort early due to error count.
- Q: Should the parser emit any log output during a parse run, or remain completely silent? → A: Option A — completely silent; all information flows through `ParseResult` only; no logging calls permitted inside parser classes.
