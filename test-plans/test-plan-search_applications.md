# Test Plan: search_applications Tool

## Overview

This test plan provides comprehensive testing guidance for the `search_applications` MCP tool. This tool searches applications in an organization with optional filters and pagination.

### Migration Notes

**This plan consolidates and replaces 5 previous test plans:**
1. `test-plan-list_all_applications.md` - No-filter listing
2. `test-plan-list_applications_with_name.md` - Name filter
3. `test-plan-get_applications_by_tag.md` - Tag filter
4. `test-plan-get_applications_by_metadata.md` - Metadata name+value filter
5. `test-plan-get_applications_by_metadata_name.md` - Metadata name-only filter

### Tool Signature

**MCP Tool Name:** `search_applications`

**Parameters:**
| Parameter | Type | Required | Default | Description |
|-----------|------|----------|---------|-------------|
| `page` | Integer | No | 1 | Page number (1-based) |
| `pageSize` | Integer | No | 50 | Items per page (max 100) |
| `name` | String | No | null | Application name filter (partial, **case-INSENSITIVE**) |
| `tag` | String | No | null | Tag filter (exact, **CASE-SENSITIVE**) |
| `metadataName` | String | No | null | Metadata field name (case-insensitive) |
| `metadataValue` | String | No | null | Metadata field value (case-insensitive, requires metadataName) |

### Response Structure

**Returns:** `PaginatedToolResponse<ApplicationData>`

```java
PaginatedToolResponse {
    List<ApplicationData> data,     // Applications for current page
    int page,                        // Current page number
    int pageSize,                    // Items per page
    int totalItems,                  // Total matching items
    int totalPages                   // Total pages available
}

ApplicationData {
    String name,                     // Application name
    String status,                   // Application status (e.g., "enabled", "disabled")
    String appID,                    // Unique application identifier (UUID)
    String lastSeenAt,               // ISO-8601 timestamp with timezone offset
    String language,                 // Programming language
    List<Metadata> metadata,         // Metadata key-value pairs
    List<String> tags,               // User-defined tags
    List<String> technologies        // Technologies/frameworks used
}

Metadata {
    String name,
    String value
}
```

### Critical Behavioral Differences

| Filter | Case Sensitivity | Match Type |
|--------|------------------|------------|
| `name` | **INSENSITIVE** | Partial (contains) |
| `tag` | **SENSITIVE** | Exact match |
| `metadataName` | **INSENSITIVE** | Exact match |
| `metadataValue` | **INSENSITIVE** | Exact match |

---

## 1. Basic Retrieval Tests (No Filters)

### Test Case 1.1: Retrieve All Applications - Success Path

**Objective:** Verify the tool retrieves all applications when no filters are specified.

**Prerequisites:**
- Valid Contrast Security credentials configured
- Organization with at least 3-5 applications

**Test Steps:**
1. Call `search_applications` with no parameters
2. Verify response contains `data` array
3. Verify `totalItems` matches expected application count
4. Verify all applications are accessible across pages

**Expected Results:**
- Returns non-empty list of ApplicationData objects
- All applications in organization are returned (paginated)
- No exceptions thrown
- Response includes pagination metadata

---

### Test Case 1.2: Organization with No Applications

**Objective:** Verify tool handles empty organizations gracefully.

**Prerequisites:**
- Empty organization OR ability to test with empty data

**Test Steps:**
1. Call `search_applications` with no parameters
2. Verify response structure is valid

**Expected Results:**
- Returns empty `data` array `[]` (not null)
- `totalItems` = 0
- `totalPages` = 0 or 1
- No exceptions thrown

---

### Test Case 1.3: Applications with Minimal Data

**Objective:** Verify tool handles applications with sparse data (no metadata, tags, or technologies).

**Prerequisites:**
- Application with only required fields populated

**Test Steps:**
1. Call `search_applications` with no parameters
2. Find application with minimal data
3. Verify all fields are present

**Expected Results:**
- Application included in results
- `metadata` is empty list `[]` (not null)
- `tags` is empty list `[]` (not null)
- `technologies` is empty list `[]` (not null)
- No NullPointerException

