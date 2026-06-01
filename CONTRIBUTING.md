# Contributing

## Build Compatibility

This repository builds and tests against Java 21 and the Spring Boot dependency-management
version pinned by `springBootVersion` in `gradle.properties` (currently Spring Boot 3.5.7).

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
./gradlew spotlessCheck checkstyleMain checkstyleTest test
./gradlew :contrast-mcp-stdio-app:bootJar
./gradlew :contrast-mcp-core:publishToMavenLocal :contrast-mcp-core:verifyCorePublicationMetadata
make check-test
make verify
```

Checkstyle rules and suppressions are the same rules used before the Gradle split. Gradle binds `checkstyle.xml` and `checkstyle-suppressions.xml` to each module, and rules remain `error` severity.

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
