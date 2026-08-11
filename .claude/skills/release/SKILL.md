---
name: release
description: Orchestrate a full release of mcp-contrast end to end. Preflight checks, release ticket and branch creation, changelog update and version stamp, smoke test, dispatch and monitoring of the Gradle Release workflow, post-release artifact verification, and the Jira/bead sweep. Run when a human asks to cut, make, or ship a release. Human gates at version choice, changelog approval, and dispatch. Optional argument is an explicit version, e.g. /release 2.5.0.
---

# Release orchestration

Runs the whole release documented in RELEASING.md, composing `/update-changelog` and
`/test-mcp-server`. The skill owns the sequence, the preflight judgment, the version
stamp, workflow dispatch and monitoring, post-release verification, and the tracker
sweep. Publishing to Artifactory and DockerHub is irreversible, so three human gates
are mandatory: version choice, changelog approval, and go/no-go before dispatch.

## Usage

- `/release` runs the full flow, recommending the next version.
- `/release X.Y.Z` uses the given version (no leading `v`).

## Re-entry

Releases fail mid-flight more than anywhere else. On re-entry, detect completed work
and skip it: if the stamped changelog is already on `origin/main`, go straight to
Phase 4 or 5; if the release workflow already ran and only verification is missing,
go to Phase 6. The workflow itself is safe to re-dispatch because it reuses an
existing `vX.Y.Z` tag at HEAD.

## Phase 1 — Preflight (read-only)

1. `git fetch origin --tags -q`. Confirm the working tree is clean and HEAD matches
   `origin/main`. If not, stop and tell the human.
2. Confirm CI is green: `gh run list --branch main --workflow build.yml --limit 1
   --json conclusion,headSha` and check the run covers the current `origin/main` HEAD.
3. Find the last release tag: `git tag --sort=-v:refname | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | head -1`.
4. Enumerate the release content: `git log --oneline --merges vLAST..origin/main`,
   plus the same without `--merges` for direct commits. If the range is empty, there
   is nothing to release, stop.
5. Recommend the next version from the change set: breaking changes → major, new
   capabilities → minor, fixes only → patch. Confirm the version with the human
   (explicit argument wins, but still show what the range contains). If later
   changelog work changes the classification, re-confirm before stamping.

## Phase 2 — Release ticket and branch

1. Create the release Jira ticket via the `jira-workflow` skill: AIML Task,
   component `Contrast MCP Server`, summary `Release mcp-contrast X.Y.Z`, assigned
   to the current user, transitioned to In Progress. Create the matching bead per
   `bead-workflow` (title prefix, external-ref, Jira URL comment).
2. Create the release branch from `origin/main`: `AIML-<id>-release-X.Y.Z`. The
   changelog and stamp commits land here.

## Phase 3 — Changelog and stamp

1. From the release branch, run `/update-changelog`. Choose current-branch mode so
   its commit lands on the release branch. It has its own approval gate.
2. After approval, stamp the version: insert `## [X.Y.Z] - <today>` directly below
   `## [Unreleased]`, moving the unreleased content under the new heading and leaving
   `[Unreleased]` empty. Commit separately: `AIML-<id> Stamp vX.Y.Z release heading`
   (matches the established two-commits-one-branch pattern, e.g. v2.4.0).
3. Push and open the PR with `pr-tools:create-pr`, then wait for the human to merge.
4. Verify the stamp reached `origin/main` before continuing.

## Phase 4 — Smoke test

Run `/test-mcp-server smoke` (always pass the mode explicitly, the skill asks
otherwise). Show the human the report. It is a judgment aid, not a gate, but the
human decides whether to continue.

## Phase 5 — Go/no-go and dispatch

1. Guard: re-fetch and confirm `origin/main` HEAD is still the commit the stamped
   changelog covers. If another PR merged in between, the tag would ship
   undocumented changes, loop back to Phase 3.
2. Ask the human for an explicit go/no-go.
3. Dispatch with the version spelled out, never blank, so the tag cannot diverge
   from the stamped heading:

   ```bash
   gh workflow run gradle-release.yml --ref main -f release_version=X.Y.Z
   ```

4. `gh workflow run` returns no run id. Fetch it with `gh run list
   --workflow=gradle-release.yml --limit 1 --json databaseId,createdAt,status` and
   check `createdAt` is after the dispatch (guards against grabbing a stale run).
5. Monitor with `gh run watch <id> --exit-status` as a background task
   (`run_in_background`), the run exceeds the foreground ceiling. Do not poll.
6. On failure, `gh run view <id> --log-failed`, diagnose, and report. Transient
   failures are fixed by re-dispatching (see Re-entry).

## Phase 6 — Post-release verification

1. Release assets: `gh release view vX.Y.Z --json assets` must show five files, the
   jar plus four SBOMs (jar and Docker image, CycloneDX and SPDX each).
2. Attestation: `gh release download vX.Y.Z --pattern '*.jar'` to a temp dir, then
   `gh attestation verify mcp-contrast-X.Y.Z.jar --repo Contrast-Security-OSS/mcp-contrast`.
3. DockerHub: `curl -sf https://hub.docker.com/v2/repositories/contrast/mcp-contrast/tags/X.Y.Z`.
4. Artifactory: `curl -sfI https://contrastsecurity.jfrog.io/artifactory/contrast-mcp-release/com/contrast/labs/ai/mcp/contrast-mcp-core/X.Y.Z/contrast-mcp-core-X.Y.Z.pom`.
   If anonymous read is refused, note it and rely on the workflow's publish step
   having succeeded.

## Phase 7 — Tracker sweep

1. Derive shipped AIML ids from the merge commits in `vLAST..vX.Y.Z` (branch names
   in merge subjects). Skip dependabot PRs.
2. Via the `jira-workflow` skill, transition shipped tickets from Ready to Deploy to
   Closed (`81`), the release ticket included. Closed is reserved for code released
   to production, which is now true.
3. Close the matching beads per `bead-workflow`, asking the human first, then
   `br sync --flush-only`.

## Phase 8 — Report

Close with the version, tag, GitHub Release URL, each verification result, and the
tickets and beads transitioned.

## Notes

- Never commit to `main` directly, everything lands via the release-branch PR.
- `make verify` is not repeated here, the workflow runs the full validation build
  including integration tests before tagging.
- This skill does not modify the Gradle Release workflow or RELEASING.md steps, it
  automates around them. If the workflow changes, update this skill to match.
