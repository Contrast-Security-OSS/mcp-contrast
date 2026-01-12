# Test Plan: get_route_coverage Tool

## Overview

This test plan provides comprehensive testing guidance for the `get_route_coverage` MCP tool. This tool retrieves route coverage data for an application with optional session filtering.

### Migration Notes

**This plan replaces:**
- `test-plan-get_route_coverage.md` (original at root level)

**Key Changes from Original Tool:**
- **Follows tool-per-class pattern**: Uses `SingleTool<RouteCoverageParams, RouteCoverageResponse>`
- **Enhanced validation**: Uses `RouteCoverageParams` with paired parameter validation
- **Clearer mutual exclusivity**: `useLatestSession` and session metadata filters documented as mutually exclusive
- **Improved null handling**: Graceful handling of null API responses

### Tool Signature

**MCP Tool Name:** `get_route_coverage`

**Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `appId` | String | Yes | Application ID (use search_applications to find) |
| `sessionMetadataName` | String | No | Session metadata field name (e.g., 'branch'). Must be paired with sessionMetadataValue |
| `sessionMetadataValue` | String | No | Session metadata field value (e.g., 'main'). Must be paired with sessionMetadataName |
| `useLatestSession` | Boolean | No | If true, filter to latest session only. Mutually exclusive with session metadata filter |

**Validation Rules:**
- `appId` is required
- `sessionMetadataName` and `sessionMetadataValue` must be provided together or both omitted
- If both `useLatestSession` and metadata params provided, `useLatestSession` takes precedence (warning issued)

### Response Structure

**Returns:** `SingleToolResponse<RouteCoverageResponse>`

```java
SingleToolResponse {
    RouteCoverageResponse data,  // Route coverage data
    String message,              // Warnings or info messages
    boolean found                // True if data returned
}

// RouteCoverageResponse structure:
RouteCoverageResponse {
    List<Route> routes,   // List of discovered/exercised routes
    boolean success       // API success indicator
}

// Route structure:
Route {
    String signature,           // Route signature (HTTP method + path)
    String routeHash,           // Unique route identifier
    int exercised,              // Number of times exercised (0 = DISCOVERED, >0 = EXERCISED)
    RouteDetailsResponse routeDetailsResponse  // Additional route details
}
```

### Critical Behavioral Notes

| Behavior | Description |
|----------|-------------|
| **Route Status** | `exercised > 0` = EXERCISED, `exercised == 0` = DISCOVERED |
| **N+1 API Pattern** | Tool fetches details for each route (may be slow for many routes) |
| **Mutual Exclusivity** | `useLatestSession=true` overrides session metadata filter |
| **Null Response** | Returns null if no routes or session not found |

---

## 1. Basic Functionality Tests

### Test Case 1.1: Unfiltered Query - All Routes

**Objective:** Verify tool returns all route coverage data without any filtering.

**Prerequisites:**
- Application with recorded routes (both discovered and exercised)

**Test Steps:**
1. Call `search_applications` to get an application with routes
2. Note the `appId`
3. Call `get_route_coverage(appId="<app-id>")`

**Expected Results:**
- Response status: success
- `data.routes` contains list of routes
- Routes include both DISCOVERED and EXERCISED statuses
- Each route has `routeDetailsResponse` populated
- `found: true`

---

### Test Case 1.2: Route Structure Verification

**Objective:** Verify each route has complete data structure.

**Test Steps:**
1. Call `get_route_coverage` with valid appId
2. Examine route structure

**Expected Results:**
- Each route contains:
  - `signature`: HTTP method + path (e.g., "GET /api/users")
  - `routeHash`: Non-null unique identifier
  - `exercised`: Integer (0 or positive)
  - `routeDetailsResponse`: Non-null with additional details
- `routeDetailsResponse.success` is true

---

### Test Case 1.3: Route Status Verification

**Objective:** Verify routes are correctly categorized as DISCOVERED or EXERCISED.

