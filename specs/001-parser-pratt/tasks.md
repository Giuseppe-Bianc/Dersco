# Tasks: Recursive Descent + Pratt Parser

**Branch**: `001-parser-pratt` | **Input**: `specs/001-parser-pratt/`

**Prerequisites**: `plan.md`, `spec.md`, `data-model.md`, `contracts/grammar.md`, `research.md`

**TDD Mandatory** (Constitution V): for every task marked `[TEST]` write the test first,
run it and observe RED, then implement until GREEN, then refactor. Never skip the RED step.

**Format**: `- [ ] TXXX [P?] [USX?] Description — file path`
- `[P]` = parallelizable (independent file, no incomplete dependency)
- `[USX]` = belongs to User Story X
- All paths are relative to repository root

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create the package skeleton and the two pure-value types that have zero
dependencies on other new parser code. Both can be written and tested in parallel before
any parsing logic exists.

- [ ] T001 Create package declaration file `package-info.java` for `org.dersbian.compiler.syntax`
  that documents the parser phase boundary (FR-009). The file must contain only the
  `@SuppressWarnings` annotation required by Checkstyle and a one-sentence Javadoc explaining
  that this package owns the parsing phase and must not import `compiler.semantic` or
  `compiler.codegen` packages. —
  `app/src/main/java/org/dersbian/compiler/syntax/package-info.java`

- [ ] T002 [P] Create `ParseResult.java` as a `public record` in package
  `org.dersbian.compiler.syntax`. Fields: `List<Stmt> statements` (unmodifiable copy via
  `List.copyOf`), `List<CompileError.SyntaxError> errors` (unmodifiable copy via `List.copyOf`).
  Compact constructor must null-check both parameters with descriptive messages matching the
  style of `LexerResult`. Add a `public boolean hasErrors()` convenience method that returns
  `!errors.isEmpty()`. Add full Javadoc on the record, both components, and `hasErrors()`.
  No `@SuppressWarnings` unless Checkstyle demands it. —
  `app/src/main/java/org/dersbian/compiler/syntax/ParseResult.java`

- [ ] T003 [P] [TEST] Write `ParseResultTest` in package `org.dersbian.compiler.syntax`.
  Class must be package-private (no `public` modifier). Use JUnit Jupiter `@Test` and AssertJ
  `assertThat`. Required test methods (each annotated `@Test`, named descriptively):
  (1) `emptyParseResultHasNoErrorsAndNoStatements` — constructs `new ParseResult(List.of(), List.of())`,
  asserts `hasErrors()` is `false`, `errors()` is empty, `statements()` is empty;
  (2) `parseResultWithErrorsReportsHasErrors` — constructs result with one synthetic
  `CompileError.SyntaxError` (use `CompileError.syntaxError(null, "test", Span.point(new SourceLocation(0,1,1)), null)`),
  asserts `hasErrors()` is `true`, `errors().size()` is 1;
  (3) `statementsListIsUnmodifiable` — asserts that `statements()` throws
  `UnsupportedOperationException` on `add()`;
  (4) `errorsListIsUnmodifiable` — asserts that `errors()` throws
  `UnsupportedOperationException` on `add()`;
  (5) `nullStatementsThrowsNullPointerException` — asserts `NullPointerException` when
  `statements` argument is `null`;
  (6) `nullErrorsThrowsNullPointerException` — asserts `NullPointerException` when
  `errors` argument is `null`.
  Run RED: `.\gradlew.bat :app:test --tests "*ParseResultTest*"` must FAIL (class does not exist yet). —
  `app/src/test/java/org/dersbian/compiler/syntax/ParseResultTest.java`

- [ ] T004 [P] Create `BindingPower.java` as a package-private `record BindingPower(int left, int right)`
  in package `org.dersbian.compiler.syntax`. Add three `static` factory methods:
  `static Optional<BindingPower> infix(TokenKind kind)` — returns the infix (led) binding
  power for the given `TokenKind` using a `switch` expression that covers every infix operator
  in FR-004 exactly as listed in `contracts/grammar.md §Operator Precedence Table`:
    - `EQUAL`, `PLUS_EQUAL`, `MINUS_EQUAL`, `STAR_EQUAL`, `SLASH_EQUAL`, `PERCENT_EQUAL`,
      `AND_EQUAL`, `OR_EQUAL`, `XOR_EQUAL`, `SHIFT_LEFT_EQUAL`, `SHIFT_RIGHT_EQUAL` → `new BindingPower(10, 9)` (right-assoc)
    - `OR_OR` → `new BindingPower(20, 21)`
    - `AND_AND` → `new BindingPower(30, 31)`
    - `OR` → `new BindingPower(40, 41)`
    - `XOR` → `new BindingPower(50, 51)`
    - `AND` → `new BindingPower(60, 61)`
    - `EQUAL_EQUAL`, `NOT_EQUAL` → `new BindingPower(70, 71)`
    - `LESS`, `LESS_EQUAL`, `GREATER`, `GREATER_EQUAL` → `new BindingPower(80, 81)`
    - `SHIFT_LEFT`, `SHIFT_RIGHT` → `new BindingPower(90, 91)`
    - `PLUS`, `MINUS` → `new BindingPower(100, 101)`
    - `STAR`, `SLASH`, `PERCENT` → `new BindingPower(110, 111)`
    - `PLUS_PLUS`, `MINUS_MINUS`, `OPEN_PAREN`, `OPEN_BRACKET` → `new BindingPower(130, 130)` (postfix/call/index — left value only, right unused)
    - all other → `Optional.empty()`
  `static OptionalInt prefix(TokenKind kind)` — returns the right-bp for prefix operators:
    - `MINUS`, `NOT`, `BITWISE_NOT`, `PLUS_PLUS`, `MINUS_MINUS` → `OptionalInt.of(120)`
    - all other → `OptionalInt.empty()`
  All three methods must be `@SuppressWarnings("PMD.CyclomaticComplexity")` if the switch
  triggers that rule. Full Javadoc on class and each method. —
  `app/src/main/java/org/dersbian/compiler/syntax/BindingPower.java`

- [ ] T005 [P] [TEST] Write `BindingPowerTest` in package `org.dersbian.compiler.syntax`.
  Class must be package-private. Required test methods:
  (1) `assignmentOperatorsAreRightAssociative` — for each of `EQUAL`, `PLUS_EQUAL`, `MINUS_EQUAL`,
  `STAR_EQUAL`, `SLASH_EQUAL`, `PERCENT_EQUAL`, `AND_EQUAL`, `OR_EQUAL`, `XOR_EQUAL`,
  `SHIFT_LEFT_EQUAL`, `SHIFT_RIGHT_EQUAL`: assert `infix(op)` is present AND `left=10, right=9`;
  (2) `logicalOrBindingPower` — assert `infix(OR_OR)` is present with `left=20, right=21`;
  (3) `logicalAndBindingPower` — assert `infix(AND_AND)` is present with `left=30, right=31`;
  (4) `bitwiseOrBindingPower` — assert `infix(OR)` is present with `left=40, right=41`;
  (5) `bitwiseXorBindingPower` — assert `infix(XOR)` is present with `left=50, right=51`;
  (6) `bitwiseAndBindingPower` — assert `infix(AND)` is present with `left=60, right=61`;
  (7) `equalityOperatorsBindingPower` — assert `infix(EQUAL_EQUAL)` and `infix(NOT_EQUAL)` each have `left=70, right=71`;
  (8) `relationalOperatorsBindingPower` — assert `infix(LESS)`, `infix(LESS_EQUAL)`, `infix(GREATER)`, `infix(GREATER_EQUAL)` each have `left=80, right=81`;
  (9) `shiftOperatorsBindingPower` — assert `infix(SHIFT_LEFT)` and `infix(SHIFT_RIGHT)` each have `left=90, right=91`;
  (10) `additiveOperatorsBindingPower` — assert `infix(PLUS)` and `infix(MINUS)` each have `left=100, right=101`;
  (11) `multiplicativeOperatorsBindingPower` — assert `infix(STAR)`, `infix(SLASH)`, `infix(PERCENT)` each have `left=110, right=111`;
  (12) `postfixAndCallAndIndexBindingPower` — assert `infix(PLUS_PLUS)`, `infix(MINUS_MINUS)`, `infix(OPEN_PAREN)`, `infix(OPEN_BRACKET)` each have `left=130`;
  (13) `prefixOperatorsHaveRightBp120` — for each of `MINUS`, `NOT`, `BITWISE_NOT`, `PLUS_PLUS`, `MINUS_MINUS`: assert `prefix(op)` is present with value `120`;
  (14) `nonOperatorTokenHasNoInfixBp` — assert `infix(TokenKind.Simple.Keyword.FUN)` is empty;
  (15) `nonPrefixTokenHasNoPrefixBp` — assert `prefix(TokenKind.Simple.Keyword.FUN)` is empty.
  Run RED: `.\gradlew.bat :app:test --tests "*BindingPowerTest*"` must FAIL. —
  `app/src/test/java/org/dersbian/compiler/syntax/BindingPowerTest.java`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: `TokenCursor` is the single cursor shared by all parsers. It must be fully
