# Test Plan: search_vulnerabilities Tool

## Overview

This test plan provides comprehensive testing guidance for the `search_vulnerabilities` MCP tool. This tool searches vulnerabilities across all applications in an organization with optional filtering and pagination.

### Migration Notes

**This plan replaces:**
- `test-plan-list_all_vulnerabilities.md` - Organization-level vulnerability listing with all filters

### Tool Signature

**MCP Tool Name:** `search_vulnerabilities`

**Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | Integer | No | 1 | Page number (1-based) |
| `pageSize` | Integer | No | 50 | Items per page (max 100) |
| `severities` | String | No | null | Comma-separated: CRITICAL,HIGH,MEDIUM,LOW,NOTE |
| `statuses` | String | No | smart defaults | Comma-separated: Reported,Suspicious,Confirmed,Remediated,Fixed |
| `vulnTypes` | String | No | null | Comma-separated vulnerability types (e.g., sql-injection,xss-reflected) |
| `environments` | String | No | null | Comma-separated: DEVELOPMENT,QA,PRODUCTION |
| `lastSeenAfter` | String | No | null | ISO date (YYYY-MM-DD) or epoch timestamp |
| `lastSeenBefore` | String | No | null | ISO date (YYYY-MM-DD) or epoch timestamp |
| `vulnTags` | String | No | null | Comma-separated vulnerability-level tags |

### Response Structure

**Returns:** `PaginatedToolResponse<VulnLight>`

```java
PaginatedToolResponse {
    List<VulnLight> items,       // Vulnerabilities for current page
    int page,                     // Current page number
    int pageSize,                 // Items per page
    Integer totalItems,           // Total matching items (may be null in some modes)
    boolean hasMorePages,         // Whether more pages exist
    String message                // Warnings or info messages
}

VulnLight {
    String title,                 // Human-readable vulnerability name
    String type,                  // Vulnerability type (e.g., "sql-injection")
    String vulnID,                // Unique identifier (UUID)
    String severity,              // CRITICAL, HIGH, MEDIUM, LOW, NOTE
    String appID,                 // Application UUID
    String appName,               // Application display name
    List<SessionMetadata> sessionMetadata,  // Session context
    String lastSeenAt,            // ISO-8601 timestamp
    String status,                // Reported, Suspicious, Confirmed, Remediated, Fixed
    String firstSeenAt,           // ISO-8601 timestamp (nullable)
    String closedAt,              // ISO-8601 timestamp (nullable)
    List<String> environments,    // Historical environments (DEVELOPMENT, QA, PRODUCTION)
    List<String> tags             // User-defined vulnerability tags
}
```

### Critical Behavioral Notes

| Behavior | Description |
|----------|-------------|
| **Smart Defaults** | When `statuses` is omitted, uses Reported,Suspicious,Confirmed (excludes Fixed,Remediated) |
| **Date Filtering** | Filters on `lastTimeSeen`, NOT discovery date |
| **Severity Validation** | Invalid severities cause HARD FAILURE (error, no results) |
| **Status Validation** | Invalid statuses cause HARD FAILURE (error, no results) |
| **Environment Validation** | Invalid environments cause HARD FAILURE (error, no results) |
| **VulnTypes** | Passed through to SDK without validation (invalid types return empty results) |
| **Case Sensitivity** | Severities and environments converted to uppercase; statuses are title-case |

---

## 1. Pagination Tests

### Test Case 1.1: First Page with Default Values

**Objective:** Verify default pagination behavior when no parameters specified.

**Prerequisites:**
- Organization with at least 51 vulnerabilities

**Test Steps:**
1. Call `search_vulnerabilities` with no parameters
2. Verify response structure

**Expected Results:**
- Returns page 1 with pageSize 50
- `items` array contains up to 50 vulnerabilities
- `hasMorePages: true` if more than 50 vulnerabilities exist
- Smart defaults warning in `message` field

---

### Test Case 1.2: Subsequent Pages

**Objective:** Verify pagination across multiple pages.

**Prerequisites:**
- Organization with at least 100 vulnerabilities

**Test Steps:**
1. Call `search_vulnerabilities` with `page=2, pageSize=50`
2. Compare results with page 1

**Expected Results:**
- Returns page 2 items (different from page 1)
- Consistent ordering across pages
- `hasMorePages` indicates if more pages exist

