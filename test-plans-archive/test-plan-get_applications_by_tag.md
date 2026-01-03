# Test Plan: get_applications_by_tag Tool

## Overview
This test plan provides comprehensive testing instructions for the `get_applications_by_tag` MCP tool. The tool takes a tag name and returns applications that have that tag in their tags list.

**Testing Approach:** Each test case describes the type of test data needed, how to identify it in the Contrast installation, and how to verify the results. Tests are designed to be executed by an AI agent that will:
1. Query the Contrast installation to find data matching test requirements
2. Execute the tool with appropriate parameters
3. Verify the results meet expected criteria

**Tool Signature:**
- **Name:** `get_applications_by_tag`
- **Input:** `tag` (String) - The tag name to filter applications by
- **Output:** List of ApplicationData objects containing applications with the specified tag
- **Filter Logic:** Uses `app.tags().contains(tag)` - exact string match, case-sensitive

---

## 1. Basic Functionality Testing

### 1.1 Single Tag Match - Common Tag
**Objective:** Verify the tool correctly filters applications by a single tag.

**Test Data Requirements:**
- At least 3-5 applications exist in the organization
- At least 2-3 applications have the same tag (e.g., "production", "critical", "team-alpha")
- At least 1-2 applications do not have this tag

**Test Steps:**
1. Use `list_all_applications` to identify applications and their tags
2. Select a tag that exists on multiple applications (e.g., "production")
3. Note which applications have this tag and which don't
4. Call `get_applications_by_tag` with `tag="production"`
5. Verify ALL returned applications have "production" in their tags list
6. Verify NO applications without "production" tag are returned
7. Verify the count matches the expected number from step 1

**Expected Results:**
- Returns only applications containing the specified tag
- Applications without the tag are excluded
- All ApplicationData fields are populated correctly (name, status, appID, tags, etc.)

---

### 1.2 Single Tag Match - Unique Tag
**Objective:** Verify the tool works when only one application has the tag.

**Test Data Requirements:**
- At least 1 application with a unique tag (e.g., "legacy", "archived", "experimental")
- Other applications exist without this tag

**Test Steps:**
1. Use `list_all_applications` to identify an application with a unique tag
2. Note the application name and the unique tag
3. Call `get_applications_by_tag` with the unique tag
4. Verify exactly 1 application is returned
5. Verify it's the correct application
6. Verify the tag is present in the returned application's tags list

**Expected Results:**
- Returns exactly 1 application
- The returned application is the correct one
- No other applications are included

---

### 1.3 Multiple Applications with Same Tag
**Objective:** Verify all applications with a tag are returned, not just the first match.

**Test Data Requirements:**
- At least 5 applications exist with the same tag (e.g., "microservice", "api", "frontend")

**Test Steps:**
1. Use `list_all_applications` to identify all applications with a common tag
2. Count how many applications have this tag (should be 5+)
3. Call `get_applications_by_tag` with the common tag
4. Verify the count of returned applications matches the expected count
5. Verify each returned application has the tag in its tags list
6. Verify no application with the tag was excluded

**Expected Results:**
- All applications with the tag are returned
- No applications are missing from the results
- Results include all applications that match, regardless of other attributes

---

## 2. Tag Matching Behavior Testing

### 2.1 Case Sensitivity - Exact Match Required
**Objective:** Verify tag matching is case-sensitive.

**Test Data Requirements:**
- At least 1 application with a tag using specific casing (e.g., "Production" with capital P)
- Ideally, different applications with different casings (e.g., "Production", "production", "PRODUCTION")

**Test Steps:**
1. Use `list_all_applications` to identify an application with tag "Production" (capital P)
2. Call `get_applications_by_tag` with `tag="Production"` (exact match)
3. Verify the application is returned
4. Call `get_applications_by_tag` with `tag="production"` (lowercase)
5. Verify the application from step 1 is NOT returned (case mismatch)
6. Call `get_applications_by_tag` with `tag="PRODUCTION"` (uppercase)
7. Verify the application from step 1 is NOT returned (case mismatch)

