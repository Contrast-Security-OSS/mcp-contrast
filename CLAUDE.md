# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an MCP (Model Context Protocol) server for Contrast Security that enables AI agents to access and analyze vulnerability data from Contrast's security platform. It serves as a bridge between Contrast Security's API and AI tools like Claude, enabling automated vulnerability remediation and security analysis.

## Branching Requirements

**All code changes must be made on a feature branch.** Never commit directly to `main`.

Branch naming: `AIML-<ticket-id>-<short-description>` (e.g., `AIML-391-add-medium-low-note-counts`)

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
make check-test  # Run both checks and tests — use this before committing
make verify      # Run all tests including integration
make format      # Auto-format code with Spotless (also runs automatically via make check)
make build       # Build the project
make clean       # Clean build artifacts

# Verbose output when debugging failures
make test VERBOSE=1
make check VERBOSE=1
```

**After a compilation failure**, stale `.class` files remain and cause "Unresolved compilation problems" on the next test run. Always run `mvn clean test-compile` (or `make clean && make test`) to recover before continuing.

**Direct Maven commands** (verbose output, use make targets above for quiet output):
- **Build**: `mvn clean install` or `./mvnw clean install`
- **Test (unit)**: `mvn test`
- **Test (all)**: `source .env.integration-test && mvn verify`
- **Format code**: `mvn spotless:apply`
- **Run locally**: `java -jar target/mcp-contrast-0.0.11.jar --CONTRAST_HOST_NAME=<host> --CONTRAST_API_KEY=<key> --CONTRAST_SERVICE_KEY=<key> --CONTRAST_USERNAME=<user> --CONTRAST_ORG_ID=<org>`

**Note:** `make check` auto-formats before checking — no separate `make format` step needed. `make check-test` is the standard pre-commit verification command.

**Integration Tests:** Require Contrast credentials in `.env.integration-test` (copy from `.env.integration-test.template`). See INTEGRATION_TESTS.md for details.

### Docker Commands
- **Build Docker image**: `docker build -t mcp-contrast .`
- **Run with Docker**: `docker run -e CONTRAST_HOST_NAME=<host> -e CONTRAST_API_KEY=<key> -e CONTRAST_SERVICE_KEY=<key> -e CONTRAST_USERNAME=<user> -e CONTRAST_ORG_ID=<org> -i --rm mcp-contrast:latest -t stdio`

### Requirements
- Java 17+
- Maven 3.6+ (or use included wrapper `./mvnw`)
- Docker (optional, for containerized deployment)

## Architecture

### Core Components

**Main Application**: `McpContrastApplication.java` - Spring Boot application that discovers and registers MCP tools via component scanning.

**Tool Layer (tool-per-class pattern)**: Each MCP tool is a standalone `@Service` class organized by domain:
```
tool/
├── base/           # BaseMcpTool, BaseGetTool, ToolParams interfaces
├── validation/     # ToolValidationContext for fluent validation
├── assess/         # Vulnerability tools (search_vulnerabilities, get_vulnerability, etc.)
├── applications/   # Application tools (search_applications, get_session_metadata)
├── sca/            # Library tools (list_application_libraries, list_applications_by_cve)
├── adr/            # Attack tools (search_attacks, get_protect_rules)
├── sast/           # SAST tools (get_sast_project, get_sast_results)
└── coverage/       # Coverage tools (get_route_coverage)
```

**Base Classes**:
- `BaseMcpTool<P, R>` - For paginated search/list operations
- `BaseGetTool<P, R>` - For single-item retrieval operations
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

- **Framework**: Spring Boot 3.4.5 with Spring AI 1.0.0-RC1
- **MCP Integration**: Spring AI MCP Server starter
- **Contrast Integration**: Contrast SDK Java 3.4.2
- **Testing**: JUnit 5
- **Build Tool**: Maven with wrapper
- **Packaging**: Executable JAR and Docker container

**SDK Source Access:** The Contrast SDK Java source code is available in the parent directory at `/Users/chrisedwards/projects/contrast/contrast-sdk-java`. Reference this when you need to understand SDK types, method signatures, or behavior.

### Working with the Contrast Codebase

All Contrast repos live under `/Users/chrisedwards/projects/contrast/`. Most use `develop` as the default branch (not `main`). Always checkout the default branch and pull before reading.

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

1. **Tool-per-Class**: Each MCP tool is a standalone `@Service` class with `@Tool` annotation, extending `BaseMcpTool` or `BaseGetTool`
2. **@Tool Annotation**: Methods annotated with `@Tool(name = "snake_case_name")` are exposed to AI agents
3. **Params Pattern**: Each tool has an associated `*Params` class extending `ToolValidationContext` for validation
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

**Checkstyle:** Three rules enforced at `error` severity (run in `validate` phase via `make check`):
- `AvoidStarImport` — no wildcard imports
- `RegexpSinglelineJava` — no fully-qualified class names in code; use imports
- `MagicNumber` — no raw numeric literals; use named constants (HTTP status codes and -1/0/1/2/100 are ignored). **Before writing any numeric literal**, check `ValidationConstants` first — it has `DEFAULT_PAGE_SIZE`, `MAX_PAGE_SIZE`, `API_MAX_PAGE_SIZE`, `DEFAULT_LIBRARY_OBS_PAGE_SIZE`, `MIN_PAGE`, `DEFAULT_PAGE`. If no existing constant fits, declare `private static final int MY_CONSTANT = <value>` in the same class.

> ⛔ **PROHIBITED:** Modifying checkstyle rules, Spotless config, or any other linter/constraint config is **expressly forbidden** without explicit user permission. When code fails a check, fix the code — never relax the rule.

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
- Simplified `mock()`: `ClassName mock = mock()` not `mock(ClassName.class)`
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
- When adding or updating dependencies in `pom.xml`: always define the version as a `${property}` in `<properties>` — never inline in `<dependency>`; never use `RELEASE` or `LATEST` as a version specifier.

### Logging

- Default log location: `/tmp/mcp-contrast.log`
- Debug logging: Add `--logging.level.root=DEBUG` to startup arguments
- Console logging is minimal by design for MCP protocol compatibility

## Beads Workflow Requirements

This project uses Beads (br) for issue tracking. See the MCP resource `beads://quickstart` for usage details.

