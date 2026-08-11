---
name: release
description: Orchestrate a full release of mcp-contrast end to end, changelog-first. Preflight, deferred-landing changelog draft, version gate, release ticket and branch, smoke test, Gradle Release dispatch and monitoring, artifact verification, release-notes sync, tracker sweep, and Slack announcement. Run when a human asks to cut, make, or ship a release. Human gates at changelog approval, version choice, changelog PR merge, go/no-go, tracker sweep, and the Slack post.
---

# Release orchestration

Runs the whole release documented in RELEASING.md, composing `/update-changelog` and
`/test-mcp-server`. The flow is changelog-first: the audited changelog draft is the
evidence for the version decision, and the release ticket and branch are created only
after the human approves the version, so they are born with the right number.
Publishing to Artifactory and DockerHub is irreversible, so the human gates below are
mandatory, never skipped.

## Usage

`/release` takes no arguments. The version always comes from the suggest-and-approve
gate in Phase 3.

## Re-entry

Releases fail mid-flight more than anywhere else. On re-entry, detect completed work
from the repo and skip it: stamped changelog already on `origin/main` → go to Phase 5
or 6; release workflow already succeeded → go to Phase 7. The workflow itself is safe
to re-dispatch because it reuses an existing `vX.Y.Z` tag at HEAD.

## Phase 1 — Preflight (read-only)

1. `git fetch origin --tags -q`. Confirm the working tree is clean and HEAD matches
   `origin/main`. If not, stop and tell the human.
2. Confirm CI is green: `gh run list --branch main --workflow build.yml --limit 1
   --json conclusion,headSha` and check the run covers the current `origin/main`
   HEAD. Red or stale CI stops the release by default. The human may explicitly
   override for a known-flaky failure, record the override in the final report.
3. Find the last release tag: `git tag --sort=-v:refname | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | head -1`.
4. Enumerate the release content: `git log --oneline --merges vLAST..origin/main`,
   plus the same without `--merges` for direct commits. If the range is empty, there
   is nothing to release, stop.
5. If every change in the range is internal (CI config, dependabot, refactors), ask
   the human whether to ship a dependency-bump release anyway. If yes, the Phase 2
   draft must include a section summarizing the dependency updates (drawn from the
   dependabot PR titles, house style) so the stamped section is never empty.

## Phase 2 — Changelog draft (deferred landing)

Run `/update-changelog` in its **deferred-landing mode**: it drafts, audits, and gets
the human's approval of the `[Unreleased]` content, but commits nothing, the landing
branch does not exist yet. It returns the approved draft and its suggested next
version, computed under the semver definition that lives in that skill.

## Phase 3 — Version gate

Present the version decision with AskUserQuestion: three options, each showing the
computed number and a one-line rationale citing the draft evidence, e.g.
`Minor → 2.5.0 (Recommended)`, `Patch → 2.4.1`, `Major → 3.0.0`. The changelog
skill's suggestion is the recommended option. The human's choice is final for
naming, only the Phase 6 loop-back can reopen it.

## Phase 4 — Release ticket, branch, and landing

1. Create the release Jira ticket via the `jira-workflow` skill: AIML Task,
   component `Contrast MCP Server`, summary `Release mcp-contrast X.Y.Z`, assigned
   to the current user, transitioned to In Progress. Create the matching bead per
   `bead-workflow` (title prefix, external-ref, Jira URL comment).
2. Create the release branch from `origin/main`: `AIML-<id>-release-X.Y.Z`.
3. Land two commits: the approved changelog draft
   (`AIML-<id> Update changelog for X.Y.Z release`), then the stamp, insert
   `## [X.Y.Z] - <today>` directly below `## [Unreleased]`, moving the unreleased
   content under the new heading and leaving `[Unreleased]` empty
   (`AIML-<id> Stamp vX.Y.Z release heading`). Two commits, matching the
   established pattern (e.g. v2.4.0).
