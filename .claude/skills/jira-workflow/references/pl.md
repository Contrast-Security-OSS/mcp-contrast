# PL project metadata

All work on this repo files into the **PL** (PLATFORM) Jira project. Use the Atlassian MCP tools. The old AIML project is deprecated, do not file there.

## Jira metadata

| Field | Value |
|-------|-------|
| **Cloud ID** | `35f55002-5211-4f07-ae86-25b46703fe59` (UUID, not the site URL) |
| **Project Key** | `PL` |
| **Component** | `MCP Server` (always, for work on this repo) |
| **Label** | `repo:mcp-contrast` (always, for work on this repo) |

## Issue types

| Type | Use for |
|------|---------|
| Story | User-facing features and improvements. Always under an Epic. |
| Task | Simple non-feature changes, refactoring, documentation, dependency and CI fixes |
| Bug | Defects |
| Epic | Large features spanning multiple tickets (typically managed by PM) |
| Research, Initiative, Sub-task | Available in the project, rarely needed here |

## Creating a ticket

```
cloudId: "35f55002-5211-4f07-ae86-25b46703fe59"
projectKey: "PL"
issueTypeName: "Task"   (or Story, Bug, Epic)
summary: "Your ticket title"
description: "markdown body"
contentFormat: "markdown"
parent: "PL-123"        (the Epic key, required for a Story)
additional_fields: {"components": [{"name": "MCP Server"}], "labels": ["repo:mcp-contrast"]}
```

To assign, look up the account ID first with `lookupJiraAccountId` (searchString = name), then pass `assignee_account_id` on the create call. For yourself, use `atlassianUserInfo`.

## Workflow transitions (transitionJiraIssue)

`2` Blocked, `11` Backlog, `21` Selected for Development (lands in To Do), `31` In Progress, `4` In Review, `3` Ready to Deploy, `41` Done (lands in Closed).

A new ticket starts in Backlog. These IDs differ from the old AIML project, so never reuse the AIML ones (for example `21` is not In Progress here).

See the main `SKILL.md` for bead-to-Jira parity rules (matching bead, title prefix, external-ref, relationship parity) and the transition-per-event table.