**Test Steps:**
1. Call `get_route_coverage` with valid appId
2. Iterate through routes
3. Check `route.exercised` value

**Expected Results:**
- Routes with `exercised > 0` are EXERCISED (received HTTP requests)
- Routes with `exercised == 0` are DISCOVERED (found but not exercised)
- Both types may be present in response

---

## 2. Session Metadata Filter Tests

### Test Case 2.1: Filter by Session Metadata

**Objective:** Verify filtering by session metadata name/value pair.

**Prerequisites:**
- Application with session metadata (e.g., branch=main)

**Test Steps:**
1. Call `get_session_metadata` to find available metadata fields
2. Note a field with values (e.g., `branchName` with value `main`)
3. Call `get_route_coverage(appId="<app-id>", sessionMetadataName="branchName", sessionMetadataValue="main")`

**Expected Results:**
- Returns only routes from sessions matching metadata filter
- Filtered count <= unfiltered count
- All routes have route details populated
- `found: true`

---

### Test Case 2.2: Missing sessionMetadataValue

**Objective:** Verify validation error when only name is provided.

**Test Steps:**
1. Call `get_route_coverage(appId="<app-id>", sessionMetadataName="branch")`
   (without sessionMetadataValue)

**Expected Results:**
- Validation error returned
- Error message: "sessionMetadataValue is required when sessionMetadataName is provided"
- No API call made

---

### Test Case 2.3: Missing sessionMetadataName

**Objective:** Verify validation error when only value is provided.

**Test Steps:**
1. Call `get_route_coverage(appId="<app-id>", sessionMetadataValue="main")`
   (without sessionMetadataName)

**Expected Results:**
- Validation error returned
- Error message: "sessionMetadataName is required when sessionMetadataValue is provided"
- No API call made

---

### Test Case 2.4: Non-Existent Metadata Field

**Objective:** Verify behavior with non-existent metadata field.

**Test Steps:**
1. Call `get_route_coverage` with non-existent metadata field:
   `sessionMetadataName="nonexistent", sessionMetadataValue="value"`

**Expected Results:**
- Either empty results or error
- Graceful handling (no crash)
- Clear indication of issue

---

## 3. Latest Session Filter Tests

### Test Case 3.1: Filter by Latest Session

**Objective:** Verify filtering to latest session only.

**Test Steps:**
1. Call `get_route_coverage(appId="<app-id>", useLatestSession=true)`
2. Compare with unfiltered results

**Expected Results:**
- Returns routes from most recent session only
- Route count likely smaller than unfiltered
- Success if session exists
- `found: true`

---

### Test Case 3.2: No Session Metadata Exists

**Objective:** Verify behavior when application has no sessions.

**Prerequisites:**
- Application with no session metadata

**Test Steps:**
1. Call `get_route_coverage(appId="<app-no-sessions>", useLatestSession=true)`

**Expected Results:**
- Response succeeds but data is null
- `found` may be false or true with null data
- No exception thrown
- Clear indication no sessions found

---

### Test Case 3.3: Mutual Exclusivity Warning

**Objective:** Verify warning when both filters provided.

**Test Steps:**
1. Call `get_route_coverage(appId="<app-id>", sessionMetadataName="branch", sessionMetadataValue="main", useLatestSession=true)`

**Expected Results:**
- Request succeeds
- Warning in response message about mutual exclusivity
- `useLatestSession` takes precedence
- Results are from latest session (not metadata filtered)

---

## 4. Validation Tests

### Test Case 4.1: Missing appId Parameter

**Objective:** Verify validation error for missing required parameter.

**Test Steps:**
1. Call `get_route_coverage` without appId

**Expected Results:**
- Validation error returned
- Error message: "appId is required"
- No API call made

---

### Test Case 4.2: Invalid appId

**Objective:** Verify behavior with non-existent application ID.

