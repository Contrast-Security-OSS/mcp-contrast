# Test Plan: list_vulns_by_app_and_metadata Tool

## Overview
This test plan provides comprehensive testing instructions for the `list_vulns_by_app_and_metadata` MCP tool. The tool filters vulnerabilities by application ID and session metadata key/value pairs, enabling analysis of vulnerabilities discovered in specific testing scenarios or runtime contexts.

**Testing Approach:** Each test case describes the type of test data needed, how to identify it in the Contrast installation, and how to verify the results. Tests are designed to be executed by an AI agent that will:
1. Query the Contrast installation to find data matching test requirements
2. Execute the tool with appropriate parameters
3. Verify the results meet expected criteria

---

## 1. Basic Functionality Testing

### 1.1 Simple Session Metadata Match - Single Result
**Objective:** Verify tool correctly filters by session metadata and returns matching vulnerabilities.

**Test Data Requirements:**
- An application exists with vulnerabilities
- At least one vulnerability has session metadata with a known name/value pair (e.g., displayLabel="build", value="12345")

**Test Steps:**
1. Use `list_all_applications` to find an application ID with vulnerabilities
2. Use `list_all_vulnerabilities` with that application to examine session metadata
3. Identify a vulnerability with session metadata and note a name/value pair
4. Query `list_vulns_by_app_and_metadata` with:
   - `appID="<application ID>"`
   - `session_Metadata_Name="<metadata display label>"`
   - `session_Metadata_Value="<metadata value>"`
5. Verify the identified vulnerability is returned
6. Verify all returned vulnerabilities have matching session metadata

**Expected Results:**
- Query executes successfully
- Returns vulnerabilities with matching session metadata
- Each returned vulnerability has at least one session metadata item with matching displayLabel and value
- No vulnerabilities without matching metadata are returned

---

### 1.2 Session Metadata Match - Multiple Results
**Objective:** Verify tool returns all vulnerabilities matching the session metadata criteria.

**Test Data Requirements:**
- An application with multiple vulnerabilities sharing the same session metadata name/value pair
- For example, multiple vulnerabilities discovered in the same test run (e.g., metadata name="run_id", value="run_123")

**Test Steps:**
1. Identify an application with multiple vulnerabilities
2. Examine session metadata to find a name/value pair shared by multiple vulnerabilities
3. Query `list_vulns_by_app_and_metadata` with the shared metadata
4. Verify multiple vulnerabilities are returned
5. Verify each returned vulnerability has the matching session metadata
6. Count vulnerabilities with this metadata manually and compare with results count

**Expected Results:**
- All vulnerabilities with matching session metadata are returned
- No vulnerabilities without matching metadata are included
- Result count matches expected count from manual inspection

---

### 1.3 Session Metadata Match - No Results
**Objective:** Verify tool returns empty list when no vulnerabilities match session metadata criteria.

**Test Data Requirements:**
- An application exists with vulnerabilities
- Session metadata name/value pair that doesn't exist in any vulnerability

**Test Steps:**
1. Select an application with vulnerabilities
2. Query `list_vulns_by_app_and_metadata` with:
   - Valid application ID
   - `session_Metadata_Name="nonexistent_metadata"`
   - `session_Metadata_Value="nonexistent_value"`
3. Verify empty list is returned
4. Verify no errors occur

**Expected Results:**
- Empty list returned
- No errors or exceptions
- Tool handles no-match scenario gracefully

---

## 2. Session Metadata Matching Behavior

### 2.1 Case Insensitive Matching - Metadata Name
**Objective:** Verify session metadata name matching is case-insensitive.

**Test Data Requirements:**
- Vulnerability with session metadata (e.g., displayLabel="Build", value="123")

**Test Steps:**
1. Identify a vulnerability with session metadata having a specific displayLabel (e.g., "Build")
2. Query with different case variations of the metadata name:
   - `session_Metadata_Name="Build"` (exact match)
   - `session_Metadata_Name="build"` (lowercase)
   - `session_Metadata_Name="BUILD"` (uppercase)
   - `session_Metadata_Name="BuILd"` (mixed case)
3. Verify all queries return the same vulnerability
4. Verify case variations all work correctly

**Expected Results:**
- Metadata name matching is case-insensitive
- All case variations return the same results
- Implementation uses `equalsIgnoreCase()` as per code