**Expected Results:**
- Tag matching is case-sensitive
- "Production" ≠ "production" ≠ "PRODUCTION"
- Only exact case matches are returned
- No case-insensitive normalization occurs

---

### 2.2 Exact String Match - No Partial Matches
**Objective:** Verify the tool requires exact tag matches, not substring matches.

**Test Data Requirements:**
- Applications with tags that could match partially (e.g., "prod", "production", "prod-east")

**Test Steps:**
1. Use `list_all_applications` to identify applications with related tags:
   - App A with tag "prod"
   - App B with tag "production"
   - App C with tag "prod-east"
2. Call `get_applications_by_tag` with `tag="prod"`
3. Verify only App A is returned (exact match only)
4. Verify App B and App C are NOT returned (they contain "prod" but are not exact matches)
5. Call `get_applications_by_tag` with `tag="production"`
6. Verify only App B is returned
7. Verify App A and App C are NOT returned

**Expected Results:**
- Only exact string matches are returned
- No substring or partial matching occurs
- "prod" does not match "production" or "prod-east"
- The contains() method checks for exact tag string, not substring within tag

---

### 2.3 Whitespace Sensitivity
**Objective:** Verify whitespace in tags is significant for matching.

**Test Data Requirements:**
- Applications with tags containing whitespace (e.g., "team alpha", "high priority")
- Applications with similar tags without spaces (e.g., "teamalpha", "highpriority")

**Test Steps:**
1. Use `list_all_applications` to identify applications with whitespace in tags
2. Call `get_applications_by_tag` with `tag="team alpha"` (with space)
3. Verify only applications with exactly "team alpha" are returned
4. Call `get_applications_by_tag` with `tag="teamalpha"` (no space)
5. Verify only applications with exactly "teamalpha" are returned
6. Verify the two queries return different applications
7. Call `get_applications_by_tag` with `tag=" team alpha "` (leading/trailing spaces)
8. Verify NO applications are returned (or only those with exact match including spaces)

**Expected Results:**
- Whitespace is significant in matching
- "team alpha" ≠ "teamalpha"
- Leading/trailing spaces are part of the match
- No automatic trimming occurs

---

### 2.4 Special Characters in Tags
**Objective:** Verify tags with special characters are matched correctly.

**Test Data Requirements:**
- Applications with tags containing special characters (e.g., "team-alpha", "v1.0", "app@prod", "critical!")

**Test Steps:**
1. Use `list_all_applications` to identify applications with special character tags
2. Call `get_applications_by_tag` with `tag="team-alpha"` (hyphen)
3. Verify applications with this exact tag are returned
4. Call `get_applications_by_tag` with `tag="v1.0"` (period)
5. Verify correct matching
6. Call `get_applications_by_tag` with `tag="app@prod"` (at symbol)
7. Verify correct matching
8. Call `get_applications_by_tag` with `tag="critical!"` (exclamation)
9. Verify correct matching

**Expected Results:**
- Special characters are matched exactly
- No special character escaping needed
- Tags with hyphens, periods, @ symbols, etc. work correctly
- No URL encoding issues

---

## 3. Multiple Tags Per Application Testing

### 3.1 Application with Multiple Tags - Match One
**Objective:** Verify an application is returned if ANY of its tags match, even if it has multiple tags.

**Test Data Requirements:**
- At least 1 application with multiple tags (e.g., tags: ["production", "critical", "team-alpha"])

**Test Steps:**
1. Use `list_all_applications` to identify an application with multiple tags
2. Note all tags for this application (e.g., ["production", "critical", "team-alpha"])
3. Call `get_applications_by_tag` with `tag="production"` (first tag)
4. Verify the application is returned
5. Call `get_applications_by_tag` with `tag="critical"` (second tag)
6. Verify the application is returned
7. Call `get_applications_by_tag` with `tag="team-alpha"` (third tag)
8. Verify the application is returned
9. Call `get_applications_by_tag` with `tag="unrelated"` (not in list)
10. Verify the application is NOT returned

