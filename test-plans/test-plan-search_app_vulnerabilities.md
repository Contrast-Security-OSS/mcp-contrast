# Test Plan: search_app_vulnerabilities Tool

## Overview

This test plan provides comprehensive testing guidance for the `search_app_vulnerabilities` MCP tool. This tool provides application-scoped vulnerability search with all standard filters plus session-based filtering capabilities.

### Migration Notes

**This plan consolidates and replaces:**
- `test-plan-list_vulnerabilities.md` - App-scoped listing with session metadata

The new tool adds:
- All filters from `search_vulnerabilities` (severity, status, vulnTypes, environments, dates, tags)
- Session filtering (latestSession, metadata name/value)
- Consistent pagination model

### Tool Signature

**MCP Tool Name:** `search_app_vulnerabilities`

**Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `appId` | String | **Yes** | - | Application ID (use search_applications to find) |
| `page` | Integer | No | 1 | Page number (1-based) |
| `pageSize` | Integer | No | 50 | Items per page (max 100) |
| `severities` | String | No | null | Comma-separated: CRITICAL,HIGH,MEDIUM,LOW,NOTE |
| `statuses` | String | No | smart defaults | Comma-separated: Reported,Suspicious,Confirmed,Remediated,Fixed |
| `vulnTypes` | String | No | null | Comma-separated vulnerability types |
| `environments` | String | No | null | Comma-separated: DEVELOPMENT,QA,PRODUCTION |
| `lastSeenAfter` | String | No | null | ISO date or epoch timestamp |
| `lastSeenBefore` | String | No | null | ISO date or epoch timestamp |
| `vulnTags` | String | No | null | Comma-separated vulnerability tags |
| `sessionMetadataName` | String | No | null | Session metadata field name (case-insensitive) |
| `sessionMetadataValue` | String | No | null | Session metadata field value (requires name) |
| `useLatestSession` | Boolean | No | false | Filter to latest session only |

### Response Structure

**Returns:** `PaginatedToolResponse<VulnLight>`

```java
PaginatedToolResponse {
    List<VulnLight> items,       // Vulnerabilities for current page
    int page,                     // Current page number
    int pageSize,                 // Items per page
    Integer totalItems,           // Total matching items (may be null in session filtering mode)
    boolean hasMorePages,         // Whether more pages exist
    String message                // Warnings or info messages
}

VulnLight {
    String title,                 // Human-readable vulnerability name
    String type,                  // Vulnerability type
    String vulnID,                // Unique identifier (UUID)
    String severity,              // CRITICAL, HIGH, MEDIUM, LOW, NOTE
    String status,                // Reported, Suspicious, Confirmed, Remediated, Fixed
    List<SessionMetadata> sessionMetadata,  // Session context
    String lastSeenAt,            // ISO-8601 timestamp
    String firstSeenAt,           // ISO-8601 timestamp (nullable)
    String closedAt,              // ISO-8601 timestamp (nullable)
    List<String> environments,    // Historical environments
    ApplicationInfo application   // Application name and ID
}

SessionMetadata {
    String sessionId,             // Agent session ID
    List<MetadataItem> metadata   // Name-value pairs
}

MetadataItem {
    String displayLabel,          // Field name (e.g., "branch", "buildNumber")
    String value                  // Field value (e.g., "main", "1.2.3")
}
```

### Critical Behavioral Notes

| Behavior | Description |
|----------|-------------|
| **appId Required** | Tool fails without appId parameter |
| **Session Filtering Mode** | When `useLatestSession` or `sessionMetadataName` specified, uses in-memory filtering |
| **No Sessions Warning** | If `useLatestSession=true` and no sessions exist, returns all with warning |
| **Truncation Warning** | Large result sets may be truncated (max 50,000 traces or 100 pages) |
| **Partial Results** | API errors during multi-page fetch return partial results with warning |
| **Case Sensitivity** | Session metadata name/value matching is **case-INSENSITIVE** |

---

## 1. Required Parameter Tests

### Test Case 1.1: Missing appId Parameter (HARD FAILURE)

**Objective:** Verify tool fails without appId.