implemented and tested before `ExpressionParser`, `StatementParser`, or `Parser` can be
written. Nothing in Phase 3+ can start until T006–T010 are complete.

**⚠️ CRITICAL**: Phases 3–6 are blocked until this phase is complete.

- [ ] T006 [TEST] Write `TokenCursorTest` in package `org.dersbian.compiler.syntax`.
  Class must be package-private. Build helper: add a `private static List<Token> tokens(TokenKind... kinds)`
  method that creates `Token` instances using `Token.create(new SourceId.Generated("test"), kind, Span.point(new SourceLocation(0,1,1)))`.
  Required test methods:
  (1) `peekReturnsFirstNonCommentToken` — given tokens `[COMMENT, PLUS]`, `peek()` returns the `PLUS` token (comment filtered);
  (2) `advanceConsumesAndReturnsToken` — given `[PLUS, MINUS]`, first `advance()` returns `PLUS`, second returns `MINUS`;
  (3) `isAtEndTrueWhenOnlyEofRemains` — given `[EOF]`, `isAtEnd()` is `true`;
  (4) `isAtEndFalseWhenTokensRemain` — given `[PLUS, EOF]`, `isAtEnd()` is `false`;
  (5) `checkReturnsTrueForMatchingKind` — given `[PLUS]`, `check(PLUS)` is `true`;
  (6) `checkReturnsFalseForNonMatchingKind` — given `[PLUS]`, `check(MINUS)` is `false`;
  (7) `expectAdvancesOnMatch` — given `[PLUS]`, `expect(PLUS, errors)` returns the `PLUS` token and errors list is empty;
  (8) `expectAddsErrorOnMismatch` — given `[MINUS]`, `expect(PLUS, errors)` adds one `CompileError.SyntaxError` to errors;
  (9) `multilineCommentIsFiltered` — given `[MULTILINE_COMMENT, STAR]`, `peek()` returns `STAR`;
  (10) `synchronizeSkipsToBrace` — given `[PLUS, MINUS, CLOSE_BRACE, STAR]`, after calling `synchronize(errors)`, `peek()` is the `STAR` token (cursor positioned after `}`);
  (11) `synchronizeStopsAtStatementKeyword` — given `[PLUS, MINUS, FUN]`, after `synchronize(errors)`, `peek()` is `FUN` (keyword is NOT consumed);
  (12) `currentSpanReturnsSpanOfPeekToken` — given `[PLUS]`, `currentSpan()` equals the span of the `PLUS` token;
  (13) `peekOnEmptyStreamReturnsEof` — given only `[EOF]`, `peek()` returns an EOF token without throwing;
  (14) `advancePastEofReturnsEof` — given `[EOF]`, calling `advance()` twice does not throw and both calls return EOF.
  Run RED: `.\gradlew.bat :app:test --tests "*TokenCursorTest*"` must FAIL. —
  `app/src/test/java/org/dersbian/compiler/syntax/TokenCursorTest.java`

- [ ] T007 Create `TokenCursor.java` as a package-private `final class` in package
  `org.dersbian.compiler.syntax`. Fields: `private final List<Token> tokens` (comment-filtered
  immutable copy built in constructor), `private int pos = 0`. Constructor
  `TokenCursor(List<Token> tokens)`: null-check the input list, then build `this.tokens` by
  streaming the input, filtering out `TokenKind.Simple.Special.COMMENT` and
  `TokenKind.Simple.Special.MULTILINE_COMMENT`, and collecting to an unmodifiable `List`.
  If the filtered list is empty or its last element is not `EOF`, append a synthetic
  `Token.eof(...)` using the last available token's `sourceId` and `span.end()` wrapped in
  `Span.point(...)` — this guarantees `peek()` is always safe.
  Method contracts (all `@SuppressWarnings` as needed for PMD):

    - `Token peek()` — returns `tokens.get(pos)` without advancing; never throws.
    - `Token advance()` — returns `tokens.get(pos)` then increments `pos` only if `pos < tokens.size() - 1`
    (i.e., never advance past the last EOF).
    - `boolean check(TokenKind expected)` — returns `peek().type().equals(expected)`.
    - `boolean checkAny(TokenKind... expected)` — returns `true` if `peek().type()` matches any in the vararg array; useful in `synchronize`.
    - `Token expect(TokenKind expected, List<CompileError.SyntaxError> errors)` — if `check(expected)`, call `advance()` and return the consumed token; else add a `CompileError.syntaxError(ErrorCode.E1006, "Expected " + expected + " but found " + peek().type(), peek().span(), null)` to the errors list and return `peek()` without consuming.
    - `void synchronize(List<CompileError.SyntaxError> errors)` — advance while `!isAtEnd()` AND the current token is not `CLOSE_BRACE` AND the current token is not one of `{FUN, VAR, CONST, IF, WHILE, FOR, RETURN, BREAK, CONTINUE, MAIN}`; after the loop, if `check(CLOSE_BRACE)` call `advance()` to consume it.
    - `boolean isAtEnd()` — returns `peek().type().equals(TokenKind.Simple.Special.EOF)`.
    - `Span currentSpan()` — returns `peek().span()`.
  Full Javadoc on class and every public/package method. Apply `spotlessApply` after writing. —
  `app/src/main/java/org/dersbian/compiler/syntax/TokenCursor.java`

- [ ] T008 Run GREEN for T006: `.\gradlew.bat :app:test --tests "*TokenCursorTest*"` must now
  pass. Fix any failures in `TokenCursor.java` until all 14 tests pass. Then run
  `.\gradlew.bat :app:spotlessApply` and `.\gradlew.bat :app:spotlessCheck` to confirm formatting.
  Do NOT proceed to T009 until this step is GREEN.

- [ ] T009 Run GREEN for T003 and T005: `.\gradlew.bat :app:test --tests "*ParseResultTest*"` and
  `.\gradlew.bat :app:test --tests "*BindingPowerTest*"` must both pass. Fix any failures in
  `ParseResult.java` or `BindingPower.java`. Then run `.\gradlew.bat :app:spotlessApply`.
  Do NOT proceed to Phase 3 until all foundational tests are GREEN.

- [ ] T010 Run the full quality gate for foundational classes:
  `.\gradlew.bat :app:check`. All Checkstyle, PMD, SpotBugs, Error Prone, Spotless, and JaCoCo
  gates must pass for the three new files (`ParseResult`, `TokenCursor`, `BindingPower`) and
  their tests. Fix all violations before proceeding. This is the **Phase 2 checkpoint** —
  record `FOUNDATION READY` as a comment in the PR/commit message.

---

