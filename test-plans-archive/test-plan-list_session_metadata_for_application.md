# Test Plan: list_session_metadata_for_application Tool

## Overview
This test plan provides comprehensive testing instructions for the `list_session_metadata_for_application` MCP tool. The tool retrieves session metadata for the latest session associated with a specified application name. Session metadata contains contextual information about the application runtime session, including user IDs, request parameters, environment variables, and other custom metadata collected by the Contrast agent.

**Tool Signature:**
```
list_session_metadata_for_application(app_name: String) -> MetadataFilterResponse
```

**Testing Approach:** Each test case describes the type of test data needed, how to identify it in the Contrast installation, and how to verify the results. Tests are designed to be executed by an AI agent that will:
1. Query the Contrast installation to find data matching test requirements
2. Execute the tool with appropriate parameters
3. Verify the results meet expected criteria

---

## 1. Basic Functionality Testing

### 1.1 Valid Application - Session Metadata Exists
**Objective:** Verify the tool successfully retrieves session metadata for a valid application with sessions.

**Test Data Requirements:**
- An application that has active sessions with session metadata
- Application should have been recently active to ensure latest session data exists

**Test Steps:**
1. Use `list_all_applications` to identify applications in the organization
2. Select an application that is known to have recent activity and sessions
3. Call `list_session_metadata_for_application` with `app_name="<selected-application-name>"`
4. Verify the response is returned successfully (no errors)
5. Verify the response is not null
6. Verify the response contains session metadata

**Expected Results:**
- Tool executes successfully without errors
- Response contains MetadataFilterResponse object
- Response includes session metadata from the latest session

---

### 1.2 Valid Application - Multiple Sessions
**Objective:** Verify the tool returns metadata from the LATEST session when multiple sessions exist.

**Test Data Requirements:**
- An application with multiple sessions (historical sessions and a recent one)
- Ability to identify which session is the latest (by timestamp or session ID)

**Test Steps:**
1. Identify an application with multiple sessions over time
2. Note the latest session identifier (if visible through other APIs or UI)
3. Call `list_session_metadata_for_application` with the application name
4. Verify the response contains metadata from the most recent session only
5. If session IDs are available in the response, verify it matches the latest session ID

**Expected Results:**
- Returns metadata from the latest/most recent session only
- Does not include metadata from older sessions
- Session timestamp or ID (if present) indicates latest session

---

### 1.3 Valid Application - Application Name Lookup
**Objective:** Verify the tool correctly resolves application name to application ID.

**Test Data Requirements:**
- An application with a known name and known application ID
- Session metadata exists for this application

**Test Steps:**
1. Use `list_all_applications` to get an application name and its corresponding ID
2. Call `list_session_metadata_for_application` with the application name
3. Verify the tool successfully retrieves session metadata
4. Optionally verify by calling SDK directly with the application ID to confirm same results

**Expected Results:**
- Application name is correctly resolved to application ID
- Session metadata is retrieved for the correct application
- Results match what would be returned using the application ID directly

---

## 2. Metadata Structure Validation

### 2.1 Response Structure - MetadataFilterResponse
**Objective:** Verify the response conforms to the expected MetadataFilterResponse structure from the Contrast SDK.

**Test Data Requirements:**
- Any application with session metadata

**Test Steps:**
1. Call `list_session_metadata_for_application` with a valid application name
2. Examine the response structure
3. Verify the response is a MetadataFilterResponse object (or JSON equivalent)
4. Verify the response contains expected fields from the SDK's MetadataFilterResponse:
   - Session metadata collection
   - Any other fields provided by the SDK

**Expected Results:**
- Response conforms to MetadataFilterResponse structure
- All SDK-documented fields are present
- Field types match SDK specification

---

### 2.2 Session Metadata Fields - Structure
**Objective:** Verify session metadata objects contain expected fields.

**Test Data Requirements:**
- An application with session metadata containing various metadata items

