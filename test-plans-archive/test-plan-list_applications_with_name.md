# Test Plan: list_applications_with_name Tool

## Overview
This test plan provides comprehensive testing instructions for the `list_applications_with_name` MCP tool. The tool takes an application name and returns applications that contain that name in their title, using case-insensitive partial matching.

**Testing Approach:** Each test case describes the type of test data needed, how to identify it in the Contrast installation, and how to verify the results. Tests are designed to be executed by an AI agent that will:
1. Query the Contrast installation to find data matching test requirements
2. Execute the tool with appropriate parameters
3. Verify the results meet expected criteria

**Tool Signature:**
- **Name:** `list_applications_with_name`
- **Input:** `app_name` (String) - The application name or name fragment to search for
- **Output:** List of ApplicationData objects containing applications that match the name
- **Filter Logic:** Uses `app.getName().toLowerCase().contains(app_name.toLowerCase())` - case-insensitive partial matching

---

## 1. Basic Functionality Testing

### 1.1 Exact Name Match - Single Application
**Objective:** Verify the tool correctly finds an application by its exact name.

**Test Data Requirements:**
- At least 3-5 applications exist in the organization
- At least 1 application with a known unique name (e.g., "WebGoat", "PetStore", "OrderService")

**Test Steps:**
1. Use `list_all_applications` to identify applications and their names
2. Select an application with a unique name (e.g., "WebGoat")
3. Call `list_applications_with_name` with `app_name="WebGoat"` (exact name)
4. Verify exactly 1 application is returned (assuming no other apps contain "WebGoat" in their name)
5. Verify it's the correct application
6. Verify the name field matches expectations

**Expected Results:**
- Returns the application with exact name match
- All ApplicationData fields are populated correctly (name, status, appID, lastSeenAt, language, metadata, tags, technologies)
- Query executes successfully

---

### 1.2 Partial Name Match - Single Fragment
**Objective:** Verify the tool finds applications containing a name fragment.

**Test Data Requirements:**
- Multiple applications with names containing common fragments (e.g., "Service", "API", "App")
- Example: "OrderService", "PaymentService", "UserService" all contain "Service"

**Test Steps:**
1. Use `list_all_applications` to identify applications with names containing a common fragment
2. Count how many applications contain the fragment (e.g., "Service")
3. Call `list_applications_with_name` with `app_name="Service"`
4. Verify all applications containing "Service" in their name are returned
5. Verify the count matches the expected number from step 2
6. Verify NO applications without "Service" in their name are returned

**Expected Results:**
- Returns all applications whose names contain the search fragment
- Partial matching works correctly
- Case-insensitive matching works (see test 2.1 for details)
- No false positives or false negatives

---

### 1.3 Multiple Applications with Similar Names
**Objective:** Verify all matching applications are returned, not just the first match.

**Test Data Requirements:**
- At least 3-5 applications with similar names (e.g., "app-frontend", "app-backend", "app-api" all contain "app")

**Test Steps:**
1. Use `list_all_applications` to identify applications with similar names
2. Count applications containing a common substring (e.g., "app")
3. Call `list_applications_with_name` with `app_name="app"`
4. Verify all applications with "app" in their name are returned
5. Verify the count matches expectations
6. Verify each returned application has "app" somewhere in its name

**Expected Results:**
- All matching applications are returned
- No applications are missing
- No duplicate applications in results
- Results are comprehensive

---

## 2. Partial Matching and Case Sensitivity Testing

### 2.1 Case Insensitive Matching - Lowercase Search
**Objective:** Verify search is case-insensitive when searching with lowercase.

**Test Data Requirements:**
- At least 1 application with mixed-case name (e.g., "WebGoat", "OrderService", "MyApp")

**Test Steps:**
1. Use `list_all_applications` to identify an application with mixed-case name (e.g., "WebGoat")
2. Call `list_applications_with_name` with `app_name="webgoat"` (all lowercase)
3. Verify the "WebGoat" application is returned
4. Call `list_applications_with_name` with `app_name="WEBGOAT"` (all uppercase)
5. Verify the "WebGoat" application is returned
6. Call `list_applications_with_name` with `app_name="WebGoat"` (mixed case)
7. Verify the "WebGoat" application is returned

**Expected Results:**
- Search is case-insensitive
- "webgoat", "WEBGOAT", "WebGoat", "wEbGoAt" all match "WebGoat"
- `.toLowerCase()` is applied to both search term and application name
- Case variations all produce the same results

---

### 2.2 Case Insensitive Matching - Partial Fragments
**Objective:** Verify case-insensitive matching works for partial fragments.

**Test Data Requirements:**
- Applications with names like "OrderService", "PaymentService", "UserService"

**Test Steps:**
1. Use `list_all_applications` to identify applications with "Service" in their name
2. Call `list_applications_with_name` with `app_name="service"` (lowercase)
3. Verify all applications with "Service" are returned
4. Call `list_applications_with_name` with `app_name="SERVICE"` (uppercase)
5. Verify the same applications are returned
6. Call `list_applications_with_name` with `app_name="SeRvIcE"` (random case)
7. Verify the same applications are returned

