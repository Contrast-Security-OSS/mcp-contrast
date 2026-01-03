# Test Plan: list_vulnerabilities Tool

> **NOTE (AIML-189)**: As of the consolidation in AIML-189, the duplicate `list_vulnerabilities` (app_name variant) tool has been removed. The remaining tool has been renamed from `list_vulnerabilities_with_id` to `list_vulnerabilities` and now exclusively uses application ID as input. Users should call `list_applications_with_name` first to get the application ID from a name.

## Overview
This test plan provides comprehensive testing instructions for the `list_vulnerabilities` MCP tool (formerly `list_vulnerabilities_with_id`) in the Contrast Security MCP server. This tool takes an Application ID (appID) and returns a list of lightweight vulnerability summaries for that application.

## Tool Details
- **Tool Name**: `list_vulnerabilities_with_id`
- **Location**: `/Users/chrisedwards/projects/contrast/mcp-contrast/src/main/java/com/contrast/labs/ai/mcp/contrast/AssessService.java` (lines 186-205)
- **Input**: Application ID (appID) as String
- **Output**: List<VulnLight> containing vulnerability summaries
- **Key Feature**: Includes session metadata with each vulnerability

## VulnLight Data Structure
Each vulnerability in the response contains:
- `title` - Human-readable vulnerability name
- `type` - Vulnerability type (e.g., "sql-injection", "xss-reflected")
- `vulnID` - Unique identifier (UUID) for the vulnerability
- `severity` - Severity level (CRITICAL, HIGH, MEDIUM, LOW, NOTE)
- `sessionMetadata` - List of session metadata entries
- `lastSeenAt` - ISO 8601 timestamp of last occurrence
- `status` - Current status (Reported, Confirmed, Suspicious, etc.)
- `firstSeenAt` - ISO 8601 timestamp of first occurrence (nullable)
- `closedAt` - ISO 8601 timestamp of closure (nullable)
- `environments` - List of environments where vulnerability was detected

---

## Test Suite

### 1. Basic Functionality Tests

#### Test 1.1: List Vulnerabilities for Valid Application
**Objective**: Verify the tool returns vulnerability data for a valid application with known vulnerabilities.

**Prerequisites**:
- Application ID of a real application in your Contrast organization that has vulnerabilities
- Minimum 1-5 vulnerabilities in the application for meaningful testing

**Test Steps**:
1. Call the tool with a valid application ID:
   ```
   list_vulnerabilities_with_id(appID="<valid-app-id>")
   ```
2. Verify response is returned successfully
3. Verify response is a list of vulnerability objects
4. Verify list is not empty

**Expected Results**:
- Response returns without errors
- Response contains a non-empty list
- Each item in the list is a VulnLight object

**Data Needed**:
- Application ID with 1-5 vulnerabilities

---

#### Test 1.2: Verify All VulnLight Fields Are Present
**Objective**: Confirm that each vulnerability object contains all expected fields.

**Prerequisites**:
- Same as Test 1.1

**Test Steps**:
1. Call the tool with a valid application ID
2. Examine the first vulnerability in the returned list
3. Verify presence of all fields

**Expected Results**:
Each vulnerability object should contain:
- `title` (non-empty string)
- `type` (non-empty string, vulnerability rule name)
- `vulnID` (non-empty string, UUID format)
- `severity` (one of: CRITICAL, HIGH, MEDIUM, LOW, NOTE)
- `sessionMetadata` (list, may be empty)
- `lastSeenAt` (ISO 8601 timestamp string)
- `status` (non-empty string)
- `firstSeenAt` (ISO 8601 timestamp string or null)
- `closedAt` (ISO 8601 timestamp string or null)
- `environments` (list, may be empty)

**Data Needed**:
- Application ID with at least 1 vulnerability

---

#### Test 1.3: Verify Vulnerability Ordering
**Objective**: Confirm vulnerabilities are returned in a consistent order.

**Prerequisites**:
- Application with multiple vulnerabilities (5+)

**Test Steps**:
1. Call the tool with an application ID that has 5+ vulnerabilities
2. Note the order of vulnerabilities returned
3. Call the tool again with the same application ID
4. Compare the order of vulnerabilities

**Expected Results**:
- Vulnerabilities are returned in a consistent, repeatable order
- Order matches the Contrast API's default ordering

**Data Needed**:
- Application ID with 5+ vulnerabilities

---

### 2. Validation Tests

#### Test 2.1: Invalid Application ID - Nonexistent UUID
**Objective**: Verify tool handles requests for non-existent applications gracefully.

**Prerequisites**:
- A valid UUID format that does not correspond to any application in the organization

