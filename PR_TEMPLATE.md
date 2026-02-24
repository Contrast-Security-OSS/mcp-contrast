# PR Description Template

## Research Phase

Gather context from:
- Beads worked on for this branch
- Git commits (`git log`, `git diff`)
- Related voice notes
- Linked Jira tickets

## Structure

1. **Why** - Problem/need that motivated change
2. **What** - High-level changes made
3. **How** - Technical approach, decisions, patterns
4. **Walkthrough** - Guide reviewer through diff in logical order
5. **Testing** - Coverage added, results, edge cases

## Goal

Make review effortless. Reviewer should not need clarifying questions.

## Stacked PR Header

For PRs with `stacked-branch` label, prepend:

```
**⚠️ DO NOT MERGE - WAITING FOR <link to base PR>**

This is a stacked PR based on #<base-pr-number>.
Please review and merge #<base-pr-number> first,
then rebase this PR onto `main` before merging.

---

**Dependency Context:**
This PR builds on #<base-pr-number>. [Brief explanation of base PR and why this follows]

---
```