**Expected Results:**
- Case-insensitive matching works for partial fragments
- All case variations return the same results
- Matching is consistent regardless of case

---

### 2.3 Beginning of Name Match
**Objective:** Verify matching works when search term appears at the beginning of the name.

**Test Data Requirements:**
- Applications with names starting with common prefixes (e.g., "app-frontend", "app-backend", "app-mobile")

**Test Steps:**
1. Use `list_all_applications` to identify applications starting with "app-"
2. Count applications with this prefix
3. Call `list_applications_with_name` with `app_name="app-"`
4. Verify all applications starting with "app-" are returned
5. Verify applications without this prefix are excluded

**Expected Results:**
- Matching at beginning of name works
- Prefix matching is supported via contains() logic
- All applications with matching prefix are returned

---

### 2.4 End of Name Match
**Objective:** Verify matching works when search term appears at the end of the name.

**Test Data Requirements:**
- Applications with names ending with common suffixes (e.g., "payment-api", "order-api", "user-api")

**Test Steps:**
1. Use `list_all_applications` to identify applications ending with "-api"
2. Count applications with this suffix
3. Call `list_applications_with_name` with `app_name="-api"`
4. Verify all applications ending with "-api" are returned
5. Verify applications without this suffix are excluded

**Expected Results:**
- Matching at end of name works
- Suffix matching is supported via contains() logic
- All applications with matching suffix are returned

---

### 2.5 Middle of Name Match
**Objective:** Verify matching works when search term appears in the middle of the name.

**Test Data Requirements:**
- Applications with names containing fragments in the middle (e.g., "my-service-app", "api-service-proxy")

**Test Steps:**
1. Use `list_all_applications` to identify applications with "service" in the middle of the name
2. Call `list_applications_with_name` with `app_name="service"`
3. Verify applications with "service" in the middle are returned
4. Verify matching is not limited to beginning or end

**Expected Results:**
- Matching works anywhere in the name (beginning, middle, or end)
- `.contains()` logic supports matches at any position
- All matching applications are returned regardless of position

---

### 2.6 Single Character Search
**Objective:** Verify the tool handles single character searches.

**Test Data Requirements:**
- Applications with names containing common single letters (e.g., "a", "e", "1")

**Test Steps:**
1. Use `list_all_applications` to count applications with "a" in their name
2. Call `list_applications_with_name` with `app_name="a"`
3. Verify all applications containing "a" are returned
4. Verify this is likely many applications
5. Verify results are reasonable

**Expected Results:**
- Single character searches work
- May return many results (most names contain common letters)
- Query executes without error
- No minimum search length validation

---

### 2.7 Very Long Application Name
**Objective:** Verify the tool handles applications with very long names.

**Test Data Requirements:**
- If possible, an application with a long name (50+ characters)

**Test Steps:**
1. Use `list_all_applications` to identify applications with long names
2. If found, call `list_applications_with_name` with the full long name
3. Verify the application is returned
4. Call with a fragment from the middle of the long name
5. Verify the application is still returned

**Expected Results:**
- Long application names are handled correctly
- Partial matching works with long names
- No truncation or string length issues
- Query completes successfully

---

## 3. Data Completeness Testing

### 3.1 All ApplicationData Fields Present
**Objective:** Verify each returned application contains all expected fields.

**Test Data Requirements:**
- At least 1 application with comprehensive data (name, status, ID, tags, metadata, technologies)

**Test Steps:**
1. Call `list_applications_with_name` with a search term that matches at least 1 application
2. Examine the first ApplicationData object in the response
3. Verify it contains all expected fields:
   - `name` (String) - Application name
   - `status` (String) - Application status
   - `appID` (String) - Unique identifier
   - `lastSeenAt` (String) - ISO timestamp of last activity
   - `language` (String) - Primary programming language
   - `metadata` (List<Metadata>) - Metadata key-value pairs
   - `tags` (List<String>) - Tag strings
   - `technologies` (List<String>) - Technology/framework strings
4. Verify fields have appropriate values
5. Verify the structure matches ApplicationData specification

**Expected Results:**
- All 8 fields are present in each ApplicationData object
- Field types match specification
- Data is complete and well-formed
- No missing or null required fields

---

### 3.2 Name Field Accuracy
**Objective:** Verify the name field is accurate and contains the search term.

**Test Data Requirements:**
- Applications with various names

**Test Steps:**
1. Call `list_applications_with_name` with `app_name="service"`
2. For each returned application, verify:
   - The `name` field exists and is not null/empty
   - The `name` field contains "service" (case-insensitive)
3. Verify the name field accurately represents the application's name

**Expected Results:**
- Name field is always present and populated
- Name field contains the search term (case-insensitive)
- Name field is accurate

---

### 3.3 Status Field Values
**Objective:** Verify the status field contains valid status values.

