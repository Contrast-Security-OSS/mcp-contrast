# MCP Tool Standards

**Version:** 2.0
**JIRA:** AIML-238 (naming, v1), AIML-942 (descriptions, envelope, enforcement, v2)
**Created:** 2025-11-18
**Updated:** 2026-07-31

Scope note. This document covers the tools in this repository. Other applications that consume `contrast-mcp-core` may layer additional standards of their own. Content specific to any private deployment or internal infrastructure is rejected from this document by policy.

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

### Sort Convention
- Use a single optional `sort` string parameter when a tool exposes caller-controlled ordering.
- Public MCP sort syntax is `property,DIRECTION`.
- Valid directions are `ASC` and `DESC`.
- Validate the property with a tool-specific allowlist before calling downstream services.
- Translate to backend-specific sort syntax inside the tool/client adapter. Do not expose backend-specific conventions such as `-property`, `sortBy` + `sortDirection`, or raw cursor sort internals as the MCP-facing contract.

**Example:** `sort="startTime,DESC"`

### Required vs Optional
- `@NonNull` - required
- `@Nullable` - optional
- Document dependencies: "sessionMetadataValue (required if sessionMetadataName provided)"


---

## Tool Descriptions

A tool description is read by an agent, not a human. It is also the most expensive prose in the interface. It ships in `tools/list` for every session whether or not the tool is used. The standard is structural, not judgment-based. Every fact about a tool has exactly one home, chosen by the moment the agent needs it.

### One home per fact

| When the agent needs it | Fact type | Home |
|---|---|---|
| Choosing a tool | Purpose, use-when/don't-use against overlapping siblings, prerequisite pointers | Description body |
| Building the call | Formats, valid value sets, filter semantics, defaults, mutual exclusivity | `@ToolParam` description |
| Reading the result, conditionally | A quirk that applies only when a condition occurs on this response | A notice in the response, emitted only when the condition fires |
| Reading the result, always | The standing meaning of a field | A self-describing field name; fallback, one clause in the body |
| Always, for every tool | A convention uniformly true across the whole exposed tool set | Server instructions, gated by the three-bar test below |

Two rules make the table binding.

**Earliest-moment tiebreaker.** A fact needed at more than one moment goes in the earliest home where it is needed, and only there. "Use score, not severity, when sorting by risk" shapes the `sort` argument, so it lives on the `sort` parameter even though it sounds like result interpretation.

**Duplication ban.** A fact stated in a parameter description must not repeat in the body. A
discovery pointer carried by the description body is never repeated in a parameter description;
parameter-level discovery cross-references apply only when the body does not carry the pointer.
Cross-checking body against params is part of review.

### Templates and budgets

Each verb shape has a fixed template. Every slot except the lead is optional. `ToolDescriptionBudgetTest` enforces the ceiling by counting words in the body prose. `@ToolParam` text is not counted.

**Search and list tools, 150 words or fewer.**

1. Lead sentence. What it searches and its scope.
2. Use-when/don't-use against siblings, only where genuine overlap exists in the exposed tool set.
3. Prerequisite pointer ("Use search_applications to find application IDs by name").
4. Call-shaping quirks that span more than one parameter. Mutual exclusivity between parameters is the canonical example ("sessionMetadataFilters and useLatestSession are mutually exclusive"). Single-parameter semantics belong on the parameter.
5. Up to three usage examples, preferring filter combinations over single filters. Simple tools get none.

