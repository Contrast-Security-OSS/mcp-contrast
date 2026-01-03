# Test Plan: list_Scan_Project Tool

## Overview
This test plan provides comprehensive coverage for the `list_Scan_Project` tool in the SastService class. This tool retrieves SAST scan project details by project name from Contrast Security.

**Tool Location**: `/Users/chrisedwards/projects/contrast/mcp-contrast/src/main/java/com/contrast/labs/ai/mcp/contrast/SastService.java` (lines 64-79)

**Test Execution Context**: These tests should be executed by an AI agent using the MCP server against a live Contrast Security instance with SAST scan projects configured.

---

## Tool Signature

```java
@Tool(name = "list_Scan_Project", description = "takes a scan project name and returns the project details")
public Project getScanProject(String projectName) throws IOException
```

**Parameters**:
- `projectName` (String, required): The name of the SAST scan project to retrieve

**Returns**:
- `Project` object: Contains scan project details from Contrast SAST

**Throws**:
- `IOException`: When project is not found or SDK encounters an error

---

## Test Categories

### 1. Basic Functionality Tests

#### Test 1.1: Retrieve Valid Project by Exact Name
**Description**: Successfully retrieve a SAST scan project using its exact name.

**Input**:
```
projectName: "<valid-project-name>"
```

**Expected Behavior**:
- Returns a Project object with complete details
- No exceptions thrown
- Log message: "Successfully found project: {projectName}"
- Project object contains non-null fields for:
  - Project ID
  - Project name (matches input)
  - Last scan ID (if scans exist)
  - Other project metadata

**Test Data Assumptions**:
- Assume at least one SAST scan project exists in the organization
- Record the exact project name before testing
- Project should have completed at least one scan

**Verification Steps**:
1. Verify Project object is returned (not null)
2. Verify project.name() matches the requested project name
3. Verify project.id() is not null
4. Verify project.lastScanId() exists if scans have been run
5. Check logs for success message

---

#### Test 1.2: Project with Multiple Scans
**Description**: Retrieve a project that has multiple completed scans.

**Input**:
```
projectName: "<project-with-multiple-scans>"
```

**Expected Behavior**:
- Returns Project object with complete scan history
- `lastScanId` field contains the most recent scan ID
- Project metadata reflects current state
- No exceptions thrown

**Test Data Assumptions**:
- Assume a project exists with at least 2-3 completed scans
- Know the expected last scan ID for verification

**Verification Steps**:
1. Verify Project object contains valid lastScanId
2. Verify lastScanId matches the most recent scan in TeamServer UI
3. Verify scan history is accessible through the project

---

#### Test 1.3: Newly Created Project (No Scans Yet)
**Description**: Retrieve a project that exists but has no completed scans yet.

**Input**:
```
projectName: "<new-project-without-scans>"
```

**Expected Behavior**:
- Returns Project object successfully
- Project exists with valid ID and name
- `lastScanId` may be null or empty (no scans yet)
- No exceptions thrown

**Test Data Assumptions**:
- Assume a project exists that has been created but not scanned
- Project is in initial state

**Verification Steps**:
1. Verify Project object is returned
2. Verify project.name() and project.id() are populated
3. Verify lastScanId handling (may be null or empty)
4. Confirm no errors due to missing scan data

---

### 2. Validation Tests

#### Test 2.1: Non-Existent Project Name
**Description**: Test with a project name that does not exist in the organization.

**Input**:
```
projectName: "NonExistentProject123XYZ"
```

**Expected Behavior**:
- Throws `IOException` with message: "Project not found"
- Log message: "Failed to find project NonExistentProject123XYZ: Project not found"
- No Project object returned
- Error is propagated to caller

**Test Data Assumptions**:
- Use a project name that definitely does not exist
- Verify the name is not present in TeamServer

**Verification Steps**:
1. Verify IOException is thrown
2. Verify error message is "Project not found"
3. Verify error is logged appropriately
4. Confirm SDK's findByName() returned Optional.empty()

---