---

### Test Case 1.3: Custom Page Sizes

**Objective:** Test various page size values.

**Test Steps:**
1. Call with `pageSize=10`
2. Call with `pageSize=25`
3. Call with `pageSize=100`

**Expected Results:**
- Each returns the specified number of items (or fewer if insufficient data)
- Response reflects the correct `pageSize` value
- `hasMorePages` calculated correctly based on page size

---

### Test Case 1.4: Maximum Page Size Boundary

**Objective:** Test page size at and beyond maximum (100).

**Test Steps:**
1. Call with `pageSize=100` (valid max)
2. Call with `pageSize=101` (exceeds max)
3. Call with `pageSize=500` (far exceeds max)

**Expected Results:**
- pageSize 100: Returns up to 100 items
- pageSize 101+: Clamped to 100 with warning in `message`
- Response shows `pageSize: 100` when clamped

---

### Test Case 1.5: Invalid Page Numbers (Zero/Negative)

**Objective:** Test zero and negative page numbers.

**Test Steps:**
1. Call with `page=0`
2. Call with `page=-1`
3. Call with `page=-100`

**Expected Results:**
- All invalid page numbers clamped to 1
- Warning in `message` field
- Returns first page of results

---

### Test Case 1.6: Invalid Page Size (Zero/Negative)

**Objective:** Test zero and negative page sizes.

**Test Steps:**
1. Call with `pageSize=0`
2. Call with `pageSize=-1`
3. Call with `pageSize=-50`

**Expected Results:**
- All invalid page sizes default to 50
- Warning in `message` field
- Returns 50 items per page

---

### Test Case 1.7: Page Beyond Available Data

**Objective:** Request page number exceeding available data.

**Test Steps:**
1. Call with `page=999, pageSize=50`

**Expected Results:**
- Returns empty `items` array
- `hasMorePages: false`
- No error, just empty results

---

## 2. Severity Filtering Tests

### Test Case 2.1: Single Severity Filter

**Objective:** Filter by each severity level individually.

**Test Steps:**
1. Call with `severities="CRITICAL"`
2. Call with `severities="HIGH"`
3. Call with `severities="MEDIUM"`
4. Call with `severities="LOW"`
5. Call with `severities="NOTE"`

**Expected Results:**
- Returns only vulnerabilities matching the specified severity
- All items have matching `severity` field
- No warnings or errors

---

### Test Case 2.2: Multiple Severities (Comma-Separated)

**Objective:** Filter by multiple severity levels.

**Test Steps:**
1. Call with `severities="CRITICAL,HIGH"`
2. Call with `severities="MEDIUM,LOW,NOTE"`
3. Call with `severities="CRITICAL,MEDIUM"`

**Expected Results:**
- Returns vulnerabilities matching any specified severity
- Each item's `severity` matches one of the requested severities
- No warnings or errors

---

### Test Case 2.3: Invalid Severity Values (HARD FAILURE)

**Objective:** Verify error handling for invalid severities.

**Test Steps:**
1. Call with `severities="INVALID"`
2. Call with `severities="URGENT"`
3. Call with `severities="CRITICAL,INVALID,HIGH"`

**Expected Results:**
- **HARD FAILURE:** Returns error response
- Empty `items` array
- Error in `message`: "Invalid severity '{value}'. Valid: CRITICAL, HIGH, MEDIUM, LOW, NOTE"
- No partial results

---

### Test Case 2.4: Case Handling for Severities

**Objective:** Verify case conversion behavior.

**Test Steps:**
1. Call with `severities="critical"` (lowercase)
2. Call with `severities="High"` (mixed case)
3. Call with `severities="MeDiUm"` (random case)

**Expected Results:**
- Values are converted to uppercase internally
- Matches work correctly regardless of input case
- Returns expected results

---

### Test Case 2.5: No Severity Filter (All Severities)

**Objective:** Verify omitting severity returns all severity levels.

**Test Steps:**
1. Call with no `severities` parameter

**Expected Results:**
- Returns vulnerabilities of all severity levels
- Response includes mix of severity levels
- No severity-related warnings

---

## 3. Status Filtering Tests

### Test Case 3.1: Single Status Filter

**Objective:** Filter by each status individually.

**Test Steps:**
1. Call with `statuses="Reported"`
2. Call with `statuses="Suspicious"`
3. Call with `statuses="Confirmed"`
4. Call with `statuses="Remediated"`
5. Call with `statuses="Fixed"`