---

### 2.2 Case Insensitive Matching - Metadata Value
**Objective:** Verify session metadata value matching is case-insensitive.

**Test Data Requirements:**
- Vulnerability with session metadata (e.g., displayLabel="environment", value="Production")

**Test Steps:**
1. Identify a vulnerability with session metadata having a specific value (e.g., "Production")
2. Query with different case variations of the metadata value:
   - `session_Metadata_Value="Production"` (exact match)
   - `session_Metadata_Value="production"` (lowercase)
   - `session_Metadata_Value="PRODUCTION"` (uppercase)
   - `session_Metadata_Value="PrOdUcTiOn"` (mixed case)
3. Verify all queries return the same vulnerability
4. Verify case variations all work correctly

**Expected Results:**
- Metadata value matching is case-insensitive
- All case variations return the same results
- Implementation uses `equalsIgnoreCase()` as per code

---

### 2.3 Exact Match Required - Partial Matches Not Supported
**Objective:** Verify that matching requires exact string equality (not partial/substring matching).

**Test Data Requirements:**
- Vulnerability with session metadata (e.g., displayLabel="test_run", value="integration_test_123")

**Test Steps:**
1. Identify a vulnerability with session metadata having specific name/value
2. Query with partial metadata name: `session_Metadata_Name="test"` (substring of "test_run")
3. Verify no results returned (partial match not supported)
4. Query with partial metadata value: `session_Metadata_Value="integration"` (substring of "integration_test_123")
5. Verify no results returned (partial match not supported)
6. Query with exact values and verify vulnerability is found

**Expected Results:**
- Partial matches do not return results
- Only exact string matches work (case-insensitive)
- Substring/contains matching is not supported

---

### 2.4 Whitespace Sensitivity - Leading/Trailing Spaces
**Objective:** Verify whitespace handling in metadata matching.

**Test Data Requirements:**
- Vulnerability with session metadata (e.g., displayLabel="version", value="1.0")

**Test Steps:**
1. Identify a vulnerability with session metadata
2. Query with leading/trailing spaces in metadata name: `session_Metadata_Name=" version "`
3. Verify behavior (likely no match due to exact string comparison)
4. Query with leading/trailing spaces in metadata value: `session_Metadata_Value=" 1.0 "`
5. Verify behavior (likely no match due to exact string comparison)
6. Query with exact values (no extra spaces) and verify success

**Expected Results:**
- Tool performs exact string matching (no automatic trimming)
- Leading/trailing spaces cause match failure
- Users must provide exact metadata values as stored in Contrast

**Note:** If implementation behavior differs (e.g., automatic trimming), document actual behavior.

---

### 2.5 Special Characters in Metadata Values
**Objective:** Verify special characters in metadata values are handled correctly.

**Test Data Requirements:**
- Vulnerability with session metadata containing special characters (e.g., value="feature/CONT-123", value="v1.2.3-rc1", value="user@example.com")

**Test Steps:**
1. Identify vulnerabilities with session metadata containing special characters:
   - Slashes (/)
   - Hyphens (-)
   - Underscores (_)
   - At signs (@)
   - Periods (.)
2. Query with exact metadata values including special characters
3. Verify correct vulnerabilities are returned
4. Verify special characters don't cause errors or unexpected behavior

**Expected Results:**
- Special characters handled correctly in matching
- No encoding/escaping issues
- Query executes successfully with special characters

---

## 3. Multiple Sessions and Metadata Items

### 3.1 Vulnerability with Multiple Session Metadata Items
**Objective:** Verify vulnerability is returned if ANY session metadata item matches criteria.

**Test Data Requirements:**
- A vulnerability with multiple session metadata items in a single session (e.g., a session with metadata items for "build", "version", "user", etc.)

**Test Steps:**
1. Identify a vulnerability with multiple metadata items in its session metadata
2. Note at least two different metadata name/value pairs from the same vulnerability
3. Query with first metadata pair and verify vulnerability is returned
4. Query with second metadata pair and verify same vulnerability is returned
5. Query with non-existent metadata pair and verify vulnerability is NOT returned

**Expected Results:**
- Vulnerability returned if ANY of its metadata items match
- Multiple different queries can return the same vulnerability if it has multiple metadata items
- OR logic applied: any matching metadata item qualifies the vulnerability

---

