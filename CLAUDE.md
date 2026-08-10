# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an MCP (Model Context Protocol) server for Contrast Security that enables AI agents to access and analyze vulnerability data from Contrast's security platform. It serves as a bridge between Contrast Security's API and AI tools like Claude, enabling automated vulnerability remediation and security analysis.

## Git Hooks

**ABSOLUTE RULE: NEVER skip git hooks.** Do not use `--no-verify`, `--no-gpg-sign`, `SKIP_COVERAGE_HOOK=1`, or any other mechanism to bypass pre-commit, pre-push, or any other git hook. No exceptions. No shortcuts. If a hook fails, fix the underlying problem. A skipped hook caused a CI build failure; this rule exists to prevent that from ever happening again. If you skip a hook, you have made an error.

## Branching Requirements

**All code changes must be made on a feature branch.** Never commit directly to `main`.

Branch naming: `AIML-<ticket-id>-<short-description>` (e.g., `AIML-391-add-medium-low-note-counts`)

## PR Requirements

PR Titles should be in the form: `<Jira Issue Id> <Title>` 
For example: `AIML-573 Generate and attach release SBOMs`

## Required Plugins

The workflows below use the **pr-tools** plugin (`/pr-tools:*` commands). If those skills aren't available, ask permission, then have the user run:

```
/plugin marketplace add Contrast-Security-Inc/claude-marketplace
/plugin install pr-tools@Contrast-Security-Inc
/reload-plugins
```

## Build and Development Commands

### Building the Project

Use these make targets for all checks and tests:

```bash
make check       # Auto-format then run static analysis (no need to run make format first)
make test        # Run unit tests (quiet output)
make coverage    # Verify JaCoCo coverage floors and print the summary
make check-test  # Run static analysis, unit tests, and coverage
make verify      # Run all tests including integration
make format      # Auto-format code with Spotless (also runs automatically via make check)
make build       # Build the project
make clean       # Clean build artifacts

make test-coverage                 # Unit tests plus coverage floors in one gradle invocation
make coverage-changed              # Changed src/main/java files must meet the changed-file floor
make coverage-changed BASE=origin/main   # Compare against a ref instead of the working tree
make buildsrc-check                # Static analysis, tests and coverage for buildSrc
make mutation                       # PIT mutation testing on contrast-mcp-core
make install-hooks                 # Install the pre-push hook (backs up any hook it replaces)

# Verbose output when debugging failures
make test VERBOSE=1
make check VERBOSE=1
make coverage VERBOSE=1
```

**After a compilation failure**, stale `.class` files may remain and cause confusing follow-up failures. Always run `make clean && make test` to recover before continuing.

**Direct Gradle commands** (verbose output, use make targets above for quiet output):
- **Build**: `./gradlew :contrast-mcp-stdio-app:bootJar`
- **Test (unit)**: `./gradlew test`
- **Test (all)**: `source .env.integration-test && ./gradlew test :contrast-mcp-stdio-app:integrationTest`
- **Static analysis**: `./gradlew spotlessCheck checkstyleMain checkstyleTest`
- **Coverage**: `./gradlew jacocoTestCoverageVerification coverageSummary`
- **Changed-file coverage**: `./gradlew jacocoChangedFileCoverageVerification -PjacocoChangedBase=origin/main`
- **Core publication metadata**: `./gradlew :contrast-mcp-core:verifyCorePublicationMetadata`
- **Mutation testing**: `./gradlew :contrast-mcp-core:pitest`
- **Format code**: `./gradlew spotlessApply`
- **Run locally**: `java -jar contrast-mcp-stdio-app/build/libs/mcp-contrast-*.jar --CONTRAST_HOST_NAME=<host> --CONTRAST_API_KEY=<key> --CONTRAST_SERVICE_KEY=<key> --CONTRAST_USERNAME=<user> --CONTRAST_ORG_ID=<org>`

**Note:** `make check` auto-formats before checking — no separate `make format` step needed. `make check-test` is the standard local verification command for static analysis, unit tests, and coverage.