**Expected Results:**
- Application matches if ANY tag in its tags list matches
- Same application can be returned by multiple different tag queries
- All tags are checked, not just the first one

---

### 3.2 Multiple Applications with Overlapping Tags
**Objective:** Verify correct filtering when multiple applications share some but not all tags.

**Test Data Requirements:**
- App A with tags: ["production", "frontend", "critical"]
- App B with tags: ["production", "backend", "critical"]
- App C with tags: ["staging", "frontend", "low-priority"]

**Test Steps:**
1. Use `list_all_applications` to identify applications with overlapping tags
2. Call `get_applications_by_tag` with `tag="production"`
3. Verify App A and App B are returned, but NOT App C
4. Call `get_applications_by_tag` with `tag="frontend"`
5. Verify App A and App C are returned, but NOT App B
6. Call `get_applications_by_tag` with `tag="critical"`
7. Verify App A and App B are returned, but NOT App C
8. Call `get_applications_by_tag` with `tag="staging"`
9. Verify only App C is returned

**Expected Results:**
- Each query returns only applications with that specific tag
- Overlapping tags don't cause incorrect filtering
- Each application is evaluated independently

---

## 4. Empty Results Testing

### 4.1 No Applications with Tag
**Objective:** Verify behavior when no applications have the specified tag.

**Test Data Requirements:**
- Applications exist in the organization
- None of these applications have a specific tag (e.g., "nonexistent-tag")

**Test Steps:**
1. Use `list_all_applications` to verify applications exist
2. Examine all tags across all applications to confirm tag doesn't exist
3. Call `get_applications_by_tag` with `tag="nonexistent-tag"`
4. Verify an empty list is returned (not null, but empty list: [])
5. Verify no error is thrown
6. Verify the response is a valid empty list structure

**Expected Results:**
- Returns empty list [] when no matches
- No exceptions or errors
- Empty list, not null
- Tool completes successfully

---

### 4.2 Tag Never Used in Organization
**Objective:** Verify behavior with a tag that has never been used in the organization.

**Test Data Requirements:**
- Applications exist
- A tag name that has never been assigned to any application (e.g., "totally-fake-tag-12345")

**Test Steps:**
1. Use `list_all_applications` to get all applications and their tags
2. Verify the tag "totally-fake-tag-12345" does not appear in any tags list
3. Call `get_applications_by_tag` with `tag="totally-fake-tag-12345"`
4. Verify empty list [] is returned
5. Verify no error or warning message
6. Verify the query executes successfully

**Expected Results:**
- Empty list returned
- No validation error (tag existence is not validated)
- Query executes normally
- No difference in behavior from test 4.1

---

### 4.3 Empty Tag String
**Objective:** Verify behavior when tag parameter is an empty string.

**Test Data Requirements:**
- Applications exist, some may or may not have empty string as tag

**Test Steps:**
1. Call `get_applications_by_tag` with `tag=""`
2. Observe behavior:
   - If applications have empty string in their tags list, they should be returned
   - If no applications have empty string tag, empty list should be returned
3. Verify no error is thrown
4. Verify query executes (empty string is a valid search term)

**Expected Results:**
- Query executes successfully
- Returns applications with "" (empty string) in tags list
- No validation error for empty string
- Empty string is treated as valid tag value

---

### 4.4 Organization with No Applications
**Objective:** Verify behavior when organization has no applications at all.

**Test Data Requirements:**
- Organization with zero applications (or use test organization if available)

**Test Steps:**
1. Use `list_all_applications` to verify organization has no applications
2. Call `get_applications_by_tag` with `tag="any-tag"`
3. Verify empty list [] is returned
4. Verify no error is thrown
5. Verify tool completes successfully

**Expected Results:**
- Returns empty list when no applications exist
- No errors or exceptions
- Graceful handling of empty organization

---

## 5. Applications with No Tags Testing

### 5.1 Application with Empty Tags List
**Objective:** Verify applications with no tags are never returned.

