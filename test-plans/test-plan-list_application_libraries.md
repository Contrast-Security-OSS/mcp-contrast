# Test Plan: list_application_libraries Tool

## Overview

This test plan provides comprehensive testing guidance for the `list_application_libraries` MCP tool. This tool retrieves all third-party libraries used by a specific application, including CVE vulnerability data and class usage metrics.

### Migration Notes

**This plan replaces:**
- `test-plan-list_application_libraries.md` (original at root level)

**Changes from Original:**
- **Tool name unchanged**: `list_application_libraries`
- **Parameter unchanged**: `appId` (was `appID`, now camelCase)
- **Architecture changed**: Now uses tool-per-class pattern with `PaginatedTool`
- **Pagination added**: Supports `page` and `pageSize` parameters
- **Pagination changed**: Now uses server-side pagination (API enforces max 50)
- **Workflow**: Use `search_applications(name=...)` first to find appId

**Future Note:** This tool may be deprecated when `search_libraries` is implemented.

### Tool Signature

**MCP Tool Name:** `list_application_libraries`

**Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `page` | Integer | No | Page number (1-based), default: 1 |
| `pageSize` | Integer | No | Items per page (max 50), default: 50 |
| `appId` | String | Yes | Application ID (use search_applications to find) |

### Response Structure

**Returns:** `PaginatedToolResponse<LibraryExtended>`

```java
PaginatedToolResponse {
    List<LibraryExtended> items,  // Libraries for current page
    int page,                     // Current page number (1-based)
    int pageSize,                 // Items per page
    Integer totalItems,           // Total library count
    boolean hasMorePages,         // True if more pages exist
    List<String> errors,          // Validation/execution errors
    List<String> warnings,        // Non-fatal warnings
    Long durationMs               // Execution time in ms
}

// LibraryExtended structure:
LibraryExtended {
    String fileName,              // Library file name (e.g., "log4j-core-2.17.1.jar")
    String version,               // Library version
    String hash,                  // Unique library hash
    long libraryId,               // Numeric library ID
    int classCount,               // Total classes in library
    int classesUsed,              // Classes actually loaded by app
    int totalVulnerabilities,     // Total CVE count
    int highVulnerabilities,      // CRITICAL + HIGH CVE count
    List<LibraryVulnerabilityExtended> vulns,  // CVE details
    String grade,                 // Security grade (A, B, C, D, F)
    double libScore,              // Numeric library score
    int monthsOutdated,           // Months since latest version
    String latestVersion,         // Latest available version
    String appId,                 // Application ID
    String app_name,              // Application name
    String appLanguage,           // Language (Java, .NET, Node.js, etc.)
    boolean custom,               // Is custom/internal library
    List<String> apps,            // Other apps using this library
    List<String> servers          // Servers where deployed
}
```

### Critical Behavioral Notes

| Behavior | Description |
|----------|-------------|
| **appId Required** | Must provide application ID, not name |
| **Class Usage** | `classesUsed=0` indicates library likely unused |
| **Vulnerability Data** | Includes CVE severity and counts |
| **Empty Response** | Returns empty list with warning if no libraries |
| **Transitive Deps** | Includes all transitive dependencies |

---

## 1. Basic Functionality Tests

### Test Case 1.1: Retrieve Libraries - Success

**Objective:** Verify tool returns libraries for a valid application.

**Prerequisites:**
- Application with third-party libraries

**Test Steps:**
1. Call `search_applications` to get an application with libraries
2. Note the `appId` from the response
3. Call `list_application_libraries(appId="<app-id>")`

**Expected Results:**
- Response status: success
- `data` is array of LibraryExtended objects
- Array contains 1+ libraries
- `found: true`

---

### Test Case 1.2: Library Data Completeness

**Objective:** Verify library objects have complete data.

**Test Steps:**
1. Call `list_application_libraries` with valid appId
2. Examine the first library

**Expected Results:**
- Each library contains:
  - `fileName`: non-empty (e.g., "spring-core-5.3.10.jar")
  - `hash`: unique identifier
  - `version`: version string
  - `classCount`: >= 0
  - `classesUsed`: >= 0 and <= classCount
  - `grade`: A, B, C, D, F, or null