**Test Steps:**
1. Call `search_app_vulnerabilities` with no parameters
2. Call with only `severities` parameter

**Expected Results:**
- **HARD FAILURE:** Returns error response
- Error in `message`: "appId is required. Use search_applications to find app IDs by name."
- Empty `items` array

---

### Test Case 1.2: Valid appId - Basic Query

**Objective:** Verify basic query with valid appId.

**Prerequisites:**
- Application ID from `search_applications` tool

**Test Steps:**
1. Call with `appId="valid-app-uuid"`

**Expected Results:**
- Returns vulnerabilities for that application
- All items have matching `application.appId`
- Smart defaults warning (status filter not specified)

---

### Test Case 1.3: Invalid appId Format

**Objective:** Test with malformed application ID.

**Test Steps:**
1. Call with `appId="not-a-uuid"`
2. Call with `appId="12345"`
3. Call with `appId=""`
4. Call with `appId="  "` (whitespace)

**Expected Results:**
- **HARD FAILURE:** Returns error response
- Error in `message` indicates invalid application ID
- Empty `items` array

---

### Test Case 1.4: Non-Existent appId (Valid UUID Format)

**Objective:** Test with UUID that doesn't exist.

**Test Steps:**
1. Call with `appId="00000000-0000-0000-0000-000000000000"`

**Expected Results:**
- Returns empty results or error (depending on SDK behavior)
- Empty `items` array
- May have message about application not found

---

## 2. All Standard Filter Tests

This tool supports all the same filters as `search_vulnerabilities`. Test each filter in the app-scoped context:

### Test Case 2.1: Severity Filter with appId

**Objective:** Test severity filtering is scoped to application.

**Test Steps:**
1. Call with `appId="xyz", severities="CRITICAL,HIGH"`

**Expected Results:**
- Returns only CRITICAL/HIGH vulnerabilities from that application
- All items have matching severity
- All items have matching appId

---

### Test Case 2.2: Status Filter with appId

**Objective:** Test status filtering is scoped to application.

**Test Steps:**
1. Call with `appId="xyz", statuses="Reported,Confirmed"`

**Expected Results:**
- Returns only Reported/Confirmed vulnerabilities from that application
- No smart defaults warning (explicit status)

---

### Test Case 2.3: Vulnerability Type Filter with appId

**Objective:** Test vuln type filtering is scoped to application.

**Test Steps:**
1. Call with `appId="xyz", vulnTypes="sql-injection"`

**Expected Results:**
- Returns only sql-injection vulnerabilities from that application
- All items have `type: "sql-injection"`

---

### Test Case 2.4: Environment Filter with appId

**Objective:** Test environment filtering is scoped to application.

**Test Steps:**
1. Call with `appId="xyz", environments="PRODUCTION"`

**Expected Results:**
- Returns vulnerabilities seen in PRODUCTION for that application
- All items' `environments` include "PRODUCTION"

---

### Test Case 2.5: Date Filters with appId

**Objective:** Test date filtering is scoped to application.

**Test Steps:**
1. Call with `appId="xyz", lastSeenAfter="2025-01-01"`
2. Call with `appId="xyz", lastSeenBefore="2025-12-31"`
3. Call with `appId="xyz", lastSeenAfter="2025-01-01", lastSeenBefore="2025-12-31"`

**Expected Results:**
- Date filtering works correctly within application scope
- Warning about last activity date

---

### Test Case 2.6: Tag Filter with appId

**Objective:** Test tag filtering is scoped to application.

**Test Steps:**
1. Call with `appId="xyz", vulnTags="reviewed"`

**Expected Results:**
- Returns only tagged vulnerabilities from that application
- Correct vulnerabilities returned

---

### Test Case 2.7: Pagination with appId

**Objective:** Test pagination works within application scope.

**Test Steps:**
1. Call with `appId="xyz", page=1, pageSize=10`
2. Call with `appId="xyz", page=2, pageSize=10`
3. Compare results

**Expected Results:**
- Pagination returns different items per page
- All items belong to the same application
- `hasMorePages` correctly indicates more data

---

### Test Case 2.8: Combined Filters with appId