### 3.2 Vulnerability with Multiple Sessions
**Objective:** Verify vulnerability is returned if metadata matches in ANY session.

**Test Data Requirements:**
- A vulnerability with multiple sessions in its sessionMetadata array
- Each session may have different metadata items

**Test Steps:**
1. Identify a vulnerability with multiple sessions (multiple entries in sessionMetadata array)
2. Examine metadata items across different sessions
3. Query with metadata from first session and verify vulnerability is returned
4. Query with metadata from second session and verify vulnerability is returned
5. Verify same vulnerability can be found via metadata from different sessions

**Expected Results:**
- Vulnerability returned if metadata matches in any of its sessions
- Tool iterates through all sessions when matching
- Multiple sessions with different metadata all contribute to matching

---

### 3.3 Different Sessions with Same Metadata Values
**Objective:** Verify vulnerability matching works across multiple sessions with same metadata.

**Test Data Requirements:**
- A vulnerability seen in multiple test runs/sessions with same metadata key but different values (e.g., session 1: run_id="run_1", session 2: run_id="run_2")

**Test Steps:**
1. Identify vulnerability with same metadata name across different sessions but different values
2. Query with first metadata value: `session_Metadata_Name="run_id", session_Metadata_Value="run_1"`
3. Verify vulnerability is returned
4. Query with second metadata value: `session_Metadata_Name="run_id", session_Metadata_Value="run_2"`
5. Verify vulnerability is returned
6. Query with non-existent value: `session_Metadata_Value="run_999"`
7. Verify vulnerability is NOT returned

**Expected Results:**
- Same vulnerability can be found via different metadata values from different sessions
- Query matches across all sessions in the vulnerability's history
- Each query filters based on the specific value provided

---

### 3.4 Vulnerabilities Without Session Metadata
**Objective:** Verify vulnerabilities without session metadata are not returned (handled gracefully).

**Test Data Requirements:**
- An application with at least one vulnerability that has no session metadata (sessionMetadata field is null or empty)

**Test Steps:**
1. Identify application with mix of vulnerabilities (some with session metadata, some without)
2. Query with any metadata name/value pair
3. Verify only vulnerabilities with matching session metadata are returned
4. Verify vulnerabilities without session metadata are excluded
5. Verify no null pointer exceptions or errors occur

**Expected Results:**
- Vulnerabilities without session metadata are silently excluded
- No errors occur when processing null/empty sessionMetadata
- Tool handles null check correctly (code: `if(vuln.sessionMetadata()!=null)`)

---

## 4. Application Resolution Testing

### 4.1 Valid Application Name - Exact Match
**Objective:** Verify tool correctly resolves application by exact name match.

**Test Data Requirements:**
- Application exists with known name and vulnerabilities

**Test Steps:**
1. Use `list_all_applications` to get exact application ID
2. Query `list_vulns_by_app_and_metadata` with exact application ID
3. Verify vulnerabilities from correct application are returned
4. Verify no vulnerabilities from other applications are included

**Expected Results:**
- Application resolved correctly by name
- Only vulnerabilities from specified application returned
- Name matching works as expected

---

### 4.2 Invalid Application Name - No Match
**Objective:** Verify behavior when application ID doesn't exist.

**Test Data Requirements:**
- None (using non-existent application ID)

**Test Steps:**
1. Query `list_vulns_by_app_and_metadata` with:
   - `appID="NonExistentApplication123"`
   - Any metadata name/value
2. Verify empty list is returned
3. Verify no errors or exceptions
4. Check logs for message indicating application not found

**Expected Results:**
- Empty list returned
- No errors or exceptions
- Tool handles non-existent application gracefully
- Log message: "Application with name {name} not found, returning empty list"

---

### 4.3 Application Name Case Sensitivity
**Objective:** Verify application ID matching case sensitivity behavior.

**Test Data Requirements:**
- Application with known name (e.g., "WebGoat")

**Test Steps:**
1. Get exact application ID from `list_all_applications`
2. Query with exact application ID and verify results
3. Query with different case variations:
   - All lowercase (e.g., "webgoat")
   - All uppercase (e.g., "WEBGOAT")
   - Mixed case variations
4. Document whether application ID matching is case-sensitive or case-insensitive