**Test Data Requirements:**
- Applications with various statuses (active, offline, archived, etc.)

**Test Steps:**
1. Call `list_applications_with_name` with a search term matching multiple applications
2. Examine the `status` field for each application
3. Verify status field contains valid values (e.g., "Active", "Offline", "Archived")
4. Verify status field is not null or empty
5. Document the status values observed

**Expected Results:**
- Status field is always present
- Status values are valid Contrast status strings
- Status reflects the current application state
- No null or empty status values

---

### 3.4 AppID Field Uniqueness
**Objective:** Verify each application has a unique appID.

**Test Data Requirements:**
- At least 3-5 applications

**Test Steps:**
1. Call `list_applications_with_name` with a search term matching multiple applications
2. Extract the `appID` field from each application
3. Verify all appIDs are unique (no duplicates)
4. Verify appIDs are non-empty strings
5. Verify appIDs follow expected format (alphanumeric identifiers)

**Expected Results:**
- Each application has a unique appID
- No duplicate appIDs in results
- AppIDs are valid identifier strings
- AppIDs can be used with other tools

---

### 3.5 LastSeenAt Timestamp Format
**Objective:** Verify lastSeenAt field contains properly formatted ISO 8601 timestamp.

**Test Data Requirements:**
- Any applications

**Test Steps:**
1. Call `list_applications_with_name` with a search term matching at least 1 application
2. Examine the `lastSeenAt` field
3. Verify it follows ISO 8601 format with timezone offset (YYYY-MM-DDTHH:MM:SS+/-HH:MM)
4. Verify the timestamp is reasonable (not in distant future or past)
5. Verify the pattern matches: `\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}[+-]\d{2}:\d{2}`

**Expected Results:**
- lastSeenAt is in ISO 8601 format
- Includes timezone offset
- Format: "2025-01-15T10:30:00+00:00" or similar
- Timestamp is reasonable and valid

---

### 3.6 Language Field
**Objective:** Verify language field contains the application's programming language.

**Test Data Requirements:**
- Applications built in various languages (Java, .NET, Node.js, Python, etc.)

**Test Steps:**
1. Call `list_applications_with_name` with a search term matching applications in different languages
2. Examine the `language` field for each application
3. Verify language field contains valid values (e.g., "Java", "DotNet", "Node", "Python", "Ruby")
4. Verify language field is not null (should have a value)
5. Document the language values observed

**Expected Results:**
- Language field is present for all applications
- Language values are valid Contrast language identifiers
- Language reflects the application's actual technology
- Language field is useful for filtering/grouping

---

### 3.7 Metadata Field Structure
**Objective:** Verify metadata field contains list of Metadata objects.

**Test Data Requirements:**
- At least 1 application with metadata entries
- At least 1 application with no metadata (empty list)

**Test Steps:**
1. Call `list_applications_with_name` to find an application with metadata
2. Examine the `metadata` field
3. Verify it's a List of Metadata objects
4. Verify each Metadata object has `name` and `value` fields
5. Find an application with no metadata
6. Verify its metadata field is an empty list [], not null

**Expected Results:**
- Metadata field is always a List (never null)
- Metadata objects have name and value fields
- Applications without metadata have empty list []
- Metadata structure is consistent

---

### 3.8 Tags Field Structure
**Objective:** Verify tags field contains list of tag strings.

**Test Data Requirements:**
- At least 1 application with multiple tags
- At least 1 application with no tags (empty list)

**Test Steps:**
1. Call `list_applications_with_name` to find an application with tags
2. Examine the `tags` field
3. Verify it's a List of String values
4. Verify tags are readable strings (e.g., "production", "critical", "team-alpha")
5. Find an application with no tags
6. Verify its tags field is an empty list [], not null

**Expected Results:**
- Tags field is always a List (never null)
- Tags are string values
- Applications without tags have empty list []
- Tags field matches application's actual tags

---

### 3.9 Technologies Field Structure
**Objective:** Verify technologies field contains list of technology/framework strings.

**Test Data Requirements:**
- At least 1 application with detected technologies
- At least 1 application with no detected technologies

**Test Steps:**
1. Call `list_applications_with_name` to find an application with technologies
2. Examine the `technologies` field (also referred to as `techs` in code)
3. Verify it's a List of String values
4. Verify technologies are recognizable names (e.g., "Spring", "Hibernate", "JSP", "Servlet")
5. Find an application with no detected technologies
6. Verify its technologies field is an empty list [], not null

**Expected Results:**
- Technologies field is always a List (never null)
- Technologies are string values representing frameworks/libraries
- Applications without detected technologies have empty list []
- Technologies reflect actual tech stack used by application

---

## 4. Cache Invalidation Testing

### 4.1 Cache Invalidation on Empty Results
**Objective:** Verify the tool clears cache and retries when no results found.

**Test Data Requirements:**
- At least 3-5 applications exist in the organization

