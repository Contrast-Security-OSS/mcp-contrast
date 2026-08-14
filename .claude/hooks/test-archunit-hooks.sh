#!/bin/bash
# Smoke test for archunit-reminder.sh and archunit-store-guard.sh.
# Run: bash .claude/hooks/test-archunit-hooks.sh
# Pipes sample hook payloads through both hooks and checks fire/deny/silent.

H="$(cd "$(dirname "$0")" && pwd)"
pass=0
fail=0

check() {
  local name="$1" script="$2" payload="$3" expect="$4"
  local out
  out=$(echo "$payload" | bash "$H/$script")
  local got="silent"
  case "$out" in
    *'"permissionDecision": "deny"'*) got="deny" ;;
    *'"additionalContext"'*) got="context" ;;
  esac
  if [ "$got" = "$expect" ]; then
    pass=$((pass + 1))
    echo "PASS $name"
  else
    fail=$((fail + 1))
    echo "FAIL $name: expected $expect, got $got"
  fi
}

STORE="contrast-mcp-core/src/test/resources/archunit-store/frozen.txt"

# --- archunit-reminder.sh ---
check "reminder: FAILED in stdout fires" archunit-reminder.sh \
  '{"tool_name":"Bash","tool_response":{"stdout":"ArchitectureTest > rule FAILED","stderr":""}}' context
check "reminder: FAILED in stderr fires" archunit-reminder.sh \
  '{"tool_name":"Bash","tool_response":{"stdout":"ok","stderr":"ArchitectureTest > rule FAILED"}}' context
check "reminder: success output silent" archunit-reminder.sh \
  '{"tool_name":"Bash","tool_response":{"stdout":"BUILD SUCCESSFUL","stderr":""}}' silent
check "reminder: string tool_response silent" archunit-reminder.sh \
  '{"tool_name":"Bash","tool_response":"some text"}' silent

# --- archunit-store-guard.sh ---
check "guard: Read in store warns" archunit-store-guard.sh \
  '{"tool_name":"Read","tool_input":{"file_path":"/x/archunit-store/stored.rules"}}' context
check "guard: Edit in store denied" archunit-store-guard.sh \
  "{\"tool_name\":\"Edit\",\"tool_input\":{\"file_path\":\"$STORE\"}}" deny
check "guard: Write in store denied" archunit-store-guard.sh \
  "{\"tool_name\":\"Write\",\"tool_input\":{\"file_path\":\"$STORE\"}}" deny
check "guard: Write outside store silent" archunit-store-guard.sh \
  '{"tool_name":"Write","tool_input":{"file_path":"/x/src/main/Foo.java"}}' silent
check "guard: Bash sed on store denied" archunit-store-guard.sh \
  "{\"tool_name\":\"Bash\",\"tool_input\":{\"command\":\"sed -i s/a/b/ $STORE\"}}" deny
check "guard: Bash redirect into store denied" archunit-store-guard.sh \
  "{\"tool_name\":\"Bash\",\"tool_input\":{\"command\":\"echo x > $STORE\"}}" deny
check "guard: Bash gradlew store update allowed" archunit-store-guard.sh \
  '{"tool_name":"Bash","tool_input":{"command":"./gradlew :contrast-mcp-core:test -ParchStoreUpdate"}}' silent
check "guard: Bash git add store allowed" archunit-store-guard.sh \
  "{\"tool_name\":\"Bash\",\"tool_input\":{\"command\":\"git add $STORE\"}}" silent
check "guard: Bash unrelated silent" archunit-store-guard.sh \
  '{"tool_name":"Bash","tool_input":{"command":"ls -la"}}' silent

echo ""
echo "$pass passed, $fail failed"
exit $fail
