# Test Plan: get_session_metadata Tool

## Overview

This test plan provides comprehensive testing guidance for the `get_session_metadata` MCP tool. This tool retrieves session metadata for a specific application by its ID.

### Migration Notes

**This plan replaces:**
- `test-plan-list_session_metadata_for_application.md` (original at root level)

**Key Changes from Original Tool:**
- **Parameter renamed**: `app_name` → `appId` (now takes ID directly, not name)
- **Lookup removed**: No longer resolves application name internally
- **Tool renamed**: `list_session_metadata_for_application` → `get_session_metadata`
- **Workflow**: Use `search_applications(name=...)` first to find appId

### Tool Signature

**MCP Tool Name:** `get_session_metadata`

**Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `appId` | String | Yes | Application ID (use search_applications to find) |

### Response Structure

**Returns:** `SingleToolResponse<MetadataFilterResponse>`

```java
SingleToolResponse {
    MetadataFilterResponse data,  // Session metadata from Contrast SDK
    String message,               // Warnings or info messages
    boolean found                 // True if data returned
}

// MetadataFilterResponse structure (from SDK):
MetadataFilterResponse {
    List<MetadataEntity> filters  // List of metadata field definitions
}

// MetadataEntity structure:
MetadataEntity {
    String fieldName,      // Internal field name (e.g., "branchName")
    String label,          // Display label (e.g., "Branch Name")
    List<String> values    // Available values for this field
}
```

### Critical Behavioral Notes

| Behavior | Description |
|----------|-------------|
| **appId Required** | Must provide application ID, not name |
| **Latest Session** | Returns metadata from the most recent session |
| **Null Response** | Returns null with warning if no sessions exist |
| **SDK Delegation** | Passes through to `sdk.getSessionMetadataForApplication()` |
| **Use Case** | Find metadata fields available for filtering vulnerabilities |

---

## 1. Basic Functionality Tests

### Test Case 1.1: Retrieve Session Metadata - Success

**Objective:** Verify tool returns session metadata for a valid application.

**Prerequisites:**
- Application with recorded sessions and metadata

**Test Steps:**
1. Call `search_applications` to get an application with sessions
2. Note the `appId` from the response
3. Call `get_session_metadata(appId="<app-id>")`

**Expected Results:**
- Response status: success
- `data` contains MetadataFilterResponse
- `data.filters` is array of metadata entities
- `found: true`

---

### Test Case 1.2: Metadata Structure Verification

**Objective:** Verify returned metadata has correct structure.

**Test Steps:**
1. Call `get_session_metadata` with valid appId
2. Examine the response structure

**Expected Results:**
- Each metadata entity contains:
  - `fieldName`: non-empty string
  - `label`: human-readable label
  - `values`: array of possible values (may be empty)
- Common fields may include:
  - `branchName` / "Branch Name"
  - `buildNumber` / "Build Number"
  - `version` / "Version"
  - Custom metadata fields

---

### Test Case 1.3: Metadata Values Are Populated

**Objective:** Verify metadata values reflect actual session data.

**Prerequisites:**
- Application with diverse session metadata

**Test Steps:**
1. Call `get_session_metadata` for an app with known metadata
2. Check that `values` arrays contain actual data

**Expected Results:**
- At least one metadata field has non-empty `values` array
- Values represent actual data from sessions
- Values are strings (even if original was numeric)

---

## 2. Empty/Edge Case Tests

### Test Case 2.1: Application with No Sessions

**Objective:** Verify behavior when application has no recorded sessions.

**Prerequisites:**
- Application that exists but has no session data

**Test Steps:**
1. Find or create an application with no sessions
2. Call `get_session_metadata(appId="<app-with-no-sessions>")`

**Expected Results:**
- Response status: success (no exception)
- `data` is null
- `message` contains warning: "No session metadata found..."
- `found: false` or `found: true` with null data

---

