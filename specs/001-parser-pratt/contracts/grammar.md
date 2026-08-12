# Contract: Dersco Grammar (Parser Phase)

**Feature**: `001-parser-pratt` | **Branch**: `001-parser-pratt` | **Date**: 2026-08-12

This document defines the public grammar contract that `Parser.parse()` must recognise.
It is the authoritative specification for what token sequences the parser accepts and what
AST nodes it produces. Test assertions in `ExpressionParserTest` and `StatementParserTest`
are derived from this contract.

---

## Statement Termination

Dersco does **not** use semicolons as statement terminators. Statements end at the logical
boundary of the construct (closing brace, next keyword, end of expression). The only
semicolons in the language are the **two separators inside `for` loop headers**:
`for ( init ; condition ; increment )`.

The grammar below reflects this; no `;` appears after `return`, `break`, `continue`,
`var`/`const` declarations, or expression statements.

> **Note on `dr_files/` samples**: A small number of files (`test_bad_index.dr`,
> `test_const_assign.dr`, `test_good_index.dr`, `test_nonarray_index.dr`,
> `test_nonempty_array.dr`, `test_var_assign.dr`, `test_empty_array.dr`,
> `test_mixed_array.dr`) use trailing `;` on statements and omit type annotations. These
> appear to be semantic-error test fixtures using an older or informal syntax variant.
> The canonical grammar is defined by `input.dr`, `simple_test.dr`, `large_toy_program.dr`,
> and `break_continue_loops.dr`.

---

## Top-Level Program

```text
program       ::= statement* EOF
```

`parse()` returns `ParseResult(List<Stmt>, List<CompileError.SyntaxError>)`.  
A successful parse with no errors has `hasErrors() == false`.

---

## Statements

```text
statement     ::= funDecl
                | varDecl
                | mainBlock
                | ifStmt
                | whileStmt
                | forStmt
                | returnStmt
                | breakStmt
                | continueStmt
                | block
                | exprStmt

block         ::= '{' statement* '}'

funDecl       ::= 'fun' IDENT '(' paramList? ')' ':' type block
paramList     ::= param ( ',' param )*
param         ::= IDENT ':' type

varDecl       ::= ('var' | 'const') binding ( ',' binding )* ':' type ( '=' expression ( ',' expression )* )?
               |  ('var' | 'const') IDENT ( ',' IDENT )* '=' expression ( ',' expression )*
binding       ::= IDENT

mainBlock     ::= 'main' block

ifStmt        ::= 'if' expression block elseBranch?
elseBranch    ::= 'else' ( ifStmt | block )

whileStmt     ::= 'while' expression block

forStmt       ::= 'for' '(' forInit? ';' expression? ';' expression? ')' block
forInit       ::= varDecl | expression

returnStmt    ::= 'return' expression?
breakStmt     ::= 'break'
continueStmt  ::= 'continue'

exprStmt      ::= expression
```

> **`varDecl` forms observed in source samples**:
>
> Form 1 — typed, with initializer per binding:
> `var a: i64 = 1 + 4 - (12 + 3) / 3`
> `var a2, b2: i64 = 12, 21`
> `var arr: i64[5] = {1, 2, 3, 4, 5}`
>
> Form 2 — typed, no initializer (implicit default):
> `var i: i32` *(inside for-init)*
>
> Form 3 — untyped multi-binding with initializers (shorthand):
> `var a,b,c = 1,2,3`
>
> The `Stmt.VarDeclaration` record accepts `List<VarBinding>` (name + optional initializer),
> a `Type` annotation, and `isMutable`. Form 3 (no type annotation) maps to `Type.Custom("")`
> or a dedicated `Type.Inferred` sentinel — this is a design decision for `speckit.tasks`.
> Until resolved, the parser SHOULD accept Form 3 syntactically and defer type resolution
> to the semantic phase.

**AST mapping**:

| Grammar rule | Produced node |
|-------------|--------------|
| `funDecl` | `Stmt.Function` |
| `varDecl` (`var`) | `Stmt.VarDeclaration(isMutable=true)` |
| `varDecl` (`const`) | `Stmt.VarDeclaration(isMutable=false)` |
| `mainBlock` | `Stmt.MainFunction` |
| `ifStmt` (no else) | `Stmt.If(elseBranch=ElseBranch.Empty)` |
| `ifStmt` (else block) | `Stmt.If(elseBranch=ElseBranch.Else)` |
| `ifStmt` (else if) | `Stmt.If(elseBranch=ElseBranch.ElseIf)` |
| `whileStmt` | `Stmt.While` |
| `forStmt` | `Stmt.For` |
| `block` (standalone) | `Stmt.Block` |
| `returnStmt` | `Stmt.Return` |
| `breakStmt` | `Stmt.Break` |
| `continueStmt` | `Stmt.Continue` |
| `exprStmt` | `Stmt.Expression` |

---

## Types

```text
type          ::= 'i8' | 'i16' | 'i32' | 'i64'
                | 'u8' | 'u16' | 'u32' | 'u64'
                | 'f32' | 'f64'
                | 'char' | 'string' | 'bool'
                | IDENT                            // custom/user-defined
                | type '[' expression ']'          // fixed-size array (right-recursive)
                | 'vec' '<' type '>'               // dynamic vector (future)
                | 'void'
```

> **Array types are right-recursive**: `i8[2][3]` parses as `Array(Array(i8, 2), 3)`,
> matching the multi-dimensional matrix declarations observed in `input.dr`:
> `var matrix: i8[2][3] = {{1i8, 2i8, 3i8}, {4i8, 5i8, 6i8}}`

---

## Expressions (Pratt precedence, lowest → highest)