**Coverage floors:** Per-module minimums live in `ext.coverageMinimums` in the root `build.gradle` and are enforced by `jacocoTestCoverageVerification`, which `check` depends on. Floors sit a couple of points under the measured figures on purpose, so one new uncovered branch cannot redden `main`. Raise a floor as coverage improves; never lower one to make a build pass. `verifyCoverageMinimums` fails the build if any floor drops below `ext.coverageStandardMinimum` (85%). `McpContrastApplication` is the only class excluded.

**Mutation testing:** PIT runs against `contrast-mcp-core` via the `info.solidsoft.pitest` Gradle plugin. It gates on test strength (killed / (killed + survived)), not mutation score, so it measures test quality independent of JaCoCo coverage. The `testStrengthThreshold` floor lives in `contrast-mcp-core/build.gradle` and is enforced in CI on pull requests. Raise the floor as survivors get fixed, never lower it. `lombok.config` at the repo root enables `@lombok.Generated` so both PIT and JaCoCo skip Lombok-generated code.

**Fixing PIT survivors:** When a mutation survives, determine whether it is a genuine test gap or an equivalent mutant. A genuine gap (the mutated behavior is observably different but no test catches it) is fixed by writing a better test. An equivalent mutant (the mutated behavior produces identical output through the public API) is left alone and accommodated by threshold headroom. Never restructure production code to eliminate equivalent-mutant sites. Small well-named helpers naturally create branches that are unreachable from their sole call site, and collapsing them to satisfy a metric trades readability for a number.

**Changed-file coverage:** `ext.changedFileCoverageMinimum` (85%) is enforced per changed `src/main/java` file by `jacocoChangedFileCoverageVerification`. Runs in CI on every pull request regardless of base branch, so stacked PRs are gated too, plus the pre-push hook (`make install-hooks`, bypass with `SKIP_COVERAGE_HOOK=1`) and `make coverage-changed`. The hook warns but still runs when dirty source, build, or resource files could change JaCoCo output; pull-request CI remains authoritative because it tests a clean checkout. Unrelated changes such as Markdown files do not warn. Set `COVERAGE_BASE_REF=origin/<parent>` on the first push of a new stacked branch. Not wired into `check`, which has no base ref to diff against. Logic lives in `buildSrc/`, which has its own checks via `make buildsrc-check`. See `scripts/git-hooks/README.md`.

The gate fails closed. A changed file absent from the JaCoCo report fails unless it is listed in `ext.coverageExcludedClassFiles`; a missing or empty report fails. A file that is in the report with no `LINE` counter has nothing countable to measure, so it passes and is named in the output. That covers roughly 49 of the 128 `contrast-mcp-core` sources, mostly interfaces and Lombok-only types.

**Integration Tests:** Require Contrast credentials in `.env.integration-test` (copy from `.env.integration-test.template`). See INTEGRATION_TESTS.md for details. Integration tests are intentionally skipped when credentials are not available (e.g., in CI forks or local builds without `.env.integration-test`).

### Docker Commands
- **Build Docker image**: `docker build -t mcp-contrast .`
- **Run with Docker**: `docker run -e CONTRAST_HOST_NAME=<host> -e CONTRAST_API_KEY=<key> -e CONTRAST_SERVICE_KEY=<key> -e CONTRAST_USERNAME=<user> -e CONTRAST_ORG_ID=<org> -i --rm mcp-contrast:latest -t stdio`

### Requirements
- Java 21+
- Gradle wrapper (`./gradlew`)
- Docker (optional, for containerized deployment)

## Architecture

### Core Components

**Main Application**: `McpContrastApplication.java` - Spring Boot application that discovers and registers MCP tools via component scanning.

**Tool Layer (tool-per-class pattern)**: Each MCP tool is a standalone `@Service` class organized by domain:
```
tool/
├── base/           # BaseTool, PaginatedTool, SingleTool, ToolParams interfaces
├── validation/     # ToolValidationContext for fluent validation
├── vulnerability/  # Vulnerability tools (search_vulnerabilities, get_vulnerability, etc.)
├── application/    # Application tools (search_applications, get_session_metadata)
├── library/        # Library tools (list_application_libraries, list_applications_by_cve)
├── attack/         # Attack tools (search_attacks, get_protect_rules)
├── server/         # Server tools (search_servers)
├── sast/           # SAST tools (get_scan_project, get_scan_results)
└── coverage/       # Coverage tools (get_route_coverage)
```

