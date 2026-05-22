#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GATE_NAME="S8-METADATA-RULES"
TOOLS="list_vulnerability_types,get_protect_rules"
OUTPUT_FILE="$(mktemp)"
START_SECONDS="$(date +%s)"

cleanup() {
  rm -f "${OUTPUT_FILE}"
}
trap cleanup EXIT

log() {
  printf '[s8-metadata-rules] %s\n' "$*"
}

duration_ms() {
  local now_seconds
  now_seconds="$(date +%s)"
  printf '%s' "$(((now_seconds - START_SECONDS) * 1000))"
}

fail_with_tail() {
  local assertion_summary="$1"
  log "gate=${GATE_NAME} status=failed command='<redacted-gradle-command>' environmentTarget=local-stdio requestId=<redacted> traceId=<redacted> toolName=${TOOLS} authOutcome=local-noop httpStatus=not-applicable mcpStatus=failed durationMs=$(duration_ms) downstreamStatus=mocked downstreamCategory=teamserver-metadata-rules retryRefresh=not-applicable assertionSummary=\"${assertion_summary}\""
  sed "s#${ROOT_DIR}#<repo-root>#g" "${OUTPUT_FILE}" | tail -80
  exit 1
}

assert_file_exists() {
  local assertion="$1"
  local file="$2"

  if [[ ! -f "${file}" ]]; then
    log "gate=${GATE_NAME} assertion=${assertion} status=failed file=<repo-root>/${file#${ROOT_DIR}/} requestId=<redacted> traceId=<redacted>"
    exit 1
  fi
  log "gate=${GATE_NAME} assertion=${assertion} status=passed file=<repo-root>/${file#${ROOT_DIR}/} requestId=<redacted> traceId=<redacted>"
}

assert_file_absent() {
  local assertion="$1"
  local file="$2"

  if [[ -e "${file}" ]]; then
    log "gate=${GATE_NAME} assertion=${assertion} status=failed file=<repo-root>/${file#${ROOT_DIR}/} requestId=<redacted> traceId=<redacted>"
    exit 1
  fi
  log "gate=${GATE_NAME} assertion=${assertion} status=passed file=<repo-root>/${file#${ROOT_DIR}/} requestId=<redacted> traceId=<redacted>"
}

assert_contains() {
  local assertion="$1"
  local file="$2"
  local pattern="$3"

  if ! grep -E -q "${pattern}" "${file}"; then
    log "gate=${GATE_NAME} assertion=${assertion} status=failed file=<repo-root>/${file#${ROOT_DIR}/} pattern=<redacted> requestId=<redacted> traceId=<redacted>"
    exit 1
  fi
  log "gate=${GATE_NAME} assertion=${assertion} status=passed file=<repo-root>/${file#${ROOT_DIR}/} requestId=<redacted> traceId=<redacted>"
}

assert_not_contains() {
  local assertion="$1"
  local file="$2"
  local pattern="$3"

  if grep -E -q "${pattern}" "${file}"; then
    log "gate=${GATE_NAME} assertion=${assertion} status=failed file=<repo-root>/${file#${ROOT_DIR}/} pattern=<redacted> requestId=<redacted> traceId=<redacted>"
    exit 1
  fi
  log "gate=${GATE_NAME} assertion=${assertion} status=passed file=<repo-root>/${file#${ROOT_DIR}/} requestId=<redacted> traceId=<redacted>"
}

cd "${ROOT_DIR}"

COMMAND=(
  ./gradlew --no-daemon
  :contrast-mcp-core:test
  --tests '*CoreBoundaryTest'
  --tests '*ListVulnerabilityTypesToolTest'
  --tests '*GetProtectRulesToolTest'
  --tests '*GetProtectRulesParamsTest'
  :contrast-mcp-stdio-app:test
  --tests '*ListVulnerabilityTypesLocalParityTest'
  --tests '*GetProtectRulesLocalParityTest'
  --tests '*GetSastResultsToolTest'
)

log "gate=${GATE_NAME} status=start command='./gradlew --no-daemon <focused metadata/rules tests>' environmentTarget=local-stdio requestId=<redacted> traceId=<redacted> toolName=${TOOLS} authOutcome=local-noop httpStatus=not-applicable mcpStatus=not-started durationMs=0 downstreamStatus=mocked downstreamCategory=teamserver-metadata-rules retryRefresh=not-applicable credentials=<redacted> bearerToken=<redacted> apiKey=<redacted> serviceKey=<redacted> principal=<redacted> cursor=<redacted> responseBody=<redacted>"