**Expected Results:**
- Returns only vulnerabilities matching the specified status
- All items have matching `status` field
- No smart defaults warning (explicit status provided)

---

### Test Case 3.2: Multiple Statuses (Comma-Separated)

**Objective:** Filter by multiple statuses.

**Test Steps:**
1. Call with `statuses="Reported,Confirmed"`
2. Call with `statuses="Suspicious,Confirmed"`
3. Call with `statuses="Remediated,Fixed"`

**Expected Results:**
- Returns vulnerabilities matching any specified status
- Each item's `status` matches one of the requested values
- No warnings

---

### Test Case 3.3: Default Behavior - Smart Defaults (CRITICAL)

**Objective:** Test smart defaults when status filter is omitted.

**Test Steps:**
1. Call with no `statuses` parameter

**Expected Results:**
- Uses smart defaults: Reported, Suspicious, Confirmed
- **Excludes** Fixed and Remediated
- Warning in `message`: "Showing actionable vulnerabilities only (excluding Fixed and Remediated)..."
- Only actionable vulnerabilities returned

---

### Test Case 3.4: Invalid Status Values (HARD FAILURE)

**Objective:** Verify error handling for invalid statuses.

**Test Steps:**
1. Call with `statuses="Open"` (invalid)
2. Call with `statuses="Closed"` (invalid)
3. Call with `statuses="Reported,Invalid,Confirmed"`

**Expected Results:**
- **HARD FAILURE:** Returns error response
- Empty `items` array
- Error in `message`: "Invalid status '{value}'. Valid: Reported, Suspicious, Confirmed, Remediated, Fixed"
- No partial results

---

### Test Case 3.5: Explicitly Include Fixed/Remediated

**Objective:** Verify Fixed/Remediated can be explicitly included.

**Test Steps:**
1. Call with `statuses="Remediated"`
2. Call with `statuses="Fixed"`
3. Call with `statuses="Reported,Suspicious,Confirmed,Remediated,Fixed"`

**Expected Results:**
- Returns vulnerabilities in specified statuses
- No smart defaults warning (explicit filter provided)
- Fixed/Remediated vulnerabilities included

---

## 4. Vulnerability Type Filtering Tests

### Test Case 4.1: Single Vulnerability Type

**Objective:** Filter by specific vulnerability types.

**Test Steps:**
1. Call with `vulnTypes="sql-injection"`
2. Call with `vulnTypes="xss-reflected"`
3. Call with `vulnTypes="path-traversal"`

**Expected Results:**
- Returns only vulnerabilities of the specified type
- All items have matching `type` field
- No validation errors

---

### Test Case 4.2: Multiple Vulnerability Types

**Objective:** Filter by multiple types.

**Test Steps:**
1. Call with `vulnTypes="sql-injection,xss-reflected"`
2. Call with `vulnTypes="crypto-bad-mac,crypto-bad-ciphers"`

**Expected Results:**
- Returns vulnerabilities matching any specified type
- Each item's `type` matches one of the requested types

---

### Test Case 4.3: Invalid/Non-existent Vulnerability Type

**Objective:** Test behavior with non-existent type.

**Test Steps:**
1. Call with `vulnTypes="non-existent-type"`
2. Call with `vulnTypes="invalid-injection"`

**Expected Results:**
- No validation error (types passed through to SDK)
- Returns empty results (SDK finds no matches)
- Empty `items` array

---

### Test Case 4.4: Case Handling for Vulnerability Types

**Objective:** Verify case conversion for vuln types.

**Test Steps:**
1. Call with `vulnTypes="SQL-INJECTION"` (uppercase)
2. Call with `vulnTypes="Sql-Injection"` (mixed case)

**Expected Results:**
- Values converted to lowercase internally
- Matches work correctly
- Returns expected results

---

### Test Case 4.5: No Vulnerability Type Filter

**Objective:** Verify omitting vulnTypes returns all types.

**Test Steps:**
1. Call with no `vulnTypes` parameter

**Expected Results:**
- Returns vulnerabilities of all types
- Response includes various vulnerability types

---

## 5. Environment Filtering Tests

### Test Case 5.1: Single Environment Filter

**Objective:** Filter by each environment individually.

