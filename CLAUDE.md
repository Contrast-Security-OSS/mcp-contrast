# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an MCP (Model Context Protocol) server for Contrast Security that enables AI agents to access and analyze vulnerability data from Contrast's security platform. It serves as a bridge between Contrast Security's API and AI tools like Claude, enabling automated vulnerability remediation and security analysis.

## Build and Development Commands

### Building the Project
- **Build**: `mvn clean install` or `./mvnw clean install`
- **Test**: `mvn test` or `./mvnw test`
- **Run locally**: `java -jar target/mcp-contrast-0.0.11.jar --CONTRAST_HOST_NAME=<host> --CONTRAST_API_KEY=<key> --CONTRAST_SERVICE_KEY=<key> --CONTRAST_USERNAME=<user> --CONTRAST_ORG_ID=<org>`

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

**SDK Extensions**: Located in `sdkexstension/` package, these extend the Contrast SDK with enhanced data models and helper methods for better AI integration.

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

### Security Considerations

This codebase handles sensitive vulnerability data. The README contains critical warnings about data privacy when using with AI models. Never expose Contrast credentials or vulnerability data to untrusted AI services.

### Logging

- Default log location: `/tmp/mcp-contrast.log`
- Debug logging: Add `--logging.level.root=DEBUG` to startup arguments
- Console logging is minimal by design for MCP protocol compatibility

## Beads Workflow Requirements

This project uses Beads (bd) for issue tracking. See the MCP resource `beads://quickstart` for usage details.

### Managing Bead Dependencies

**Command syntax:** `bd dep add <dependent-task> <prerequisite-task>`

Example: If B must be done after A completes, use `bd dep add B A` (not `bd dep add A B`).

Verify with `bd show <task-id>` - dependent tasks show "Depends on", prerequisites show "Blocks".

### Testing Requirements Before Closing Beads

**CRITICAL: Before closing any bead, you MUST:**

1. **Write tests for ALL code changes** - No exceptions
2. **Run unit tests** - `mvn test` must pass with 0 failures
3. **Run integration tests** - `mvn verify` must pass (requires credentials in `.env.integration-test`)
   - If credentials unavailable, verify integration tests pass in CI/CD
4. **Verify new tests are included** - Ensure your tests ran and passed

All code changes require corresponding test coverage. Do not close beads without tests.

See INTEGRATION_TESTS.md for integration test setup and credentials.

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