**Objective:** Test multiple filters combined.

**Test Steps:**
1. Call with:
   - `appId="xyz"`
   - `severities="CRITICAL,HIGH"`
   - `statuses="Reported"`
   - `environments="PRODUCTION"`
   - `lastSeenAfter="2025-06-01"`

**Expected Results:**
- All filters applied within application scope
- Results match all criteria

---

## 3. Session Metadata Filtering Tests

### Test Case 3.1: Filter by Metadata Name Only

**Objective:** Filter vulnerabilities that have a specific metadata field.

**Prerequisites:**
- Application with session metadata (use `get_session_metadata` to discover fields)

**Test Steps:**
1. Call with `appId="xyz", sessionMetadataName="branch"`

**Expected Results:**
- Returns vulnerabilities that have "branch" metadata field
- All items have sessionMetadata entries with "branch" field (any value)
- Case-insensitive matching (Branch, BRANCH, branch all work)

---

### Test Case 3.2: Filter by Metadata Name and Value

**Objective:** Filter vulnerabilities by specific metadata name-value pair.

**Test Steps:**
1. Call with `appId="xyz", sessionMetadataName="branch", sessionMetadataValue="main"`

**Expected Results:**
- Returns vulnerabilities where branch="main"
- Case-insensitive for both name and value
- "Main", "main", "MAIN" all match

---

### Test Case 3.3: Case-Insensitive Metadata Name Matching (CRITICAL)

**Objective:** Verify metadata name matching is case-insensitive.

**Test Steps:**
1. Call with `sessionMetadataName="branch"` (lowercase)
2. Call with `sessionMetadataName="Branch"` (title case)
3. Call with `sessionMetadataName="BRANCH"` (uppercase)
4. Call with `sessionMetadataName="bRaNcH"` (mixed case)

**Expected Results:**
- All variations return the same results
- Case-insensitive matching confirmed

---

### Test Case 3.4: Case-Insensitive Metadata Value Matching (CRITICAL)

**Objective:** Verify metadata value matching is case-insensitive.

**Test Steps:**
1. Call with `sessionMetadataValue="main"` (lowercase)
2. Call with `sessionMetadataValue="Main"` (title case)
3. Call with `sessionMetadataValue="MAIN"` (uppercase)

**Expected Results:**
- All variations return the same results
- Case-insensitive matching confirmed

---

### Test Case 3.5: Metadata Value Without Name (Validation)

**Objective:** Test specifying value without name.

**Test Steps:**
1. Call with `appId="xyz", sessionMetadataValue="main"` (no name)

**Expected Results:**
- Value should be ignored without name
- No error, but warning may appear
- Returns all vulnerabilities (no session filtering)

---

### Test Case 3.6: Non-Existent Metadata Field

**Objective:** Test filtering by metadata field that doesn't exist.

**Test Steps:**
1. Call with `sessionMetadataName="nonexistent_field"`

**Expected Results:**
- Returns empty results
- Empty `items` array
- No error (just no matches)

---

### Test Case 3.7: Non-Existent Metadata Value

**Objective:** Test filtering by value that doesn't exist.

**Test Steps:**
1. Call with `sessionMetadataName="branch", sessionMetadataValue="nonexistent-branch"`

**Expected Results:**
- Returns empty results
- Empty `items` array
- No error (just no matches)

---

### Test Case 3.8: Common Metadata Fields - Build Number

**Objective:** Test filtering by buildNumber metadata.

**Test Steps:**
1. Call with `sessionMetadataName="buildNumber", sessionMetadataValue="1.2.3"`

**Expected Results:**
- Returns vulnerabilities from that specific build
- All items have matching buildNumber metadata

---

### Test Case 3.9: Common Metadata Fields - Version

**Objective:** Test filtering by version metadata.

**Test Steps:**
1. Call with `sessionMetadataName="version", sessionMetadataValue="2.0.0"`

**Expected Results:**
- Returns vulnerabilities from that version
- All items have matching version metadata

---

### Test Case 3.10: Session Metadata with Standard Filters

**Objective:** Combine session metadata with other filters.