**Test Steps:**
1. Call with `environments="DEVELOPMENT"`
2. Call with `environments="QA"`
3. Call with `environments="PRODUCTION"`

**Expected Results:**
- Returns vulnerabilities seen in the specified environment
- Items' `environments` array includes the requested environment
- Note: `environments` shows historical presence

---

### Test Case 5.2: Multiple Environments

**Objective:** Filter by multiple environments.

**Test Steps:**
1. Call with `environments="PRODUCTION,QA"`
2. Call with `environments="DEVELOPMENT,QA,PRODUCTION"`

**Expected Results:**
- Returns vulnerabilities seen in any specified environment
- Each item's `environments` includes at least one requested environment

---

### Test Case 5.3: Invalid Environment Values (HARD FAILURE)

**Objective:** Verify error handling for invalid environments.

**Test Steps:**
1. Call with `environments="PROD"` (abbreviation)
2. Call with `environments="STAGING"` (not valid)
3. Call with `environments="production"` (lowercase)
4. Call with `environments="PRODUCTION,INVALID"`

**Expected Results:**
- **HARD FAILURE:** Returns error response
- Empty `items` array
- Error in `message`: "Invalid environment '{value}'. Valid: DEVELOPMENT, QA, PRODUCTION"
- No partial results

---

### Test Case 5.4: No Environment Filter

**Objective:** Verify omitting environments returns all.

**Test Steps:**
1. Call with no `environments` parameter

**Expected Results:**
- Returns vulnerabilities from all environments
- No environment-related warnings

---

## 6. Date Filtering Tests

### Test Case 6.1: Valid ISO Date - lastSeenAfter

**Objective:** Filter vulnerabilities active after a date.

**Test Steps:**
1. Call with `lastSeenAfter="2025-01-01"`
2. Call with `lastSeenAfter="2025-10-01"`

**Expected Results:**
- Returns vulnerabilities with `lastSeenAt` after the date
- Warning in `message`: "Time filters apply to LAST ACTIVITY DATE (lastTimeSeen), not discovery date."
- All items have `lastSeenAt` >= specified date

---

### Test Case 6.2: Valid ISO Date - lastSeenBefore

**Objective:** Filter vulnerabilities active before a date.

**Test Steps:**
1. Call with `lastSeenBefore="2025-12-31"`
2. Call with `lastSeenBefore="2025-10-01"`

**Expected Results:**
- Returns vulnerabilities with `lastSeenAt` before the date
- Warning about last activity date
- All items have `lastSeenAt` <= specified date

---

### Test Case 6.3: Epoch Timestamp Format

**Objective:** Test date filtering with epoch timestamps.

**Test Steps:**
1. Call with `lastSeenAfter="1704067200000"` (2025-01-01 in ms)
2. Call with `lastSeenBefore="1735689599000"` (2024-12-31 in ms)

**Expected Results:**
- Accepts epoch timestamps (milliseconds)
- Filters correctly based on timestamp
- Warning about last activity date

---

### Test Case 6.4: Date Range (Both After and Before)

**Objective:** Filter within a date range.

**Test Steps:**
1. Call with `lastSeenAfter="2025-01-01", lastSeenBefore="2025-12-31"`

**Expected Results:**
- Returns vulnerabilities with `lastSeenAt` between the dates
- Warning about last activity date
- All items fall within specified range

---

### Test Case 6.5: Invalid Date Formats (HARD FAILURE)

**Objective:** Test various invalid date formats.

**Test Steps:**
1. Call with `lastSeenAfter="2025/01/01"` (wrong separator)
2. Call with `lastSeenAfter="01-01-2025"` (wrong order)
3. Call with `lastSeenAfter="invalid-date"`
4. Call with `lastSeenAfter="2025-13-01"` (invalid month)

**Expected Results:**
- **HARD FAILURE:** Returns error response
- Empty `items` array
- Error in `message` with parsing failure details

---

### Test Case 6.6: Invalid Date Range - After > Before (HARD FAILURE)

**Objective:** Test when lastSeenAfter is later than lastSeenBefore.

**Test Steps:**
1. Call with `lastSeenAfter="2025-12-31", lastSeenBefore="2025-01-01"`

**Expected Results:**
- **HARD FAILURE:** Returns error response
- Error in `message`: "Invalid date range: lastSeenAfter must be before lastSeenBefore"
- Empty `items` array