#### Test 2.2: Null Project Name
**Description**: Test behavior when null is passed as project name.

**Input**:
```
projectName: null
```

**Expected Behavior**:
- Throws exception (NullPointerException or IOException)
- Error indicates invalid input
- Log message shows error attempting to find project
- No Project object returned

**Test Data Assumptions**:
- Any test data is acceptable

**Verification Steps**:
1. Verify appropriate exception is thrown
2. Verify error is logged with context
3. Confirm SDK handles null appropriately
4. No server-side errors or crashes

---

#### Test 2.3: Empty String Project Name
**Description**: Test behavior when empty string is passed as project name.

**Input**:
```
projectName: ""
```

**Expected Behavior**:
- Throws `IOException` with message: "Project not found"
- Empty string treated as non-existent project name
- Log message: "Failed to find project : Project not found"
- No Project object returned

**Test Data Assumptions**:
- Assume no project exists with an empty string name

**Verification Steps**:
1. Verify IOException is thrown
2. Verify empty string is handled gracefully
3. Verify error message is appropriate
4. No unexpected exceptions

---

#### Test 2.4: Whitespace-Only Project Name
**Description**: Test with project name containing only whitespace characters.

**Input**:
```
Test cases:
- projectName: " "
- projectName: "   "
- projectName: "\t"
- projectName: "\n"
```

**Expected Behavior**:
- Throws `IOException` with message: "Project not found"
- Whitespace-only names treated as non-existent
- No Project object returned
- Handled gracefully without crashes

**Test Data Assumptions**:
- Assume no project exists with whitespace-only names

**Verification Steps**:
1. Verify IOException for each whitespace variant
2. Verify consistent error handling across whitespace types
3. Verify no trimming/normalization occurs (exact match required)
4. No server-side issues

---

#### Test 2.5: Very Long Project Name
**Description**: Test with an extremely long project name string.

**Input**:
```
projectName: "<string-with-1000-characters>"
```

**Expected Behavior**:
- Throws `IOException` with message: "Project not found" (assuming no such project exists)
- No buffer overflow or truncation issues
- SDK handles long strings appropriately
- No performance degradation or timeout

**Test Data Assumptions**:
- Use a project name with 500-1000+ characters
- Assume no project with this name exists

**Verification Steps**:
1. Verify IOException is thrown (not found)
2. Verify no truncation or string handling errors
3. Verify response time is reasonable
4. No SDK or server errors

---

### 3. Project Name Matching Tests

#### Test 3.1: Case Sensitivity - Exact Match
**Description**: Verify that project name matching is case-sensitive (exact match required).

**Input**:
```
Assume project exists: "MyProject"

Test cases:
- projectName: "MyProject" (exact match)
- projectName: "myproject" (lowercase)
- projectName: "MYPROJECT" (uppercase)
- projectName: "myProject" (different casing)
```

**Expected Behavior**:
- "MyProject" (exact): Returns Project object successfully
- "myproject": Throws IOException "Project not found"
- "MYPROJECT": Throws IOException "Project not found"
- "myProject": Throws IOException "Project not found"
- Demonstrates case-sensitive matching (no normalization)

**Test Data Assumptions**:
- Assume a project exists with known casing (e.g., "MyProject")
- No projects exist with different case variations

**Verification Steps**:
1. Verify exact case match succeeds
2. Verify all case variations fail (case-sensitive)
3. Verify error messages for non-matches
4. Document case sensitivity behavior

---

#### Test 3.2: Special Characters in Project Name
**Description**: Test project names containing special characters.

**Input**:
```
Test cases (if such projects exist):
- projectName: "Project-With-Dashes"
- projectName: "Project_With_Underscores"
- projectName: "Project.With.Dots"
- projectName: "Project (With Parentheses)"
- projectName: "Project#123"
```

**Expected Behavior**:
- If project exists with exact name: Returns Project object
- If project does not exist: Throws IOException "Project not found"
- Special characters handled correctly by SDK
- No encoding or escaping issues

