# AI Agent Guidelines

## Issue Tracking with bd (beads)

**IMPORTANT**: This project uses **bd (beads)** for ALL issue tracking. Do NOT use markdown TODOs, task lists, or other tracking methods.

### Why bd?

- Dependency-aware: Track blockers and relationships between issues
- Git-friendly: Auto-syncs to JSONL for version control
- Agent-optimized: JSON output, ready work detection, discovered-from links
- Prevents duplicate tracking systems and confusion

### Quick Start

**Check for ready work:**
```bash
bd ready --json
```

**Create new issues:**
```bash
bd create "Issue title" -t bug|feature|task -p 0-4 --json
bd create "Issue title" -p 1 --deps discovered-from:bd-123 --json
```

**Claim and update:**
```bash
bd update bd-42 --status in_progress --json
bd update bd-42 --priority 1 --json
```

**Complete work:**
```bash
bd close bd-42 --reason "Completed" --json
```

### Issue Types

- `bug` - Something broken
- `feature` - New functionality
- `task` - Work item (tests, docs, refactoring)
- `epic` - Large feature with subtasks
- `chore` - Maintenance (dependencies, tooling)

### Priorities

- `0` - Critical (security, data loss, broken builds)
- `1` - High (major features, important bugs)
- `2` - Medium (default, nice-to-have)
- `3` - Low (polish, optimization)
- `4` - Backlog (future ideas)

### Workflow for AI Agents

1. **Check ready work**: `bd ready` shows unblocked issues
2. **Claim your task**: `bd update <id> --status in_progress`
3. **Work on it**: Implement, test, document
4. **Discover new work?** Create linked issue:
   - `bd create "Found bug" -p 1 --deps discovered-from:<parent-id>`
5. **Complete**: `bd close <id> --reason "Done"`
6. **Commit together**: Always commit the `.beads/issues.jsonl` file together with the code changes so issue state stays in sync with code state

### Jira Integration

When creating beads that relate to Jira tickets:
- **Always prepend the Jira issue ID to the bead title**
  - Example: `"AIML-224: Consolidate RouteCoverageService tools"`
  - If creating Jira ticket for existing bead, update title: `bd update <id> --title="AIML-XXX: ..."`
- **Set the `external_ref` to the Jira issue ID**
  - `bd update <id> --external-ref=AIML-224`
- This creates bidirectional linkage:
  - Bead tracks local technical work (code, tests, implementation)
  - Jira tracks project management (planning, stakeholders, timeline)
  - Clear connection between both via issue ID in title and external_ref field

### Auto-Sync

bd automatically syncs with git:
- Exports to `.beads/issues.jsonl` after changes (5s debounce)
- Imports from JSONL when newer (e.g., after `git pull`)
- No manual export/import needed!

### MCP Server (Recommended)

If using Claude or MCP-compatible clients, install the beads MCP server:

```bash
pip install beads-mcp
```

Add to MCP config (e.g., `~/.config/claude/config.json`):
```json
{
  "beads": {
    "command": "beads-mcp",
    "args": []
  }
}
```

Then use `mcp__beads__*` functions instead of CLI commands.

### Managing AI-Generated Planning Documents

AI assistants often create planning and design documents during development:
- PLAN.md, IMPLEMENTATION.md, ARCHITECTURE.md
- DESIGN.md, CODEBASE_SUMMARY.md, INTEGRATION_PLAN.md
- TESTING_GUIDE.md, TECHNICAL_DESIGN.md, and similar files

**Best Practice: Use a dedicated directory for these ephemeral files**

**Recommended approach:**
- Create a `history/` directory in the project root
- Store ALL AI-generated planning/design docs in `history/`
- Keep the repository root clean and focused on permanent project files
- Only access `history/` when explicitly asked to review past planning

**Example .gitignore entry (optional):**
```
# AI planning documents (ephemeral)
history/
```

**Benefits:**
- ✅ Clean repository root
- ✅ Clear separation between ephemeral and permanent documentation
- ✅ Easy to exclude from version control if desired
- ✅ Preserves planning history for archeological research
- ✅ Reduces noise when browsing the project

### Multi-Agent Collaboration

Multiple AI agents may work in the same repository simultaneously. Follow these rules:

- ⚠️ **NEVER revert unexpected changes without asking the user first**
  - Other agents may have made those changes intentionally
  - Ask: "I found unexpected changes in X. Should I keep or revert them?"
- ⚠️ **NEVER assume uncommitted changes are errors or accidents**
  - Another agent may be mid-task on a different branch
  - Check `git status` and ask before discarding work
- ✅ If you encounter merge conflicts, pause and ask for guidance
- ✅ Use bd to check if other work is `in_progress` before touching shared files
- ✅ Commit your work frequently to reduce conflict windows

### Code Refactoring with ast-grep

For bulk structural code changes (renaming, pattern replacement), use **ast-grep (sg)** instead of sed/grep:

```bash
# Preview changes
sg run -p 'ContrastConfig' -r 'ContrastSDKFactory' -l java src/

# Apply changes (-U = update all)
sg run -p 'config.getSDK()' -r 'sdkFactory.getSDK()' -l java -U src/

# Pattern with metavariable
sg run -p 'ReflectionTestUtils.setField($T, "config", config)' \
       -r 'ReflectionTestUtils.setField($T, "sdkFactory", sdkFactory)' -l java -U src/
```

**Why ast-grep over sed:**
- Understands Java syntax (won't match inside strings/comments)
- Handles formatting variations (whitespace, line breaks)
- Metavariables (`$VAR`) capture and reuse matched code
- Safer bulk refactoring across many files

### Testing Anti-Patterns

- ❌ **Don't test library behavior** - Never write tests that verify Lombok `@Getter`/`@Setter` works, or that Spring annotations function. Trust your dependencies.
- ✅ **Do test your logic** - Test business rules, validation, conditional branching, and integration points.

### Important Rules

- ✅ Use bd for ALL task tracking
- ✅ Always use `--json` flag for programmatic use
- ✅ Link discovered work with `discovered-from` dependencies
- ✅ Check `bd ready` before asking "what should I work on?"
- ✅ Store AI planning docs in `history/` directory
- ❌ Do NOT create markdown TODO lists
- ❌ Do NOT use external issue trackers
- ❌ Do NOT duplicate tracking systems
- ❌ Do NOT clutter repo root with planning documents
- ❌ Do NOT revert unexpected changes without asking

For more details, see README.md and QUICKSTART.md.
