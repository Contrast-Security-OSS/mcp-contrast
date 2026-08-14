---
name: jira-workflow
description: Jira ticket lifecycle for the AIML project. Covers creation, bead parity, and status transitions through the PR lifecycle. Use when creating a Jira ticket, when a PR is opened or merges for a Jira-linked branch, or when reconciling bead-to-Jira state. Triggers include "create a jira ticket", "create an AIML ticket", a bead needing an external Jira reference, or a user reporting a merged PR whose branch matches an AIML key.
---

# Jira Workflow

Beads (`br`) is the working issue store for this repo and Jira is the external reference. Every Jira ticket has a matching bead, and the two are kept in sync. This skill covers ticket creation, bead parity rules, and lifecycle transitions. For the broader beads workflow see the `bead-workflow` skill and `docs/agents/issue-tracker.md`.

**Public repo note.** This repository is public. Committed files may reference the AIML project, the `Contrast MCP Server` component, and the transition IDs below, all already public in this repo. Do not commit other component names, internal service or product names, or ticket content beyond what a public reader should see.

## 1. Create the ticket

All work on this repo files into the **AIML** Jira project with component **Contrast MCP Server**. Use the Atlassian MCP tools (load via ToolSearch if needed). Metadata and a creation example are in `references/aiml.md`.

- Issue type: `Story` for features, `Task` for simple non-feature changes (refactoring, docs, bug fixes), `Epic` for large multi-ticket efforts (typically managed by Product Management).
- Look up an assignee with `lookupJiraAccountId`, link related tickets with `createIssueLink`.
- When starting work, assign the ticket to the current user (`atlassianUserInfo`) and transition it to In Progress (`21`).

## 2. Keep a bead in parity (every time)

Beads is primary. A Jira ticket should not exist without a matching bead.

When a Jira ticket is created:

1. Find or create the matching bead. If a bead already drove the ticket, use it. Otherwise create one per the `bead-workflow` skill, matching the Jira issue type.
2. Prefix the bead title with the Jira key, then a colon and a space: `AIML-1304: <title>`. Do not use space-dash-space. Keep the existing title text.
3. Set the bead `external-ref` to the Jira key, `br update <bead> --external-ref AIML-1304`.
4. Add a bead comment with the Jira URL so the thread records the link.

**Keep relationships in parity.** Mirror Jira issue links as bead dependencies and bead dependencies as Jira links:

- Jira **Blocks** (A blocks B) maps to `br dep add <bead-B> <bead-A>` (B depends on A).
- Jira parent or Epic maps to `br dep add <child-bead> <parent-bead> --type parent-child`.
- Jira **Relates** has no hard bead dependency. Note it in a bead comment, or use parent-child if it is really hierarchy.

When you add a bead dependency for work that also has Jira tickets, add the matching `createIssueLink` so both sides agree.

**Child beads.** When creating a Jira ticket for a child bead whose parent has a Jira Epic, create the child as a Story under that Epic. If the parent's ticket is not an Epic, ask the user how to proceed.

## 3. Status transitions

Transition IDs are in `references/aiml.md`. The lifecycle for this repo:

| Event | Transition |
|-------|------------|
| Work starts on the bead | In Progress (`21`) |
| Ready PR opened targeting `main` | In Review (`41`) |
| Stacked draft PR opened | stay at In Progress |
| Stacked PR promoted to ready | In Review (`41`) |
| PR merged to `main` | Ready to Deploy (`51`) |
| Code released to production | Closed (`81`) |

**Post-merge rule.** When a PR merges and its branch name carries a Jira key (`AIML-XXX-...`), transition that ticket to Ready to Deploy (`51`). This applies whether or not beads are involved, the trigger is the merged PR. Skip it when the PR merges into another feature branch rather than `main` (a stacked PR landing on its parent).

## References

- `references/aiml.md`, AIML project metadata, issue types, component, creation example, transition IDs.
