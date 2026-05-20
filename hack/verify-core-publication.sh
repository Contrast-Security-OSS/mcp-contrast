#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

log() {
  printf '[core-publication] %s\n' "$*"
}

cd "${ROOT_DIR}"

version="$(./gradlew -q printVersion)"
jar_path="${ROOT_DIR}/contrast-mcp-core/build/libs/contrast-mcp-core-${version}.jar"

log "gate=S3B-CORE-BOUNDARY-SMOKE step=publishToMavenLocal version=${version}"
./gradlew --no-daemon :contrast-mcp-core:publishToMavenLocal :contrast-mcp-core:verifyCorePublicationMetadata

log "gate=S3B-CORE-BOUNDARY-SMOKE step=jarBoundary jar=${jar_path}"
if [[ ! -f "${jar_path}" ]]; then
  log "assertion=jar_exists status=failed"
  exit 1
fi

required_classes=(
  "com/contrast/labs/ai/mcp/contrast/tool/validation/ToolValidationContext.class"
  "com/contrast/labs/ai/mcp/contrast/tool/base/ToolParams.class"
  "com/contrast/labs/ai/mcp/contrast/tool/vulnerability/ListVulnerabilityTypesTool.class"
  "com/contrast/labs/ai/mcp/contrast/hints/HintGenerator.class"
)

for required_class in "${required_classes[@]}"; do
  if jar tf "${jar_path}" | grep -Fxq "${required_class}"; then
    log "assertion=required_class_present class=${required_class} status=passed"
  else
    log "assertion=required_class_present class=${required_class} status=failed"
    exit 1
  fi
done

forbidden_classes=(
  "com/contrast/labs/ai/mcp/contrast/McpContrastApplication.class"
  "com/contrast/labs/ai/mcp/contrast/config/ContrastProperties.class"
  "com/contrast/labs/ai/mcp/contrast/config/ContrastSDKFactory.class"
  "com/contrast/labs/ai/mcp/contrast/config/SDKExtensionFactory.class"
  "com/contrast/labs/ai/mcp/contrast/client/SdkApiClient.class"
  "com/contrast/labs/ai/mcp/contrast/sdkextension/SDKHelper.class"
  "com/contrast/labs/ai/mcp/contrast/sdkextension/SDKExtension.class"
  "com/contrast/labs/ai/mcp/contrast/tool/sast/GetSastResultsTool.class"
)

for forbidden_class in "${forbidden_classes[@]}"; do
  if jar tf "${jar_path}" | grep -Fxq "${forbidden_class}"; then
    log "assertion=forbidden_class_absent class=${forbidden_class} status=failed"
    exit 1
  fi
  log "assertion=forbidden_class_absent class=${forbidden_class} status=passed"
done

log "assertion_summary=passed credentials=not-logged requestId=not-applicable toolName=not-applicable auth=not-applicable durationMs=not-measured"