**Test Data Requirements:**
- At least 1 application with empty tags list: tags = []
- At least 1 application with populated tags list

**Test Steps:**
1. Use `list_all_applications` to identify applications with empty tags lists
2. Note an application with tags = []
3. Call `get_applications_by_tag` with any tag value
4. Verify the application with empty tags is NOT returned
5. Call `get_applications_by_tag` with tags from other applications
6. Verify only applications with tags are returned
7. Confirm applications with empty tags lists never appear in any results

**Expected Results:**
- Applications with no tags never match any query
- Empty tags list [] does not contain any string
- Only applications with at least one tag can be returned

---

### 5.2 Application with Null Tags
**Objective:** Verify applications with null tags field are handled gracefully.

**Test Data Requirements:**
- If possible, an application with tags = null (may not exist in normal scenarios)

**Test Steps:**
1. Use `list_all_applications` to check for applications with null tags
2. If found, call `get_applications_by_tag` with any tag
3. Verify the application with null tags is either:
   - Excluded from results (preferred), or
   - Causes no error (handled gracefully)
4. Verify query completes successfully

**Expected Results:**
- Null tags are handled gracefully
- No NullPointerException or other errors
- Applications with null tags are excluded from results
- Query completes successfully

---

## 6. Input Validation Testing

### 6.1 Null Tag Parameter
**Objective:** Verify behavior when tag parameter is null.

**Test Data Requirements:**
- Any applications exist

**Test Steps:**
1. Call `get_applications_by_tag` with `tag=null`
2. Observe behavior:
   - Option A: Throws NullPointerException or validation error (expected Java behavior)
   - Option B: Treats as empty search and returns empty results
   - Option C: Returns validation error message
3. Document the actual behavior

**Expected Results:**
- Should handle null gracefully with error message, OR
- Should throw appropriate exception, OR
- Should treat as invalid input and return error
- Should NOT crash the tool or return incorrect results

**Note:** This test verifies error handling behavior. The tool should not silently return all applications or incorrect results.

---

### 6.2 Very Long Tag Name
**Objective:** Verify behavior with unusually long tag strings.

**Test Data Requirements:**
- None (testing edge case)

**Test Steps:**
1. Call `get_applications_by_tag` with `tag="<500-character-long-string>"`
2. Verify query executes without crashing
3. Verify returns empty list (assuming no tag this long exists)
4. Verify no buffer overflow or string length errors

**Expected Results:**
- Query handles long strings gracefully
- No errors or crashes
- Returns empty list if no match
- No string length limitations cause issues

---

### 6.3 Tag with Only Whitespace
**Objective:** Verify behavior when tag is only whitespace.

**Test Data Requirements:**
- Applications exist, likely none with whitespace-only tags

**Test Steps:**
1. Call `get_applications_by_tag` with `tag="   "` (spaces only)
2. Verify query executes
3. Verify returns empty list (unless an application actually has "   " as a tag)
4. Call with `tag="\t"` (tab character)
5. Verify query executes
6. Call with `tag="\n"` (newline)
7. Verify query executes

**Expected Results:**
- Whitespace-only tags are treated as valid search terms
- Query executes without error
- Returns matches only if exact whitespace string exists in tags
- No automatic trimming or validation

---

### 6.4 Tag with Unicode Characters
**Objective:** Verify tags with Unicode/international characters work correctly.

**Test Data Requirements:**
- If possible, applications with Unicode tags (e.g., "重要" (important in Chinese), "crítico" (critical in Spanish))

**Test Steps:**
1. If applications with Unicode tags exist:
   - Call `get_applications_by_tag` with the Unicode tag
   - Verify correct applications are returned
2. If no Unicode tags exist:
   - Call `get_applications_by_tag` with `tag="重要"`
   - Verify returns empty list (no error)
3. Verify Unicode characters are handled correctly
4. Verify no encoding issues

**Expected Results:**
- Unicode characters in tags work correctly
- No encoding or character set issues
- Exact Unicode string matching works
- Query completes successfully