**Test Steps:**
1. Call `list_applications_with_name` with `app_name="NonexistentAppXYZ123"` (should match nothing)
2. Verify empty list [] is returned
3. Examine logs at /tmp/mcp-contrast.log
4. Look for evidence of cache invalidation:
   - Initial search returns 0 results
   - Cache is cleared (look for cache clearing log message)
   - Search is performed again
5. Verify the query completes successfully despite no matches

**Expected Results:**
- Tool handles no-match scenario gracefully
- Cache is cleared when initial search yields empty results
- Search is retried after cache clearing
- Empty list [] is returned (not null)
- No errors thrown

**Implementation Note:** The code has logic:
```java
if(filteredApps.isEmpty()) {
    SDKHelper.clearApplicationsCache();
    // retry search
}
```

---

### 4.2 Cache Works for Subsequent Searches
**Objective:** Verify caching improves performance for repeated searches.

**Test Data Requirements:**
- Any applications

**Test Steps:**
1. Call `list_applications_with_name` with `app_name="service"` (first call)
2. Note the response time
3. Immediately call `list_applications_with_name` with `app_name="service"` (second call)
4. Note the response time
5. Verify the second call is faster (or same speed, using cache)
6. Verify results are identical between both calls
7. Examine logs to verify cache usage

**Expected Results:**
- Second call uses cached application data
- Response times are comparable or faster on second call
- Results are identical and consistent
- Caching works correctly via `SDKHelper.getApplicationsWithCache()`

---

### 4.3 Cache Cleared Only When Needed
**Objective:** Verify cache is NOT cleared when results are found.

**Test Data Requirements:**
- Applications that will match the search term

**Test Steps:**
1. Call `list_applications_with_name` with a search term that matches applications (e.g., "app")
2. Verify applications are returned (non-empty results)
3. Examine logs at /tmp/mcp-contrast.log
4. Verify cache clearing did NOT occur (only clears on empty results)
5. Verify the cache is used efficiently

**Expected Results:**
- Cache is NOT cleared when results are found
- Cache clearing only happens on empty results (as per implementation)
- Efficient cache usage for successful queries
- No unnecessary cache invalidation

---

### 4.4 Cache Behavior Across Different Searches
**Objective:** Verify cache is shared across different search terms.

**Test Data Requirements:**
- Applications with various names

**Test Steps:**
1. Call `list_applications_with_name` with `app_name="service"` (populates cache)
2. Call `list_applications_with_name` with `app_name="app"` (uses same cache)
3. Verify both queries work correctly
4. Verify the application list is cached, not the search results
5. Verify different search terms filter the same cached application list

**Expected Results:**
- Cache stores the full application list
- Different search terms filter the same cached data
- No separate caches per search term
- Efficient reuse of cached application data

---

## 5. Empty Results Testing

### 5.1 No Matching Applications
**Objective:** Verify behavior when no applications match the search term.

**Test Data Requirements:**
- Applications exist in the organization
- None have a specific search term in their name (e.g., "ZZZ_NONEXISTENT")

**Test Steps:**
1. Use `list_all_applications` to verify applications exist
2. Choose a search term that doesn't appear in any application name
3. Call `list_applications_with_name` with `app_name="ZZZ_NONEXISTENT"`
4. Verify an empty list [] is returned (not null)
5. Verify no error is thrown
6. Verify the response is a valid empty list structure

**Expected Results:**
- Returns empty list [] when no matches
- No exceptions or errors
- Empty list, not null
- Tool completes successfully

---

### 5.2 Search Term Not in Any Application
**Objective:** Verify behavior with a valid search term that just doesn't match.

**Test Data Requirements:**
- Applications with various names

**Test Steps:**
1. Use `list_all_applications` to examine all application names
2. Identify a string that doesn't appear in any name (e.g., "QQQQQ")
3. Call `list_applications_with_name` with `app_name="QQQQQ"`
4. Verify empty list [] is returned
5. Verify cache invalidation occurs (see test 4.1)
6. Verify no error message

**Expected Results:**
- Empty list returned
- Cache invalidation logic triggers
- No errors or warnings
- Query executes successfully

---

### 5.3 Organization with No Applications
**Objective:** Verify behavior when organization has no applications at all.

**Test Data Requirements:**
- Organization with zero applications (or use test organization if available)

**Test Steps:**
1. Use `list_all_applications` to verify organization has no applications
2. Call `list_applications_with_name` with `app_name="any"`
3. Verify empty list [] is returned
4. Verify no error is thrown
5. Verify tool completes successfully

**Expected Results:**
- Returns empty list when no applications exist
- No errors or exceptions
- Graceful handling of empty organization
- No null pointer exceptions

---

## 6. Input Validation and Edge Cases Testing

### 6.1 Empty String Search
**Objective:** Verify behavior when search term is an empty string.

**Test Data Requirements:**
- Applications exist in the organization

**Test Steps:**
1. Call `list_applications_with_name` with `app_name=""`
2. Observe behavior:
   - Option A: Returns all applications (empty string matches everything via contains)
   - Option B: Returns empty list
   - Option C: Returns validation error