---

## 2. Name Filtering Tests

### Test Case 2.1: Exact Name Match

**Objective:** Verify name filter matches exact application name.

**Prerequisites:**
- Application with unique name (e.g., "WebGoat")

**Test Steps:**
1. Use no-filter search to identify unique application name
2. Call `search_applications` with `name="WebGoat"`
3. Verify application is returned

**Expected Results:**
- Returns the matching application
- Count = 1 (assuming unique name)

---

### Test Case 2.2: Partial Name Match

**Objective:** Verify name filter supports partial matching via contains().

**Prerequisites:**
- Applications with names containing common fragment (e.g., "Service")

**Test Steps:**
1. Identify applications with common name fragment
2. Call `search_applications` with `name="Service"`
3. Verify ALL applications containing "Service" are returned

**Expected Results:**
- Returns all applications with "Service" in their name
- Matches "OrderService", "PaymentService", "UserService"
- Partial matching works correctly

---

### Test Case 2.3: Case-Insensitive Name Matching (CRITICAL)

**Objective:** Verify name filter is case-insensitive.

**Prerequisites:**
- Application with mixed-case name (e.g., "WebGoat")

**Test Steps:**
1. Call `search_applications` with `name="webgoat"` (lowercase)
2. Verify "WebGoat" application is returned
3. Call `search_applications` with `name="WEBGOAT"` (uppercase)
4. Verify "WebGoat" application is returned
5. Call `search_applications` with `name="WeBgOaT"` (mixed case)
6. Verify "WebGoat" application is returned

**Expected Results:**
- All case variations match the same applications
- "webgoat", "WEBGOAT", "WebGoat" all match "WebGoat"
- Case-insensitive matching confirmed

---

### Test Case 2.4: Position Matching - Beginning of Name

**Objective:** Verify partial matching works at beginning of name.

**Prerequisites:**
- Applications with names starting with common prefix (e.g., "app-frontend", "app-backend")

**Test Steps:**
1. Call `search_applications` with `name="app-"`
2. Verify all applications starting with "app-" are returned

**Expected Results:**
- Prefix matching works via contains()
- All matching applications returned

---

### Test Case 2.5: Position Matching - Middle of Name

**Objective:** Verify partial matching works in middle of name.

**Prerequisites:**
- Applications with fragments in middle (e.g., "my-service-app")

**Test Steps:**
1. Call `search_applications` with `name="service"`
2. Verify applications with "service" anywhere in name are returned

**Expected Results:**
- Middle matching works via contains()
- All matching applications returned

---

### Test Case 2.6: Position Matching - End of Name

**Objective:** Verify partial matching works at end of name.

**Prerequisites:**
- Applications with names ending with common suffix (e.g., "order-api", "user-api")

**Test Steps:**
1. Call `search_applications` with `name="-api"`
2. Verify all applications ending with "-api" are returned

**Expected Results:**
- Suffix matching works via contains()
- All matching applications returned

---

### Test Case 2.7: Name Filter - No Matches

**Objective:** Verify behavior when name filter matches no applications.

**Test Steps:**
1. Call `search_applications` with `name="ZZZ_NONEXISTENT_XYZ"`
2. Verify response is valid

**Expected Results:**
- Returns empty `data` array `[]`
- `totalItems` = 0
- No exceptions thrown

---

### Test Case 2.8: Single Character Name Search

**Objective:** Verify single character name searches work.

**Test Steps:**
1. Call `search_applications` with `name="a"`
2. Verify all applications containing "a" are returned

**Expected Results:**
- Single character searches execute successfully
- May return many results
- No minimum length validation

---

## 3. Tag Filtering Tests

### Test Case 3.1: Single Tag Match - Common Tag

**Objective:** Verify tag filter correctly identifies applications by tag.

**Prerequisites:**
- Multiple applications with tag "production"

