# Dersco

[![CI / CD Workflow](https://github.com/Giuseppe-Bianc/Dersco/actions/workflows/ci.yml/badge.svg)](https://github.com/Giuseppe-Bianc/Dersco/actions/workflows/ci.yml)
[![Java 25](https://img.shields.io/badge/Java-25-orange.svg)](https://jdk.java.net/25/)
[![Gradle](https://img.shields.io/badge/Gradle-9.6-blue.svg)](https://gradle.org)
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
- **Gradle 9.6** (included via `./gradlew` wrapper).

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
- **Dependabot (`.github/dependabot.yml`)**: Checks daily for updates to Gradle dependencies in `libs.versions.toml` and GitHub Actions workflows.

---

## License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
