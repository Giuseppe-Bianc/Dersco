# Research: Recursive Descent + Pratt Parser for Dersco

**Feature**: `001-parser-pratt` | **Branch**: `001-parser-pratt` | **Date**: 2026-08-12

---

## 1. Parsing Strategy

### Decision

Combine **Recursive Descent** for statement-level rules (declarations, control flow) with
**Pratt Parsing** (top-down operator precedence) for all expression-level constructs.

### Rationale

- Recursive Descent maps cleanly onto the fixed statement grammar: each keyword (`fun`, `if`,
  `while`, `for`, `var`, `const`, `return`, `break`, `continue`, `main`) has exactly one
  dispatch branch. Control flow and nesting are expressed naturally as recursive calls.
- Pratt Parsing handles the 13-level operator precedence table (FR-004) without manual
  left-recursion elimination. The algorithm's numeric binding-power representation maps
  directly to precedence integers; associativity is encoded as `left-bp = right-bp` (left)
  or `left-bp = right-bp - 1` (right). This is iterative on precedence climbs, so stack
  depth is bounded by syntactic nesting depth, not by operator chains (edge-case requirement).
- The combination is the canonical modern approach; it is used in Clang, Rust's parser,
  GCC, and numerous well-documented hobby compilers (Crafting Interpreters, Writing a
  Compiler in Go). No academic uncertainty remains.

### Alternatives Considered

- **ANTLR / parser generator**: Rejected — introduces a new build-time dependency, generates
  code that would clash with the project's Checkstyle/Spotless/SpotBugs gates, and is not
  aligned with the codebase's hand-written style.
- **Full recursive descent for expressions**: Rejected — requires one grammar rule per
  precedence level (13 levels), producing deeply recursive call stacks for long expressions
  and making precedence table changes expensive. Pratt eliminates this.
- **Operator precedence climbing (a Pratt variant)**: Effectively identical; "binding power"
  and "minimum precedence" are dual representations of the same algorithm. The binding-power
  formulation is chosen because it maps cleanly to an immutable value type.

---

## 2. Class Decomposition (FR-010)

### Decision

Five classes, each with a single responsibility:

| Class | Responsibility |
|-------|---------------|
| `TokenCursor` | Stateful view over `List<Token>`: `peek()`, `advance()`, `expect()`, `synchronize()`, comment filtering |
| `BindingPower` | Immutable operator → (left-bp, right-bp) lookup; encodes the 13-level precedence table |
| `ExpressionParser` | Pratt loop: `parseExpression(minBp)`, `parsePrimary()`, infix/postfix dispatch |
| `StatementParser` | Recursive-descent rules: one `parse*()` method per statement keyword |
| `Parser` | Entry point: constructs sub-components, owns `parse()` → `ParseResult` |

`ParseResult` is a record, not a class with behaviour. It is not counted as a "responsibility"
class for FR-010 purposes.

### Rationale

FR-010 explicitly prohibits a single class from owning more than one of: tokenization,
expression parsing, statement parsing, error collection. This decomposition satisfies that
constraint and makes each class independently unit-testable.

### Alternatives Considered

- **Merging `ExpressionParser` into `Parser`**: Rejected — violates FR-010.
- **One class with inner record for expression state**: Rejected — still co-locates
  statement and expression logic in a single compilation unit.

---

## 3. Binding Power Representation

### Decision

A package-private `record BindingPower(int left, int right)` paired with a static lookup
method `BindingPower.of(TokenKind)` returning `Optional<BindingPower>`. An absent value
means the token cannot act as an infix operator and terminates the Pratt loop.

Precedence integers (multiples of 10 for readability):

| Level | Operators | Left BP | Right BP | Associativity |
|-------|-----------|---------|----------|---------------|
| 1 | `=` `+=` `-=` `*=` `/=` `%=` `&=` `\|=` `^=` `<<=` `>>=` | 10 | 9 | Right |
| 2 | `\|\|` | 20 | 21 | Left |
| 3 | `&&` | 30 | 31 | Left |
| 4 | `\|` | 40 | 41 | Left |
| 5 | `^` | 50 | 51 | Left |
| 6 | `&` | 60 | 61 | Left |
| 7 | `==` `!=` | 70 | 71 | Left |
| 8 | `<` `<=` `>` `>=` | 80 | 81 | Left |
| 9 | `<<` `>>` | 90 | 91 | Left |
| 10 | `+` `-` | 100 | 101 | Left |
| 11 | `*` `/` `%` | 110 | 111 | Left |
| 12 | (prefix) `-` `!` `~` `++` `--` | — | 120 | Right (prefix nud) |
| 13 | (postfix) `++` `--` `(` `[` | 130 | — | Left (postfix led) |

Right-associativity is encoded as `right-bp = left-bp - 1` (so the loop recurs when equal);
left-associativity as `right-bp = left-bp + 1`.

### Rationale

Multiples of 10 leave room for future levels without renumbering. The `Optional<BindingPower>`
sentinel avoids a separate `isInfix(TokenKind)` predicate and keeps the loop condition clean.

### Alternatives Considered

- **Enum with left/right fields**: Viable but ties `BindingPower` to a closed set of
  operators; a `record` + `switch` in a static factory is equally readable and easier to
  extend.
- **Single integer per operator + convention for right-assoc**: Rejected — forces the caller
  to know the convention; an explicit left+right pair is self-documenting.

---

## 4. Comment Filtering (FR-011)

### Decision

`TokenCursor` filters `TokenKind.Simple.Special.COMMENT` and
`TokenKind.Simple.Special.MULTILINE_COMMENT` tokens during construction (or lazily on each
`advance()` call). Both variants are silently discarded before any `peek()` or grammar rule
ever sees them.

### Rationale