---

### Test Case 1.3: Multiple Libraries Retrieval

**Objective:** Verify all libraries are returned.

**Test Steps:**
1. Call `list_application_libraries` for app with many libraries
2. Count libraries in response

**Expected Results:**
- All libraries returned (typical apps have 10-100+)
- Each library has unique hash
- Different library types (framework, utility, logging, etc.)

---

### Test Case 1.4: Library Version Information

**Objective:** Verify version and outdated data is populated.

**Test Steps:**
1. Call `list_application_libraries`
2. Examine version-related fields

**Expected Results:**
- `version`: current version in use
- `latestVersion`: latest available (if outdated)
- `monthsOutdated`: >= 0 (0 if current)
- Logical relationship: if `monthsOutdated > 0`, versions should differ

---

## 2. Class Usage Tests

### Test Case 2.1: Libraries with Zero Class Usage

**Objective:** Identify unused libraries (transitive dependencies).

**Test Steps:**
1. Call `list_application_libraries`
2. Find libraries where `classesUsed = 0`

**Expected Results:**
- Some libraries have `classesUsed = 0`
- These are "unused" (likely transitive dependencies)
- `classCount > 0` but `classesUsed = 0`
- Lower vulnerability risk (not actively loaded)

---

### Test Case 2.2: Libraries with Active Usage

**Objective:** Identify actively used libraries.

**Test Steps:**
1. Call `list_application_libraries`
2. Find libraries where `classesUsed > 0`

**Expected Results:**
- Some libraries have `classesUsed > 0`
- `classesUsed <= classCount` (always)
- Higher vulnerability risk if CVEs present

---

### Test Case 2.3: Class Usage Edge Cases

**Objective:** Verify class count logic.

**Test Steps:**
1. Call `list_application_libraries`
2. Check all libraries for class count consistency

**Expected Results:**
- `classesUsed <= classCount` for all libraries
- No negative values
- `classCount = 0` rare but possible (resource-only libs)

---

## 3. Vulnerability Tests

### Test Case 3.1: Libraries with CVE Vulnerabilities

**Objective:** Identify vulnerable libraries.

**Prerequisites:**
- Application with libraries that have known CVEs

**Test Steps:**
1. Call `list_application_libraries`
2. Find libraries with `totalVulnerabilities > 0`

**Expected Results:**
- `vulns` array is not empty
- `totalVulnerabilities` matches `vulns.size()`
- Each vulnerability has `severityCode`

---

### Test Case 3.2: Vulnerability Severity Distribution

**Objective:** Verify severity counts are accurate.

**Test Steps:**
1. Find library with vulnerabilities
2. Count HIGH/CRITICAL in `vulns` list
3. Compare to `highVulnerabilities` field

**Expected Results:**
- `highVulnerabilities` = count of CRITICAL + HIGH
- Severity codes: CRITICAL, HIGH, MEDIUM, LOW
- Counts are accurate

---

### Test Case 3.3: High-Risk Libraries

**Objective:** Identify highest priority for remediation.

**Test Steps:**
1. Find libraries where:
   - `classesUsed > 0` AND
   - `highVulnerabilities > 0`

**Expected Results:**
- These are highest risk (used + critical CVEs)
- Should be prioritized for upgrade
- Can calculate risk score from these fields

---

### Test Case 3.4: Low-Risk Vulnerable Libraries

**Objective:** Identify lower priority vulnerabilities.

**Test Steps:**
1. Find libraries where:
   - `classesUsed = 0` AND
   - `totalVulnerabilities > 0`

**Expected Results:**
- These are lower risk (not actively used)
- CVEs present but unexploitable
- Lower remediation priority

---

## 4. Empty/Edge Case Tests

### Test Case 4.1: Application with No Libraries

**Objective:** Verify behavior when no libraries exist.

**Prerequisites:**
- Application without third-party dependencies

**Test Steps:**
1. Find application with no libraries
2. Call `list_application_libraries(appId="<app-no-libs>")`

**Expected Results:**
- Response status: success
- `data` is empty array `[]`
- `message` contains warning about no libraries
- `found: true`