## Phase 3: User Story 1 + 2 — Expression Parsing & Pratt Engine (Priority: P1) 🎯 MVP

**Goal (US1)**: The parser can process a complete token stream and return a `ParseResult`
with top-level `Stmt` nodes. **Goal (US2)**: Every operator is grouped according to the
13-level precedence table; associativity is correct for all levels.

**Why combined**: `ExpressionParser` is prerequisite for all statement rules that contain
expressions (var initializers, if conditions, while conditions, for clauses, return values,
expression statements). US1 and US2 share the same production class; they are tested by
separate test classes.

**Independent Test (US2)**: `.\gradlew.bat :app:test --tests "*ExpressionParserTest*"` covers
every precedence level and every associativity direction without requiring statement parsing.

**Independent Test (US1)**: `.\gradlew.bat :app:test --tests "*ParserTest*"` parses
`large_toy_program.dr` and verifies the top-level node count and types.

### TDD Cycle — ExpressionParser (US2 first, US1 integration after)

- [ ] T011 [TEST] [US2] Write `ExpressionParserTest` in package `org.dersbian.compiler.syntax`.
  Class must be package-private. Add a private helper method:
  `private ExpressionParser parserFor(String source)` that: (a) uses the real `Lexer` to
  tokenize `source` appended with a newline, (b) wraps the resulting `List<Token>` in a
  `TokenCursor`, (c) creates a mutable `ArrayList<CompileError.SyntaxError> errors`, (d)
  constructs and returns an `ExpressionParser(cursor, errors)`. Also add
  `private List<CompileError.SyntaxError> errors` as a field updated after each parse.
  Required test methods — **each must be a separate `@Test` method**:

  **Literals**:
  (1) `integerLiteralProducesExprLiteral` — parse `"1i32"`, assert result is `Expr.Literal`,
  assert `((Expr.Literal) result).value()` is `LiteralValue.Numeric`;
  (2) `boolTrueLiteralProducesExprLiteralBoolTrue` — parse `"true"`, assert `LiteralValue.Bool(true)`;
  (3) `boolFalseLiteralProducesExprLiteralBoolFalse` — parse `"false"`, assert `LiteralValue.Bool(false)`;
  (4) `stringLiteralProducesStringLit` — parse `"\"hello\""`, assert `LiteralValue.StringLit("hello")`;
  (5) `charLiteralProducesCharLit` — parse `"'a'"`, assert `LiteralValue.CharLit("a")`;
  (6) `nullptrLiteralProducesNullPtr` — parse `"nullptr"`, assert `LiteralValue.NullPtr`;
  (7) `identifierProducesExprVariable` — parse `"myVar"`, assert result is `Expr.Variable` with `name="myVar"`;
  (8) `groupingProducesExprGrouping` — parse `"(1i32)"`, assert result is `Expr.Grouping`;

  **Additive**:
  (9) `additionProducesBinaryAdd` — parse `"1i32 + 2i32"`, assert `Expr.Binary` with `op=BinaryOp.ADD`;
  (10) `subtractionProducesBinarySubtract` — parse `"5i32 - 3i32"`, assert `BinaryOp.SUBTRACT`;

  **Multiplicative beats additive**:
  (11) `multiplicationBindsTighterThanAddition` — parse `"1i32 + 2i32 * 3i32"`, assert outer is
  `Expr.Binary(_, ADD, _)` and the right child is `Expr.Binary(_, MULTIPLY, _)`;

  **Left-associativity**:
  (12) `subtractionIsLeftAssociative` — parse `"a - b - c"`, assert outer is
  `Binary(Binary(Variable(a), SUBTRACT, Variable(b)), SUBTRACT, Variable(c))`;
  (13) `additionIsLeftAssociative` — parse `"a + b + c"`, assert outer is
  `Binary(Binary(Variable(a), ADD, Variable(b)), ADD, Variable(c))`;

  **Right-associativity — assignments (SC-003)**:
  (14) `simpleAssignmentIsRightAssociative` — parse `"a = b = c"`, assert outer is
  `Assign(Variable(a), Assign(Variable(b), Variable(c)))`;
  (15) `compoundAssignPlusDesugarsToBinaryAdd` — parse `"a += 5i32"`, assert result is
  `Expr.Assign` whose `value()` is `Expr.Binary` with `op=BinaryOp.ADD`;
  (16) `compoundAssignMinusDesugarsToBinarySubtract` — parse `"a -= 5i32"`, assert desugaring to `SUBTRACT`;
  (17) `compoundAssignStarDesugarsToBinaryMultiply` — parse `"a *= 5i32"`, assert desugaring to `MULTIPLY`;
  (18) `compoundAssignSlashDesugarsToBinaryDivide` — parse `"a /= 5i32"`, assert desugaring to `DIVIDE`;
  (19) `compoundAssignPercentDesugarsToBinaryModulo` — parse `"a %= 5i32"`, assert desugaring to `MODULO`;
  (20) `compoundAssignAndEqualDesugarsToBitwiseAnd` — parse `"a &= 5i32"`, assert desugaring to `BITWISE_AND`;
  (21) `compoundAssignOrEqualDesugarsToBitwiseOr` — parse `"a |= 5i32"`, assert desugaring to `BITWISE_OR`;
  (22) `compoundAssignXorEqualDesugarsToBitwiseXor` — parse `"a ^= 5i32"`, assert desugaring to `BITWISE_XOR`;
  (23) `compoundAssignShiftLeftEqualDesugarsToShiftLeft` — parse `"a <<= 1i32"`, assert desugaring to `SHIFT_LEFT`;
  (24) `compoundAssignShiftRightEqualDesugarsToShiftRight` — parse `"a >>= 1i32"`, assert desugaring to `SHIFT_RIGHT`;

  **Cross-level precedence (SC-002)**:
  (25) `andBindsTighterThanOr` — parse `"a && b || c"`, assert outer is
  `Binary(Binary(Variable(a), AND, Variable(b)), OR, Variable(c))`;
  (26) `bitwiseOrBindsTighterThanLogicalOr` — parse `"a | b || c"`, assert outer is
  `Binary(Binary(a, BITWISE_OR, b), OR, c)`;
  (27) `bitwiseXorBindsTighterThanBitwiseOr` — parse `"a ^ b | c"`, assert outer is
  `Binary(Binary(a, BITWISE_XOR, b), BITWISE_OR, c)`;
  (28) `bitwiseAndBindsTighterThanBitwiseXor` — parse `"a & b ^ c"`, assert outer is
  `Binary(Binary(a, BITWISE_AND, b), BITWISE_XOR, c)`;
  (29) `equalityBindsTighterThanBitwiseAnd` — parse `"a == b & c"`, assert outer is
  `Binary(Binary(a, EQUAL, b), BITWISE_AND, c)`;
  (30) `relationalBindsTighterThanEquality` — parse `"a < b == c"`, assert outer is
  `Binary(Binary(a, LESS, b), EQUAL, c)`;
  (31) `shiftBindsTighterThanAdditive` — parse `"a + b << c"`, assert outer is
  `Binary(Binary(a, ADD, b), SHIFT_LEFT, c)`;

  **Unary**:
  (32) `prefixNegateProducesUnaryNegate` — parse `"-a"`, assert `Expr.Unary` with `op=UnaryOp.NEGATE, side=UnaryOpSide.PREFIX`;
  (33) `prefixNotProducesUnaryNot` — parse `"!a"`, assert `UnaryOp.NOT, PREFIX`;
  (34) `prefixBitwiseNotProducesUnaryBitwiseNot` — parse `"~a"`, assert `UnaryOp.BITWISE_NOT, PREFIX`;
  (35) `prefixIncrementProducesUnaryIncrement` — parse `"++a"`, assert `UnaryOp.INCREMENT, PREFIX`;
  (36) `prefixDecrementProducesUnaryDecrement` — parse `"--a"`, assert `UnaryOp.DECREMENT, PREFIX`;
  (37) `postfixIncrementProducesUnaryIncrementPostfix` — parse `"a++"`, assert `UnaryOp.INCREMENT, POSTFIX`;
  (38) `postfixDecrementProducesUnaryDecrementPostfix` — parse `"a--"`, assert `UnaryOp.DECREMENT, POSTFIX`;
  (39) `chainedPrefixUnaryRightAssociative` — parse `"!-a"`, assert outer is
  `Unary(NOT, PREFIX, Unary(NEGATE, PREFIX, Variable(a)))`;

  **Call and Index**:
  (40) `functionCallProducesExprCall` — parse `"f()"`, assert result is `Expr.Call` with empty arguments;
  (41) `functionCallWithArgsProducesExprCall` — parse `"f(1i32, 2i32)"`, assert `Expr.Call` with 2 arguments;
  (42) `arrayIndexProducesExprArrayAccess` — parse `"a[0i32]"`, assert result is `Expr.ArrayAccess`;
  (43) `emptyArrayLiteralProducesExprArrayLiteralEmpty` — parse `"{}"`, assert `Expr.ArrayLiteral` with empty elements;
  (44) `nonEmptyArrayLiteralProducesExprArrayLiteralWithElements` — parse `"{1i32, 2i32, 3i32}"`, assert `Expr.ArrayLiteral` with 3 elements;

  **Edge cases**:
  (45) `eofMidExpressionAddsError` — create cursor with tokens `[PLUS, EOF]`, call `parseExpression(0)`,
  assert `errors` is non-empty (unexpected token at start of primary);
  (46) `deeplyNestedGroupingDoesNotStackOverflow` — build a string of 50 nested parentheses wrapping `"1i32"`,
  parse it, assert no exception is thrown and result is an `Expr.Grouping` (recursion depth bounded by nesting, not operator count).

  Run RED: `.\gradlew.bat :app:test --tests "*ExpressionParserTest*"` must FAIL. —
  `app/src/test/java/org/dersbian/compiler/syntax/ExpressionParserTest.java`

