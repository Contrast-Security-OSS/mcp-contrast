# Hosted Core Consumption

This document records the Slice 3B local development and verification flow for
`contrast-mcp-core`. It implements the ADR-009, ADR-011, and ADR-012 direction:
`mcp-contrast` publishes a public core library, while `aiml-hosted-mcp-server`
depends only on that core library and never on `contrast-mcp-stdio-app`.

## Local Composite Build

Use this path when `mcp-contrast` and `aiml-services` are sibling checkouts under
the same parent directory:

```bash
cd /Users/chrisedwards/projects/contrast/mcp-contrast
./hack/verify-core-publication.sh

cd /Users/chrisedwards/projects/contrast/aiml-services
./scripts/verify-hosted-mcp-core-classpath.sh
```

`aiml-services/settings.gradle.kts` conditionally includes `../mcp-contrast` when
that directory exists. Gradle then substitutes
`com.contrast.labs.ai.mcp:contrast-mcp-core` with the local
`:contrast-mcp-core` project. Engineers without the sibling checkout use the
published artifact path below.

## Artifact Fallback

Until Artifactory publishing is approved for the public repository, local
composite builds are the supported verification path. After approval, publish
the snapshot or release artifact from `mcp-contrast` using configured Contrast
Artifactory credentials in `~/.gradle/gradle.properties`:

```bash
cd /Users/chrisedwards/projects/contrast/mcp-contrast
./gradlew :contrast-mcp-core:publishMavenJavaPublicationToContrastInternalSnapshotRepository
```

For release versions, the repository task name changes to
`publishMavenJavaPublicationToContrastInternalReleaseRepository`.

## Boundary Contract

The hosted service may compile against minimal core support types during Slice
3B, but it must not register public shared tools until Slice 5. The hosted
classpath scan must prove these stdio/local-only classes are absent:

- `McpContrastApplication`
- `ContrastProperties`
- `ContrastSDKFactory`
- `SDKExtensionFactory`
- local `SdkApiClient`
- `SDKHelper` and SDK extension caches
- `GetSastResultsTool`

The scan emits sanitized diagnostic logs: command, gate name, resolved
coordinate/source, transitive dependency summary, forbidden class assertions,
and assertion summary. Credentials, bearer tokens, raw claims, cursor values,
internal response bodies, and stack traces are not logged.