**Test Steps:**
1. Call `list_session_metadata_for_application` with a valid application name
2. Extract session metadata from the response
3. For each session metadata object, verify it contains:
   - `session_id` (string) - the agent session identifier
   - `metadata` (array) - collection of metadata items
4. Verify each metadata item in the collection contains:
   - `value` (string) - the metadata value
   - `display_label` (string) - human-readable label
   - `agent_label` (string) - agent-internal label

**Expected Results:**
- Session metadata contains `session_id` field
- Session metadata contains `metadata` array
- Each metadata item has `value`, `display_label`, and `agent_label` fields
- All fields have appropriate data types (strings)

---

### 2.3 Metadata Items - Common Types
**Objective:** Verify common metadata types are present in the response.

**Test Data Requirements:**
- An application with diverse session metadata (user information, request parameters, etc.)

**Test Steps:**
1. Call `list_session_metadata_for_application` with an application that has rich metadata
2. Examine the metadata items in the response
3. Identify common metadata types such as:
   - User identifiers (e.g., username, user ID)
   - Request parameters (e.g., URL parameters, headers)
   - Session attributes (e.g., session ID, session timeout)
   - Environment variables
   - Custom metadata
4. Verify each metadata item has meaningful `display_label` and `value` pairs

**Expected Results:**
- Response includes diverse metadata types
- `display_label` provides human-readable context for each metadata item
- `value` contains the actual metadata value
- `agent_label` provides agent-specific identification

---

### 2.4 Metadata Values - Data Types and Encoding
**Objective:** Verify metadata values are properly encoded and represented as strings.

**Test Data Requirements:**
- An application with metadata containing various data types (numbers, booleans, strings, special characters)

**Test Steps:**
1. Identify an application with metadata containing:
   - Numeric values (e.g., user ID: 12345)
   - Boolean values (e.g., authenticated: true)
   - String values with special characters (e.g., email with @, URLs with /)
   - Empty or null values
2. Call `list_session_metadata_for_application` for this application
3. Verify all values are represented as strings
4. Verify special characters are properly encoded/escaped
5. Verify empty/null values are handled gracefully

**Expected Results:**
- All metadata values are strings (even if original value was numeric/boolean)
- Special characters are properly encoded
- No encoding errors or malformed values
- Empty/null values handled appropriately (empty string or omitted)

---

## 3. Empty Results and Edge Cases

### 3.1 Valid Application - No Session Metadata
**Objective:** Verify behavior when application exists but has no session metadata.

**Test Data Requirements:**
- An application that exists but has no recorded sessions or no session metadata
- This might be a newly onboarded application or one with no recent activity

**Test Steps:**
1. Identify an application with no sessions or no session metadata
2. Call `list_session_metadata_for_application` with this application name
3. Verify the response is successful (no exception thrown)
4. Verify the response structure is valid but contains no metadata items:
   - Response may contain empty metadata array
   - Response may contain no session metadata objects

**Expected Results:**
- Tool executes successfully (no exception)
- Response structure is valid but empty:
  - Empty metadata array, or
  - Empty response indicating no sessions
- No error messages
- Graceful handling of empty state

---

### 3.2 Valid Application - No Recent Sessions
**Objective:** Verify behavior when application has old sessions but no recent activity.

**Test Data Requirements:**
- An application that had sessions in the past but none recently
- Contrast installation should have retention policies that might affect old session data

**Test Steps:**
1. Identify an application with old sessions but no recent activity
2. Call `list_session_metadata_for_application` with this application name
3. Verify the response behavior:
   - Either returns the latest available session (even if old), or
   - Returns empty response indicating no current sessions
4. Check if timestamps (if available) indicate old data

**Expected Results:**
- Tool executes successfully
- Either returns latest available session or empty response
- Behavior is consistent and predictable
- If old data is returned, timestamps reflect the actual age

---

### 3.3 Empty Application Name
**Objective:** Verify behavior when application name parameter is empty.

**Test Data Requirements:**
- None (testing validation)