3. Document the actual behavior
4. Verify no errors or crashes

**Expected Results:**
- Query executes without crashing
- Likely returns all applications (empty string is contained in all strings)
- No validation error for empty string
- Behavior should be consistent and documented

**Note:** In Java, `"anyString".contains("")` returns `true`, so all applications should match.

---

### 6.2 Whitespace-Only Search
**Objective:** Verify behavior when search term is only whitespace.

**Test Data Requirements:**
- Applications exist, likely none with whitespace-only names

**Test Steps:**
1. Call `list_applications_with_name` with `app_name="   "` (spaces only)
2. Verify query executes
3. Check if any applications are returned (unlikely unless app names contain spaces)
4. Call with `app_name="\t"` (tab character)
5. Verify query executes
6. Call with `app_name="\n"` (newline)
7. Verify query executes

**Expected Results:**
- Whitespace-only searches execute without error
- Returns applications whose names contain the whitespace character
- No automatic trimming (search term used as-is)
- No validation error

---

### 6.3 Null Search Parameter
**Objective:** Verify behavior when app_name parameter is null.

**Test Data Requirements:**
- Any applications exist

**Test Steps:**
1. Attempt to call `list_applications_with_name` with `app_name=null`
2. Observe behavior:
   - Option A: Throws NullPointerException
   - Option B: Returns validation error
   - Option C: Treats as empty search
3. Document the actual behavior
4. Verify tool does not crash the server

**Expected Results:**
- Should handle null gracefully with error message, OR
- Should throw appropriate exception, OR
- Should return error response
- Should NOT crash the tool or return incorrect results

**Note:** This test verifies error handling. Tool should not silently fail or return wrong results.

---

### 6.4 Very Long Search String
**Objective:** Verify behavior with unusually long search strings.

**Test Data Requirements:**
- None (testing edge case)

**Test Steps:**
1. Call `list_applications_with_name` with `app_name="<500-character-long-string>"`
2. Verify query executes without crashing
3. Verify returns empty list (assuming no application has a name this long)
4. Verify no buffer overflow or string length errors
5. Verify tool handles long strings gracefully

**Expected Results:**
- Query handles long strings gracefully
- No errors or crashes
- Returns empty list if no match (or matches if app name is also very long)
- No string length limitations cause issues

---

### 6.5 Special Characters in Search Term
**Objective:** Verify search terms with special characters are handled correctly.

**Test Data Requirements:**
- If possible, applications with special characters in names (e.g., "app-prod", "app.service", "app@v1")

**Test Steps:**
1. Use `list_all_applications` to identify applications with special characters in names
2. Call `list_applications_with_name` with `app_name="app-prod"` (hyphen)
3. Verify correct matching
4. Call `list_applications_with_name` with `app_name="app.service"` (period)
5. Verify correct matching
6. Call `list_applications_with_name` with `app_name="app@v1"` (at symbol)
7. Verify correct matching
8. Test other special characters: underscore, slash, parentheses, brackets

**Expected Results:**
- Special characters are matched exactly
- No special character escaping needed
- `.contains()` method handles special chars correctly
- No regex interpretation (literal string matching)

---

### 6.6 Unicode Characters in Search Term
**Objective:** Verify search terms with Unicode/international characters work correctly.

**Test Data Requirements:**
- If possible, applications with Unicode in names (e.g., "应用" (application in Chinese), "приложение" (application in Russian))

**Test Steps:**
1. If applications with Unicode names exist:
   - Call `list_applications_with_name` with the Unicode name
   - Verify correct applications are returned
2. If no Unicode names exist:
   - Call `list_applications_with_name` with `app_name="应用"`
   - Verify returns empty list (no error)
3. Verify Unicode characters are handled correctly
4. Verify no encoding issues

**Expected Results:**
- Unicode characters in search terms work correctly
- No encoding or character set issues
- Exact Unicode string matching works
- Case-insensitive matching works with Unicode (depends on locale)

---

### 6.7 Regex Special Characters (Not Interpreted)
**Objective:** Verify regex special characters are treated as literal, not regex patterns.

**Test Data Requirements:**
- Applications with various names

**Test Steps:**
1. Call `list_applications_with_name` with `app_name=".*"` (regex wildcard)
2. Verify it searches for literal ".*" string, not regex pattern
3. Verify it does NOT match all applications
4. Call `list_applications_with_name` with `app_name="app[0-9]"` (regex character class)
5. Verify it searches for literal "app[0-9]", not regex
6. Verify no regex interpretation occurs

**Expected Results:**
- Regex special characters are literal, not patterns
- ".*" does not act as wildcard
- "[0-9]" does not match digits
- Only literal string matching via `.contains()`
- No regex interpretation

---

### 6.8 SQL Injection Attempt (Security)
**Objective:** Verify tool is safe from SQL injection (defense in depth).

**Test Data Requirements:**
- None (security testing)