---

### Test Case 4.2: Newly Created Application

**Objective:** Verify behavior for new applications.

**Test Steps:**
1. Use appId of recently created application
2. Call `list_application_libraries`

**Expected Results:**
- Returns empty list or minimal data
- No errors
- Warning if no library data collected yet

---

## 5. Validation Tests

### Test Case 5.1: Missing appId Parameter

**Objective:** Verify validation error for missing parameter.

**Test Steps:**
1. Call `list_application_libraries` without appId

**Expected Results:**
- Validation error returned
- Error message: "appId is required"
- No API call made

---

### Test Case 5.2: Invalid appId Format

**Objective:** Verify behavior with malformed ID.

**Test Steps:**
1. Call `list_application_libraries(appId="invalid")`
2. Call `list_application_libraries(appId="!!bad!!")`

**Expected Results:**
- Either validation error or API error
- Clear error message
- No crash

---

### Test Case 5.3: Nonexistent Application ID

**Objective:** Verify behavior when appId doesn't exist.

**Test Steps:**
1. Call `list_application_libraries(appId="00000000-0000-0000-0000-000000000000")`

**Expected Results:**
- Error response or empty list
- Appropriate error message

---

### Test Case 5.4: Whitespace in appId

**Objective:** Verify whitespace handling.

**Test Steps:**
1. Call with whitespace: `appId=" abc123 "`
2. Call with only whitespace: `appId="   "`

**Expected Results:**
- Whitespace-only: validation error
- Leading/trailing: either trimmed or error

---

## 6. Integration Tests

### Test Case 6.1: Workflow with search_applications

**Objective:** Verify typical workflow.

**Test Steps:**
1. Call `search_applications(name="<app-name>")`
2. Extract `appId` from results
3. Call `list_application_libraries(appId="<extracted-id>")`

**Expected Results:**
- Both calls succeed
- Libraries returned for correct application
- `appId` in library objects matches input

---

### Test Case 6.2: Use with list_applications_by_cve

**Objective:** Verify CVE data consistency.

**Test Steps:**
1. Call `list_applications_by_cve(cveId="CVE-2021-44228")`
2. Get an affected appId from results
3. Call `list_application_libraries` for that app
4. Find the library with that CVE

**Expected Results:**
- Library with CVE found in results
- CVE ID matches
- Severity data consistent

---

## 7. Response Structure Tests

### Test Case 7.1: Response Schema Validation

**Objective:** Verify response conforms to schema.

**Test Steps:**
1. Call `list_application_libraries` with valid appId
2. Examine response fields

**Expected Results:**
- Response has `data`, `message`, `found` fields
- `data` is array of LibraryExtended
- All required fields present

---

### Test Case 7.2: Warning Messages

**Objective:** Verify warning messages are helpful.

**Test Steps:**
1. Test scenarios that generate warnings:
   - No libraries found

**Expected Results:**
- Warnings are clear and actionable
- Help user understand data state

---

## 8. Data Quality Tests

### Test Case 8.1: Hash Uniqueness

**Objective:** Verify each library has unique hash.

**Test Steps:**
1. Get all libraries
2. Check for duplicate hashes

**Expected Results:**
- All hashes unique (unless same JAR appears twice)
- Hash is non-empty for all

---

### Test Case 8.2: Application Context Consistency

**Objective:** Verify app context matches request.

**Test Steps:**
1. Call `list_application_libraries`
2. Check `appId` and `app_name` in each library

**Expected Results:**
- All libraries have matching `appId`
- `app_name` consistent across response

---

### Test Case 8.3: Grade and Score Correlation

**Objective:** Verify grade correlates with score.

**Test Steps:**
1. Get libraries
2. Compare `grade` to `libScore`

**Expected Results:**
- Grade "A" = high score
- Grade "F" = low score
- Logical correlation

---

## 9. Error Handling Tests

### Test Case 9.1: API Connection Failure

**Objective:** Verify graceful failure handling.

**Prerequisites:**
- Simulate connection failure

**Test Steps:**
1. Configure invalid credentials
2. Call `list_application_libraries`

