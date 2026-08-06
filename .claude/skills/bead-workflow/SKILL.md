---
name: bead-workflow
description: Manage beads through their full lifecycle in this repository. Use when creating, updating, claiming, implementing, closing, triaging, or commenting on a bead. Covers labeling, pre-claim checks, branch requirements, stacked-branch handling, commit-before-close discipline, Jira integration, and sync. Triggers on any bead mutation, not reads.
---

# Bead Workflow

Beads (`br`) is the working issue store. This skill governs every bead mutation, create, claim, implement, close, triage, and comment. For read-only commands (`br show`, `br ready`, `br list`) this skill does not need to be loaded.

For Jira ticket creation, bead-to-Jira parity rules, and post-merge transitions, invoke the `jira-workflow` skill. This skill does not duplicate those rules.

**Public repo note.** This repository is public. `.beads/` is gitignored and must stay that way, bead content is internal. Never commit anything under `.beads/`, and never quote internal-only detail from beads into committed files.

## 1. Creating a bead

```bash
br create "Title" --type <type> --priority <0-4> --description "..." \
  --labels "repo:mcp-contrast" [--parent <parent-id>]
```

**Required fields:**
- `--type`: task, bug, feature, epic, chore, docs, question. Use your judgment based on context. Epics are typically parent beads with children, matching how Jira epics work.
- `--priority`: Default to P2 (medium) unless context clearly indicates otherwise. P0/P1 for blocking issues, P3/P4 for nice-to-haves and backlog.

**Repo label (`repo:<name>`):**
Apply a `repo:` label for the repo where the code work will happen (normally `repo:mcp-contrast`). Work that spans repos can carry multiple `repo:` labels. If the target repo changes later, update the label. Figure the repo out from context, do not ask the user.

**Multi-line content:** quoted heredocs prevent shell interpretation.

```bash
br create "Title" --description "$(cat << 'EOF'
Markdown with `code`, --flags, and $(vars), all literal.
EOF
)"
```

For the design field, always use `scripts/br-set-design <bead-id> <file-path>` (the CLI misparses `--` in markdown).

**Parent-child relationships:**
Use `--parent <parent-id>` when creating a child bead. Child beads work on the same branch as their parent. If the parent has a Jira ticket and the user wants one for the child, follow the `jira-workflow` skill (Story under Epic when the parent ticket is an Epic, otherwise ask). Only create Jira tickets when the user explicitly requests it.

**After creation:**
Run `br sync --flush-only` to export changes locally.

## 2. Pre-claim checklist

Before claiming a bead for implementation, run `br show <id>` and verify:

1. **No unresolved blocking dependencies.** If the bead is blocked, surface the blockers to the user. Exception, a `blocks` edge on a stacked-branch bead pointing at its base bead blocks the *merge*, not the work, see section 3.
2. **No blocking labels.** Check for `needs-decision`, `needs-triage`, `needs-human-review`, `needs-info`, `human-security-review`, `external-approval`. If present, tell the user. `human-reviewed` is the cleared marker.
3. **No existing assignee.** If someone else is assigned, ask the user before taking it over.
4. **Branch requirement.** If the bead has an `external-ref` pointing to a Jira ticket, work happens on a branch named `{JIRA-KEY}-{kebab-case-description}` (e.g. `AIML-1304-improve-ai-skills`). Check the current branch:
   - Already on the correct branch, proceed.
   - On another feature branch, offer to use `pr-tools:create-stacked-branch` to create the `{JIRA-KEY}-...` branch stacked on the current one.
   - On `main`, create the `{JIRA-KEY}-...` branch, or ask the user which branch to base it on if stacking is plausible.
   - No Jira ticket, no branch naming requirement, but never commit directly to `main`.

## 3. Stacked branches

When the new branch is based on another PR branch rather than `main`:

- Label the bead `stacked-branch`.
- Record the merge ordering with `br dep add <new-bead> <base-bead> --type blocks`. This means the base PR must merge first, work on the stacked bead proceeds anyway.
- `br` refuses `--claim` and status changes on a blocked bead. Set `--status in_progress --assignee <you>` *before* adding the blocks edge, or temporarily `br dep remove`, update, and re-add.
- PRs for stacked beads are created as drafts targeting the parent branch (`pr-tools:create-pr` detects this). Apply `pr-created` but not `in-review` until the PR is promoted with `pr-tools:promote-stacked-pr`.

## 4. Implementing a bead

**Claim the bead:**
```bash
br update <id> --claim
```
This atomically sets the assignee and status to `in_progress`. Record the branch name in the bead so it is easy to find later.

**Transition the Jira ticket.** If the bead has an `external-ref` pointing to a Jira ticket, transition it to In Progress (transition ID `21`) and assign it to the current user, per the `jira-workflow` skill.

**Read the full context.** Run `br show <id>` for the bead. If it has a Jira `external-ref`, also fetch the ticket body and comments with `getJiraIssue` so both descriptions and any ticket discussion inform the work.

**Do the work.** Write tests for all code changes. `make check-test` must pass, and `make verify` (integration tests) must pass before review, see CLAUDE.md Testing Requirements.

**Commit and push before closing.** A bead that produced code changes must not be closed until those changes are committed and pushed. This is non-negotiable.

**Open the PR.** After pushing, create the PR with `pr-tools:create-pr`. Non-stacked beads get a ready PR targeting `main`, apply `pr-created` and `in-review` to the bead and transition Jira to In Review (`41`). Stacked beads get a draft PR, apply `pr-created` only and keep Jira at In Progress.

## 5. Closing a bead

**Always ask the user before closing a bead.** Parent beads cannot close while children are open, and typically stay `in_progress` (with `in-review`) until the PR merges.

Closing follows a strict sequence:

1. **Commit and push** all related changes (if any code was written).
2. **Add a summary comment** to the bead:
   ```bash
   br comments add <id> --message "Summary of changes..."
   ```
   Include a short paragraph on what changed and why, which files were modified, and the commit hash(es). If no code changed (resolved by discussion, decided not to proceed), say that instead.
3. **Close the bead:**
   ```bash
   br close <id> --reason "Brief closure reason"
   ```
   The `--reason`/`-r` flag is required, a positional argument fails.
4. **Transition the Jira ticket.** After the PR merges to `main`, transition the ticket to Ready to Deploy (`51`), per the `jira-workflow` skill. Closed (`81`) is reserved for when the code actually ships.
5. **Sync:**
   ```bash
   br sync --flush-only
   ```

After a merge, run `pr-tools:after-pr-merged` to find and optionally promote dependent stacked PRs.

## 6. Triage

When triaging a bead (updating status, labels, priority, or assignments during review):

- Set the `repo:` label if not already set or if it was wrong.
- Apply triage labels as appropriate, `needs-decision`, `needs-info`, `ready-for-agent`, `ready-for-human`. See `docs/agents/triage-labels.md` for the vocabulary.
- Update priority and type if the initial values were off.
- Run `br sync --flush-only` after mutations.

## 7. Jira integration

This skill does not own Jira ticket creation. When Jira work is needed, invoke the `jira-workflow` skill, which handles ticket creation and all bead-to-Jira parity rules (title prefixing, external-ref, relationship mirroring). When a Jira ticket is created for a bead, the bead's title gets the `AIML-<id>: ` prefix and its `external-ref` gets the ticket key, no exceptions.
