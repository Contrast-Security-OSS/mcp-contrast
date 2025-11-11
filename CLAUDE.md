# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

**Note**: This project uses [bd (beads)](https://github.com/steveyegge/beads) for issue tracking. Use `bd` commands instead of markdown TODOs. See AGENTS.md for workflow details.

## Project Overview

This is an MCP (Model Context Protocol) server for Contrast Security that enables AI agents to access and analyze vulnerability data from Contrast's security platform. It serves as a bridge between Contrast Security's API and AI tools like Claude, enabling automated vulnerability remediation and security analysis.

## Build and Development Commands

### Building the Project
- **Build**: `mvn clean install` or `./mvnw clean install`
- **Package without tests**: `mvn clean package -DskipTests`
- **Test**: `mvn test` or `./mvnw test`
- **Run single test**: `mvn test -Dtest=HintGeneratorTest` or `mvn test -Dtest=HintGeneratorTest#specificTestMethod`
- **Run locally**: `java -jar target/mcp-contrast-0.0.12-SNAPSHOT.jar --CONTRAST_HOST_NAME=<host> --CONTRAST_API_KEY=<key> --CONTRAST_SERVICE_KEY=<key> --CONTRAST_USERNAME=<user> --CONTRAST_ORG_ID=<org>`

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

**Service Layer**: Each service handles a specific aspect of Contrast Security data and exposes `@Tool` annotated methods:
- `AssessService` - Vulnerability analysis and trace data
- `SastService` - Static application security testing data
- `SCAService` - Software composition analysis (library vulnerabilities)
- `ADRService` - Attack detection and response events
- `RouteCoverageService` - Route coverage analysis

**SDK Extensions**: Located in `sdkexstension/` package:
- `SDKExtension.java` - Extends Contrast SDK API with additional endpoints not in standard SDK (library observations, route details, session metadata, etc.)
- `SDKHelper.java` - Utility methods for hostname protocol handling and common operations
- `data/` subpackages - Enhanced data models with AI-friendly representations organized by domain (application, adr, routecoverage, sca, traces, sessionmetadata)

**Data Models**: Comprehensive POJOs in `data/` package representing vulnerability information, library data, applications, and attack events used by service layer.

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

- **Framework**: Spring Boot 3.4.5 with Spring AI 1.0.1
- **MCP Integration**: Spring AI MCP Server starter
- **Contrast Integration**: Contrast SDK Java 3.4.2
- **JSON Processing**: Gson (via Contrast SDK)
- **Testing**: JUnit 5 with Spring Boot Test
- **Build Tool**: Maven 3.6+ with wrapper
- **Packaging**: Executable JAR and Docker container

### Development Patterns

1. **MCP Tools**: Services expose methods via `@Tool` annotation for AI agent consumption. Register new services in `McpContrastApplication.tools()` bean.
2. **SDK Extension Pattern**:
   - Use `SDKExtension` to add new Contrast API endpoints not in the standard SDK
   - Use `SDKHelper` for common utility operations (hostname handling, etc.)
   - Enhanced data models in `sdkexstension/data/` provide AI-friendly JSON representations
3. **Hint Generation**: Rule-based system in `hints/` package provides contextual security guidance for vulnerability remediation
4. **Defensive Design**: All external API calls include proper resource management (try-with-resources), error handling, and logging
5. **Pagination Handling**: SDK extension methods handle pagination automatically (see `getLibraryObservations` for pattern)

### Security Considerations

This codebase handles sensitive vulnerability data. The README contains critical warnings about data privacy when using with AI models. Never expose Contrast credentials or vulnerability data to untrusted AI services.

### Logging

- Default log location: `/tmp/mcp-contrast.log`
- Debug logging: Add `--logging.level.root=DEBUG` to startup arguments
- Console logging is minimal by design for MCP protocol compatibility
- Debug mode buffers API responses for logging (memory impact with large datasets)

### Troubleshooting

For common issues (SSL certificates, proxy configuration, debug logging), see the "Common Issues" and "Proxy Configuration" sections in [README.md](README.md).

### Adding New MCP Tools

To add a new tool/service:
1. Create a new `@Service` class with methods annotated with `@Tool(description="...")`
2. Inject dependencies (ContrastSDK, SDKExtension) via constructor
3. Register the service in `McpContrastApplication.tools()` bean method
4. Use `SDKExtension` to add new API endpoints if needed
5. Create enhanced data models in appropriate `sdkexstension/data/` subpackage