**Expected Results:**
- Document actual case sensitivity behavior of application ID resolution
- Behavior should be consistent across all queries
- If case-sensitive: only exact case matches work
- If case-insensitive: all case variations work

**Note:** This depends on the underlying `SDKHelper.getApplicationByName()` implementation.

---

### 4.4 Application with No Vulnerabilities
**Objective:** Verify behavior when application exists but has no vulnerabilities.

**Test Data Requirements:**
- Application exists but has no vulnerabilities
- Or use an application with vulnerabilities but query for metadata that doesn't exist

**Test Steps:**
1. Identify application with no vulnerabilities (or use metadata filter that matches none)
2. Query `list_vulns_by_app_and_metadata` with valid app name
3. Verify empty list is returned
4. Verify no errors occur
5. Verify behavior is same as "no matching metadata" scenario

**Expected Results:**
- Empty list returned
- No errors or exceptions
- Tool handles "no vulnerabilities" scenario gracefully

---

### 4.5 Application with Multiple Matching Vulnerabilities
**Objective:** Verify tool returns all matching vulnerabilities from an application.

**Test Data Requirements:**
- Application with many vulnerabilities (10+)
- Several vulnerabilities share the same session metadata

**Test Steps:**
1. Identify application with multiple vulnerabilities
2. Find session metadata shared by multiple vulnerabilities
3. Query with that metadata
4. Verify all matching vulnerabilities are returned
5. Manually count expected matches and compare with actual results

**Expected Results:**
- All matching vulnerabilities returned
- No pagination (tool returns full list)
- Result count matches manual count

---

## 5. Parameter Validation Testing

### 5.1 Null or Empty Application Name
**Objective:** Verify behavior with null or empty application ID.

**Test Data Requirements:**
- None (testing validation)

**Test Steps:**
1. Query with `appID=""` (empty string), valid metadata name/value
2. Document behavior (likely returns empty list or error)
3. Query with `appID=null` if tool interface allows
4. Document behavior

**Expected Results:**
- Tool handles invalid application ID gracefully
- Either returns empty list or provides error message
- No unexpected exceptions or crashes

**Note:** Actual behavior depends on validation implementation. Document observed behavior.

---

### 5.2 Null or Empty Metadata Name
**Objective:** Verify behavior with null or empty metadata name.

**Test Data Requirements:**
- Valid application with vulnerabilities

**Test Steps:**
1. Query with valid appID, `session_Metadata_Name=""` (empty string), valid metadata value
2. Document behavior (likely no matches found)
3. Query with `session_Metadata_Name=null` if tool interface allows
4. Document behavior

**Expected Results:**
- Tool handles empty metadata name gracefully
- Likely returns empty list (no metadata has empty displayLabel)
- No unexpected exceptions

---

### 5.3 Null or Empty Metadata Value
**Objective:** Verify behavior with null or empty metadata value.

**Test Data Requirements:**
- Application with vulnerabilities
- Verify if any vulnerabilities have session metadata with empty string values

**Test Steps:**
1. Query with valid appID, valid metadata name, `session_Metadata_Value=""` (empty string)
2. Document behavior:
   - If vulnerabilities exist with empty string values: should return those
   - If no vulnerabilities have empty string values: returns empty list
3. Query with `session_Metadata_Value=null` if tool interface allows
4. Document behavior

**Expected Results:**
- Tool handles empty metadata value
- If empty value exists in data: matches are returned
- If empty value doesn't exist: empty list returned
- No unexpected exceptions

---

### 5.4 Very Long Parameter Values
**Objective:** Verify tool handles unusually long parameter values.

**Test Data Requirements:**
- None (testing edge cases)

**Test Steps:**
1. Query with very long application ID (1000+ characters)
2. Verify behavior (likely no match found, but no error)
3. Query with very long metadata name (1000+ characters)
4. Verify behavior
5. Query with very long metadata value (1000+ characters)
6. Verify behavior

**Expected Results:**
- Tool handles long values gracefully
- No buffer overflow or performance issues
- Likely returns empty list (no matches for nonsense values)
- No crashes or exceptions

---

### 5.5 Special Characters in Application Name
**Objective:** Verify special characters in application ID are handled correctly.

**Test Data Requirements:**
- Application with special characters in name (if any exist, e.g., "My-App", "App (v2)", "App/Service")