**Test Steps:**
1. Call `list_applications_with_name` with `app_name="'; DROP TABLE applications; --"`
2. Verify query executes safely
3. Verify no database error occurs
4. Verify returns empty list or matches (if app literally has this in name)
5. Call with `app_name="1' OR '1'='1"`
6. Verify safe execution
7. Verify no applications are incorrectly returned

**Expected Results:**
- SQL injection attempts are harmless
- Search term is treated as literal string
- No database manipulation occurs
- Query executes safely
- Returns correct results based on literal string matching

**Note:** Given the implementation (filter on in-memory list with `.contains()`), SQL injection should not be possible, but this test verifies defense in depth.

---

## 7. Response Structure and Consistency Testing

### 7.1 List Order Determinism
**Objective:** Verify the order of returned applications is consistent.

**Test Data Requirements:**
- At least 3-5 applications matching the same search term

**Test Steps:**
1. Call `list_applications_with_name` with a search term matching multiple applications
2. Note the order of returned applications
3. Call the same query again
4. Verify the order is the same as step 2
5. Call the query a third time
6. Verify consistent ordering across all calls

**Expected Results:**
- Application order is consistent across calls
- Order is deterministic (same query = same order)
- Order likely matches the order from SDK's application list
- No random or unstable ordering

---

### 7.2 Response Type - List, Not Null
**Objective:** Verify the response is always a List, never null.

**Test Data Requirements:**
- Various scenarios: applications matching, no applications matching

**Test Steps:**
1. Call `list_applications_with_name` with a search term that matches applications
2. Verify response is a List (not null)
3. Call `list_applications_with_name` with a search term that matches no applications
4. Verify response is an empty List [] (not null)
5. Verify in all cases, response is a valid List object

**Expected Results:**
- Response is always a List
- Never returns null
- Empty results return empty List []
- Safe to iterate over response without null checks

---

### 7.3 No Duplicate Applications
**Objective:** Verify each application appears only once in results.

**Test Data Requirements:**
- Applications matching a search term

**Test Steps:**
1. Call `list_applications_with_name` with a search term matching multiple applications
2. Examine the returned list
3. Check for duplicate appIDs
4. Verify each application appears exactly once
5. Verify no duplication in the results

**Expected Results:**
- Each application appears exactly once
- No duplicate appIDs in results
- No duplicate entries
- Results are deduplicated (if necessary)

---

### 7.4 ApplicationData Completeness for All Results
**Objective:** Verify ALL returned applications have complete data, not just the first one.

**Test Data Requirements:**
- At least 3-5 applications matching a search term

**Test Steps:**
1. Call `list_applications_with_name` with a search term matching multiple applications
2. For EACH application in the results:
   - Verify all 8 fields are present (name, status, appID, lastSeenAt, language, metadata, tags, technologies)
   - Verify name contains the search term (case-insensitive)
   - Verify fields are populated correctly
3. Verify data completeness is consistent across all results

**Expected Results:**
- Every application has complete data
- Not just the first or last application
- All fields present and populated for all results
- Consistent data quality across all returned applications

---

## 8. Integration Testing

### 8.1 Integration with list_all_applications
**Objective:** Verify list_applications_with_name returns subset of list_all_applications results.

**Test Data Requirements:**
- Multiple applications exist with various names

**Test Steps:**
1. Call `list_all_applications` to get all applications
2. Manually filter the applications by a specific name fragment (case-insensitive contains)
3. Call `list_applications_with_name` with the same name fragment
4. Verify the results from step 3 match the manual filter from step 2
5. Verify every application returned by list_applications_with_name is present in list_all_applications
6. Verify application fields match between both tools

**Expected Results:**
- list_applications_with_name returns subset of list_all_applications
- Results are consistent between tools
- Application data matches exactly
- No discrepancies in filtering logic

---

### 8.2 Chained Filtering - Combine with Other Filters
**Objective:** Verify results can be further filtered by other criteria.

**Test Data Requirements:**
- Applications with various names, tags, and metadata

**Test Steps:**
1. Call `list_applications_with_name` with `app_name="service"`
2. Note the returned applications and their tags
3. Manually identify which of these also have tag "production"
4. For verification, call `get_applications_by_tag` with `tag="production"`
5. Find the intersection (applications with "service" in name AND "production" tag)
6. Verify this workflow is practical for multi-criteria filtering

**Expected Results:**
- Tool can be used as first step in multi-criteria filtering
- Results can be manually filtered further by tags, metadata, status, etc.
- Tool is composable with other filtering tools
- Practical for complex filtering workflows

---

### 8.3 Application Identification for Further Operations
**Objective:** Verify tool can be used to find applications for further operations.

**Test Data Requirements:**
- Applications with specific names

**Test Steps:**
1. Use `list_applications_with_name` to find applications with "service" in the name
2. Extract the appID field from each returned application
3. Use these appIDs with other tools (e.g., `list_all_vulnerabilities` with appId parameter)
4. Verify the workflow is smooth and appIDs are valid
5. Verify identified applications can be operated on