**Test Steps:**
1. Call `get_route_coverage(appId="invalid-app-id-12345")`

**Expected Results:**
- Either error response or empty results
- Graceful handling
- Clear error message

---

### Test Case 4.3: Whitespace in appId

**Objective:** Verify handling of whitespace in parameter.

**Test Steps:**
1. Call `get_route_coverage(appId=" abc123 ")` (with spaces)
2. Call `get_route_coverage(appId="   ")` (only whitespace)

**Expected Results:**
- Whitespace-only: validation error
- Leading/trailing spaces: either trimmed or error
- Consistent behavior

---

### Test Case 4.4: useLatestSession = false

**Objective:** Verify false is treated same as not provided.

**Test Steps:**
1. Call `get_route_coverage(appId="<app-id>", useLatestSession=false)`
2. Compare with unfiltered query

**Expected Results:**
- Same results as unfiltered query
- `false` treated as null (no filter)

---

## 5. Empty/Edge Case Tests

### Test Case 5.1: Application with No Routes

**Objective:** Verify behavior when application has no routes.

**Prerequisites:**
- Application that exists but has no discovered routes

**Test Steps:**
1. Call `get_route_coverage(appId="<app-no-routes>")`

**Expected Results:**
- Response succeeds
- `data.routes` is empty list (not null)
- `found: true`
- No error thrown

---

### Test Case 5.2: Application with Many Routes

**Objective:** Verify performance with large number of routes.

**Prerequisites:**
- Application with 100+ routes

**Test Steps:**
1. Call `get_route_coverage` for application with many routes
2. Measure response time

**Expected Results:**
- All routes returned
- Response time acceptable (< 30 seconds)
- Route details populated for all routes
- No timeout

**Performance Note:**
- N+1 API pattern means each route requires separate details call
- 100 routes = ~100 additional API calls

---

## 6. Integration Tests

### Test Case 6.1: Workflow with search_applications

**Objective:** Verify typical workflow of finding app then getting coverage.

**Test Steps:**
1. Call `search_applications(name="<known-app-name>")`
2. Extract `appId` from results
3. Call `get_route_coverage(appId="<extracted-id>")`

**Expected Results:**
- Both calls succeed
- Routes returned for correct application
- IDs match between calls

---

### Test Case 6.2: Workflow with get_session_metadata

**Objective:** Verify session metadata can be used for filtering.

**Test Steps:**
1. Call `get_session_metadata(appId="<app-id>")`
2. Note available metadata fields and values
3. Use those values in `get_route_coverage` filter

**Expected Results:**
- Metadata fields work as filter names
- Filter produces subset of all routes
- No errors with valid metadata

---

### Test Case 6.3: Filter Comparison

**Objective:** Compare results from different filter types.

**Test Steps:**
1. Get unfiltered route coverage (all routes)
2. Get session metadata filtered coverage
3. Get latest session coverage
4. Compare counts

**Expected Results:**
- Unfiltered count >= session metadata filtered count
- Unfiltered count >= latest session count
- Filtered results are subsets of unfiltered

---

## 7. Error Handling Tests

### Test Case 7.1: API Connection Failure

**Objective:** Verify graceful handling when API unavailable.

**Prerequisites:**
- Simulate connection failure

**Test Steps:**
1. Configure invalid credentials or hostname
2. Call `get_route_coverage`

**Expected Results:**
- Error response returned
- Error message indicates connection issue
- No crash or hang

---

### Test Case 7.2: Timeout Handling

**Objective:** Verify behavior with slow responses.

**Test Steps:**
1. Test with large application (many routes)
2. Monitor for timeout

**Expected Results:**
- Returns within reasonable timeout
- Either success or error (no hang)

---

### Test Case 7.3: Repeated Calls Consistency

**Objective:** Verify consistent results across calls.

**Test Steps:**
1. Call `get_route_coverage` 3 times with same appId
2. Compare results

