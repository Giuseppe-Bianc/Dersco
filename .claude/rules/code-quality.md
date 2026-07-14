---
alwaysApply: true
---

# Code Quality

## Anti-defaults (counter common Claude tendencies)

- No premature abstractions. Three similar lines beats a helper used once.
- Don't add features or improvements beyond what was asked.
- Don't refactor adjacent code while fixing a bug.
- No dead code or commented-out blocks. Git has history.
- WHY comments, never WHAT. If code needs a "what" comment, rename instead.
- API docs at module boundaries only, not every internal function. The repo mixes English and Italian Javadoc; preserve existing style rather than normalizing.

## Naming

- Files: PascalCase matching the single top-level type (`Token.java`, `LexerResult.java`).
- Classes: PascalCase, sealed interfaces with `permits` clauses, records for value carriers.
- Methods: camelCase, verb-first (`scanNumber`, `consumeOperatorKind`).
- Constants: `SCREAMING_SNAKE` (`MIN_TAIL3_LENGTH`, `UNKNOWN`). `private static final` on a `final` class.
- Booleans: `is` / `has` / `should` / `can` prefix. Predicates stay short.

## Code Markers

`TODO(author): desc (#issue)` for planned work. `FIXME(author): desc (#issue)` for known bugs. `HACK(author): desc (#issue)` for ugly workarounds. Owner and issue link required. Never `XXX`, `TEMP`, `REMOVEME`.

## File Organization

- One top-level type per file. Records and helper records may share a file when they are a tight pair.
- Static-utility classes: `final` class, `private` no-arg constructor (`@NoArgsConstructor(access = AccessLevel.PRIVATE)`).
- Imports: Spotless `removeUnusedImports` is enforced -- do not leave dead imports.
- Loggers: `@Slf4j` (Lombok) instead of hand-rolled `LoggerFactory.getLogger(...)`.
- Indentation: 4 spaces, Allman braces, line length managed by Spotless `reflowLongStrings`.
