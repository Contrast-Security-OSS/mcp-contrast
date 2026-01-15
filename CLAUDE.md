# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an MCP (Model Context Protocol) server for Contrast Security that enables AI agents to access and analyze vulnerability data from Contrast's security platform. It serves as a bridge between Contrast Security's API and AI tools like Claude, enabling automated vulnerability remediation and security analysis.

## Build and Development Commands

### Building the Project

Use these make targets for all checks and tests:

```bash
make check       # Run format check (quiet output)
make test        # Run unit tests (quiet output)
make check-test  # Run both checks and tests
make verify      # Run all tests including integration
make format      # Auto-format code with Spotless
make build       # Build the project
make clean       # Clean build artifacts

# Verbose output when debugging failures
make test VERBOSE=1
make check VERBOSE=1
```

**Direct Maven commands** (verbose output, use make targets above for quiet output):
- **Build**: `mvn clean install` or `./mvnw clean install`
- **Test (unit)**: `mvn test`
- **Test (all)**: `source .env.integration-test && mvn verify`
- **Format code**: `mvn spotless:apply`
- **Run locally**: `java -jar target/mcp-contrast-0.0.11.jar --CONTRAST_HOST_NAME=<host> --CONTRAST_API_KEY=<key> --CONTRAST_SERVICE_KEY=<key> --CONTRAST_USERNAME=<user> --CONTRAST_ORG_ID=<org>`

**Note:** Spotless enforces Google Java Format style automatically. Run `make format` before committing.

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
- `contrast.api.protocol=https` (configurable for local development)

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

**String Validation:**
- `StringUtils.hasText()` or `isNotBlank()` over manual null/empty checks
- `isBlank()` better than `isEmpty()` (whitespace handling)

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
- Naming: `methodName_should_expectedBehavior_when_condition()`
- Example: `getVulnerability_should_return_data_when_valid_id()`
- **Anonymous builders**: Use `AnonymousXxxBuilder` pattern for complex mocks (see `AnonymousApplicationBuilder.java`)
  - Provides valid defaults for all fields with lenient stubbing
  - Tests only specify fields they care about: `AnonymousApplicationBuilder.validApp().withName("MyApp").build()`
  - Avoids over-mocking anti-pattern and UnnecessaryStubbingException

### Security Considerations

This codebase handles sensitive vulnerability data. The README contains critical warnings about data privacy when using with AI models. Never expose Contrast credentials or vulnerability data to untrusted AI services.

### Logging

- Default log location: `/tmp/mcp-contrast.log`
- Debug logging: Add `--logging.level.root=DEBUG` to startup arguments
- Console logging is minimal by design for MCP protocol compatibility

## Beads Workflow Requirements

This project uses Beads (bd) for issue tracking. See the MCP resource `beads://quickstart` for usage details.

### Bead Status Management

**IMPORTANT: Update bead status as you work:**

1. **When starting work on a bead**: Immediately set status to `in_progress`
   ```
   bd update <bead-id> status=in_progress
   ```

2. **While working**: Keep the bead `in_progress` until all work is complete, tested, and ready to close

3. **When work is complete**: Close the bead only after all acceptance criteria are met
   ```
   bd close <bead-id>
   ```

**Status lifecycle:**
- `open` → Task not yet started
- `in_progress` → Actively working on task (SET THIS WHEN YOU START!)
- `closed` → Task complete, tested, and merged

### Human Review Required Label

**IMPORTANT: Beads labeled `needs-human-review` require human approval before AI work begins.**

**AI Behavior:**
- Before starting work on any bead, check for the `needs-human-review` label
- If present, **DO NOT start work** on the bead
- Instead, ask the human to review the bead description and approach

### Human Review Label Workflow

**When a human reviews a bead with the `needs-human-review` label:**

1. **Review the bead description** - Evaluate the proposed approach, implementation details, and any concerns
2. **Update the bead** - If changes are needed based on review, update the bead description with the approved approach
3. **Update labels:**
   ```bash
   bd label remove <bead-id> needs-human-review
   bd label add <bead-id> human-reviewed
   ```
4. **Proceed to next bead** - Continue reviewing remaining beads with `needs-human-review` label