**Expected Results:**
- All 3 calls return same data
- No variation (unless new routes discovered)
- Consistent performance

---

## 8. Performance Tests

### Test Case 8.1: Response Time Benchmarks

**Objective:** Measure acceptable performance.

**Test Steps:**
1. Call tool and measure response time
2. Test with various route counts
3. Calculate averages

**Expected Results:**
- Small app (< 20 routes): < 5 seconds
- Medium app (20-50 routes): < 15 seconds
- Large app (100+ routes): < 30 seconds

---

## Test Execution Guidelines

### Pre-Test Setup
1. Verify MCP server running with valid credentials
2. Have at least one application with routes
3. Know an appId with session metadata for filter testing
4. Have applications with varying route counts

### Workflow for Finding Test Data
```
1. search_applications() → get list of apps
2. Pick app with recent activity
3. Note appId for testing
4. Call get_session_metadata(appId) to see available filters
5. Call get_route_coverage with various filter combinations
```

### Success Criteria
The `get_route_coverage` tool passes testing if:
- Unfiltered query succeeds (TC 1.1-1.3)
- Session metadata filtering works (TC 2.1)
- Latest session filtering works (TC 3.1)
- Validation catches invalid input (TC 4.1-4.4)
- Edge cases handled gracefully (TC 5.1)
- Integration workflows succeed (TC 6.1-6.3)
- Error handling is graceful (TC 7.1-7.3)
- Performance is acceptable

---

## Test Coverage Summary

| Category | Test Cases | Coverage |
|----------|------------|----------|
| Basic Functionality | 3 | Core behavior, structure |
| Session Metadata Filter | 4 | Paired params, validation |
| Latest Session Filter | 3 | Latest, no sessions, mutual exclusivity |
| Validation | 4 | Required params, edge cases |
| Empty/Edge Cases | 2 | No routes, many routes |
| Integration | 3 | Workflow with other tools |
| Error Handling | 3 | Connection, timeout, consistency |
| Performance | 1 | Response time |

**Total: 23 test cases**

---

## Appendix: Expected Behavior Examples

### Successful Response Example (Unfiltered)
```json
{
  "data": {
    "routes": [
      {
        "signature": "GET /api/users",
        "routeHash": "abc123...",
        "exercised": 15,
        "routeDetailsResponse": {
          "success": true,
          ...
        }
      },
      {
        "signature": "POST /api/orders",
        "routeHash": "def456...",
        "exercised": 0,
        "routeDetailsResponse": {
          "success": true,
          ...
        }
      }
    ],
    "success": true
  },
  "message": null,
  "found": true
}
```

### Mutual Exclusivity Warning
```json
{
  "data": {...},
  "message": "Both useLatestSession and sessionMetadataName provided - useLatestSession takes precedence and sessionMetadata filter will be ignored",
  "found": true
}
```

### No Routes Response
```json
{
  "data": {
    "routes": [],
    "success": true
  },
  "message": null,
  "found": true
}
```

### Validation Error Response
```json
{
  "data": null,
  "message": "Validation failed: sessionMetadataValue is required when sessionMetadataName is provided",
  "found": false
}
```

---

## Logging Verification

Check MCP server logs (`/tmp/mcp-contrast.log`) for:
- "Fetching route coverage data for application ID: {appId}"
- Filter-specific logs:
  - "Filtering by session metadata: {}={}"
  - "Using latest session ID: {}"
  - "No filters applied - retrieving all route coverage"
- "Found {} routes for application"
- "Successfully retrieved route coverage for application ID: {} ({} routes)"

---

## References

- **Tool Implementation**: `tool/coverage/GetRouteCoverageTool.java`
- **Params Class**: `tool/coverage/params/RouteCoverageParams.java`
- **Related Tools**: `search_applications`, `get_session_metadata`
- **Old Test Plan**: `test-plan-get_route_coverage.md` (root level)
