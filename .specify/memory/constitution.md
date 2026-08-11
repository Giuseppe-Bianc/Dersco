<!--
Sync Impact Report
- Version change: unratified scaffold → 1.0.0
- Modified principles: none; five unpopulated template principles were defined.
- Added sections: Technical Constraints; Development Workflow and Compliance.
- Removed sections: none.
- Follow-up TODOs: none.
-->
# Dersco Constitution

## Core Principles

### I. Java 25 and Gradle 9.7.0 Baseline
Production code MUST target Java 25 and the project MUST be built through the Gradle 9.7.0 wrapper.
Build and dependency changes MUST preserve this supported baseline. Preview Java features are forbidden
unless the build and this constitution are explicitly amended to support them. This keeps compiler
semantics, developer environments, and automated builds reproducible.

### II. Explicit Compiler Boundaries
Each compiler capability MUST have a defined phase boundary and an observable contract. The CLI,
compiler service, lexer, diagnostic model, and general utilities MUST retain their distinct
responsibilities; compiler-domain behavior MUST NOT be placed in generic utilities. Source handling
and diagnostic output MUST remain UTF-8 safe. These boundaries make language behavior testable and
allow phases to evolve without hiding coupling or duplicating error reporting.

### III. JUnit Conformance and Test Design (NON-NEGOTIABLE)
All test creation, implementation, and maintenance MUST use JUnit 9.1.3 or a later compatible
version. Official documentation for the adopted JUnit version is the primary authority for APIs,
annotations, execution, and conventions; documented, established JUnit community practices MAY
supplement it. Tests MUST use only supported, documented JUnit facilities and MUST NOT introduce
obsolete, deprecated, undocumented, or version-incompatible usage. Every test MUST have an
identifiable purpose, assert observable behavior, and remain readable, isolated, deterministic,
reproducible, and maintainable. This prevents passing tests from concealing unreliable verification.

### IV. Complete Behavioral Coverage
For every changed behavior, the relevant suite MUST cover representative ordinary use, applicable
corner cases, and applicable edge cases, including inclusive and exclusive boundaries and values
immediately around them. When the specification permits, tests MUST also cover invalid input,
missing data, unexpected conditions, and problematic parameter combinations. Tests MUST be
independent of execution order and external mutable state; filesystem tests MUST use isolated
temporary directories. Manual source samples are smoke-test inputs unless a test explicitly adopts
them. Coverage is adequate only when it demonstrates the specified behavior and meaningful domain
limits, not merely when it reaches a numerical target.

### V. Red, Green, Refactor (NON-NEGOTIABLE)
Every new feature, behavior change, and technically reproducible defect fix MUST follow TDD. First,
write a test that specifies the required behavior and observe it fail (Red); then add only the code
needed to make it pass (Green); finally improve structure without changing verified behavior
(Refactor), with the relevant suite passing afterwards. Repeat this cycle for each meaningful
increment. A deviation is permitted only for a concrete technical reason and MUST be documented in
the code or design record; convenience is not a valid reason. This makes the test suite an active
specification rather than an after-the-fact check.

## Technical Constraints

- The build MUST use Gradle 9.7.0 and Java 25, with UTF-8 preserved for source files and
  diagnostics.
- Java changes MUST satisfy configured Checkstyle, PMD, SpotBugs, Error Prone with `-Werror`,
  Spotless, and JaCoCo gates. Formatting MUST be applied by the configured Google Java Format AOSP
  profile.
- Tests MUST use JUnit Jupiter on the JUnit Platform. Test classes MUST mirror the corresponding
  production package where applicable, use descriptive names, and keep their fixtures local to the
  test scope.
- Dependency updates, especially JUnit upgrades, MUST be reviewed against the official release
  documentation and MUST update tests when API, convention, or behavior changes affect them.

## Development Workflow and Compliance

Before declaring a change complete, contributors MUST run focused tests for the affected behavior,
run `:app:spotlessCheck` after Java edits, and run `:app:check` for the complete quality gate when
the change can affect the module. Test selection and assertions MUST be proportional to the code's
risks and contracts, avoiding redundant, fragile, ambiguous, or order-dependent tests. Reviews MUST
verify TDD evidence where applicable, the three required test categories, supported JUnit use, and
compliance with the technical constraints. A failed quality gate blocks completion until it is fixed
or a documented constitutional exception has been accepted.

## Governance

This constitution supersedes conflicting development practices and guidance. An amendment MUST
state its rationale, affected principles, migration impact, and any required changes to dependent
artifacts. Compliance MUST be reviewed for every behavior-changing contribution and whenever a
toolchain or testing-framework version changes.

Constitution versions follow semantic versioning: MAJOR for incompatible removal or redefinition of
governance, MINOR for a new principle or material expansion, and PATCH for clarification or
non-semantic refinements. The ratification date records first formal adoption; the last-amended date
changes with every approved amendment. Exceptions require a concrete technical justification,
documented scope, and a review date; they cannot be used merely to reduce implementation effort.

**Version**: 1.0.0 | **Ratified**: 2026-08-11 | **Last Amended**: 2026-08-11