- [ ] T012 [US2] Create `ExpressionParser.java` as a package-private `final class` in package
  `org.dersbian.compiler.syntax`. Constructor:
  `ExpressionParser(TokenCursor cursor, List<CompileError.SyntaxError> errors)` — store both
  as `private final` fields; null-check both.

  Method `Expr parseExpression(int minBp)` — implements the Pratt loop:
  1. Call `parsePrimary()` to obtain `lhs`.
  2. Loop: call `BindingPower.infix(cursor.peek().type())`.
     - If empty OR `bp.left() < minBp`, break.
     - If the operator is a postfix/call/index (`PLUS_PLUS`, `MINUS_MINUS`, `OPEN_PAREN`, `OPEN_BRACKET`): handle as led (see below) without recursing with a right-bp.
     - Otherwise: consume the operator token via `cursor.advance()`, recursively call `parseExpression(bp.right())` to get `rhs`, then wrap: if the operator is an assignment op (`EQUAL`, `PLUS_EQUAL`, etc.) construct the correct `Expr.Assign` or desugared `Expr.Assign(lhs, Expr.Binary(lhs, baseOp, rhs, span), span)`; else construct `Expr.Binary(lhs, BinaryOp.getOp(opToken), rhs, span)` where `span = lhs.span().merge(rhs.span())`.
     - For postfix `PLUS_PLUS`/`MINUS_MINUS`: consume token, wrap `lhs` in `Expr.Unary(UnaryOp.INCREMENT/DECREMENT, UnaryOpSide.POSTFIX, lhs, span)`.
     - For call `OPEN_PAREN`: consume `(`, parse zero or more comma-separated expressions until `)`, consume `)` via `expect`, wrap in `Expr.Call(lhs, args, span)`.
     - For index `OPEN_BRACKET`: consume `[`, parse one expression, consume `]` via `expect`, wrap in `Expr.ArrayAccess(lhs, index, span)`.
  3. Return `lhs`.

  Method `Expr parsePrimary()` — handles nud (null-denotation):

    - `TokenKind.Numeric`, `TokenKind.Binary`, `TokenKind.Octal`, `TokenKind.Hexadecimal` → advance, wrap in  `Expr.Literal(new LiteralValue.Numeric(kind.value()), token.span())`.
    - `TokenKind.KeywordBool` → advance, wrap in `Expr.Literal(new LiteralValue.Bool(kind.value()), token.span())`.
    - `TokenKind.StringLiteral` → advance, wrap in `Expr.Literal(new LiteralValue.StringLit(kind.value()), token.span())`.
    - `TokenKind.CharLiteral` → advance, wrap in `Expr.Literal(new LiteralValue.CharLit(kind.value()), token.span())`.
    - `Keyword.NULLPTR` → advance, wrap in `Expr.Literal(new LiteralValue.NullPtr(), token.span())`.
    - `IdentifierAscii` or `IdentifierUnicode` → advance, wrap in `Expr.Variable(kind.value(), token.span())`.
    - `Delimiter.OPEN_PAREN` → advance, parse inner expression with `parseExpression(0)`, expect `CLOSE_PAREN`, wrap in `Expr.Grouping(inner, openToken.span().merge(closeToken.span()))`.
    - `Delimiter.OPEN_BRACE` → advance, parse comma-separated expressions until `CLOSE_BRACE` or EOF, expect `CLOSE_BRACE`, wrap in `Expr.ArrayLiteral(elements, openToken.span().merge(closeToken.span()))`.
    - Prefix operators (`MINUS`, `NOT`, `BITWISE_NOT`, `PLUS_PLUS`, `MINUS_MINUS`) → advance, get right-bp from `BindingPower.prefix(kind)`, recursively call `parseExpression(rightBp)`, wrap in `Expr.Unary(op, UnaryOpSide.PREFIX, operand, opToken.span().merge(operand.span()))`.
    - Anything else → add `CompileError.syntaxError(ErrorCode.E1006, "Unexpected token in expression: " + peek.type(), peek.span(), null)` to errors, advance (recovery), return `Expr.nullExpr(peek.span())`.

  For compound assignment desugaring: when the infix operator is `PLUS_EQUAL` through `SHIFT_RIGHT_EQUAL`, the `baseOp` is determined by stripping the `_EQUAL` suffix (e.g., `PLUS_EQUAL` → `BinaryOp.ADD`). Use a private static helper method `private static BinaryOp baseOp(BinaryOp compound)` with a switch expression.

  Add `@SuppressWarnings` as required by PMD/Checkstyle for cyclomatic complexity and method length. Full Javadoc. —
  `app/src/main/java/org/dersbian/compiler/syntax/ExpressionParser.java`

- [ ] T013 [US2] Run GREEN for T011:
  `.\gradlew.bat :app:test --tests "*ExpressionParserTest*"` — all 46 tests must pass.
  Fix failures in `ExpressionParser.java`. Then run `.\gradlew.bat :app:spotlessApply`.
  Do NOT proceed to T014 until fully GREEN.

