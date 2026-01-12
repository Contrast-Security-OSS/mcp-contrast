# Test Plan: get_scan_project Tool

## Overview

This test plan provides comprehensive testing guidance for the `get_scan_project` MCP tool. This tool retrieves SAST (Contrast Scan) project details by project name.

### Migration Notes

**This plan replaces:**
- `test-plan-list_Scan_Project.md` (original at root level)

**Key Changes from Original Tool:**
- **Tool renamed**: `list_Scan_Project` → `get_scan_project`
- **Follows tool-per-class pattern**: Uses `SingleTool<GetSastProjectParams, Project>`
- **Enhanced validation**: Uses `GetSastProjectParams` for parameter validation
- **SDK Project record**: Returns SDK `Project` record (immutable)

### Tool Signature

**MCP Tool Name:** `get_scan_project`

**Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `projectName` | String | Yes | Scan project name (case-sensitive, must match exactly) |

### Response Structure

**Returns:** `SingleToolResponse<Project>`

```java
SingleToolResponse {
    Project data,      // SAST project details from SDK
    String message,    // Warnings or info messages
    boolean found      // True if project found
}

// Project record (from Contrast SDK):
Project {
    String id(),                    // Unique project identifier (UUID)
    String name(),                  // Project name (matches input)
    String language(),              // Programming language (Java, JavaScript, etc.)
    String lastScanId(),            // ID of most recent scan (null if no scans)
    Instant lastScanTime(),         // When last scan completed
    int completedScans(),           // Total number of completed scans
    int critical(),                 // Critical severity findings
    int high(),                     // High severity findings
    int medium(),                   // Medium severity findings
    int low(),                      // Low severity findings
    int note()                      // Note severity findings
}
```

### Critical Behavioral Notes

| Behavior | Description |
|----------|-------------|
| **Case Sensitivity** | Project names are case-sensitive (exact match required) |
| **No Partial Match** | Only exact name matches work (no wildcards or fuzzy search) |
| **Null lastScanId** | May be null if project has no completed scans |
| **SDK findByName** | Uses `sdk.scan(orgId).projects().findByName()` |

---

## 1. Basic Functionality Tests

### Test Case 1.1: Retrieve Valid Project by Exact Name

**Objective:** Verify tool successfully retrieves SAST project using exact name.

**Prerequisites:**
- At least one SAST scan project exists in the organization
- Know the exact project name

**Test Steps:**
1. Call `get_scan_project(projectName="<valid-project-name>")`
2. Verify response structure

**Expected Results:**
- Response status: success
- `data` contains Project object
- `data.name()` matches requested project name
- `data.id()` is non-null UUID
- `found: true`

---

### Test Case 1.2: Project Structure Verification

**Objective:** Verify all Project fields are populated correctly.

**Test Steps:**
1. Call `get_scan_project` with valid project name
2. Examine all fields

**Expected Results:**
- `id()`: Non-null, valid UUID format
- `name()`: Matches requested project name exactly
- `language()`: Programming language (e.g., "JAVA", "JAVASCRIPT")
- `lastScanId()`: Valid scan ID if scans exist, null otherwise
- `lastScanTime()`: Timestamp if scans exist
- `completedScans()`: Integer >= 0
- Vulnerability counts: `critical()`, `high()`, `medium()`, `low()`, `note()`

---

### Test Case 1.3: Project with Multiple Scans

**Objective:** Verify tool returns project with complete scan history info.

**Prerequisites:**
- Project with at least 2-3 completed scans

**Test Steps:**
1. Call `get_scan_project` for project with multiple scans
2. Verify lastScanId and completedScans

**Expected Results:**
- `lastScanId()` contains most recent scan ID
- `completedScans()` reflects actual scan count
- `lastScanTime()` is recent timestamp

---

### Test Case 1.4: Newly Created Project (No Scans)

**Objective:** Verify tool handles project with no completed scans.

**Prerequisites:**
- Project that exists but has never been scanned

**Test Steps:**
1. Call `get_scan_project(projectName="<new-project>")`
2. Check lastScanId handling