**Test Steps:**
1. Identify applications with special characters in their names
2. Query with exact application IDs including special characters
3. Verify correct application's vulnerabilities are returned
4. Verify no encoding or escaping issues

**Expected Results:**
- Special characters in application IDs handled correctly
- No encoding issues
- Query executes successfully

---

## 6. Data Integrity and Filtering Accuracy

### 6.1 Verify Only Specified Application's Vulnerabilities Returned
**Objective:** Ensure no vulnerabilities from other applications are included.

**Test Data Requirements:**
- Multiple applications exist, each with vulnerabilities
- Multiple applications have vulnerabilities with same session metadata

**Test Steps:**
1. Identify two applications (App A and App B) both with vulnerabilities
2. Identify session metadata that exists in both applications (e.g., metadata name="environment", value="production")
3. Query for App A with the shared metadata
4. Verify ONLY App A vulnerabilities are returned
5. Verify no App B vulnerabilities are included
6. Query for App B with same metadata
7. Verify ONLY App B vulnerabilities are returned

**Expected Results:**
- Application filtering is strictly enforced
- No cross-application contamination
- Each query returns only vulnerabilities from specified application

---

### 6.2 Verify Metadata Filtering Accuracy
**Objective:** Ensure only vulnerabilities with exact metadata matches are returned.

**Test Data Requirements:**
- Application with vulnerabilities having different session metadata

**Test Steps:**
1. Identify application with various session metadata values
2. Select specific metadata name/value pair (e.g., name="version", value="1.0")
3. Query with this metadata
4. Manually inspect each returned vulnerability's session metadata
5. Verify each has at least one matching metadata item
6. Verify no vulnerabilities without matching metadata are included

**Expected Results:**
- 100% filtering accuracy
- No false positives (vulnerabilities without matching metadata)
- No false negatives (missing vulnerabilities with matching metadata)

---

### 6.3 Verify VulnLight Data Structure
**Objective:** Verify returned vulnerabilities have correct data structure.

**Test Steps:**
1. Query with valid parameters returning at least one vulnerability
2. Examine returned VulnLight objects
3. Verify each contains expected fields:
   - title (string)
   - type (string)
   - vulnID (string)
   - severity (string)
   - sessionMetadata (array of SessionMetadata objects)
   - lastSeenAt (string, timestamp)
   - status (string)
   - firstSeenAt (string, timestamp)
   - closedAt (string or null)
   - environments (array of strings)
4. Verify sessionMetadata structure:
   - Each SessionMetadata has sessionId
   - Each SessionMetadata has metadata array
   - Each MetadataItem has displayLabel, value, agentLabel
5. Verify at least one metadata item matches query criteria

**Expected Results:**
- All fields present with correct types
- SessionMetadata structure matches schema
- Returned data is complete and well-formed

---

## 7. Error Handling and Edge Cases

### 7.1 Vulnerability with Null Session Metadata - Null Safety
**Objective:** Verify null safety when vulnerability has null sessionMetadata.

**Test Data Requirements:**
- Vulnerability with sessionMetadata=null (or ability to test this scenario)

**Test Steps:**
1. Identify or create scenario where vulnerability might have null sessionMetadata
2. Query application containing such vulnerabilities
3. Verify no NullPointerException occurs
4. Verify tool processes other vulnerabilities correctly
5. Verify null sessionMetadata vulnerability is excluded from results

**Expected Results:**
- No errors or exceptions
- Null check in code prevents NullPointerException: `if(vuln.sessionMetadata()!=null)`
- Tool continues processing other vulnerabilities

---

### 7.2 SessionMetadata with Null or Empty Metadata List
**Objective:** Verify handling when SessionMetadata object has null or empty metadata list.

**Test Data Requirements:**
- Vulnerability with SessionMetadata but null or empty metadata array

**Test Steps:**
1. Identify vulnerability with SessionMetadata that has empty metadata list
2. Query for this vulnerability with any metadata criteria
3. Verify no errors occur
4. Verify vulnerability is not returned (no metadata items to match)

**Expected Results:**
- No errors when iterating empty metadata list
- Vulnerability not returned (no matching metadata items)
- Tool handles empty collections gracefully

---

### 7.3 MetadataItem with Null displayLabel or value
**Objective:** Verify handling when MetadataItem has null fields.

**Test Data Requirements:**
- Vulnerability with MetadataItem that has null displayLabel or null value