**Test Steps:**
1. Call `list_session_metadata_for_application` with `app_name=""`
2. Verify the response behavior:
   - Tool should fail to find application with empty name
   - IOException should be thrown with message about application not found
3. Verify error message is helpful and indicates the issue

**Expected Results:**
- Tool fails gracefully
- IOException thrown with message: "Failed to list session metadata for application: [empty] application name not found."
- No system errors or crashes

---

### 3.4 Null or Whitespace Application Name
**Objective:** Verify behavior when application name is null or only whitespace.

**Test Data Requirements:**
- None (testing validation)

**Test Steps:**
1. Call `list_session_metadata_for_application` with `app_name="   "` (whitespace)
2. Verify the tool fails to find the application
3. Verify IOException is thrown
4. Verify error message indicates application not found
5. If possible, test with null parameter (may depend on MCP framework handling)

**Expected Results:**
- Whitespace-only names handled as invalid
- IOException thrown with appropriate message
- No system errors
- Null parameter handled by framework or throws appropriate error

---

## 4. Validation - Invalid Application Names

### 4.1 Nonexistent Application Name
**Objective:** Verify behavior when application name does not exist in the organization.

**Test Data Requirements:**
- None (using a fabricated application name)

**Test Steps:**
1. Call `list_session_metadata_for_application` with `app_name="nonexistent-application-12345"`
2. Verify the tool fails appropriately
3. Verify IOException is thrown
4. Verify error message clearly states: "Failed to list session metadata for application: nonexistent-application-12345 application name not found."
5. Verify no partial data is returned

**Expected Results:**
- Tool fails with IOException (not silent failure)
- Clear error message identifying the application name that was not found
- No partial or incorrect data returned
- Error handling is consistent with similar tools

---

### 4.2 Invalid Application Name - Special Characters
**Objective:** Verify behavior when application name contains special characters.

**Test Data Requirements:**
- Application names may legitimately contain special characters (spaces, dashes, underscores, etc.)

**Test Steps:**
1. Identify if any applications in the organization have special characters in their names
2. If such applications exist:
   - Call `list_session_metadata_for_application` with the exact application name including special characters
   - Verify the tool correctly handles and resolves the name
3. If no such applications exist:
   - Call with a name containing special characters (e.g., "app-with-dashes", "app with spaces", "app_underscores")
   - Verify appropriate "not found" error is returned
4. Test with potentially problematic characters:
   - Spaces: "My Application"
   - Dashes: "my-application"
   - Underscores: "my_application"
   - Dots: "my.application"
   - Slashes: "my/application" (should fail)

**Expected Results:**
- Legitimate special characters in valid application names work correctly
- Invalid or nonexistent names with special characters return "not found" error
- No encoding errors or system failures
- Special characters do not cause unexpected behavior

---

### 4.3 Case Sensitivity in Application Names
**Objective:** Verify case sensitivity behavior when looking up application names.

**Test Data Requirements:**
- An application with a known name (e.g., "MyApplication")

**Test Steps:**
1. Identify an application with mixed-case name (e.g., "MyApplication")
2. Call `list_session_metadata_for_application` with exact case: `app_name="MyApplication"`
3. Verify successful retrieval
4. Call again with different case: `app_name="myapplication"` (all lowercase)
5. Observe the behavior:
   - If lookup is case-sensitive: expect "not found" error
   - If lookup is case-insensitive: expect successful retrieval
6. Call with different case variations: `app_name="MYAPPLICATION"` (all uppercase)
7. Document the observed case sensitivity behavior

**Expected Results:**
- Behavior is consistent (either case-sensitive or case-insensitive)
- If case-sensitive:
  - Exact case match required for success
  - Different case returns "not found" error
- If case-insensitive:
  - All case variations retrieve the same application
- Error messages (if any) are clear

---

### 4.4 Application Name with Leading/Trailing Whitespace
**Objective:** Verify handling of whitespace around application names.

