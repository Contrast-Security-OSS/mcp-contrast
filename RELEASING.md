# Release Process

This document describes how to create a new release of mcp-contrast.

## Overview

This project uses the **Gradle Release** GitHub Actions workflow. Release versions come from `vX.Y.Z` Git tags via the Axion release plugin; `application.properties` receives the Axion-computed version during the Gradle resource-processing step.

## Prerequisites

- Maintainer access to the [mcp-contrast GitHub repository](https://github.com/Contrast-Security-OSS/mcp-contrast)
- Access to trigger GitHub Actions workflows
- A green `main` branch

## Pre-release Testing

Before you cut a release, exercise the shipped tools against a live org. From a
Claude Code session on the release branch, run `/test-mcp-server`. It builds the
stdio jar fresh, wires that exact artifact into a separate headless Claude
instance, and has that instance test every tool it discovers using credentials
from `.env.integration-test`. Pass `smoke` for a fast is-it-alive pass or run it
plain for in-depth exploratory testing. The run is read-only and prints a report
for you to read. Treat it as a judgement aid, not an automatic gate. See
`.claude/skills/test-mcp-server/` for details.

## Release Steps

1. Ensure all desired changes are merged to `main`.
2. Confirm CI is passing.
3. Navigate to the [GitHub Actions](https://github.com/Contrast-Security-OSS/mcp-contrast/actions) page.
4. Select the **Gradle Release** workflow.
5. Click **Run workflow** and select `main`.
6. Leave `release_version` blank for a normal patch release, or enter an explicit `X.Y.Z` version for a major, minor, or migration release.
7. Start the run.

The first Axion-managed release must be run with `release_version=2.0.0`. After `v2.0.0` exists, leaving `release_version` blank creates the next patch release from the latest `vX.Y.Z` tag.

## What the Workflow Does

1. Validates that the workflow was dispatched from `main`.
2. Validates the optional `release_version` input.
3. Runs the release validation build.
4. Creates or reuses the `vX.Y.Z` release tag with Axion.
5. Checks out the release tag.
6. Verifies that `./gradlew -q printVersion` matches the tag version.
7. Builds `contrast-mcp-stdio-app/build/libs/mcp-contrast-{version}.jar` from the tag.
8. Generates CycloneDX and SPDX JSON SBOMs for the release JAR.
9. Publishes `contrast-mcp-core` to Artifactory (signed with PGP).
10. Attests build provenance and both SBOM formats for the release JAR.
11. Builds the Docker image and generates CycloneDX and SPDX JSON SBOM release assets from the loaded image.
12. Signs the Docker image with Docker Content Trust and publishes the release and `latest` tags.
13. Creates the GitHub release and attaches the JAR plus all four JAR and Docker image SBOM files.

The workflow does not commit release-version or next-snapshot changes to `main`.
The release-attached Docker image SBOMs are convenience artifacts; a separate repository generates the registry-bound image SBOM.

## Versioning

This project follows [Semantic Versioning](https://semver.org/) with the format `MAJOR.MINOR.PATCH`.

- **MAJOR**: Incompatible API changes, such as tool renames or parameter changes
- **MINOR**: Backwards-compatible functionality additions
- **PATCH**: Backwards-compatible bug fixes

For a normal patch release, leave `release_version` blank. For an intentional major or minor release, enter the exact release version in the workflow input without a leading `v` and without `-SNAPSHOT`.

Accepted examples:

```text
2.0.0
2.1.0
3.0.0
```

Rejected examples:

```text
v2.0.0
2.0.0-SNAPSHOT
latest
```

## Manual Release

Use this only if the workflow cannot run:

```bash
git switch main
git pull
./gradlew clean spotlessCheck check :contrast-mcp-stdio-app:integrationTest :contrast-mcp-stdio-app:bootJar
./gradlew release -Prelease.forceVersion=X.Y.Z
git checkout vX.Y.Z
./gradlew clean :contrast-mcp-stdio-app:bootJar -x test
test -f contrast-mcp-stdio-app/build/libs/mcp-contrast-X.Y.Z.jar
```

Then create a GitHub release for the tag and attach `contrast-mcp-stdio-app/build/libs/mcp-contrast-X.Y.Z.jar`. Note that these manual instructions do not publish to Artifactory, sign the Docker image, generate SBOMs, or create attestations. Use the workflow whenever possible.

## Verify the Release

This checklist describes an automated workflow release. For a manual recovery release, verify only the outputs you explicitly reproduced and document any omitted publication, SBOM, or attestation assets in the release notes.

- GitHub release exists with the expected tag, attached JAR, and CycloneDX and SPDX JSON SBOMs for both the JAR and Docker image.
- All four downloaded SBOM files are non-empty and pass `jq empty <file>`.
- `gh attestation verify mcp-contrast-X.Y.Z.jar --repo Contrast-Security-OSS/mcp-contrast` succeeds.
- DockerHub has the version tag and updated `latest` tag (signed with DCT).
- `contrast-mcp-core` artifact is available in Artifactory at the release version.
- `main` has no release-version or next-snapshot commits from the workflow.
- `./gradlew -q printVersion` on the release tag prints `X.Y.Z`.

## Troubleshooting

**Workflow fails with tracked local modifications**

The release workflow expects tracked files to stay clean before tagging and before publishing. Confirm no generated files are modifying tracked files.

**Recovering from a failed run after tag creation**

If a run fails after creating `vX.Y.Z`, rerun the workflow from `main` with `release_version=X.Y.Z`. The workflow reuses the tag only if it already points at the current `main` commit; it fails if the tag points somewhere else.

**Version is not reflected in the app**

Confirm `contrast-mcp-stdio-app/src/main/resources/application.properties` still uses the `@project.version@` placeholder and `:contrast-mcp-stdio-app:processResources` is configured to replace it.

**Docker publishing fails in a fork**

DockerHub credentials and signing keys are only available in the main repository. A fork can validate the JAR release path, but Docker publishing is expected to fail without those secrets.

## Testing the Release Workflow in a Fork

1. Fork the repository.
2. Enable GitHub Actions and read/write workflow permissions in the fork.
3. Push the branch containing `.github/workflows/gradle-release.yml` to the fork's `main` branch.
4. Run **Gradle Release** from the Actions tab.
5. Verify the release tag, JAR build, and JAR CycloneDX and SPDX SBOM generation.
6. Without equivalent Artifactory, DockerHub, and signing configuration, expect the workflow to stop before Docker SBOM generation and the GitHub release. If the fork is fully configured, verify the attached JAR and all four SBOM files.
7. Delete any test release and tag created by the run when done.

## Support

For issues with the release process, check the [GitHub Actions logs](https://github.com/Contrast-Security-OSS/mcp-contrast/actions) and open an issue in the repository with the failed workflow run link.