**Test Steps**:
1. Call the tool with a properly formatted but non-existent application ID:
   ```
   list_vulnerabilities_with_id(appID="00000000-0000-0000-0000-000000000000")
   ```

**Expected Results**:
- Tool should either:
  - Return an empty list (if API treats non-existent app as having no vulnerabilities), OR
  - Throw an IOException with an error message about the application not being found
- Should NOT cause a system crash or hang

**Data Needed**:
- A non-existent but valid UUID format

---

#### Test 2.2: Invalid Application ID - Malformed String
**Objective**: Verify tool handles malformed application IDs.

**Prerequisites**:
- None

**Test Steps**:
1. Call the tool with various malformed application IDs:
   ```
   list_vulnerabilities_with_id(appID="not-a-valid-uuid")
   list_vulnerabilities_with_id(appID="12345")
   list_vulnerabilities_with_id(appID="app-123-invalid")
   ```

**Expected Results**:
- Tool should throw an IOException or return appropriate error
- Error message should indicate the application ID is invalid
- Should NOT cause a system crash

**Data Needed**:
- Various malformed string values

---

#### Test 2.3: Empty Application ID
**Objective**: Verify tool handles empty or whitespace-only application IDs.

**Prerequisites**:
- None

**Test Steps**:
1. Call the tool with empty or whitespace strings:
   ```
   list_vulnerabilities_with_id(appID="")
   list_vulnerabilities_with_id(appID="   ")
   ```

**Expected Results**:
- Tool should throw an IOException or return appropriate error
- Should NOT attempt to call the Contrast API with empty parameter
- Should NOT cause a system crash

**Data Needed**:
- Empty string and whitespace-only strings

---

#### Test 2.4: Null Application ID
**Objective**: Verify tool handles null application ID parameter.

**Prerequisites**:
- None

**Test Steps**:
1. Call the tool with null appID (if framework allows):
   ```
   list_vulnerabilities_with_id(appID=null)
   ```

**Expected Results**:
- Tool should throw an exception or return appropriate error
- Should NOT cause a NullPointerException
- Error message should clearly indicate the parameter is required

**Data Needed**:
- None

---

### 3. Empty Results Tests

#### Test 3.1: Application with No Vulnerabilities
**Objective**: Verify tool correctly handles applications that have no vulnerabilities.

**Prerequisites**:
- Application ID of an application that exists but has zero vulnerabilities

**Test Steps**:
1. Verify the application exists and has 0 vulnerabilities (check in Contrast UI)
2. Call the tool with this application ID:
   ```
   list_vulnerabilities_with_id(appID="<app-with-no-vulns>")
   ```

**Expected Results**:
- Tool returns successfully (no error/exception)
- Response is an empty list: `[]`
- Tool logs indicate successful retrieval of 0 vulnerabilities

**Data Needed**:
- Application ID with zero vulnerabilities (may need to create a clean test application)

---

#### Test 3.2: Newly Created Application
**Objective**: Verify tool handles applications that exist but have never been scanned.

**Prerequisites**:
- Newly created application that has not yet reported any data

**Test Steps**:
1. Create a new application in Contrast (or use an application that just started)
2. Call the tool immediately:
   ```
   list_vulnerabilities_with_id(appID="<new-app-id>")
   ```

**Expected Results**:
- Tool returns successfully
- Response is an empty list
- No errors or exceptions

**Data Needed**:
- Newly created application ID

---

### 4. Data Completeness Tests

#### Test 4.1: Verify vulnID Is Always Present
**Objective**: Ensure the critical vulnID field is included in all vulnerability records.

**Prerequisites**:
- Application with multiple vulnerabilities

**Test Steps**:
1. Call the tool with an application that has 5+ vulnerabilities
2. Iterate through all returned vulnerabilities
3. For each vulnerability, verify:
   - `vulnID` field exists
   - `vulnID` is not null
   - `vulnID` is not empty string
   - `vulnID` matches UUID format pattern

