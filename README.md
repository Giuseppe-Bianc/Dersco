# Dersco

[![CI / CD Workflow](https://github.com/Giuseppe-Bianc/Dersco/actions/workflows/ci.yml/badge.svg)](https://github.com/Giuseppe-Bianc/Dersco/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/Giuseppe-Bianc/Dersco/graph/badge.svg)](https://codecov.io/gh/Giuseppe-Bianc/Dersco)
[![Java 25](https://img.shields.io/badge/Java-25-orange.svg)](https://jdk.java.net/25/)
[![Gradle](https://img.shields.io/badge/Gradle-9.7-blue.svg)](https://gradle.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

Dersco is a Java 25 compiler project for the Dersco programming language. The current implementation provides a Picocli command-line interface, UTF-8 source loading, lexical analysis, source location tracking, structured tokens, and diagnostic rendering. The compiler service currently stops after lexing. Parsing, semantic analysis, intermediate representation, and code generation are represented by project structure and request models but are not connected to the compilation flow.

## Current status

The implemented execution path is:

```text
CLI command
    |
    v
input validation
    |
    v
DefaultCompilerService
    |
    +--> UTF-8 source loading
    |
    +--> Lexer
    |      |
    |      +--> tokens
    |      +--> lexical errors
    |
    +--> ErrorReporter for lexical errors
    |
    +--> debug logging of tokens when lexing succeeds
    |
    v
return to CLI
```

`dersco check` executes the syntax-check entry point. Despite the command name and its current user-facing description, the service currently performs lexical analysis and does not invoke a parser. A successful run therefore means that no lexer errors were reported.

`dersco compile` builds a `CompilationRequest` and passes it to `DefaultCompilerService.compile(...)`. The service currently calls `checkSyntax(...)` and then returns. The requested output path, optimization level, IR flag, and advanced diagnostics flag are stored in the request but are not consumed by a backend. A successful `compile` invocation therefore does not create the requested output file or emit IR.

## Implemented capabilities

- Java 25 toolchain with Gradle 9.7.
- Picocli CLI with `compile`, `check`, help, and version support.
- Repeatable logging controls: `-v`, `-vv`, `-vvv`, and `-q`.
- UTF-8 source loading with `StandardCharsets.UTF_8`.
- Lexer infrastructure based on `Lexer` and `SourceCursor`.
- Source line tracking and source spans used by diagnostics.
- Typed tokens, token kinds, and payload-bearing token representations.
- Numeric token parsing infrastructure under `lexer/token/number`.
- Structured compiler errors with phases, severities, error codes, and source-aware reporting.
- `ICompilerService` abstraction with a default implementation that can be replaced in CLI tests.
- JUnit 6 and AssertJ tests.
- Quality gates using Checkstyle, PMD, SpotBugs, Error Prone, Spotless, and JaCoCo.
- GitHub Actions CI and tagged-release configuration.

## Repository layout

```text
Dersco/
├── app/
│   ├── config/
│   │   ├── checkstyle/                 # Checkstyle configuration
│   │   └── pmd/                        # PMD ruleset
│   ├── src/
│   │   ├── main/java/org/dersbian/
│   │   │   ├── App.java                # Application entry point
│   │   │   ├── cli/                    # Picocli commands and logging configuration
│   │   │   ├── compiler/
│   │   │   │   ├── error/              # Compiler error model and diagnostic reporting
│   │   │   │   ├── lexer/              # Lexer, source cursor, code-point helpers, tokens
│   │   │   │   ├── location/            # Source line/location tracking support
│   │   │   │   └── syntax/              # Reserved structure for syntax/AST work
│   │   │   └── util/                   # Shared file and size utilities
│   │   └── test/java/                  # Unit tests
│   └── build.gradle.kts                # Application, dependency, quality, and packaging configuration
├── docs/
│   ├── architecture.md                # Current compiler architecture and execution flow
│   └── cli.md                         # Command-line contract and current limitations
├── gradle/
│   └── libs.versions.toml             # Dependency version catalog
├── .github/
│   ├── dependabot.yml                 # Dependency update configuration
│   └── workflows/
│       └── ci.yml                     # CI and release workflow
├── codecov.yml                         # Coverage configuration
├── build.gradle.kts                    # Root Gradle configuration
├── settings.gradle.kts                 # Project/module configuration and Foojay resolver
└── LICENSE
```

## Requirements

- JDK 25.
- Git.
- No system Gradle installation is required because the repository includes the Gradle wrapper.

The build uses the Foojay Gradle toolchain resolver. When supported by the local environment, Gradle can provision the configured JDK automatically.

## Getting started

Clone the repository and run the verification task:

```bash
git clone https://github.com/Giuseppe-Bianc/Dersco.git
cd Dersco
./gradlew check
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Command-line usage

Run the application through Gradle during development:

```bash
./gradlew run --args="check path/to/source.der"
./gradlew run --args="compile path/to/source.der"
```

Build the shaded application JAR:

```bash
./gradlew shadowJar
```

The artifact is written under `app/build/libs/`. The configured project version is `0.1.0`, so the normal shaded artifact name is `Dersco-0.1.0.jar`.

Run the packaged application:

```bash
java -jar app/build/libs/Dersco-0.1.0.jar --help
java -jar app/build/libs/Dersco-0.1.0.jar check path/to/source.der
```

See [docs/cli.md](docs/cli.md) for command syntax, options, exit codes, and current behavior.

## Development workflow

### Run tests

```bash
./gradlew test
```

Run a specific test class:

```bash
./gradlew test --tests "*NumericParsersTest*"
```

### Run the verification gate

```bash
./gradlew check
```

The configured `check` task depends on Checkstyle, PMD, SpotBugs, Spotless verification, and the JaCoCo test report. Tests are executed as part of the JaCoCo report task dependency.

### Apply formatting

```bash
./gradlew spotlessApply
```

Spotless uses Google Java Format 1.35.0 in AOSP mode with four-space indentation, plus unused-import removal and annotation formatting.

### Build the shaded JAR

```bash
./gradlew shadowJar
```

The application main class is `org.dersbian.App`. The JAR manifest also contains the implementation version and native-access metadata.

## Dependencies and build tooling

Runtime and compile-time dependencies are managed through `gradle/libs.versions.toml`. The application currently uses:

- Picocli 4.7.7 for the command-line interface and code generation annotations.
- SLF4J 2.0.18 and Logback 1.6.3 for logging.
- Jansi 2.4.3 as a runtime terminal dependency.
- Guava 33.6.0-jre.
- Error Prone 2.50.0 for compiler-time static analysis.

Test dependencies are JUnit Jupiter 6.1.3 and AssertJ 3.27.7. The Gradle build configures the JUnit Platform and enables parallel test forks according to available processors.

Quality tooling includes Checkstyle 13.7.0, PMD 7.26.0, SpotBugs 4.10.2, Spotless with Google Java Format 1.35.0, and JaCoCo 0.8.14. Shadow 9.6.1 produces the shaded application JAR.

## CI/CD

The repository contains a GitHub Actions workflow for verification, packaging, coverage reporting, and tagged releases. The documented local equivalents are:

```bash
./gradlew check
./gradlew shadowJar
./gradlew jacocoTestReport
```

JaCoCo writes the XML report to `app/build/reports/jacoco/test/jacocoTestReport.xml`. Codecov is configured through `codecov.yml`, and the CI upload is configured not to fail the workflow when Codecov itself reports an upload error.

Tagged releases matching `v*` use the release job after the build job succeeds. Dependabot is configured for Gradle dependencies and GitHub Actions dependencies.

## Architecture boundaries

The code is deliberately separated into CLI, compiler service, lexical analysis, source-location, token, diagnostic, and utility layers. `ICompilerService` is the boundary between the CLI and the compiler implementation. `DefaultCompilerService` currently coordinates file loading, lexing, and diagnostic reporting.

The repository also contains `compiler/syntax/ast` and token parsing infrastructure for future language-processing stages. These structures should not be described as an active parser or semantic pipeline until `DefaultCompilerService` invokes them.

For the detailed component responsibilities and current data flow, see [docs/architecture.md](docs/architecture.md). For the externally visible command contract, see [docs/cli.md](docs/cli.md).

## License

Dersco is licensed under the Apache License 2.0. See [LICENSE](LICENSE).
