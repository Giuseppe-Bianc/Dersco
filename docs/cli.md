# CLI reference

The Dersco command-line application is named `dersco`. It is implemented with Picocli and exposes two project commands, `check` and `compile`, plus the standard help and version options.

## Global command

```text
dersco [OPTIONS] COMMAND
```

When no subcommand is supplied, Dersco prints the command usage information.

Built-in options include:

```bash
dersco --help
dersco --version
```

## Logging options

Both `check` and `compile` expose the logging mixin.

| Option | Effect |
| --- | --- |
| `-q`, `--quiet` | Suppress non-essential output. Sets the root logger to `ERROR`. |
| `-v`, `--verbose` | Increase verbosity to `INFO`. |
| `-vv` | Increase verbosity to `DEBUG`. |
| `-vvv` | Increase verbosity to `TRACE`. |

Without a verbosity flag, the root logger uses `WARN`.

Examples:

```bash
dersco check source.der
dersco check -v source.der
dersco compile -vv source.der
dersco compile -q source.der
```

## `check`

Checks a source file without producing a compilation output.

```text
dersco check [OPTIONS] FILE
```

Example:

```bash
./gradlew run --args="check examples/hello.der"
```

The command validates that `FILE` exists and is readable, then runs the compiler service's syntax-check path. The current implementation loads the source as UTF-8, tokenizes it, and renders lexer errors with source context.

### Exit codes

| Code | Meaning |
| ---: | --- |
| `0` | The check completed without detected syntax errors. |
| `1` | A syntax or compiler error was detected. |

An unreadable or missing input file is reported as a Picocli parameter error.

## `compile`

Runs the current compilation entry point for a source file.

```text
dersco compile [OPTIONS] FILE
```

### Options

| Option | Default | Meaning |
| --- | --- | --- |
| `-o`, `--output FILE` | `a.exe` | Requested output path. |
| `-O`, `--optimize LEVEL` | `NONE` | Optimization level: `NONE`, `BASIC`, or `AGGRESSIVE`. |
| `--emit-ir` | disabled | Request intermediate representation output. |
| `--diagnostics` | disabled | Enable advanced diagnostics. |

Example:

```bash
./gradlew run --args="compile examples/hello.der --output build/hello.exe --optimize BASIC"
```

### Current limitation

The CLI already models output, optimization, IR, and advanced diagnostics, but the backend is not connected yet. `DefaultCompilerService.compile(...)` currently delegates to `checkSyntax(...)` and then returns. Therefore the current command does **not** create the requested `--output` file and does not emit IR.

Documenting those flags as implemented backend features would be incorrect until code generation is added.

### Exit codes

| Code | Meaning |
| ---: | --- |
| `0` | The current compilation entry point completed after the front-end check. |
| `1` | A compiler error was detected. |

## Running from the repository

During development, invoke the application through Gradle:

```bash
./gradlew run --args="--help"
./gradlew run --args="check path/to/source.der"
./gradlew run --args="compile path/to/source.der"
```

After creating the shaded JAR:

```bash
./gradlew shadowJar
java -jar app/build/libs/Dersco-0.1.0.jar --help
```

## Source file encoding

The current compiler service reads source files using UTF-8. This is the encoding to use for Dersco source files until the language specification defines a different policy.
