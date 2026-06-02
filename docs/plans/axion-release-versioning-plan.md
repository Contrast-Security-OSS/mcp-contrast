# Axion Release Versioning Plan

## Context

The current `Gradle Release` workflow derives the release version from `gradle.properties`, commits the release version, tags it, commits the next `-SNAPSHOT` version, and pushes those commits back to `main`.

That model no longer works with the repository rules on `main`. GitHub rejects the direct push with:

```text
GH013: Repository rule violations found for refs/heads/main.
Changes must be made through a pull request.
```

The goal of this plan is narrow: change how release versions are handled so the workflow no longer commits version changes to a protected branch. Existing release security controls remain in scope and must be preserved:

* GitHub build provenance attestation for the release JAR.
* OpenPGP signing for `contrast-mcp-core` Maven publication.
* JFrog OIDC authentication for Artifactory publication.
* Docker image publishing and Docker Content Trust signing.
* Existing release artifact names and Docker tags.

## Accepted Direction

Use the idiomatic Axion release model: Git tags are the version source of truth.

The project version should no longer be owned by `gradle.properties`. Instead:

* `vX.Y.Z` tags define released versions.
* When `HEAD` is exactly on `vX.Y.Z`, Gradle version is `X.Y.Z`.
* When commits exist after the latest tag, Gradle version is the next Axion snapshot, for example `2.0.1-SNAPSHOT`.
* The workflow must never push release-version or next-snapshot commits to `main`.

This aligns with the broad model used by `aiml-services`: release versions are tag-derived, protected default branch changes flow through pull requests, and release artifacts are built from immutable release tags.

## Tag Format

Use the existing repository convention:

```text
vX.Y.Z
```

Examples:

```text
v2.0.0
v2.0.1
v2.1.0
```

There is one historical typo tag, `v.0.0.2`. Treat it as legacy noise. Future Axion configuration should use prefix `v` with no separator so tags are created as `v2.0.0`, not `v-2.0.0`.

## Operator Flow

The release workflow remains manually dispatched from `main`.

Normal patch release:

```text
Run workflow: Gradle Release
Branch: main
release_version: blank
```

Axion derives the next patch from the latest `vX.Y.Z` tag.

Major, minor, or migration release:

```text
Run workflow: Gradle Release
Branch: main
release_version: 2.0.0
```

The explicit version is passed to Axion as a forced release version.

The first Axion-managed release must explicitly use:

```text
release_version: 2.0.0
```

Reason: the latest existing normal tag is `v1.0.0`, while the repo has already been prepared for `2.0.0`. A blank Axion release before `v2.0.0` exists would likely create `v1.0.1`, which is wrong.

After `v2.0.0` exists, blank/default patch releases are acceptable.

## Workflow Shape

The release job should follow this sequence:

1. Check out `main` with full history and tags.
2. Fail unless the workflow was dispatched from `refs/heads/main`.
3. Validate the optional `release_version` input.
4. Validate Gradle wrapper and set up Java 21.
5. Verify the tracked worktree is clean before release tagging.
6. Run the existing release validation build.
7. Run Axion release:
   * Blank `release_version`: `./gradlew release --no-daemon`
   * Explicit version: `./gradlew release -Prelease.forceVersion=<version> --no-daemon`
8. Determine the release tag and release version.
9. Check out `vX.Y.Z`.
10. Assert `./gradlew -q printVersion` equals `X.Y.Z`.
11. Verify the tracked worktree is clean before publishing.
12. Build the release JAR from the tag.
13. Validate JFrog configuration variables.
14. Authenticate to JFrog with OIDC.
15. Publish `contrast-mcp-core` to public Artifactory.
16. Attest the release JAR.
17. Build, push, and sign Docker images exactly as today.
18. Create the GitHub Release and attach the JAR as the final public release marker.

The GitHub Release should be created after Maven, JAR attestation, and Docker publishing succeed. That prevents a visible GitHub Release from existing while other public release outputs are missing.

## GitHub and JFrog Authorization Model

