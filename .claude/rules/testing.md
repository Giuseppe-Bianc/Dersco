---
alwaysApply: true
---

# Testing

- Use JUnit Jupiter (`@Test`, `Assertions.*`) and AssertJ (`assertThat`) -- the project does not use JUnit 4 or legacy Hamcrest.
- Test classes are package-private (`class XxxTest`, not `public`) and live next to the code under test, not in a separate `integration/` tree.
- Test method names describe behavior, not the method being tested (`parseIntegerWithMultiCharacterSuffix`, not `test1` or `shouldParse`).
- One logical assertion per test. `assertThat` chains are fine when they read as a single expectation.
- Verify behavior, not implementation. Don't assert mock call counts when output values would do.
- Prefer real implementations. Mock only at system boundaries (filesystem, clock, randomness, picocli `CommandLine`).
- Run the specific test class after changes: `./gradlew test --tests "*NumericParsersTest*"`. Wildcard around the simple class name is required.
- Flaky test? Fix it or delete it. Never retry to make it pass.
- Static-utility classes use `@NoArgsConstructor(access = AccessLevel.PRIVATE)`; tests for them live in the same package and call the static methods directly.