**Label meanings:**
- `needs-human-review` - Bead requires human approval before AI can start work
- `human-reviewed` - Bead has been reviewed and approved by human, AI may proceed when ready

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
     - Create blocks dependency: `bd dep add <new-bead-id> <base-bead-id> --type blocks`
       - This represents that the base bead "blocks" the new bead from being merged
       - Example: If AIML-230 is stacked on AIML-228, use `bd dep add mcp-uuv mcp-9kr --type blocks`

**3. Update Jira (if applicable):**
   - If bead has a linked Jira ticket:
     - Update Jira status to "In Progress" using Atlassian MCP
     - **Assign the ticket to the current user** (the authenticated Atlassian MCP user)

**4. Enter plan mode and present approach:**
   - Present a textual plan of what needs to be done
   - Discuss the approach with the user
   - **Tell user: "ASK ME TO GENERATE A PLAN WHEN YOU ARE READY"**
   - Wait for user approval before generating full plan and proceeding

### Creating Related Beads

**When creating new beads from a Jira-linked bead:**
- Ask user if the new bead should be a child of the Jira-linked bead
- If yes, establish parent-child relationship using `bd dep add <child> <parent>` with `parent-child` dependency type
- Child beads work on the same branch as their parent
  
### Managing Bead Dependencies

**Command syntax:** `bd dep add <dependent-task> <prerequisite-task>`

Example: If B must be done after A completes, use `bd dep add B A` (not `bd dep add A B`).

Verify with `bd show <task-id>` - dependent tasks show "Depends on", prerequisites show "Blocks".

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

All code changes require corresponding test coverage. Do not move to review without tests.

See INTEGRATION_TESTS.md for integration test setup and credentials.

### Creating High-Quality PR Descriptions

**This section defines the shared approach for creating PR descriptions used by both "Moving to Review" and "Stacked PRs" workflows.**

Human review is the bottleneck in AI-assisted development. Creating exceptional PR descriptions that make review effortless is critical to development velocity.

**Research Phase** - Gather comprehensive context from:
- All beads that have been worked on for this branch
- All git commits in the branch (`git log`, `git diff`)
- Voice notes that relate to this work
- Any related Jira tickets

**PR Description Structure:**

1. **Why**: Explain the problem or need that motivated this change
   - What problem are we solving?
   - What value does this provide?
   - What was the business or technical driver?

2. **What**: Describe what changes were made at a high level
   - What components/files were modified?
   - What new capabilities exist?
   - What behavior changed?

3. **How**: Explain how it was implemented
   - Technical approach and architecture decisions
   - Key implementation choices and trade-offs
   - Design patterns used
   - Integration points

4. **Step-by-step walkthrough**: Guide the reviewer through the changes in logical order
   - Walk through the diff in a sensible sequence
   - Explain complex sections
   - Call out important details
   - Help reviewer understand the flow

5. **Testing**: Summarize test coverage and results
   - Unit test coverage added
   - Integration test coverage added
   - Test results (pass/fail counts)
   - Manual testing performed
   - Edge cases covered

**Goal**: Make reviewing effortless by providing all information the reviewer needs to understand and evaluate the changes with confidence and speed. The reviewer should not need to ask clarifying questions. 

**IMPORTANT**: Ensure the text of the PR clearly explains all the changes and leads the developer through the logical progression of the chain of thought that produced it.

### Moving to Review

**When user says "move to review" or "ready for review" for a bead WITHOUT the `stacked-branch` label:**

This workflow creates a standard PR ready for immediate review, targeting the `main` branch.

**1. Label the bead(s):**
   - Create/apply labels: `pr-created` and `in-review`
   - Apply to all beads worked on in this branch

**2. Update Jira status (if applicable):**
   - If bead has linked Jira ticket, transition to "In Review" or equivalent review status

**3. Push to remote:**
   - Push the feature branch to remote repository

**4. Complete time tracking:**
   - Follow the **"Completing Time Tracking"** process in the Time Tracking section
   - This is for parent beads only (child beads were already rated when closed)

**5. Create or update Pull Request:**
   - If PR doesn't exist, create it with base branch `main`
   - If PR exists, update the description
   - PR should be ready for review (NOT draft)

**6. Generate comprehensive PR description:**
   - Follow the **"Creating High-Quality PR Descriptions"** section above
   - Use the standard structure: Why / What / How / Walkthrough / Testing
   - No special warnings or dependency context needed