**Expected Results:**
- Project returned successfully
- `id()` and `name()` populated
- `lastScanId()` is null
- `completedScans()` is 0
- No exception due to missing scan data

---

## 2. Project Name Matching Tests

### Test Case 2.1: Case Sensitivity - Exact Match

**Objective:** Verify project name matching is case-sensitive.

**Prerequisites:**
- Know exact casing of a project name (e.g., "MyProject")

**Test Steps:**
1. Call with exact match: `get_scan_project(projectName="MyProject")`
2. Call with lowercase: `get_scan_project(projectName="myproject")`
3. Call with uppercase: `get_scan_project(projectName="MYPROJECT")`

**Expected Results:**
- Exact match: Returns Project successfully
- Wrong case variations: Returns null/not found
- Demonstrates case-sensitive matching

---

### Test Case 2.2: Special Characters in Project Name

**Objective:** Verify tool handles project names with special characters.

**Test Steps:**
Test with project names containing (if they exist):
1. `Project-With-Dashes`
2. `Project_With_Underscores`
3. `Project.With.Dots`
4. `Project (With Parentheses)`

**Expected Results:**
- If project exists: Returns Project object
- Special characters handled correctly by SDK
- No encoding or escaping issues

---

### Test Case 2.3: Project Name with Leading/Trailing Whitespace

**Objective:** Verify whitespace handling in project names.

**Prerequisites:**
- Know project named "MyProject" (no whitespace)

**Test Steps:**
1. Call with leading space: `" MyProject"`
2. Call with trailing space: `"MyProject "`
3. Call with both: `" MyProject "`

**Expected Results:**
- Either all fail (not trimmed) or all succeed (trimmed)
- Document actual SDK behavior
- Consistent handling

---

### Test Case 2.4: Partial Name Match (Should Not Work)

**Objective:** Verify partial matches do not return results.

**Prerequisites:**
- Project named "MyLongProjectName" exists

**Test Steps:**
1. Call with prefix: `"MyLong"`
2. Call with suffix: `"ProjectName"`
3. Call with substring: `"LongProject"`

**Expected Results:**
- All return not found
- No partial matching or fuzzy search
- Exact name match required

---

## 3. Validation Tests

### Test Case 3.1: Missing projectName Parameter

**Objective:** Verify validation error for missing required parameter.

**Test Steps:**
1. Call `get_scan_project` without projectName (null or empty)

**Expected Results:**
- Validation error returned
- Error message: "projectName is required"
- No API call made

---

### Test Case 3.2: Non-Existent Project Name

**Objective:** Verify behavior when project doesn't exist.

**Test Steps:**
1. Call `get_scan_project(projectName="NonExistentProject123XYZ")`

**Expected Results:**
- Response indicates not found
- `data` is null
- `found: false` or appropriate error
- No exception

---

### Test Case 3.3: Empty String Project Name

**Objective:** Verify handling of empty string.

**Test Steps:**
1. Call `get_scan_project(projectName="")`

**Expected Results:**
- Validation error: "projectName is required"
- Empty string treated as missing
- No API call made

---

### Test Case 3.4: Whitespace-Only Project Name

**Objective:** Verify handling of whitespace-only input.

**Test Steps:**
1. Call `get_scan_project(projectName="   ")`
2. Call `get_scan_project(projectName="\t")`

**Expected Results:**
- Validation error or not found
- Whitespace-only treated as invalid
- Graceful handling

---

### Test Case 3.5: Very Long Project Name

**Objective:** Verify handling of extremely long project name.

**Test Steps:**
1. Call `get_scan_project(projectName="<500+ character string>")`

**Expected Results:**
- Returns not found (assuming no such project)
- No truncation or string handling errors
- Reasonable response time

---

## 4. Data Completeness Tests

### Test Case 4.1: Verify All Project Fields Populated

**Objective:** Verify complete project data returned.

**Prerequisites:**
- Project with completed scans and findings

