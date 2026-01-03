# Test Plan: get_route_coverage

## Overview
Test plan for the consolidated `get_route_coverage` MCP tool that retrieves route coverage data for an application with optional filtering.

**Tool Name:** `mcp__contrast__get_route_coverage` (34 characters - under Claude API 64-char limit)

**Replaces:** 6 previous methods consolidated as part of AIML-224:
- `get_application_route_coverage`
- `get_application_route_coverage_by_app_id`
- `get_application_route_coverage_by_app_name_and_session_metadata`
- `get_application_route_coverage_by_app_id_and_session_metadata`
- `get_application_route_coverage_by_app_name_latest_session`
- `get_application_route_coverage_by_app_id_latest_session`

## Method Signature

```java
public RouteCoverageResponse getRouteCoverage(
    String appId,                    // Required - application ID
    String sessionMetadataName,      // Optional - session metadata filter name
    String sessionMetadataValue,     // Optional - session metadata filter value
    Boolean useLatestSession         // Optional - filter to latest session
) throws IOException
```

## Test Scenarios

### 1. Unfiltered Query (All Parameters Null/Not Provided)

**Description:** Retrieve all route coverage data for an application without any filtering.

**Parameters:**
- `appId`: Valid application ID
- `sessionMetadataName`: `null`
- `sessionMetadataValue`: `null`
- `useLatestSession`: `null` or `false`

**Expected Result:**
- Returns all routes for the application (both DISCOVERED and EXERCISED)
- Each route includes route details
- Success status is true

**Test Steps:**
1. Call `get_route_coverage` with only `appId` parameter
2. Verify response contains routes
3. Verify each route has `routeDetailsResponse` populated
4. Count DISCOVERED vs EXERCISED routes

**Verification:**
- Response.isSuccess() == true
- Response.getRoutes().size() > 0 (assuming app has routes)
- All routes have non-null route details

---

### 2. Session Metadata Filter

**Description:** Filter route coverage data by session metadata name/value pair (e.g., branch=main).

**Parameters:**
- `appId`: Valid application ID
- `sessionMetadataName`: "branch" (or other metadata field)
- `sessionMetadataValue`: "main" (or other metadata value)
- `useLatestSession`: `null`

**Expected Result:**
- Returns only routes from sessions matching the metadata filter
- Filtered results are a subset of unfiltered results
- Success status is true

**Test Steps:**
1. Call `get_route_coverage` with `appId`, `sessionMetadataName`, and `sessionMetadataValue`
2. Verify response contains filtered routes
3. Compare count with unfiltered query (should be <= unfiltered)
4. Verify all routes have route details

**Verification:**
- Response.isSuccess() == true
- Filtered route count <= Unfiltered route count
- All routes have non-null route details

**Error Case:**
- Providing `sessionMetadataName` without `sessionMetadataValue` should throw `IllegalArgumentException`

---

### 3. Latest Session Filter

**Description:** Retrieve route coverage data from only the latest application session.

**Parameters:**
- `appId`: Valid application ID
- `sessionMetadataName`: `null`
- `sessionMetadataValue`: `null`
- `useLatestSession`: `true`

**Expected Result:**
- Returns routes from the most recent session only
- If no session metadata exists, returns empty response with success=false
- Success status depends on session existence

**Test Steps:**
1. Call `get_route_coverage` with `appId` and `useLatestSession=true`
2. Check if session metadata exists for the application
3. If exists: verify routes are returned
4. If not exists: verify response.isSuccess() == false

**Verification:**
- If session exists:
  - Response.isSuccess() == true
  - Routes returned are from latest session
  - All routes have non-null route details
- If no session:
  - Response.isSuccess() == false
  - Response.getRoutes() may be null or empty

---

### 4. Invalid Application ID

**Description:** Test error handling with invalid/non-existent application ID.