---

### Test Case 6.7: Same Date for After and Before

**Objective:** Test when dates are equal.

**Test Steps:**
1. Call with `lastSeenAfter="2025-10-15", lastSeenBefore="2025-10-15"`

**Expected Results:**
- Valid query (not an error)
- Returns vulnerabilities with `lastSeenAt` on that date
- May return empty results if none match

---

## 7. Tag Filtering Tests

### Test Case 7.1: Single Tag Filter

**Objective:** Filter by vulnerability tag.

**Test Steps:**
1. Call with `vulnTags="reviewed"`
2. Call with `vulnTags="urgent"`
3. Call with `vulnTags="false-positive"`

**Expected Results:**
- Returns only vulnerabilities with the specified tag
- Tags are case-sensitive
- URL encoding handled automatically

---

### Test Case 7.2: SmartFix Remediated Tag (Space in Name)

**Objective:** Test special tag with space.

**Test Steps:**
1. Call with `vulnTags="SmartFix Remediated"`

**Expected Results:**
- Returns SmartFix-remediated vulnerabilities
- Space in tag name is URL-encoded automatically
- Works correctly despite space

---

### Test Case 7.3: Multiple Tags (Comma-Separated)

**Objective:** Filter by multiple tags.

**Test Steps:**
1. Call with `vulnTags="reviewed,urgent"`
2. Call with `vulnTags="SmartFix Remediated,reviewed"`

**Expected Results:**
- Returns vulnerabilities matching any specified tag
- Each tag is URL-encoded if it contains spaces

---

### Test Case 7.4: Case Sensitivity Verification

**Objective:** Verify tags are case-sensitive.

**Test Steps:**
1. Call with `vulnTags="Reviewed"` (capital R)
2. Call with `vulnTags="reviewed"` (lowercase)
3. Call with `vulnTags="REVIEWED"` (uppercase)

**Expected Results:**
- Only exact case match returns results
- Different cases return different (or no) results
- Case-sensitive matching confirmed

---

### Test Case 7.5: Non-Existent Tag

**Objective:** Test with tag that doesn't exist.

**Test Steps:**
1. Call with `vulnTags="nonexistent-tag-xyz"`

**Expected Results:**
- Returns empty results
- Empty `items` array
- No errors (just no matches)

---

### Test Case 7.6: No Tag Filter

**Objective:** Verify omitting tags returns all.

**Test Steps:**
1. Call with no `vulnTags` parameter

**Expected Results:**
- Returns vulnerabilities regardless of tags
- Includes both tagged and untagged vulnerabilities

---

### Test Case 7.7: Tag with Special Characters

**Objective:** Test tags containing URL-special characters.

**Test Steps:**
1. Call with `vulnTags="tag&value"` (ampersand)
2. Call with `vulnTags="tag=value"` (equals sign)
3. Call with `vulnTags="tag+value"` (plus sign)

**Expected Results:**
- Special characters are URL-encoded automatically
- Returns matching vulnerabilities correctly
- No encoding errors or malformed requests

---

## 8. Combined Filter Tests

### Test Case 8.1: Production Critical Issues

**Objective:** Combine severity and environment filters.

**Test Steps:**
1. Call with `severities="CRITICAL", environments="PRODUCTION"`

**Expected Results:**
- Returns only CRITICAL vulnerabilities in PRODUCTION
- All items have `severity: "CRITICAL"` and `environments` includes "PRODUCTION"

---

### Test Case 8.2: High-Priority Open Issues with Recent Activity

**Objective:** Combine severity, status, and date filters.

**Test Steps:**
1. Call with `severities="CRITICAL,HIGH", statuses="Reported,Confirmed", lastSeenAfter="2025-09-01"`

**Expected Results:**
- Returns CRITICAL or HIGH severity vulnerabilities
- Status is Reported or Confirmed
- Last seen after September 1, 2025
- No smart defaults warning (explicit status provided)
- Warning about time filter on last activity date

---

### Test Case 8.3: SmartFix Remediated Vulnerabilities

**Objective:** Combine tag filter with status filter.

**Test Steps:**
1. Call with `vulnTags="SmartFix Remediated", statuses="Remediated"`

**Expected Results:**
- Returns vulnerabilities remediated by SmartFix
- All items have status "Remediated"
- No smart defaults warning

---

