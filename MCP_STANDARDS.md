# MCP Tool Naming Standards

**Version:** 1.0
**JIRA:** AIML-238
**Created:** 2025-11-18

---

## Core Convention: `action_entity`

Tool names (in `@Tool` annotation) use `action_entity` snake_case format.

**Format:**
- Action verb: `search`, `list`, or `get`
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
- ❌ `get_protect_rules(appId)` - Should be `list_application_rules(appId)`

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

## Checklist

- [ ] `action_entity` snake_case format
- [ ] Verb matches capability (search/list/get)
- [ ] Entity clear and unabbreviated
- [ ] Parameters camelCase and consistent
- [ ] Return type follows standards
- [ ] @Tool description clear and concise
- [ ] Required vs optional documented
- [ ] No redundant words

