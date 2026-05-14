# Release Process

This document describes how to create a new release of mcp-contrast.

## Overview

This project uses the **Gradle Release** GitHub Actions workflow. The release version comes from `gradle.properties`; `application.properties` receives that version during the Gradle resource-processing step.

## Prerequisites

- Maintainer access to the [mcp-contrast GitHub repository](https://github.com/Contrast-Security-OSS/mcp-contrast)
- Access to trigger GitHub Actions workflows
- A green `main` branch

## Release Steps

1. Ensure all desired changes are merged to `main`.
2. Confirm CI is passing.
3. Navigate to the [GitHub Actions](https://github.com/Contrast-Security-OSS/mcp-contrast/actions) page.
4. Select the **Gradle Release** workflow.
5. Click **Run workflow**, select `main`, and start the run.

If `gradle.properties` contains `version=1.0.1-SNAPSHOT`, the workflow releases `1.0.1` and then updates `main` to `1.0.2-SNAPSHOT`.

## What the Workflow Does

1. Reads the current version with `./gradlew -q printVersion`.
2. Updates `gradle.properties` to the release version and commits it.
3. Tags the release as `v{version}`.
4. Updates `gradle.properties` to the next patch `-SNAPSHOT` version and commits it.
5. Pushes commits and the tag.
6. Builds `contrast-mcp-stdio-app/build/libs/mcp-contrast-{version}.jar`.
7. Creates the GitHub release and attaches the JAR.
8. Builds and publishes the Docker image with the release and `latest` tags.

## Versioning

This project follows [Semantic Versioning](https://semver.org/) with the format `MAJOR.MINOR.PATCH`.

- **MAJOR**: Incompatible API changes, such as tool renames or parameter changes
- **MINOR**: Backwards-compatible functionality additions
- **PATCH**: Backwards-compatible bug fixes

To control a major or minor bump, update `version` in `gradle.properties` to the desired `-SNAPSHOT` version, merge that change to `main`, then run the release workflow.

## Manual Release

Use this only if the workflow cannot run:

```bash
git switch main
git pull
perl -0pi -e 's/^version=.*/version=1.0.1/m' gradle.properties
./gradlew clean :contrast-mcp-stdio-app:bootJar -x test
git add gradle.properties
git commit -m "[gradle-release] prepare release v1.0.1"
git tag v1.0.1
perl -0pi -e 's/^version=.*/version=1.0.2-SNAPSHOT/m' gradle.properties
git add gradle.properties
git commit -m "[gradle-release] prepare next development iteration"
git push origin main
git push origin v1.0.1
```

Then create a GitHub release for the tag and attach `contrast-mcp-stdio-app/build/libs/mcp-contrast-1.0.1.jar`.

## Verify the Release

- GitHub release exists with the expected tag and attached JAR.
- DockerHub has the version tag and updated `latest` tag.
- `main` contains the next `-SNAPSHOT` version in `gradle.properties`.
- No uncommitted release changes remain.

## Troubleshooting

**Workflow fails with local modifications**

The release workflow expects a clean checkout. Confirm no generated files are being modified by the build.

**Version is not reflected in the app**

Confirm `contrast-mcp-stdio-app/src/main/resources/application.properties` still uses the `@project.version@` placeholder and `:contrast-mcp-stdio-app:processResources` is configured to replace it.

**Docker publishing fails in a fork**

DockerHub credentials and signing keys are only available in the main repository. A fork can validate the JAR release path, but Docker publishing is expected to fail without those secrets.

## Testing the Release Workflow in a Fork

1. Fork the repository.
2. Enable GitHub Actions and read/write workflow permissions in the fork.
3. Push the branch containing `.github/workflows/gradle-release.yml` to the fork's `main` branch.
4. Run **Gradle Release** from the Actions tab.
5. Verify the release tag, attached JAR, and version commits.
6. Delete the test release and tag when done.

## Support

For issues with the release process, check the [GitHub Actions logs](https://github.com/Contrast-Security-OSS/mcp-contrast/actions) and open an issue in the repository with the failed workflow run link.