4. Push, open the PR with `pr-tools:create-pr`, and wait for the human to merge.
   The PR is the human's second look at the changelog.
5. Verify the stamp reached `origin/main` before continuing.

## Phase 5 — Smoke test

Sync the local checkout to the merged `origin/main` first, the test builds the jar
from the current checkout and it must exercise the exact content that gets tagged.
Then run `/test-mcp-server smoke` (always pass the mode explicitly, the skill asks
otherwise). Show the human the report. It is purely advisory, the smoke test drives
a live org and flaky data is expected, so no failure class blocks by itself. The
human decides at the next gate.

## Phase 6 — Go/no-go and dispatch

1. Guard: re-fetch and confirm `origin/main` HEAD is still the commit the stamped
   changelog covers. If another PR merged in between, the tag would ship
   undocumented changes. On this loop-back, this skill owns folding the new entries
   into the stamped `[X.Y.Z]` section (`/update-changelog` treats stamped versions
   as settled and will not reopen them). If the new entries escalate the version
   class, re-confirm the version with the human, retitle the Jira ticket, restamp,
   and keep the branch name, it is scaffolding with a merged PR hanging off it.
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
6. On failure, `gh run view <id> --log-failed`, diagnose, report, and wait for the
   human. Never re-dispatch on your own, the workflow publishes to three external
   registries and a partial-publish state needs a human to see which steps
   completed first.

## Phase 7 — Post-release verification

1. Release assets: `gh release view vX.Y.Z --json assets` must show five files, the
   jar plus four SBOMs (jar and Docker image, CycloneDX and SPDX each).
2. Attestation: `gh release download vX.Y.Z --pattern '*.jar'` to a temp dir, then
   `gh attestation verify mcp-contrast-X.Y.Z.jar --repo Contrast-Security-OSS/mcp-contrast`.
3. DockerHub: `curl -sf https://hub.docker.com/v2/repositories/contrast/mcp-contrast/tags/X.Y.Z`.
4. Artifactory: `curl -sfI https://contrastsecurity.jfrog.io/artifactory/contrast-mcp-release/com/contrast/labs/ai/mcp/contrast-mcp-core/X.Y.Z/contrast-mcp-core-X.Y.Z.pom`.
   If anonymous read is refused, note it and rely on the workflow's publish step
   having succeeded.

For any failed check, report it together with a suggested remediation path (which
workflow step to inspect, whether re-dispatch is safe). Do not attempt remediation
yourself, a half-published release is exactly where automation causes damage.

## Phase 8 — Release notes

Replace the workflow's auto-generated GitHub release notes with the stamped
changelog section, one source of truth for what users read:

```bash
gh release edit vX.Y.Z --notes-file <file containing the [X.Y.Z] section>
```

## Phase 9 — Tracker sweep

1. Derive shipped AIML ids from the merge commits in `vLAST..vX.Y.Z` (branch names
   in merge subjects). Skip dependabot PRs.
2. Show the human the full list with each ticket's current status and let them pick
   the set to transition. Transition the chosen tickets to Closed (`81`) via the
   `jira-workflow` skill, the release ticket included. Closed is reserved for code
   released to production, which is now true.
3. Close the matching beads per `bead-workflow` after one batch confirmation showing
   the full list, then `br sync --flush-only`.

## Phase 10 — Announce

Only after Phase 7 passed, never for a failed or abandoned release: draft a Slack
post for `#_aiml-team` from the stamped changelog section plus the GitHub Release
link (which links onward to everything else). Show the human the draft and send it
only on their approval.

## Report

Close with the version, tag, GitHub Release URL, each verification result, any CI
override recorded in Phase 1, the tickets and beads transitioned, and the Slack
post status.

## Notes

- Never commit to `main` directly, everything lands via the release-branch PR.
- `make verify` is not repeated here, the workflow runs the full validation build
  including integration tests before tagging.
- This skill does not modify the Gradle Release workflow or RELEASING.md steps, it
  automates around them. If the workflow changes, update this skill to match.