### Test Case 8.4: Kitchen Sink (All Filters)

**Objective:** Test with all filter parameters specified.

**Test Steps:**
1. Call with:
   - `page=2, pageSize=25`
   - `severities="CRITICAL,HIGH"`
   - `statuses="Reported,Confirmed"`
   - `vulnTypes="sql-injection,xss-reflected"`
   - `environments="PRODUCTION"`
   - `lastSeenAfter="2025-01-01", lastSeenBefore="2025-12-31"`
   - `vulnTags="reviewed"`

**Expected Results:**
- All filters applied simultaneously
- Returns page 2 with 25 items per page
- Results match all specified criteria
- Time filter warning in message

---

### Test Case 8.5: Conflicting Filters (Empty Results)

**Objective:** Test filter combinations that return no results.

**Test Steps:**
1. Call with `severities="CRITICAL", vulnTypes="non-existent-type"`
2. Call with `lastSeenAfter="2099-01-01"` (future date)

**Expected Results:**
- Returns empty `items` array
- `totalItems: 0`
- `hasMorePages: false`
- No errors (valid query, just no matching data)

---

## 9. Empty Results Tests

### Test Case 9.1: No Vulnerabilities in Organization

**Objective:** Test against organization with no vulnerabilities.

**Test Steps:**
1. Call with no filters (or any filters) on empty org

**Expected Results:**
- Returns empty `items` array
- `totalItems: 0`
- `hasMorePages: false`
- Message may indicate no vulnerabilities

---

### Test Case 9.2: All Vulnerabilities Filtered Out

**Objective:** Test filters that exclude all vulnerabilities.

**Test Steps:**
1. Call with `severities="CRITICAL"` (when none exist)
2. Call with `lastSeenAfter="2099-01-01"` (future date)

**Expected Results:**
- Returns empty `items` array
- `totalItems: 0`
- `hasMorePages: false`
- No errors

---

### Test Case 9.3: Smart Defaults Exclude All

**Objective:** Test when smart defaults filter out all vulnerabilities.

**Test Steps:**
1. Call with no `statuses` parameter (in org where all are Fixed/Remediated)

**Expected Results:**
- Smart defaults applied: Reported, Suspicious, Confirmed
- If all vulnerabilities are Fixed or Remediated, returns empty
- Warning about smart defaults in `message`

---

## 10. Response Validation Tests

### Test Case 10.1: Response Structure Validation

**Objective:** Verify all response fields are present and correctly typed.

**Test Steps:**
1. Make any valid query

**Expected Results:**
- Response contains all required fields:
  - `items`: array of VulnLight objects
  - `page`: integer >= 1
  - `pageSize`: integer (1-100)
  - `totalItems`: integer or null
  - `hasMorePages`: boolean
  - `message`: string or null

---

### Test Case 10.2: VulnLight Object Structure

**Objective:** Verify each vulnerability item has correct structure.

**Test Steps:**
1. Query vulnerabilities and examine each item

**Expected Results:**
- Each item contains:
  - `title`: non-empty string
  - `type`: string (vulnerability type)
  - `vulnID`: string (UUID format)
  - `severity`: one of CRITICAL, HIGH, MEDIUM, LOW, NOTE
  - `appID`: string (UUID format)
  - `appName`: string (application name)
  - `status`: one of Reported, Suspicious, Confirmed, Remediated, Fixed
  - `sessionMetadata`: array (may be empty)
  - `lastSeenAt`: ISO timestamp
  - `firstSeenAt`: ISO timestamp or null
  - `closedAt`: ISO timestamp or null
  - `environments`: array of strings
  - `tags`: array of strings (may be empty)

---

### Test Case 10.3: Application Fields Presence

**Objective:** Verify application context is included.

**Test Steps:**
1. Query vulnerabilities
2. Check each item for application fields

**Expected Results:**
- Each item has `appID` field (UUID)
- Each item has `appName` field (application display name)
- Both fields are direct properties of VulnLight (not nested)

---

### Test Case 10.4: hasMorePages Accuracy

**Objective:** Verify hasMorePages flag is correctly set.

**Test Steps:**
1. Call with `page=1, pageSize=10` (when 50 total items exist)
2. Call with `page=5, pageSize=10` (when 50 total items exist)
3. Call with `page=6, pageSize=10` (when 50 total items exist)