**Base Classes**:
- `PaginatedTool<P, R>` - For paginated search/list operations (extends `BaseTool`)
- `SingleTool<P, R>` - For single-item retrieval operations (extends `BaseTool`)
- `ToolValidationContext` - Fluent validation API for params

**SDK Extensions**: Located in `sdkextension/` package, these extend the Contrast SDK with enhanced data models and helper methods for better AI integration.

**Data Models**: Comprehensive POJOs in `data/` package representing vulnerability information, library data, applications, and attack events.

**Hint System**: `hints/` package provides context-aware security guidance for vulnerability remediation.

### Configuration

The application uses Spring Boot configuration with the following key properties:
- `spring.ai.mcp.server.name=mcp-contrast`
- `spring.main.web-application-type=none` (CLI application, not web server)
- `contrast.api.protocol=https` (HTTPS only — server rejects HTTP at startup)

Required environment variables/arguments:
- `CONTRAST_HOST_NAME` - Contrast TeamServer URL
- `CONTRAST_API_KEY` - API authentication key
- `CONTRAST_SERVICE_KEY` - Service authentication key  
- `CONTRAST_USERNAME` - User account
- `CONTRAST_ORG_ID` - Organization identifier

### Technology Stack

- **Framework**: Spring Boot 4.* with Spring AI 1.*
- **MCP Integration**: Spring AI MCP Server starter
- **Contrast Integration**: Contrast SDK Java 3.*
- **Testing**: JUnit 5
- **Build Tool**: Gradle with wrapper
- **Packaging**: Executable JAR and Docker container

**SDK Source Access:** The Contrast SDK Java source code is available at `../contrast-sdk-java`. Reference this when you need to understand SDK types, method signatures, or behavior.

### Working with the Contrast Codebase

All Contrast repos live under `../`. Most use `develop` as the default branch (not `main`). Always checkout the default branch and pull before reading.

**Finding code in unknown repos — search before guessing:**
```bash
gh search code "ClassName" --owner Contrast-Security-Inc --limit 10
```
This immediately shows which repo and path contains any class or symbol. Never guess a repo name and try to clone it — search first.

**Reading files from a repo with local changes blocking pull:**
```bash
gh api repos/Contrast-Security-Inc/repo-name/contents/path/to/File.java \
  --jq '.content' | base64 -d
```
When `git pull` fails due to local modifications (and `git stash` is hook-blocked), read files directly from GitHub instead of fighting the local state.

**Reliable default branch detection:**
```bash
git remote show origin | grep 'HEAD branch' | awk '{print $NF}'
```

**Do not use the Write tool for `/tmp` files.** A previous failed Bash attempt may have already created the file, causing Write to fail with "read first". Use Bash heredoc instead:
```bash
cat > /tmp/file.txt << 'EOF'
content here
EOF
```

### Development Patterns

1. **Tool-per-Class**: Each MCP tool is a standalone `@Service` class with `@Tool` annotation, extending `PaginatedTool`, `SingleTool`, or `CursorPaginatedTool`
2. **@Tool Annotation**: Methods annotated with `@Tool(name = "snake_case_name")` are exposed to AI agents
3. **Params Pattern**: Each tool has an associated `*Params` class extending `BaseToolParams` for validation
4. **Template Method**: Base classes enforce consistent pipeline (validation → execution → response building)
5. **SDK Extension Pattern**: Enhanced data models extend base SDK classes with AI-friendly representations
6. **Hint Generation**: Rule-based system provides contextual security guidance
7. **Defensive Design**: All external API calls include error handling and logging via base classes

### MCP Tool Standards

**All MCP tool development MUST follow the standards defined in [MCP_STANDARDS.md](./MCP_STANDARDS.md).**

When creating or modifying MCP tools:
- Read MCP_STANDARDS.md for complete naming and design standards
- Use `action_entity` naming convention (e.g., `search_vulnerabilities`, `get_vulnerability`)
- Follow verb hierarchy: `search_*` (flexible filtering) > `list_*` (scoped) > `get_*` (single item)
- Use camelCase for parameters, snake_case for tool names
- Document all tools with clear descriptions and parameter specifications
- See MCP_STANDARDS.md for anti-patterns, examples, and detailed requirements

