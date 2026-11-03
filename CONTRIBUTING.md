# Contributing

## Build Compatibility

This repository builds and tests against Java 21 and the Spring Boot dependency-management
version pinned by `springBootVersion` in `gradle.properties` (currently Spring Boot 4.1.0).

The `contrast-mcp-core` module imports the Spring Boot BOM as a regular Gradle `platform()`
so published-library consumers can keep control of their dependency graph. The deployable
`contrast-mcp-stdio-app` module uses `enforcedPlatform()` so the shipped application runs
with the exact Spring-managed dependency set tested by this repository.

No broader Spring Boot compatibility range is claimed unless a downstream consumer validates
that override with the full test suite.

## Contributor Build Workflow

Use the Gradle wrapper from a JDK 21 shell. The public repo is a two-module build:

- `contrast-mcp-core` contains transport-neutral shared support types that can be published as `com.contrast.labs.ai.mcp:contrast-mcp-core`.
- `contrast-mcp-stdio-app` contains the local stdio Spring Boot application, local Contrast SDK credential wiring, SDK helper/cache implementations, and the local-only raw SARIF `get_scan_results` tool.

Common commands:

```bash
./gradlew check
./gradlew :contrast-mcp-stdio-app:bootJar
./gradlew :contrast-mcp-core:publishToMavenLocal :contrast-mcp-core:verifyCorePublicationMetadata
make check
make verify
```

Checkstyle rules and suppressions are the same rules used before the Gradle split. Gradle binds `checkstyle.xml` and `checkstyle-suppressions.xml` to each module, and rules remain `error` severity.

## Test Coverage

JaCoCo measures coverage on every `test` run and writes HTML and XML reports to `<module>/build/reports/jacoco/test/`. `make check` verifies the floors and prints a summary.

Per-module floors live in `ext.coverageMinimums` in the root `build.gradle` and are enforced by `jacocoTestCoverageVerification`, which `check` depends on. They are set just below the measured baseline so they block regression rather than blocking adoption. Raise a floor as coverage improves. Never lower one to make a build pass. CI runs the same verification and uploads the reports as a build artifact.

Changed files face a separate per-file floor: any changed `src/main/java` file must reach 85% line coverage. This catches an undertested file that the aggregate module percentage could otherwise absorb. CI enforces the rule on every pull request, and `make coverage-changed` runs the same check locally. `make install-hooks` installs an optional pre-push hook that runs it before you push. See `scripts/git-hooks/README.md` for detail.

## Consuming `contrast-mcp-core` Locally

Downstream projects can validate unpublished shared-code changes by using a Gradle composite build that substitutes the published `contrast-mcp-core` artifact with a local checkout:

```kotlin
includeBuild("../mcp-contrast") {
    dependencySubstitution {
        substitute(module("com.contrast.labs.ai.mcp:contrast-mcp-core"))
            .using(project(":contrast-mcp-core"))
    }
}
```

Use this only for local development. Released consumers should depend on the published `com.contrast.labs.ai.mcp:contrast-mcp-core` artifact.