**Test Steps:**
1. Identify vulnerability with metadata item having null fields
2. Query with metadata criteria
3. Verify no NullPointerException occurs
4. Verify null fields don't match query criteria (null != any string)

**Expected Results:**
- No NullPointerException from equalsIgnoreCase() on null
- Null displayLabel or value causes match failure
- Tool handles null gracefully

**Note:** If null fields cause errors, this is a bug that should be reported.

---

### 7.4 Empty Result Set - Multiple Filters Applied
**Objective:** Verify behavior when both application and metadata filters result in no matches.

**Test Data Requirements:**
- Valid application but no vulnerabilities with specified metadata

**Test Steps:**
1. Select application with vulnerabilities
2. Query with metadata that definitely doesn't exist
3. Verify empty list returned
4. Verify no errors or warnings
5. Verify this is distinguishable from "application not found" scenario (check logs)

**Expected Results:**
- Empty list returned
- No errors
- Log message should indicate application was found, but then filtered results are empty

---

### 7.5 Performance with Large Result Sets
**Objective:** Verify performance when application has many vulnerabilities.

**Test Data Requirements:**
- Application with 100+ vulnerabilities
- Many vulnerabilities share session metadata

**Test Steps:**
1. Identify application with large number of vulnerabilities
2. Query with metadata that matches many vulnerabilities (50+)
3. Measure response time
4. Verify query completes in reasonable time (< 60 seconds)
5. Verify all matching vulnerabilities are returned

**Expected Results:**
- Query completes successfully
- All matching vulnerabilities returned (no pagination applied)
- Performance is acceptable
- No timeout or memory issues

---

### 7.6 Performance with Many Metadata Items per Vulnerability
**Objective:** Verify performance when vulnerabilities have many session metadata items.

**Test Data Requirements:**
- Vulnerabilities with 10+ metadata items in single session

**Test Steps:**
1. Identify vulnerability with many metadata items
2. Query with metadata that appears late in the metadata list
3. Verify query completes successfully
4. Verify performance is acceptable
5. Verify nested loop iteration doesn't cause issues

**Expected Results:**
- Query completes successfully
- Performance acceptable even with nested loops (sessions -> metadata items)
- No timeout issues

---

## 8. Integration Testing

### 8.1 Integration with list_all_applications
**Objective:** Verify application IDs from list_all_applications work correctly.

**Test Data Requirements:**
- Multiple applications exist

**Test Steps:**
1. Call `list_all_applications` to get application list
2. Select an application ID from the results
3. Query `list_vulns_by_app_and_metadata` with that exact name
4. Verify query executes successfully
5. Verify results are from the correct application

**Expected Results:**
- Application names from list_all_applications work correctly
- No discrepancies in name formatting or encoding
- Seamless integration between tools

---

### 8.2 Integration with list_all_vulnerabilities
**Objective:** Verify consistency between this tool and list_all_vulnerabilities.

**Test Data Requirements:**
- Application with vulnerabilities that have session metadata

**Test Steps:**
1. Query `list_all_vulnerabilities` with `appId` filter for a specific application
2. Examine returned vulnerabilities and their session metadata
3. Note a vulnerability with specific session metadata
4. Query `list_vulns_by_app_and_metadata` with same app name and the noted metadata
5. Verify the same vulnerability is returned by both tools
6. Verify vulnerability data is consistent (same vulnID, same fields)

**Expected Results:**
- Consistent results between tools
- Same vulnerabilities returned given same filters
- Data consistency across tools

---

### 8.3 Chaining Queries - Finding All Metadata Values
**Objective:** Verify tool can be used to explore session metadata systematically.

**Test Data Requirements:**
- Application with vulnerabilities containing various session metadata

**Test Steps:**
1. Use `list_all_vulnerabilities` to get all vulnerabilities for an application
2. Extract all unique session metadata names (displayLabels)
3. For each unique metadata name, extract all unique values
4. Query `list_vulns_by_app_and_metadata` for each name/value pair
5. Verify each query returns expected vulnerabilities
6. Verify no vulnerabilities are missed

**Expected Results:**
- Tool can be used to systematically explore session metadata
- All vulnerabilities can be found via their metadata
- No data is missed when querying by all possible name/value combinations

---

## 9. Use Case Scenarios