The workflow authorization context remains `main`, even though the artifact source is the release tag.

Expected GitHub context for JFrog OIDC:

```text
repository: Contrast-Security-OSS/mcp-contrast
workflow: .github/workflows/gradle-release.yml
event: workflow_dispatch
ref: refs/heads/main
```

The workflow then checks out `vX.Y.Z` locally before publishing artifacts. This keeps both properties:

* Control plane: only the release workflow dispatched from `main` can publish.
* Artifact source plane: Maven, JAR, and Docker outputs are built from the immutable release tag.

The JFrog identity mapping should not require the OIDC `ref` claim to be `refs/tags/v*`, because the workflow is dispatched from `main`.

## JFrog Permission Expectations

The Platform/JFrog/Security setup should allow only the release publication path:

* Repository: `Contrast-Security-OSS/mcp-contrast`.
* Workflow: `.github/workflows/gradle-release.yml`.
* Event/ref: `workflow_dispatch` from `refs/heads/main`.
* Artifact: `com.contrast.labs.ai.mcp:contrast-mcp-core`.
* Repository/path: public Maven release repository/path only.

It should not grant:

* Snapshot publication.
* Overwrite/redeploy.
* Delete.
* Docker, Helm, or generic repository access.
* Broad Artifactory admin.
* Unrelated repository access.
* Long-lived JFrog credentials.

Non-secret configuration remains repository variables:

```text
CONTRAST_JFROG_URL
JFROG_OIDC_PROVIDER_NAME
CONTRAST_PUBLIC_MAVEN_RELEASE_URL
```

Signing secrets remain separate:

```text
CODE_SIGNING_PKEY
CODE_SIGNING_PASSPHRASE
```

## Artifactory Smoke Mode

Remove `artifactory-smoke` mode and permanent smoke artifact publication.

The smoke path was useful to prove JFrog OIDC and Gradle publication before merging the release workflow changes. That has already been tested on `main`, so the extra branch/policy exception is no longer needed.

Removing it simplifies the workflow and the JFrog policy:

* No synthetic non-release coordinates.
* No branch smoke exceptions.
* No `publish_smoke_artifact` input.
* No separate smoke job.

## Gradle Build Changes

Add the Axion plugin at the root build.

Representative Groovy configuration:

```groovy
plugins {
    id 'base'
    id 'pl.allegro.tech.build.axion-release' version '1.21.1'
    id 'com.diffplug.spotless' version "${spotlessGradlePluginVersion}" apply false
    id 'org.springframework.boot' version "${springBootVersion}" apply false
}

scmVersion {
    tag {
        prefix = 'v'
        versionSeparator = ''
    }
}

version = scmVersion.version

allprojects {
    group = 'com.contrast.labs.ai.mcp'
    version = rootProject.version
}
```

Keep `printVersion`, but make it print Axion's computed `project.version`.

Remove the custom `setVersion` task. It edits `gradle.properties`, which is the old version model.

Remove `version=...` from `gradle.properties` so there is not a competing version source.

## Release Version Input

Use one optional workflow input:

```yaml
release_version:
  description: "Optional explicit release version, e.g. 2.1.0. Leave blank for next patch."
  required: false
  default: ""
  type: string
```

Validation:

```bash
if [[ -n "$RELEASE_VERSION" && ! "$RELEASE_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "release_version must look like 2.1.0, without leading v"
  exit 1
fi
```

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

## Rerun and Failure Recovery

The workflow must not accidentally create a different release version when rerun after a tag has already been created.

Policy:

* If `release_version` is explicit and `vX.Y.Z` already exists at the current `main` commit, reuse it.
* If `release_version` is explicit and `vX.Y.Z` exists at a different commit, fail hard.
* If `release_version` is explicit and the tag does not exist, create it with Axion.
* If `release_version` is blank, Axion creates the next patch on the first run. If a failure occurs after tag creation, rerun handling must be tested so it reuses the same tag or the operator should rerun with the explicit version.