**Test Steps:**
1. Call with:
   - `appId="xyz"`
   - `sessionMetadataName="branch", sessionMetadataValue="main"`
   - `severities="CRITICAL,HIGH"`
   - `statuses="Reported"`

**Expected Results:**
- All filters applied together
- Returns CRITICAL/HIGH Reported vulnerabilities from branch=main sessions
- All criteria met by each result

---

## 4. Latest Session Filtering Tests

### Test Case 4.1: useLatestSession=true - Basic

**Objective:** Filter to vulnerabilities from most recent session.

**Prerequisites:**
- Application with multiple sessions

**Test Steps:**
1. Call with `appId="xyz", useLatestSession=true`

**Expected Results:**
- Returns vulnerabilities from the latest session only
- All items have sessionMetadata matching the latest session ID
- Warning may indicate session ID used

---

### Test Case 4.2: useLatestSession=true - No Sessions Exist (Warning)

**Objective:** Test behavior when application has no sessions.

**Prerequisites:**
- Application with no sessions (new app or sessions not configured)

**Test Steps:**
1. Call with `appId="xyz", useLatestSession=true`

**Expected Results:**
- **WARNING:** "No sessions found for this application. Returning all vulnerabilities..."
- Returns all vulnerabilities for the application (falls back to no session filter)
- Warning in `message` field

---

### Test Case 4.3: useLatestSession=false (Default)

**Objective:** Verify default behavior without latest session filter.

**Test Steps:**
1. Call with `appId="xyz", useLatestSession=false`
2. Call with `appId="xyz"` (no useLatestSession parameter)

**Expected Results:**
- Both return all vulnerabilities across all sessions
- No session filtering applied
- Results should be identical

---

### Test Case 4.4: useLatestSession with Other Filters

**Objective:** Combine latest session with other filters.

**Test Steps:**
1. Call with:
   - `appId="xyz"`
   - `useLatestSession=true`
   - `severities="CRITICAL"`

**Expected Results:**
- Returns CRITICAL vulnerabilities from latest session only
- Both filters applied
- Results match both criteria

---

### Test Case 4.5: useLatestSession with sessionMetadataName (Combined)

**Objective:** Test combining session filters.

**Test Steps:**
1. Call with:
   - `appId="xyz"`
   - `useLatestSession=true`
   - `sessionMetadataName="branch", sessionMetadataValue="main"`

**Expected Results:**
- Both session filters applied
- Returns vulnerabilities from latest session AND matching branch=main
- Results match all criteria

---

## 5. Large Data Set / Truncation Tests

### Test Case 5.1: Truncation Warning for Large Results

**Objective:** Verify truncation warning appears for large result sets.

**Prerequisites:**
- Application with 50,000+ vulnerabilities (or adjust test limits)

**Test Steps:**
1. Call with `appId="large-app", useLatestSession=true` (triggers session filtering mode)

**Expected Results:**
- Warning in `message`: "Results were truncated due to limits..."
- Warning suggests narrowing search with filters
- Partial results returned

---

### Test Case 5.2: API Error During Multi-Page Fetch

**Objective:** Verify partial results returned on API error.

**Prerequisites:**
- Test environment that can simulate API failures

**Test Steps:**
1. Trigger API error during session filtering multi-page fetch

**Expected Results:**
- Warning in `message`: "Partial data returned due to API error..."
- Returns successfully fetched results before error
- No crash or exception thrown

---

### Test Case 5.3: Session Filtering Performance

**Objective:** Measure response time with session filtering.

**Prerequisites:**
- Application with moderate number of vulnerabilities (1000+)

**Test Steps:**
1. Call with `useLatestSession=true`
2. Call with `sessionMetadataName="branch", sessionMetadataValue="main"`
3. Measure response times

**Expected Results:**
- Response completes in reasonable time (< 30 seconds)
- No timeouts
- Results returned successfully

---

### Test Case 5.4: Early Termination Optimization

**Objective:** Verify early termination when enough results found.

**Test Steps:**
1. Call with `useLatestSession=true, page=1, pageSize=10`
2. Monitor logs for early termination message

