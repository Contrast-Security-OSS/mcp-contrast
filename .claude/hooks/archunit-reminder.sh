#!/bin/bash
# PostToolUse hook: remind to load the archunit skill when ArchUnit tests fail

# Capture stdin once — each jq invocation consumes it entirely
input=$(cat)

# PostToolUse payloads put Bash output under tool_response, not tool_result.
# The trailing ? suppresses errors if tool_response is not an object.
combined=$(echo "$input" | jq -r '(.tool_response.stdout? // "") + " " + (.tool_response.stderr? // "")')

# Only fire when ArchitectureTest appears in a FAILED context
if ! echo "$combined" | grep -q 'ArchitectureTest.*FAILED'; then
  exit 0
fi

jq -n '{"hookSpecificOutput":{"hookEventName":"PostToolUse","additionalContext":"ArchUnit test failed. Load the archunit skill (`/archunit`) before attempting a fix. The fix is always a code change, never a store edit or flag."}}'