- [ ] T014 [P] [TEST] [US1] Write `ParserTest` in package `org.dersbian.compiler.syntax`.
  Class package-private. Add helper:
  `private ParseResult parseFile(Path drFile)` that reads the file, runs `Lexer`, then
  `new Parser(tokens, drFile).parse()`.
  Required test methods:
  (1) `emptyMainBlockParsesWithNoErrors` — inline source `"main { }"`, assert `result.hasErrors()` is `false`, `statements` has exactly 1 element which is `Stmt.MainFunction`, its `body.statements()` is empty;
  (2) `largeToyProgramParsesWithNoErrors` — `parseFile(Path.of("dr_files/large_toy_program.dr"))`, assert `hasErrors()` is `false`, assert `statements().stream().anyMatch(s -> s instanceof Stmt.MainFunction)`, assert `statements().stream().filter(s -> s instanceof Stmt.Function).count() >= 1`;
  (3) `simpleToyParsesWithNoErrors` — `parseFile(Path.of("dr_files/simple_test.dr"))`, assert `hasErrors()` is `false`;
  (4) `inputDrParsesWithNoErrors` — `parseFile(Path.of("dr_files/input.dr"))`, assert `hasErrors()` is `false`;
  (5) `breakContinueLoopsParsesWithNoErrors` — `parseFile(Path.of("dr_files/break_continue_loops.dr"))`, assert `hasErrors()` is `false`;
  (6) `testForFileParsesWithNoErrors` — `parseFile(Path.of("dr_files/test_for.dr"))`, assert `hasErrors()` is `false`;
  (7) `parseLargeToyProgramUnder100ms` — load token stream first (outside timing), then time only `parser.parse()`, assert elapsed `< 100_000_000L` nanoseconds (100 ms);
  (8) `parseResultStatementsListIsUnmodifiable` — parse any source, assert `statements()` throws on `add()`.
  Run RED: `.\gradlew.bat :app:test --tests "*ParserTest*"` must FAIL (`Parser` class does not exist). —
  `app/src/test/java/org/dersbian/compiler/syntax/ParserTest.java`

---

## Phase 4: User Story 3 — Statement Parsing (Priority: P2)

**Goal**: Every statement construct in the grammar (`fun`, `var`/`const`, `main`, `if`/`else if`/`else`,
`while`, `for`, `return`, `break`, `continue`, standalone `block`, expression statement) is parsed
into the correct `Stmt` variant with correct fields.

**Dependency**: Requires `ExpressionParser` (Phase 3) fully GREEN before starting.

**Independent Test**: `.\gradlew.bat :app:test --tests "*StatementParserTest*"` verifies all
`Stmt` variants in isolation without a full file.

### TDD Cycle — StatementParser (US3)

- [ ] T015 [TEST] [US3] Write `StatementParserTest` in package `org.dersbian.compiler.syntax`.
  Class package-private. Helper: `private Stmt parseStatement(String source)` — tokenize `source`,
  build full `Parser`, call `parse()`, return `statements().get(0)` (assert list non-empty first).
  Also `private ParseResult parseAll(String source)` for multi-statement cases.

  Required test methods — each annotated `@Test`:

  **Expression statements**:
  (1) `expressionStatementWrapsExpression` — source `"a"`, assert result is `Stmt.Expression`, inner `expr()` is `Expr.Variable`;

  **var declarations**:
  (2) `varDeclarationIsMutable` — source `"var x: i32 = 5i32"`, assert `Stmt.VarDeclaration` with `isMutable=true`, `bindings().size()=1`, binding name `"x"`, initializer present, `typeAnnotation()` is `Type.I32`;
  (3) `constDeclarationIsNotMutable` — source `"const y: bool = true"`, assert `isMutable=false`, `typeAnnotation()` is `Type.Bool`;
  (4) `varDeclarationWithoutInitializer` — source `"var i: i32"`, assert `isMutable=true`, binding initializer `Optional.empty()`;
  (5) `varDeclarationMultipleBindings` — source `"var a, b: i64 = 12, 21"`, assert `bindings().size()=2`, first name `"a"`, second name `"b"`, each initializer present;
  (6) `varDeclarationArrayType` — source `"var arr: i64[5] = {1, 2, 3, 4, 5}"`, assert `typeAnnotation()` is `Type.Array`;

  **fun declarations**:
  (7) `funDeclarationNoParams` — source `"fun f(): void { }"`, assert `Stmt.Function`, `name()="f"`, `parameters().isEmpty()`, `returnType()` is `Type.VoidT`, `body().statements().isEmpty()`;
  (8) `funDeclarationWithParams` — source `"fun add(a: i32, b: i32): i32 { return a }"`,
  assert `Stmt.Function`, `parameters().size()=2`, first param `name="a"` `typeAnnotation=Type.I32`, second `name="b"` `typeAnnotation=Type.I32`, `returnType()=Type.I32`;
  (9) `funDeclarationBodyHasStatements` — source `"fun f(): void { var x: i32 = 1i32 }"`,
  assert `body().statements().size()=1`, that statement is `Stmt.VarDeclaration`;

  **main block**:
  (10) `mainBlockProducesStmtMainFunction` — source `"main { }"`, assert `Stmt.MainFunction`, `body().statements().isEmpty()`;
  (11) `mainBlockWithBody` — source `"main { var x: i32 = 1i32 }"`, assert `body().statements().size()=1`;

  **if / else if / else**:
  (12) `ifWithNoElseProducesElseBranchNone` — source `"if (true) { }"`, assert `Stmt.If`,
  `elseBranch()` is `ElseBranch.None`;
  (13) `ifWithElseBlockProducesElseBranchBlock` — source `"if (true) { } else { }"`,
  assert `elseBranch()` is `ElseBranch.Block`;
  (14) `ifWithElseIfProducesElseBranchElseIf` — source `"if (a == 1) { } else if (a == 2) { }"`,
  assert `elseBranch()` is `ElseBranch.ElseIf`, the nested `ElseBranch.ElseIf.ifStmt()`
  is a `Stmt.If` whose `elseBranch()` is `ElseBranch.None`;
  (15) `ifElseIfElseChain` — source `"if (a == 1) { } else if (a == 2) { } else { }"`,
  assert outer `elseBranch()` is `ElseBranch.ElseIf`, innermost `elseBranch()` is `ElseBranch.Block`;

  **while**:
  (16) `whileLoopProducesStmtWhile` — source `"while (true) { break }"`, assert `Stmt.While`,
  `condition()` is `Expr.Literal(Bool(true))`, `body().statements().size()=1`,
  that statement is `Stmt.Break`;

  **for**:
  (17) `forLoopAllClausesPresent` — source `"for (var i: i32 = 0i32; i < 10i32; i = i + 1i32) { break }"`,
  assert `Stmt.For`, `initializer()` is `Optional.of(Stmt.VarDeclaration(...))`,
  `condition()` is `Optional.of(Expr.Binary(_, LESS, _))`, `increment()` is `Optional.of(Expr.Assign(...))`;
  (18) `forLoopNoClausesInfiniteLoop` — source `"for (;;) { break }"`, assert `Stmt.For`,
  `initializer()=Optional.empty()`, `condition()=Optional.empty()`, `increment()=Optional.empty()`;
  (19) `forLoopConditionOnlyPresent` — source `"for (; i < 10i32;) { break }"`,
  assert `initializer()=Optional.empty()`, `condition()=Optional.of(...)`, `increment()=Optional.empty()`;

  **return / break / continue**:
  (20) `returnWithValueProducesStmtReturn` — source `"fun f(): i32 { return 42i32 }"`,
  extract the inner `Stmt.Return`, assert `value()=Optional.of(Expr.Literal(...))`;
  (21) `returnWithNoValueProducesStmtReturnEmpty` — source `"fun f(): void { return }"`,
  extract inner `Stmt.Return`, assert `value()=Optional.empty()`;
  (22) `breakProducesStmtBreak` — source `"fun f(): void { while(true) { break } }"`,
  extract nested `Stmt.Break`;
  (23) `continueProducesStmtContinue` — source `"fun f(): void { while(true) { continue } }"`,
  extract nested `Stmt.Continue`;

  **standalone block**:
  (24) `standaloneBlockProducesStmtBlock` — source wrapping a standalone `"{ }"` inside a `main` block,
  assert one of the body statements is `Stmt.Block` with empty statements;

  **multi-dimensional array**:
  (25) `multiDimensionalArrayTypeParses` — source `"var matrix: i8[2][3] = {{1i8, 2i8, 3i8}, {4i8, 5i8, 6i8}}"`,
  assert `typeAnnotation()` is `Type.Array` whose `elementType()` is also `Type.Array`;

  **type keywords round-trip**:
  (26) `allPrimitiveTypeKeywordsParsed` — for each type keyword `i8`, `i16`, `i32`, `i64`, `u8`, `u16`,
  `u32`, `u64`, `f32`, `f64`, `char`, `string`, `bool`: parse `"var x: <type> = ..."`, assert the
  corresponding `Type.*` variant is produced (use `@ParameterizedTest` with `@ValueSource` or
  one explicit `@Test` per type).

  Run RED: `.\gradlew.bat :app:test --tests "*StatementParserTest*"` must FAIL. —
  `app/src/test/java/org/dersbian/compiler/syntax/StatementParserTest.java`

