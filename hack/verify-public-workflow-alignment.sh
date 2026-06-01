#!/usr/bin/env bash
set -euo pipefail

# Temporary tracer-bullet gate for AIML-757 S3C. This script and the Makefile
# workflow-check target exist only to prove the public Gradle/docs/CI alignment
# during this implementation slice; remove both once the tracer has served its
# purpose and the durable CI/docs shape is settled.

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GATE="S3C-PUBLIC-WORKFLOW-ALIGNMENT"
START_NS="$(date +%s%N)"
ASSERTIONS=0
INTERNAL_MAVEN_REPOSITORY_PATTERN='artifactory/''maven|maven-''release-local|maven-''snapshot-local'

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

assert_match_count() {
  local file="$1"
  local pattern="$2"
  local expected="$3"
  local label="$4"
  local actual
  ASSERTIONS=$((ASSERTIONS + 1))
  actual="$(grep -Ec "${pattern}" "${ROOT_DIR}/${file}" || true)"
  if [[ "${actual}" != "${expected}" ]]; then
    log "status=fail assertion=\"${label}\" expected=${expected} actual=${actual} file=${file} durationMs=$(duration_ms)"
    exit 1
  fi
  log "status=pass assertion=\"${label}\" count=${actual} file=${file}"
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

assert_contains ".github/workflows/publish-core-artifactory.yml" 'workflow_dispatch:' "Artifactory publish workflow is manual"
assert_contains ".github/workflows/publish-core-artifactory.yml" 'release_tag:' "Artifactory publish workflow requires release_tag input"
assert_contains ".github/workflows/publish-core-artifactory.yml" 'contents: read' "Artifactory publish workflow uses read-only contents permission"
assert_not_contains ".github/workflows/publish-core-artifactory.yml" 'contents: write|pull-requests: write|packages: write|id-token: write' "Artifactory publish workflow has no write permissions"
assert_contains ".github/workflows/publish-core-artifactory.yml" 'needs: verify-core-artifact' "Artifactory publish waits for credential-free verification"
assert_not_contains ".github/workflows/publish-core-artifactory.yml" 'environment: contrast-artifactory-publish' "Artifactory workflow does not depend on removed protected environment"
assert_contains ".github/workflows/publish-core-artifactory.yml" 'CONTRAST_ARTIFACTORY_RELEASE_URL: \$\{\{ secrets\.CONTRAST_ARTIFACTORY_RELEASE_URL \}\}' "Artifactory URL comes from GitHub secret"
assert_contains ".github/workflows/publish-core-artifactory.yml" 'ORG_GRADLE_PROJECT_contrastArtifactoryReleaseUrl: \$\{\{ secrets\.CONTRAST_ARTIFACTORY_RELEASE_URL \}\}' "Artifactory URL maps to Gradle property"
assert_contains ".github/workflows/publish-core-artifactory.yml" 'publishMavenJavaPublicationToContrastInternalReleaseRepository' "Artifactory workflow publishes only to release repository task"
assert_contains ".github/workflows/publish-core-artifactory.yml" 'artifact_exists_check' "Artifactory workflow checks coordinate preexistence"
assert_contains ".github/workflows/publish-core-artifactory.yml" 'repository_url=<redacted>' "Artifactory evidence redacts repository URL"
assert_not_contains ".github/workflows/publish-core-artifactory.yml" "${INTERNAL_MAVEN_REPOSITORY_PATTERN}" "Artifactory workflow does not expose internal repository details"
assert_not_contains ".github/workflows/publish-core-artifactory.yml" 'git push|git tag|git commit|gh release create|docker (push|build)|DOCKERHUB|DIGICERT|setVersion' "Artifactory workflow does not mutate releases or Docker artifacts"

assert_contains ".github/dependabot.yml" 'package-ecosystem: "gradle"' "Dependabot tracks Gradle"
assert_not_contains ".github/dependabot.yml" 'package-ecosystem: "maven"' "Dependabot has no Maven ecosystem"

assert_contains "README.md" '\./gradlew :contrast-mcp-stdio-app:bootJar' "README documents user-facing JAR build command"
assert_not_contains "README.md" 'Contributor Build Workflow|aiml-hosted-mcp-server|includeBuild\("\.\./mcp-contrast"\)|checkstyle-suppressions|verifyCorePublicationMetadata' "README omits developer-facing contributor workflow"

assert_contains "CONTRIBUTING.md" 'JDK 21|Java 21' "CONTRIBUTING documents Java 21"
assert_contains "CONTRIBUTING.md" '\./gradlew' "CONTRIBUTING documents Gradle wrapper commands"
assert_contains "CONTRIBUTING.md" 'aiml-hosted-mcp-server' "CONTRIBUTING documents hosted local-dev consumer"
assert_contains "CONTRIBUTING.md" 'includeBuild\("\.\./mcp-contrast"\)' "CONTRIBUTING documents composite build substitution"
assert_contains "CONTRIBUTING.md" 'checkstyle-suppressions\.xml' "CONTRIBUTING documents Checkstyle suppressions"
assert_contains "CONTRIBUTING.md" 'verifyCorePublicationMetadata' "CONTRIBUTING documents core publication metadata verification"

assert_contains "AGENTS.md" '\./gradlew' "AGENTS documents Gradle wrapper commands"
assert_contains "AGENTS.md" 'JDK 21|Java 21' "AGENTS documents Java 21"
assert_contains "AGENTS.md" 'aiml-hosted-mcp-server' "AGENTS documents hosted local-dev consumer"
assert_contains "CLAUDE.md" 'make workflow-check' "CLAUDE documents temporary workflow-check target"
assert_contains "CLAUDE.md" 'contrast-mcp-stdio-app:bootJar' "CLAUDE documents stdio app bootJar"
assert_contains "CLAUDE.md" 'contrast-mcp-core:verifyCorePublicationMetadata' "CLAUDE documents core publication metadata verification"
assert_not_contains "CLAUDE.md" 'docs/plans|mcp-contrast-0\.0\.11|make format && make check-test|validate phase|(^|[[:space:]`])(\./mvnw|mvn)([[:space:]]|$)' "CLAUDE has no stale Maven or old build command references"

assert_contains "AGENTS.md" 'does not commit bead state to git' "AGENTS documents local-only bead state"
assert_not_contains "AGENTS.md" 'git add \.beads|Commit together|Always commit|commit it with|commit .*related code' "AGENTS does not tell agents to commit beads"

assert_contains "MCP_STANDARDS.md" 'For the local stdio app' "Standards distinguish local stdio registration"
assert_contains "MCP_STANDARDS.md" 'For the hosted remote server' "Standards distinguish hosted explicit registration"
assert_contains "MCP_STANDARDS.md" 'generated `tools/list` snapshot review' "Standards require hosted tools/list snapshot review"

assert_contains "build.gradle" "apply plugin: 'checkstyle'" "Gradle applies Checkstyle"
assert_contains "build.gradle" "configFile = rootProject.file\\('checkstyle.xml'\\)" "Gradle preserves Checkstyle config"
assert_contains "build.gradle" 'checkstyle.suppressions.file' "Gradle preserves Checkstyle suppressions path"
assert_contains "checkstyle.xml" 'severity" value="error"' "Checkstyle rules remain error severity"

assert_contains "contrast-mcp-core/build.gradle" "id 'maven-publish'" "Core module publishes with maven-publish"
assert_contains "contrast-mcp-core/build.gradle" 'verifyCorePublicationMetadata' "Core publication metadata is verified"
assert_contains "contrast-mcp-core/build.gradle" 'contrastArtifactoryReleaseUrl' "Core release publication URL is property-driven"
assert_contains "contrast-mcp-core/build.gradle" 'contrastArtifactorySnapshotUrl' "Core snapshot publication URL is property-driven"
assert_not_contains "contrast-mcp-core/build.gradle" "${INTERNAL_MAVEN_REPOSITORY_PATTERN}" "Core publication config does not hardcode internal Maven repository URLs"
assert_contains "hack/verify-core-publication.sh" 'S3B-CORE-BOUNDARY-SMOKE' "Core boundary diagnostic remains available"

log "status=pass assertions=${ASSERTIONS} durationMs=$(duration_ms) assertionSummary=\"public workflow alignment checks passed\""