**Expected Results:**
- Tool stops fetching when enough results found for current page
- Performance better than fetching all data
- Log shows "Early termination" if triggered

---

## 6. Empty Results Tests

### Test Case 6.1: Application with No Vulnerabilities

**Objective:** Handle application with zero vulnerabilities.

**Prerequisites:**
- Application with no vulnerabilities

**Test Steps:**
1. Call with `appId="no-vuln-app"`

**Expected Results:**
- Returns empty `items` array
- `totalItems: 0`
- `hasMorePages: false`
- No errors

---

### Test Case 6.2: Filters Exclude All Application Vulnerabilities

**Objective:** Test when filters match nothing in the application.

**Test Steps:**
1. Call with `appId="xyz", severities="CRITICAL"` (when app has no critical)
2. Call with `appId="xyz", vulnTypes="non-existent-type"`

**Expected Results:**
- Returns empty `items` array
- `totalItems: 0`
- No errors

---

### Test Case 6.3: Session Filter Excludes All

**Objective:** Test when session filter matches nothing.

**Test Steps:**
1. Call with `appId="xyz", sessionMetadataName="branch", sessionMetadataValue="nonexistent"`

**Expected Results:**
- Returns empty `items` array
- `totalItems: 0`
- No errors

---

## 7. Response Validation Tests

### Test Case 7.1: Response Structure Validation

**Objective:** Verify response structure with session filtering.

**Test Steps:**
1. Call with `appId="xyz", useLatestSession=true`

**Expected Results:**
- Response has all required fields
- `items`: array of VulnLight objects
- `page`, `pageSize`: integers
- `totalItems`: may be integer or null in session filtering mode
- `hasMorePages`: boolean
- `message`: string with any warnings

---

### Test Case 7.2: SessionMetadata Field Structure

**Objective:** Verify session metadata is correctly populated.

**Test Steps:**
1. Call with `appId="xyz"` (no session filtering)
2. Examine `sessionMetadata` in results

**Expected Results:**
- `sessionMetadata` is array (may be empty)
- Each entry has `sessionId` (string)
- Each entry has `metadata` array of MetadataItem
- MetadataItem has `displayLabel` and `value`

---

### Test Case 7.3: Application Info in Results

**Objective:** Verify application context is included.

**Test Steps:**
1. Call with `appId="xyz"`
2. Check each item's application field

**Expected Results:**
- All items have `application.appId` matching input appId
- All items have `application.appName`
- Consistent application info across all results

---

### Test Case 7.4: hasMorePages with Session Filtering

**Objective:** Verify hasMorePages is correct in session filtering mode.

**Test Steps:**
1. Call with `appId="xyz", useLatestSession=true, page=1, pageSize=10`
2. Note hasMorePages value
3. Request subsequent pages until hasMorePages is false

**Expected Results:**
- `hasMorePages` correctly indicates more data
- Last page has `hasMorePages: false`
- Pagination works correctly in session filtering mode

---

## 8. Error Handling Tests

### Test Case 8.1: All Standard Filter Validation Errors

**Objective:** Verify validation errors work with appId.

**Test Steps:**
1. Invalid severity: `appId="xyz", severities="INVALID"`
2. Invalid status: `appId="xyz", statuses="Closed"`
3. Invalid environment: `appId="xyz", environments="STAGING"`
4. Invalid date: `appId="xyz", lastSeenAfter="invalid"`
5. Invalid date range: `appId="xyz", lastSeenAfter="2025-12-31", lastSeenBefore="2025-01-01"`

**Expected Results:**
- Each returns **HARD FAILURE**
- Error in `message` with details
- Empty `items` array

---

### Test Case 8.2: Pagination Clamping with appId

**Objective:** Verify pagination clamping works with appId.

**Test Steps:**
1. `appId="xyz", page=0`
2. `appId="xyz", page=-1`
3. `appId="xyz", pageSize=0`
4. `appId="xyz", pageSize=101`

**Expected Results:**
- Invalid values clamped with warnings
- Results returned successfully
- Warning in `message` about clamping

---

### Test Case 8.3: API Timeout Handling

**Objective:** Test handling of slow API responses.