- [ ] T016 [US3] Create `StatementParser.java` as a package-private `final class` in package
  `org.dersbian.compiler.syntax`. Constructor:
  `StatementParser(TokenCursor cursor, ExpressionParser exprParser, List<CompileError.SyntaxError> errors)` —
  store all three as `private final` fields.

  `Stmt parseStatement()` — switch on `cursor.peek().type()`:

    - `Keyword.FUN` → `parseFunDecl()`
    - `Keyword.VAR` → `parseVarDecl(true)`
    - `Keyword.CONST` → `parseVarDecl(false)`
    - `Keyword.IF` → `parseIf()`
    - `Keyword.WHILE` → `parseWhile()`
    - `Keyword.FOR` → `parseFor()`
    - `Keyword.RETURN` → `parseReturn()`
    - `Keyword.BREAK` → `parseBreak()`
    - `Keyword.CONTINUE` → `parseContinue()`
    - `Keyword.MAIN` → `parseMainBlock()`
    - `Delimiter.OPEN_BRACE` → `parseBlock()` (standalone block)
    - default → `parseExpressionStatement()`

  `Stmt.Block parseBlock()` — expect `OPEN_BRACE`, loop calling `parseStatement()` until
  `check(CLOSE_BRACE)` or `isAtEnd()`, expect `CLOSE_BRACE`, return
  `new Stmt.Block(List.copyOf(stmts), openSpan.merge(closeSpan))`.

  `Stmt parseFunDecl()` — consume `fun` token (record its span), expect `IdentifierAscii` or
  `IdentifierUnicode` for the name, expect `OPEN_PAREN`, parse `paramList` (zero or more
  `IDENT : type` pairs separated by commas), expect `CLOSE_PAREN`, expect `COLON`, parse
  return type via `parseType()`, call `parseBlock()` for body, return `Stmt.Function(name, params, returnType, body, span)`.

  `Stmt parseVarDecl(boolean isMutable)` — consume `var`/`const` token, parse one or more
  binding names (identifiers) separated by commas, then either:
    - if next token is `COLON`: consume `:`, parse `typeAnnotation` via `parseType()`, then if
    next token is `EQUAL`: consume `=`, parse comma-separated initializer expressions (one per binding);
    - else (no colon): expect `EQUAL`, parse comma-separated initializer expressions, use
    `Type.Custom("")` as a placeholder type annotation for untyped shorthand `var a,b = 1,2`.
  Return `Stmt.VarDeclaration(bindings, typeAnnotation, isMutable, span)`.

  `Stmt parseIf()` — consume `if`, parse condition expression (which may or may not be parenthesized;
  the grammar shows `if expression block` but all sample files wrap the condition in `()`; parse
  as a plain expression — the `(` and `)` will be consumed as `Expr.Grouping`), call `parseBlock()`
  for thenBranch, then:
    - if `check(Keyword.ELSE)`: consume `else`; if `check(Keyword.IF)`: recursively parse `parseIf()`,
      wrap in `ElseBranch.ElseIf(nestedIf)`;
      else: parse block, wrap in `ElseBranch.Block(block)`.
    - else: use `new ElseBranch.None()`.
  Return `Stmt.If(condition, thenBranch, elseBranch, span)`.

  `Stmt parseWhile()` — consume `while`, parse condition expression, call `parseBlock()`,
  return `Stmt.While(condition, body, span)`.

  `Stmt parseFor()` — consume `for`, expect `OPEN_PAREN`,
  parse optional initializer: if `check(SEMICOLON)` → `Optional.empty()`;
    else if `check(Keyword.VAR)` or `check(Keyword.CONST)` → `Optional.of(parseVarDecl(...))`;
    else → `Optional.of(parseExpressionStatement())`.
  Expect `SEMICOLON` (first separator).
  Parse optional condition: if `check(SEMICOLON)` → `Optional.empty()`; else parse expression.
  Expect `SEMICOLON` (second separator).
  Parse optional increment: if `check(CLOSE_PAREN)` → `Optional.empty()`; else parse expression.
  Expect `CLOSE_PAREN`. Call `parseBlock()`.
  Return `Stmt.For(initializer, condition, increment, body, span)`.

  `Stmt parseReturn()` — consume `return`; if not at block boundary (not `CLOSE_BRACE` and not EOF),
  parse expression; return `Stmt.Return(Optional.ofNullable(value), span)`.

  `Stmt parseBreak()` — consume `break`, return `Stmt.Break(span)`.

  `Stmt parseContinue()` — consume `continue`, return `Stmt.Continue(span)`.

  `Stmt parseMainBlock()` — consume `main`, call `parseBlock()`,
  return `Stmt.MainFunction(body, span)`.

  `Stmt parseExpressionStatement()` — call `exprParser.parseExpression(0)`,
  return `Stmt.Expression(expr)`.

  `Type parseType()` — switch on `cursor.peek().type()`:
    - `TypeKeyword.I8` → advance, return `new Type.I8()`
    - (repeat for all 13 type keywords)
    - `IdentifierAscii` / `IdentifierUnicode` → advance, return `new Type.Custom(name)`
    - `Keyword.VOID` (if present) or any void-like token → advance, return `new Type.VoidT()`
  After parsing the base type, loop: while `check(OPEN_BRACKET)` → consume `[`, parse size expression,
  expect `CLOSE_BRACKET`, wrap accumulated type in `new Type.Array(baseType, sizeExpr)`.
  If no recognizable type token: add error `"Expected type annotation, found " + peek.type()`,
  return `new Type.Custom("?")` as error sentinel.

  Add `@SuppressWarnings` as required. Full Javadoc on class and every method. —
  `app/src/main/java/org/dersbian/compiler/syntax/StatementParser.java`

- [ ] T017 [US3] Run GREEN for T015:
  `.\gradlew.bat :app:test --tests "*StatementParserTest*"` — all tests must pass.
  Fix failures in `StatementParser.java`. Run `.\gradlew.bat :app:spotlessApply`.

---

## Phase 5: User Story 4 — Parser Entry Point, Error Recovery & Service Wiring (Priority: P2)

**Goal**: `Parser` ties all components together. `DefaultCompilerService` calls the parser
after lexing. Structured `CompileError.SyntaxError` errors with correct spans are produced
for every syntax error, and the parser recovers to report multiple errors in one pass.

**Dependency**: Requires `StatementParser` (Phase 4) fully GREEN before starting.

**Independent Test**: `.\gradlew.bat :app:test --tests "*ParserErrorRecoveryTest*"` verifies
every error-recovery path and error message contract independently.

### TDD Cycle — Parser + Error Recovery (US4)