**Expected Results:**
- Tool effectively identifies applications for further operations
- Returned appIDs work with other tools
- Smooth integration in multi-step workflows
- Name-based selection is useful for operational tasks

---

## 9. Performance Testing

### 9.1 Large Number of Applications
**Objective:** Verify performance with large application counts.

**Test Data Requirements:**
- Organization with 100+ applications

**Test Steps:**
1. Call `list_applications_with_name` with a common search term appearing in many names
2. Measure response time
3. Verify query completes within reasonable time (< 30 seconds)
4. Verify all matching applications are returned
5. Call with a search term matching few applications
6. Compare response times

**Expected Results:**
- Query completes in reasonable time even with many applications
- All matching applications are returned
- Performance is acceptable for large organizations
- Response time scales reasonably with application count

---

### 9.2 Search Term Matching Many Applications
**Objective:** Verify performance when search term matches most applications.

**Test Data Requirements:**
- Applications with names containing common substrings (e.g., "app", "service")

**Test Steps:**
1. Identify a common substring appearing in many application names
2. Call `list_applications_with_name` with this common substring
3. Verify many applications are returned
4. Measure response time
5. Verify query completes successfully
6. Verify all matching applications are returned

**Expected Results:**
- Query handles many matches efficiently
- All matching applications are returned
- Response time is reasonable
- No performance degradation with many matches

---

### 9.3 Concurrent Queries
**Objective:** Verify tool handles concurrent queries correctly.

**Test Data Requirements:**
- Multiple applications with different names

**Test Steps:**
1. Make multiple concurrent calls to `list_applications_with_name` with different search terms
2. Verify each call returns correct, independent results
3. Verify no data corruption or result mixing between queries
4. Verify no caching issues cause incorrect results

**Expected Results:**
- Concurrent queries work correctly
- Results are independent and correct
- No interference between queries
- Thread-safe operation (if applicable)

---

## 10. Logging and Debugging Testing

### 10.1 Log Messages - Success Case
**Objective:** Verify appropriate log messages for successful queries.

**Test Data Requirements:**
- Applications matching a search term
- Access to logs at /tmp/mcp-contrast.log

**Test Steps:**
1. Call `list_applications_with_name` with `app_name="service"`
2. Examine logs for:
   - INFO level log: "Listing active applications matching name: service"
   - DEBUG level log: "Retrieved X total applications from Contrast"
   - DEBUG level log for each match: "Found matching application - ID: ..., Name: ..., Status: ..."
   - INFO level log: "Found Y applications matching 'service'"
3. Verify log messages are helpful for debugging
4. Verify log messages contain relevant information

**Expected Results:**
- Appropriate log messages at INFO and DEBUG levels
- Logs contain search term and result counts
- Logs show each matched application (at DEBUG level)
- Logs are helpful for troubleshooting
- No sensitive data in logs (appIDs and names are OK)

---

### 10.2 Log Messages - Empty Results Case
**Objective:** Verify appropriate log messages when no matches found.

**Test Data Requirements:**
- Access to logs at /tmp/mcp-contrast.log

**Test Steps:**
1. Call `list_applications_with_name` with `app_name="NonexistentXYZ"`
2. Examine logs for:
   - INFO level log: "Listing active applications matching name: NonexistentXYZ"
   - DEBUG level log about applications retrieved
   - Evidence of cache clearing (when empty result triggers cache invalidation)
   - INFO level log: "Found 0 applications matching 'NonexistentXYZ'"
3. Verify empty results are logged appropriately
4. Verify cache invalidation is logged

**Expected Results:**
- Empty results are logged clearly
- Cache invalidation is logged when triggered
- No error messages for legitimate empty results
- Logs help understand why no results were found

---

### 10.3 Log Messages - Error Case
**Objective:** Verify appropriate log messages when errors occur.

**Test Data Requirements:**
- Scenario that causes an error (e.g., SDK unavailable, network issue)

**Test Steps:**
1. If possible, trigger an error (e.g., disconnect network, invalid credentials)
2. Call `list_applications_with_name` with any search term
3. Examine logs for:
   - ERROR level log: "Error listing applications matching name: ..."
   - Exception stack trace
   - Helpful error context
4. Verify error messages are informative

**Expected Results:**
- Errors are logged at ERROR level
- Error messages include search term and exception details
- Stack traces are present for debugging
- Error messages are helpful for troubleshooting
- No sensitive data leaked in error logs

---

## Test Execution Guidelines

### For AI Test Executors

1. **Discovery Phase:** Start by querying the Contrast installation to understand what data is available
   - Use `list_all_applications` to see what applications exist and their names
   - Examine the name field for each application
   - Identify patterns: common substrings, unique names, prefixes/suffixes, special characters
   - Note the variety of application names for test planning