```text
expression    ::= assignExpr

assignExpr    ::= unary assignOp assignExpr         // right-assoc
                | logicalOr
assignOp      ::= '=' | '+=' | '-=' | '*=' | '/=' | '%='
                | '&=' | '|=' | '^=' | '<<=' | '>>='

logicalOr     ::= logicalAnd ( '||' logicalAnd )*
logicalAnd    ::= bitwiseOr  ( '&&' bitwiseOr  )*
bitwiseOr     ::= bitwiseXor ( '|'  bitwiseXor )*
bitwiseXor    ::= bitwiseAnd ( '^'  bitwiseAnd )*
bitwiseAnd    ::= equality   ( '&'  equality   )*
equality      ::= relational ( ( '==' | '!=' ) relational )*
relational    ::= shift      ( ( '<' | '<=' | '>' | '>=' ) shift )*
shift         ::= additive   ( ( '<<' | '>>' ) additive )*
additive      ::= multiplicative ( ( '+' | '-' ) multiplicative )*
multiplicative::= prefix     ( ( '*' | '/' | '%' ) prefix )*
prefix        ::= ( '-' | '!' | '~' | '++' | '--' ) prefix
                | postfix
postfix       ::= primary ( '++' | '--' | callSuffix | indexSuffix )*
callSuffix    ::= '(' argList? ')'
indexSuffix   ::= '[' expression ']'
argList       ::= expression ( ',' expression )*

primary       ::= NUMBER | FLOAT | BOOL | STRING | CHAR | 'nullptr'
                | IDENT
                | '(' expression ')'
                | '{' ( expression ( ',' expression )* ','? )? '}'   // array literal
```

> The grammar above is written in precedence-level form for documentation clarity.
> The implementation uses the Pratt loop with binding powers (see `data-model.md §BindingPower`),
> not this recursive form directly.

**AST mapping**:

| Grammar construct | Produced node |
|-------------------|--------------|
| `primary` — integer/float literal | `Expr.Literal(LiteralValue.Numeric)` |
| `primary` — bool | `Expr.Literal(LiteralValue.Bool)` |
| `primary` — string | `Expr.Literal(LiteralValue.StringLit)` |
| `primary` — char | `Expr.Literal(LiteralValue.CharLit)` |
| `primary` — nullptr | `Expr.Literal(LiteralValue.NullPtr)` |
| `primary` — identifier | `Expr.Variable` |
| `primary` — grouped `(e)` | `Expr.Grouping` |
| `primary` — array literal `{…}` | `Expr.ArrayLiteral` (empty elements for `{}`) |
| infix binary op | `Expr.Binary` |
| `=` assignment | `Expr.Assign(target, rhs)` |
| `op=` compound assignment | `Expr.Assign(target, Expr.Binary(target, baseOp, rhs))` |
| prefix unary | `Expr.Unary(op, UnaryOpSide.PREFIX, operand)` |
| postfix `++`/`--` | `Expr.Unary(op, UnaryOpSide.POSTFIX, operand)` |
| call `f(args)` | `Expr.Call(callee, args)` |
| index `a[i]` | `Expr.ArrayAccess(array, index)` |

---

## Operator Precedence Table (normative)

From lowest (1) to highest (13):

| Level | Operator(s) | Associativity | `left bp` / `right bp` |
|-------|-------------|---------------|------------------------|
| 1 | `=` `+=` `-=` `*=` `/=` `%=` `&=` `\|=` `^=` `<<=` `>>=` | Right | 10 / 9 |
| 2 | `\|\|` | Left | 20 / 21 |
| 3 | `&&` | Left | 30 / 31 |
| 4 | `\|` | Left | 40 / 41 |
| 5 | `^` | Left | 50 / 51 |
| 6 | `&` | Left | 60 / 61 |
| 7 | `==` `!=` | Left | 70 / 71 |
| 8 | `<` `<=` `>` `>=` | Left | 80 / 81 |
| 9 | `<<` `>>` | Left | 90 / 91 |
| 10 | `+` `-` | Left | 100 / 101 |
| 11 | `*` `/` `%` | Left | 110 / 111 |
| 12 | (prefix) `-` `!` `~` `++` `--` | Right (nud) | — / 120 |
| 13 | (postfix) `++` `--`, call `(`, index `[` | Left (led) | 130 / — |

---

## Error Contract

When the parser encounters an unexpected token, it must produce a
`CompileError.SyntaxError` with:

- `errorSpan`: the span of the unexpected token (or the span of the last consumed token
  if at EOF).
- `errorMessage`: a human-readable string that names the expected token or construct and
  the token actually found. Examples:
    - `"Expected ':' after variable name, found 'i32'"`
    - `"Expected ')' to close function call, found '}'"` 
    - `"Unexpected end of file after '+'"`  (EOF mid-expression)
- `errorCode`: `Optional.of(ErrorCode.E1005)` where applicable; `Optional.empty()` for
  parser-specific errors without a matching code.
- `errorHelp`: `Optional.empty()` unless a corrective suggestion is obvious.

After emitting the error, `synchronize()` advances past the next `}` (block boundary).
Because there are no semicolons as statement terminators, `}` is the primary
synchronization point; the parser may also synchronize at the next statement-opening
keyword (`fun`, `var`, `const`, `if`, `while`, `for`, `return`, `break`, `continue`,
`main`) as a secondary heuristic.

---

## Comment Filtering Contract

All tokens with `TokenKind.Simple.Special.COMMENT` or
`TokenKind.Simple.Special.MULTILINE_COMMENT` are **invisible** to all grammar rules.
They are filtered by `TokenCursor` during construction and never appear in any
grammar rule's lookahead or consume operations. They never cause a parse error.