The spec explicitly mandates that comment tokens never reach a parse function or trigger
a parse error (FR-011). Filtering at the cursor level is the cleanest single point of
enforcement; it avoids scattered `if (isComment) continue` guards in every grammar method.

The implementation choice (eager vs lazy) is an internal detail of `TokenCursor`. Eager
pre-filtering (copying to a new `ArrayList`) is preferred because the token list is already
fully materialized by the `Lexer`; the memory overhead is bounded and the cursor logic stays
simple.

### Alternatives Considered

- **Filter in `Lexer`**: Rejected — the lexer currently emits all tokens including comments
  (they are used by external tools/IDE integrations). The spec says the *cursor* filters, not
  the lexer.
- **Filter in `StatementParser`/`ExpressionParser`**: Rejected — duplicates the guard across
  dozens of parse methods.

---

## 5. Error Recovery Strategy (FR-008)

### Decision

**Synchronization-point recovery**: when a syntax error is detected, emit a
`CompileError.SyntaxError`, add it to the error list, then call `synchronize()` which
advances the cursor past the next `}` token (block boundary). As a secondary heuristic,
the cursor also stops at the next statement-opening keyword (`fun`, `var`, `const`, `if`,
`while`, `for`, `return`, `break`, `continue`, `main`). Parsing then resumes from that
point. No error cap (FR-008). No exception is thrown for recoverable errors (FR-001).

> **Note**: The original spec (FR-008) mentions `; or }` as synchronization points. Since
> Dersco does not use semicolons as statement terminators (confirmed by all canonical
> `dr_files/` samples), `}` and statement-opening keywords are the practical sync points.
> The spec's mention of `;` likely reflects a C-family assumption; it is inert in practice
> because `;` only appears in `for` headers, never as a statement terminator.

### Rationale

Recovery at block-close (`}`) prevents a single malformed block from cascading errors
through the rest of the file. Stopping at statement-opening keywords catches errors in
non-block contexts (top-level declarations). This is consistent with the actual grammar
and with hand-written parsers for brace-delimited, no-semicolon languages.

### Alternatives Considered

- **Synchronize to `;` as in original spec**: `;` appears only inside `for` headers in
  Dersco; using it as a sync point would cause the parser to overshoot into the `for`
  condition or increment clause, producing confusing secondary errors.
- **No secondary keyword sync, only `}`**: Simpler but misses top-level errors where
  there is no enclosing `}` to synchronize to.
- **Error productions in the grammar**: Too complex for the scope of this feature; deferred
  to a future improvement.
- **Throw on first error**: Rejected — violates FR-001 and FR-008.

---

## 6. Compound Assignment Representation (FR-004b)

### Decision

`Expr.Assign(target, Expr.Binary(target, op, rhs))` where `op` is the non-compound
`BinaryOp` (e.g., `ADD` for `+=`). No new `Expr` variant is introduced.

The `target` sub-expression is re-syntactically-evaluated on both sides (i.e., the identifier
or array-access expression is parsed once but referenced twice in the AST). The lvalue
validity check is deferred to the semantic phase.

### Rationale

Confirmed by the spec clarification session (2026-08-12, Q1). Reusing `Expr.Assign` and
`Expr.Binary` avoids adding a new sealed variant (which would require updating `AstPrinter`,
`NodeCounter`, and any future visitor).

### Alternatives Considered

- **New `Expr.CompoundAssign` variant**: Rejected per spec clarification.
- **`Expr.Assign` with a non-null `op` field**: Would require changing the existing sealed
  record, which is out of scope.

---

## 7. `ParseResult` vs Exception API (FR-001)

### Decision

`parse()` always returns a `ParseResult` record; it never throws for recoverable syntax
errors. A `CompilerException` may still propagate if an unexpected `null` or truly
unrecoverable internal condition occurs (e.g., cursor state corruption), but those are bugs,
not expected error paths.

```java
public record ParseResult(List<Stmt> statements, List<CompileError.SyntaxError> errors) {
    public ParseResult {
        statements = List.copyOf(statements);
        errors     = List.copyOf(errors);
    }
    public boolean hasErrors() { return !errors.isEmpty(); }
}
```

### Rationale

Confirmed by spec clarification (2026-08-12, Q3). The caller (`DefaultCompilerService`)
inspects `result.hasErrors()` to decide whether to surface errors via `ErrorReporter`. This
matches the `LexerResult` precedent already in the codebase.

### Alternatives Considered

- **Throw on first error**: Rejected by spec.
- **Dual API (result + exception)**: Rejected by spec.

---

## 8. `DefaultCompilerService` Integration

### Decision

After `lexer.tokenize()`, `DefaultCompilerService.checkSyntax` constructs a `Parser` with
`result.tokens()`, calls `parser.parse()`, collects `ParseResult.errors()`, renders them
through `ErrorReporter`, and throws `CompilerException` if any errors exist. The existing
lexer error path is preserved.

```java
// pseudocode addition to checkSyntax:
final Parser parser = new Parser(result.tokens(), source);
final ParseResult parseResult = parser.parse();
final String parseErrorReport = errorReporter.reportErrors(parseResult.errors().stream()
    .map(e -> (CompileError) e)
    .toList());
if (!parseErrorReport.isEmpty()) {
    System.out.println(parseErrorReport);
    throw new CompilerException("Parse failed with " + parseResult.errors().size() + " error(s)");
}
```

### Rationale

Minimal change to the existing service; keeps the lexer and parser error paths symmetric
(both funnel through `ErrorReporter`). Per the spec assumption, the resulting AST is not
passed to any downstream phase yet.

### Alternatives Considered

- **Combine lex + parse into a single `LexParseResult`**: Rejected — premature coupling;
  the lexer and parser have independent error sets and are independently testable.
