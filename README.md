# Dersco

[![CI / CD Workflow](https://github.com/Giuseppe-Bianc/Dersco/actions/workflows/ci.yml/badge.svg)](https://github.com/Giuseppe-Bianc/Dersco/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/Giuseppe-Bianc/Dersco/graph/badge.svg)](https://codecov.io/gh/Giuseppe-Bianc/Dersco)
[![Java 25](https://img.shields.io/badge/Java-25-orange.svg)](https://jdk.java.net/25/)
[![Gradle](https://img.shields.io/badge/Gradle-9.7.1-blue.svg)](https://gradle.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

Dersco is a Java-based compiler project for the Dersco programming language. The current implementation provides a command-line interface, UTF-8 source loading, lexical analysis, recursive-descent/Pratt parsing, typed token and AST models, source locations, and compiler diagnostics. Semantic analysis, IR generation, optimization, and native code generation are not wired into the compilation service yet.

## Project status

The project is under active development. The implemented pipeline currently looks like this:

```text
source file
    |
    v
UTF-8 source loading
    |
    v
Lexer + source locations
    |
    v
Token stream + lexer errors
    |
    v
Parser + AST
    |
    v
Diagnostic rendering / AST output
```

`dersco check` runs this pipeline, reports lexical or syntax errors with source context, and prints the parsed AST when the input is valid.

`dersco compile` currently validates the source through the same front end and then stops. Its CLI accepts output, optimization, IR, and diagnostic options, but the backend that would consume those options is not implemented yet. In particular, the current implementation does not create the requested executable or output file.

See [CLI reference](docs/cli.md) for the exact command-line contract and [architecture notes](docs/architecture.md) for the current compiler structure.

## Implemented capabilities

- Java 25 toolchain with Gradle 9.7.1.
- Picocli CLI with `compile`, `check`, and built-in help/version support.
- Repeatable logging controls: `-v`, `-vv`, `-vvv`, and `-q`.
- UTF-8 source loading.
- Lexer infrastructure with a dedicated source cursor and line tracking.
- Strongly typed tokens with source spans and payload-bearing token records.
- Diagnostic rendering with source context and underlining.
- Compiler service abstraction suitable for unit testing.
- Unit tests with JUnit Jupiter 6.1.3 and AssertJ 3.27.7.
- Quality gates with Checkstyle, PMD, SpotBugs, Error Prone, Spotless, and JaCoCo.
- GitHub Actions CI for verification, packaging, coverage reporting, and tagged releases.

## Repository layout

```text
Dersco/
├── app/
│   ├── config/
│   │   ├── checkstyle/             # Checkstyle configuration
│   │   └── pmd/                    # PMD ruleset
│   ├── src/
│   │   ├── main/java/org/dersbian/
│   │   │   ├── App.java            # Application entry point
│   │   │   ├── cli/                # Picocli commands and logging options
│   │   │   ├── compiler/           # Compiler service, lexer, diagnostics
│   │   │   └── util/               # Shared utilities
│   │   └── test/java/              # Unit tests
│   └── build.gradle.kts             # Application build and quality configuration
├── docs/
│   ├── architecture.md             # Compiler architecture and execution flow
│   └── cli.md                      # Command-line reference
├── gradle/
│   └── libs.versions.toml          # Dependency version catalog
├── .github/
│   ├── dependabot.yml              # Dependency update configuration
│   └── workflows/
│       └── ci.yml                  # CI and release workflow
├── codecov.yml                     # Coverage configuration
├── gradlew                         # Gradle wrapper (Unix)
├── gradlew.bat                     # Gradle wrapper (Windows)
├── build.gradle.kts                # Root Gradle configuration
├── settings.gradle.kts             # Project settings and toolchain resolver
└── LICENSE
```

## Requirements

- JDK 25.
- Git.
- No system Gradle installation is required because the repository includes the Gradle wrapper.

The build also uses Gradle's Foojay toolchain resolver, so a JDK can be provisioned automatically when the local environment permits it.

## Getting started

Clone the repository and use the Gradle wrapper:

```bash
git clone https://github.com/Giuseppe-Bianc/Dersco.git
cd Dersco
./gradlew check
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Command-line usage

Run the application through Gradle during development:

```bash
./gradlew run --args="check path/to/source.dr"
./gradlew run --args="compile path/to/source.dr"
```

Build the distributable JAR:

```bash
./gradlew shadowJar
```

The generated artifact is written under `app/build/libs/`.

Run the packaged application:

```bash
java -jar app/build/libs/Dersco-0.1.0-all.jar --help
java -jar app/build/libs/Dersco-0.1.0-all.jar check path/to/source.dr
```

For supported flags and exit codes, see [docs/cli.md](docs/cli.md).

## Development workflow

### Run tests

```bash
./gradlew test
```

Run a specific test class:

```bash
./gradlew test --tests "*NumericParsersTest*"
```

### Run the full verification gate

```bash
./gradlew check
```

The `check` task includes the configured static analysis, formatting verification, tests, and JaCoCo report generation.

### Apply formatting

```bash
./gradlew spotlessApply
```

Spotless uses Google Java Format in AOSP mode with four-space indentation.

### Build the executable JAR

```bash
./gradlew shadowJar
```

This creates the shaded JAR and sets its manifest main class to `org.dersbian.App`.

## CI/CD

The GitHub Actions workflow performs verification and packaging on pushes and pull requests. The main build job provisions JDK 25, runs `./gradlew check`, builds the shaded JAR, uploads the artifact, and generates the JaCoCo XML report for Codecov.

Tagged releases matching `v*` trigger the release job after the build job succeeds. The release job publishes the shaded JAR to a GitHub Release.

Dependabot checks Gradle dependencies and GitHub Actions workflow dependencies according to `.github/dependabot.yml`.

For local reproduction of the CI verification stages:

```bash
./gradlew check
./gradlew shadowJar
./gradlew jacocoTestReport
```

## Code coverage

JaCoCo writes the XML report to:

```text
app/build/reports/jacoco/test/jacocoTestReport.xml
```

`codecov.yml` configures project and patch coverage reporting. The CI workflow uses `fail_ci_if_error: true`, so a Codecov upload failure fails the build job.

## Architecture and implementation notes

The compiler is intentionally split into small services and models. CLI code depends on `ICompilerService`, while the default implementation coordinates file loading, lexing, source tracking, and diagnostics.

The parser is currently connected to `DefaultCompilerService` and its AST is printed by both successful `check` and `compile` executions. Semantic analysis, IR generation, optimization, and code generation remain future stages. Do not document `compile` as producing native code until those stages are connected to `DefaultCompilerService`.

Detailed design notes are in [docs/architecture.md](docs/architecture.md).

## License

Dersco is licensed under the Apache License 2.0. See [LICENSE](LICENSE).