### Bead Command Reference

```bash
# Status — set in_progress immediately when starting; close only when done
br update <bead-id> --status in_progress
br close <bead-id> --reason "why it's done"   # --reason/-r is REQUIRED; positional arg fails
br reopen <bead-id>

# Viewing
br show <bead-id>
br ready                                       # list unblocked open beads
br list

# Comments — 'br comment' does NOT exist; use 'br comments add'
br comments add <bead-id> --message "short single-line text"
br comments add <bead-id> -f /tmp/comment.txt  # use -f for multi-line (avoids shell interpretation)

# Labels
br label add <bead-id> -l <label>
br label remove <bead-id> -l <label>

# Dependencies
br dep add <dependent> <prerequisite>          # dependent "blocks on" prerequisite
br dep add <child> <parent> --type parent-child
```

**Multi-line content — quoted heredoc prevents all shell interpretation:**
```bash
# Comments — pipe via stdin (-f - supported)
cat << 'EOF' | br comments add <bead-id> -f -
## Heading

With `code blocks`, **markdown**, and $(variables) — all literal.
EOF

# Description/design — capture via $() (inner heredoc is still safe with quoted delimiter)
br create "Title" --description "$(cat << 'EOF'
## Description with `code` and $(vars) — all literal.
EOF
)"

# Design field — same $() pattern works
br update <bead-id> --design "$(cat << 'EOF'
## Design with `code` and --flags and $(vars) — all literal.
EOF
)"
```

### Human Review Labels

- `needs-human-review` — DO NOT start work; ask human to review first
- `human-reviewed` — approved; AI may proceed

When reviewing a `needs-human-review` bead, update the description if needed, then:
```bash
br label remove <bead-id> -l needs-human-review
br label add <bead-id> -l human-reviewed
```

## Helper Scripts

- **`scripts/br-set-design <bead-id> <file-path>`** — Copy plan file into bead design field. Always use this instead of `br update --design` (CLI misparses `--` in markdown).

## Project Management

### Jira Issue Tracking

This project is tracked in Jira under the **AIML** project. When creating Jira tickets for this codebase:

**Standard Configuration:**
- **Project**: `AIML`
- **Component**: `Contrast MCP Server` (always use this component for work on this repository)
- **Issue Type**:
  - `Story` - for features and improvements
  - `Task` - for simple non-feature changes (refactoring, documentation, bug fixes)
  - `Epic` - for large features with many dependent tasks (typically managed by Product Management)

**Access**: Use the Atlassian MCP server to read or write Jira tickets programmatically.

**AIML Project Transition IDs** (use with `transitionJiraIssue`, cloudId: `https://contrast.atlassian.net`):
- `11` → To Do
- `21` → In Progress
- `41` → In Review
- `51` → Ready to Deploy
- `61` → Blocked
- `71` → Backlog
- `81` → Closed

**IMPORTANT**: When a jira ticket is created for a bead, you must do 2 things:
1. Update the `external-ref` of the bead to be the jira ticket id
2. Update the `title` of the bead to be prefixed with the jira ticket id.

----

## AI Development Workflow

This section defines the complete workflow for a Developer using AI agents working with beads and Jira tickets in this project.

### Workflow Overview

**Key Labels:**
- `stacked-branch` - Branch is based on another PR branch (not main)
- `pr-created` - Pull request has been created
- `in-review` - Pull request is ready for human review (not draft)

**Decision Tree:**

```
Branch Creation:
├─ Based on main → No special label
└─ Based on another PR branch → Label with `stacked-branch`

PR Creation:
├─ Has `stacked-branch` label?
│  └─ YES → Create DRAFT PR (Stacked PRs workflow)
│            - Base: parent PR's branch
│            - Labels: `pr-created` (NOT `in-review` yet)
│            - Add warning banner + dependency context
│
└─ NO → Create ready PR (Moving to Review workflow)
         - Base: main
         - Labels: `pr-created`, `in-review`
         - Standard PR description

Promoting Stacked PR (after base PR merges):
└─ Rebase onto main, update base branch, remove warnings
   Add `in-review` label, mark PR ready
```