**Expected Results:**
- Error response returned
- Error message indicates connection issue
- No crash

---

### Test Case 9.2: Timeout Handling

**Objective:** Verify timeout behavior.

**Test Steps:**
1. Test with network delays
2. Call `list_application_libraries`

**Expected Results:**
- Returns within reasonable timeout
- Either success or error (no hang)

---

### Test Case 9.3: Repeated Calls Consistency

**Objective:** Verify consistent results.

**Test Steps:**
1. Call `list_application_libraries` 3 times
2. Compare results

**Expected Results:**
- All calls return same data
- Consistent library counts

---

## 10. Performance Tests

### Test Case 10.1: Response Time - Small Application

**Objective:** Measure performance for small apps.

**Test Steps:**
1. Call for app with ~10 libraries
2. Measure response time

**Expected Results:**
- First call: < 3 seconds
- Response time acceptable

---

### Test Case 10.2: Response Time - Large Application

**Objective:** Measure performance for large apps.

**Test Steps:**
1. Call for app with 100+ libraries
2. Measure response time

**Expected Results:**
- First call: < 15 seconds
- All libraries returned
- No truncation

---

## 11. Pagination Tests

### Test Case 11.1: Default Pagination

**Objective:** Verify default page and pageSize when not provided.

**Test Steps:**
1. Call `list_application_libraries(appId="<valid-id>")`
2. Examine pagination metadata

**Expected Results:**
- `page: 1`
- `pageSize: 50`
- `totalItems`: total library count
- `hasMorePages`: true if totalItems > 50

---

### Test Case 11.2: Custom Page Size

**Objective:** Verify pagination with custom pageSize.

**Test Steps:**
1. Call `list_application_libraries(page=1, pageSize=10, appId="<valid-id>")`
2. Compare with full results

**Expected Results:**
- `items.length <= 10`
- `pageSize: 10`
- `totalItems` matches full count
- `hasMorePages: true` if totalItems > 10

---

### Test Case 11.3: Second Page

**Objective:** Verify second page retrieval.

**Test Steps:**
1. Call `list_application_libraries(page=2, pageSize=5, appId="<valid-id>")`
2. Compare to first page

**Expected Results:**
- Different libraries than page 1
- `page: 2`
- Items 6-10 from full list

---

### Test Case 11.4: Page Beyond Results

**Objective:** Verify behavior when page exceeds data.

**Test Steps:**
1. Call `list_application_libraries(page=1000, pageSize=50, appId="<valid-id>")`

**Expected Results:**
- `items: []` (empty array)
- `totalItems`: actual count
- `hasMorePages: false`
- No error
- **No warning** (warning only appears when app has zero libraries total)

---

### Test Case 11.5: Invalid Pagination Values

**Objective:** Verify graceful handling of invalid values.

**Test Steps:**
1. Call with `page=0`
2. Call with `page=-1`
3. Call with `pageSize=0`
4. Call with `pageSize=100`

**Expected Results:**
- `page < 1` → uses page 1 with warning
- `pageSize < 1` → uses default 50 with warning
- `pageSize > 50` → caps at 50 with warning
  - Response `pageSize` field shows `50` (capped value, not requested value)
  - Warning message mentions "exceeds maximum 50"

---

### Test Case 11.6: Pagination Integrity - No Duplicates

**Objective:** Verify paginating through all results produces complete, non-duplicate dataset.

**Prerequisites:** Application with 100+ libraries

**Test Steps:**
1. Call `list_application_libraries(page=1, pageSize=25, appId="<valid-id>")`
2. Call `list_application_libraries(page=2, pageSize=25, appId="<valid-id>")`
3. Continue until `hasMorePages: false`
4. Collect all library hashes from all pages

**Expected Results:**
- Total items collected equals `totalItems` from first response
- No duplicate hashes across pages
- Each page returns exactly `pageSize` items (except last page)
- `hasMorePages: false` only on final page

---

## Test Execution Guidelines

### Pre-Test Setup
1. Verify MCP server running with valid credentials
2. Have applications with:
   - Many libraries (50+)
   - Vulnerable libraries
   - Mix of used/unused dependencies

