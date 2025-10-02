# Release Process

This document describes how to create a new release of mcp-contrast.

## Overview

This project uses the Maven Release Plugin with GitHub Actions to automate the release process. Version numbers are automatically managed across all files (pom.xml, application.properties, and SDKHelper.java) using Maven's resource filtering feature.

## Prerequisites

- Maintainer access to the [mcp-contrast GitHub repository](https://github.com/Contrast-Security-OSS/mcp-contrast)
- Access to trigger GitHub Actions workflows

## Release Steps

### 1. Prepare for Release

Before starting a release:

1. Ensure the `main` branch is in a releasable state
2. All tests should be passing
3. All desired features/fixes for the release should be merged
4. Review and update the README.md if needed

### 2. Trigger the Release Workflow

1. Navigate to the [GitHub Actions](https://github.com/Contrast-Security-OSS/mcp-contrast/actions) page
2. Select the "Maven Release" workflow from the left sidebar
3. Click "Run workflow" button (top right)
4. Fill in the required inputs:
   - **Release version**: The version number for this release (e.g., `0.0.12`)
   - **Next development version**: The next SNAPSHOT version (e.g., `0.0.13-SNAPSHOT`)
5. Click "Run workflow"

### 3. What Happens Automatically

The GitHub Actions workflow will:

1. **Prepare the Release**:
   - Update version in `pom.xml` to the release version (e.g., `0.0.12`)
   - Update version in `application.properties` via Maven filtering
   - Update version in `SDKHelper.java` (now reads from properties at runtime)
   - Commit changes with `[maven-release-plugin] [skip ci]` prefix
   - Create a git tag (e.g., `v0.0.12`)

2. **Build Release Artifacts**:
   - Build the JAR file with the release version
   - Package as `mcp-contrast-{version}.jar`

3. **Push Changes**:
   - Push version update commits to `main` branch
   - Push the version tag to GitHub

4. **Create GitHub Release**:
   - Create a new GitHub release for the tag
   - Attach the JAR artifact to the release
   - Generate release notes from commit history

5. **Update to Next Development Version**:
   - Update version to next SNAPSHOT (e.g., `0.0.13-SNAPSHOT`)
   - Commit and push changes

6. **Trigger Docker Build** (automatically):
   - The existing `docker-release.yml` workflow triggers on release publication
   - Builds and pushes Docker image to [DockerHub](https://hub.docker.com/r/contrast/mcp-contrast)
   - Tags Docker image with version and `latest`

### 4. Verify the Release

After the workflow completes:

1. **Verify GitHub Release**: Check the [Releases page](https://github.com/Contrast-Security-OSS/mcp-contrast/releases)
   - Release should be published with correct version
   - JAR artifact should be attached
   - Release notes should be generated

2. **Verify Docker Image**: Check [DockerHub](https://hub.docker.com/r/contrast/mcp-contrast)
   - New version tag should be present
   - `latest` tag should be updated

3. **Verify Version Bump**: Check the `main` branch
   - `pom.xml` should show next SNAPSHOT version
   - No uncommitted changes should remain

## Versioning Scheme

This project follows [Semantic Versioning](https://semver.org/) with the format `MAJOR.MINOR.PATCH`:

- **MAJOR**: Incompatible API changes
- **MINOR**: Backwards-compatible functionality additions
- **PATCH**: Backwards-compatible bug fixes

For pre-1.0 releases, we use `0.0.X` versioning where X increments for each release.

## Development Versions

Between releases, the version in `main` branch is always a SNAPSHOT version (e.g., `0.0.13-SNAPSHOT`). This indicates ongoing development and distinguishes development builds from official releases.

## Rollback

If a release needs to be rolled back:

1. Delete the Git tag locally and remotely:
   ```bash
   git tag -d v0.0.12
   git push origin :refs/tags/v0.0.12
   ```

2. Delete the GitHub release through the GitHub web interface

3. Manually revert the version commits if needed:
   ```bash
   git revert <commit-sha>
   git push origin main
   ```

## Manual Release (Emergency Only)

If the automated process fails, you can perform a manual release:

```bash
# Ensure you're on main and up-to-date
git checkout main
git pull

# Run Maven release plugin locally
./mvnw release:clean release:prepare -DreleaseVersion=0.0.12 -DdevelopmentVersion=0.0.13-SNAPSHOT

# Build the JAR
./mvnw clean package -DskipTests

# Manually create GitHub release and upload JAR from target/ directory
```

## Troubleshooting

### Workflow Fails with "local modifications"

The Maven Release Plugin requires a clean working directory. Make sure all changes are committed before triggering the workflow.

### Docker Build Doesn't Trigger

Ensure the GitHub release is marked as "published" (not draft or pre-release). The `docker-release.yml` workflow only triggers on published releases.

### Version Not Updated Correctly

The resource filtering in Maven should automatically update `application.properties`. If versions are out of sync, check that:
- `pom.xml` has `<filtering>true</filtering>` in the resources section
- `application.properties` uses `@project.version@` placeholder
- `SDKHelper.java` reads version from Spring Environment

## Testing the Release Process

To test the release process without affecting production users, you have several options:

### Option 1: Local Dry-Run (Recommended First Step)

Test the Maven release plugin locally without making any changes:

```bash
# Clean any previous release attempts
./mvnw release:clean

# Dry-run to test the release process
./mvnw release:prepare -DdryRun=true -DreleaseVersion=0.0.12 -DdevelopmentVersion=0.0.13-SNAPSHOT

# Clean up dry-run artifacts
./mvnw release:clean
```

This validates:
- Version updates in pom.xml
- Git operations (simulated)
- Build process

**Does NOT:**
- Push to GitHub
- Create tags
- Publish artifacts

### Option 2: Test on a Fork

For full end-to-end testing:

1. Fork the repository to your personal GitHub account
2. Update the workflow file to push to your fork's main branch
3. Run the Maven Release workflow
4. Verify the release process completes successfully
5. Delete the test release and tags from your fork

**Pros:** Tests the entire process including GitHub Actions
**Cons:** Requires fork management and cleanup

### Option 3: Use Draft Releases (Safest Production Test)

Modify the workflow temporarily to create draft releases:

1. Edit `.github/workflows/maven-release.yml`
2. Change line 61: `draft: false` to `draft: true`
3. Run the workflow with a test version like `0.0.12-test`
4. Verify everything works
5. Delete the draft release and test tag
6. Revert the workflow change

**Important:** Draft releases do NOT trigger the Docker build workflow, so users won't be affected.

### Option 4: Use Pre-release Versions

Create a release with pre-release flag:

1. Use version like `0.0.12-rc1` (release candidate)
2. Set `prerelease: true` in the workflow (line 62)
3. This creates a visible release but marked as "Pre-release"

**Note:** Pre-releases still trigger Docker builds but are clearly marked as non-stable.

### Recommended Testing Workflow

Before your first production release:

1. **Local dry-run** to validate Maven plugin configuration
2. **Fork testing** to validate GitHub Actions integration
3. **Draft release** in production repo to validate permissions and workflows
4. **Production release** once confident

For subsequent releases, a local dry-run is usually sufficient.

## Support

For issues with the release process, please:
1. Check the [GitHub Actions logs](https://github.com/Contrast-Security-OSS/mcp-contrast/actions)
2. Open an issue in the [GitHub repository](https://github.com/Contrast-Security-OSS/mcp-contrast/issues)
3. Reference this document and the [Handover documentation](https://contrast.atlassian.net/wiki/spaces/CL/pages/4293132316/Handover) in Confluence