### Starting Work on a Bead

**1. Determine if a feature branch is needed:**
   - **Bead has Jira ticket**: Create a new feature branch
     - **Ask user which branch to base it off of**
     - Show recently updated branches (sorted by most recent commits/PRs)
     - User may be working with stacked branches where each new branch comes off the previous PR branch
     - Name the branch with Jira ID prefix (e.g., `AIML-224-description`)
     - **If based on another PR branch (not main)**: Label bead with `stacked-branch`
   - **Bead is a child of Jira-linked bead**: Use the same branch as the parent bead
   - **Bead has no Jira association and no parent**:
     - **Ask user if it should have a Jira ticket**
     - Most code changes need a Jira ticket and branch before merging
     - Code changes should generally have a Jira ticket in scope

**2. Update bead status and labels:**
   - Set bead status to `in_progress`
   - Record the branch name in the bead (so it's easily found later)
   - **If this is a stacked branch** (based on another PR branch):
     - Label the bead with `stacked-branch`
     - Create blocks dependency: `br dep add <new-bead-id> <base-bead-id> --type blocks`
       - This represents that the base bead "blocks" the new bead from being merged
       - Example: If AIML-230 is stacked on AIML-228, use `br dep add mcp-uuv mcp-9kr --type blocks`

**3. Update Jira (if applicable):**
   - If bead has a linked Jira ticket:
     - Update Jira status to "In Progress" using Atlassian MCP
     - **Assign the ticket to the current user** (the authenticated Atlassian MCP user)

### Creating Related Beads

**When creating new beads from a Jira-linked bead:**
- Ask user if the new bead should be a child of the Jira-linked bead
- If yes, establish parent-child relationship using `br dep add <child> <parent>` with `parent-child` dependency type
- Child beads work on the same branch as their parent
  
### Managing Bead Dependencies

**Command syntax:** `br dep add <dependent-task> <prerequisite-task>`

Example: If B must be done after A completes, use `br dep add B A` (not `br dep add A B`).

Verify with `br show <task-id>` - dependent tasks show "Depends on", prerequisites show "Blocks".

NOTE: This is not for parent-child dependencies, these are blocks dependencies. 
**IMPORTANT** If you are asked to add a bead as a child or with phrasing that implies a parent-child relationship, ensure you add the dependency of type parent-child. The default is a blocks type.

### During Development

**Build and verify artifacts** as needed for testing:
- Build JAR for MCP server manual testing: `mvn clean package`
- Verify version logging to confirm correct build is running

### Testing Requirements Before Moving to Review

**CRITICAL: Before requesting review, you MUST:**
1. **Write tests for ALL code changes** - No exceptions
2. **Run unit tests** - `make format && make check-test` must pass with 0 failures
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

### Moving to Review

When user says "move to review" or "ready for review" for a bead WITHOUT the `stacked-branch` label:

1. Apply the `pr-created` and `in-review` labels to the bead(s) on this branch
2. Transition the linked Jira ticket to "In Review"
3. Push the feature branch
4. Run `/pr-tools:create-pr` — it generates the description and creates a ready-for-review PR targeting `main`

### Stacked PRs (Ready for Draft Review)

When user says "ready for stacked PR" or creating a PR for a bead WITH the `stacked-branch` label:

1. Apply the `pr-created` label to the bead(s) — do **not** add the `in-review` label yet
2. Keep the linked Jira ticket at "In Progress"
3. Push the branch
4. Run `/pr-tools:create-pr` — it auto-detects the stacked context, creates a draft PR with the warning banner, and targets the parent branch

The PR stays in draft until the parent merges and this branch is rebased onto `main` (use `/pr-tools:promote-stacked-pr`).

### Promoting Stacked PR to Ready for Review

When user says "promote stacked PR" or "finalize stacked PR" for a bead WITH the `stacked-branch` label:

1. Run `/pr-tools:promote-stacked-pr` — it rebases onto main with `--onto`, force-pushes safely, retargets the PR base, updates the body, and marks it ready
2. Add the `in-review` label to the bead and update notes with the PR URL
3. Keep the bead `in_progress` until the PR merges

### After PR is Merged to main

1. Close the bead with brief description of what was done.
2. Update Jira status to "Ready to Deploy"
3. Handle dependent stacked PRs:
   - Run `/pr-tools:after-pr-merged` to find and optionally promote child PRs

**Rationale:**
- "Ready to Deploy" indicates the code is merged and ready for the next release
- "Closed" should only be used when the code is actually deployed/released to production
- This allows tracking what code is ready to go out in the next release vs what's already deployed


### Closing Beads

**IMPORTANT**: Always ask the user before closing a bead.

**Cannot close parent beads** if they still have open children. Ensure all child beads are closed first.

**Parent beads** typically remain `in_progress` (with `in-review` label) until the PR review is complete and merged. Only close beads when explicitly instructed by the user.



@SECURITY.md