### Stacked PRs (Ready for Draft Review)

**When user says "ready for stacked PR", "ready for draft review", or when creating a PR for a bead WITH the `stacked-branch` label:**

This workflow creates a draft PR that depends on another unmerged PR (stacked branches).

**1. Identify the base PR:**
   - Find the PR for the base branch using `gh pr list --head <base-branch-name>`
   - Note the PR number and URL

**2. Label the bead(s):**
   - Create/apply label `pr-created` to the bead
   - **Do NOT add `in-review` label yet** (only added when promoted to ready-for-review)
   - Apply to all beads worked on in this branch

**3. Update Jira status (if applicable):**
   - If bead has linked Jira ticket, keep status as "In Progress" (draft PR, not ready for review yet)

**4. Push to remote:**
   - Push the feature branch: `git push -u origin <branch-name>`

**5. Complete time tracking:**
   - Follow the **"Completing Time Tracking"** process in the Time Tracking section
   - This is for parent beads only (child beads were already rated when closed)

**6. Create DRAFT Pull Request:**
   - **Base branch**: Set to the parent PR's branch (NOT main)
   - **Status**: MUST be draft
   - **Title**: Include `[STACKED]` indicator
   - **Body**: Start with prominent warning, then add dependency context, followed by standard PR description:
     ```
     **⚠️ DO NOT MERGE - WAITING FOR <link to base PR>**

     This is a stacked PR based on #<base-pr-number>.
     Please review and merge #<base-pr-number> first,
     then rebase this PR onto `main` before merging.

     ---

     **Dependency Context:**
     This PR builds on the work from #<base-pr-number>. [Briefly explain
     what the base PR did and why this PR follows it]

     ---
     ```
   - After the warning and dependency context, follow the **"Creating High-Quality PR Descriptions"** section
   - Use the standard structure: Why / What / How / Walkthrough / Testing

**7. Verify configuration:**
   - Confirm PR is in draft status
   - Confirm base branch is the parent PR's branch
   - Confirm warning and dependency context are prominently displayed

**Example command:**
```bash
gh pr create --draft \
  --base AIML-226-parent-branch \
  --title "AIML-228: Feature name [STACKED]" \
  --body "$(cat <<'EOF'
**⚠️ DO NOT MERGE - WAITING FOR https://github.com/org/repo/pull/27**

This is a stacked PR based on #27. Please review and merge #27 first.

---

## Summary
[Your PR description here]
EOF
)"
```

**IMPORTANT**: Stacked PRs must remain in draft until:
1. The base PR is merged
2. This PR is rebased onto main
3. CI/CD passes on the rebased code

### Promoting Stacked PR to Ready for Review

**When user says "move stacked PR to ready for review", "promote stacked PR", or "finalize stacked PR" for a bead WITH the `stacked-branch` label:**

This workflow promotes a draft stacked PR to ready-for-review after its base PR has been merged to main.

**Prerequisites:**
- Bead must have `stacked-branch` label
- Base PR must be merged to main
- All tests must be passing on the stacked branch
- PR is currently in draft status targeting the base PR's branch

**Steps:**

**1. Identify PR context:**
   - Determine which PR to promote (by PR number or current branch)
   - Identify the base PR it depends on
   - Example: "Which PR should I promote?" or infer from current branch

**2. Verify base PR is merged:**
   ```bash
   gh pr view <base-pr-number> --json state,mergedAt,baseRefName
   ```
   - Must show `state: "MERGED"`
   - Note when it was merged for reference
   - If NOT merged, inform user and wait

