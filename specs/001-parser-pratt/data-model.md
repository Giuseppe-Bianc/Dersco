# Data Model: Recursive Descent + Pratt Parser

**Feature**: `001-parser-pratt` | **Branch**: `001-parser-pratt` | **Date**: 2026-08-12

All types reside in `org.dersbian.compiler.syntax` unless marked otherwise.
Existing types (marked **existing**) are not modified.

---

## Core Result Type

### `ParseResult` *(new)*

```java
public record ParseResult(
    List<Stmt> statements,                     // top-level parsed statements
    List<CompileError.SyntaxError> errors      // all collected syntax errors
) {
    public ParseResult {
        statements = List.copyOf(statements);
        errors     = List.copyOf(errors);
    }
    /** Convenience: true when at least one syntax error was collected. */
    public boolean hasErrors() { return !errors.isEmpty(); }
}
```

**Invariants**:

- Both lists are non-null and unmodifiable.
- `statements` contains the top-level nodes parsed before or between errors; it may be
  non-empty even when `hasErrors()` is true (error recovery continues parsing).
- `errors` order matches source order (earlier span first).

**Relationships**: `ParseResult` is the output of `Parser.parse()` and the input inspected
by `DefaultCompilerService.checkSyntax`.

**State transitions**: None — immutable record.

---

## Cursor

### `TokenCursor` *(new, package-private)*

```java
final class TokenCursor {
    // Constructed from List<Token>; comments eagerly stripped.
    TokenCursor(List<Token> tokens) { … }

    Token peek();                          // lookahead without consuming
    Token advance();                       // consume and return current token
    boolean check(TokenKind expected);     // true if peek().type() == expected
    Token expect(TokenKind expected,       // advance if match, else record error and return synthetic EOF
                 List<CompileError.SyntaxError> errors);
    void synchronize(List<CompileError.SyntaxError> errors); // skip to next '}' or statement keyword
    boolean isAtEnd();                     // true when only EOF remains
    Span currentSpan();                    // span of the current peek token
}
```

**Fields**:

- `tokens: List<Token>` — comment-filtered, immutable copy
- `pos: int` — current index (0-based)

**Invariants**:

- `peek()` never returns a `COMMENT` or `MULTILINE_COMMENT` token.
- `peek()` always returns an `EOF` token when `isAtEnd()` is true (never throws).
- `expect()` emits at most one error per call; it does not synchronize.

---

## Binding Power

### `BindingPower` *(new, package-private)*

```java
record BindingPower(int left, int right) {
    /** Returns the infix binding power for the given token, or empty if not an infix operator. */
    static Optional<BindingPower> infix(TokenKind kind) { … }

    /** Returns the prefix right-binding power for the given token, or empty if not a prefix op. */
    static OptionalInt prefix(TokenKind kind) { … }

    /** Returns the postfix left-binding power for the given token, or empty if not a postfix op. */
    static OptionalInt postfix(TokenKind kind) { … }
}
```

**Precedence table** (multiples of 10; right-associative: `right = left - 1`):

| `left` | `right` | Operators |
|--------|---------|-----------|
| 10 | 9 | `=` `+=` `-=` `*=` `/=` `%=` `&=` `\|=` `^=` `<<=` `>>=` |
| 20 | 21 | `\|\|` |
| 30 | 31 | `&&` |
| 40 | 41 | `\|` |
| 50 | 51 | `^` |
| 60 | 61 | `&` |
| 70 | 71 | `==` `!=` |
| 80 | 81 | `<` `<=` `>` `>=` |
| 90 | 91 | `<<` `>>` |
| 100 | 101 | `+` `-` |
| 110 | 111 | `*` `/` `%` |
| — | 120 | (prefix) `-` `!` `~` `++` `--` |
| 130 | — | (postfix/call/index) `++` `--` `(` `[` |

**Invariants**:

- Left-associative: `right = left + 1`
- Right-associative: `right = left - 1`
- Prefix tokens have no `left` value (they terminate the infix loop).
- Postfix / call / index tokens have no `right` value (they are consumed in `parseLed`).

---

## Parser Components

### `ExpressionParser` *(new, package-private)*

