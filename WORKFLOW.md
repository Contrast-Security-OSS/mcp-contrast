# Developer Workflow Quick Reference

Personal workflow guide for Chris Edwards - how to use beads + Jira + Claude Code effectively.

## Starting New Work

### 1. Check for Ready Work
```bash
bd ready
```

### 2. Create or Pick a Bead

**For Jira-tracked work:**
- Create Jira ticket first in AIML project
- Create bead with Jira ID in title and external_ref:
  ```bash
  bd create "AIML-XXX: Description" -t task -p 1 --external-ref AIML-XXX
  ```

**For discovered work during implementation:**
- Create child bead linked to parent:
  ```bash
  bd create "Fix bug found in feature X" -t bug -p 1 --deps discovered-from:mcp-parent-id
  ```

### 3. Start Work with Claude Code

Tell Claude to start work on the bead:
```
Start work on bead mcp-XXX
```

Claude will:
- Ask which branch to base off (for stacked PRs)
- Create feature branch (if Jira-linked): `AIML-XXX-description`
- Update bead status to `in_progress`
- Update Jira status to "In Progress" and assign to you
- Present a textual plan and ask you to discuss
- Wait for you to say "generate a plan" before proceeding

**Important**: Discuss the approach BEFORE telling Claude to generate a full plan.

## During Development

### Working with Claude Code

**Tell Claude what to do:**
- "Implement the changes we discussed"
- "Write tests for the new feature"
- "Run the full test suite"

**Claude follows your workflow automatically:**
- Writes tests for all code changes
- Runs `mvn test` and `mvn verify`
- Builds artifacts when needed (`mvn clean package`)
- Records branch name in bead

### Managing Related Work

**Creating child beads:**
- When Claude suggests new work, it will ask if it should be a child bead
- Child beads share the same branch as parent
- Use for: bug fixes found during implementation, follow-up tasks, subtasks

**Check dependencies:**
```bash
bd show mcp-XXX
```

## Moving to Review

When ready for review, tell Claude:
```
Move to review
```

Claude will:
1. Apply `in-review` label to all beads on the branch
2. Push branch to remote
3. Create or update PR with comprehensive description by researching:
   - All beads worked on in this branch
   - All commits and diffs
   - Related voice notes for context
   - Jira ticket details
4. Write PR description with why/what/how and step-by-step walkthrough

**Review the PR description** before marking ready - Claude makes it easy for reviewers.

## Long Sessions: Landing the Plane

When context is running low or you need to pause, tell Claude:
```
Let's land the plane
```

Claude will:
1. Create child beads for any remaining work
2. Update current bead with complete status and context
3. Commit all WIP with descriptive message
4. Generate continuation prompt for next session

Copy the continuation prompt to resume work in a fresh session.

## Closing Work

**Never close beads yourself during development.** Tell Claude when you're ready:
```
Close bead mcp-XXX
```

Claude will:
- Ask for confirmation
- Check for open child beads (can't close parent with open children)
- Only close after you explicitly confirm

**Typical lifecycle:**
1. `open` → Start work → `in_progress`
2. `in_progress` → Move to review → `in_progress` + `in-review` label
3. `in_progress` + `in-review` → PR merged → Ask Claude to close → `closed`

## Stacked PRs Workflow

When working on multiple related changes:

1. **First PR**: Branch from `main`, implement, create PR
2. **Second PR**: Tell Claude to base new branch off first PR branch
3. **Continue stacking**: Each new branch comes off the previous one

Claude will ask which branch to base off and show recent branches.

## Quick Commands Reference

### Beads
```bash
bd ready                                    # Show unblocked work
bd list --status in_progress                # Your current work
bd show mcp-XXX                             # Show bead details
bd update mcp-XXX --priority 0              # Bump priority
bd dep add mcp-child mcp-parent             # Add dependency
```

### Git
```bash
git log --oneline -10                       # Recent commits
git diff main...HEAD                        # All changes in branch
gh pr view                                  # View current PR
gh pr checks                                # Check CI status
```

### Maven
```bash
mvn test -q                                 # Run unit tests (quiet)
mvn verify -q                               # Run integration tests
mvn clean package                           # Build JAR
```

### Log Work
```bash
/log-work                                   # Log work to voice notes
```

## Tips

- **Always discuss the plan** with Claude before implementation
- **Let Claude manage workflow state** (bead status, Jira status, branches)
- **Use stacked PRs** for dependent changes to keep PRs small
- **Land the plane** when context runs low - don't fight it
- **Voice notes are your friend** - Claude uses them for PR descriptions
- **Test everything** - Claude won't move to review without passing tests

## Common Scenarios

### "I found a bug while implementing"
Tell Claude: "Create a child bead for this bug"
Claude will ask if it should be a child and create it.

### "This is taking too long"
Tell Claude: "Let's land the plane"
Resume in next session with the continuation prompt.

### "I need to work on something urgent"
Commit current work, switch branches. Return to original work by:
```
Continue work on bead mcp-XXX
```

### "PR got approved and merged"
Tell Claude: "Close bead mcp-XXX" (for all beads in that branch)

### "I need to update the PR description"
Tell Claude: "Update the PR description with latest changes"
Claude will research and regenerate.

## Files to Know

- `CLAUDE.md` - Instructions for Claude Code (AI workflow)
- `AGENTS.md` - General beads usage patterns
- `WORKFLOW.md` - This file (your reference)
- `.beads/issues.jsonl` - Bead database (auto-synced with git)
- `voice-notes/YYYY-MM-DD.md` - Daily work log

## Remember

**You focus on the work. Claude manages the workflow.**
