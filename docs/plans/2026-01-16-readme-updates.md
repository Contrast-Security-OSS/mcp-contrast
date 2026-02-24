# README.md Updates: CHANGELOG Link & Available Tools

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Update README.md to include a link to CHANGELOG.md and add an "Available Tools" section listing all 13 MCP tools with brief descriptions.

**Architecture:** Simple documentation update - add two new sections to README.md without modifying existing content structure.

**Tech Stack:** Markdown documentation only.

---

## Task 1: Add CHANGELOG Link to README

**Files:**
- Modify: `README.md` (after the security warning block, before Quick Start)

**Step 1: Identify insertion point**

The CHANGELOG link should be added after the security warning (line 20) and before the Quick Start section (line 21). This is a natural location for "What's New" type content.

**Step 2: Add CHANGELOG link section**

Insert after line 20 (the closing `>` of the warning block):

```markdown

## What's New

See [CHANGELOG.md](CHANGELOG.md) for the complete release history, including breaking changes and new features.
```

**Step 3: Verify the link**

Run: `ls -la CHANGELOG.md`
Expected: File exists at root of repository.

**Step 4: Commit**

```bash
git add README.md
git commit -m "$(cat <<'EOF'
docs: add changelog link to README

Add a "What's New" section with a link to CHANGELOG.md so users
can easily discover release history and breaking changes.
EOF
)"
```

---

## Task 2: Add Available Tools Section to README

**Files:**
- Modify: `README.md` (after the "What's New" section, before Quick Start)

**Step 1: Add Available Tools section**

Insert after the "What's New" section:

```markdown

## Available Tools

The Contrast MCP Server provides 13 tools for security analysis and vulnerability management:

### Applications
| Tool | Description |
|------|-------------|
| `search_applications` | Search applications by name, tag, or metadata filters |
| `get_session_metadata` | Get session metadata fields available for an application |

### Vulnerabilities
| Tool | Description |
|------|-------------|
| `search_vulnerabilities` | Search vulnerabilities across all applications (org-level) |
| `search_app_vulnerabilities` | Search vulnerabilities within a specific application with session filtering |
| `get_vulnerability` | Get detailed vulnerability info including stack trace and remediation guidance |
| `list_vulnerability_types` | List all available vulnerability types for filtering |

### Libraries (SCA)
| Tool | Description |
|------|-------------|
| `list_application_libraries` | List libraries used by an application with vulnerability counts |
| `list_applications_by_cve` | Find applications affected by a specific CVE |

### Protection (ADR/Protect)
| Tool | Description |
|------|-------------|
| `search_attacks` | Search attack events with filtering by status, type, and rules |
| `get_protect_rules` | Get protection rules configured for an application |

### Coverage
| Tool | Description |
|------|-------------|
| `get_route_coverage` | Get route coverage data showing exercised vs discovered routes |

### SAST (Scan)
| Tool | Description |
|------|-------------|
| `get_scan_project` | Get SAST project details and vulnerability counts |
| `get_scan_results` | Get SAST scan results in SARIF format |
```

**Step 2: Verify tool count matches**

Run: `grep -c "@Tool(" src/main/java/com/contrast/labs/ai/mcp/contrast/tool/**/*Tool.java | grep -v ":0" | wc -l`
Expected: 13 (matches the number of tools listed)

**Step 3: Commit**

```bash
git add README.md
git commit -m "$(cat <<'EOF'
docs: add available tools section to README

List all 13 MCP tools organized by category (Applications,
Vulnerabilities, Libraries, Protection, Coverage, SAST) with
brief descriptions to help users understand available capabilities.
EOF
)"
```

---

## Task 3: Verify README formatting

**Files:**
- Review: `README.md`

**Step 1: Check markdown renders correctly**

The structure should now be:
1. Title & badges
2. Description & security warning
3. **What's New** (NEW)
4. **Available Tools** (NEW)
5. Quick Start
6. ... rest of document

**Step 2: Run any markdown linting if available**

Run: `make format && make check`
Expected: No formatting issues.

**Step 3: Create final commit if needed**

If any adjustments were needed, commit them:

```bash
git add README.md
git commit -m "docs: fix README formatting"
```

---

## Summary

This plan adds two sections to README.md:
1. **What's New** - Links to CHANGELOG.md for release history
2. **Available Tools** - Documents all 13 MCP tools organized by category

Total: 3 small tasks, ~10-15 minutes of work.
