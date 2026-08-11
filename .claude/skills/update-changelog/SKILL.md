---
name: update-changelog
description: Bring the [Unreleased] section of CHANGELOG.md up to date before a release. Diffs the last release tag against origin/main, drafts entries PR-by-PR, audits the draft for completeness with a read-only subagent, and lands the result on a branch after the user approves. Run pre-release, before dispatching the Gradle Release workflow, or whenever a human asks for a changelog update. Optional free text is treated as guidance, e.g. classification hints.
---

# Pre-release changelog update

Brings `[Unreleased]` in CHANGELOG.md up to date with every user-facing change merged
since the last release tag, so the release ships with a finished changelog. The skill
drafts, audits, and only then shows the human one final draft. Nothing is written to
disk before the human approves.

**When to run.** Before triggering the Gradle Release workflow (see RELEASING.md), or
when a human explicitly asks. Not part of routine feature development.

## Usage

- `/update-changelog` runs the full flow.
- `/update-changelog <guidance>` passes free-text hints, e.g.
  `/update-changelog the SBOM work is internal, skip it`.
- **Deferred-landing mode**, invoked by the `/release` skill: draft, audit, and get
  the human's approval as usual, but skip the branch question in Step 1 and all of
  Step 6. The range is the base range only. Return the approved draft text and the
  suggested next version to the caller, which creates the release branch and lands
  the changelog itself.

## Ground rules

- Content goes into `[Unreleased]`. Never stamp a version heading for the upcoming
  release. Version selection belongs to the release workflow, not this skill.
- Versions that already have a changelog section are settled. Never reopen, rewrite,
  or re-audit them.
- Existing `[Unreleased]` entries may be amended or expanded when they are incomplete
  or wrong. Never delete one silently. Propose deletions in the draft and let the
  human decide.
- Report a suggested next version (breaking changes → major, new capabilities → minor,
  otherwise patch). It is informational only. Do not act on it. **Breaking** here
  means removed or renamed tools, or changes to hosting requirements, a new Java
  version, a Spring Boot major, changed Docker or jar run instructions. Changes to
  the `contrast-mcp-core` library API are not breaking on their own, its only
  consumer is internal. Small tool contract changes (parameters, response shape)
  stay minor, AI consumers adapt.
- Do not create beads. This is a one-file docs change.

## Step 1 — Orient and choose the branch

1. `git fetch origin --tags -q`
2. Find the newest release tag, `git tag --sort=-v:refname | grep -E '^v[0-9]+\.[0-9]+\.[0-9]+$' | head -1`
3. Find the newest version heading in CHANGELOG.md (`## [X.Y.Z]`).
4. **Backfill check.** If release tags exist that are newer than the newest heading,
   those releases shipped without changelog notes. Offer to backfill exactly those
   versions in this run. A backfilled section is stamped `[X.Y.Z] - <tag date>`
   (`git log -1 --format=%cs vX.Y.Z`). Do not scan further back than the newest
   heading. Versions at or below it are never touched.
5. **Ask the human where the change lands.** Use the current Jira branch, or create a
   new branch. A new branch needs a Jira ticket, either an existing one or a new AIML
   Task with component `Contrast MCP Server`, and follows the repo convention
   `AIML-<ticket>-<short-description>`. This choice also determines the diff range in
   Step 2, so ask it now.

## Step 2 — Determine the range

- Base range is `vLAST..origin/main`, where `vLAST` is the newest release tag.
- **Current-branch mode.** Also include the branch's own unmerged work
  (`git log --oneline origin/main..HEAD` and `git diff origin/main...HEAD --stat`).
  Choosing this mode signals that the branch ships in the release, so its changes
  are documented too.
- **Backfill ranges.** One range per backfilled version, `vN-1..vN`.

## Step 3 — Enumerate and draft

For each range:

1. List merged PRs, `git log --oneline --merges <range>`. Also run without `--merges`
   to catch direct commits.
2. For each PR, `gh pr view <N> --json title,body,labels`. Identify the Jira ticket
   and summarize what changed. Read the diff when the PR body is thin or ambiguous.
3. Classify every change:
   - **USER-FACING**: new tool, tool behavior or contract change, bug fix a user would
     notice, breaking change, security-relevant dependency change, install or docs a
     user relies on.
   - **INTERNAL**: test-only fixes, CI/build config, dependabot internal tooling,
     release plumbing, refactors with no observable effect.
4. Draft an entry for each user-facing change in the house style below. Amend an
   existing `[Unreleased]` entry instead of duplicating it when it already covers the
   same change but is incomplete.

**House style** (match the existing file exactly):

- Section order within a version: Breaking Changes, Bug Fixes, Improvements, Security,
  Documentation. Include only non-empty sections.
- Hosting-requirement changes (Java version, Spring Boot major, Docker or jar run
  instructions) belong under Breaking Changes, that section drives the version bump.