if ! "${COMMAND[@]}" >"${OUTPUT_FILE}" 2>&1; then
  fail_with_tail "focused metadata/rules tests failed"
fi

if grep -Eiq 'eyJ[A-Za-z0-9._-]*|bearer[[:space:]]+[A-Za-z0-9._-]+|api[_-]?key[[:space:]]*[=:]|service[_-]?key[[:space:]]*[=:]|raw-token-value|local-org-id-should-not-serialize|AuthenticatedMcpPrincipal|BearerTokenContext|McpTransportContext' "${OUTPUT_FILE}"; then
  fail_with_tail "focused checks passed but Gradle output contained a forbidden secret or context marker"
fi

CORE_LIST_TYPES="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/vulnerability/ListVulnerabilityTypesTool.java"
CORE_PROTECT_RULES="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/attack/GetProtectRulesTool.java"
CORE_PROTECT_PARAMS="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/attack/params/GetProtectRulesParams.java"
STDIO_PROTECT_RULES="${ROOT_DIR}/contrast-mcp-stdio-app/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/attack/GetProtectRulesTool.java"
STDIO_PROTECT_PARAMS="${ROOT_DIR}/contrast-mcp-stdio-app/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/attack/params/GetProtectRulesParams.java"
CORE_SCAN_RESULTS="${ROOT_DIR}/contrast-mcp-core/src/main/java/com/contrast/labs/ai/mcp/contrast/tool/sast/GetSastResultsTool.java"

assert_file_exists "list_vulnerability_types_remains_in_core" "${CORE_LIST_TYPES}"
assert_file_exists "get_protect_rules_moved_to_core" "${CORE_PROTECT_RULES}"
assert_file_exists "get_protect_rules_params_moved_to_core" "${CORE_PROTECT_PARAMS}"
assert_file_absent "get_protect_rules_removed_from_stdio_app" "${STDIO_PROTECT_RULES}"
assert_file_absent "get_protect_rules_params_removed_from_stdio_app" "${STDIO_PROTECT_PARAMS}"
assert_file_absent "get_scan_results_remains_out_of_core" "${CORE_SCAN_RESULTS}"

assert_contains "legacy_get_protect_rules_name_preserved" "${CORE_PROTECT_RULES}" 'name = "get_protect_rules"'
assert_contains "get_protect_rules_uses_contrast_api_client" "${CORE_PROTECT_RULES}" 'ContrastApiClient'
assert_contains "get_protect_rules_calls_client_method" "${CORE_PROTECT_RULES}" 'contrastApiClient\.getProtectRules\(params\.appId\(\)\)'
assert_contains "get_protect_rules_declares_tool_context" "${CORE_PROTECT_RULES}" 'ToolContext toolContext'
assert_contains "get_protect_rules_forwards_tool_context" "${CORE_PROTECT_RULES}" 'executePipeline\(\(\) -> GetProtectRulesParams\.of\(appId\), toolContext\)'
assert_not_contains "get_protect_rules_has_no_rename_alias_churn" "${CORE_PROTECT_RULES}" 'list_application_rules|list_protect_rules|alias'
assert_not_contains "get_protect_rules_has_no_local_sdk_factory_cache_or_raw_sarif_access" "${CORE_PROTECT_RULES}" 'LocalSdkSingleTool|ContrastSDK|SDKExtensionFactory|SDKHelper|getSDKExtension|getOrgId|get_scan_results|GetSastResultsTool|SarifResult'
assert_not_contains "get_protect_rules_has_no_hidden_auth_or_org_parameters" "${CORE_PROTECT_RULES}" 'orgId|organizationId|organizationUuid|bearer|token|apiKey|serviceKey|credential'

log "gate=${GATE_NAME} status=passed command='./gradlew --no-daemon <focused metadata/rules tests>' environmentTarget=local-stdio requestId=<redacted> traceId=<redacted> toolName=${TOOLS} authOutcome=local-noop httpStatus=not-applicable mcpStatus=passed durationMs=$(duration_ms) downstreamStatus=mocked downstreamCategory=teamserver-metadata-rules retryRefresh=not-applicable assertionSummary=\"core metadata/rules tools, local parity, sanitized error output, legacy naming, and raw SARIF exclusion passed\" credentials=<redacted> bearerToken=<redacted> apiKey=<redacted> serviceKey=<redacted> principal=<redacted> cursor=<redacted> responseBody=<redacted>"