Implementation should test Axion's exact behavior when `HEAD` is already tagged before relying on blank-version reruns.

The safe fallback is to require an explicit `release_version` when recovering a failed run after tag creation.

## Consistency Gates

Add a tag/version consistency gate after checking out the release tag:

```bash
ACTUAL="$(./gradlew -q printVersion --no-daemon)"
EXPECTED="${TAG_NAME#v}"

if [ "$ACTUAL" != "$EXPECTED" ]; then
  echo "Gradle version $ACTUAL does not match tag $TAG_NAME"
  exit 1
fi
```

This protects:

* JAR filename.
* Maven coordinate.
* Docker version tag.
* GitHub Release name.
* Attestation subject path.

Add tracked-worktree cleanliness checks before tagging and before publishing:

```bash
git diff --quiet
git diff --cached --quiet
```

Do not fail on untracked build output under `build/`.

## Publishing Constraints

The Gradle-side Artifactory guard currently requires:

* Non-SNAPSHOT version.
* `contrastPublicMavenReleaseUrl`.
* OpenPGP signing key.

It does not require publishing from `main`.

Publishing should continue to happen after checking out the release tag. The workflow dispatch/ref guard on `main` is the authorization control. The checked-out tag is the source artifact control.

## Out of Scope

Do not change:

* Docker Content Trust to Cosign.
* Docker registry or image naming.
* GitHub build provenance attestation behavior.
* OpenPGP signing behavior.
* JFrog OIDC setup action and short-lived credential flow.
* Maven publication metadata checks, except as needed to preserve compatibility with Axion-derived versions.
* Public user docs beyond release-process corrections.

The existing Docker signing/multi-platform migration remains separate work.

## Documentation Updates

Update `RELEASING.md` to describe the new flow:

* Release versions come from `vX.Y.Z` tags via Axion.
* Run `Gradle Release` from `main`.
* Leave `release_version` blank for normal patch releases.
* Enter `2.0.0` for the first Axion-managed release.
* Enter explicit versions for intentional major/minor releases.
* The workflow creates the tag, builds from the tag, publishes Maven/JAR/Docker outputs, then creates the GitHub Release.
* There are no release-version or next-snapshot commits to `main`.

Remove old guidance that says:

* Update `gradle.properties` to control the release.
* The workflow commits release and next development versions.
* `main` should contain the next `-SNAPSHOT` version after release.

## Verification Plan

Local/CI verification should cover:

* `./gradlew -q printVersion` returns an Axion-derived snapshot on an untagged commit.
* A temporary local tag such as `v9.9.9-test` or an isolated test repo proves tag-derived release behavior without polluting real tags.
* `./gradlew spotlessCheck checkstyleMain checkstyleTest test` passes.
* `./gradlew :contrast-mcp-stdio-app:bootJar` produces a JAR with the Axion-derived version.
* `./gradlew :contrast-mcp-core:verifyCorePublicationMetadata` passes.
* Workflow syntax is valid.
* Workflow release logic handles:
  * blank version,
  * explicit `2.0.0`,
  * invalid version input,
  * existing same-commit tag,
  * existing different-commit tag.

Remote verification for the first real release:

* Dispatch from `main` with `release_version=2.0.0`.
* Confirm `v2.0.0` points at the intended commit.
* Confirm JAR artifact name is `mcp-contrast-2.0.0.jar`.
* Confirm Maven coordinate is `com.contrast.labs.ai.mcp:contrast-mcp-core:2.0.0`.
* Confirm Docker tags `2.0.0` and `latest` are published and signed as before.
* Confirm GitHub Release is created only after the other outputs succeed.
* Confirm attestation verification succeeds for the release JAR.

## Summary

This migration changes the release version source from a mutable committed file to immutable Git tags. It keeps the release workflow controlled by `main`, publishes from the created `vX.Y.Z` tag, and removes direct pushes to the protected branch. It preserves all existing artifact signing, attestation, JFrog OIDC, Maven publication, and Docker signing behavior.