**Test Steps:**
1. Call `get_scan_project` for active project
2. Check all fields

**Expected Results:**
- All fields have appropriate values
- Vulnerability counts match TeamServer UI
- Timestamps are in valid format

---

### Test Case 4.2: Vulnerability Counts Accuracy

**Objective:** Verify severity counts match actual findings.

**Test Steps:**
1. Call `get_scan_project`
2. Compare counts with TeamServer UI

**Expected Results:**
- `critical()`, `high()`, `medium()`, `low()`, `note()` match UI
- Counts sum to total findings
- No stale data

---

### Test Case 4.3: Project with No Scans - Field Handling

**Objective:** Verify field handling when no scans exist.

**Test Steps:**
1. Call `get_scan_project` for project with no scans
2. Check all fields

**Expected Results:**
- `lastScanId()` is null
- `lastScanTime()` is null
- `completedScans()` is 0
- Vulnerability counts may be 0 or null
- No errors due to missing data

---

## 5. Error Handling Tests

### Test Case 5.1: API Connection Failure

**Objective:** Verify graceful handling of connection issues.

**Prerequisites:**
- Simulate connection failure

**Test Steps:**
1. Configure invalid hostname
2. Call `get_scan_project`

**Expected Results:**
- Error response returned
- Clear error message
- MCP server remains stable

---

### Test Case 5.2: Authentication Failure

**Objective:** Verify handling of invalid credentials.

**Test Steps:**
1. Configure invalid API credentials
2. Call `get_scan_project`

**Expected Results:**
- Authentication error returned
- No credentials exposed in logs
- Clear error message

---

### Test Case 5.3: Insufficient Permissions

**Objective:** Verify behavior when user lacks SAST permissions.

**Prerequisites:**
- Account without SAST access

**Test Steps:**
1. Configure limited account
2. Call `get_scan_project`

**Expected Results:**
- Permission error returned
- Clear error message
- No partial data leakage

---

### Test Case 5.4: Concurrent Requests

**Objective:** Verify concurrent requests handled correctly.

**Test Steps:**
1. Call `get_scan_project` for 3 different projects concurrently
2. Verify each returns correct data

**Expected Results:**
- All requests complete successfully
- Each returns correct project (no mixing)
- No thread safety issues

---

## 6. Performance Tests

### Test Case 6.1: Response Time Baseline

**Objective:** Measure acceptable response time.

**Test Steps:**
1. Call `get_scan_project` 5 times
2. Measure response times
3. Calculate average

**Expected Results:**
- Normal case: < 3 seconds
- Consistent across calls

---

### Test Case 6.2: Project with Many Scans

**Objective:** Verify performance with extensive scan history.

**Prerequisites:**
- Project with 50+ completed scans

**Test Steps:**
1. Call `get_scan_project` for project with many scans
2. Measure response time

**Expected Results:**
- Response time reasonable (< 5 seconds)
- Large history doesn't cause timeout
- All data returned correctly

---

## 7. Integration Tests

### Test Case 7.1: Integration with get_scan_results

**Objective:** Verify project data can be used with scan results tool.

**Test Steps:**
1. Call `get_scan_project(projectName="<project>")`
2. Note lastScanId
3. Call `get_scan_results(projectName="<same-project>")`
4. Verify results are for correct project

**Expected Results:**
- Both tools work with same project name
- Data is consistent between tools
- lastScanId matches scan in results

---

### Test Case 7.2: Workflow: Find Project Then Get Results

**Objective:** Verify typical SAST workflow.

**Test Steps:**
1. Call `get_scan_project` to get project details
2. Check if `lastScanId` exists
3. If exists, call `get_scan_results` for full findings

**Expected Results:**
- Workflow completes without errors
- Project info helps decide if results available
- Consistent data

---

## 8. Edge Cases and Security Tests

### Test Case 8.1: SQL Injection Patterns

**Objective:** Verify SQL-like patterns handled safely.

**Test Steps:**
Test with project names:
1. `Project'; DROP TABLE projects;--`
2. `Project' OR '1'='1`

