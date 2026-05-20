#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GATE_NAME="S4A-CLIENT-BOUNDARY"
START_SECONDS="$(date +%s)"

log() {
  printf '[s4a-client-boundary] %s\n' "$*"
}

duration_ms() {
  local now_seconds
  now_seconds="$(date +%s)"
  printf '%s' "$(((now_seconds - START_SECONDS) * 1000))"
}

assert_not_contains() {
  local assertion="$1"
  local file="$2"
  local pattern="$3"

  if grep -E -q "${pattern}" "${file}"; then
    log "gate=${GATE_NAME} assertion=${assertion} status=failed file=${file#${ROOT_DIR}/} pattern=<redacted>"
    exit 1
  fi
  log "gate=${GATE_NAME} assertion=${assertion} status=passed file=${file#${ROOT_DIR}/}"
}

cd "${ROOT_DIR}"

log "gate=${GATE_NAME} step=tests command='./gradlew --no-daemon :contrast-mcp-core:test :contrast-mcp-stdio-app:compileJava' envTarget=local requestId=not-applicable traceId=not-applicable toolName=base-pipeline authOutcome=local-noop-and-configured-strategy-tested httpStatus=not-applicable mcpStatus=not-applicable downstreamStatus=not-invoked retryRefresh=not-applicable"
./gradlew --no-daemon :contrast-mcp-core:test :contrast-mcp-stdio-app:compileJava

CLIENT_SOURCE="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/client/ContrastApiClient.java"
assert_not_contains "client_has_no_hidden_auth_or_org_parameters" "${CLIENT_SOURCE}" 'orgId|organizationId|organizationUuid|bearer|token|apiKey|serviceKey|credential'

while IFS= read -r source_file; do
  assert_not_contains "core_base_has_no_local_sdk_factory_access" "${source_file}" 'ContrastSDKFactory|SDKExtensionFactory|SDKHelper|SdkApiClient|get_scan_results|sarif|SARIF'
done < <(find "${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/base" -name '*.java' -print)

log "gate=${GATE_NAME} assertion_summary=passed credentials=<redacted> bearerToken=<redacted> apiKey=<redacted> serviceKey=<redacted> principal=<redacted> cursor=<redacted> requestId=not-applicable traceId=not-applicable toolName=base-pipeline authOutcome=sanitized durationMs=$(duration_ms) downstreamCategory=not-invoked retryRefresh=not-applicable"