---

### 6.5 Tag with SQL Injection Attempt
**Objective:** Verify tool is safe from SQL injection (defense in depth).

**Test Data Requirements:**
- None (security testing)

**Test Steps:**
1. Call `get_applications_by_tag` with `tag="'; DROP TABLE applications; --"`
2. Verify query executes safely
3. Verify no database error occurs
4. Verify returns empty list (no application has this tag)
5. Call with `tag="1' OR '1'='1"`
6. Verify safe execution
7. Verify no applications are incorrectly returned

**Expected Results:**
- SQL injection attempts are harmless
- Tag is treated as literal string for matching
- No database manipulation occurs
- Query executes safely
- Returns empty results (or matches only if an app literally has that tag)

**Note:** Given the implementation (filter on in-memory list), SQL injection should not be possible, but this test verifies defense in depth.

---

## 7. Response Structure Testing

### 7.1 ApplicationData Structure - All Fields Present
**Objective:** Verify each returned application contains all expected fields.

**Test Data Requirements:**
- At least 1 application with a known tag

**Test Steps:**
1. Call `get_applications_by_tag` with a tag that matches at least 1 application
2. Examine the first ApplicationData object in the response
3. Verify it contains all expected fields:
   - `name` (String)
   - `status` (String)
   - `appID` (String)
   - `lastSeenAt` (String, ISO timestamp)
   - `language` (String)
   - `metadata` (List of Metadata objects, may be empty)
   - `tags` (List of Strings, should contain the searched tag)
   - `technologies` (List of Strings, may be empty)
4. Verify fields have appropriate non-null values
5. Verify the searched tag is present in the `tags` field

**Expected Results:**
- All fields are present in each ApplicationData object
- Field types match specification
- The matching tag is confirmed in the tags list
- Data is complete and well-formed

---

### 7.2 Tags Field Contains Multiple Tags
**Objective:** Verify the tags field in response contains all tags, not just the matched one.

**Test Data Requirements:**
- At least 1 application with multiple tags

**Test Steps:**
1. Use `list_all_applications` to identify an application with multiple tags (e.g., ["prod", "critical", "team-a"])
2. Call `get_applications_by_tag` with one of the tags (e.g., `tag="prod"`)
3. Verify the application is returned
4. Examine the `tags` field in the returned ApplicationData
5. Verify ALL tags are present in the tags list, not just "prod"
6. Verify tags list matches the original tags list from step 1

**Expected Results:**
- The full tags list is returned for each application
- Not filtered to only show the matching tag
- Complete application data is returned
- Tags list is unchanged from original

---

### 7.3 List Order and Determinism
**Objective:** Verify the order of returned applications is consistent.

**Test Data Requirements:**
- At least 3-5 applications with the same tag

**Test Steps:**
1. Call `get_applications_by_tag` with a tag matching multiple applications
2. Note the order of returned applications
3. Call the same query again
4. Verify the order is the same as step 2
5. Call the query a third time
6. Verify consistent ordering across all calls

**Expected Results:**
- Application order is consistent across calls
- Order is deterministic (same query = same order)
- Order likely matches the order from `getAllApplications()` method
- No random or unstable ordering

---

### 7.4 Response Type - List, Not Null
**Objective:** Verify the response is always a List, never null.

**Test Data Requirements:**
- Various scenarios: applications with tag, no applications with tag, etc.

**Test Steps:**
1. Call `get_applications_by_tag` with a tag that matches applications
2. Verify response is a List (not null)
3. Call `get_applications_by_tag` with a tag that matches no applications
4. Verify response is an empty List [] (not null)
5. Verify in all cases, response is a valid List object

**Expected Results:**
- Response is always a List
- Never returns null
- Empty results return empty List []
- Safe to iterate over response without null checks

---

## 8. Integration Testing

### 8.1 Integration with list_all_applications
**Objective:** Verify get_applications_by_tag returns subset of list_all_applications results.

**Test Data Requirements:**
- Multiple applications exist with various tags