**Parameters:**
- `appId`: "invalid-app-id-12345" (definitely doesn't exist)
- Other parameters: `null`

**Expected Result:**
- API either returns empty response or throws IOException
- Graceful error handling

**Test Steps:**
1. Call `get_route_coverage` with invalid `appId`
2. Catch any exceptions
3. Verify error is handled gracefully

**Verification:**
- Either IOException is thrown OR
- Empty response is returned (routes.size() == 0)

---

### 5. Parameter Validation

**Description:** Verify parameter validation rules are enforced.

**Test Cases:**

**5a. Missing sessionMetadataValue**
- `appId`: Valid ID
- `sessionMetadataName`: "branch"
- `sessionMetadataValue`: `null`
- Expected: `IllegalArgumentException` with message "sessionMetadataValue is required"

**5b. All null parameters**
- `appId`: Valid ID
- All other parameters: `null`
- Expected: Same as unfiltered query (test case #1)

**5c. useLatestSession = false**
- `appId`: Valid ID
- `useLatestSession`: `false`
- Expected: Same as unfiltered query (false is treated as null)

---

### 6. Route Status Verification

**Description:** Verify routes are correctly categorized as DISCOVERED or EXERCISED.

**Parameters:**
- `appId`: Valid application ID with known route states
- Other parameters: `null`

**Expected Result:**
- Routes with `exercised > 0` are EXERCISED
- Routes with `exercised == 0` are DISCOVERED

**Test Steps:**
1. Call `get_route_coverage` with `appId`
2. Iterate through routes
3. Check `route.getExercised()` value
4. Count DISCOVERED vs EXERCISED

**Verification:**
- Each route has either exercised > 0 or exercised == 0
- At least some routes exist (if application has activity)

---

### 7. Route Details Population

**Description:** Verify route details are fetched and attached to each route.

**Parameters:**
- `appId`: Valid application ID
- Other parameters: `null`

**Expected Result:**
- Every route has `routeDetailsResponse` populated
- Route details include additional information about the route

**Test Steps:**
1. Call `get_route_coverage` with `appId`
2. For each route in response:
   - Verify `route.getRouteDetailsResponse()` is not null
   - Verify `route.getRouteDetailsResponse().isSuccess()` is true

**Verification:**
- All routes have non-null `routeDetailsResponse`
- Route details indicate success

---

### 8. Filter Comparison

**Description:** Compare results from different filter types to ensure they make logical sense.

**Test Steps:**
1. Get unfiltered route coverage (all routes)
2. Get session metadata filtered route coverage
3. Get latest session route coverage
4. Compare counts

**Expected Relationships:**
- Unfiltered count >= Session metadata filtered count
- Unfiltered count >= Latest session count (if session exists)
- Filtered results should be subsets of unfiltered

**Verification:**
- Logical relationships between different filter results hold true
- No filtered query returns more results than unfiltered

---

## Integration Test Requirements

Run integration tests with real Contrast TeamServer:

```bash
# Set environment variables
export CONTRAST_HOST_NAME="your-contrast-host"
export CONTRAST_API_KEY="your-api-key"
export CONTRAST_SERVICE_KEY="your-service-key"
export CONTRAST_USERNAME="your-username"
export CONTRAST_ORG_ID="your-org-id"
export TEST_APP_ID="application-id-with-routes"
export TEST_METADATA_NAME="branch"
export TEST_METADATA_VALUE="main"

# Run integration tests
mvn verify
```

Integration tests are in: `RouteCoverageServiceIntegrationTest.java`

---

## Unit Test Coverage

Unit tests are in: `RouteCoverageServiceTest.java`

**Test Count:** 14 tests covering:
- Unfiltered queries (2 tests)
- Session metadata filtering (3 tests)
- Latest session filtering (3 tests)
- Error handling (3 tests)
- SDK configuration (1 test)
- Parameter combinations (2 tests)

**Mocking Strategy:**
- Mock `SDKExtension` for Contrast SDK calls
- Mock `SDKHelper` for SDK initialization
- Use reflection to set configuration fields

---

## Performance Considerations

- Each route requires an additional API call to fetch route details
- Large applications with many routes may take longer to process
- Consider pagination or limiting for applications with 100+ routes

---

## Success Criteria

- ✅ Tool name is under 64 characters (34 chars)
- ✅ All 6 previous use cases work with new consolidated method
- ✅ Unit tests pass (14 tests)
- ✅ Integration tests pass (5 tests)
- ✅ MCP server loads successfully with Claude API
- ✅ No validation errors from Claude API

---

## Migration Notes

**For AI Agents:**
- Old method calls need to be updated to use new signature
- App name lookups must be done separately (use `list_applications_with_name` tool)
- Parameter order: `appId, sessionMetadataName, sessionMetadataValue, useLatestSession`

**Example Migrations:**
```
Old: get_application_route_coverage_by_app_id(appId)
New: get_route_coverage(appId, null, null, null)

Old: get_application_route_coverage_by_app_id_and_session_metadata(appId, name, value)
New: get_route_coverage(appId, name, value, null)

Old: get_application_route_coverage_by_app_id_latest_session(appId)
New: get_route_coverage(appId, null, null, true)
```

---

## Tool Name Validation

**MCP Tool Name:** `mcp__contrast__get_route_coverage`
- Length: 34 characters
- Limit: 64 characters (Claude API requirement)
- Status: ✅ PASS (within limit)

**Previous Tool Names (now removed):**
- `mcp__contrast__get_application_route_coverage_by_app_id_and_session_metadata` (72 chars) ❌
- `mcp__contrast__get_application_route_coverage_by_app_name_and_session_metadata` (77 chars) ❌
- `mcp__contrast__get_application_route_coverage_by_app_id_latest_session` (71 chars) ❌
- `mcp__contrast__get_application_route_coverage_by_app_name_latest_session` (72 chars) ❌
