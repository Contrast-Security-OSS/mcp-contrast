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
4. Select the branch (usually `main`)
5. Click "Run workflow"

**Note:** The release version is automatically determined from `pom.xml`. If `pom.xml` shows `0.0.12-SNAPSHOT`, the workflow will:
- Release version `0.0.12`
- Set next development version to `0.0.13-SNAPSHOT` (automatic patch increment)

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

### Controlling Version Numbers

The release workflow automatically increments the **patch** version. To control major or minor version bumps:

1. **Edit `pom.xml`** to set the desired next version with `-SNAPSHOT`:
   ```xml
   <version>1.0.0-SNAPSHOT</version>  <!-- For major bump -->
   <version>0.1.0-SNAPSHOT</version>  <!-- For minor bump -->
   <version>0.0.15-SNAPSHOT</version> <!-- To skip versions -->
   ```

2. **Commit and merge** the version change to `main`

3. **Run the release workflow** - it will release that version and auto-increment the patch

This approach provides full control while maintaining automation. Version changes are tracked in git history and reviewable via pull requests.

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

To test the release process without affecting production users, test it on a personal fork of the repository. This validates the entire end-to-end workflow safely.

### Step 1: Fork the Repository

1. Go to https://github.com/Contrast-Security-OSS/mcp-contrast
2. Click **Fork** (top right)
3. **Uncheck** "Copy the main branch only" to include all branches
4. Click **Create fork**

### Step 2: Enable GitHub Actions in Your Fork

1. In your fork, go to **Settings** → **Actions** → **General**
2. Under **Actions permissions**, select **"Allow all actions and reusable workflows"**
3. Under **Workflow permissions**, select **"Read and write permissions"**
4. Click **Save**

### Step 3: Set Up Local Environment

```bash
# Add your fork as a remote (replace YOUR_USERNAME)
git remote add fork https://github.com/YOUR_USERNAME/mcp-contrast.git

# Verify remotes
git remote -v

# Fetch your fork
git fetch fork
```

### Step 4: Merge Changes to Your Fork's Main Branch

The workflow file must be on the default branch (main) for GitHub to display it in the Actions tab.

```bash
# Create a branch tracking your fork's main
git checkout -b fork-main fork/main

# Merge your feature branch
git merge AIML-82 -m "Add maven-release workflow for testing"

# Push to your fork's main branch
git push fork fork-main:main
```

### Step 5: Run the Test Release Workflow

1. Go to your fork: `https://github.com/YOUR_USERNAME/mcp-contrast`
2. Click the **Actions** tab
3. If workflows don't appear, refresh the page
4. Click **Maven Release** in the left sidebar
5. Click the green **Run workflow** button
6. Select branch: `main`
7. Click **Run workflow**

**Note:** The version is automatically read from `pom.xml`. If `pom.xml` shows `0.0.12-SNAPSHOT`, it will release `0.0.12` and create `0.0.13-SNAPSHOT` for next development.

### Step 6: Verify the Release

The workflow will:
- ✅ Update versions in all files
- ✅ Build the JAR artifact
- ✅ Create tag (e.g., `v0.0.12`)
- ✅ Create GitHub release with JAR attached
- ❌ Docker build will fail (expected - you don't have DockerHub credentials)

Check the results:

1. **Verify JAR artifact:**
   - Go to **Releases** in your fork
   - Find the release matching your version
   - Under **Assets**, confirm the JAR file is attached

2. **Verify workflow success:**
   - Go to **Actions** tab
   - **Maven Release** should show green checkmark ✅
   - **Build and Push Docker Image** will show red X ❌ (this is expected and okay)

3. **Verify version updates:**
   - Check the `test-release` branch was created
   - View the release commits to see version changes

### Step 7: Cleanup Your Fork

After successful testing, clean up:

1. **Delete the test release:**
   - Go to **Releases**
   - Click **Edit** on the release you created
   - Scroll down and click **Delete this release**

2. **Delete the test tag:**
   ```bash
   # Delete tag (replace VERSION with your version, e.g., v0.0.12)
   git push fork :refs/tags/vVERSION
   ```

3. **Optional: Reset your fork's main branch** to match upstream:
   ```bash
   # Fetch latest from upstream
   git fetch origin main

   # Reset your fork's main to match upstream (removes test commits)
   git push fork origin/main:main --force
   ```

3. **Clean up local branches:**
   ```bash
   git checkout main
   git branch -D fork-main
   ```

### What This Testing Validates

✅ Maven Release Plugin configuration
✅ Version synchronization across all files (pom.xml, application.properties, SDKHelper.java)
✅ JAR artifact building with correct version
✅ Git tagging
✅ GitHub release creation
✅ JAR attachment to release
✅ GitHub Actions workflow execution

**Note:** Docker publishing cannot be fully tested in a fork since it requires DockerHub credentials and signing keys configured in the main repository's secrets.

## Support

For issues with the release process, please:
1. Check the [GitHub Actions logs](https://github.com/Contrast-Security-OSS/mcp-contrast/actions)
2. Open an issue in the [GitHub repository](https://github.com/Contrast-Security-OSS/mcp-contrast/issues)
3. Reference this document and the [Handover documentation](https://contrast.atlassian.net/wiki/spaces/CL/pages/4293132316/Handover) in Confluence
