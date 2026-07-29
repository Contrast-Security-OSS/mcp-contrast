#!/usr/bin/env bash
#
# Copyright 2026 Contrast Security
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# Pre-release exploratory test for the Contrast stdio MCP server.
#
# Builds the stdio jar fresh from the current branch, wires that exact artifact
# into a throwaway headless Claude instance, and has that instance test every
# tool the server exposes. The report is printed to stdout for a human to read.
#
# Usage: run.sh [smoke|regular] [focus text...]
#   smoke    checks each tool's main use case only
#   regular  (default) in-depth exploratory testing; accepts an optional focus
#
# The tool list is never hardcoded here. The orchestrator asks the running
# server what it exposes, so a new tool is covered with no change to this script.

set -euo pipefail

# --- parse mode and focus -------------------------------------------------
# Default to regular. Only consume the first word if it is an explicit mode;
# otherwise the whole argument is focus text on a regular run.
MODE="regular"
if [[ "${1:-}" == "smoke" || "${1:-}" == "regular" ]]; then
  MODE="$1"
  shift
fi
FOCUS="$*"

# --- locate repo root -----------------------------------------------------
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../.." && pwd)"
cd "$REPO_ROOT"

log() { echo "[test-mcp-server] $*" >&2; }

# --- build the jar fresh --------------------------------------------------
log "Building the stdio MCP jar from the current branch..."
if ! ./gradlew :contrast-mcp-stdio-app:bootJar -q; then
  log "Build failed. Fix the build before testing. Aborting."
  exit 1
fi

JAR="$(ls -t contrast-mcp-stdio-app/build/libs/mcp-contrast-*.jar 2>/dev/null | grep -v -- '-plain.jar' | head -1 || true)"
if [ -z "$JAR" ]; then
  log "No runnable jar was produced under contrast-mcp-stdio-app/build/libs. Aborting."
  exit 1
fi
JAR="$(cd "$(dirname "$JAR")" && pwd)/$(basename "$JAR")"
log "Testing artifact: $JAR"

# --- credentials (never printed) ------------------------------------------
ENV_FILE="$REPO_ROOT/.env.integration-test"
if [ ! -f "$ENV_FILE" ]; then
  log "Missing .env.integration-test."
  log "Copy .env.integration-test.template, fill in credentials for a test org, then rerun. See INTEGRATION_TESTS.md."
  exit 1
fi
set -a
# shellcheck disable=SC1090
source "$ENV_FILE"
set +a
if [ -z "${CONTRAST_HOST_NAME:-}" ]; then
  log ".env.integration-test did not set CONTRAST_HOST_NAME. See INTEGRATION_TESTS.md."
  exit 1
fi

# --- wire the fresh jar into a throwaway MCP config -----------------------
# No secrets go in this file. The java subprocess inherits CONTRAST_* from the
# environment sourced above, so credentials never touch disk.
WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
MCP_CONFIG="$WORK/mcp.json"
cat > "$MCP_CONFIG" <<EOF
{
  "mcpServers": {
    "contrast": {
      "command": "java",
      "args": ["-jar", "$JAR"]
    }
  }
}
EOF

# --- the Sonnet tester persona (short and human on purpose) ---------------
TESTER_PROMPT='You are a hands-on tester for one Contrast MCP tool. You have the Contrast tools available. Work out what your tool does from its description, then find real data to test it with by calling the other Contrast tools first (for example, search for an application, a vulnerability, or a server). Then actually call your tool and see how it behaves. Report back plainly: what you tried, what worked, anything that looked wrong or confusing, and a one-word verdict of WORKS, ISSUES, BROKEN, or INCONCLUSIVE (use INCONCLUSIVE if you could not find data to test with). Keep it short and honest. Do not spawn other agents.'
AGENTS_JSON="$(cat <<EOF
{"mcp-tool-tester": {"description": "Exploratory tester for a single Contrast MCP tool", "prompt": "$TESTER_PROMPT", "model": "sonnet"}}
EOF
)"

# --- the Opus orchestrator brief ------------------------------------------
if [ "$MODE" == "smoke" ]; then
  DEPTH="This is a smoke test. Tell each tester to just confirm the tool works for its main use case, nothing more."
else
  DEPTH="This is an in-depth test. Tell each tester to explore its tool properly: filters, pagination, edge cases, and bad inputs, whatever a careful tester would try."
fi
FOCUS_LINE=""
if [ -n "$FOCUS" ]; then
  FOCUS_LINE="Pay extra attention to: $FOCUS"
fi

ORCH_PROMPT="$(cat <<EOF
You are checking the Contrast MCP server before we release it. You are connected to it right now.

First, list every tool the contrast MCP server exposes to you. Then spawn one mcp-tool-tester subagent per tool, in parallel, and tell each one which tool to test.

$DEPTH
$FOCUS_LINE

Let the subagents do the testing and find their own data. Do not test tools yourself beyond what you need to discover the tool list, and do not edit files or run shell commands.

When every tester has reported back, pull it all together into one report:
- Start with a short overall read: is this build safe to release, and the headline problems if not.
- Then one short section per tool: its verdict and the key things the tester found.
EOF
)"

# --- run the nested instance ----------------------------------------------
log "Launching a $MODE test. One Opus orchestrator, one Sonnet tester per tool."
log "This can take several minutes for a full regular run."

claude -p "$ORCH_PROMPT" \
  --model opus \
  --mcp-config "$MCP_CONFIG" \
  --strict-mcp-config \
  --allowedTools "mcp__contrast Task" \
  --agents "$AGENTS_JSON"