**Expected Results**:
- Every vulnerability has a `vulnID` field
- All `vulnID` values are non-empty strings
- All `vulnID` values match UUID format: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`

**Data Needed**:
- Application with 5+ vulnerabilities

---

#### Test 4.2: Verify vulnID Uniqueness
**Objective**: Confirm that each vulnerability has a unique vulnID within the response.

**Prerequisites**:
- Application with multiple vulnerabilities

**Test Steps**:
1. Call the tool with an application that has 10+ vulnerabilities
2. Extract all `vulnID` values from the response
3. Check for duplicates

**Expected Results**:
- All `vulnID` values are unique within the response
- No duplicate vulnerability IDs

**Data Needed**:
- Application with 10+ vulnerabilities

---

#### Test 4.3: Verify Timestamp Formatting
**Objective**: Confirm timestamps are in ISO 8601 format with timezone information.

**Prerequisites**:
- Application with vulnerabilities that have various timestamps

**Test Steps**:
1. Call the tool with an application that has vulnerabilities
2. Examine timestamp fields: `lastSeenAt`, `firstSeenAt`, `closedAt`
3. Verify format matches ISO 8601 with timezone

**Expected Results**:
- All non-null timestamps match pattern: `YYYY-MM-DDTHH:MM:SS±HH:MM`
- Examples of valid formats:
  - `2025-01-15T10:30:00+00:00`
  - `2025-02-19T13:20:00-05:00`
- Timestamps include timezone offset
- `lastSeenAt` is always present (non-null)
- `firstSeenAt` may be null for some vulnerabilities
- `closedAt` is null for open vulnerabilities

**Data Needed**:
- Application with vulnerabilities in various states (open, closed)

---

#### Test 4.4: Verify Session Metadata Inclusion
**Objective**: Confirm session metadata is included when available.

**Prerequisites**:
- Application with vulnerabilities that have session metadata
- Session metadata might include deployment info, build numbers, etc.

**Test Steps**:
1. Call the tool with an application known to have session metadata
2. Examine the `sessionMetadata` field in returned vulnerabilities
3. Verify structure and content

**Expected Results**:
- `sessionMetadata` field is present in all vulnerability objects
- `sessionMetadata` is a list (may be empty for some vulnerabilities)
- When present, session metadata contains name/value pairs
- Session metadata provides contextual information about when/where vulnerability was detected

**Data Needed**:
- Application with vulnerabilities that include session metadata

---

#### Test 4.5: Verify Environments Extraction
**Objective**: Confirm environments are correctly extracted and deduplicated.

**Prerequisites**:
- Application with vulnerabilities detected in multiple environments

**Test Steps**:
1. Call the tool with an application that has vulnerabilities in multiple environments
2. Examine the `environments` field for several vulnerabilities
3. Verify proper extraction and formatting

**Expected Results**:
- `environments` field is present and is a list
- Environments are deduplicated (no duplicates in the list)
- Environments are sorted alphabetically
- Common environment values: "DEVELOPMENT", "QA", "PRODUCTION"
- Empty list is acceptable if vulnerability has no associated servers

**Data Needed**:
- Application with vulnerabilities across multiple environments

---

### 5. Volume Handling Tests

#### Test 5.1: Application with Many Vulnerabilities (50-100)
**Objective**: Verify tool handles applications with moderate number of vulnerabilities.

**Prerequisites**:
- Application with 50-100 vulnerabilities

**Test Steps**:
1. Call the tool with an application that has 50-100 vulnerabilities
2. Measure response time
3. Verify all vulnerabilities are returned

**Expected Results**:
- Tool returns successfully
- Response time is reasonable (< 10 seconds)
- All vulnerabilities are included in response (count matches expected)
- No pagination is applied (full list returned)
- Memory usage is reasonable

**Data Needed**:
- Application with 50-100 vulnerabilities

---

#### Test 5.2: Application with Very Many Vulnerabilities (200+)
**Objective**: Test tool behavior with high-volume vulnerability data.

**Prerequisites**:
- Application with 200+ vulnerabilities (if available)

**Test Steps**:
1. Call the tool with an application that has 200+ vulnerabilities
2. Monitor response time and memory usage
3. Verify response completeness

**Expected Results**:
- Tool returns successfully (though may take longer)
- All vulnerabilities are returned
- Response time is acceptable (< 30 seconds)
- No memory errors or timeouts
- Note: If pagination is needed, this test may reveal that requirement

**Data Needed**:
- Application with 200+ vulnerabilities

**Note**: If no application with this many vulnerabilities exists, this test can be marked as "Not Applicable" but should be performed if such data becomes available.

---

#### Test 5.3: Performance Consistency
**Objective**: Verify consistent performance across multiple calls.

**Prerequisites**:
- Application with known number of vulnerabilities (20-50 range ideal)

**Test Steps**:
1. Call the tool 5 times in succession with the same application ID
2. Record response time for each call
3. Compare response times

**Expected Results**:
- Response times are relatively consistent (within 20% variance)
- No degradation in performance over multiple calls
- No memory leaks or resource exhaustion
- Results are identical across all calls

**Data Needed**:
- Application with 20-50 vulnerabilities

---

### 6. Error Handling Tests

#### Test 6.1: Network Timeout
**Objective**: Verify tool handles network timeouts gracefully.

**Prerequisites**:
- Ability to simulate network issues (may require test environment configuration)

**Test Steps**:
1. Configure network to introduce delays or timeouts
2. Call the tool with a valid application ID
3. Observe error handling

**Expected Results**:
- Tool should throw IOException with appropriate message
- Error message should indicate connection/timeout issue
- Should not hang indefinitely
- Should clean up resources properly

**Data Needed**:
- Valid application ID
- Test environment with network control

---

#### Test 6.2: Authentication Failure
**Objective**: Verify tool handles authentication errors appropriately.

**Prerequisites**:
- Ability to test with invalid credentials (separate test instance recommended)

**Test Steps**:
1. Configure MCP server with invalid API credentials
2. Attempt to call the tool
3. Observe error handling

**Expected Results**:
- Tool should throw IOException or UnauthorizedException
- Error message should indicate authentication failure
- Should not expose sensitive credential information in error messages

**Data Needed**:
- Invalid API credentials (test safely!)

---

#### Test 6.3: Partial Data Response
**Objective**: Verify tool handles incomplete data from API.

**Prerequisites**:
- This may require API mocking or specific test conditions

**Test Steps**:
1. If possible, simulate API returning incomplete vulnerability data
2. Call the tool
3. Verify tool handles missing fields appropriately

**Expected Results**:
- Tool should either:
  - Use default/null values for missing optional fields
  - Throw appropriate exception for missing required fields
- Should not cause NullPointerException
- Should log warnings about incomplete data

**Data Needed**:
- Test scenario with incomplete API responses (may require mocking)

---

## Test Execution Notes

### Required Test Data Setup
1. **Valid Application with Vulnerabilities**: Primary test application with 10-20 vulnerabilities covering various severities and types
2. **Application with No Vulnerabilities**: Clean application for empty result testing
3. **High-Volume Application**: Application with 50+ vulnerabilities if available
4. **Invalid Application IDs**: Collection of malformed IDs for validation testing

### Test Environment Requirements
- Running instance of mcp-contrast server
- Valid Contrast Security credentials configured
- Access to Contrast organization with test applications
- MCP client capable of calling tools (e.g., Claude with MCP integration)

### Test Execution Order
Recommended execution order:
1. Start with Test 1.1 (Basic Functionality) to confirm tool is working
2. Run Data Completeness tests (Section 4) to verify output format
3. Execute Validation tests (Section 2) to confirm error handling
4. Run Empty Results tests (Section 3)
5. Perform Volume Handling tests (Section 5)
6. Finally, run Error Handling tests (Section 6) if environment permits

### Success Criteria
The `list_vulnerabilities_with_id` tool passes testing if:
- All basic functionality tests pass (Section 1)
- All validation tests properly handle invalid inputs (Section 2)
- Empty results are handled correctly (Section 3)
- `vulnID` and all required fields are always present (Section 4)
- Tool handles at least 50 vulnerabilities efficiently (Section 5)
- Error conditions are handled gracefully (Section 6)

### Known Limitations
- This tool does not support pagination - it returns all vulnerabilities for an application
- For applications with thousands of vulnerabilities, performance may degrade
- The tool returns lightweight vulnerability summaries - detailed vulnerability information requires using other tools

---

## Appendix: Expected Tool Behavior

### Successful Response Example
```json
[
  {
    "title": "SQL Injection",
    "type": "sql-injection",
    "vulnID": "1A2B-3C4D-5E6F-7890",
    "severity": "HIGH",
    "sessionMetadata": [
      {
        "name": "buildNumber",
        "value": "1.2.3"
      }
    ],
    "lastSeenAt": "2025-01-15T10:30:00+00:00",
    "status": "Reported",
    "firstSeenAt": "2025-01-10T08:15:00+00:00",
    "closedAt": null,
    "environments": ["DEVELOPMENT", "QA"]
  }
]
```

### Empty Response Example
```json
[]
```

### Error Response Example
Tool should throw an IOException with descriptive message:
```
IOException: Failed to list vulnerabilities: Application not found
```

---

## Test Results Template

For each test, record results using this format:

**Test ID**: [e.g., Test 1.1]
**Test Name**: [e.g., List Vulnerabilities for Valid Application]
**Date**: [YYYY-MM-DD]
**Tester**: [Name or AI Agent ID]
**Status**: [PASS/FAIL/BLOCKED/NOT APPLICABLE]
**Application ID Used**: [actual appID]
**Vulnerabilities Returned**: [count]
**Notes**: [Any observations, issues, or additional context]
**Evidence**: [Log excerpts, screenshots, or response samples]

---

## Version History
- **v1.0** - 2025-10-21 - Initial test plan created