**Expected Results:**
- Patterns treated as literal strings
- Returns "not found" (not SQL error)
- No security vulnerabilities

---

### Test Case 8.2: Path Traversal Patterns

**Objective:** Verify path traversal patterns handled safely.

**Test Steps:**
Test with project names:
1. `../../../etc/passwd`
2. `Project/../../other`

**Expected Results:**
- Patterns treated as literal project names
- Returns "not found"
- No file system access

---

### Test Case 8.3: Unicode Characters

**Objective:** Verify Unicode handling in project names.

**Test Steps:**
Test with project names (if such projects exist):
1. `Проект` (Cyrillic)
2. `项目` (Chinese)
3. `Proyecto_ñ` (Spanish)

**Expected Results:**
- Unicode characters handled correctly
- Exact match works with proper encoding
- No encoding corruption

---

## Test Execution Guidelines

### Pre-Test Setup
1. Verify MCP server running with valid credentials
2. Ensure SAST functionality is enabled in organization
3. Have at least 3-5 SAST projects with different characteristics:
   - Project with multiple completed scans
   - Project with single scan
   - Project with no scans (newly created)

### Workflow for Finding Test Data
```
1. Log into Contrast TeamServer
2. Navigate to Scan section
3. Note project names (exact casing)
4. Record which projects have scans
5. Use those names for testing
```

### Success Criteria
The `get_scan_project` tool passes testing if:
- Valid project retrieval succeeds (TC 1.1-1.4)
- Case sensitivity is enforced (TC 2.1)
- Validation catches invalid input (TC 3.1-3.5)
- Project data is complete (TC 4.1-4.3)
- Error handling is graceful (TC 5.1-5.4)
- Performance is acceptable (< 5 seconds)

---

## Test Coverage Summary

| Category | Test Cases | Coverage |
|----------|------------|----------|
| Basic Functionality | 4 | Core behavior, structure |
| Project Name Matching | 4 | Case sensitivity, special chars |
| Validation | 5 | Required params, edge cases |
| Data Completeness | 3 | Fields, accuracy |
| Error Handling | 4 | Connection, auth, concurrent |
| Performance | 2 | Response time, large data |
| Integration | 2 | Workflow with other tools |
| Edge Cases/Security | 3 | Injection, traversal, Unicode |

**Total: 27 test cases**

---

## Appendix: Expected Behavior Examples

### Successful Response Example
```json
{
  "data": {
    "id": "proj-abc123-def456",
    "name": "MyWebApplication",
    "language": "JAVA",
    "lastScanId": "scan-789xyz",
    "lastScanTime": "2025-01-15T10:30:00Z",
    "completedScans": 15,
    "critical": 2,
    "high": 8,
    "medium": 24,
    "low": 45,
    "note": 12
  },
  "message": null,
  "found": true
}
```

### Project Not Found Response
```json
{
  "data": null,
  "message": null,
  "found": false
}
```

### Project with No Scans
```json
{
  "data": {
    "id": "proj-abc123-def456",
    "name": "NewProject",
    "language": "JAVA",
    "lastScanId": null,
    "lastScanTime": null,
    "completedScans": 0,
    "critical": 0,
    "high": 0,
    "medium": 0,
    "low": 0,
    "note": 0
  },
  "message": null,
  "found": true
}
```

### Validation Error Response
```json
{
  "data": null,
  "message": "Validation failed: projectName is required",
  "found": false
}
```

---

## Logging Verification

Check MCP server logs (`/tmp/mcp-contrast.log`) for:
- "Retrieving scan project details for project: {projectName}"
- "Successfully found project: {} (id: {}, language: {})"
- "Project not found: {projectName}" (for not found cases)
- No error logs in success case

---

## References

- **Tool Implementation**: `tool/sast/GetSastProjectTool.java`
- **Params Class**: `tool/sast/params/GetSastProjectParams.java`
- **Related Tools**: `get_scan_results`
- **Old Test Plan**: `test-plan-list_Scan_Project.md` (root level)