**Test Steps:**
1. Call `list_all_applications` to get all applications
2. Manually filter the applications by a specific tag (examine tags field)
3. Call `get_applications_by_tag` with the same tag
4. Verify the results from step 3 match the manual filter from step 2
5. Verify every application returned by get_applications_by_tag is present in list_all_applications
6. Verify application fields match between both tools

**Expected Results:**
- get_applications_by_tag returns subset of list_all_applications
- Results are consistent between tools
- Application data matches
- No discrepancies in filtering logic

---

### 8.2 Chained Filtering - Multiple Tag Queries
**Objective:** Verify multiple tag queries can be used to narrow down applications.

**Test Data Requirements:**
- Applications with various tag combinations

**Test Steps:**
1. Call `get_applications_by_tag` with `tag="production"`
2. Note the returned applications
3. Manually check which of these also have tag "critical"
4. Call `get_applications_by_tag` with `tag="critical"`
5. Find the intersection of results from steps 2 and 4
6. Verify this represents applications with both "production" AND "critical" tags

**Expected Results:**
- Tool can be called multiple times to narrow results
- Results can be manually intersected for AND logic
- Consistent results across multiple calls
- Tool is composable for complex filtering

**Note:** The tool itself does not support multiple tags in one call (no AND/OR logic), but multiple calls can be combined by the AI agent.

---

### 8.3 Tag-based Application Identification
**Objective:** Verify tool can be used to find applications for further operations.

**Test Data Requirements:**
- Applications with specific tags that correlate to operational needs

**Test Steps:**
1. Use `get_applications_by_tag` to find applications with tag "critical"
2. Extract the appID field from each returned application
3. Use these appIDs with other tools (e.g., `list_all_vulnerabilities` with appId filter)
4. Verify the workflow is smooth and appIDs are valid

**Expected Results:**
- Tool effectively identifies applications for further operations
- Returned appIDs work with other tools
- Smooth integration in multi-step workflows
- Tag-based selection is useful for operational tasks

---

## 9. Performance Testing

### 9.1 Large Number of Applications
**Objective:** Verify performance with large application counts.

**Test Data Requirements:**
- Organization with 100+ applications

**Test Steps:**
1. Call `get_applications_by_tag` with a common tag appearing on many applications
2. Measure response time
3. Verify query completes within reasonable time (< 30 seconds)
4. Verify all matching applications are returned
5. Call with a tag matching few applications
6. Compare response times

**Expected Results:**
- Query completes in reasonable time even with many applications
- All matching applications are returned
- Performance is acceptable for large organizations
- Response time scales reasonably with number of matches

---

### 9.2 Application with Many Tags
**Objective:** Verify performance when applications have many tags.

**Test Data Requirements:**
- At least 1 application with 20+ tags

**Test Steps:**
1. Use `list_all_applications` to identify an application with many tags
2. Call `get_applications_by_tag` with one of its tags
3. Verify the application is returned
4. Verify performance is acceptable
5. Verify all tags are included in the response

**Expected Results:**
- Large tag lists don't cause performance issues
- Application is correctly matched
- All tags are returned in the response
- No truncation or performance degradation

---

### 9.3 Concurrent Queries
**Objective:** Verify tool handles concurrent queries correctly.

**Test Data Requirements:**
- Multiple applications with different tags

**Test Steps:**
1. Make multiple concurrent calls to `get_applications_by_tag` with different tags
2. Verify each call returns correct, independent results
3. Verify no data corruption or result mixing between queries
4. Verify no caching issues cause incorrect results

**Expected Results:**
- Concurrent queries work correctly
- Results are independent and correct
- No interference between queries
- Thread-safe operation (if applicable)

---

## 10. Logging and Debugging

### 10.1 Log Messages
**Objective:** Verify appropriate log messages are generated.

**Test Data Requirements:**
- Access to logs at /tmp/mcp-contrast.log