- [ ] T018 [TEST] [US4] Write `ParserErrorRecoveryTest` in package `org.dersbian.compiler.syntax`.
  Class package-private. Helper: `private ParseResult parseSource(String source)` — tokenize with
  real `Lexer`, construct `new Parser(tokens, Path.of("<test>")).parse()`.
  Required test methods:

  (1) `missingColonInVarDeclAddsError` — source `"var x i32 = 5i32"` (no `:` before `i32`),
  assert `result.hasErrors()` is `true`, `errors().get(0).errorSpan()` is non-null,
  `errors().get(0).errorMessage()` contains the string `"Expected"` and contains `"':'"`
  (case-insensitive match acceptable);

  (2) `missingClosingParenInCallAddsError` — source `"main { f(1i32 }"` (missing `)` before `}`),
  assert at least one `SyntaxError` in `errors()`, the first error's `errorMessage()` references
  `")"` as the expected token;

  (3) `unexpectedEofMidExpressionAddsError` — source `"main { var x: i32 = 1i32 +"`,
  assert `result.hasErrors()` is `true`, at least one error message contains `"EOF"` or
  `"end of"` (case-insensitive);

  (4) `multipleErrorsAreAllCollected` — source `"main { var : i32 var : bool }"` (two missing
  identifier names), assert `result.errors().size() >= 2` (recovery continued after first error);

  (5) `parseDoesNotThrowOnSyntaxError` — source `"fun {"` (invalid token after `fun`),
  assert that calling `parseSource("fun {")` does NOT throw any exception and returns a
  `ParseResult` (use `assertThatCode(() -> parseSource("fun {")).doesNotThrowAnyException()`);

  (6) `errorSpanPointsToOffendingToken` — source `"var x: i32 = +"` (operator where expression expected),
  assert the error span's start position is not zero (it points into the source, not position 0);

  (7) `recoveryAfterErrorContinuesParsingNextTopLevelDecl` — source
  `"fun bad { } fun good(): void { }"` (first `fun` missing `()` and return type),
  assert `errors()` is non-empty AND `statements()` contains at least one `Stmt.Function`
  (recovery reached `fun good`);

  (8) `missingFunctionReturnTypeAddsError` — source `"fun f() { }"` (missing `: returnType`),
  assert at least one `SyntaxError`;

  (9) `missingFunctionNameAddsError` — source `"fun (a: i32): void { }"` (missing name),
  assert at least one `SyntaxError`.

  Run RED: `.\gradlew.bat :app:test --tests "*ParserErrorRecoveryTest*"` must FAIL
  (`Parser` class does not exist yet). —
  `app/src/test/java/org/dersbian/compiler/syntax/ParserErrorRecoveryTest.java`

- [ ] T019 [US4] Create `Parser.java` as a `public final class` in package
  `org.dersbian.compiler.syntax`. Constructor:
  `public Parser(List<Token> tokens, Path source)` — null-check both parameters.
  Fields: `private final TokenCursor cursor`, `private final List<CompileError.SyntaxError> errors`
  (mutable `new ArrayList<>()`), `private final ExpressionParser exprParser`,
  `private final StatementParser stmtParser`, `private final Path source`.
  Construct sub-components in this order in the constructor body:
  1. `this.cursor = new TokenCursor(tokens);`
  2. `this.exprParser = new ExpressionParser(cursor, errors);`
  3. `this.stmtParser = new StatementParser(cursor, exprParser, errors);`

  `public ParseResult parse()` — never throws for recoverable syntax errors:
  
  ```java
  List<Stmt> statements = new ArrayList<>();
  while (!cursor.isAtEnd()) {
      try {
          statements.add(stmtParser.parseStatement());
      } catch (RuntimeException e) {
          // only truly unexpected internal errors reach here;
          // add a synthetic SyntaxError and synchronize
          errors.add(CompileError.syntaxError(null,
              "Internal parse error: " + e.getMessage(),
              cursor.currentSpan(), null));
          cursor.synchronize(errors);
      }
  }
  return new ParseResult(List.copyOf(statements), List.copyOf(errors));
  ```
  
  No SLF4J or any other logging inside this class (FR-012). No `System.out` calls.
  Full Javadoc on class and `parse()`. Add `@SuppressWarnings` as required. —
  `app/src/main/java/org/dersbian/compiler/syntax/Parser.java`

- [ ] T020 [US4] Run GREEN for T018 (error recovery) and T014 (integration):
  `.\gradlew.bat :app:test --tests "*ParserErrorRecoveryTest*"` — all 9 tests must pass.
  `.\gradlew.bat :app:test --tests "*ParserTest*"` — all 8 tests must pass.
  Fix failures in `Parser.java`, `StatementParser.java`, or `ExpressionParser.java`.
  Run `.\gradlew.bat :app:spotlessApply` after each fix.

- [ ] T021 [US4] Wire `Parser` into `DefaultCompilerService.checkSyntax` in
  `app/src/main/java/org/dersbian/compiler/DefaultCompilerService.java`.
  Locate the existing `// TODO: wire up the real parser.` comment. Replace it with:
  
  ```java
  final Parser parser = new Parser(result.tokens(), source);
  final ParseResult parseResult = parser.parse();
  final String parseErrorReport = errorReporter.reportErrors(
      parseResult.errors().stream()
          .map(e -> (CompileError) e)
          .toList());
  if (!parseErrorReport.isEmpty()) {
      System.out.println(parseErrorReport);
      throw new CompilerException(
          "Parse failed with " + parseResult.errors().size() + " error(s)");
  }
  ```
  
  Add the necessary import `import org.dersbian.compiler.syntax.Parser;` and
  `import org.dersbian.compiler.syntax.ParseResult;`.
  Do NOT add logging inside the parser or here beyond what already exists.
  Do NOT change the method signature, the lexer path, or the `ErrorReporter` construction. —
  `app/src/main/java/org/dersbian/compiler/DefaultCompilerService.java`

- [ ] T022 [US1] [TEST] Verify `DefaultCompilerService` integration by running the existing CLI-level
  test suite (if any): `.\gradlew.bat :app:test --tests "*DefaultCompilerService*"`.
  If no test class exists for `DefaultCompilerService`, create `DefaultCompilerServiceParserTest`
  in `app/src/test/java/org/dersbian/compiler/` with two tests:
  (1) `checkSyntaxOnValidFileDoesNotThrow` — create a `@TempDir Path tmpDir`, write valid source
  `"main { }"` to `tmpDir.resolve("valid.dr")`, call `new DefaultCompilerService().checkSyntax(path)`,
  assert no exception is thrown;
  (2) `checkSyntaxOnInvalidFileThrowsCompilerException` — write source `"fun {"` to a temp file,
  assert `CompilerException` is thrown by `checkSyntax`.
  Run RED then implement (T021 is the implementation), then run GREEN. —
  `app/src/test/java/org/dersbian/compiler/DefaultCompilerServiceParserTest.java`

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Quality gate sweep, coverage verification, CLI smoke tests, and minor
corrections discovered during integration. This phase begins only after all user story
checkpoints are GREEN.

- [ ] T023 [P] Run the full quality gate:
  `.\gradlew.bat :app:check`
  All sub-gates must pass: Checkstyle, PMD, SpotBugs, Error Prone (`-Werror`), Spotless,
  JUnit (all tests pass), JaCoCo (≥ 80% branch coverage on all classes in
  `org.dersbian.compiler.syntax` except `package-info.java`).
  Fix every violation found. Do not suppress a warning without a documented reason in the
  `@SuppressWarnings` annotation string that names the specific tool and rule.

- [ ] T024 [P] Verify JaCoCo branch coverage report for parser classes:
  Open `app/build/reports/jacoco/test/html/index.html` after `.\gradlew.bat :app:check`.
  Confirm that `TokenCursor`, `BindingPower`, `ExpressionParser`, `StatementParser`, `Parser`,
  and `ParseResult` each show ≥ 80% branch coverage. If any class is below threshold, add
  targeted test cases to the appropriate test class until the threshold is met, then rerun
  `.\gradlew.bat :app:check` to confirm. (SC-004)