**Test Data Assumptions**:
- Create test projects with special characters in names
- Verify TeamServer allows these characters
- Record exact names for testing

**Verification Steps**:
1. Verify special characters are preserved (not encoded/escaped)
2. Verify exact match including special characters
3. Verify no URL encoding issues in SDK call
4. Test common special characters used in project names

---

#### Test 3.3: Project Name with Leading/Trailing Whitespace
**Description**: Test if leading or trailing whitespace affects matching.

**Input**:
```
Assume project exists: "MyProject"

Test cases:
- projectName: " MyProject"
- projectName: "MyProject "
- projectName: " MyProject "
```

**Expected Behavior**:
- If SDK does not trim: All throw IOException "Project not found"
- If SDK trims whitespace: All return Project object successfully
- Behavior should be consistent with SDK's findByName() implementation
- Document whitespace handling behavior

**Test Data Assumptions**:
- Assume a project "MyProject" exists (no whitespace in actual name)
- Test to determine SDK's whitespace handling

**Verification Steps**:
1. Test each whitespace variant
2. Document whether SDK trims whitespace
3. Verify consistent behavior across variants
4. Note implications for tool users

---

#### Test 3.4: Project Name with Unicode Characters
**Description**: Test project names containing Unicode/international characters.

**Input**:
```
Test cases (if such projects exist):
- projectName: "Проект" (Cyrillic)
- projectName: "项目" (Chinese)
- projectName: "プロジェクト" (Japanese)
- projectName: "Proyecto_ñ" (Spanish with ñ)
```

**Expected Behavior**:
- Unicode characters handled correctly
- Exact match works with proper encoding
- Returns Project object if project exists
- No encoding or character corruption issues

**Test Data Assumptions**:
- Create test projects with Unicode names (if supported by TeamServer)
- Verify TeamServer handles Unicode project names

**Verification Steps**:
1. Verify Unicode characters are preserved
2. Verify exact match works with international characters
3. Test multiple character encodings
4. Document Unicode support

---

#### Test 3.5: Partial Name Match (Should Not Work)
**Description**: Verify that partial matches do not return results (exact match only).

**Input**:
```
Assume project exists: "MyLongProjectName"

Test cases:
- projectName: "MyLong" (prefix)
- projectName: "ProjectName" (suffix)
- projectName: "LongProject" (substring)
```

**Expected Behavior**:
- All throw IOException "Project not found"
- No partial matching or fuzzy search
- Exact name match required
- Demonstrates strict matching behavior

**Test Data Assumptions**:
- Assume a project with a longer name exists
- No other projects with partial name matches exist

**Verification Steps**:
1. Verify all partial matches fail
2. Verify error message consistency
3. Document exact-match requirement
4. No wildcard or pattern matching

---

### 4. Data Completeness Tests

#### Test 4.1: Verify All Project Fields Populated
**Description**: Verify that the returned Project object contains all expected fields with valid data.

**Input**:
```
projectName: "<valid-project-with-scans>"
```

**Expected Behavior**:
- Project object contains all standard fields:
  - `id()`: Non-null, unique project identifier (UUID or similar)
  - `name()`: Matches the requested project name
  - `lastScanId()`: Present if scans exist, contains valid scan ID
  - Additional metadata fields (organization, creation date, etc. if available)
- All fields have appropriate types
- No null values for required fields

**Test Data Assumptions**:
- Assume a fully configured project with completed scans
- Project has been active with recent scan activity

**Verification Steps**:
1. Verify project.id() is not null and has valid format
2. Verify project.name() equals the requested name
3. Verify project.lastScanId() exists and is valid (if scans present)
4. Check all accessible Project fields
5. Document complete Project structure

---

#### Test 4.2: Project Metadata Accuracy
**Description**: Verify that project metadata matches what's shown in TeamServer UI.

**Input**:
```
projectName: "<known-project-name>"
```

**Expected Behavior**:
- Project ID matches TeamServer UI
- Project name matches exactly
- Last scan ID matches the most recent scan in UI
- All metadata fields are accurate and current
- No stale or cached data

