#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

log() {
  printf '[core-artifactory-publication] %s\n' "$*"
}

cd "${ROOT_DIR}"

ACTIVE_PUBLIC_PATHS=(
  "contrast-mcp-core"
  ".github"
  "README.md"
  "CONTRIBUTING.md"
  "RELEASING.md"
  "AGENTS.md"
)

HOST_PATTERN='artifactory/maven|maven-release-local|maven-snapshot-local'
if rg -n "${HOST_PATTERN}" "${ACTIVE_PUBLIC_PATHS[@]}" >/tmp/core-artifactory-hostname-scan.txt; then
  log "assertion=active_public_config_omits_internal_artifactory_hostname status=failed"
  sed 's#https://[^[:space:]]*#<redacted-url>#g' /tmp/core-artifactory-hostname-scan.txt
  exit 1
fi
log "assertion=active_public_config_omits_internal_artifactory_hostname status=passed"

log "step=local_publication command='./gradlew --no-daemon :contrast-mcp-core:publishToMavenLocal :contrast-mcp-core:verifyCorePublicationMetadata'"
./gradlew --no-daemon :contrast-mcp-core:publishToMavenLocal :contrast-mcp-core:verifyCorePublicationMetadata
log "assertion=local_publication_without_remote_url_properties status=passed"

assert_missing_property_failure() {
  local task="$1"
  local property_name="$2"
  local version_arg="$3"
  local output_file
  output_file="$(mktemp)"

  set +e
  ./gradlew --no-daemon "${task}" "${version_arg}" "-P${property_name}=" >"${output_file}" 2>&1
  local status=$?
  set -e

  if [[ ${status} -eq 0 ]]; then
    log "assertion=remote_publish_requires_${property_name} status=failed reason=publish_task_succeeded"
    rm -f "${output_file}"
    exit 1
  fi

  if ! grep -Fq "Missing Gradle property ${property_name} for remote publication." "${output_file}"; then
    log "assertion=remote_publish_requires_${property_name} status=failed reason=missing_sanitized_error"
    sed 's#https://[^[:space:]]*#<redacted-url>#g' "${output_file}"
    rm -f "${output_file}"
    exit 1
  fi

  if grep -Eq "${HOST_PATTERN}" "${output_file}"; then
    log "assertion=remote_publish_requires_${property_name} status=failed reason=internal_hostname_in_error"
    sed 's#https://[^[:space:]]*#<redacted-url>#g' "${output_file}"
    rm -f "${output_file}"
    exit 1
  fi

  rm -f "${output_file}"
  log "assertion=remote_publish_requires_${property_name} status=passed"
}

assert_missing_property_failure \
  ":contrast-mcp-core:publishMavenJavaPublicationToContrastInternalReleaseRepository" \
  "contrastArtifactoryReleaseUrl" \
  "-Pversion=1.0.1"

assert_missing_property_failure \
  ":contrast-mcp-core:publishMavenJavaPublicationToContrastInternalSnapshotRepository" \
  "contrastArtifactorySnapshotUrl" \
  "-Pversion=1.0.1-SNAPSHOT"

log "assertion_summary=passed repository=contrast-internal-maven-release repository_url=<redacted> credentials=<redacted>"