**Test Data Requirements:**
- An application with a known name (e.g., "TestApp")

**Test Steps:**
1. Identify an application with a known name
2. Call `list_session_metadata_for_application` with leading whitespace: `app_name=" TestApp"`
3. Observe behavior (whitespace trimmed or treated literally)
4. Call with trailing whitespace: `app_name="TestApp "`
5. Call with both: `app_name=" TestApp "`
6. Verify consistent behavior

**Expected Results:**
- Whitespace is either:
  - Trimmed automatically (preferred behavior): all variations succeed
  - Treated literally: variations with whitespace fail with "not found"
- Behavior is consistent across leading, trailing, and both
- Error messages are clear if whitespace causes lookup failure

---

### 4.5 Very Long Application Name
**Objective:** Verify handling of unusually long application names.

**Test Data Requirements:**
- None (testing edge case)

**Test Steps:**
1. Call `list_session_metadata_for_application` with a very long application name:
   - 100 characters
   - 500 characters
   - 1000 characters
2. Verify the tool handles long names gracefully:
   - No buffer overflow or system errors
   - Returns "not found" error (assuming name doesn't exist)
   - Error message is properly formatted (not truncated or malformed)

**Expected Results:**
- Long names handled without system errors
- Appropriate "not found" error returned
- No performance degradation or timeouts
- Error messages remain readable and well-formed

---

## 5. Latest Session Verification

### 5.1 Latest Session - Timestamp Verification
**Objective:** Verify the tool returns metadata from the most recent session by timestamp.

**Test Data Requirements:**
- An application with multiple sessions at different times
- Access to session timestamps or ability to infer session order

**Test Steps:**
1. Identify an application with multiple historical sessions
2. If possible, use other tools or APIs to determine:
   - All session IDs for the application
   - Timestamps or order of sessions
   - Which session is the latest
3. Call `list_session_metadata_for_application` for this application
4. Examine the returned session metadata
5. If session ID is included, verify it matches the expected latest session
6. If timestamps are available, verify they correspond to the latest session

**Expected Results:**
- Metadata is from the most recent session
- Session ID (if present) matches latest session
- Timestamps (if present) indicate latest activity
- Does not return metadata from older sessions

---

### 5.2 Latest Session - After New Session Created
**Objective:** Verify the tool returns updated metadata after a new session is created.

**Test Data Requirements:**
- An application where you can trigger new session creation
- Ability to distinguish between old and new sessions (different metadata values)

**Test Steps:**
1. Call `list_session_metadata_for_application` for an application
2. Note the session ID and metadata values
3. Trigger a new session for the application:
   - Restart the application, or
   - Trigger new agent activity that creates a new session
4. Wait for the new session to be registered in Contrast
5. Call `list_session_metadata_for_application` again
6. Verify the returned metadata is from the NEW session:
   - Session ID is different (if present)
   - Metadata values reflect new session (if different)

**Expected Results:**
- Tool returns metadata from the new session after it's created
- Previous session metadata is not returned
- Latest session is correctly identified as the new one

---

### 5.3 Latest Session - Multiple Concurrent Sessions
**Objective:** Verify behavior when application has multiple concurrent sessions.

**Test Data Requirements:**
- An application with multiple concurrent active sessions (multiple instances or nodes)
- This is common in clustered or load-balanced environments

**Test Steps:**
1. Identify an application running multiple instances/nodes with concurrent sessions
2. Call `list_session_metadata_for_application`
3. Verify the response contains metadata from ONE session (the latest)
4. If possible, determine which session is considered "latest":
   - Most recent activity timestamp
   - Most recent session start time
   - Highest session ID (if numerically ordered)
5. Verify consistency: calling multiple times should return same session (unless new activity)

**Expected Results:**
- Returns metadata from a single "latest" session
- Definition of "latest" is consistent and predictable
- Does not return merged metadata from multiple sessions
- Does not return all concurrent sessions

---

### 5.4 Latest Session - Definition Clarity
**Objective:** Understand and document what "latest" means in the context of this tool.

**Test Data Requirements:**
- Application with session activity over time

**Test Steps:**
1. Through testing and observation, determine the definition of "latest session":
   - Most recent session start time?
   - Most recent session activity (last heartbeat)?
   - Most recent metadata update?
   - Session with highest ID?
2. Document the observed behavior
3. Verify the behavior is consistent across multiple calls
4. Verify the behavior makes sense for the tool's intended use case

**Expected Results:**
- Clear understanding of "latest session" definition
- Behavior is consistent and logical
- Documentation accurately reflects the behavior
- Use case is supported by the "latest" definition

---

## 6. Integration Testing

### 6.1 Integration with list_all_applications
**Objective:** Verify application names from `list_all_applications` work correctly with this tool.

**Test Data Requirements:**
- Multiple applications in the organization

**Test Steps:**
1. Call `list_all_applications` to retrieve all application names
2. Select several application names from the response
3. For each application name:
   - Call `list_session_metadata_for_application` with that name
   - Verify successful retrieval or appropriate "no metadata" response
4. Verify no application names from `list_all_applications` cause unexpected errors

**Expected Results:**
- All application names from `list_all_applications` are valid inputs
- No encoding or format mismatches
- Tool successfully processes all valid application names
- Consistent behavior across all applications

---

### 6.2 Integration with list_vulns_by_app_and_metadata
**Objective:** Verify session metadata from this tool can be used to filter vulnerabilities.

**Test Data Requirements:**
- An application with both session metadata and vulnerabilities
- Vulnerabilities that have session metadata attached

**Test Steps:**
1. Call `list_session_metadata_for_application` for an application
2. Extract a metadata name/value pair from the response:
   - Use `display_label` as the metadata name
   - Use `value` as the metadata value
3. Call `list_vulns_by_app_and_metadata` with:
   - Same application name
   - Metadata name and value from step 2
4. Verify the vulnerability query executes successfully
5. If vulnerabilities are returned, verify they have matching session metadata

**Expected Results:**
- Metadata from this tool is compatible with vulnerability filtering tool
- Metadata names and values work correctly as filter parameters
- Integration between the two tools works seamlessly
- Results are logically consistent

---

### 6.3 Integration with list_latest_vulnerabilities_for_application
**Objective:** Verify session metadata concept aligns with "latest" vulnerabilities tool.

**Test Data Requirements:**
- An application with recent vulnerabilities

**Test Steps:**
1. Call `list_latest_vulnerabilities_for_application` for an application
2. Note that this tool uses latest session metadata internally
3. Call `list_session_metadata_for_application` for the same application
4. Verify the session metadata returned matches or is consistent with what the "latest vulnerabilities" tool would use
5. If vulnerability response includes session metadata, verify it matches

**Expected Results:**
- Both tools use consistent definition of "latest session"
- Session metadata is aligned between tools
- No conflicting or inconsistent session information
- Tools complement each other logically

---

## 7. Error Handling and Robustness

### 7.1 SDK Connection Error
**Objective:** Verify graceful handling when SDK cannot connect to Contrast.

**Test Data Requirements:**
- Ability to simulate connection failure (invalid credentials, network issue, etc.)
- Note: This may be difficult to test without environment manipulation

**Test Steps:**
1. If possible, temporarily cause SDK connection to fail:
   - Invalid API credentials
   - Network isolation
   - Contrast server unavailable
2. Call `list_session_metadata_for_application` with a valid application name
3. Verify the tool fails gracefully:
   - IOException is thrown
   - Error message indicates connection or SDK problem
   - No system crashes or hanging
4. Restore connection and verify tool resumes normal operation

**Expected Results:**
- Connection errors result in IOException with descriptive message
- No hanging or indefinite waits
- Error message helps diagnose the issue (connection problem, not application problem)
- System remains stable

---

### 7.2 SDK API Error
**Objective:** Verify handling when Contrast API returns an error.

**Test Data Requirements:**
- Scenario where API call might fail (permissions issue, API error, etc.)

**Test Steps:**
1. If possible, trigger an API error:
   - Request with insufficient permissions
   - Malformed request to SDK
   - Contrast server internal error
2. Call `list_session_metadata_for_application`
3. Verify appropriate exception is thrown
4. Verify error message provides useful information

**Expected Results:**
- API errors result in IOException with descriptive message
- Error message distinguishes API errors from "not found" errors
- System remains stable
- Errors are logged appropriately

---

### 7.3 Timeout Handling
**Objective:** Verify behavior if API call times out.

**Test Data Requirements:**
- Ability to cause slow response (network latency, large data set, etc.)

**Test Steps:**
1. If possible, cause a timeout scenario:
   - Very large session metadata response
   - Network latency
   - Slow Contrast server response
2. Call `list_session_metadata_for_application`
3. Verify timeout is handled gracefully:
   - Request times out after reasonable period
   - Exception is thrown with timeout indication
   - No indefinite hanging
4. Verify system recovers for subsequent requests

**Expected Results:**
- Timeouts result in exception after reasonable period
- Error message indicates timeout
- No indefinite waiting
- System recovers for subsequent requests

---

### 7.4 Malformed Response from SDK
**Objective:** Verify handling of unexpected or malformed responses from SDK.

**Test Data Requirements:**
- None (testing error handling)

**Test Steps:**
1. This test may require SDK mocking or unusual server state
2. If possible, trigger a scenario where SDK returns unexpected data:
   - Null response
   - Malformed JSON
   - Missing required fields
3. Call `list_session_metadata_for_application`
4. Verify the tool handles the situation gracefully:
   - Exception is thrown
   - Error message is helpful
   - No null pointer exceptions or system crashes

**Expected Results:**
- Malformed responses cause exceptions with helpful messages
- No uncaught exceptions or system crashes
- Error messages help diagnose the issue
- System remains stable

---

## 8. Performance Testing

### 8.1 Response Time - Normal Case
**Objective:** Measure typical response time for the tool.

**Test Data Requirements:**
- Application with normal amount of session metadata (not excessive)

**Test Steps:**
1. Call `list_session_metadata_for_application` for an application
2. Measure the response time from request to response
3. Repeat 5-10 times to get average
4. Verify response time is acceptable:
   - Target: < 5 seconds for normal cases
   - < 10 seconds is acceptable
   - > 10 seconds may indicate performance issue

**Expected Results:**
- Response time is acceptable (< 5 seconds typical)
- Response time is consistent across multiple calls
- No significant performance degradation

---

### 8.2 Response Time - Large Metadata
**Objective:** Measure response time with large amount of session metadata.

**Test Data Requirements:**
- Application with extensive session metadata (many metadata items)

**Test Steps:**
1. Identify application with large amount of session metadata:
   - Many custom metadata fields
   - Large values in metadata
   - Complex session configuration
2. Call `list_session_metadata_for_application` for this application
3. Measure response time
4. Verify response time is still acceptable
5. Verify large metadata doesn't cause memory issues

**Expected Results:**
- Large metadata handled efficiently
- Response time may be slower but still acceptable (< 15 seconds)
- No memory errors or out-of-memory exceptions
- Data is complete and not truncated

---

### 8.3 Concurrent Calls
**Objective:** Verify tool handles concurrent requests efficiently.

**Test Data Requirements:**
- Multiple applications with session metadata

**Test Steps:**
1. Make multiple concurrent calls to `list_session_metadata_for_application`:
   - Different application names
   - Same application name (multiple times)
2. Verify all calls complete successfully
3. Verify no data corruption or mixing between requests
4. Verify response times remain acceptable under concurrent load

**Expected Results:**
- All concurrent requests complete successfully
- No data mixing or corruption
- Response times remain acceptable
- No deadlocks or resource contention

---

## 9. Comparison with Similar Tools

### 9.1 Behavior Comparison - list_vulnerabilities vs list_session_metadata_for_application
**Objective:** Document behavioral differences between similar tools.

**Test Data Requirements:**
- Nonexistent application name

**Test Steps:**
1. Call `list_vulnerabilities` with nonexistent application name
2. Note the behavior: returns empty list (no exception)
3. Call `list_session_metadata_for_application` with same nonexistent name
4. Note the behavior: throws IOException
5. Document the different error handling approaches
6. Verify each approach makes sense for the tool's use case

**Expected Results:**
- Different error handling documented and understood
- `list_vulnerabilities`: returns empty list (allows silent failure)
- `list_session_metadata_for_application`: throws exception (explicit failure)
- Both approaches are reasonable for their respective use cases
- Documentation clarifies the difference

---

## Test Execution Guidelines

### For AI Test Executors

1. **Discovery Phase:** Start by querying the Contrast installation to understand what data is available
   - Use `list_all_applications` to see what applications exist
   - Identify applications with recent activity (likely to have sessions)
   - Identify applications with and without session metadata
   - Identify applications with varying amounts of metadata

2. **Test Selection:** Based on available data, determine which tests are feasible
   - Skip tests if required data doesn't exist
   - Prioritize basic functionality and validation tests
   - Document skipped tests and reasons

3. **Test Execution:** For each test:
   - Document the application name and parameters used
   - Document the expected results based on prior data discovery
   - Execute the tool call
   - Compare actual results with expected
   - Verify response structure and content
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
- The returned data matches expected criteria (latest session, correct application)
- Error messages are clear and helpful when failures occur
- Response structure matches SDK specification
- Session metadata contains expected fields and values
- Integration with other tools works correctly

### Failure Scenarios

A test fails if:
- Tool fails when it should succeed
- Returns metadata from wrong session (not latest)
- Returns metadata for wrong application
- Response structure differs from SDK specification
- Missing or malformed metadata fields
- Exception messages are unclear or misleading
- Performance is unacceptable (> 15 seconds for normal cases)
- System crashes or hangs

---

## Appendix: Quick Reference

### Tool Signature
```
list_session_metadata_for_application(app_name: String) -> MetadataFilterResponse
```

### Parameters
- **app_name** (required): Name of the application to retrieve session metadata for

### Return Type: MetadataFilterResponse
From Contrast SDK, containing session metadata for the latest session.

### Session Metadata Structure
```
SessionMetadata {
  session_id: String
  metadata: [
    MetadataItem {
      value: String
      display_label: String
      agent_label: String
    }
  ]
}
```

### Error Conditions
- **Application not found**: Throws IOException with message "Failed to list session metadata for application: {app_name} application name not found."
- **SDK connection error**: Throws IOException with connection details
- **API error**: Throws IOException with API error details

### Related Tools
- `list_all_applications` - Get list of application names
- `list_vulns_by_app_and_metadata` - Filter vulnerabilities by session metadata
- `list_latest_vulnerabilities_for_application` - Uses latest session metadata internally

### Common Metadata Types
Typical session metadata may include:
- User identifiers (username, user_id, email)
- Request parameters (URL parameters, headers, query strings)
- Session attributes (session_id, session_timeout, creation_time)
- Environment variables
- Application-specific custom metadata
- Authentication context (roles, permissions, auth_method)

### Testing Notes
- "Latest session" means the most recent session by activity or creation time
- Application name lookup may be case-sensitive (test to confirm)
- Empty/null application names should fail with clear error
- Tool returns data from ONE session only (not aggregated from multiple)
- Unlike `list_vulnerabilities`, this tool throws exception for nonexistent applications

---

## Document Version
**Version:** 1.0
**Date:** 2025-10-21
**Author:** Claude Code (AI Assistant)
**Tool Location:** `/src/main/java/com/contrast/labs/ai/mcp/contrast/AssessService.java` (lines 283-293)