2. **Test Data Mapping:** Map available data to test requirements
   - Create a matrix of which tests can be executed with available data
   - Identify data gaps that prevent certain tests
   - Document assumptions about data
   - Note any applications with interesting characteristics (long names, special chars, etc.)

3. **Test Execution:** For each test:
   - Document the search term used
   - Document the expected results based on prior data discovery
   - Execute the tool call
   - Compare actual results with expected
   - Verify all matching applications are returned
   - Verify no non-matching applications are returned
   - Document pass/fail and any discrepancies

4. **Result Reporting:** Provide summary:
   - Total tests attempted
   - Tests passed
   - Tests failed (with details)
   - Tests skipped (with reasons)
   - Any unexpected behaviors or bugs found
   - Performance observations

### Success Criteria

A test passes if:
- The tool executes without unexpected errors
- All returned applications contain the search term in their name (case-insensitive)
- All applications containing the search term are returned
- Response structure matches ApplicationData specification
- Edge cases are handled gracefully
- Filtering logic is correct (case-insensitive contains)
- Cache behavior works as expected

### Failure Scenarios

A test fails if:
- Tool crashes or throws unexpected exceptions
- Returned applications don't contain the search term in their names
- Applications matching the search term are missing from results
- False positives (applications that don't match are returned)
- False negatives (applications that match are not returned)
- Response structure is malformed or missing fields
- Null handling causes errors
- Case-insensitive matching doesn't work correctly
- Cache invalidation logic fails

---

## Appendix: Quick Reference

### Tool Parameters
- **app_name** (String, required): The application name or name fragment to search for
  - Case-insensitive partial matching
  - Uses `.toLowerCase().contains()` logic
  - Searches anywhere in the name (beginning, middle, end)
  - No minimum length requirement
  - No validation (any string accepted, including empty string)

### ApplicationData Fields
- **name**: Application name (String)
- **status**: Application status (String) - e.g., "Active", "Offline"
- **appID**: Unique application identifier (String)
- **lastSeenAt**: ISO 8601 timestamp of last activity (String) - format: YYYY-MM-DDTHH:MM:SS+/-HH:MM
- **language**: Primary programming language (String) - e.g., "Java", "DotNet", "Node"
- **metadata**: List of metadata key-value pairs (List<Metadata>)
- **tags**: List of tag strings (List<String>)
- **technologies**: List of technology/framework strings (List<String>) - also called "techs"

### Filter Logic
```java
applications.stream()
    .filter(app -> app.getName().toLowerCase().contains(app_name.toLowerCase()))
    .map(app -> new ApplicationData(...))
    .collect(Collectors.toList());
```
- Case-insensitive: Both name and search term converted to lowercase
- Partial matching: Uses `.contains()` - matches anywhere in name
- Returns empty list if no matches
- Triggers cache invalidation if empty list (then retries)

### Cache Behavior
- Uses `SDKHelper.getApplicationsWithCache()` to retrieve applications
- Cache is cleared (`SDKHelper.clearApplicationsCache()`) when empty results are found
- After cache clearing, search is retried
- Cache is shared across all search terms (stores full application list)
- Different search terms filter the same cached data

### Related Tools
- **list_all_applications**: Returns all applications without filtering
- **get_applications_by_tag**: Filters applications by tag instead of name
- **get_applications_by_metadata**: Filters applications by metadata key-value pairs

---

## Known Limitations

1. **No Exact Match Option**: The tool always uses partial matching via `.contains()`. There's no way to request exact-only matching.

2. **No Wildcard/Regex Support**: The tool does not support wildcards or regular expressions. Only literal substring matching via `.contains()`.

3. **No Sorting Options**: Results are returned in the order from the SDK. No custom sorting is available.

4. **Case-Insensitive Only**: No case-sensitive mode available. All searches are case-insensitive.

5. **Empty String Matches All**: Searching with empty string `""` will return all applications (since all strings contain empty string).

6. **Cache Invalidation on Empty**: Cache is cleared and search retried when no results found. This might cause slight delays for legitimate "no match" queries.

7. **In-Memory Filtering**: The tool retrieves all applications and filters in memory. For very large organizations (1000+ apps), this may be slower than API-level filtering.

8. **No Multi-Criteria Search**: Can only search by name. To filter by name AND other criteria (tags, metadata, etc.), multiple tool calls must be combined.

---

## Document Version
**Version:** 1.0
**Date:** 2025-10-21
**Author:** Claude Code (AI Assistant)
**Tool Version:** mcp-contrast 0.0.11
**Based on Code:** AssessService.java lines 317-352
**Implementation Details:**
- Case-insensitive partial matching: `app.getName().toLowerCase().contains(app_name.toLowerCase())`
- Uses SDK caching via `SDKHelper.getApplicationsWithCache()`
- Cache invalidation on empty results: `SDKHelper.clearApplicationsCache()` when `filteredApps.isEmpty()`
- Returns List<ApplicationData> with all 8 fields populated
- Timestamp formatting via `FilterHelper.formatTimestamp()`
- Metadata conversion via `getMetadataFromApp()`