**Get tools, 40 words or fewer.** Lead sentence plus a discovery pointer for the identifier when the
catalog has a discovery tool for it and the pointer would help ("Use search_vulnerabilities to find
vulnerability IDs"). Omit the pointer when no discovery tool exists or it would not help.
Interpretation quirks arrive as notices, not prose.

**Update tools, 60 words or fewer.** Lead sentence, allowed transitions when not carried by the parameter, and an audit or reversibility note.

**Escape hatch.** A description may exceed its budget only when the overage is use-when/don't-use disambiguation, and the overage must name the sibling it disambiguates against. The enforcement test records the exemption in a reviewed allowlist.

### Repository boundary

Tool descriptions in this public repository must refer only to tools exposed by `mcp-contrast`.
They must never name hosted-only tools from `aiml-services`, because `mcp-contrast` can be
deployed independently and those tools may not exist. The hosted `aiml-services` catalog may refer
to public `mcp-contrast` tools because it deliberately aggregates them. Cross-repo routing that
would otherwise require a reverse public-to-hosted reference belongs in the hosted server's
instructions once that channel clears its delivery gate.

### Transitional clauses

Interpretation prose stays in a description until the notice, rename, or output-schema property that replaces it ships, and it moves in the same change as the code that replaces it. Transitional clauses count against the budget and are tied to the change that removes them, so shipping the replacement is how a tool reclaims its headroom.

### Never in a description

- A parameter the tool does not accept. The validation error teaches the rare misuse.
- A field-by-field response inventory. Self-evident fields (title, ids, timestamps) carry nothing anywhere.
- A "Related tools" footer that restates what siblings do. The agent holds every tool's description already. Keep the routing decision inline, in the use-when slot.
- A fact already on a parameter (duplication ban).

### Deprecation

A deprecated tool announces it three ways. The description starts with `DEPRECATED:` naming the reason and the replacement, `doExecute()` emits a notice on every call, and the Java `@Deprecated` annotation covers human readers. When the reason is output size, say so concretely.

---

## Parameter Descriptions

The `@ToolParam` description is the single home for call-construction facts.

1. Name what the parameter filters or selects, in the parameter's own terms.
2. List the valid value set once, here and nowhere else ("Comma-separated severities: CRITICAL,HIGH,MEDIUM,LOW,NOTE"). Validation enforces it on a miss.
3. State the format only when non-obvious (dates accepting ISO-8601 or epoch milliseconds, a JSON object shape).
4. Cross-reference the discovery tool when the value comes from another tool ("use search_applications to find").
5. Clarify semantics when the name misleads ("lastSeenAfter filters on last activity, not discovery date") or when two parameters look interchangeable ("keyword is substring match across many fields, rules is exact match on rule IDs").

Required-ness goes on the `@ToolParam(required=...)` flag, never in prose.

---

## Response Envelope, Errors, and Notices

Every tool response wraps in a shared envelope with exactly two message tiers. The distinction is behavioral, an agent acts differently on each.

- **`errors`** mean the call failed or was invalid. Fix the call and retry. `isSuccess()` keys off this list.
- **`notices`** are informational. Read them while interpreting the result. They carry applied defaults, failed optional enrichments, empty-result explanations, and interpretation facts.

Do not add a third tier. A distinction that does not change agent behavior is taxonomy, not signal.

### Notice wording

State the fact affirmatively. Never phrase a teaching notice as a failure.

- Good. "severity and score omitted means unscored, not low risk."
- Good. "totalItems is available on the first page only."
- Bad. "Warning: missing severity."

Degradation notices keep the established suffix pattern ("route coverage not available (retrieval error, HTTP 403)") so agents can tell transient absence from legitimate absence.

### Point-of-use teaching

Interpretation facts are delivered where the confusion would occur, not up front.

**Conditional quirks** become notices emitted only when the condition occurs on that response. An always-emitted notice is a smell, it belongs in another home.

**Always-true field meaning** is carried by the field name itself. Prefer renaming a misleading field (`hint` containing AI-generated remediation guidance becomes `remediationHint`) over documenting it. A rename costs zero recurring tokens. When a rename is not feasible, the fallback is one clause in the description body, tagged transitional if a rename is planned.

### Output schema, end state

When the stack gains `outputSchema` support, always-true field facts that a rename cannot carry move onto the schema property's `description`. Notices are unaffected, conditional facts stay point-of-use. The schema then joins the same budget discipline, property descriptions only where the name cannot carry the meaning, no auto-generated full-graph schemas, adoption per tool and justified. Nothing load-bearing moves exclusively to the schema until the consuming clients are confirmed to surface it.

---

## Server Instructions

The MCP `instructions` string is read once near session start, far from any call site. A fact may move there only when it clears all three bars.

1. **Delivered.** A captured `initialize` response from the running server proves the field is emitted, and the consuming clients are confirmed to surface it.
2. **Recalled.** The agent plausibly still holds the fact when it builds a call. Mechanical call-shaping facts (1-based pages) fail this bar and stay on the tool.
3. **Uniformly true.** The fact holds for every exposed tool. The moment it varies by tool, globalizing it makes it false for some.

---

## Enforcement

The standard is enforced by `ToolDescriptionBudgetTest`.

- **Budget checks.** The test counts body words per description against the template ceilings. The word count is the plain whitespace-delimited count of the description body, parameter descriptions excluded. Exceeding a ceiling fails the build unless the tool has a named allowlist entry, which is itself a reviewed artifact.

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
├── server/                # Server inventory and agent health tools
├── sast/                  # SAST scan tools
└── coverage/              # Route coverage tools
```

### Base Class Usage

**`PaginatedTool<P extends ToolParams, R>`** - For paginated search/list tools:
- Template method `executePipeline()` handles pagination, validation, exceptions
- Subclasses implement `doExecute()` returning `ExecutionResult<R>`
- Returns `PaginatedToolResponse<R>` with items, pagination metadata, errors, notices

**`SingleTool<P extends ToolParams, R>`** - For single-item get tools:
- Template method `executePipeline()` handles validation, exceptions
- Subclasses implement `doExecute()` returning item or null
- Returns `SingleToolResponse<R>` with item, errors, notices

**`CursorPaginatedTool<P extends ToolParams, R>`** - For cursor/keyset-backed list tools:
- Template method `executePipeline()` handles cursor pagination, validation, exceptions
- Subclasses treat cursor values as opaque continuation tokens
- Returns `CursorToolResponse<R>` with items, `nextCursor`, `hasMore`, errors, and notices

### Parameter Classes (Params Pattern)

Each tool has an associated `*Params` class extending `BaseToolParams` and validating input through a `ToolValidationContext` used by composition:
- Validates and parses input parameters
- Collects errors and notices via fluent API
- Converts to SDK filter objects (e.g., `toTraceFilterForm()`)

Example:
```java
// In tool class
return executePipeline(page, pageSize,
    () -> VulnerabilityFilterParams.of(severities, statuses, ...));

// Params class
public class VulnerabilityFilterParams extends BaseToolParams {
  public static VulnerabilityFilterParams of(String severities, ...) {
    var params = new VulnerabilityFilterParams();
    var ctx = new ToolValidationContext(); // composition, not inheritance
    params.severities = ctx.enumSetParam(severities, RuleSeverity.class, "severities").get();
    // ... more fluent validation on ctx
    params.setValidationResult(ctx); // transfer errors/notices
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
3. Create corresponding `*Params` class extending `BaseToolParams`
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

**Naming and shape**
- [ ] `action_entity` snake_case format
- [ ] Verb matches capability (search/list/get/update)
- [ ] Entity clear and unabbreviated
- [ ] Parameters camelCase and consistent
- [ ] Return type follows standards
- [ ] No redundant words
- [ ] Extends `PaginatedTool`, `CursorPaginatedTool`, or `SingleTool`
- [ ] Has corresponding Params class
- [ ] Registered in the appropriate explicit `tools()` bean
- [ ] Unit and integration tests present

**Description**
- [ ] Body follows the verb-shape template and is within budget (search/list ≤150 words, get ≤40, update ≤60), or carries a named allowlist exemption for sibling disambiguation
- [ ] Every fact sits in its one home per the routing table, no body/param duplication
- [ ] Prerequisite and discovery pointers inline, no "Related tools" footer
- [ ] References only tools exposed by `mcp-contrast`, never hosted-only `aiml-services` tools
- [ ] At most three usage examples, combinations preferred
- [ ] No documented parameter the tool does not accept, no response-field inventory
- [ ] Transitional clauses tagged and tied to the change that removes them
- [ ] Deprecated tools start with `DEPRECATED:`, name the replacement, and notice on every call

**Parameters**
- [ ] Valid value set listed once on the parameter, nowhere else
- [ ] Required-ness on the `@ToolParam(required=...)` flag, not prose
- [ ] Format noted only when non-obvious, discovery cross-references present
- [ ] Misleading names and confusable parameter pairs clarified on the parameter

**Envelope**
- [ ] Two tiers only, errors actionable, notices informational
- [ ] Notices worded affirmatively, never as failure
- [ ] Conditional interpretation facts emitted as notices at point of use
- [ ] Misleading response field names renamed rather than documented, when feasible

**Enforcement**
- [ ] Body word count verified against the template ceiling, or the exemption is in the reviewed allowlist