**Test Data Assumptions**:
- Know the expected values from TeamServer UI
- Have reference data for comparison
- Project has recent activity

**Verification Steps**:
1. Compare returned Project.id() with UI
2. Compare Project.name() with UI
3. Compare Project.lastScanId() with latest scan in UI
4. Verify timestamps are recent (not stale cache)
5. Confirm data consistency across API and UI

---

#### Test 4.3: Project with No Scans - lastScanId Field
**Description**: Verify handling of lastScanId field when project has no scans.

**Input**:
```
projectName: "<project-with-no-scans>"
```

**Expected Behavior**:
- Project object returned successfully
- `lastScanId()` is null, empty, or default value
- No errors due to missing scan data
- Other project fields populated correctly

**Test Data Assumptions**:
- Assume a project exists with no completed scans
- Project is in initial/empty state

**Verification Steps**:
1. Verify Project object is returned
2. Check lastScanId() value (null or empty)
3. Verify no exceptions due to missing scans
4. Verify other project fields are still valid
5. Document behavior for scan-less projects

---

#### Test 4.4: Project Field Types and Format
**Description**: Verify that Project fields have correct types and formats.

**Input**:
```
projectName: "<valid-project-name>"
```

**Expected Behavior**:
- All fields have correct Java types
- IDs use appropriate format (UUID, long, string, etc.)
- Names are strings
- Timestamps (if present) are in valid format
- No type conversion errors

**Test Data Assumptions**:
- Any valid project is acceptable

**Verification Steps**:
1. Verify field types match Project class definition
2. Check ID format validity
3. Verify string fields don't have encoding issues
4. Check timestamp formats (if applicable)
5. Document Project data model

---

### 5. Error Handling Tests

#### Test 5.1: SDK Connection Failure
**Description**: Test behavior when SDK cannot connect to Contrast TeamServer.

**Input**:
```
projectName: "<any-project-name>"
(With invalid/unreachable TeamServer configuration)
```

**Expected Behavior**:
- Throws IOException with connection error details
- Error message indicates connection failure (not "Project not found")
- Log message: "Failed to find project {name}: {connection error}"
- Appropriate exception propagated to caller

**Test Data Assumptions**:
- Requires misconfigured connection or TeamServer downtime
- May need to temporarily modify configuration

**Verification Steps**:
1. Verify IOException is thrown
2. Verify error message distinguishes connection failure from not-found
3. Check logs for connection error details
4. Verify SDK initialization error handling

---

#### Test 5.2: Authentication Failure
**Description**: Test behavior when API credentials are invalid or expired.

**Input**:
```
projectName: "<any-project-name>"
(With invalid API credentials)
```

**Expected Behavior**:
- Throws IOException with authentication error
- Error message indicates permission/authentication issue
- Log message shows authentication failure
- No security information leaked in error messages

**Test Data Assumptions**:
- Requires invalid or expired API credentials
- May need test environment with auth enforcement

**Verification Steps**:
1. Verify IOException with authentication context
2. Verify error distinguishes auth failure from not-found
3. Check logs for auth error (without exposing credentials)
4. Verify appropriate error propagation

---

#### Test 5.3: Insufficient Permissions
**Description**: Test behavior when user lacks permission to access SAST projects.

**Input**:
```
projectName: "<valid-project-name>"
(With user account lacking SAST permissions)
```

**Expected Behavior**:
- Throws IOException with permission error
- Error message indicates insufficient permissions
- No partial data returned
- Appropriate error logged

**Test Data Assumptions**:
- Requires test account with limited permissions
- SAST feature access restricted for test user

**Verification Steps**:
1. Verify IOException is thrown
2. Verify error message indicates permission issue
3. Verify no data leakage despite permission failure
4. Check logs for permission context

---

#### Test 5.4: Timeout on Long-Running Query
**Description**: Test behavior if SDK call times out.

**Input**:
```
projectName: "<any-project-name>"
(In environment with slow response or timeout)
```