- [ ] T025 [P] Run CLI smoke tests against all canonical `dr_files/` samples (SC-001, SC-005):

  ```powershell
  .\gradlew.bat :app:run --args="check dr_files/input.dr"
  .\gradlew.bat :app:run --args="check dr_files/simple_test.dr"
  .\gradlew.bat :app:run --args="check dr_files/large_toy_program.dr"
  .\gradlew.bat :app:run --args="check dr_files/break_continue_loops.dr"
  .\gradlew.bat :app:run --args="check dr_files/test_for.dr"
  .\gradlew.bat :app:run --args="check dr_files/float_test.dr"
  .\gradlew.bat :app:run --args="check dr_files/sccp_test.dr"
  .\gradlew.bat :app:run --args="check dr_files/ssa_test.dr"
  .\gradlew.bat :app:run --args="check dr_files/complex_ssa_test.dr"
  ```

  Every invocation must exit code 0. If any file fails, investigate the parse error,
  fix the parser rule responsible, and rerun the affected test class.

- [ ] T026 [P] Build and smoke-test the shadow JAR (SC-006, SC-007):

  ```powershell
  .\gradlew.bat :app:shadowJar
  java -jar app/build/libs/Dersco-0.1.0.jar check dr_files/simple_test.dr
  java -jar app/build/libs/Dersco-0.1.0.jar check dr_files/large_toy_program.dr
  ```

  Both invocations must exit code 0. If the JAR does not exist or exits non-zero, fix the
  underlying parser or service issue first, rebuild, and re-verify.

- [ ] T027 Correct the `ElseBranch` variant names in `data-model.md`: the actual sealed interface
  uses `None`, `Block`, `ElseIf` — not `Empty`, `Else`, `ElseIf` as written in some data-model
  sections. Search all occurrences of `ElseBranch.Empty` and `ElseBranch.Else` in
  `specs/001-parser-pratt/data-model.md` and replace with `ElseBranch.None` and
  `ElseBranch.Block` respectively to match the actual source in `ElseBranch.java`.
  Verify no other spec artifact contains `ElseBranch.Empty` or `ElseBranch.Else`. —
  `specs/001-parser-pratt/data-model.md`

- [ ] T028 [P] Final formatting sweep:
  `.\gradlew.bat :app:spotlessApply`
  followed immediately by:
  `.\gradlew.bat :app:spotlessCheck`
  Both must succeed with no diff. Commit the formatted files.

---

## Dependencies & Execution Order

### Phase Dependencies

```text
Phase 1 (T001–T005)       ─── no dependencies; start immediately
    │
    ▼
Phase 2 (T006–T010)       ─── depends on Phase 1 complete (ParseResult, BindingPower created)
    │                         ⚠️ BLOCKS all Phase 3+ work
    ▼
Phase 3 (T011–T014)       ─── depends on Phase 2 complete (TokenCursor GREEN)
    │                         ExpressionParser test (T011) can be written in parallel with T006
    ▼
Phase 4 (T015–T017)       ─── depends on Phase 3 GREEN (ExpressionParser available)
    │
    ▼
Phase 5 (T018–T022)       ─── depends on Phase 4 GREEN (StatementParser available)
    │
    ▼
Phase 6 (T023–T028)       ─── depends on Phase 5 GREEN (full pipeline wired)
```

### User Story Dependencies

| Story | Priority | Depends on | Blocks |
|-------|----------|-----------|--------|
| US2 — Expression Precedence | P1 | Phase 2 | US1 full integration, US3, US4 |
| US1 — Full File Parse | P1 | US2 (ExpressionParser) | US4 wiring |
| US3 — Statement Constructs | P2 | US2 (ExpressionParser) | US4 wiring |
| US4 — Error Recovery + Service | P2 | US1 + US3 | Phase 6 polish |

### Within Each Phase

- **TDD order within a story**: write test (RED) → implement (GREEN) → spotlessApply → check gate
- **Models before consumers**: `TokenCursor` before `ExpressionParser`; `ExpressionParser` before `StatementParser`; both before `Parser`
- **Never suppress a gate violation** without a Javadoc-style comment explaining why it is safe

### Parallel Opportunities

Tasks marked `[P]` within the same phase can run concurrently:

- T002 (`ParseResult`) and T004 (`BindingPower`) can be written simultaneously (no shared state)
- T003 (`ParseResultTest`) and T005 (`BindingPowerTest`) can be written simultaneously
- T006 (`TokenCursorTest`) can be started as soon as T002/T004 are done (does not require their GREEN step)
- T011 (`ExpressionParserTest` — test file only) can be started before Phase 2 is GREEN, since
  it only references types that already exist; it will fail until `ExpressionParser` is created
- T014 (`ParserTest`) and T018 (`ParserErrorRecoveryTest`) can be written in parallel once the
  test helpers they need (`Parser` constructor signature) are known from T019's design
- T023, T024, T025, T026, T028 in Phase 6 are all independent of each other

---

## Parallel Execution Examples

### User Story 2 — Expression Parsing (single developer)

```text
T011 → write ExpressionParserTest (46 tests, all RED)
T012 → write ExpressionParser (Pratt loop + parsePrimary)
T013 → run GREEN for T011 (fix until all 46 pass)
```

### User Story 2 — Two developers in parallel

```text
Developer A: T011 (write all expression tests)
Developer B: T004 + T005 (BindingPower + its tests)

After both green:
Developer A: T012 (ExpressionParser implementation)
Developer B: T006 + T007 (TokenCursorTest + TokenCursor)
```

### User Story 3 — Statement Parsing

```text
T015 → write StatementParserTest (26 tests, all RED — requires ExpressionParser GREEN)
T016 → write StatementParser
T017 → run GREEN for T015
```

---

## Implementation Strategy

### MVP (User Stories 1 + 2 only — Phases 1–3)

1. Complete Phase 1 (T001–T005): ParseResult + BindingPower created and tested
2. Complete Phase 2 (T006–T010): TokenCursor created and tested → **FOUNDATION READY**
3. Complete Phase 3 (T011–T014): ExpressionParser + Parser skeleton
4. **STOP and VALIDATE**: `.\gradlew.bat :app:test --tests "org.dersbian.compiler.syntax.*"` GREEN
5. Parser can be called from CLI and returns results for expression-containing files

### Incremental Delivery

1. Phases 1+2 → Foundation ready
2. Phase 3 → Expression parsing + top-level parse loop → MVP (US1+US2)
3. Phase 4 → Statement parsing (US3)
4. Phase 5 → Error recovery + service wiring (US4)
5. Phase 6 → Quality gate sweep + smoke tests → DONE

### Quality Gate Checkpoints

| After | Command | Must pass |
|-------|---------|-----------|
| Phase 2 | `.\gradlew.bat :app:check` | ParseResult, BindingPower, TokenCursor + tests |
| Phase 3 | `.\gradlew.bat :app:test --tests "*ExpressionParserTest*"` | All 46 expression tests |
| Phase 4 | `.\gradlew.bat :app:test --tests "*StatementParserTest*"` | All 26 statement tests |
| Phase 5 | `.\gradlew.bat :app:test --tests "org.dersbian.compiler.syntax.*"` | All parser tests |
| Phase 6 | `.\gradlew.bat :app:check` | Full quality gate, ≥80% JaCoCo branch coverage |

---

## Notes

- `[P]` = task touches only its own file(s); safe to run in parallel with other `[P]` tasks
  in the same phase
- Constitution V (TDD) is non-negotiable: every production class must have a failing test
  before its implementation is written; do not write any production code before the RED step
- Span construction in parser: always use `openToken.span().merge(closeToken.span())` for
  multi-token constructs; use `token.span()` for single-token constructs
- `ElseBranch` variants are `None`, `Block`, `ElseIf` — not `Empty`, `Else`, `ElseIf`
- No semicolons as statement terminators; the only `;` in the grammar are the two separators
  in `for (init ; cond ; incr)` headers
- `ErrorCode.E1006` through `E1015` are available for parser-specific errors; use the
  lowest available code for each distinct error category (missing identifier, missing colon,
  missing brace, unexpected token in expression, unexpected EOF, etc.)
- Apply `.\gradlew.bat :app:spotlessApply` after every file write; never hand-format Java