### Coding Standards

**CLAUDE.md Principle**: Maximum conciseness to minimize token usage. Violate grammar rules for brevity. No verbose examples.

**Java Style:**
- `var` for obvious types: `var list = new ArrayList<String>()`
- No wildcard imports - explicit only
- Import order: static first, blank line, third-party alphabetically (Spotless handles)
- `.toList()` not `.collect(Collectors.toList())` (Java 16+)
- Guard clauses over nested ifs
- No fully-qualified class names - use imports
- `isEmpty()` not `size() > 0` for collections

**Comments (WHY for external-system quirks):**
- When code works around a TeamServer/SDK oddity, a magic sentinel, or other non-obvious external behavior, add a comment stating the concrete behavior and why the workaround exists. That reason lives in another system and cannot be recovered from this codebase alone.
- Cite the source when known, such as the class/method, the sentinel constant, or a filed ticket (e.g. `TS-43252` for a TeamServer defect).
- Do not comment self-evident code. Reserve this for reasoning the code cannot express on its own.

**Checkstyle:** 18 rules enforced at `error` severity by Gradle Checkstyle tasks via `make check`. The full list lives in `checkstyle.xml`. Highlights:
- **Imports:** `AvoidStarImport`, `UnusedImports`, `RedundantImport`, `RegexpSinglelineJava` (no FQCN — use imports)
- **Numbers:** `MagicNumber` — no raw numeric literals; use named constants (HTTP status codes and -1/0/1/2/100 are ignored). **Before writing any numeric literal**, check `ValidationConstants` first — it has `DEFAULT_PAGE_SIZE`, `MAX_PAGE_SIZE`, `API_MAX_PAGE_SIZE`, `DEFAULT_LIBRARY_OBS_PAGE_SIZE`, `MIN_PAGE`, `DEFAULT_PAGE`. If no existing constant fits, declare `private static final int MY_CONSTANT = <value>` in the same class. `UpperEll` (`1L` not `1l`).
- **Correctness:** `EmptyCatchBlock`, `MissingOverride`, `EqualsHashCode`, `StringLiteralEquality` (`s == "FOO"` is always wrong), `FallThrough`, `DefaultComesLast`, `MissingSwitchDefault`, `ModifiedControlVariable`
- **Style:** `SimplifyBooleanExpression`, `SimplifyBooleanReturn`
- **Codebase conventions (regex):** ban `Collectors.toList()` (use `.toList()`), `mock(X.class)` (use `mock()` with explicit-type LHS for assignments; extract to a typed local variable when at argument position — `mock()` without the class arg won't compile there), `.size() > 0` (use `isEmpty()`), JUnit assertions in tests (use AssertJ), `Assumptions.assume*` (fail loudly), manual `Logger` fields (use `@Slf4j`)

> ⛔ **PROHIBITED:** Modifying checkstyle rules, Spotless config, or any other linter/constraint config is **expressly forbidden** without explicit user permission. This includes adding entries to `checkstyle-suppressions.xml` — a suppression is relaxing a rule at a site, which is equally prohibited. When code fails a check, fix the code — never relax the rule.

**String Validation:**
- `StringUtils.hasText()` or `isNotBlank()` over manual null/empty checks
- `isBlank()` better than `isEmpty()` (whitespace handling)

**Enums:**
- Before using a string literal for a known set of values (e.g., severity codes), check for an existing enum in the SDK or codebase — use `MyEnum.VALUE.name()` instead of `"VALUE"`

**Lombok:**
- `@RequiredArgsConstructor` on `@Service` classes with `final` fields
- `@Slf4j` for logging (not manual `Logger` declaration)
- `@Value` + `@Builder` for immutable DTOs
- `@Data` for mutable POJOs needing getters/setters/toString/equals/hashCode (e.g., data classes, SDK extensions)
- `@Getter`/`@Setter` alone when only accessors needed (no equals/hashCode/toString)

**Logging:**
- `@Slf4j` annotation for logger injection
- SLF4J fluent API: `log.atInfo().setMessage("msg").addKeyValue("key", val).log()`
- Markers for categorization: `log.atInfo().addMarker(MCP_CONTRAST).log()`
- Levels: DEBUG (diagnostics), INFO (business events), WARN (handled exceptions), ERROR (critical failures)

**Null Handling:**
- `Optional<T>` for methods that may not return value
- Never return null collections - use `Collections.emptyList()` or empty collection
- `Optional.ofNullable(x).orElse(default)` over ternary `x != null ? x : default`

**Testing:**
- Simplified `mock()`: `ClassName mock = mock()` not `mock(ClassName.class)` — when `mock(X.class)` appears as a method argument (not an assignment), extract to a typed local first: `Foo x = mock(); when(x.method())...`
- AssertJ fluent: `assertThat(x).isEqualTo(y)` not `assertEquals(y, x)`
- Naming: `methodName_should_expectedBehavior_when_condition()` — body must verify the behavior the name promises. If assertions don't match the name, strengthen the assertions. Do **not** delete or weaken the name.
- Example: `getVulnerability_should_return_data_when_valid_id()`
- **Anonymous builders**: Use `AnonymousXxxBuilder` pattern for complex mocks (see `AnonymousApplicationBuilder.java`)
  - Provides valid defaults for all fields with lenient stubbing
  - Tests only specify fields they care about: `AnonymousApplicationBuilder.validApp().withName("MyApp").build()`
  - Avoids over-mocking anti-pattern and UnnecessaryStubbingException

**Assertion quality (applies to every test):**
- **Never delete a failing, flaky, or weak test to make it go away.** Strengthen, un-flake, or fix the underlying bug. Deletion is only allowed when the behavior itself is being deliberately removed — and requires user approval.
- **Exercise ≠ verify.** `assertThat(x).isNotNull()` + `log.info("✓ ...")` is not a test. No `✓` in logs as a stand-in for an assertion.
- **Mutation check.** Before committing, ask: *if I deleted the production logic under test, would this test go red?* If no, strengthen it until the answer is yes.
- **AAA completeness.** Every parameter set in arrange must appear in at least one assertion. Setting `severity="HIGH"` without asserting over returned severities means the arrange step is decorative.
- **Fail fast on missing data.** If a test requires seeded data, `assertThat(data).as("requires seeded X — ...").isNotEmpty()` as the first assertion. Never skip, never silently pass.
- **Prefer behavioral over shape assertions**: `allSatisfy`, `isSortedAccordingTo`, `containsExactlyInAnyOrderElementsOf`, `extracting(...).allMatch(...)` over `isNotNull` / `isNotEmpty`.
- **Deterministic expectations.** A test must assert a single expected outcome. Never write `if (response.isSuccess()) { ... } else { ... }` treating both branches as pass. If both outcomes are legitimate, split into two named tests, each with one expected outcome.
- **Populated ≠ non-null.** When a test name promises to "populate" or "return" a field, assert `isNotBlank()` for strings and `isNotEmpty()` for collections — not `isNotNull()`. An empty list is not populated; an empty string is not populated.

**Testing anti-patterns (do not reintroduce):**
- `if (data.isEmpty()) return;` — silent skip; assert `isNotEmpty()` up front with diagnostic message instead
- `Assumptions.assumeTrue(...)` — skipping hides data rot from CI; fail loudly instead
- `assertThat(items).isNotEmpty(); log.info("✓ filter works")` — logging is not an assertion
- Filter test that never calls `allSatisfy` over the filter predicate
- Sort test that never calls `isSortedAccordingTo`
- Pagination test that fetches page 2 without comparing IDs to page 1
- Combined-filter test that doesn't verify every filter predicate holds
- Error-path test using `errors().anyMatch(e -> e.contains("x"))` where `"x"` is just the parameter name — assert full message shape including valid options listed
- Dual-path test: `if (response.isSuccess()) { assertX } else { assertY }` — both branches treated as pass; split into deterministic tests
- Filter verified with `anyMatch(...)` — only proves one item matches; use `allMatch` / `allSatisfy` to prove every result matches the predicate
- Manual loop for sort/filter verification (e.g., `for (int i=1; i<list.size(); i++)`) — use `isSortedAccordingTo` / `allSatisfy` for clearer failure messages
- `_populate_*` or `_return_*` test using `isNotNull()` on the claimed field — asserts shape, not content; use `isNotBlank` / `isNotEmpty` / content-level check

### Security Considerations

This codebase handles sensitive vulnerability data. The README contains critical warnings about data privacy when using with AI models. Never expose Contrast credentials or vulnerability data to untrusted AI services.

**Dependency Policy (ENTSEC-1742):**
- Never suggest upgrading to a dependency version published fewer than 7 days ago. See SECURITY.md for the full policy and break glass procedure.
- When adding or updating dependencies in Gradle files: define versions in `gradle.properties` rather than inline dependency declarations; never use dynamic versions such as `+`, `latest.release`, `RELEASE`, or `LATEST`.

### Logging

- Default log location: `/tmp/mcp-contrast.log`
- Debug logging: Add `--logging.level.root=DEBUG` to startup arguments
- Console logging is minimal by design for MCP protocol compatibility

## Beads Workflow Requirements

This project uses Beads (`br`) for issue tracking. **Every bead mutation (create, claim, close, triage, comment, label) is governed by the `bead-workflow` skill** — invoke it before mutating a bead. Read-only commands (`br show`, `br ready`, `br list`) need no skill.

Quick reference (full detail in `docs/agents/issue-tracker.md`):
- `br update <id> --claim` when starting; `br close <id> --reason "..."` only when done (`--reason` is required)
- `br sync --flush-only` after mutations; `.beads/` is gitignored and must never be committed (public repo)
- Human-review and triage labels: see `docs/agents/triage-labels.md`
- Multi-line content: quoted heredocs; design field via `scripts/br-set-design`

## Helper Scripts

- **`scripts/br-set-design <bead-id> <file-path>`** — Copy plan file into bead design field. Always use this instead of `br update --design` (CLI misparses `--` in markdown).

## Project Management

### Jira Issue Tracking

This project is tracked in Jira under the **AIML** project, component **Contrast MCP Server**. **All Jira lifecycle operations — ticket creation, bead-to-Jira parity (title prefix + `external-ref`), and status transitions — are governed by the `jira-workflow` skill.** Metadata (issue types, transition IDs, creation example) lives in `.claude/skills/jira-workflow/references/aiml.md`.

----

## AI Development Workflow

The bead and Jira lifecycle (starting work, branching, stacked branches, labels, dependencies, closing) is owned by the `bead-workflow` and `jira-workflow` skills — invoke them rather than working from memory. The sections below cover what stays in this file: build verification and testing gates, plus the user trigger phrases that map to pr-tools skills.

**Workflow labels:** `stacked-branch` (branch based on another PR branch), `pr-created`, `in-review` (PR ready for human review, not draft). Details in the `bead-workflow` skill.

**Dependency direction:** `br dep add <dependent> <prerequisite>` — if B must wait for A, `br dep add B A`. When phrasing implies hierarchy ("add as a child"), use `--type parent-child`; the default is a blocks edge.

### During Development

**Build and verify artifacts** as needed for testing:
- Build JAR for MCP server manual testing: `./gradlew :contrast-mcp-stdio-app:bootJar`
- Verify version logging to confirm correct build is running

### Testing Requirements Before Moving to Review

**CRITICAL: Before requesting review, you MUST:**
1. **Write tests for ALL code changes** - No exceptions
2. **Run local verification** - `make check-test` must pass with 0 failures
3. **Run integration tests** - `make verify` must pass (requires credentials in `.env.integration-test`)
   - If credentials unavailable, verify integration tests pass in CI/CD
4. **Verify new tests are included** - Ensure your tests ran and passed

All code changes require corresponding test coverage. Do not create a PR until `make verify` passes. Do not move to review without tests.

See INTEGRATION_TESTS.md for integration test setup and credentials.

### Integration Test Standards

`*IT.java` tests verify the contract across the SDK/API boundary. Shape-only smoke checks belong in unit tests; an IT that only confirms "no exception thrown" has degraded into an auth smoke test.

**Required assertion patterns:**
- Filter by field → `allSatisfy(item -> assertThat(item.field())...)`
- Sort by field → `isSortedAccordingTo(Comparator.comparing(...))`
- Pagination → page N+1 items disjoint from page N by ID; count = `min(pageSize, remaining)`
- Combined filters → `allSatisfy` over every filter predicate, not `isSuccess()`
- Error path → specific message content + `noneMatch("Contrast API error")`
- Field mapping → response values reflect inputs (e.g., response `appID` equals filter `appId`)
- "Populate" claim → `isNotBlank` for strings, `isNotEmpty` for collections, content-level check — never just `isNotNull`
- Deterministic behavior → single expected outcome per test — no `if (isSuccess) ... else ...` dual-path

**Data dependency rule:**
ITs that depend on seeded data must assert the precondition up front with a descriptive `.as(...)` clause. Missing data must fail loudly with an actionable message — never skip, never silently pass.

```java
assertThat(libraries)
    .as("requires seeded libraries with CVE associations — see INTEGRATION_TESTS.md")
    .isNotEmpty();
```

**Canonical examples to emulate:**
- `SearchAttacksToolIT` sort-validation tests (`_should_reject_invalid_sort_fields`, `_should_return_validation_error_for_invalid_sort_field`)
- `GetVulnerabilityToolIT` — comprehensive field verification
- `GetRouteCoverageToolIT` — pagination + filter + edge cases
- `GetSastProjectToolIT` — regression coverage for field mapping

### Review and merge triggers

Each trigger phrase maps to a pr-tools skill; the `bead-workflow` and `jira-workflow` skills own the accompanying bead labels and Jira transitions.

| User says | Run | Bead / Jira effect |
|-----------|-----|--------------------|
| "move to review" / "ready for review" (no `stacked-branch` label) | `/pr-tools:create-pr` | `pr-created` + `in-review`; Jira → In Review |
| "ready for stacked PR" (`stacked-branch` label) | `/pr-tools:create-pr` (draft, targets parent) | `pr-created` only; Jira stays In Progress |
| "promote stacked PR" / "finalize stacked PR" | `/pr-tools:promote-stacked-pr` | add `in-review`; Jira → In Review |
| PR merged to `main` | `/pr-tools:after-pr-merged` | close bead (ask first); Jira → Ready to Deploy |

**Always ask the user before closing a bead.** Parent beads cannot close with open children and typically stay `in_progress` (with `in-review`) until the PR merges. Jira "Closed" is reserved for code actually released to production.

## Agent skills

### Bead workflow

Every bead mutation (create, claim, implement, close, triage, comment) follows the `bead-workflow` skill: pre-claim checks, stacked-branch handling, commit-before-close, sync discipline. See `.claude/skills/bead-workflow/`.

### Jira workflow

Jira ticket creation, bead parity, and status transitions follow the `jira-workflow` skill. See `.claude/skills/jira-workflow/`.

### Issue tracker

Issues are tracked in Jira (AIML project) and Beads (`br`), not GitHub Issues. Beads is the working store, Jira the external reference. See `docs/agents/issue-tracker.md`.

### Triage labels

Canonical triage labels (`needs-decision`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`) for all new triage work; older labels and human-review labels are documented for awareness. See `docs/agents/triage-labels.md`.

### Domain docs

Single-context: one `CONTEXT.md` + `docs/adr/` at the repo root (created lazily). See `docs/agents/domain.md`.

### Harness engineering

Run the harness-engineering playbooks against this repo on demand via `/harness-review` (broad diagnostic) and `/improve-harness` (one bounded change-and-verify loop). The corpus is pinned and kept read-only. See `docs/agents/harness.md`.

### Pre-release MCP test

`/test-mcp-server` runs an exploratory test of the freshly built server against the `.env.integration-test` org (in-depth by default, `smoke` for a fast pass). Run it before a release or when explicitly asked — never as part of routine feature development. See `.claude/skills/test-mcp-server/`.

### Pre-release changelog update

`/update-changelog` brings `[Unreleased]` in CHANGELOG.md up to date against everything merged since the last release tag, audits completeness with a read-only subagent per range, and lands the result on a branch after user approval. Run before dispatching the Gradle Release workflow or when explicitly asked. See `.claude/skills/update-changelog/`.



@SECURITY.md