### Workflow for Finding Test Data
```
1. search_applications() → get list of apps
2. Pick apps with different library profiles
3. Note appIds for testing
4. Call list_application_libraries(appId) for each
```

### Success Criteria
The `list_application_libraries` tool passes testing if:
- Basic functionality test passes (TC 1.1)
- Library data is complete (TC 1.2)
- Class usage logic is correct (TC 2.1-2.2)
- Vulnerability data is accurate (TC 3.1-3.2)
- Empty cases handled gracefully (TC 4.1)
- Validation catches invalid input (TC 5.1-5.4)
- Integration workflow works (TC 6.1)
- Performance is acceptable

---

## Test Coverage Summary

| Category | Test Cases | Coverage |
|----------|------------|----------|
| Basic Functionality | 4 | Core behavior verification |
| Class Usage | 3 | Usage analysis tests |
| Vulnerabilities | 4 | CVE data tests |
| Empty/Edge Cases | 2 | No data scenarios |
| Validation | 4 | Parameter validation |
| Integration | 2 | Workflow with other tools |
| Response Structure | 2 | Schema validation |
| Data Quality | 3 | Data consistency checks |
| Error Handling | 3 | Failures, timeouts |
| Performance | 2 | Response time |
| Pagination | 6 | Page navigation, edge cases, integrity |

**Total: 35 test cases**

---

## Appendix: Expected Behavior Examples

### Successful Response Example
```json
{
  "items": [
    {
      "fileName": "spring-core-5.3.10.jar",
      "version": "5.3.10",
      "hash": "abc123def456...",
      "libraryId": 12345,
      "classCount": 1250,
      "classesUsed": 345,
      "totalVulnerabilities": 2,
      "highVulnerabilities": 1,
      "vulns": [
        {"severityCode": "HIGH"},
        {"severityCode": "MEDIUM"}
      ],
      "grade": "C",
      "libScore": 65.5,
      "monthsOutdated": 8,
      "latestVersion": "5.3.23",
      "appId": "app-123",
      "app_name": "MyApplication",
      "appLanguage": "Java",
      "custom": false
    },
    {
      "fileName": "log4j-core-2.17.1.jar",
      "version": "2.17.1",
      "hash": "def456ghi789...",
      "libraryId": 12346,
      "classCount": 450,
      "classesUsed": 0,
      "totalVulnerabilities": 0,
      "highVulnerabilities": 0,
      "vulns": [],
      "grade": "A",
      "libScore": 95.0,
      "monthsOutdated": 0,
      "latestVersion": "2.17.1",
      "appId": "app-123",
      "app_name": "MyApplication",
      "appLanguage": "Java",
      "custom": false
    }
  ],
  "page": 1,
  "pageSize": 50,
  "totalItems": 127,
  "hasMorePages": true,
  "errors": [],
  "warnings": [],
  "durationMs": 245
}
```

### Empty Response Example
```json
{
  "items": [],
  "page": 1,
  "pageSize": 50,
  "totalItems": 0,
  "hasMorePages": false,
  "errors": [],
  "warnings": ["No libraries found for this application. The application may not have any third-party dependencies, or library data may not have been collected yet."],
  "durationMs": 42
}
```

### Error Response Example
```json
{
  "items": [],
  "page": 1,
  "pageSize": 50,
  "totalItems": 0,
  "hasMorePages": false,
  "errors": ["appId is required"],
  "warnings": [],
  "durationMs": null
}
```

---

## Logging Verification

Check MCP server logs (`/tmp/mcp-contrast.log`) for:
- Debug: "Retrieving libraries for application: {appId}"
- Debug: "Retrieved {N} libraries for application {appId}"
- No error logs in success case

---

## References

- **Tool Implementation**: `tool/library/ListApplicationLibrariesTool.java`
- **Params Class**: `tool/library/params/ListApplicationLibrariesParams.java`
- **Data Model**: `sdkextension/data/LibraryExtended.java`
- **Related Tools**: `search_applications`, `list_applications_by_cve`
- **Old Test Plan**: `test-plan-list_application_libraries.md` (root level)
