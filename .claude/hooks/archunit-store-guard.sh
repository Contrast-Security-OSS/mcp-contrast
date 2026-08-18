#!/bin/bash
# PreToolUse hook: block manual writes to the ArchUnit freeze store, warn on reads.
# Covers Read/Write/Edit by file_path and Bash by command text.

# Capture stdin once — each jq invocation consumes it entirely
input=$(cat)
tool_name=$(echo "$input" | jq -r '.tool_name // ""')

reason="Do not modify archunit-store files manually. Load the archunit skill (/archunit). The fix is always a code change. Only the Gradle store-update task (./gradlew :contrast-mcp-core:test -ParchStoreUpdate) may modify these files. Use the Read tool to inspect them."

deny() {
  jq -n --arg reason "$reason" '{"hookSpecificOutput":{"hookEventName":"PreToolUse","permissionDecision":"deny","permissionDecisionReason":$reason}}'
  exit 0
}

if [ "$tool_name" = "Bash" ]; then
  command=$(echo "$input" | jq -r '.tool_input.command // ""')
  case "$command" in
    *archunit-store/*|*archunit-store)
      # Split on shell operators and check each segment against the allow-list.
      # A chained command like "git status; rm archunit-store/foo" must deny.
      segments=${command//&&/$'\n'}
      segments=${segments//||/$'\n'}
      segments=${segments//;/$'\n'}
      segments=${segments//|/$'\n'}
      while IFS= read -r seg; do
        seg="${seg#"${seg%%[![:space:]]*}"}"
        case "$seg" in
          *archunit-store/*|*archunit-store)
            case "$seg" in
              ./gradlew*|make*|git\ *) ;;
              *) deny ;;
            esac
            ;;
        esac
      done <<< "$segments"
      exit 0
      ;;
  esac
  exit 0
fi

file_path=$(echo "$input" | jq -r '.tool_input.file_path // ""')

case "$file_path" in
  *archunit-store/*|*archunit-store) ;;
  *) exit 0 ;;
esac

if [ "$tool_name" = "Read" ]; then
  jq -n --arg ctx "$reason" '{"hookSpecificOutput":{"hookEventName":"PreToolUse","additionalContext":$ctx}}'
  exit 0
fi

deny
