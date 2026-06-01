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

assert_line_order() {
  local file="$1"
  local first_pattern="$2"
  local second_pattern="$3"
  local label="$4"
  local first_line
  local second_line
  ASSERTIONS=$((ASSERTIONS + 1))
  first_line="$(grep -En "${first_pattern}" "${ROOT_DIR}/${file}" | head -n1 | cut -d: -f1 || true)"
  second_line="$(grep -En "${second_pattern}" "${ROOT_DIR}/${file}" | head -n1 | cut -d: -f1 || true)"
  if [[ -z "${first_line}" || -z "${second_line}" || "${first_line}" -ge "${second_line}" ]]; then
    log "status=fail assertion=\"${label}\" firstLine=${first_line:-missing} secondLine=${second_line:-missing} file=${file} durationMs=$(duration_ms)"
    exit 1
  fi
  log "status=pass assertion=\"${label}\" firstLine=${first_line} secondLine=${second_line} file=${file}"
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

assert_missing ".github/workflows/publish-core-artifactory.yml" "Superseded standalone Artifactory publish workflow removed"
assert_contains ".github/workflows/gradle-release.yml" 'id-token: write' "Gradle release has OIDC permission"
assert_contains ".github/workflows/gradle-release.yml" 'jfrog/setup-jfrog-cli@[0-9a-f]{40}' "Gradle release uses SHA-pinned JFrog setup action"
assert_contains ".github/workflows/gradle-release.yml" 'JF_URL: \$\{\{ vars\.CONTRAST_JFROG_URL \}\}' "JFrog base URL comes from repo variable"
assert_contains ".github/workflows/gradle-release.yml" 'oidc-provider-name: \$\{\{ vars\.JFROG_OIDC_PROVIDER_NAME \}\}' "JFrog OIDC provider comes from repo variable"
assert_contains ".github/workflows/gradle-release.yml" 'ORG_GRADLE_PROJECT_contrastPublicMavenReleaseUrl: \$\{\{ vars\.CONTRAST_PUBLIC_MAVEN_RELEASE_URL \}\}' "Public Maven release URL comes from repo variable"
assert_contains ".github/workflows/gradle-release.yml" 'ORG_GRADLE_PROJECT_contrastArtifactoryUser: \$\{\{ steps\.[^.]+\.outputs\.oidc-user \}\}' "Gradle publish user comes from JFrog OIDC output"
assert_contains ".github/workflows/gradle-release.yml" 'ORG_GRADLE_PROJECT_contrastArtifactoryPassword: \$\{\{ steps\.[^.]+\.outputs\.oidc-token \}\}' "Gradle publish password comes from JFrog OIDC output"
assert_contains ".github/workflows/gradle-release.yml" 'publishMavenJavaPublicationToContrastPublicReleaseRepository' "Gradle release publishes core to public release repository"
assert_contains ".github/workflows/gradle-release.yml" 'CODE_SIGNING_PKEY' "Gradle release maps OpenPGP private key secret"
assert_contains ".github/workflows/gradle-release.yml" 'CODE_SIGNING_PASSPHRASE' "Gradle release maps OpenPGP passphrase secret"
assert_not_contains ".github/workflows/gradle-release.yml" 'CONTRAST_ARTIFACTORY_RELEASE_URL|CONTRAST_ARTIFACTORY_USER|CONTRAST_ARTIFACTORY_PASSWORD|artifact_exists_check|contrastInternalRelease|contrastArtifactoryReleaseUrl|contrastArtifactorySnapshotUrl' "Gradle release avoids legacy internal Artifactory publish flow"
assert_line_order ".github/workflows/gradle-release.yml" 'name: Checkout Release Tag' 'name: Publish contrast-mcp-core to public Artifactory' "Core publish runs after release tag checkout"
assert_line_order ".github/workflows/gradle-release.yml" 'name: Publish contrast-mcp-core to public Artifactory' 'name: Create GitHub Release' "Core publish runs before GitHub release creation"

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
assert_contains "contrast-mcp-core/build.gradle" 'contrastPublicMavenReleaseUrl' "Core public release publication URL is property-driven"
assert_contains "contrast-mcp-core/build.gradle" 'contrastPublicRelease' "Core remote publication repository is public-release named"
assert_not_contains "contrast-mcp-core/build.gradle" 'contrastInternalRelease|contrastInternalSnapshot|contrastArtifactoryReleaseUrl|contrastArtifactorySnapshotUrl' "Core publication config avoids legacy internal Artifactory names"
assert_not_contains "contrast-mcp-core/build.gradle" "${INTERNAL_MAVEN_REPOSITORY_PATTERN}" "Core publication config does not hardcode internal Maven repository URLs"
assert_contains "hack/verify-core-publication.sh" 'S3B-CORE-BOUNDARY-SMOKE' "Core boundary diagnostic remains available"

log "status=pass assertions=${ASSERTIONS} durationMs=$(duration_ms) assertionSummary=\"public workflow alignment checks passed\""
