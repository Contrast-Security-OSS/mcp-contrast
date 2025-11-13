# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an MCP (Model Context Protocol) server for Contrast Security that enables AI agents to access and analyze vulnerability data from Contrast's security platform. It serves as a bridge between Contrast Security's API and AI tools like Claude, enabling automated vulnerability remediation and security analysis.

## Build and Development Commands

### Building the Project
- **Build**: `mvn clean install` or `./mvnw clean install`
- **Test**: `mvn test` or `./mvnw test`
- **Format code**: `mvn spotless:apply` - Auto-format all Java files (run before committing)
- **Check formatting**: `mvn spotless:check` - Verify code formatting (runs automatically during build)
- **Run locally**: `java -jar target/mcp-contrast-0.0.11.jar --CONTRAST_HOST_NAME=<host> --CONTRAST_API_KEY=<key> --CONTRAST_SERVICE_KEY=<key> --CONTRAST_USERNAME=<user> --CONTRAST_ORG_ID=<org>`

**Note:** Spotless enforces Google Java Format style automatically. The `spotless:check` goal runs during the `validate` phase, so any `mvn compile`, `mvn test`, or `mvn install` will fail if code is not properly formatted. Run `mvn spotless:apply` before committing to ensure formatting is correct.

### Docker Commands
- **Build Docker image**: `docker build -t mcp-contrast .`
- **Run with Docker**: `docker run -e CONTRAST_HOST_NAME=<host> -e CONTRAST_API_KEY=<key> -e CONTRAST_SERVICE_KEY=<key> -e CONTRAST_USERNAME=<user> -e CONTRAST_ORG_ID=<org> -i --rm mcp-contrast:latest -t stdio`

### Requirements
- Java 17+
- Maven 3.6+ (or use included wrapper `./mvnw`)
- Docker (optional, for containerized deployment)

## Architecture

### Core Components

**Main Application**: `McpContrastApplication.java` - Spring Boot application that registers MCP tools from all service classes.

**Service Layer**: Each service handles a specific aspect of Contrast Security data:
- `AssessService` - Vulnerability analysis and trace data
- `SastService` - Static application security testing data
- `SCAService` - Software composition analysis (library vulnerabilities)
- `ADRService` - Attack detection and response events
- `RouteCoverageService` - Route coverage analysis
- `PromptService` - AI prompt management

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

### Development Patterns

1. **MCP Tools**: Services expose methods via `@Tool` annotation for AI agent consumption
2. **SDK Extension Pattern**: Enhanced data models extend base SDK classes with AI-friendly representations
3. **Hint Generation**: Rule-based system provides contextual security guidance
4. **Defensive Design**: All external API calls include error handling and logging

### Coding Standards

- **Prefer `var`** for local variables when the type is obvious from the right-hand side
- **Use `isEmpty()`** instead of `size() > 0` or `size() == 0` for collection checks
- **No wildcard imports** - All imports must be explicit. Do not use `import package.*` syntax
- **Simplified mock() syntax** - Use `mock()` without class parameter (Mockito 5.x+). When using `var`, specify the type explicitly: `ClassName mock = mock();` instead of `var mock = mock(ClassName.class);`. For explicit types: `ClassName mock = mock();` instead of `ClassName mock = mock(ClassName.class);`
- **Use AssertJ for test assertions** - Prefer AssertJ's fluent API over JUnit assertions for more readable and expressive tests. Use `assertThat(actual).isEqualTo(expected)` instead of `assertEquals(expected, actual)`, and `assertThat(condition).isTrue()` instead of `assertTrue(condition)`

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
   Or use the MCP tool:
   ```
   mcp__plugin_beads_beads__update(issue_id="<bead-id>", status="in_progress")
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

### Managing Bead Dependencies

**Command syntax:** `bd dep add <dependent-task> <prerequisite-task>`

Example: If B must be done after A completes, use `bd dep add B A` (not `bd dep add A B`).

Verify with `bd show <task-id>` - dependent tasks show "Depends on", prerequisites show "Blocks".

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

### During Development

**Build and verify artifacts** as needed for testing:
- Build JAR for MCP server manual testing: `mvn clean package`
- Verify version logging to confirm correct build is running

### Testing Requirements Before Moving to Review

**CRITICAL: Before requesting review, you MUST:**
1. **Write tests for ALL code changes** - No exceptions
2. **Run unit tests** - `mvn test` must pass with 0 failures
3. **Run integration tests** - `mvn verify` must pass (requires credentials in `.env.integration-test`)
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

### Moving to Review

**When user says "move to review" or "ready for review" for a bead WITHOUT the `stacked-branch` label:**

This workflow creates a standard PR ready for immediate review, targeting the `main` branch.

**1. Label the bead(s):**
   - Create/apply labels: `pr-created` and `in-review`
   - Apply to all beads worked on in this branch

**2. Push to remote:**
   - Push the feature branch to remote repository

**3. Create or update Pull Request:**
   - If PR doesn't exist, create it with base branch `main`
   - If PR exists, update the description
   - PR should be ready for review (NOT draft)

**4. Generate comprehensive PR description:**
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

**3. Push to remote:**
   - Push the feature branch: `git push -u origin <branch-name>`

**4. Create DRAFT Pull Request:**
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

**5. Verify configuration:**
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
   ```bash
   git fetch origin
   git checkout <feature-branch>
   git rebase origin/main
   ```
   - Handle conflicts if they arise (pause and ask user for guidance)
   - Clean rebase expected for well-structured stacks

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

Beads typically remain `in_progress` (with `in-review` label) until the PR review is complete and merged. Only close beads when explicitly instructed by the user.