**Test Steps:**
1. Use no-filter search to identify applications with "production" tag
2. Call `search_applications` with `tag="production"`
3. Verify ALL returned applications have "production" in their tags list
4. Verify applications without this tag are excluded

**Expected Results:**
- Returns only applications with matching tag
- Count matches expected number

---

### Test Case 3.2: Tag Filter - CASE-SENSITIVE Verification (CRITICAL)

**Objective:** Verify tag matching is case-sensitive (unlike name filter).

**Prerequisites:**
- Application with tag "Production" (capital P)

**Test Steps:**
1. Call `search_applications` with `tag="Production"` (exact case)
2. Verify application is returned
3. Call `search_applications` with `tag="production"` (lowercase)
4. Verify application is **NOT** returned (case mismatch)
5. Call `search_applications` with `tag="PRODUCTION"` (uppercase)
6. Verify application is **NOT** returned (case mismatch)

**Expected Results:**
- **Tag matching is CASE-SENSITIVE**
- "Production" ≠ "production" ≠ "PRODUCTION"
- Only exact case matches are returned

---

### Test Case 3.3: Tag Filter - Exact String Match (No Partial)

**Objective:** Verify tag filter requires exact match, not substring.

**Prerequisites:**
- Applications with tags: "prod", "production", "prod-east"

**Test Steps:**
1. Call `search_applications` with `tag="prod"`
2. Verify only applications with exactly "prod" tag are returned
3. Verify "production" and "prod-east" apps are **NOT** returned

**Expected Results:**
- Only exact tag matches returned
- No substring or partial matching
- "prod" does not match "production"

---

### Test Case 3.4: Tag Filter - Whitespace Sensitivity

**Objective:** Verify whitespace in tags is significant.

**Prerequisites:**
- Applications with tags containing whitespace (e.g., "team alpha")

**Test Steps:**
1. Call `search_applications` with `tag="team alpha"` (with space)
2. Verify correct applications returned
3. Call `search_applications` with `tag="teamalpha"` (no space)
4. Verify different results (if any exist)

**Expected Results:**
- Whitespace is significant
- "team alpha" ≠ "teamalpha"
- No automatic trimming

---

### Test Case 3.5: Application with Multiple Tags

**Objective:** Verify application matches if ANY tag matches.

**Prerequisites:**
- Application with multiple tags: ["production", "critical", "team-alpha"]

**Test Steps:**
1. Call `search_applications` with `tag="production"`
2. Verify application is returned
3. Call `search_applications` with `tag="critical"`
4. Verify same application is returned
5. Call `search_applications` with `tag="team-alpha"`
6. Verify same application is returned

**Expected Results:**
- Application matches if ANY tag in its list matches
- Same app returned for different tag queries

---

### Test Case 3.6: Tag Filter - No Matches

**Objective:** Verify behavior when tag filter matches no applications.

**Test Steps:**
1. Call `search_applications` with `tag="totally-fake-tag-12345"`
2. Verify response is valid

**Expected Results:**
- Returns empty `data` array `[]`
- `totalItems` = 0
- No exceptions thrown

---

## 4. Metadata Filtering Tests

### 4.1 Metadata Name + Value Tests

### Test Case 4.1.1: Metadata Name+Value - Exact Match

**Objective:** Verify filtering by metadata name AND value together.

**Prerequisites:**
- Application with metadata: {name: "environment", value: "production"}

**Test Steps:**
1. Call `search_applications` with `metadataName="environment"` and `metadataValue="production"`
2. Verify application is returned

**Expected Results:**
- Returns applications with matching metadata name AND value
- Both must match

---

### Test Case 4.1.2: Metadata - Case-Insensitive Name Matching

**Objective:** Verify metadata name matching is case-insensitive.

**Prerequisites:**
- Application with metadata: {name: "Environment", value: "production"}

**Test Steps:**
1. Call with `metadataName="environment"` (lowercase)
2. Call with `metadataName="ENVIRONMENT"` (uppercase)
3. Call with `metadataName="EnViRoNmEnT"` (mixed case)

**Expected Results:**
- All three calls return the same applications
- Metadata name matching is case-insensitive