### Test Case 2.2: Application with Sessions but No Metadata

**Objective:** Verify behavior when sessions exist but have no custom metadata.

**Test Steps:**
1. Find application with sessions but minimal/no custom metadata
2. Call `get_session_metadata`

**Expected Results:**
- Response succeeds
- May return empty `filters` array
- Or returns filters with empty `values` arrays
- No error thrown

---

## 3. Validation Tests

### Test Case 3.1: Missing appId Parameter

**Objective:** Verify validation error for missing required parameter.

**Test Steps:**
1. Call `get_session_metadata` without appId (or with empty string)

**Expected Results:**
- Validation error returned
- Error message: "appId is required" or similar
- No API call made

---

### Test Case 3.2: Invalid appId Format

**Objective:** Verify behavior with malformed application ID.

**Test Steps:**
1. Call `get_session_metadata(appId="invalid-format-12345")`
2. Call `get_session_metadata(appId="!!invalid!!")`

**Expected Results:**
- Either validation error or API error
- Clear error message
- No crash or hang

---

### Test Case 3.3: Nonexistent Application ID

**Objective:** Verify behavior when appId doesn't exist.

**Test Steps:**
1. Call `get_session_metadata(appId="00000000-0000-0000-0000-000000000000")`

**Expected Results:**
- Error response or null data
- Appropriate error message
- No crash

---

### Test Case 3.4: Whitespace in appId

**Objective:** Verify handling of whitespace in parameter.

**Test Steps:**
1. Call `get_session_metadata(appId=" abc123 ")` (with spaces)
2. Call `get_session_metadata(appId="   ")` (only whitespace)

**Expected Results:**
- Whitespace-only: validation error
- Leading/trailing spaces: either trimmed or error
- Consistent behavior

---

## 4. Integration Tests

### Test Case 4.1: Workflow with search_applications

**Objective:** Verify typical workflow of finding app then getting metadata.

**Test Steps:**
1. Call `search_applications(name="<known-app-name>")`
2. Extract `appId` from results
3. Call `get_session_metadata(appId="<extracted-id>")`

**Expected Results:**
- Both calls succeed
- Metadata returned for correct application
- IDs match between calls

---

### Test Case 4.2: Use Metadata with search_app_vulnerabilities

**Objective:** Verify metadata fields can filter vulnerabilities.

**Test Steps:**
1. Call `get_session_metadata` and note a field with values
2. Call `search_app_vulnerabilities` with session filter using that metadata
3. Example: `search_app_vulnerabilities(appId="...", sessionMetadataName="branchName", sessionMetadataValue="main")`

**Expected Results:**
- Session filter works without error
- Returned vulnerabilities filtered by metadata
- Field names from metadata are valid filter names

---

### Test Case 4.3: Use with get_route_coverage

**Objective:** Verify metadata can be used for route coverage filtering.

**Test Steps:**
1. Get session metadata for an application
2. Use metadata values with `get_route_coverage` session filters

**Expected Results:**
- Route coverage filtered by session metadata
- Consistent behavior with vulnerability filtering

---

## 5. Response Structure Tests

### Test Case 5.1: Response Schema Validation

**Objective:** Verify response conforms to SingleToolResponse structure.

**Test Steps:**
1. Call `get_session_metadata` with valid appId
2. Examine response fields

**Expected Results:**
- Response has `data`, `message`, `found` fields
- `data` is MetadataFilterResponse or null
- `message` is string or null
- `found` is boolean

---

### Test Case 5.2: Warning Messages

**Objective:** Verify warning messages are informative.

**Test Steps:**
1. Test scenarios that generate warnings:
   - No session metadata
   - Empty metadata

**Expected Results:**
- Warnings are clear and actionable
- Warnings don't indicate errors (just info)
- Help user understand the data state

---

## 6. Error Handling Tests

### Test Case 6.1: API Connection Failure

