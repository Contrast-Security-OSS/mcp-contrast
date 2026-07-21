#!/bin/bash
# PostToolUse hook: run Checkstyle after Java files are created or edited

f=$(jq -r '.tool_input.file_path // ""')

# Only run for Java files
if ! echo "$f" | grep -qE '\.java$'; then
  exit 0
fi

# Navigate to project root (two levels up from .claude/hooks/)
cd "$(dirname "$0")/../.." || exit 0

output=$(./gradlew checkstyleMain checkstyleTest 2>&1)
rc=$?

[ $rc -eq 0 ] && exit 0

# Feed violations back to Claude as additional context
jq -n --arg ctx "Checkstyle violations after editing $f:

$output" \
  '{"hookSpecificOutput":{"hookEventName":"PostToolUse","additionalContext":$ctx}}'