### 9.1 Finding Vulnerabilities from Specific Test Run
**Objective:** Simulate real-world use case of finding vulnerabilities from a CI/CD test run.

**Test Data Requirements:**
- Vulnerabilities with session metadata like "build_id", "run_id", "ci_job", etc.

**Test Steps:**
1. Identify vulnerabilities from a specific test run (e.g., metadata name="build_id", value="build_12345")
2. Query with application ID and build metadata
3. Verify all vulnerabilities from that build are returned
4. Verify this provides useful subset for analysis
5. Verify can be used to track vulnerability introduction in specific builds

**Expected Results:**
- Tool effectively filters vulnerabilities by test run
- Useful for CI/CD integration scenarios
- Provides actionable subset of vulnerabilities for developers

---

### 9.2 Finding Vulnerabilities from Specific User Session
**Objective:** Simulate finding vulnerabilities discovered during specific user session.

**Test Data Requirements:**
- Vulnerabilities with session metadata like "user", "session_id", "request_id"

**Test Steps:**
1. Identify vulnerabilities associated with user session (e.g., metadata name="user", value="test_user@example.com")
2. Query with application ID and user metadata
3. Verify vulnerabilities from that user's session are returned
4. Verify useful for debugging user-reported issues

**Expected Results:**
- Tool effectively filters by user session
- Useful for support and debugging scenarios
- Can correlate vulnerabilities with user activities

---

### 9.3 Finding Vulnerabilities from Specific Environment
**Objective:** Simulate finding vulnerabilities discovered in specific runtime environment.

**Test Data Requirements:**
- Vulnerabilities with session metadata like "environment", "stage", "deployment"

**Test Steps:**
1. Identify vulnerabilities with environment metadata (e.g., metadata name="environment", value="staging")
2. Query with application ID and environment metadata
3. Verify vulnerabilities from that environment are returned
4. Compare with vulnerabilities from different environments

**Expected Results:**
- Tool effectively filters by runtime environment metadata
- Useful for environment-specific analysis
- Can compare vulnerability profiles across environments

---

### 9.4 Finding Vulnerabilities from Specific Feature Branch
**Objective:** Simulate finding vulnerabilities introduced in feature branch.

**Test Data Requirements:**
- Vulnerabilities with session metadata like "branch", "feature", "git_branch"

**Test Steps:**
1. Identify vulnerabilities with branch metadata (e.g., metadata name="branch", value="feature/new-login")
2. Query with application ID and branch metadata
3. Verify vulnerabilities from that branch are returned
4. Verify useful for code review and PR analysis

**Expected Results:**
- Tool effectively filters by source code branch
- Useful for development workflow integration
- Can identify vulnerabilities introduced by specific changes

---

## 10. Comparison Testing

### 10.1 Compare with Manual Filtering
**Objective:** Verify tool results match manual filtering of vulnerability list.

**Test Data Requirements:**
- Application with vulnerabilities having various session metadata

**Test Steps:**
1. Get all vulnerabilities for an application using `list_all_vulnerabilities`
2. Manually filter results by inspecting session metadata for specific name/value pair
3. Query `list_vulns_by_app_and_metadata` with same criteria
4. Compare manual filtering results with tool results
5. Verify identical results

**Expected Results:**
- Tool results match manual filtering exactly
- No discrepancies
- Tool correctly implements filtering logic

---

### 10.2 Compare Different Session Metadata Queries on Same Application
**Objective:** Verify different metadata queries return different, correct subsets.

**Test Data Requirements:**
- Application with vulnerabilities having multiple different session metadata values

**Test Steps:**
1. Query application with metadata pair A (e.g., name="version", value="1.0")
2. Note vulnerabilities returned
3. Query same application with metadata pair B (e.g., name="version", value="2.0")
4. Note vulnerabilities returned
5. Verify sets are different (unless vulnerabilities exist in both sessions)
6. Verify no overlap unless vulnerability legitimately matches both criteria

**Expected Results:**
- Different queries return different results
- Results are mutually exclusive (unless vulnerability in multiple sessions)
- Each query correctly identifies its subset

---

## Test Execution Guidelines

### For AI Test Executors

1. **Discovery Phase:** Start by querying the Contrast installation to understand what data is available
   - Use `list_all_applications` to see what apps exist
   - Use `list_all_vulnerabilities` to examine vulnerabilities and their session metadata
   - Identify which vulnerabilities have session metadata and what names/values are used
   - Look for patterns in metadata naming (e.g., "build", "version", "environment", "user", etc.)