**Objective:** Verify graceful handling when API unavailable.

**Prerequisites:**
- Simulate connection failure (invalid credentials, network issue)

**Test Steps:**
1. Configure invalid credentials
2. Call `get_session_metadata`

**Expected Results:**
- Error response returned
- Error message indicates connection issue
- No crash or hang

---

### Test Case 6.2: Timeout Handling

**Objective:** Verify behavior with slow responses.

**Test Steps:**
1. Test with network delays if possible
2. Call `get_session_metadata`

**Expected Results:**
- Returns within reasonable timeout
- Either success or error (no hang)

---

### Test Case 6.3: Repeated Calls Consistency

**Objective:** Verify consistent results across calls.

**Test Steps:**
1. Call `get_session_metadata` 3 times with same appId
2. Compare results

**Expected Results:**
- All 3 calls return same data
- No variation (unless new sessions created)
- Consistent performance

---

## 7. Performance Tests

### Test Case 7.1: Response Time Benchmarks

**Objective:** Measure acceptable performance.

**Test Steps:**
1. Call tool and measure response time
2. Repeat 5 times
3. Calculate average

**Expected Results:**
- Normal case: < 3 seconds
- Large metadata: < 5 seconds
- Consistent across calls

---

## Test Execution Guidelines

### Pre-Test Setup
1. Verify MCP server running with valid credentials
2. Have at least one application with session metadata
3. Know an appId with sessions for testing

### Workflow for Finding Test Data
```
1. search_applications() → get list of apps
2. Pick app with recent activity
3. Note appId for testing
4. Call get_session_metadata(appId)
```

### Success Criteria
The `get_session_metadata` tool passes testing if:
- Basic functionality test passes (TC 1.1)
- Metadata structure is correct (TC 1.2)
- Empty cases handled gracefully (TC 2.1)
- Validation catches invalid input (TC 3.1-3.4)
- Integration with other tools works (TC 4.1-4.2)
- Error handling is graceful (TC 6.1)
- Performance is acceptable (< 5 seconds)

---

## Test Coverage Summary

| Category | Test Cases | Coverage |
|----------|------------|----------|
| Basic Functionality | 3 | Core behavior verification |
| Empty/Edge Cases | 2 | No data scenarios |
| Validation | 4 | Parameter validation |
| Integration | 3 | Workflow with other tools |
| Response Structure | 2 | Schema validation |
| Error Handling | 3 | Failures, timeouts |
| Performance | 1 | Response time |

**Total: 18 test cases**

---

## Appendix: Expected Behavior Examples

### Successful Response Example
```json
{
  "data": {
    "filters": [
      {
        "fieldName": "branchName",
        "label": "Branch Name",
        "values": ["main", "develop", "feature/auth"]
      },
      {
        "fieldName": "buildNumber",
        "label": "Build Number",
        "values": ["1234", "1235", "1236"]
      },
      {
        "fieldName": "version",
        "label": "Version",
        "values": ["1.0.0", "1.1.0"]
      }
    ]
  },
  "message": null,
  "found": true
}
```

### No Sessions Response
```json
{
  "data": null,
  "message": "No session metadata found for this application. This may indicate the application has no recorded sessions.",
  "found": true
}
```

### Error Response Example
```json
{
  "data": null,
  "message": "Validation failed: appId is required",
  "found": false
}
```

---

## Logging Verification

Check MCP server logs (`/tmp/mcp-contrast.log`) for:
- Request received with appId
- SDK call to `getSessionMetadataForApplication`
- Response processing
- No error logs in success case

---

## References

- **Tool Implementation**: `tool/application/GetSessionMetadataTool.java`
- **Params Class**: `tool/application/params/GetSessionMetadataParams.java`
- **Related Tools**: `search_applications`, `search_app_vulnerabilities`, `get_route_coverage`
- **Old Test Plan**: `test-plan-list_session_metadata_for_application.md` (root level)