```java
final class ExpressionParser {
    ExpressionParser(TokenCursor cursor, List<CompileError.SyntaxError> errors) { … }

    Expr parseExpression(int minBp);    // Pratt loop entry (pass 0 for top-level)
    Expr parsePrimary();                // literals, identifiers, grouping, unary prefix
}
```

**Fields**:

- `cursor: TokenCursor` — shared with `StatementParser`
- `errors: List<CompileError.SyntaxError>` — mutable accumulator, shared with `Parser`

**Key responsibilities**:

- `parsePrimary()` handles: integer/float/bool/string/char/nullptr literals, identifiers,
  `(expr)` grouping, prefix unary (`-`, `!`, `~`, `++`, `--`), and array literals `{…}`.
- `parseExpression(minBp)` runs the Pratt loop: peek for infix; if `infix.left >= minBp`,
  consume and recur with `infix.right` as the new minimum; else return.
- Postfix `++`/`--` and call `(…)` / index `[…]` are handled as left-denotation cases
  at binding power 130.
- Compound assignment: parsed as regular `=` (right-assoc, bp=10/9), then lowered to
  `Expr.Assign(target, Expr.Binary(target, op, rhs))` in the led handler.

---

### `StatementParser` *(new, package-private)*

```java
final class StatementParser {
    StatementParser(TokenCursor cursor, ExpressionParser exprParser,
                    List<CompileError.SyntaxError> errors) { … }

    Stmt parseStatement();          // dispatch on current token
    Stmt.Block parseBlock();        // '{' statement* '}'

    // One method per keyword:
    Stmt parseFunDecl();
    Stmt parseVarDecl(boolean isMutable);
    Stmt parseIf();
    Stmt parseWhile();
    Stmt parseFor();
    Stmt parseReturn();
    Stmt parseBreak();
    Stmt parseContinue();
    Stmt parseMainBlock();
    Stmt parseExpressionStatement();
}
```

**Fields**:

- `cursor: TokenCursor` — shared
- `exprParser: ExpressionParser` — shared
- `errors: List<CompileError.SyntaxError>` — shared mutable accumulator

**Dispatch table** (`parseStatement()` switch):

| Current token | Dispatches to |
|---------------|--------------|
| `Keyword.FUN` | `parseFunDecl()` |
| `Keyword.VAR` | `parseVarDecl(true)` |
| `Keyword.CONST` | `parseVarDecl(false)` |
| `Keyword.IF` | `parseIf()` |
| `Keyword.WHILE` | `parseWhile()` |
| `Keyword.FOR` | `parseFor()` |
| `Keyword.RETURN` | `parseReturn()` |
| `Keyword.BREAK` | `parseBreak()` |
| `Keyword.CONTINUE` | `parseContinue()` |
| `Keyword.MAIN` | `parseMainBlock()` |
| anything else | `parseExpressionStatement()` |

---

### `Parser` *(new, public final)*

```java
public final class Parser {
    public Parser(List<Token> tokens, Path source) { … }

    public ParseResult parse();     // entry point; never throws for syntax errors
}
```

**Fields**:

- `cursor: TokenCursor`
- `errors: List<CompileError.SyntaxError>` — mutable; drained into `ParseResult`
- `exprParser: ExpressionParser`
- `stmtParser: StatementParser`
- `source: Path` — for error context

**Algorithm** (`parse()`):

1. Construct internal components.
2. Loop: while `!cursor.isAtEnd()`, call `stmtParser.parseStatement()` and collect results.
3. Return `new ParseResult(statements, errors)`.

---

## Existing AST Types (unchanged)

All types below are **existing** in `org.dersbian.compiler.syntax.ast`; no fields or
method signatures are modified.

### `Expr` (sealed interface, existing)

| Variant | Fields | Notes |
|---------|--------|-------|
| `Binary` | `left: Expr`, `op: BinaryOp`, `right: Expr`, `span: Span` | Infix binary ops |
| `Unary` | `op: UnaryOp`, `side: UnaryOpSide`, `expr: Expr`, `span: Span` | Prefix and postfix unary |
| `Grouping` | `expr: Expr`, `span: Span` | Parenthesized sub-expression |
| `Literal` | `value: LiteralValue`, `span: Span` | All literal types |
| `ArrayLiteral` | `elements: List<Expr>`, `span: Span` | `{…}` array literal; empty list for `{}` |
| `Variable` | `name: String`, `span: Span` | Identifier reference |
| `Assign` | `target: Expr`, `value: Expr`, `span: Span` | `=` and compound assignments |
| `Call` | `callee: Expr`, `arguments: List<Expr>`, `span: Span` | Function call; empty args for `f()` |
| `ArrayAccess` | `array: Expr`, `index: Expr`, `span: Span` | `a[i]` |