**Expected Behavior**:
- Throws IOException with timeout indication
- Error message indicates timeout (not "Project not found")
- Log message shows timeout details
- No indefinite hanging

**Test Data Assumptions**:
- Requires slow network or TeamServer response
- May need to adjust timeout settings for testing

**Verification Steps**:
1. Verify timeout exception is thrown
2. Verify timeout error message is clear
3. Check logs for timeout context
4. Verify operation doesn't hang indefinitely

---

#### Test 5.5: Malformed Response from SDK
**Description**: Test resilience when SDK returns unexpected data format.

**Input**:
```
projectName: "<valid-project-name>"
(In environment where SDK returns malformed data)
```

**Expected Behavior**:
- Handles malformed response gracefully
- Throws appropriate exception (IOException or parsing exception)
- Error message indicates data format issue
- No null pointer exceptions or crashes

**Test Data Assumptions**:
- Difficult to test without SDK mocking
- May require specific TeamServer version with known issues

**Verification Steps**:
1. Verify graceful error handling
2. Verify no crashes or null pointer exceptions
3. Check logs for parsing error details
4. Verify appropriate exception type

---

#### Test 5.6: Multiple Concurrent Requests
**Description**: Test behavior when multiple requests for different projects occur simultaneously.

**Input**:
```
Request multiple projects concurrently:
- projectName: "Project1"
- projectName: "Project2"
- projectName: "Project3"
```

**Expected Behavior**:
- All requests complete successfully
- Each returns correct Project data (no cross-contamination)
- No race conditions or threading issues
- SDK instances handled correctly

**Test Data Assumptions**:
- Assume 3+ projects exist
- Test from multiple threads or parallel AI agent requests

**Verification Steps**:
1. Verify all concurrent requests succeed
2. Verify each returns correct project (no mixing)
3. Check for any thread safety issues
4. Verify SDK connection pooling works correctly

---

### 6. Edge Cases and Boundary Tests

#### Test 6.1: Project Name at Maximum Length
**Description**: Test with project name at maximum allowed length by TeamServer.

**Input**:
```
projectName: "<max-length-project-name>"
```

**Expected Behavior**:
- If project exists: Returns Project object successfully
- If project doesn't exist: Throws IOException "Project not found"
- No truncation or length validation errors
- Maximum length handled correctly

**Test Data Assumptions**:
- Determine maximum project name length from TeamServer
- Create project at or near maximum length

**Verification Steps**:
1. Verify full-length name handled correctly
2. Verify no truncation in SDK call
3. Verify exact match works at max length
4. Document maximum length limit

---

#### Test 6.2: Project Name with SQL Injection Patterns
**Description**: Test that project names containing SQL-like patterns are handled safely.

**Input**:
```
Test cases:
- projectName: "Project'; DROP TABLE projects;--"
- projectName: "Project' OR '1'='1"
- projectName: "Project\" OR \"1\"=\"1"
```

**Expected Behavior**:
- Patterns treated as literal strings (no SQL injection)
- Throws IOException "Project not found" (assuming no such project)
- No security vulnerabilities
- SDK uses parameterized queries

**Test Data Assumptions**:
- These project names likely don't exist
- Test to verify security of SDK implementation

**Verification Steps**:
1. Verify SQL patterns treated as literal text
2. Verify "Project not found" error (not SQL error)
3. Verify no database errors or security issues
4. Confirm SDK uses safe query methods

---

#### Test 6.3: Project Name with Path Traversal Patterns
**Description**: Test that path traversal patterns are handled safely.

**Input**:
```
Test cases:
- projectName: "../../../etc/passwd"
- projectName: "..\\..\\windows\\system32"
- projectName: "Project/../../other"
```

**Expected Behavior**:
- Patterns treated as literal project names
- Throws IOException "Project not found"
- No file system access or path traversal
- SDK handles as project name, not file path

**Test Data Assumptions**:
- These project names don't exist
- Test to verify no path traversal vulnerabilities

**Verification Steps**:
1. Verify path patterns treated as literal strings
2. Verify "Project not found" error
3. Verify no file system access attempts
4. Verify no security vulnerabilities