2. **Test Selection:** Based on available data, determine which tests are feasible
   - Skip tests if required data doesn't exist (e.g., if no vulnerabilities have session metadata)
   - Document skipped tests and reasons
   - Prioritize tests based on available data

3. **Test Execution:** For each test:
   - Document the query parameters used
   - Document the expected results based on prior data discovery
   - Execute the query
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
- The query executes without unexpected errors
- The returned vulnerabilities match the filtering criteria (correct application AND correct session metadata)
- All returned vulnerabilities have the matching session metadata
- No vulnerabilities without matching metadata are returned
- Edge cases are handled gracefully (null checks, empty results, etc.)
- Data structure matches expected format

### Failure Scenarios

A test fails if:
- Query fails when it should succeed
- Returned vulnerabilities don't all have matching session metadata
- Vulnerabilities from wrong application are returned
- Null pointer exceptions occur
- Expected vulnerabilities are missing from results
- Unexpected vulnerabilities are included in results
- Case sensitivity doesn't work as documented
- Performance is unacceptable

---

## Appendix: Implementation Details

### Code Reference
**File:** `/Users/chrisedwards/projects/contrast/mcp-contrast/src/main/java/com/contrast/labs/ai/mcp/contrast/AssessService.java`
**Lines:** 210-247

### Key Implementation Details

1. **Application Resolution:**
   - Uses `SDKHelper.getApplicationByName()` to find application
   - Returns empty list if application not found (no error)
   - Log message: "Application with name {name} not found, returning empty list"

2. **Vulnerability Fetching:**
   - Calls `listVulnsByAppId()` to get all vulnerabilities for application
   - Then filters in-memory by session metadata

3. **Filtering Logic:**
   - Iterates through all vulnerabilities from the application
   - For each vulnerability, checks if sessionMetadata is not null
   - Iterates through each SessionMetadata object
   - Iterates through each MetadataItem in the SessionMetadata
   - Matches if `displayLabel.equalsIgnoreCase(session_Metadata_Name)` AND `value.equalsIgnoreCase(session_Metadata_Value)`
   - Breaks inner loop after first match (vulnerability only needs one matching metadata item)

4. **Case Sensitivity:**
   - Both metadata name and value matching use `equalsIgnoreCase()`
   - Case-insensitive matching for both fields

5. **Return Value:**
   - Returns `List<VulnLight>`
   - Empty list if application not found
   - Empty list if no vulnerabilities match metadata criteria
   - All matching vulnerabilities if found

### Data Structures

**VulnLight:** Record containing vulnerability summary data
**SessionMetadata:** Contains sessionId and list of MetadataItem objects
**MetadataItem:** Contains displayLabel (name), value, and agentLabel

### Error Handling
- No explicit validation of input parameters
- Null check for sessionMetadata to prevent NPE
- IOException thrown if vulnerability listing fails
- Application not found returns empty list (graceful degradation)

---

## Appendix: Quick Reference

### Tool Parameters
- **appID**: Application name (string, case sensitivity depends on SDK implementation)
- **session_Metadata_Name**: Metadata displayLabel to match (string, case-insensitive matching)
- **session_Metadata_Value**: Metadata value to match (string, case-insensitive matching)

### Return Type
- **List<VulnLight>**: Array of matching vulnerabilities (empty if no matches)

### Matching Behavior
- Application name: Resolved via SDK helper (case sensitivity TBD)
- Metadata name: Case-insensitive (equalsIgnoreCase)
- Metadata value: Case-insensitive (equalsIgnoreCase)
- Matching type: Exact match (not partial/substring)
- Logic: Returns vulnerability if ANY session metadata item matches BOTH name AND value

### Common Session Metadata Names
(Based on typical Contrast usage patterns - actual names vary by installation)
- build, build_id, build_number
- version, app_version
- environment, stage, deployment
- branch, git_branch, feature
- run_id, test_run, ci_job
- user, username, session_user
- request_id, correlation_id

---

## Document Version
**Version:** 1.0
**Date:** 2025-10-21
**Author:** Claude Code (AI Assistant)
**Tool Version:** mcp-contrast 0.0.11
