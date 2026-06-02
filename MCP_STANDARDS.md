# MCP Tool Naming Standards

**Version:** 1.0
**JIRA:** AIML-238
**Created:** 2025-11-18

---

## Core Convention: `action_entity`

Tool names (in `@Tool` annotation) use `action_entity` snake_case format.

**Format:**
- Action verb: `search`, `list`, `get`, or `update`
- Entity: what's operated on
- Separator: single underscore
- Casing: lowercase throughout

**Examples:**
- ✅ `search_vulnerabilities`, `get_vulnerability`, `list_application_libraries`
- ❌ `list_Scan_Project`, `get_ADR_Protect_Rules`, `listApplications`

**Limits:**
- 64 character max
- No redundant words ("all", "data")
- Abbreviate only when widely known (cve, id)

---

## Verb Hierarchy

### `search_*` - Flexible Filtering
- Multiple optional filters
- Paginated results
- Returns items matching filter combinations
- Use when: "find all X where..."

**Example:** `search_vulnerabilities` with optional appId, severities, statuses, etc.

### `list_*` - Scoped Lists
- Returns all items in a scope
- Requires scope identifier (appId, projectName)
- Minimal filtering
- Use when: "show all X for Y"

**Example:** `list_application_libraries(appId)` - all libs for one app

### `get_*` - Single Item
- Fetches one item by identifier
- Required identifier(s)
- Returns single object
- Throws if not found
- Use when: "get details of X"

**Example:** `get_vulnerability(vulnId, appId)` - one specific vuln

### `update_*` - Mutate Existing Item
- Mutates state of one identified item
- Required identifier(s) (which item) and required mutation parameter (what to change)
- Returns the updated item or operation result
- Side effect: state change — must never appear under `get_*`
- Use when: "change X to state Y"

**Example:** `update_issue_status(issueId, status)` — change one issue's status.

> Scope note: `update_*` is the only mutation verb currently in the standard. `create_*` and `delete_*` are intentionally not yet defined — add them when a tool actually requires that semantic.

---

## Exceptions and Special Cases

### Analytical and Aggregate Data

Tools returning analytical data (reports, coverage, metadata) may use `get_*` even when returning collections, if the data represents a singular conceptual entity.

**Criteria for exception:**
- Data represents a report/analysis/coverage for a single entity (e.g., one app)
- Primary semantic is "getting THE [singular concept]" not "listing items"
- Filter parameters refine the analysis scope, not search for matching items

**Approved exceptions:**
- ✅ `get_route_coverage(appId, ...)` - Gets THE route coverage report for an app
- ✅ `get_session_metadata(appId)` - Gets THE metadata for a session
- ✅ `get_scan_results(projectName)` - Gets THE scan results for a project

**Counter-examples (should use list_* or search_*):**
- ❌ `get_applications_by_tag(tag)` - Should be `search_applications(tag=...)`

**Legacy exception:**
- `get_protect_rules(appId)` is grandfathered for AIML-110 local/remote parity. Do not rename it as part of hosted MCP; handle any future alias or rename as separate compatibility work.

---

## Parameters

### Naming: camelCase
- ✅ `appId`, `vulnId`, `sessionMetadataName`
- ❌ `app_id`, `session_Metadata_Name`

### Identifier Suffixes
- `*Id` - UUID/numeric: `appId`, `vulnId`, `attackId`
- `*Name` - string: `projectName`, `metadataName`
- Never: `*ID` (caps) or `*_id` (snake_case)

### Standard Names

| Parameter | Usage |
|-----------|-------|
| `appId` | Application identifier |
| `vulnId` | Vulnerability identifier |
| `cveId` | CVE identifier |
| `sessionMetadataName/Value` | Session metadata |
| `page` / `pageSize` | Pagination (1-based) |
| `useLatestSession` | Latest session flag |

### Filter Conventions
- **Plural** for comma-separated: `severities`, `statuses`, `environments`
- **Singular** for single values: `appId`, `keyword`, `sort`

### Required vs Optional
- `@NonNull` - required
- `@Nullable` - optional
- Document dependencies: "sessionMetadataValue (required if sessionMetadataName provided)"


---

## Tool-per-Class Architecture

All MCP tools follow a **one-class-per-tool** pattern with shared base classes. Each tool is a standalone `@Service` class that extends `PaginatedTool` for offset/page-backed search/list operations, `CursorPaginatedTool` for cursor/keyset-backed list operations, or `SingleTool` for single-item retrieval.

### Package Structure