### `Stmt` (sealed interface, existing)

| Variant | Key Fields | Notes |
|---------|-----------|-------|
| `Expression` | `expr: Expr` | Expression statement |
| `VarDeclaration` | `bindings`, `typeAnnotation`, `isMutable`, `span` | `var`/`const` |
| `Function` | `name`, `parameters`, `returnType`, `body: Block`, `span` | `fun` declaration |
| `If` | `condition`, `thenBranch: Block`, `elseBranch: ElseBranch`, `span` | `if`/`else if`/`else` |
| `While` | `condition`, `body: Block`, `span` | `while` loop |
| `For` | `initializer: Optional<Stmt>`, `condition: Optional<Expr>`, `increment: Optional<Expr>`, `body: Block`, `span` | `for` loop |
| `Block` | `statements: List<Stmt>`, `span` | `{…}` block |
| `Return` | `value: Optional<Expr>`, `span` | `return` statement |
| `Break` | `span` | `break` statement |
| `Continue` | `span` | `continue` statement |
| `MainFunction` | `body: Block`, `span` | `main { … }` block |

### `BinaryOp` (enum, existing)

29 variants: `ADD`, `SUBTRACT`, `MULTIPLY`, `DIVIDE`, `MODULO`, `EQUAL`, `NOT_EQUAL`,
`LESS`, `LESS_EQUAL`, `GREATER`, `GREATER_EQUAL`, `AND`, `OR`, `BITWISE_AND`, `BITWISE_OR`,
`BITWISE_XOR`, `SHIFT_LEFT`, `SHIFT_RIGHT` plus compound-assignment variants. Used as the
`op` in `Expr.Binary`; the `_EQUAL` variants appear inside the desugared compound-assignment
`Expr.Binary` node.

### `UnaryOp` (enum, existing)

`NEGATE`, `NOT`, `BITWISE_NOT`, `INCREMENT`, `DECREMENT`.

### `UnaryOpSide` (enum, existing)

`PREFIX`, `POSTFIX` — distinguishes `++a` from `a++`.

### `ElseBranch` (sealed interface, existing)

`ElseBranch.Empty`, `ElseBranch.Else(Block)`, `ElseBranch.ElseIf(Stmt.If)`.

### `Type` (sealed interface, existing)

All primitive types (`I8`…`U64`, `F32`, `F64`, `Char`, `StringT`, `Bool`), `Custom`,
`Array`, `Vector`, `VoidT`, `NullPtr`.

### `LiteralValue` (sealed interface, existing)

`Numeric(INumber)`, `Bool(boolean)`, `StringLit(String)`, `CharLit(String)`, `NullPtr`.

### `Parameter` (record, existing)

`name: String`, `type: Type`.

---

## Error Type (existing)

### `CompileError.SyntaxError`

```text
SyntaxError(
    errorCode:    Optional<ErrorCode>,
    errorMessage: String,
    errorSpan:    Span,
    errorHelp:    Optional<String>
)
```

**Validation rules**:

- `errorMessage` and `errorSpan` are non-null (enforced by factory method).
- `errorCode` is `Optional.of(ErrorCode.E1005)` for operator errors; for parser-specific
  errors a new `ErrorCode` entry may be needed — researched separately (see research.md §2).
  If no suitable code exists, `Optional.empty()` is used and a descriptive message is
  provided.
- `errorHelp` is `Optional.empty()` unless a corrective suggestion is obvious.

---

## Span (existing)

`Span` in `org.dersbian.compiler.lexer.token.Span` carries source position for all nodes.
Every new `Expr` or `Stmt` record constructed by the parser must receive the span of the
opening token to the closing token of the construct, using `Span.merge()` or the appropriate
span factory.
