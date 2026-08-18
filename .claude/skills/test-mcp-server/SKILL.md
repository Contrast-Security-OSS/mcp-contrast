---
name: test-mcp-server
description: Pre-release exploratory test of the Contrast stdio MCP server. Builds the jar fresh, wires it into a separate headless Claude instance, and has that instance spawn one subagent per discovered tool to test it against a live org. Run on demand only, before a release or when a human explicitly asks, not during routine feature development. Asks which mode to run if not specified.
---

# Pre-release MCP server test

Exercises a fresh build of the Contrast stdio MCP server before release. A helper
script builds the jar, wires that exact artifact into a separate headless Claude
instance, and has that instance test every tool it discovers. You show the human
the report so they can decide whether to release. This does not gate the release.

**When to run:** before a release, or when a human explicitly asks. This is not
part of routine feature development, so never invoke it automatically while
building features. It builds a jar and drives a live org, so it is deliberate and
on demand only.

## Usage

- `/test-mcp-server` runs the in-depth (regular) pass using the same model as the launching session.
- `/test-mcp-server smoke` runs a fast pass that just checks each tool's main use case.
- `/test-mcp-server regular <focus>` runs in-depth with a nudge, e.g. `/test-mcp-server regular focus on the server tools`.
- `--model <id>` overrides the model for orchestrator and testers, e.g. `/test-mcp-server smoke --model sonnet`.

## What to do

1. Work out the mode and focus from the arguments. If the first word is `smoke` or
   `regular`, use it as the mode and treat the rest as focus text. If no mode was
   provided, use `AskUserQuestion` to ask which mode to run before launching.
   Never default to a mode on your own. Always pass an explicit mode to the script.
2. Launch the helper in the background so the user can keep chatting. A regular
   run spawns one tester per tool and can take 15-20 minutes. Set
   `CLAUDE_CODE_PRINT_BG_WAIT_CEILING_MS=0` in the command so the background
   output capture waits indefinitely instead of cutting off at the default
   600-second ceiling:

   ```
   CLAUDE_CODE_PRINT_BG_WAIT_CEILING_MS=0 bash .claude/skills/test-mcp-server/run.sh <mode> <focus>
   ```

3. When it finishes, read its output and show the human the report as the script
   produced it. A one-line lead of your own is fine if it helps. Do not re-run any
   tools yourself.
4. If the script stops early (build failed, or credentials missing), relay the
   message plainly and stop.

## Notes

- Credentials come from `.env.integration-test`. The script sources them and never
  prints them. Do not read that file yourself.
- The run is read-only. The nested instance may only call the Contrast tools and
  spawn its testers, nothing else.
- The tool list is discovered from the running server, so a newly added tool is
  covered automatically with no change to this skill.
- This is a live exercise of the shipped tools. It does not run the unit or
  integration suites, which you run separately with `make check` and `make verify`.
