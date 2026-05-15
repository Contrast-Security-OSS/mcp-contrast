#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GATE="S3C-PUBLIC-WORKFLOW-ALIGNMENT"
START_NS="$(date +%s%N)"
ASSERTIONS=0

log() {
  printf 'gate=%s environmentTarget=local-docs authOutcome=not_applicable httpStatus=not_applicable mcpStatus=not_applicable downstreamStatus=not_applicable %s\n' "${GATE}" "$*"
}

duration_ms() {
  local end_ns
  end_ns="$(date +%s%N)"
  printf '%s' "$(((end_ns - START_NS) / 1000000))"
}

assert_contains() {
  local file="$1"
  local pattern="$2"
  local label="$3"
  ASSERTIONS=$((ASSERTIONS + 1))
  if ! grep -Eq "${pattern}" "${ROOT_DIR}/${file}"; then
    log "status=fail assertion=\"${label}\" file=${file} durationMs=$(duration_ms)"
    exit 1
  fi
  log "status=pass assertion=\"${label}\" file=${file}"
}

assert_not_contains() {
  local file="$1"
  local pattern="$2"
  local label="$3"
  ASSERTIONS=$((ASSERTIONS + 1))
  if grep -Eq "${pattern}" "${ROOT_DIR}/${file}"; then
    log "status=fail assertion=\"${label}\" file=${file} durationMs=$(duration_ms)"
    exit 1
  fi
  log "status=pass assertion=\"${label}\" file=${file}"
}

assert_missing() {
  local path="$1"
  local label="$2"
  ASSERTIONS=$((ASSERTIONS + 1))
  if [[ -e "${ROOT_DIR}/${path}" ]]; then
    log "status=fail assertion=\"${label}\" path=${path} durationMs=$(duration_ms)"
    exit 1
  fi
  log "status=pass assertion=\"${label}\" path=${path}"
}

log "status=start command=hack/verify-public-workflow-alignment.sh toolName=${GATE}"

assert_missing "pom.xml" "maven root pom removed"
assert_missing "mvnw" "maven wrapper removed"
assert_missing ".mvn" "maven wrapper directory removed"

assert_contains "Makefile" 'GRADLE \?= \./gradlew' "Makefile delegates through Gradle wrapper"
assert_not_contains "Makefile" '(^|[[:space:]])(\./mvnw|mvn)([[:space:]]|$)' "Makefile has no Maven command"

assert_contains ".github/workflows/build.yml" "java-version: '21'" "CI uses JDK 21"
assert_contains ".github/workflows/build.yml" '\./gradlew' "CI invokes Gradle wrapper"
assert_not_contains ".github/workflows/build.yml" '(^|[[:space:]])(\./mvnw|mvn)([[:space:]]|$)' "CI has no Maven command"
assert_contains ".github/workflows/build.yml" 'verify-public-workflow-alignment\.sh' "CI runs S3C workflow alignment gate"

assert_contains ".github/dependabot.yml" 'package-ecosystem: "gradle"' "Dependabot tracks Gradle"
assert_not_contains ".github/dependabot.yml" 'package-ecosystem: "maven"' "Dependabot has no Maven ecosystem"

assert_contains "README.md" 'JDK 21|Java 21' "README documents Java 21"
assert_contains "README.md" '\./gradlew' "README documents Gradle wrapper commands"
assert_contains "README.md" 'aiml-hosted-mcp-server' "README documents hosted local-dev consumer"
assert_contains "README.md" 'includeBuild\("\.\./mcp-contrast"\)' "README documents composite build substitution"

assert_contains "AGENTS.md" '\./gradlew' "AGENTS documents Gradle wrapper commands"
assert_contains "AGENTS.md" 'JDK 21|Java 21' "AGENTS documents Java 21"
assert_contains "AGENTS.md" 'aiml-hosted-mcp-server' "AGENTS documents hosted local-dev consumer"

assert_contains "WORKFLOW.md" 'br ready' "WORKFLOW uses br ready"
assert_not_contains "WORKFLOW.md" '(^|[[:space:]`])bd (ready|list|show|create|update|close|dep|stats|sync)' "WORKFLOW has no bd commands"
assert_contains "WORKFLOW.md" 'br sync --flush-only' "WORKFLOW documents explicit br sync"
assert_contains "WORKFLOW.md" 'do not commit \.beads' "WORKFLOW documents local-only bead state"
assert_contains "AGENTS.md" 'does not commit bead state to git' "AGENTS documents local-only bead state"
assert_not_contains "AGENTS.md" 'git add \.beads|Commit together|Always commit|commit it with|commit .*related code' "AGENTS does not tell agents to commit beads"
assert_not_contains "WORKFLOW.md" 'git add \.beads|Commit together|Always commit|commit it with related|commit .* with related code' "WORKFLOW does not tell agents to commit beads"

assert_contains "MCP_STANDARDS.md" 'For the local stdio app' "Standards distinguish local stdio registration"
assert_contains "MCP_STANDARDS.md" 'For the hosted remote server' "Standards distinguish hosted explicit registration"
assert_contains "MCP_STANDARDS.md" 'generated `tools/list` snapshot review' "Standards require hosted tools/list snapshot review"

assert_contains "build.gradle" "apply plugin: 'checkstyle'" "Gradle applies Checkstyle"
assert_contains "build.gradle" "configFile = rootProject.file\\('checkstyle.xml'\\)" "Gradle preserves Checkstyle config"
assert_contains "build.gradle" 'checkstyle.suppressions.file' "Gradle preserves Checkstyle suppressions path"
assert_contains "checkstyle.xml" 'severity" value="error"' "Checkstyle rules remain error severity"

assert_contains "contrast-mcp-core/build.gradle" "id 'maven-publish'" "Core module publishes with maven-publish"
assert_contains "contrast-mcp-core/build.gradle" 'verifyCorePublicationMetadata' "Core publication metadata is verified"
assert_contains "hack/verify-core-publication.sh" 'S3B-CORE-BOUNDARY-SMOKE' "Core boundary diagnostic remains available"

log "status=pass assertions=${ASSERTIONS} durationMs=$(duration_ms) assertionSummary=\"public workflow alignment checks passed\""
