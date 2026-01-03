# Test Plan: get_applications_by_metadata_name Tool

## Overview
This test plan provides comprehensive testing instructions for the `get_applications_by_metadata_name` MCP tool. The tool retrieves applications that have metadata with a specified name, regardless of the metadata value.

**Testing Approach:** Each test case describes the type of test data needed, how to identify it in the Contrast installation, and how to verify the results. Tests are designed to be executed by an AI agent that will:
1. Query the Contrast installation to find data matching test requirements
2. Execute the tool with appropriate parameters
3. Verify the results meet expected criteria

**Tool Signature:**
- **Input:** `metadata_name` (String) - The name of the metadata to filter by
- **Output:** List of ApplicationData objects that have metadata with the specified name
- **Matching:** Case-insensitive metadata name matching
- **No Pagination:** Returns all matching applications

---

## 1. Basic Functionality Testing

### 1.1 Basic Metadata Name Filter - Single Match
**Objective:** Verify tool correctly filters applications by a metadata name that exists in at least one application.

**Test Data Requirements:**
- At least one application exists with metadata attached
- Identify a metadata name that exists (e.g., "environment", "team", "cost-center")

**Test Steps:**
1. Use `list_all_applications` to explore available applications and their metadata
2. Identify an application with metadata and note the metadata name (e.g., "environment")
3. Call `get_applications_by_metadata_name` with `metadata_name="environment"`
4. Verify response is a list of ApplicationData objects
5. Verify ALL returned applications have at least one metadata entry with name="environment"
6. Verify the metadata value can be anything (tool doesn't filter by value)
7. Compare count with applications that don't have this metadata to ensure filtering worked

**Expected Results:**
- Returns list of applications with matching metadata name
- All returned applications have metadata with the specified name
- Metadata values are not filtered (any value is acceptable)
- Applications without this metadata name are excluded

---

### 1.2 Multiple Applications with Same Metadata Name
**Objective:** Verify tool returns all applications that have the specified metadata name.

**Test Data Requirements:**
- Multiple applications exist with the same metadata name but potentially different values
- Example: 3+ applications with "environment" metadata (values might be "dev", "staging", "prod")

**Test Steps:**
1. Use `list_all_applications` to identify applications with a common metadata name
2. Count how many applications have this metadata name (e.g., "environment")
3. Call `get_applications_by_metadata_name` with `metadata_name="environment"`
4. Verify the count of returned applications matches the expected count
5. Verify each returned application has the "environment" metadata name
6. Verify different metadata values are all included (e.g., "dev", "staging", "prod")
7. Verify no duplicate applications in results

**Expected Results:**
- All applications with matching metadata name are returned
- Different metadata values don't affect inclusion (value-agnostic filtering)
- No duplicates in result set
- Count matches expected number

---

### 1.3 Metadata Name with Different Values
**Objective:** Verify tool returns applications regardless of metadata value, only matching by name.

**Test Data Requirements:**
- Multiple applications with same metadata name but distinctly different values
- Example: "owner" metadata with values "team-a", "team-b", "team-c"

**Test Steps:**
1. Identify a metadata name used across multiple applications with different values
2. Note the distinct values (e.g., "team-a", "team-b", "team-c")
3. Call `get_applications_by_metadata_name` with `metadata_name="owner"`
4. Verify ALL applications with "owner" metadata are returned, regardless of value
5. Verify presence of all different values in the returned applications
6. Confirm the tool did not filter by any specific value

**Expected Results:**
- All applications with matching metadata name returned
- All metadata values for that name are included
- Value diversity doesn't affect filtering
- Only the name matters, not the value

---

## 2. Case Sensitivity Testing

### 2.1 Case Insensitive Matching - Lowercase Query
**Objective:** Verify metadata name matching is case-insensitive when query uses lowercase.

**Test Data Requirements:**
- Applications with metadata names in various cases (e.g., "Environment", "ENVIRONMENT", "environment")

**Test Steps:**
1. Identify applications with metadata names that have mixed case (e.g., "Environment")
2. Call `get_applications_by_metadata_name` with `metadata_name="environment"` (lowercase)
3. Verify applications with "Environment" (capitalized) are returned
4. Verify applications with "ENVIRONMENT" (uppercase) are returned
5. Verify applications with "environment" (lowercase) are returned
6. Confirm case doesn't affect matching

**Expected Results:**
- Metadata name matching is case-insensitive
- "environment", "Environment", "ENVIRONMENT" all match
- All case variations are returned

---

### 2.2 Case Insensitive Matching - Uppercase Query
**Objective:** Verify metadata name matching is case-insensitive when query uses uppercase.

**Test Data Requirements:**
- Applications with metadata names in lowercase or mixed case

**Test Steps:**
1. Identify applications with metadata name in lowercase (e.g., "environment")
2. Call `get_applications_by_metadata_name` with `metadata_name="ENVIRONMENT"` (uppercase)
3. Verify applications with "environment" (lowercase) are returned
4. Verify case-insensitive matching works bidirectionally

**Expected Results:**
- Uppercase query matches lowercase metadata names
- Case-insensitive matching is bidirectional
- All matching applications returned regardless of case

---

### 2.3 Case Insensitive Matching - Mixed Case Query
**Objective:** Verify metadata name matching works with mixed case queries.

**Test Data Requirements:**
- Applications with consistent metadata name casing

**Test Steps:**
1. Identify applications with metadata name (e.g., "environment")
2. Call `get_applications_by_metadata_name` with `metadata_name="EnViRoNmEnT"` (mixed case)
3. Verify applications with any casing of "environment" are returned
4. Confirm mixed case queries work correctly

**Expected Results:**
- Mixed case query matches any case variation
- All matching applications returned
- Case-insensitive matching is robust

---

## 3. Empty Results Testing

### 3.1 No Applications with Specified Metadata Name
**Objective:** Verify behavior when no applications have the specified metadata name.

**Test Data Requirements:**
- A metadata name that doesn't exist in any application (e.g., "nonexistent-metadata")

**Test Steps:**
1. Use `list_all_applications` to confirm no applications have a certain metadata name
2. Call `get_applications_by_metadata_name` with `metadata_name="nonexistent-metadata"`
3. Verify response is an empty list (not null)
4. Verify no errors are thrown
5. Verify result is a valid empty list: `[]`

**Expected Results:**
- Returns empty list when no matches found
- No errors or exceptions
- Empty list is valid return value
- Graceful handling of no matches

---

### 3.2 No Applications in Organization
**Objective:** Verify behavior when organization has no applications at all.

**Test Data Requirements:**
- Organization with no applications (or test in isolated environment)

**Test Steps:**
1. Confirm organization has no applications using `list_all_applications`
2. Call `get_applications_by_metadata_name` with any `metadata_name="anything"`
3. Verify returns empty list
4. Verify no errors occur
5. Verify graceful handling

**Expected Results:**
- Returns empty list
- No errors or exceptions
- Handles missing applications gracefully

---

### 3.3 Applications Exist but None Have Metadata
**Objective:** Verify behavior when applications exist but none have any metadata.

**Test Data Requirements:**
- Applications that have no metadata attached

**Test Steps:**
1. Identify applications without metadata (metadata field is null or empty list)
2. Call `get_applications_by_metadata_name` with any `metadata_name="anything"`
3. Verify returns empty list
4. Verify applications without metadata are correctly excluded
5. Verify null metadata is handled gracefully

**Expected Results:**
- Returns empty list
- Applications without metadata are safely excluded
- Null metadata doesn't cause errors
- Graceful null handling

---

## 4. Validation and Error Handling Testing

### 4.1 Empty String Metadata Name
**Objective:** Verify behavior when metadata_name is an empty string.

**Test Data Requirements:**
- Any applications (may or may not have metadata)

**Test Steps:**
1. Call `get_applications_by_metadata_name` with `metadata_name=""`
2. Observe behavior:
   - Option A: Returns empty list (no metadata names match empty string)
   - Option B: Returns error message about invalid parameter
3. Verify no exceptions are thrown
4. Verify response is predictable and documented behavior

**Expected Results:**
- Returns empty list (most likely behavior given code implementation)
- No matching metadata names exist for empty string
- No errors or exceptions
- Consistent behavior

**Note:** The implementation uses `equalsIgnoreCase()` which will not match empty strings to non-empty metadata names.

---

### 4.2 Null Metadata Name
**Objective:** Verify behavior when metadata_name is null.

**Test Data Requirements:**
- Any applications

**Test Steps:**
1. Attempt to call `get_applications_by_metadata_name` with `metadata_name=null`
2. Observe behavior:
   - Option A: NullPointerException occurs
   - Option B: Returns empty list with graceful null handling
   - Option C: Returns error message about null parameter
3. Document actual behavior
4. Verify behavior is consistent

**Expected Results:**
- Likely throws NullPointerException (based on code using `equalsIgnoreCase()`)
- OR returns empty list if null handling is added
- Behavior should be documented and consistent

**Note:** The code checks `m.name() != null` before calling `equalsIgnoreCase(metadata_name)`, but if `metadata_name` itself is null, `equalsIgnoreCase()` will throw NPE.

---

### 4.3 Whitespace-Only Metadata Name
**Objective:** Verify behavior when metadata_name contains only whitespace.

**Test Data Requirements:**
- Applications with various metadata names

**Test Steps:**
1. Call `get_applications_by_metadata_name` with `metadata_name="   "` (spaces only)
2. Verify returns empty list (unlikely any metadata name is whitespace-only)
3. Verify no errors occur
4. Verify whitespace-only is treated as a valid search (even if no matches)

**Expected Results:**
- Returns empty list (no matches)
- No errors or exceptions
- Whitespace-only is treated as a literal search term
- Graceful handling

---

### 4.4 Special Characters in Metadata Name
**Objective:** Verify behavior with special characters in metadata name.

**Test Data Requirements:**
- Applications with metadata names containing special characters (e.g., "cost-center", "owner:team", "app/version")

**Test Steps:**
1. Identify applications with metadata names containing special characters
2. Note the exact metadata name (e.g., "cost-center")
3. Call `get_applications_by_metadata_name` with `metadata_name="cost-center"`
4. Verify applications with matching metadata name are returned
5. Verify special characters are matched literally
6. Test with various special characters: hyphens, colons, slashes, underscores, dots

**Expected Results:**
- Special characters are matched literally
- No special escaping needed
- Exact string matching (case-insensitive)
- All matching applications returned

---

### 4.5 Very Long Metadata Name
**Objective:** Verify behavior with very long metadata names.

**Test Data Requirements:**
- Applications with long metadata names (or artificially create one in test)

**Test Steps:**
1. Identify or create a metadata name that is very long (100+ characters)
2. Call `get_applications_by_metadata_name` with the long metadata name
3. Verify query executes successfully
4. Verify matching applications are returned
5. Verify no truncation or errors occur

**Expected Results:**
- Long metadata names handled correctly
- No truncation or size limits encountered
- Query executes successfully
- Matches found if they exist

---

## 5. Multiple Metadata Entries per Application

### 5.1 Application with Multiple Metadata Entries - Match One
**Objective:** Verify applications with multiple metadata entries are returned when one matches.

**Test Data Requirements:**
- Application with multiple metadata entries (e.g., "environment", "owner", "cost-center")

**Test Steps:**
1. Identify an application with multiple metadata entries
2. Note all metadata names for this application (e.g., ["environment", "owner", "cost-center"])
3. Call `get_applications_by_metadata_name` with one of the names (e.g., `metadata_name="owner"`)
4. Verify the application is returned
5. Verify ALL metadata entries for the application are included in the response
6. Verify only the matching metadata name is required for inclusion

**Expected Results:**
- Application returned if ANY metadata name matches
- All metadata entries for the application are included
- Partial matching (one metadata name sufficient)
- Complete application data returned

---

### 5.2 Application with Duplicate Metadata Names
**Objective:** Verify behavior when application has multiple metadata entries with the same name.

**Test Data Requirements:**
- Application with duplicate metadata names but different values (e.g., two "tag" entries)

**Test Steps:**
1. Identify or create application with duplicate metadata names
   - Example: metadata=["tag:value1", "tag:value2", "environment:prod"]
2. Call `get_applications_by_metadata_name` with `metadata_name="tag"`
3. Verify application is returned once (not duplicated)
4. Verify application appears only once in results
5. Verify both "tag" metadata entries are visible in the returned application data

**Expected Results:**
- Application returned once even with duplicate metadata names
- No duplicate applications in result
- All metadata entries preserved in response
- Duplicate names don't cause issues

---

## 6. Integration Testing

### 6.1 Integration with list_all_applications
**Objective:** Verify results are consistent with list_all_applications data.

**Test Data Requirements:**
- Multiple applications with various metadata configurations

**Test Steps:**
1. Call `list_all_applications` to get complete application list
2. Manually inspect the metadata of each application
3. Count how many applications have a specific metadata name (e.g., "environment")
4. Call `get_applications_by_metadata_name` with `metadata_name="environment"`
5. Verify the count matches
6. Verify all expected applications are in results
7. Verify no unexpected applications are included

**Expected Results:**
- Results consistent with list_all_applications data
- Counts match manual inspection
- No missing or extra applications
- Data integrity maintained

---

### 6.2 Filtering Consistency - Compare with getAllApplications
**Objective:** Verify filtering logic correctly implements the specification.

**Test Data Requirements:**
- Multiple applications with mixed metadata configurations

**Test Steps:**
1. Call `list_all_applications` to get all applications
2. Manually filter for applications with a specific metadata name
3. Call `get_applications_by_metadata_name` with the same metadata name
4. Compare the two result sets
5. Verify they contain the same applications
6. Verify same application IDs, names, and metadata

**Expected Results:**
- Tool filtering matches manual filtering
- No discrepancies in results
- Same application IDs returned
- Consistent behavior

---

## 7. Response Structure Testing

### 7.1 Response Type and Structure
**Objective:** Verify response is a properly structured list of ApplicationData objects.

**Test Data Requirements:**
- At least one application with metadata

**Test Steps:**
1. Call `get_applications_by_metadata_name` with a metadata name that has matches
2. Verify response is a list (array)
3. Verify each element is an ApplicationData object
4. Verify each ApplicationData has expected fields:
   - name (String)
   - status (String)
   - appID (String)
   - lastSeenAt (String)
   - language (String)
   - metadata (List<Metadata>)
   - tags (List<String>)
   - technologies (List<String>)

**Expected Results:**
- Response is a list/array
- Each element is ApplicationData record
- All fields present and correctly typed
- Structure matches ApplicationData specification

---

### 7.2 Metadata Field Structure in Response
**Objective:** Verify metadata field in ApplicationData is correctly structured.

**Test Data Requirements:**
- Applications with metadata

**Test Steps:**
1. Call `get_applications_by_metadata_name` with a metadata name
2. Examine the metadata field in each returned ApplicationData
3. Verify metadata is a list of Metadata objects
4. Verify each Metadata object has:
   - name (String)
   - value (String)
5. Verify the matching metadata name is present
6. Verify all metadata entries for the application are included (not just matching ones)

**Expected Results:**
- Metadata field is List<Metadata>
- Each Metadata has name and value fields
- All metadata for application included (not filtered to just matching name)
- Complete metadata information preserved

---

### 7.3 Empty Metadata Handling in Response
**Objective:** Verify applications with null or empty metadata are not included in results.

**Test Data Requirements:**
- Mix of applications with and without metadata

**Test Steps:**
1. Use `list_all_applications` to identify applications without metadata
2. Call `get_applications_by_metadata_name` with any metadata name
3. Verify applications with null metadata are NOT in results
4. Verify applications with empty metadata list are NOT in results
5. Verify only applications with actual metadata entries are returned

**Expected Results:**
- Null metadata applications excluded
- Empty metadata applications excluded
- Only applications with metadata entries included
- Safe null handling

---

## 8. Performance and Scale Testing

### 8.1 Large Number of Applications
**Objective:** Verify tool performs acceptably with large number of applications.

**Test Data Requirements:**
- Organization with 100+ applications
- Many applications with the same metadata name

**Test Steps:**
1. Identify an organization with many applications
2. Call `get_applications_by_metadata_name` with a common metadata name
3. Measure response time
4. Verify completes within reasonable time (< 30 seconds)
5. Verify all matching applications are returned
6. Verify no timeout or performance issues

**Expected Results:**
- Query completes successfully
- Response time acceptable
- All results returned
- No performance degradation

**Note:** Since there's no pagination, very large result sets may cause performance issues. This should be documented.

---

### 8.2 No Pagination - Large Result Sets
**Objective:** Document behavior with large result sets (no pagination available).

**Test Data Requirements:**
- Metadata name that matches many applications (50+)

**Test Steps:**
1. Identify a metadata name with many matches
2. Call `get_applications_by_metadata_name`
3. Verify ALL matching applications are returned in a single response
4. Note the size of the response
5. Document any performance concerns with large result sets
6. Verify no results are truncated

**Expected Results:**
- All results returned in single response
- No pagination available
- Large result sets may impact performance
- Complete results always returned (no truncation)

**Note:** For very large organizations, this could be a concern. Document as limitation.

---

## 9. Edge Cases and Corner Cases

### 9.1 Metadata Name with Leading/Trailing Spaces
**Objective:** Verify behavior with metadata names that have whitespace.

**Test Data Requirements:**
- Applications with metadata names that may have spaces

**Test Steps:**
1. Call `get_applications_by_metadata_name` with `metadata_name=" environment "` (leading/trailing spaces)
2. Observe behavior:
   - Option A: Spaces are significant, only matches " environment " literally
   - Option B: Spaces are trimmed, matches "environment"
3. Document actual behavior
4. Verify consistency

**Expected Results:**
- Likely spaces are significant (no trimming in code)
- Exact string matching with spaces included
- " environment " != "environment"
- Document this behavior

**Note:** The code doesn't trim spaces, so " environment " would not match "environment".

---

### 9.2 Unicode and International Characters
**Objective:** Verify behavior with Unicode characters in metadata names.

**Test Data Requirements:**
- Applications with metadata names containing Unicode (e.g., "propriétaire", "所有者", "владелец")

**Test Steps:**
1. Identify or create applications with Unicode metadata names
2. Call `get_applications_by_metadata_name` with Unicode metadata name
3. Verify matching applications are returned
4. Verify Unicode comparison works correctly with `equalsIgnoreCase()`
5. Test with various Unicode characters

**Expected Results:**
- Unicode characters handled correctly
- Case-insensitive matching works with Unicode
- International characters supported
- No encoding issues

---

### 9.3 Metadata Value Verification - Ignored
**Objective:** Explicitly verify that metadata values do not affect filtering.

**Test Data Requirements:**
- Applications with same metadata name but very different values (including null, empty, special chars)

**Test Steps:**
1. Identify applications with metadata name "owner" with values:
   - "team-a"
   - ""
   - null
   - "owner/admin:user"
2. Call `get_applications_by_metadata_name` with `metadata_name="owner"`
3. Verify ALL applications with "owner" metadata name are returned
4. Verify values don't affect inclusion (even null or empty values)
5. Confirm tool only checks metadata name, never value

**Expected Results:**
- All applications with matching metadata name returned
- Metadata values completely ignored
- Null values don't exclude applications
- Empty values don't exclude applications
- Value is irrelevant to filtering

---

## 10. Error Scenarios

### 10.1 SDK Connection Failure
**Objective:** Verify behavior when SDK cannot connect to Contrast server.

**Test Data Requirements:**
- Ability to simulate connection failure (e.g., invalid credentials, network issue)

**Test Steps:**
1. Configure environment to cause SDK connection failure
2. Call `get_applications_by_metadata_name` with any metadata name
3. Observe error behavior
4. Verify appropriate error message or exception
5. Verify error is propagated to caller

**Expected Results:**
- IOException thrown (per method signature)
- Appropriate error message
- Error propagated to caller
- No silent failures

---

### 10.2 SDK Returns Null Applications List
**Objective:** Verify behavior when SDK returns null instead of application list.

**Test Data Requirements:**
- Scenario where SDK might return null (may require mocking)

**Test Steps:**
1. Simulate SDK returning null applications list
2. Call `get_applications_by_metadata_name` with any metadata name
3. Observe behavior
4. Verify no NullPointerException occurs (or verify it's handled appropriately)

**Expected Results:**
- Graceful handling of null application list
- OR appropriate exception with clear message
- No unhandled NullPointerException
- Predictable behavior

**Note:** The code calls `getAllApplications()` which should handle this, but worth testing.

---

## Test Execution Guidelines

### For AI Test Executors

1. **Discovery Phase:** Start by querying the Contrast installation to understand what data is available
   - Use `list_all_applications` to see what applications exist
   - Examine the metadata field for each application
   - Identify metadata names that exist, their frequency, and value diversity
   - Note applications without metadata

2. **Test Selection:** Based on available data, determine which tests are feasible
   - Skip tests if required data doesn't exist
   - Document skipped tests and reasons
   - Prioritize tests based on available data

3. **Test Execution:** For each test:
   - Document the metadata name used
   - Document the expected applications to be returned (based on prior data discovery)
   - Execute the query
   - Compare actual results with expected
   - Document pass/fail and any discrepancies

4. **Result Reporting:** Provide summary:
   - Total tests attempted
   - Tests passed
   - Tests failed (with details)
   - Tests skipped (with reasons)
   - Any unexpected behaviors or bugs found
   - Documentation issues (if any)

### Success Criteria

A test passes if:
- The query executes without unexpected errors
- The returned applications all have metadata with the specified name (case-insensitive)
- Applications without the metadata name are excluded
- The response structure is correct (List<ApplicationData>)
- Edge cases are handled gracefully
- Behavior is consistent and predictable

### Failure Scenarios

A test fails if:
- Query fails unexpectedly when it should succeed
- Returned applications don't all have the specified metadata name
- Applications with the metadata name are missing from results
- Case sensitivity doesn't work as expected (should be case-insensitive)
- Response structure is incorrect
- Null handling causes unexpected exceptions
- Duplicate applications appear in results

---

## Appendix: Quick Reference

### Tool Specification
- **Tool Name:** `get_applications_by_metadata_name`
- **Input:** `metadata_name` (String) - case-insensitive
- **Output:** List<ApplicationData> - all matching applications
- **Matching:** Case-insensitive equality on metadata name only
- **Pagination:** None - returns all matches in single response
- **Throws:** IOException on SDK errors

### ApplicationData Structure
```
ApplicationData {
  name: String
  status: String
  appID: String
  lastSeenAt: String
  language: String
  metadata: List<Metadata>
  tags: List<String>
  technologies: List<String>
}
```

### Metadata Structure
```
Metadata {
  name: String
  value: String
}
```

### Implementation Details
- Uses `equalsIgnoreCase()` for metadata name comparison
- Filters from `getAllApplications()` results
- Checks for null metadata, null metadata entries, and null metadata names
- No trimming of whitespace in metadata names
- No pagination - returns complete result set

### Common Test Data Patterns

**Metadata Name Examples:**
- "environment" (common)
- "owner" (common)
- "cost-center" (common)
- "team" (common)
- "project" (common)

**Expected Value Patterns:**
- Single value per application
- Multiple different values across applications
- Empty string values
- Null values (should still match by name)

---

## Document Version
**Version:** 1.0
**Date:** 2025-10-21
**Author:** Claude Code (AI Assistant)
**Tool Location:** `/Users/chrisedwards/projects/contrast/mcp-contrast/src/main/java/com/contrast/labs/ai/mcp/contrast/AssessService.java` (lines 388-402)
