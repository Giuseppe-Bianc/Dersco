# Dersco

[![CI / CD Workflow](https://github.com/Giuseppe-Bianc/Dersco/actions/workflows/ci.yml/badge.svg)](https://github.com/Giuseppe-Bianc/Dersco/actions/workflows/ci.yml)
[![codecov](https://codecov.io/gh/Giuseppe-Bianc/Dersco/graph/badge.svg)](https://codecov.io/gh/Giuseppe-Bianc/Dersco)
[![Java 25](https://img.shields.io/badge/Java-25-orange.svg)](https://jdk.java.net/25/)
[![Gradle](https://img.shields.io/badge/Gradle-9.7-blue.svg)](https://gradle.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](LICENSE)

Dersco is a Java-based compiler project for the Dersco programming language. The repository currently implements the command-line interface, UTF-8 source loading, source-location tracking, lexical analysis, token modeling, and compiler diagnostics. Parsing, semantic analysis, intermediate representation generation, and code generation are not connected to the compiler service yet.

## Project status

The implemented front-end flow is:

```text
Dersco source file
        |
        v
UTF-8 file loading
        |
        v
Lexer + source cursor + line tracking
        |
        v
Token stream + lexer errors
        |
        v
Diagnostic rendering
```

`dersco check` validates the input file, tokenizes it, renders lexical diagnostics, and returns a non-zero command status when lexer errors are found. Despite the command's historical `checkSyntax` service name, a parser is not currently invoked, so this must not be interpreted as a complete syntax check.

`dersco compile` builds a `CompilationRequest` and passes it to `DefaultCompilerService`. The service currently invokes the same front-end validation and then stops. The requested output path, optimization level, IR flag, and advanced diagnostics flag are captured by the request object but are not consumed by a backend. Therefore the current command does not create the requested output file or emit IR.

See [CLI reference](docs/cli.md) for the command contract and [architecture notes](docs/architecture.md) for the implementation structure and execution flow.

## Implemented capabilities

- Java 25 toolchain with Gradle 9.7.
- Picocli CLI with `compile`, `check`, and built-in help/version support.
- Repeatable logging controls: `-v`, `-vv`, `-vvv`, and `-q`.
- UTF-8 source loading through `java.nio.file.Files`.
- Lexer infrastructure with a dedicated source cursor, code-point helpers, and line tracking.
- Strongly typed tokens with source spans and payload-bearing token values.
- Structured compiler errors and diagnostic rendering with source context.
- Compiler service abstraction suitable for unit testing through dependency injection.
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
│   │   │   ├── App.java            # Application bootstrap
│   │   │   ├── cli/                # Picocli commands and logging options
│   │   │   ├── compiler/           # Compiler service, lexer, locations, diagnostics, AST models
│   │   │   └── util/               # Shared file-size and path utilities
│   │   └── test/java/              # Unit tests
│   └── build.gradle.kts             # Application build and quality configuration
├── docs/
│   ├── architecture.md             # Compiler architecture and execution flow
│   └── cli.md                      # Command-line reference
├── dr_files/                       # Dersco source fixtures used by development/testing
├── gradle/
│   └── libs.versions.toml          # Dependency version catalog
├── .github/
│   ├── dependabot.yml              # Dependency update configuration
│   └── workflows/
│       └── ci.yml                  # CI and release workflow
├── codecov.yml                     # Coverage configuration
├── gradle.properties               # Gradle project properties
├── gradlew / gradlew.bat           # Gradle wrapper entry points
├── settings.gradle.kts             # Root project and app module configuration
└── LICENSE
```

The repository also contains development tooling and agent configuration under `.agents`, `.claude`, `.kiro`, and `.specify`. Those directories support development workflows and are not part of the Dersco compiler runtime.

## Requirements

- JDK 25 for local development, unless Gradle's configured Foojay toolchain resolver can provision it automatically.
- Git.
- No system Gradle installation is required because the repository includes the Gradle wrapper.

The Gradle wrapper is pinned to Gradle 9.7.0. The application module selects Java 25 as its toolchain. The root build includes the Foojay resolver convention plugin for toolchain provisioning.

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
./gradlew run --args="check path/to/source.der"
./gradlew run --args="compile path/to/source.der"
```

Build the shaded application JAR:

```bash
./gradlew shadowJar
```

The generated artifact is written under `app/build/libs/`. Its manifest declares `org.dersbian.App` as the main class and includes the project version.

Run the packaged application with:

```bash
java -jar app/build/libs/Dersco-0.1.0.jar --help
java -jar app/build/libs/Dersco-0.1.0.jar check path/to/source.der
```

The exact command-line options and current limitations are documented in [docs/cli.md](docs/cli.md).

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

The configured `check` task depends on Checkstyle, PMD, SpotBugs, Spotless verification, and the JaCoCo test report. Tests also run as part of the JaCoCo report task.

Java compilation uses UTF-8, enables `-Xlint` diagnostics, treats compiler warnings as errors, and runs Error Prone. Spotless applies Google Java Format in AOSP mode with four-space indentation.

### Apply formatting

```bash
./gradlew spotlessApply
```

### Build the executable JAR

```bash
./gradlew shadowJar
```

This creates the shaded JAR and sets its manifest main class to `org.dersbian.App`.

## Dependencies and build tools

The application module uses the following runtime or build dependencies:

| Component | Version | Role |
| --- | --- | --- |
| Java | 25 | Application toolchain |
| Gradle wrapper | 9.7.0 | Build system |
| Picocli | 4.7.7 | CLI parsing and command model |
| SLF4J | 2.0.18 | Logging API |
| Logback | 1.6.3 | Logging implementation |
| Jansi | 2.4.3 | Terminal support runtime dependency |
| Guava | 33.6.0-jre | General-purpose Java utilities dependency |
| JUnit Jupiter | 6.1.3 | Unit testing |
| AssertJ | 3.27.7 | Test assertions |
| Error Prone | 2.50.0 | Java compiler analysis |
| Checkstyle | 13.7.0 | Style verification |
| PMD | 7.26.0 | Static analysis |
| SpotBugs | 4.10.2 | Bytecode static analysis |
| Spotless / Google Java Format | 8.9.0 / 1.35.0 | Formatting |
| JaCoCo | 0.8.14 | Coverage reporting |
| Shadow | 9.6.1 | Shaded application JAR |

Dependency versions are defined in `gradle/libs.versions.toml` and build plugins are configured in `app/build.gradle.kts`.

## CI/CD

The GitHub Actions workflow runs on pushes to `main` or `master`, pull requests targeting those branches, tags matching `v*`, and manual dispatches.

The main job:

1. checks out the repository with full Git history;
2. installs JDK 25 through Temurin;
3. configures Gradle;
4. runs `./gradlew check`;
5. builds the shaded JAR with `./gradlew shadowJar`;
6. uploads JAR artifacts;
7. generates the JaCoCo XML report;
8. uploads coverage to Codecov.

The current workflow sets `fail_ci_if_error: true` for Codecov, so a Codecov upload failure fails that workflow step. A tag starting with `v` triggers a separate release job after the build job succeeds. The release job rebuilds the shaded JAR and attaches it to a GitHub Release.

Dependabot is configured for Gradle dependencies and GitHub Actions dependencies.

For local reproduction of the main verification stages:

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

`codecov.yml` controls project and patch coverage reporting. The CI workflow explicitly uploads this XML report to Codecov.

## Architecture and implementation boundaries

The runtime dependency direction is intentionally small:

```text
Picocli CLI
    |
    v
ICompilerService
    |
    v
DefaultCompilerService
    |
    +--> UTF-8 source loading
    +--> Lexer
    |      +--> SourceCursor
    |      +--> CodePoints
    |      +--> line tracking
    |      +--> token model
    |      +--> lexer errors
    |
    +--> ErrorReporter
    |
    +--> future parser / semantic analysis / IR / code generation
```

`CompilationRequest` is the boundary for future compilation options. At present, `DefaultCompilerService.compile(...)` only uses its `source` field by passing it to `checkSyntax(...)`. The remaining request fields are retained for the future backend.

The `compiler/syntax/ast` package contains AST model and traversal support, but no parser currently produces those nodes during the CLI compilation flow. Do not document the AST package as an active parser stage.

Detailed design information is in [docs/architecture.md](docs/architecture.md).

## License

Dersco is licensed under the Apache License 2.0. See [LICENSE](LICENSE).
