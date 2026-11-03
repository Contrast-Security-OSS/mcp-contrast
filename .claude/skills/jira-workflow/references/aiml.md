# AIML project metadata

All work on this repo files into the **AIML** Jira project. Use the Atlassian MCP tools.

## Jira metadata

| Field | Value |
|-------|-------|
| **Cloud ID** | `35f55002-5211-4f07-ae86-25b46703fe59` (UUID, not the site URL) |
| **Project Key** | `AIML` |
| **Component** | `Contrast MCP Server` (always, for work on this repo) |

## Issue types

| Type | Use for |
|------|---------|
| Story | User-facing features and improvements |
| Task | Simple non-feature changes, refactoring, documentation, bug fixes |
| Bug | Defects |
| Epic | Large features spanning multiple tickets (typically managed by PM) |

## Creating a ticket

```
cloudId: "35f55002-5211-4f07-ae86-25b46703fe59"
projectKey: "AIML"
issueTypeName: "Task"   (or Story, Bug, Epic)
summary: "Your ticket title"
description: "markdown body"
contentFormat: "markdown"
additional_fields: {"components": [{"name": "Contrast MCP Server"}], "labels": ["repo:mcp-contrast"]}
```

To assign, look up the account ID first with `lookupJiraAccountId` (searchString = name), then pass `assignee_account_id` on the create call. For yourself, use `atlassianUserInfo`.

## Workflow transitions (transitionJiraIssue)

`11` To Do, `21` In Progress, `41` In Review, `51` Ready to Deploy, `61` Blocked, `71` Backlog, `81` Closed.

See the main `SKILL.md` for bead-to-Jira parity rules (matching bead, title prefix, external-ref, relationship parity) and the transition-per-event table.
