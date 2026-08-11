# Dersco

[![CI / CD Workflow](https://github.com/Giuseppe-Bianc/Dersco/actions/workflows/ci.yml/badge.svg)](https://github.com/Giuseppe-Bianc/Dersco/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/Giuseppe-Bianc/Dersco/graph/badge.svg)](https://codecov.io/gh/Giuseppe-Bianc/Dersco)
[![Java 25](https://img.shields.io/badge/Java-25-orange.svg)](https://jdk.java.net/25/)
[![Gradle](https://img.shields.io/badge/Gradle-9.7-blue.svg)](https://gradle.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

**Dersco** is a modern compiler infrastructure for the Dersco programming language, targeting **Java 25** and built with **Gradle**. It features a robust token model, rich diagnostic error reporting with source context rendering, and strict static-analysis quality gates.

---

## Features

- **Java 25 & Gradle Toolchain**: Leverages modern Java language features (sealed interfaces, record types, pattern matching) and the Gradle version catalog (`libs.versions.toml`).
- **Rich CLI**: Built with [picocli](https://picocli.info/) and [Jansi](https://fusesource.github.io/jansi/) for colorized, git-style subcommands (`compile`, `check`).
- **Strongly-Typed Lexer Model**: Immutable `Token`, `TokenKind` (sealed interface with `Simple` enum variants and payload-bearing records), and `Span` source position tracking.
- **Diagnostic Engine**: High-fidelity error reporting (`ErrorReporter`) featuring line tracking, ANSI color highlights, and source code context underlines.
- **Strict Quality Gates**: Zero-warning enforcement with Checkstyle, PMD, SpotBugs (Max effort), Error Prone (`-Werror`), and Spotless code formatting.
- **CI/CD Ready**: Configured with GitHub Actions for automated build, verification, artifact creation, releases, and Dependabot dependency updates.

---

## Project Structure

```text
Dersco/
├── app/
│   ├── config/              # Static analysis rulesets (Checkstyle, PMD)
│   └── src/
│       ├── main/java/org/dersbian/
│       │   ├── App.java                   # CLI bootstrap entrypoint
│       │   ├── cli/                       # Picocli commands (RootCommand, CompileCommand, CheckCommand)
│       │   ├── compiler/                  # Compiler service & diagnostic surface
│       │   │   ├── error/                 # CompileError, ErrorReporter, ErrorCode
│       │   │   ├── lexer/                 # Lexer infrastructure & token model
│       │   │   └── location/              # Source location & LineTracker
│       │   └── util/                      # Helper utilities
│       └── test/java/                     # Unit test suite mirroring production packages
├── gradle/
│   └── libs.versions.toml   # Version Catalog for dependencies
├── .github/
│   ├── dependabot.yml       # Dependabot configuration (Gradle & GitHub Actions)
│   └── workflows/
│       └── ci.yml           # GitHub Actions CI/CD workflow
├── build.gradle.kts
└── settings.gradle.kts
```

---

## Prerequisites

- **Java 25 SDK** (or allow Gradle's Foojay Toolchain Resolver to provision it automatically).
- **Gradle 9.7** (included via `./gradlew` wrapper).

---

## Quick Start & Usage

### 1. Build the Executable Application

To compile the project and build the standalone shadow JAR:

```bash
./gradlew shadowJar
```

The resulting executable JAR will be located at:
`app/build/libs/Dersco-0.1.0.jar`

### 2. Run the CLI

You can execute commands directly using Gradle:

```bash
# Check syntax of a source file
./gradlew run --args="check path/to/source.dr"

# Compile a source file
./gradlew run --args="compile path/to/source.dr"
```

Or run the built fat JAR directly:

```bash
java -jar app/build/libs/Dersco-0.1.0.jar check path/to/source.dr
java -jar app/build/libs/Dersco-0.1.0.jar compile path/to/source.dr
```

---

## Verification & Development Gates

### Running Tests

Execute the unit test suite:

```bash
./gradlew test
```

To run a specific test class:

```bash
./gradlew test --tests "*NumericParsersTest*"
```

### Full Quality-Gate Validation

Run all verification gates (Checkstyle, PMD, SpotBugs, Spotless, and tests):

```bash
./gradlew check
```

### Code Formatting

Formatting is strictly enforced by [Spotless](https://github.com/diffplug/spotless) using Google Java Format (AOSP style, 4 spaces). To format all files automatically:

```bash
./gradlew spotlessApply
```

---

## CI / CD & Automation

- **GitHub Actions (`.github/workflows/ci.yml`)**: Runs `./gradlew check` and `./gradlew shadowJar` on every push and pull request. When a release tag (e.g. `v0.1.0`) is pushed, a GitHub Release is published with the compiled executable attached.
- **Code coverage & Codecov**: The pipeline also runs `./gradlew jacocoTestReport` and uploads the resulting XML report to [Codecov](https://codecov.io/gh/Giuseppe-Bianc/Dersco) via the official `codecov/codecov-action@v5`. Coverage configuration lives in [`codecov.yml`](codecov.yml) at the repository root.
- **Dependabot (`.github/dependabot.yml`)**: Checks daily for updates to Gradle dependencies in `libs.versions.toml` and GitHub Actions workflows.

### Pipeline phases

The `build-and-check` job executes these stages in order. Each step fails the job on a non-zero exit code; the next step never starts if the current one fails.

| # | Phase | Purpose |
|---|-------|---------|
| 1 | `actions/checkout@v7` | Clone the repository at the triggering ref. |
| 2 | `actions/setup-java@v5` | Provision a Temurin JDK 25 toolchain that matches `app/build.gradle.kts`. |
| 3 | `gradle/actions/setup-gradle@v6` | Configure the Gradle wrapper, enable the build cache, and publish the summary. |
| 4 | `chmod +x gradlew` | Ensure the wrapper is executable on the Linux runner. |
| 5 | `./gradlew check` | Run Checkstyle, PMD, SpotBugs, Spotless, JUnit, and JaCoCo in a single invocation. |
| 6 | `./gradlew shadowJar` | Produce the executable fat JAR under `app/build/libs/`. |
| 7 | `actions/upload-artifact@v7` | Attach the JAR to the workflow run so it can be downloaded. |
| 8 | `./gradlew jacocoTestReport` | Regenerate the JaCoCo XML report at `app/build/reports/jacoco/test/jacocoTestReport.xml` (also produced by `check`, called explicitly so a future decoupling is safe). |
| 9 | `codecov/codecov-action@v5` | Upload the JaCoCo XML to Codecov, using `CODECOV_TOKEN` from repository secrets when present. |

A second `release` job, gated on the `v*` tag pattern, depends on `build-and-check` and publishes the fat JAR to a GitHub Release with `softprops/action-gh-release@v3`.

### Required secrets

| Secret | Required for | Notes |
|--------|---------------|-------|
| `GITHUB_TOKEN` | Always | Provided automatically by GitHub Actions; used by the release job and by Gradle's cache integration. |
| `CODECOV_TOKEN` | Private repositories | Codecov upload token. Create it at <https://codecov.io/gh/Giuseppe-Bianc/Dersco/settings> and add it under *Settings -> Secrets and variables -> Actions*. For public repositories the upload works without a token, but supplying one improves rate limits and unlocks private status checks. |

### Codecov configuration

[`codecov.yml`](codecov.yml) defines:

- **Project target** — the overall coverage floor, set to `auto` (Codecov derives it from the baseline).
- **Patch target** — coverage of newly added or modified lines must reach **80%** with a **5%** tolerance.
- **Ignore paths** — build outputs, the Gradle wrapper, and any auto-generated sources.
- **Comment layout** — header, diff, flags, components, files, footer; required changes disabled to avoid blocking PRs on cosmetic diffs.

`fail_ci_if_error: false` keeps the workflow green when Codecov itself is unreachable; the Codecov UI still records the failure, and the status checks configured in `codecov.yml` are the authoritative gate for the PR.

### Local reproduction

Run the same steps the CI runs:

```bash
./gradlew check
./gradlew jacocoTestReport
# Then upload the local report (optional, requires a Codecov token):
bash <(curl -s https://codecov.io/bash) -f app/build/reports/jacoco/test/jacocoTestReport.xml -t <CODECOV_TOKEN>
```

---

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
