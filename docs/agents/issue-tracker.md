# Issue tracker: Jira + Beads

This repo tracks work in two coordinated systems, not GitHub Issues:

- **Jira** (project `AIML`, component `Contrast MCP Server`) — the system of record for
  tickets that ship. Issue types: `Story` (features), `Task` (refactors/docs/bugfixes),
  `Epic` (large multi-ticket efforts). Accessed via the Atlassian MCP server
  (cloudId `https://contrast.atlassian.net`).
- **Beads** (`br`) — local issue tracker for implementation work, hydrated from PRDs.
  Beads carry the `AIML-<id>` Jira prefix in their title and an `external-ref` linking
  back to Jira. See the `beads://quickstart` MCP resource and the Beads Workflow section
  of CLAUDE.md for full command reference.

Most code changes need a Jira ticket and a feature branch (`AIML-<id>-<desc>`) before merging.

## When a skill says "publish to the issue tracker"

Create a Beads issue with `br create`, then (for anything that will ship) create or link a
Jira ticket via the Atlassian MCP. When a Jira ticket is created for a bead, do two things:
1. Set the bead's `external-ref` to the Jira ticket id.
2. Prefix the bead's `title` with the Jira ticket id (e.g. `AIML-827: ...`).

For a PRD, prefer the project PRD location (`plans/<ticket>/`) over an issue body.

## When a skill says "fetch the relevant ticket"

- Bead: `br show <bead-id>`.
- Jira: `getJiraIssue` via the Atlassian MCP (issueIdOrKey `AIML-<n>`, cloudId
  `https://contrast.atlassian.net`).

## Jira transition IDs (AIML project)

`11` To Do · `21` In Progress · `41` In Review · `51` Ready to Deploy · `61` Blocked ·
`71` Backlog · `81` Closed.

## Labels and state

See `triage-labels.md` for triage roles. Beads also use workflow labels documented in
CLAUDE.md (`stacked-branch`, `pr-created`, `in-review`) and human-review labels
(`needs-human-review`, `human-security-review`, `external-approval`, `human-reviewed`).