- Each entry is a short prose paragraph led by a **bold summary phrase**, wrapped at
  roughly 100 characters.
- No PR numbers or ticket IDs in the published text. Cite external-system defects
  (e.g. `TS-42992`) only when the entry needs them to make sense.
- Write for the user of the MCP server, not the developer. Name the tool and the
  observable behavior.

## Step 4 — Audit

Launch one **read-only** subagent per range, in parallel when there are several, using
the prompt template at the bottom of this file. Reproduce the full draft section text
inline in the prompt. In current-branch mode, list the branch's own unmerged changes
in the prompt as additional items to classify.

Then verify before applying. For every MISSING or PARTIAL finding, check the claim
against the actual PR diff (`gh pr diff <N>` or `git show`) before accepting it.
Auditors can misattribute details, especially between merged work and similar unmerged
work on other branches. Discard findings the diff does not support and say so.

## Step 5 — Present the draft

Show the human, in one message, before writing anything:

- The complete proposed `[Unreleased]` section (and any backfilled sections).
- What changed versus the current file. New entries, amended entries with the reason,
  and any proposed deletions flagged for a decision.
- The audit verdict per range, including anything found and rejected in verification.
- The suggested next version, marked informational.

Wait for approval. Apply requested edits and re-present if the human asks for changes.

## Step 6 — Land

**Current-branch mode.** Edit CHANGELOG.md in place, commit on the current branch with
the branch's Jira ID prefix, e.g. `AIML-1285 Update changelog for upcoming release`.
Do not push or open a PR. The change rides the branch's normal flow.

**New-branch mode.** Keep the working tree untouched by using a throwaway worktree:

```bash
git worktree add -b AIML-<ticket>-update-changelog /tmp/changelog-wt origin/main
# edit CHANGELOG.md in the worktree, then
git -C /tmp/changelog-wt add CHANGELOG.md
git -C /tmp/changelog-wt commit -m "AIML-<ticket> Update changelog for upcoming release"
git -C /tmp/changelog-wt push -u origin AIML-<ticket>-update-changelog
git worktree remove /tmp/changelog-wt
```

Then run `/pr-tools:create-pr` for the new branch. PR title follows the repo
convention `<Jira Id> <Title>`.

## Step 7 — Report

Close with the suggested next version, a one-paragraph audit summary, what was added
or amended, any deletions or backfills performed, and the PR URL or commit hash.

## Audit subagent prompt template

Fill in `<RANGE>`, `<REPO_PATH>`, and `<DRAFT>`. Keep the READ-ONLY framing verbatim.

```text
You are auditing a draft CHANGELOG section for completeness. READ-ONLY task: do not
modify any files, do not commit, do not checkout branches, do not touch the working
tree. Only use read-only git commands (git log, git show, git diff) and
`gh pr view` / `gh pr list` / `gh pr diff`.

Repo: <REPO_PATH> (an MCP server for Contrast Security)

Verify that the draft below documents every USER-FACING change in the range <RANGE>,
and that nothing user-facing was missed.

STEP 1 — Enumerate everything in the range:
- Run `git log --oneline --merges <RANGE>` to list merged PRs.
- Also run without --merges to see direct commits.
- For each merged PR, get its title/body with `gh pr view <N> --json title,body,labels`.
  Identify the Jira ticket (AIML-xxx / ENTSEC-x etc.) and summarize what changed.

STEP 2 — Classify each PR/change as one of:
  (a) USER-FACING (new tool, tool behavior/contract change, bug fix a user would
      notice, breaking change, security-relevant dependency change, install/docs a
      user relies on)
  (b) INTERNAL (test-only fixes, CI/build config, dependabot internal tooling,
      release plumbing, refactors with no observable effect)

STEP 3 — Cross-check against the draft reproduced below. For each USER-FACING change,
decide: COVERED / MISSING / PARTIALLY-COVERED.

--- DRAFT ---
<DRAFT>
--- END DRAFT ---

RETURN a concise report with:
1. A table: PR# | ticket | one-line summary | classification (user-facing/internal) |
   changelog status (covered/missing/partial)
2. A clear list of any USER-FACING changes that are MISSING or PARTIAL, with a
   suggested one-line changelog entry and which section (Breaking Changes/Bug
   Fixes/Improvements/Security/Documentation) it belongs in.
3. A final verdict: is the draft complete, or does it need additions?

Be precise and evidence-based. Quote PR titles. Do not speculate. If unsure whether
something is user-facing, say so and explain.
```

## Notes

- If CHANGELOG.md has no `[Unreleased]` heading, create one directly under the intro.
- The audit checks a closed set, so run it only after the draft is complete for the
  range. Re-running after human-requested edits is cheap and worth it when the edits
  change coverage rather than wording.
- This skill does not touch GitHub release notes. The release workflow generates
  those, and any sync from the changelog is separate work.