---

#### Test 6.4: Rapid Repeated Requests for Same Project
**Description**: Test caching behavior and performance with repeated requests.

**Input**:
```
Request same project multiple times rapidly:
- projectName: "SameProject" (x10)
```

**Expected Behavior**:
- All requests succeed
- Consistent response data across all requests
- Reasonable performance (caching may help)
- No rate limiting issues

**Test Data Assumptions**:
- Assume one valid project exists
- Execute 10+ requests in quick succession

**Verification Steps**:
1. Verify all requests succeed
2. Verify consistent Project data returned
3. Measure response times (check for caching)
4. Verify no rate limiting errors
5. Check for any performance degradation

---

#### Test 6.5: Project Name with Only Numbers
**Description**: Test project name consisting only of numeric characters.

**Input**:
```
projectName: "123456"
```

**Expected Behavior**:
- If project exists: Returns Project object
- If project doesn't exist: Throws IOException "Project not found"
- Numeric name handled as string (not converted to integer)
- No type conversion issues

**Test Data Assumptions**:
- Create a project with numeric-only name (if supported)
- Or test with non-existent numeric name

**Verification Steps**:
1. Verify numeric string handled correctly
2. Verify no type conversion errors
3. Verify exact string match (not numeric match)
4. Document support for numeric project names

---

### 7. Integration Tests

#### Test 7.1: Integration with list_Scan_Results Tool
**Description**: Verify that Project data from list_Scan_Project can be used with list_Scan_Results.

**Input**:
```
Step 1: projectName: "<valid-project-name>" (list_Scan_Project)
Step 2: Use same projectName with list_Scan_Results
```

**Expected Behavior**:
- list_Scan_Project returns Project with valid lastScanId
- list_Scan_Results accepts same projectName
- Both tools work with same project data
- Consistent results across both tools

**Test Data Assumptions**:
- Assume a project with completed scans exists
- Both tools should access same underlying project

**Verification Steps**:
1. Get Project using list_Scan_Project
2. Verify lastScanId is present
3. Call list_Scan_Results with same projectName
4. Verify scan results are returned
5. Verify data consistency between tools

---

#### Test 7.2: Project Retrieved After New Scan Completes
**Description**: Verify that newly completed scans are reflected in Project data.

**Input**:
```
Step 1: Get Project before scan: projectName: "<project-name>"
Step 2: Run new scan on project (outside tool)
Step 3: Get Project after scan: projectName: "<project-name>"
```

**Expected Behavior**:
- Before scan: lastScanId is previous scan ID (or null)
- After scan: lastScanId is updated to new scan ID
- Project data reflects current state
- No stale cache data

**Test Data Assumptions**:
- Ability to trigger scan completion
- Access to scan before and after

**Verification Steps**:
1. Record initial lastScanId
2. Complete new scan
3. Retrieve project again
4. Verify lastScanId is updated
5. Verify no caching issues

---

#### Test 7.3: Listing All Projects then Retrieving Specific One
**Description**: Verify that project names from a project listing can be used to retrieve individual projects.

**Input**:
```
Step 1: List all SAST projects (if such tool exists)
Step 2: Extract project name from list
Step 3: projectName: "<name-from-list>"
```

**Expected Behavior**:
- Project name from list matches retrievable project
- Exact name match works
- Consistent data between list and detail view
- No naming inconsistencies

**Test Data Assumptions**:
- Assume project listing capability exists (via API or other tool)
- Multiple projects exist

**Verification Steps**:
1. Get project names from listing
2. Retrieve each project individually
3. Verify all retrievals succeed
4. Verify data consistency
5. Verify no naming format issues

---

### 8. Performance Tests

#### Test 8.1: Response Time for Single Project Retrieval
**Description**: Measure baseline response time for retrieving a project.

**Input**:
```
projectName: "<valid-project-name>"
```

**Expected Behavior**:
- Response within acceptable time (e.g., < 2 seconds)
- Consistent response times across multiple requests
- No significant performance degradation
- Log timing information