**Prerequisites:**
- Test environment with network delays or slow API

**Test Steps:**
1. Configure network delays or slow API response
2. Call `search_app_vulnerabilities(appId="xyz")`

**Expected Results:**
- Returns within reasonable timeout
- May return partial data with warnings in session filtering mode
- No hang or crash
- User-friendly error message if timeout occurs

---

### Test Case 8.4: Authentication Error

**Objective:** Test handling of authentication failures.

**Prerequisites:**
- Invalid Contrast credentials configured

**Test Steps:**
1. Configure invalid API credentials
2. Call `search_app_vulnerabilities(appId="xyz")`

**Expected Results:**
- Returns error response
- Error message indicates authentication failure
- No sensitive data exposed in error
- `items` array is empty

---

### Test Case 8.5: Partial Data Response

**Objective:** Test handling when API returns incomplete data.

**Prerequisites:**
- Session filtering mode enabled (triggers multi-page fetch)

**Test Steps:**
1. Call with `useLatestSession=true` in environment where API fails mid-fetch
2. Or simulate network interruption during multi-page retrieval

**Expected Results:**
- Warning in `message`: "Partial data returned due to API error..."
- Returns successfully fetched results before error
- No crash or exception thrown
- `hasMorePages` may be false even if more data exists

---

## 9. Discovery Integration Tests

### Test Case 9.1: Discover Session Metadata Fields

**Objective:** Verify integration with get_session_metadata tool.

**Test Steps:**
1. Call `get_session_metadata(appId="xyz")` to discover available fields
2. Use discovered field names with `search_app_vulnerabilities`

**Expected Results:**
- Field names from `get_session_metadata` work with this tool
- Consistent naming between tools
- Integration works correctly

---

### Test Case 9.2: Workflow: Find App, Search Vulns, Filter by Session

**Objective:** Test complete workflow.

**Test Steps:**
1. `search_applications(name="WebGoat")` - get appId
2. `get_session_metadata(appId=<from step 1>)` - discover fields
3. `search_app_vulnerabilities(appId=<from step 1>, sessionMetadataName="branch", sessionMetadataValue="main")`

**Expected Results:**
- End-to-end workflow works
- Data flows between tools correctly
- Session filtering works with discovered fields

---

## Test Execution Guidelines

### Pre-Test Setup
1. Verify MCP server is running with valid Contrast credentials
2. Identify test applications with:
   - Known vulnerabilities (varying severities, statuses, types)
   - Session metadata configured (branch, buildNumber, version)
   - Multiple sessions for latest session testing
3. Use `search_applications` to get valid appIds
4. Use `get_session_metadata` to discover available metadata fields

### Test Data Recommendations
- Application with 100+ vulnerabilities
- Application with session metadata (at least 2 fields: branch, buildNumber)
- Application with multiple sessions (at least 3)
- Application with no vulnerabilities (for empty result testing)
- Application with no sessions (for fallback testing)

### Success Criteria
Each test passes when:
1. Response structure matches expected format
2. Filtering works correctly within application scope
3. Session filtering returns expected results
4. Warnings appear for edge cases
5. No unexpected exceptions or errors

---

## Test Coverage Summary

| Category | Test Cases | Coverage |
|----------|------------|----------|
| Required Parameter | 4 | appId validation |
| Standard Filters | 8 | All filters from search_vulnerabilities |
| Session Metadata | 10 | Name/value filtering, case sensitivity |
| Latest Session | 5 | useLatestSession behavior |
| Large Data/Truncation | 4 | Limits, errors, performance |
| Empty Results | 3 | No data scenarios |
| Response Validation | 4 | Structure, metadata fields |
| Error Handling | 5 | Validation, clamping, timeout, auth, partial data |
| Discovery Integration | 2 | Workflow with other tools |

**Total: 45 test cases**

---

## References

- **Tool Implementation**: `tool/assess/SearchAppVulnerabilitiesTool.java`
- **Related Tools**: `search_vulnerabilities`, `search_applications`, `get_session_metadata`, `get_vulnerability`
- **Old Test Plan**: `test-plan-list_vulnerabilities.md`