**Test Steps:**
1. Call `get_applications_by_tag` with `tag="test-tag"`
2. Examine logs for:
   - INFO level log: "Retrieving applications with tag: test-tag"
   - DEBUG level log: "Retrieved X total applications, filtering by tag"
   - INFO level log: "Found Y applications with tag 'test-tag'"
3. Verify log messages are helpful for debugging
4. Verify log messages contain relevant information

**Expected Results:**
- Appropriate log messages at INFO and DEBUG levels
- Logs contain tag name and result counts
- Logs are helpful for troubleshooting
- No sensitive data in logs

---

## Test Execution Guidelines

### For AI Test Executors

1. **Discovery Phase:** Start by querying the Contrast installation to understand what data is available
   - Use `list_all_applications` to see what applications exist and what tags are used
   - Examine the tags field for each application
   - Identify patterns: common tags, unique tags, applications with multiple tags, applications with no tags

2. **Test Data Mapping:** Map available data to test requirements
   - Create a matrix of which tests can be executed with available data
   - Identify data gaps that prevent certain tests
   - Document assumptions about data

3. **Test Execution:** For each test:
   - Document the query parameters used
   - Document the expected results based on prior data discovery
   - Execute the tool call
   - Compare actual results with expected
   - Document pass/fail and any discrepancies

4. **Result Reporting:** Provide summary:
   - Total tests attempted
   - Tests passed
   - Tests failed (with details)
   - Tests skipped (with reasons)
   - Any unexpected behaviors or bugs found

### Success Criteria

A test passes if:
- The tool executes without unexpected errors
- The returned applications all contain the specified tag
- Applications without the tag are excluded
- Response structure matches ApplicationData specification
- Edge cases are handled gracefully
- Filtering logic is correct (exact match, case-sensitive)

### Failure Scenarios

A test fails if:
- Tool crashes or throws unexpected exceptions
- Returned applications don't all have the specified tag
- Applications with the tag are missing from results
- Filtering logic is incorrect (wrong case handling, partial matches, etc.)
- Response structure is malformed
- Null handling causes errors

---

## Appendix: Quick Reference

### Tool Parameters
- **tag** (String, required): The exact tag name to filter by
  - Case-sensitive exact match
  - No partial matching or wildcards
  - Whitespace significant
  - No validation (any string accepted)

### ApplicationData Fields
- **name**: Application name (String)
- **status**: Application status (String)
- **appID**: Unique application identifier (String)
- **lastSeenAt**: ISO timestamp of last activity (String)
- **language**: Primary programming language (String)
- **metadata**: List of metadata key-value pairs (List<Metadata>)
- **tags**: List of tag strings (List<String>)
- **technologies**: List of technology strings (List<String>)

### Filter Logic
```java
allApps.stream()
    .filter(app -> app.tags().contains(tag))
    .collect(Collectors.toList());
```
- Uses Java List.contains() method
- Exact string match required
- Case-sensitive
- Returns empty list if no matches

### Related Tools
- **list_all_applications**: Returns all applications without filtering
- **get_applications_by_metadata**: Filters by metadata key-value pairs instead of tags

---

## Known Limitations

1. **No Tag Validation**: The tool does not validate whether the tag exists in the organization. Invalid/nonexistent tags simply return empty results.

2. **No Multiple Tag Support**: The tool accepts only one tag. To find applications with multiple tags, multiple calls must be made and results intersected manually.

3. **No Wildcard/Regex**: The tool does not support wildcards or regular expressions. Only exact string matches work.

4. **No Sorting Options**: Results are returned in the order from `getAllApplications()`. No custom sorting is available.

5. **Case Sensitivity**: Tag matching is case-sensitive. "Production" and "production" are different tags. There is no case-insensitive mode.

6. **In-Memory Filtering**: The tool retrieves all applications and filters in memory. For very large organizations (1000+ apps), this may be slower than API-level filtering.

---

## Document Version
**Version:** 1.0
**Date:** 2025-10-21
**Author:** Claude Code (AI Assistant)
**Tool Version:** mcp-contrast 0.0.11
**Based on Code:** AssessService.java lines 357-369