**Test Data Assumptions**:
- Assume normal network conditions
- TeamServer responsive

**Verification Steps**:
1. Record request timestamp
2. Record response timestamp
3. Calculate elapsed time
4. Compare against performance baseline
5. Test multiple times for consistency

---

#### Test 8.2: Performance with Large Project (Many Scans)
**Description**: Test performance when retrieving project with extensive scan history.

**Input**:
```
projectName: "<project-with-many-scans>"
```

**Expected Behavior**:
- Returns Project object successfully
- Response time reasonable (< 5 seconds)
- Large scan history doesn't cause timeout
- No performance issues with large datasets

**Test Data Assumptions**:
- Assume a project with 50+ completed scans
- Project has extensive scan history

**Verification Steps**:
1. Measure response time
2. Verify completion within timeout
3. Verify all data returned correctly
4. Compare to baseline performance
5. Check for any memory issues

---

---

## Test Execution Guidelines

### Pre-Test Setup
1. Verify MCP server is running and connected to a valid Contrast Security instance
2. Ensure test environment has:
   - At least 3-5 SAST scan projects with different characteristics:
     - Project with multiple completed scans
     - Project with single scan
     - Project with no scans (newly created)
   - Projects with various naming patterns (normal names, special characters, etc.)
   - Access to SAST functionality enabled for the test organization
   - Valid API credentials with SAST permissions

3. Document test data:
   - Record exact project names for testing
   - Note which projects have scans and how many
   - Identify projects for specific test scenarios
   - Verify project visibility in TeamServer UI

### During Testing
1. Record all request parameters for each test
2. Capture complete response including:
   - Full Project object structure
   - All field values
   - Any exceptions thrown
   - Error messages
3. Check log files at: `/tmp/mcp-contrast.log`
   - Look for info, debug, and error messages
   - Verify logging behavior matches expectations
4. Note any unexpected behavior or edge cases
5. Measure response times for performance tests

### Test Data Recommendations
For comprehensive testing, the test environment should ideally have:
- **Minimum 5 SAST projects** with different characteristics:
  - 2-3 projects with completed scans (different scan counts)
  - 1-2 newly created projects without scans
  - 1 project with special characters in name (if supported)
  - 1 project with very long name (test boundary)

- **Project Naming Variations**:
  - Standard alphanumeric names: "MyProject", "WebApp2024"
  - Names with special characters: "Project-Dev", "App_v2.0"
  - Names with spaces: "My Project" (if supported)
  - Mixed case names: "MyProject", "myproject", "MYPROJECT" (for case testing)

- **Scan History**:
  - At least one project with 10+ scans (for performance testing)
  - At least one project with exactly 1 scan
  - At least one project with 0 scans

### Success Criteria
Each test passes when:
1. Response matches expected behavior (success or specific error)
2. Project data is accurate and complete
3. Error handling is appropriate and informative
4. No unexpected exceptions or crashes occur
5. Log messages provide appropriate context
6. Performance is acceptable (within timeout limits)
7. Security: No injection vulnerabilities or data leakage

### Known Limitations and Behaviors
1. **Case Sensitivity**: Project names are case-sensitive (exact match required)
2. **No Partial Matching**: Only exact name matches work (no wildcards or fuzzy search)
3. **SDK Dependency**: Tool behavior depends on Contrast SDK's `findByName()` implementation
4. **Optional.orElseThrow Pattern**: When project not found, SDK returns empty Optional which triggers IOException
5. **SAST Feature Requirement**: Requires SAST functionality enabled in Contrast license
6. **No Pagination**: Returns single Project object (not a list)

---

## Appendix A: Example Test Execution Commands

These examples show how an AI agent might invoke the tool during testing:

```json
// Test 1.1: Retrieve valid project
{
  "projectName": "MyWebApp"
}

// Test 2.1: Non-existent project
{
  "projectName": "NonExistentProject123"
}

// Test 2.2: Null project name
{
  "projectName": null
}

// Test 2.3: Empty string
{
  "projectName": ""
}

// Test 3.1: Case sensitivity test
{
  "projectName": "MyProject"    // Assume this exists
}
{
  "projectName": "myproject"    // Should fail if above exists
}

// Test 3.2: Special characters
{
  "projectName": "Project-With-Dashes"
}

// Test 4.1: Data completeness
{
  "projectName": "ProjectWithScans"
}
// Then inspect: project.id(), project.name(), project.lastScanId()
```

---

## Appendix B: Expected Project Object Structure

Based on the Contrast SDK, the returned Project object should contain:

```java
interface Project {
    String id();              // Unique project identifier (UUID format)
    String name();            // Project name (matches input)
    String lastScanId();      // ID of most recent scan (may be null if no scans)
    // Additional fields as provided by SDK:
    // - organizationId
    // - createdTime
    // - lastScanTime
    // - configuration details
    // (Exact fields depend on SDK version and Project implementation)
}
```

**Verification Checklist for Each Test**:
- [ ] Project object is not null (on success cases)
- [ ] project.id() is not null and has valid format
- [ ] project.name() matches requested project name exactly
- [ ] project.lastScanId() is populated correctly (or null if no scans)
- [ ] Exception type is correct (IOException for expected errors)
- [ ] Error message is descriptive and accurate
- [ ] Log messages provide appropriate context

---

## Appendix C: Test Execution Worksheet

Use this worksheet to track test execution:

| Test ID | Test Name | Status | Notes | Timestamp |
|---------|-----------|--------|-------|-----------|
| 1.1     | Retrieve Valid Project | | | |
| 1.2     | Project with Multiple Scans | | | |
| 1.3     | Newly Created Project | | | |
| 2.1     | Non-Existent Project | | | |
| 2.2     | Null Project Name | | | |
| 2.3     | Empty String | | | |
| 2.4     | Whitespace-Only | | | |
| 2.5     | Very Long Name | | | |
| 3.1     | Case Sensitivity | | | |
| 3.2     | Special Characters | | | |
| 3.3     | Leading/Trailing Whitespace | | | |
| 3.4     | Unicode Characters | | | |
| 3.5     | Partial Name Match | | | |
| 4.1     | All Fields Populated | | | |
| 4.2     | Metadata Accuracy | | | |
| 4.3     | No Scans - lastScanId | | | |
| 4.4     | Field Types and Format | | | |
| 5.1     | SDK Connection Failure | | | |
| 5.2     | Authentication Failure | | | |
| 5.3     | Insufficient Permissions | | | |
| 5.4     | Timeout | | | |
| 5.5     | Malformed Response | | | |
| 5.6     | Concurrent Requests | | | |
| 6.1     | Maximum Length Name | | | |
| 6.2     | SQL Injection Patterns | | | |
| 6.3     | Path Traversal Patterns | | | |
| 6.4     | Rapid Repeated Requests | | | |
| 6.5     | Numeric-Only Name | | | |
| 7.1     | Integration with list_Scan_Results | | | |
| 7.2     | After New Scan | | | |
| 7.3     | From Project Listing | | | |
| 8.1     | Response Time Baseline | | | |
| 8.2     | Large Project Performance | | | |

**Status Values**: PASS | FAIL | SKIP | BLOCKED

---

## Test Coverage Summary

This test plan covers:
- ✓ 3 basic functionality test cases
- ✓ 5 validation test cases
- ✓ 5 project name matching test cases
- ✓ 4 data completeness test cases
- ✓ 6 error handling test cases
- ✓ 5 edge case and boundary test cases
- ✓ 3 integration test cases
- ✓ 2 performance test cases

**Total: 33 test cases**

Each test case is designed to be executed by an AI agent using the MCP server, with clear input parameters, expected behaviors, and test data assumptions.

---

## Revision History

| Version | Date | Changes | Author |
|---------|------|---------|--------|
| 1.0     | 2025-10-21 | Initial test plan creation | AI Agent |

---

**End of Test Plan**