**3. Fetch and rebase onto main:**

   **CRITICAL: Use `git rebase --onto` to avoid replaying base commits that are already in main!**

   ```bash
   # Fetch latest from origin (including the merged base PR)
   git fetch origin --prune

   # Checkout the stacked branch
   git checkout <feature-branch>

   # Find the base commit (last commit of the base branch)
   git log --oneline --graph --decorate -20
   # Look for the commit right before your stacked commits started
   # This is typically the last commit from the base branch

   # Rebase ONLY the stacked commits onto main
   git rebase --onto origin/main <base-commit-hash>
   ```

   **Example:**
   If your branch history shows:
   ```
   * abc1234 (HEAD) Your stacked commit 3
   * def5678 Your stacked commit 2
   * ghi9012 Your stacked commit 1
   * jkl3456 Last commit from base branch  <-- This is your <base-commit-hash>
   * mno7890 Base branch commit
   ```

   Use: `git rebase --onto origin/main jkl3456`

   This rebases only commits `ghi9012`, `def5678`, and `abc1234` onto main,
   avoiding conflicts from commits that are already merged.

   **Troubleshooting:**
   - If you get conflicts about changes already in main, you likely used the wrong base commit
   - Handle genuine conflicts by pausing and asking user for guidance
   - Clean rebase expected for well-structured stacks with correct base commit

**4. Force push safely:**
   ```bash
   git push --force-with-lease origin <feature-branch>
   ```
   - Use `--force-with-lease` (NOT `--force`) for safety
   - Prevents overwriting if branch was updated elsewhere

**5. Update PR base branch:**
   ```bash
   gh pr edit <pr-number> --base main
   ```
   - Changes PR from targeting base branch to targeting main
   - GitHub will update the diff automatically

**6. Update PR description:**
   - Remove "DO NOT MERGE" and dependency warnings
   - Remove stacked PR notes about targeting other branches
   - Add line confirming rebase: "Rebased onto main after #<base-pr> merged"
   - Update test counts if they changed
   - Keep all other content (Why/What/How/Testing)

   Example:
   ```bash
   # Create updated description in temp file
   gh pr view <pr-number> --json body -q .body > /tmp/pr_body.txt
   # Edit to remove warnings and add rebase note
   gh pr edit <pr-number> --body-file /tmp/updated_pr_body.txt
   ```

**7. Mark PR ready for review:**
   ```bash
   gh pr ready <pr-number>
   ```
   - Removes draft status
   - PR is now visible in review queue
   - CI/CD should trigger automatically

**8. Verify tests pass:**
   ```bash
   mvn clean verify
   ```
   - Run full test suite to verify rebase didn't break anything
   - Address any failures before proceeding
   - Check CI status on GitHub

**9. Update bead:**
   - **Add `in-review` label to the bead** (PR is now truly ready for human review)
   - Update bead notes with PR status: "Rebased onto main, ready for review"
   - Keep bead in `in_progress` status
   - Don't close until PR is merged

**10. Confirm completion:**
   - Provide PR URL to user
   - Confirm tests passing
   - Note CI status
   - Summary: "PR #<number> rebased onto main and ready for review"

**Example full workflow:**
```bash
# Check base PR merged
gh pr view 24 --json state,mergedAt
# Output: {"state":"MERGED","mergedAt":"2025-11-12T21:44:33Z"}

# Fetch and rebase
git fetch origin
git checkout AIML-224-consolidate-route-coverage
git rebase origin/main
# Output: Successfully rebased and updated refs/heads/AIML-224-consolidate-route-coverage

# Force push
git push --force-with-lease origin AIML-224-consolidate-route-coverage

# Update PR
gh pr edit 25 --base main
gh pr edit 25 --body-file /tmp/updated_description.txt
gh pr ready 25

# Verify
mvn clean verify
```

**Common Issues:**

**Rebase conflicts:**
- Pause and show user the conflicts
- Ask for guidance on resolution
- Don't attempt to auto-resolve without user input

**Base PR not merged:**
- Inform user: "Base PR #<number> is not yet merged (status: <state>)"
- Wait for user instruction
- Don't proceed with promotion

**Tests fail after rebase:**
- Report failures to user
- Keep PR in draft until tests pass
- Investigate if rebase introduced issues

**CI not triggering:**
- GitHub CI may take a few minutes to start
- Verify workflow configuration targets main branch
- Check `.github/workflows/` for PR triggers

### After PR is Merged

**When a PR is merged to main:**

This workflow completes the development cycle by closing the bead and updating the Jira ticket status.

**Steps:**

**1. Close the bead:**
   ```bash
   bd close <bead-id>
   ```
   - Provide a brief reason mentioning the merged PR
   - Example: "PR #28 merged to main. Successfully added appID and appName fields to VulnLight record."