---

### Test Case 4.1.3: Metadata - Case-Insensitive Value Matching

**Objective:** Verify metadata value matching is case-insensitive.

**Prerequisites:**
- Application with metadata: {name: "status", value: "Active"}

**Test Steps:**
1. Call with `metadataName="status"` and `metadataValue="active"` (lowercase)
2. Call with `metadataName="status"` and `metadataValue="ACTIVE"` (uppercase)
3. Call with `metadataName="status"` and `metadataValue="AcTiVe"` (mixed case)

**Expected Results:**
- All three calls return the same applications
- Metadata value matching is case-insensitive

---

### Test Case 4.1.4: Metadata - Exact Match Required (Not Partial)

**Objective:** Verify metadata requires exact match, not substring.

**Prerequisites:**
- Application A with metadata: {name: "environment", value: "production"}
- Application B with metadata: {name: "environ", value: "prod"}

**Test Steps:**
1. Call with `metadataName="environ"` and `metadataValue="production"`
2. Verify returns empty (name doesn't match)
3. Call with `metadataName="environment"` and `metadataValue="prod"`
4. Verify returns empty (value doesn't match)

**Expected Results:**
- Exact match required for both name and value
- No substring matching

---

### Test Case 4.1.5: Application with Multiple Metadata Entries

**Objective:** Verify matching works when application has multiple metadata entries.

**Prerequisites:**
- Application with metadata:
  - {name: "environment", value: "production"}
  - {name: "team", value: "security"}
  - {name: "criticality", value: "high"}

**Test Steps:**
1. Call with `metadataName="environment"` and `metadataValue="production"`
2. Call with `metadataName="team"` and `metadataValue="security"`
3. Call with `metadataName="criticality"` and `metadataValue="high"`

**Expected Results:**
- All three calls return the same application
- Any matching metadata entry qualifies the application

---

### 4.2 Metadata Name Only Tests

### Test Case 4.2.1: Metadata Name Only - Basic Filter

**Objective:** Verify filtering by metadata name alone (ignoring value).

**Prerequisites:**
- Applications with same metadata name but different values

**Test Steps:**
1. Call `search_applications` with `metadataName="environment"` (no metadataValue)
2. Verify ALL applications with "environment" metadata are returned
3. Verify different values ("dev", "staging", "prod") all included

**Expected Results:**
- Returns all applications with matching metadata name
- Value is ignored when metadataValue not specified
- Different values all included

---

### Test Case 4.2.2: Metadata Name Only - Case-Insensitive

**Objective:** Verify metadata name-only filter is case-insensitive.

**Test Steps:**
1. Call with `metadataName="environment"` (lowercase)
2. Call with `metadataName="ENVIRONMENT"` (uppercase)

**Expected Results:**
- Both calls return same applications
- Case-insensitive matching

---

## 5. Pagination Tests (NEW)

### Test Case 5.1: Default Pagination

**Objective:** Verify default pagination behavior.

**Test Steps:**
1. Call `search_applications` with no pagination parameters
2. Verify response includes pagination metadata

**Expected Results:**
- `page` = 1 (default)
- `pageSize` = 50 (default) or actual items if fewer
- `totalItems` = total matching applications
- `totalPages` calculated correctly

---

### Test Case 5.2: Custom Page Size

**Objective:** Verify pageSize parameter works correctly.

**Prerequisites:**
- Organization with 10+ applications

**Test Steps:**
1. Call `search_applications` with `pageSize=5`
2. Verify `data` array has at most 5 items
3. Verify `totalPages` is calculated correctly

**Expected Results:**
- At most 5 applications returned
- `totalPages` = ceil(totalItems / 5)
- Pagination metadata accurate

---

### Test Case 5.3: Page Navigation

**Objective:** Verify page parameter navigates through results.

**Prerequisites:**
- Organization with 10+ applications

**Test Steps:**
1. Call `search_applications` with `pageSize=3`, `page=1`
2. Note the applications returned
3. Call with `pageSize=3`, `page=2`
4. Verify different applications returned
5. Verify no overlap between pages

**Expected Results:**
- Page 1 and Page 2 contain different applications
- No duplicates across pages
- Pages are mutually exclusive

---

### Test Case 5.4: Page Beyond Data (Out of Bounds)

**Objective:** Verify behavior when page number exceeds available pages.

**Prerequisites:**
- Organization with fewer than 100 applications

**Test Steps:**
1. Call `search_applications` with `page=999`
2. Verify response structure

**Expected Results:**
- Returns empty `data` array `[]`
- `page` = 999 (requested)
- `totalItems` unchanged
- No error thrown

---

### Test Case 5.5: Maximum Page Size

**Objective:** Verify pageSize respects maximum of 100.

**Test Steps:**
1. Call `search_applications` with `pageSize=100`
2. Verify at most 100 items returned

**Expected Results:**
- Maximum 100 items per page
- Pagination metadata accurate

---

### Test Case 5.6: Invalid Pagination - Page Zero (NEW)

**Objective:** Verify behavior with invalid page=0.

**Test Steps:**
1. Call `search_applications` with `page=0`
2. Observe behavior

**Expected Results:**
- Either returns error/validation message, OR
- Treats as page 1 (graceful handling)
- Behavior is documented and consistent

---

### Test Case 5.7: Invalid Pagination - PageSize Exceeds Max (NEW)

**Objective:** Verify behavior when pageSize > 100.

**Test Steps:**
1. Call `search_applications` with `pageSize=200`
2. Observe behavior

**Expected Results:**
- Either returns error/validation message, OR
- Caps at 100 (graceful handling)
- Behavior is documented and consistent

---

## 6. Combined Filters Tests (NEW)

### Test Case 6.1: Name + Tag Filter

**Objective:** Verify combining name and tag filters.

**Prerequisites:**
- Application with name containing "Service" AND tag "production"

**Test Steps:**
1. Call `search_applications` with `name="Service"` and `tag="production"`
2. Verify returned applications satisfy BOTH criteria

**Expected Results:**
- Only applications matching BOTH filters returned
- Filters are ANDed together
- Results are intersection of both criteria

---

### Test Case 6.2: Name + Metadata Filter

**Objective:** Verify combining name and metadata filters.

**Prerequisites:**
- Application with name containing "App" AND metadata environment=production

**Test Steps:**
1. Call `search_applications` with `name="App"`, `metadataName="environment"`, `metadataValue="production"`
2. Verify returned applications satisfy ALL criteria

**Expected Results:**
- Only applications matching all filters returned
- Filters are ANDed together

---

### Test Case 6.3: Tag + Metadata Filter

**Objective:** Verify combining tag and metadata filters.

**Prerequisites:**
- Application with tag "critical" AND metadata tier=production

**Test Steps:**
1. Call `search_applications` with `tag="critical"`, `metadataName="tier"`, `metadataValue="production"`
2. Verify returned applications satisfy BOTH criteria

**Expected Results:**
- Filters are ANDed together
- Both criteria must match

---

### Test Case 6.4: All Filters Combined

**Objective:** Verify all filters work together.

**Prerequisites:**
- Application matching all criteria

**Test Steps:**
1. Call `search_applications` with all parameters: `name`, `tag`, `metadataName`, `metadataValue`
2. Verify results match ALL criteria

**Expected Results:**
- All filters ANDed together
- Very narrow result set (or empty if no perfect match)

---

## 7. Data Completeness Tests

### Test Case 7.1: Verify All 8 ApplicationData Fields

**Objective:** Verify all fields are populated correctly.

**Test Steps:**
1. Call `search_applications` with any filter
2. For each returned ApplicationData, verify:
   - `name` is non-null, non-empty string
   - `status` is non-null string
   - `appID` is non-null, valid UUID format
   - `lastSeenAt` is non-null, ISO-8601 formatted
   - `language` is non-null string
   - `metadata` is non-null list (may be empty)
   - `tags` is non-null list (may be empty)
   - `technologies` is non-null list (may be empty)

**Expected Results:**
- All 8 fields present
- No null values for required fields
- Collections are non-null (empty list acceptable)

---

### Test Case 7.2: Timestamp Formatting

**Objective:** Verify lastSeenAt uses ISO-8601 format with timezone offset.

**Test Steps:**
1. Call `search_applications` and examine `lastSeenAt` field
2. Verify format: `YYYY-MM-DDTHH:MM:SS±HH:MM`

**Expected Results:**
- Format: "2025-01-15T05:30:00-05:00"
- Includes timezone offset (not 'Z')
- Uses numeric offset (e.g., "-05:00" not "EST")

---

### Test Case 7.3: Metadata Structure

**Objective:** Verify metadata field structure.

**Test Steps:**
1. Find application with metadata
2. Verify metadata is List<Metadata>
3. Verify each Metadata has `name` and `value` fields

**Expected Results:**
- Metadata is List type
- Each entry has name and value
- No null entries within list

---

### Test Case 7.4: Tags Field Structure

**Objective:** Verify tags field is always a list.

**Test Steps:**
1. Find application with tags
2. Verify tags is List<String>
3. Find application without tags
4. Verify tags is empty list `[]` (not null)

**Expected Results:**
- Tags is always List type
- Never null
- Empty applications have `[]`

---

### Test Case 7.5: Technologies Field Structure

**Objective:** Verify technologies field is always a list.

**Test Steps:**
1. Find application with technologies
2. Verify technologies is List<String>
3. Find application without technologies
4. Verify technologies is empty list `[]` (not null)

**Expected Results:**
- Technologies is always List type
- Never null
- Empty applications have `[]`

---

## 8. Empty Results Tests

### Test Case 8.1: Name Filter - No Matches

**Objective:** Verify empty results for non-matching name.

**Test Steps:**
1. Call `search_applications` with `name="ZZZZZ_NONEXISTENT_12345"`

**Expected Results:**
- `data` is empty array `[]`
- `totalItems` = 0
- No errors

---

### Test Case 8.2: Tag Filter - No Matches

**Objective:** Verify empty results for non-matching tag.

**Test Steps:**
1. Call `search_applications` with `tag="totally-fake-tag-xyz"`

**Expected Results:**
- `data` is empty array `[]`
- `totalItems` = 0
- No errors

---

### Test Case 8.3: Metadata Filter - No Matches

**Objective:** Verify empty results for non-matching metadata.

**Test Steps:**
1. Call `search_applications` with `metadataName="nonexistent"` and `metadataValue="nothing"`

**Expected Results:**
- `data` is empty array `[]`
- `totalItems` = 0
- No errors

---

### Test Case 8.4: Combined Filters - No Matches

**Objective:** Verify empty results when combined filters match nothing.

**Test Steps:**
1. Call `search_applications` with conflicting filters (name exists, but tag doesn't match)

**Expected Results:**
- `data` is empty array `[]`
- `totalItems` = 0
- No errors

---

## 9. Input Validation and Edge Cases

### Test Case 9.1: Empty String - Name Filter

**Objective:** Verify behavior with empty string name.

**Test Steps:**
1. Call `search_applications` with `name=""`

**Expected Results:**
- Likely returns all applications (empty string contained in all)
- OR returns validation error
- Behavior is consistent

---

### Test Case 9.2: Empty String - Tag Filter

**Objective:** Verify behavior with empty string tag.

**Test Steps:**
1. Call `search_applications` with `tag=""`

**Expected Results:**
- Returns empty list (no app has "" as tag)
- OR returns validation error
- Behavior is consistent

---

### Test Case 9.3: Whitespace-Only Strings

**Objective:** Verify behavior with whitespace-only parameters.

**Test Steps:**
1. Call `search_applications` with `name="   "`
2. Call `search_applications` with `tag="   "`

**Expected Results:**
- Treated as literal search terms
- No automatic trimming (based on implementation)
- Behavior is consistent

---

### Test Case 9.4: Special Characters

**Objective:** Verify special characters are handled correctly.

**Test Steps:**
1. Call `search_applications` with `name="app-prod"` (hyphen)
2. Call `search_applications` with `tag="v1.0"` (period)
3. Call `search_applications` with `tag="app@prod"` (at symbol)

**Expected Results:**
- Special characters matched literally
- No escaping required
- Correct applications returned

---

### Test Case 9.5: Unicode Characters

**Objective:** Verify Unicode characters work correctly.

**Test Steps:**
1. Call `search_applications` with `name="应用"` (Chinese)
2. Call `search_applications` with `tag="crítico"` (Spanish)

**Expected Results:**
- Unicode handled correctly
- No encoding issues
- Query executes successfully

---

### Test Case 9.6: Very Long Strings

**Objective:** Verify long parameter values are handled.

**Test Steps:**
1. Call `search_applications` with `name="<500-character-string>"`

**Expected Results:**
- Query executes without error
- Returns empty list (no match expected)
- No truncation or buffer issues

---

### Test Case 9.7: SQL Injection Attempt (Defense in Depth)

**Objective:** Verify tool is safe from injection attacks.

**Test Steps:**
1. Call `search_applications` with `name="'; DROP TABLE applications; --"`
2. Call `search_applications` with `tag="1' OR '1'='1"`

**Expected Results:**
- Parameters treated as literal strings
- No database manipulation
- Query executes safely
- Returns empty list (or matches if literal string exists)

---

### Test Case 9.8: MetadataValue Without MetadataName

**Objective:** Verify behavior when metadataValue provided without metadataName.

**Test Steps:**
1. Call `search_applications` with `metadataValue="production"` (no metadataName)

**Expected Results:**
- Either returns validation error, OR
- Ignores metadataValue (only filters by metadataName if provided), OR
- Returns all applications (graceful handling)
- Behavior is documented and consistent

---

## 10. Error Handling Tests

### Test Case 10.1: Invalid Credentials

**Objective:** Verify proper error handling for authentication failures.

**Prerequisites:**
- Invalid credentials configured

**Test Steps:**
1. Configure with invalid credentials
2. Call `search_applications`

**Expected Results:**
- Appropriate error message returned
- No sensitive information in logs
- Clear authentication failure indication

---

### Test Case 10.2: Network Connectivity Issues

**Objective:** Verify error handling for network failures.

**Prerequisites:**
- Network issue simulation (unreachable host)

**Test Steps:**
1. Configure with unreachable host
2. Call `search_applications`

**Expected Results:**
- Clear error message about connection failure
- No infinite retries
- Appropriate exception handling

---

## 11. Performance Tests

### Test Case 11.1: Large Number of Applications

**Objective:** Verify performance with many applications.

**Prerequisites:**
- Organization with 100+ applications

**Test Steps:**
1. Call `search_applications` with no filters
2. Measure response time
3. Verify completes within 30 seconds

**Expected Results:**
- Query completes in reasonable time
- All applications paginated correctly
- No timeout errors

---

### Test Case 11.2: Large Metadata Sets

**Objective:** Verify performance with applications having many metadata entries.

**Prerequisites:**
- Application with 50+ metadata entries

**Test Steps:**
1. Call `search_applications` with metadata filter
2. Verify all metadata entries preserved in response

**Expected Results:**
- No truncation of metadata
- Response includes all entries
- Performance acceptable

---

### Test Case 11.3: Concurrent Requests

**Objective:** Verify concurrent query handling.

**Test Steps:**
1. Make 5 concurrent calls to `search_applications` with different parameters
2. Verify all calls complete successfully
3. Verify results are independent

**Expected Results:**
- All requests succeed
- Results are consistent
- No race conditions or data mixing

---

## 12. Integration Tests

### Test Case 12.1: MCP Protocol Integration

**Objective:** Verify tool works correctly through MCP protocol.

**Prerequisites:**
- MCP server running
- MCP client connected

**Test Steps:**
1. List available tools via MCP
2. Verify `search_applications` appears
3. Call tool via MCP protocol
4. Verify response format matches specification

**Expected Results:**
- Tool discoverable via MCP
- Response is valid JSON
- Pagination metadata included

---

### Test Case 12.2: Cross-Tool Consistency

**Objective:** Verify results are consistent with other application tools.

**Prerequisites:**
- Access to get_session_metadata tool

**Test Steps:**
1. Call `search_applications` to get application IDs
2. Use returned appIDs with other tools
3. Verify appIDs are valid and usable

**Expected Results:**
- AppIDs returned are valid for other tool calls
- Cross-tool integration works smoothly

---

## Test Data Requirements

### Minimal Test Environment
- Organization with 5-10 applications
- At least 1 application with complete data (all fields)
- At least 1 application with minimal data (empty metadata/tags)
- Applications with various names (for partial matching tests)
- Applications with various tags (including case variations)
- Applications with metadata entries

### Comprehensive Test Environment
- 100+ applications
- Applications with:
  - Common name fragments (e.g., "Service", "App")
  - Tags with case variations ("Production", "production")
  - Multiple metadata entries
  - Various languages
  - Complete and minimal data sets
- Empty organization (for edge case testing)

---

## Test Execution Guidelines

### For AI Test Executors

**1. Discovery Phase:**
- Call `search_applications` with no filters to see all applications
- Document application names, tags, and metadata
- Identify test data that matches test requirements

**2. Test Selection:**
- Map available data to test requirements
- Skip tests if required data doesn't exist
- Document skipped tests with reasons

**3. Test Execution:**
- Document parameters used
- Document expected results based on discovery
- Execute tool call
- Compare actual vs expected
- Document pass/fail

**4. Result Reporting:**
- Total tests attempted
- Tests passed
- Tests failed (with details)
- Tests skipped (with reasons)
- Unexpected behaviors found

---

## Success Criteria

A test passes if:
- Tool executes without unexpected errors
- Returned applications match filter criteria
- Pagination metadata is accurate
- Response structure matches specification
- Edge cases handled gracefully
- Filter logic is correct (case sensitivity, match type)

### Quality Metrics
- Zero NullPointerExceptions
- Zero data field omissions
- 100% consistent results on repeated calls
- Correct case sensitivity behavior (CRITICAL)

---

## Known Limitations

1. **Caching:** Application data cached for 5 minutes - recent changes may not appear immediately
2. **In-Memory Filtering:** Filters applied after retrieval - very large organizations may see performance impact
3. **No Sorting:** Results returned in SDK order, no custom sorting available
4. **No Wildcard/Regex:** Only literal string matching (via contains() for name, exact for tag/metadata)
5. **Single Tag Filter:** Only one tag can be filtered at a time (no AND/OR logic for multiple tags)
6. **MetadataValue Requires MetadataName:** Cannot filter by value without specifying name

---

## Appendix: Filter Logic Reference

```java
// Name filter: Case-INSENSITIVE, partial matching
app.getName().toLowerCase().contains(name.toLowerCase())

// Tag filter: Case-SENSITIVE, exact matching
app.getTags().contains(tag)

// Metadata filter (name+value): Case-INSENSITIVE, exact matching
metadata.getName().equalsIgnoreCase(metadataName) &&
metadata.getValue().equalsIgnoreCase(metadataValue)

// Metadata filter (name only): Case-INSENSITIVE, exact matching
metadata.getName().equalsIgnoreCase(metadataName)
```

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0 | 2026-01-02 | Initial consolidated plan (replaces 5 old plans) |

---

## Consolidated From

This test plan consolidates the following legacy test plans:
1. `test-plan-list_all_applications.md` (created 2025-10-21)
2. `test-plan-list_applications_with_name.md` (created 2025-10-21)
3. `test-plan-get_applications_by_tag.md` (created 2025-10-21)
4. `test-plan-get_applications_by_metadata.md` (created 2025-10-21)
5. `test-plan-get_applications_by_metadata_name.md` (created 2025-10-21)

All test cases from the original plans have been preserved and organized by filter type for clearer execution flow.
