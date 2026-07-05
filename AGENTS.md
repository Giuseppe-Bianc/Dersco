# AGENTS.md

This repository is a Java 25 Gradle application. Keep changes aligned with the existing toolchain and quality gates in [app/build.gradle.kts](app/build.gradle.kts) and [settings.gradle.kts](settings.gradle.kts).

## Working Rules

- Use Java 25 semantics and target the configured Gradle toolchain before introducing newer or preview language features.
- When a change depends on Java language or platform behavior, prefer the official Java SE docs, JEPs, and standard library documentation as the source of truth.
- Prefer small, targeted edits that preserve the current package structure under [app/src/main/java](app/src/main/java) and mirrored tests under [app/src/test/java](app/src/test/java).
- Keep production code compatible with the configured static-analysis setup: Checkstyle, PMD, SpotBugs, Error Prone, and Spotless are enforced by the build.
- Do not duplicate documentation that already exists in the repo. Link to existing docs instead of copying them.
- Follow the project's existing conventions for final classes, descriptive test names, and package-private test visibility when that pattern already fits the code.

## Validation

- Prefer `./gradlew test` for focused verification and `./gradlew check` for full quality-gate validation.
- If you touch formatting-sensitive Java code, verify with Spotless rather than hand-formatting against a guess.
- If a change affects CLI wiring or packaging, also consider `./gradlew run` or `./gradlew shadowJar` as the relevant follow-up check.

## Reference Files

- Build and toolchain: [app/build.gradle.kts](app/build.gradle.kts)
- Build layout: [settings.gradle.kts](settings.gradle.kts)
- Project overview: [README.md](README.md)

## Ongoing Refinement

- Use `/chronicle improve` to capture recurring friction and keep these instructions aligned with what actually slows development down.