```
com.contrast.labs.ai.mcp.contrast.tool/
├── base/                  # Shared base classes (BaseTool, PaginatedTool, CursorPaginatedTool, SingleTool, ToolParams)
├── validation/            # Shared validation utilities (ToolValidationContext)
├── vulnerability/         # Vulnerability tools
│   ├── SearchVulnerabilitiesTool.java
│   ├── GetVulnerabilityTool.java
│   └── params/            # Parameter classes for vulnerability tools
├── application/           # Application tools
├── library/               # Library (SCA) tools
├── attack/                # Attack detection tools
├── sast/                  # SAST scan tools
└── coverage/              # Route coverage tools
```

### Base Class Usage

**`PaginatedTool<P extends ToolParams, R>`** - For paginated search/list tools:
- Template method `executePipeline()` handles pagination, validation, exceptions
- Subclasses implement `doExecute()` returning `ExecutionResult<R>`
- Returns `PaginatedToolResponse<R>` with items, pagination metadata, errors, warnings

**`SingleTool<P extends ToolParams, R>`** - For single-item get tools:
- Template method `executePipeline()` handles validation, exceptions
- Subclasses implement `doExecute()` returning item or null
- Returns `SingleToolResponse<R>` with item, errors, warnings

**`CursorPaginatedTool<P extends ToolParams, R>`** - For cursor/keyset-backed list tools:
- Template method `executePipeline()` handles cursor pagination, validation, exceptions
- Subclasses treat cursor values as opaque continuation tokens
- Returns `CursorToolResponse<R>` with items, `nextCursor`, `hasMore`, errors, and warnings

### Parameter Classes (Params Pattern)

Each tool has an associated `*Params` class extending `ToolValidationContext`:
- Validates and parses input parameters
- Collects errors and warnings via fluent API
- Converts to SDK filter objects (e.g., `toTraceFilterForm()`)

Example:
```java
// In tool class
return executePipeline(page, pageSize,
    () -> VulnerabilityFilterParams.of(severities, statuses, ...));

// Params class
public class VulnerabilityFilterParams extends ToolValidationContext {
  public static VulnerabilityFilterParams of(String severities, ...) {
    var params = new VulnerabilityFilterParams();
    params.severities = params.enumSetParam(severities, RuleSeverity.class, "severities").get();
    // ... more fluent validation
    return params;
  }
}
```

### Testing Layer

Each tool requires corresponding test classes:
- `*ToolTest.java` - Unit tests with mocked SDK
- `*ToolIT.java` - Integration tests against live API (in `src/test/java` with `IT` suffix)

### Adding a New Tool

1. Create tool class in appropriate domain package (e.g., `tool/vulnerability/`)
2. Extend `PaginatedTool`, `CursorPaginatedTool`, or `SingleTool` with appropriate type parameters
3. Create corresponding `*Params` class extending `ToolValidationContext`
4. Implement `doExecute()` with tool-specific logic
5. Add `@Tool` annotation with snake_case name following naming standards
6. **Register the tool explicitly in the appropriate application** (see Tool Registration below)
7. Write unit and integration tests

---

## Tool Registration

Tools are registered with Spring AI MCP through explicit wiring. Spring component scanning may discover `@Service` classes, but they must be explicitly added to the tools bean for the application that exposes them.

### Registration Mechanism

1. **Component Scanning**: Spring discovers `@Service` tool classes in `com.contrast.labs.ai.mcp.contrast.tool.*`
2. **Injection**: the application-specific `tools()` bean receives each tool as a constructor parameter
3. **Callback Conversion**: `ToolCallbacks.from(...)` converts tool objects to `ToolCallback` instances
4. **MCP Exposure**: Spring AI MCP Server starter exposes these callbacks to AI agents

### Registering a New Tool

For the local stdio app, after creating a tool class, add it to `McpContrastApplication.java`:

```java
@Bean
public List<ToolCallback> tools(
    // ... existing tools ...
    MyNewTool myNewTool) {  // 1. Add as parameter
  return of(
      ToolCallbacks.from(
          // ... existing tools ...
          myNewTool));       // 2. Add to ToolCallbacks.from()
}
```

**Important**: Both steps are required. The tool won't be exposed to AI agents if only the `@Service` annotation is present.

For any other application that consumes `contrast-mcp-core`, register tools through an explicit `ToolCallback` list owned by that application. Do not rely on component scanning alone, and review the generated `tools/list` surface before exposing a tool set to users.

---

## Checklist

- [ ] `action_entity` snake_case format
- [ ] Verb matches capability (search/list/get)
- [ ] Entity clear and unabbreviated
- [ ] Parameters camelCase and consistent
- [ ] Return type follows standards
- [ ] @Tool description clear and concise
- [ ] Required vs optional documented
- [ ] No redundant words
- [ ] Extends `PaginatedTool`, `CursorPaginatedTool`, or `SingleTool`
- [ ] Has corresponding Params class
- [ ] Registered in the appropriate explicit `tools()` bean
- [ ] Unit and integration tests present
