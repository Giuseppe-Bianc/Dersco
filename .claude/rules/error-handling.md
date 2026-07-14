---
paths:
  - "app/src/main/java/org/dersbian/compiler/**"
  - "app/src/main/java/org/dersbian/cli/**"
---

# Error Handling

- Use the `CompileError` sealed hierarchy keyed by `CompilerPhase`. Do not throw generic `RuntimeException` from compiler code.
- Carry `(code, message, span, help)` on every diagnostic; `IoError` wraps `IOException` and `AsmGeneratorError` is span-free by design.
- Never swallow errors silently. Log via `@Slf4j` and either rethrow as a `CompilerException` or convert to a `CompileError` with a stable `ErrorCode`.
- Handle every checked `IOException` from file I/O at the boundary. Do not let it propagate past `ICompilerService`.
- Render errors through `ErrorReporter` with ANSI color and a `LineTracker` source-context underline. Never print raw stack traces in production output.
- Validate inputs at the lex/parse boundary; fail fast on invalid input. Recover (skip, warn) on internal invariants, not user input.
- Include source `SourceLocation` (`line`, `column`, `offset`) on every diagnostic that can be tied to a span.