**2. Update Jira status to "Ready to Deploy":**
   - Use the Atlassian MCP to transition the Jira ticket
   - Transition to "Ready to Deploy" status (NOT "Closed")
   - "Closed" status is reserved for when code is actually released/deployed
   - Example:
   ```python
   mcp__atlassian__transitionJiraIssue(
       cloudId="https://contrast.atlassian.net",
       issueIdOrKey="AIML-XXX",
       transition={"id": "51"}  # "Ready to Deploy" transition
   )
   ```

**3. Check for dependent beads/PRs:**
   - If this was a base PR for stacked PRs, the dependent PRs can now be promoted
   - Check for beads with `stacked-branch` label that depend on this one
   - Follow the "Promoting Stacked PR to Ready for Review" workflow for each dependent PR

**Rationale:**
- "Ready to Deploy" indicates the code is merged and ready for the next release
- "Closed" should only be used when the code is actually deployed/released to production
- This allows tracking what code is ready to go out in the next release vs what's already deployed

### Landing the Plane

**When user says "let's land the plane":**

This workflow is for ending the current session while preserving all state so work can continue seamlessly in a new session (due to context limits or time constraints).

1. **Create follow-up beads:**
   - Identify any remaining work that needs to be done
   - Create child beads of the current bead for each follow-up task
   - Use parent-child dependencies to maintain relationship

2. **Update current bead with complete status:**
   - Document everything done so far
   - Record current state, blockers, decisions made
   - Include any context the next AI will need to continue
   - Update bead notes with progress details

3. **Commit changes:**
   - Stage and commit all work-in-progress changes
   - Write appropriate commit message describing current state

4. **Generate continuation prompt:**
   - Create a prompt that will allow the user to resume work in next session
   - User should be able to copy/paste this prompt to continue working on the bead
   - Include bead ID, current status, and what needs to happen next

### Closing Beads

**IMPORTANT**: Always ask the user before closing a bead.

**Cannot close parent beads** if they still have open children. Ensure all child beads are closed first.

**For child beads:** When closing a child bead, complete time tracking using the **"Completing Time Tracking"** process in the Time Tracking section. This captures the time spent on that specific subtask.

**For parent beads:** Time tracking is completed when the PR is created, not when the bead is closed. Beads typically remain `in_progress` (with `in-review` label) until the PR review is complete and merged. Only close beads when explicitly instructed by the user.

### After Completing a Bead - Suggesting Next Steps

**When a bead is completed (closed or moved to review), proactively suggest what to work on next:**

**1. Check for parent bead context:**
   - If the completed bead is a child bead, check the parent bead using `bd show <parent-bead-id>`
   - Look in the parent bead's `notes` field for recommended execution order or priority guidance
   - Example: "Recommended execution order for child beads: 1. mcp-981, 2. mcp-dw1, 3. mcp-j1i..."

**2. Identify next bead options:**
   - Look at other child beads of the same parent (siblings)
   - Check for beads that are `status=open` and have no blocking dependencies
   - Use `bd ready` to find beads ready to work on
   - Consider priority levels (Priority 1 > Priority 2 > Priority 3)

**3. Present recommendations to the user:**
   - Clearly state the recommended next bead with its ID and title
   - Explain WHY it's recommended (e.g., "highest priority", "easy config with big impact", "follows logical sequence")
   - Provide a brief summary of what the bead involves
   - Show alternative beads if there are multiple good options
   - Ask the user if they want to work on the recommended bead or choose another

**Example response format:**
```
Based on the recommended execution order in the parent bead, the next task is:

## **mcp-dw1 - Enable parallel test execution** (Priority 2)

**Description:** Configure Maven Failsafe plugin to execute integration test classes in parallel.

**Why this next:**
- "Easy config, big impact" per parent bead notes
- Expected 2-4x speedup depending on CPU cores
- Quick configuration change

**Other available beads** (can work in any order):
- mcp-j1i - Add performance instrumentation (Priority 3)
- mcp-xni - Extract shared utilities (Priority 2)

Would you like to work on **mcp-dw1** next, or would you prefer a different one?
```

**When there are no more child beads:**
- If all child beads are closed and parent is complete, suggest moving parent to review
- If this was a standalone bead, suggest using `bd ready` to find next available work
- Check for any blocked beads that might now be unblocked