**Expected Results:**
- page 1, pageSize 10, 50 total: `hasMorePages: true`
- page 5, pageSize 10, 50 total: `hasMorePages: false` (last page)
- page 6, pageSize 10, 50 total: `hasMorePages: false` (beyond last page)

---

### Test Case 10.5: Message Field Warnings

**Objective:** Verify warning messages appear correctly.

**Test Steps:**
1. Default status filter (should show smart defaults warning)
2. Date filters (should show last activity warning)
3. Invalid page/pageSize (should show clamping warnings)
4. Multiple warnings (combine above)

**Expected Results:**
- `message` field contains warning text when applicable
- Multiple warnings may be combined
- Warnings are informative and actionable

---

### Test Case 10.6: Error Message Format

**Objective:** Verify error messages for validation failures.

**Test Steps:**
1. Invalid severity: "INVALID"
2. Invalid status: "Closed"
3. Invalid environment: "STAGING"
4. Invalid date: "invalid-date"
5. Invalid date range: after > before

**Expected Results:**
- `items`: empty array
- `totalItems: 0`
- `hasMorePages: false`
- `message`: contains descriptive error with valid options and examples

---

### Test Case 10.7: Environments Field - Historical Data

**Objective:** Verify environments field shows historical presence.

**Test Steps:**
1. Query vulnerabilities
2. Examine `environments` field

**Expected Results:**
- `environments` shows ALL environments where vulnerability has been seen
- A vulnerability may show multiple environments (historical, not just current)
- Example: ["DEVELOPMENT", "PRODUCTION"]

---

## 11. Permission and Access Tests

### Test Case 11.1: Org-Level API Permissions

**Objective:** Verify tool requires appropriate permissions.

**Prerequisites:**
- Contrast Platform Admin or Org Admin permissions

**Test Steps:**
1. Call `search_vulnerabilities` with admin credentials

**Expected Results:**
- Tool completes successfully
- Returns organization-wide vulnerability data

---

### Test Case 11.2: API Returns No Data

**Objective:** Test graceful handling when API returns null.

**Test Steps:**
1. Test in environment where org-level API fails or returns null

**Expected Results:**
- Warning in response: "API returned no trace data. Verify permissions and filters."
- Empty results returned
- No crash or exception

---

## Test Execution Guidelines

### Pre-Test Setup
1. Verify MCP server is running with valid Contrast credentials
2. Ensure test environment has:
   - Multiple applications with vulnerabilities
   - Vulnerabilities across different severities, statuses, types
   - Vulnerabilities in different environments
   - Vulnerabilities with various tags
   - Mix of open, remediated, and fixed vulnerabilities

### Test Data Recommendations
- At least 150 vulnerabilities (for pagination testing)
- Vulnerabilities at all severity levels
- Vulnerabilities in all statuses
- At least 5-10 different vulnerability types
- Vulnerabilities across all environments
- Vulnerabilities with lastSeenAt dates spanning months
- Vulnerabilities with various tags

### Success Criteria
Each test passes when:
1. Response structure matches expected format
2. Data filtering works correctly
3. Pagination calculations are accurate
4. Warning and error messages are appropriate
5. No unexpected exceptions or errors

---

## Test Coverage Summary

| Category | Test Cases | Coverage |
|----------|------------|----------|
| Pagination | 7 | Page navigation, boundaries, invalid values |
| Severity Filtering | 5 | Single, multiple, invalid, case handling |
| Status Filtering | 5 | Single, multiple, smart defaults, invalid |
| Vuln Type Filtering | 5 | Single, multiple, invalid, case handling |
| Environment Filtering | 4 | Single, multiple, invalid |
| Date Filtering | 7 | ISO format, epoch, range, invalid |
| Tag Filtering | 7 | Single, multiple, special chars, case, URL encoding |
| Combined Filters | 5 | Multi-filter scenarios |
| Empty Results | 3 | No data scenarios |
| Response Validation | 7 | Structure, accuracy, warnings |
| Permission/Access | 2 | Org-level access |

**Total: 57 test cases**

---

## References

- **Tool Implementation**: `tool/assess/SearchVulnerabilitiesTool.java`
- **Related Tools**: `search_app_vulnerabilities`, `search_applications`, `list_vulnerability_types`
- **Old Test Plan**: `test-plan-list_all_vulnerabilities.md`
