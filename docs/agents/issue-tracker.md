# Issue tracker (beads + Jira)

Issues for this repo run on a dual model. **Beads (`br`) is the working issue store** and **Jira (the AIML project, component `Contrast MCP Server`) is the external reference**. The `gh` CLI is used for pull requests, not for issues. GitHub Issues is not the tracker for this repo.

When a skill says "the issue tracker", it means beads first, with Jira as the linked external ticket for anything that needs one. Most code changes need a Jira ticket and a feature branch (`AIML-<id>-<desc>`) before merging.

The **`bead-workflow`** skill governs every bead mutation and the **`jira-workflow`** skill governs ticket creation, bead-to-Jira parity, and status transitions. This doc is the quick reference.

## Beads is the primary store

The database lives in this repo's `.beads/`, which is **gitignored and local-only**. This is a public repository, never commit anything under `.beads/`.

- **Create**: `br create "Title" --description "..." --type task --priority 2 --labels "repo:mcp-contrast"`. For multi-line content use a quoted heredoc through `$()` to keep markdown, backticks, and `$(vars)` literal. For the design field use `scripts/br-set-design <bead-id> <file>`.
- **Start work**: `br update <bead-id> --claim` sets assignee and status atomically. Falls back to `--status in_progress --assignee <you>` when a stacked-branch blocks edge makes the bead "blocked" (see the `bead-workflow` skill).
- **Find work**: `br ready` lists unblocked open beads. `br list` and `br show <bead-id>` for detail. `br search "keyword"` for full text.
- **Comment**: `br comments add <bead-id> --message "..."` for one line, or `br comments add <bead-id> -f /tmp/comment.txt` for multi-line. Note `br comment` (singular) does not exist.
- **Label**: `br label add <bead-id> -l <label>` / `br label remove <bead-id> -l <label>`. See `triage-labels.md` for the vocabulary.
- **Dependencies**: `br dep add <dependent> <prerequisite>` for a blocks edge, `br dep add <child> <parent> --type parent-child` for hierarchy.
- **Close**: `br close <bead-id> --reason "why it's done"`. The `--reason`/`-r` flag is required. Ask the user before closing a bead, and close child beads before their parent.
- **Sync**: `br sync --flush-only` exports the DB to the local JSONL after mutations and before ending a session.

## Jira is the external reference

Significant beads get a Jira ticket in the **AIML** project (cloudId `https://contrast.atlassian.net`), issue types `Story` (features), `Task` (refactors/docs/bugfixes), `Epic` (large multi-ticket efforts). Use the Atlassian MCP tools to read and write Jira. Full metadata lives in `.claude/skills/jira-workflow/references/aiml.md`.

When a Jira ticket is created for a bead, do two things.

1. Set the bead's `external-ref` to the Jira ticket ID.
2. Prefix the bead's `title` with the Jira ticket ID (for example `AIML-1304: ...`).

AIML transition IDs for `transitionJiraIssue`: `11` To Do, `21` In Progress, `41` In Review, `51` Ready to Deploy, `61` Blocked, `71` Backlog, `81` Closed. The `jira-workflow` skill maps repo events (PR opened, stacked PR promoted, PR merged) onto these transitions.

## When a skill says "publish to the issue tracker"

Create a bead with `br create`. If the work needs an external ticket, also create the Jira issue per the `jira-workflow` skill and link it back via `external-ref` and the title prefix. For a PRD, prefer the project PRD location (`plans/<ticket>/`) over an issue body.

## When a skill says "fetch the relevant ticket"

Run `br show <bead-id>` (and `br comments` for the thread). If a Jira ID is referenced, fetch it with the Atlassian MCP `getJiraIssue` tool.

## Labels and state

See `triage-labels.md` for triage roles and human-review labels. Beads also use workflow labels (`stacked-branch`, `pr-created`, `in-review`) documented in the `bead-workflow` skill.
