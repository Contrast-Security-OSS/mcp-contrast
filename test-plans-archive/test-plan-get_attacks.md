# Test Plan: get_attacks Tool

## Overview
This test plan provides comprehensive coverage for the `get_attacks` tool in the ADRService class. This tool retrieves all attacks from Contrast ADR (Attack Detection and Response) with no filtering parameters, returning attack summaries with essential information.

**Tool Location**: `/Users/chrisedwards/projects/contrast/mcp-contrast/src/main/java/com/contrast/labs/ai/mcp/contrast/ADRService.java` (lines 137-169)

**Test Execution Context**: These tests should be executed by an AI agent using the MCP server against a live Contrast Security instance with appropriate test data.

---

## Test Categories

### 1. Basic Functionality Tests

#### Test 1.1: Retrieve All Attacks
**Description**: Call the tool with no parameters to retrieve all attacks in the organization.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Returns a list of AttackSummary objects
- Each AttackSummary contains all required fields
- Response time is logged
- Success message logged with count: "Successfully retrieved {N} attacks (took {X} ms)"
- No errors or exceptions occur

**Test Data Assumptions**:
- Assume at least one attack exists in the ADR system
- Attacks may be from various sources and time periods

---

#### Test 1.2: Verify Tool Registration
**Description**: Verify the tool is registered and accessible via MCP.

**Input**:
```
(Query available MCP tools)
```

**Expected Behavior**:
- Tool name: "get_attacks"
- Tool description mentions: "Attack Detection and Response", "attack summaries", "dates, rules, status, severity, applications, source IP, and probe count"
- Tool accepts no parameters
- Tool is callable from AI agents

**Test Data Assumptions**:
- None (MCP server running)

---

### 2. Data Completeness Tests

#### Test 2.1: AttackSummary Required Fields
**Description**: Verify all required fields are present in each AttackSummary.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
Each AttackSummary object contains:
- `attackId`: string (UUID)
- `status`: string (attack status)
- `source`: string (source IP address)
- `rules`: list of strings (attack rule names)
- `probes`: integer (number of probes)
- `startTime`: string (formatted date)
- `endTime`: string (formatted date)
- `startTimeMs`: long (epoch milliseconds)
- `endTimeMs`: long (epoch milliseconds)
- `firstEventTime`: string (formatted date)
- `lastEventTime`: string (formatted date)
- `firstEventTimeMs`: long (epoch milliseconds)
- `lastEventTimeMs`: long (epoch milliseconds)
- `applications`: list of ApplicationAttackInfo objects

All fields are non-null and properly typed.

**Test Data Assumptions**:
- Assume attacks exist with complete data

---

#### Test 2.2: ApplicationAttackInfo Fields
**Description**: Verify all application-specific attack information is present.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
Each ApplicationAttackInfo within an AttackSummary contains:
- `applicationId`: string
- `applicationName`: string
- `language`: string (e.g., "Java", "Node", ".NET")
- `severity`: string (attack severity for this application)
- `status`: string (attack status for this application)
- `startTime`: string (formatted date)
- `endTime`: string (formatted date)
- `startTimeMs`: long (epoch milliseconds)
- `endTimeMs`: long (epoch milliseconds)

All fields are non-null and properly typed.

**Test Data Assumptions**:
- Assume attacks exist that target specific applications

---

#### Test 2.3: Attack ID Uniqueness
**Description**: Verify each attack has a unique attackId.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- All attackId values are unique across all returned attacks
- attackId follows UUID format (e.g., "a1b2c3d4-e5f6-7890-abcd-ef1234567890")
- No duplicate attackId values in the response

**Test Data Assumptions**:
- Assume multiple attacks exist

---

#### Test 2.4: Source IP Format
**Description**: Verify source IP addresses are properly formatted.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- `source` field contains valid IP addresses
- Format examples: "192.168.1.100", "10.0.0.5", "203.0.113.42"
- May include IPv6 addresses (e.g., "2001:db8::1")
- No null or empty source values

**Test Data Assumptions**:
- Assume attacks exist with various source IP addresses

---

#### Test 2.5: Rules List Content
**Description**: Verify rules list contains attack rule identifiers.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- `rules` field is a list (array) of strings
- List may contain one or more rule names
- Rule names identify attack patterns (e.g., "sql-injection", "xss-attack", "cmd-injection")
- List is not null (may be empty if no rules matched)

**Test Data Assumptions**:
- Assume attacks exist that match various detection rules

---

#### Test 2.6: Probe Count Validity
**Description**: Verify probe count is a valid integer.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- `probes` field is an integer >= 0
- Represents the number of attack probes/attempts
- Higher probe counts indicate more persistent attacks
- Typical values range from 1 to thousands

**Test Data Assumptions**:
- Assume attacks exist with various probe counts

---

### 3. Attack Types Tests

#### Test 3.1: SQL Injection Attacks
**Description**: Verify attacks with SQL injection rules are returned.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Attacks with SQL injection rules are included in results
- Rules list includes SQL-related rule names (e.g., "sql-injection", "sqli")
- Attack data is complete and properly formatted

**Test Data Assumptions**:
- Assume SQL injection attacks exist in the ADR system
- May need to generate test attacks if none exist

---

#### Test 3.2: Cross-Site Scripting (XSS) Attacks
**Description**: Verify attacks with XSS rules are returned.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Attacks with XSS rules are included in results
- Rules list includes XSS-related rule names (e.g., "xss-reflected", "xss-stored", "xss")
- Attack data is complete and properly formatted

**Test Data Assumptions**:
- Assume XSS attacks exist in the ADR system

---

#### Test 3.3: Path Traversal Attacks
**Description**: Verify attacks with path traversal rules are returned.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Attacks with path traversal rules are included in results
- Rules list includes path traversal rule names (e.g., "path-traversal", "directory-traversal")
- Attack data is complete and properly formatted

**Test Data Assumptions**:
- Assume path traversal attacks exist in the ADR system

---

#### Test 3.4: Command Injection Attacks
**Description**: Verify attacks with command injection rules are returned.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Attacks with command injection rules are included in results
- Rules list includes command injection rule names (e.g., "cmd-injection", "command-injection")
- Attack data is complete and properly formatted

**Test Data Assumptions**:
- Assume command injection attacks exist in the ADR system

---

#### Test 3.5: Multiple Attack Rules
**Description**: Verify attacks matching multiple rules are handled correctly.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Some attacks may match multiple detection rules
- `rules` list contains all matched rule names
- List may have 2+ entries for sophisticated attacks
- All rule names are distinct (no duplicates)

**Test Data Assumptions**:
- Assume complex attacks exist that match multiple rules

---

#### Test 3.6: Attack Type Diversity
**Description**: Verify the response includes diverse attack types.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Results include attacks with various rule types
- May include: SQL injection, XSS, path traversal, command injection, XXE, SSRF, etc.
- Demonstrates comprehensive attack detection coverage
- No single attack type dominates the entire result set

**Test Data Assumptions**:
- Assume attacks exist from various attack categories

---

### 4. Attack Status Tests

#### Test 4.1: Unreviewed Attacks
**Description**: Verify unreviewed attacks are included in results.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Attacks with "unreviewed" status are included
- Status field accurately reflects unreviewed state
- These represent attacks that haven't been analyzed yet
- No filtering excludes unreviewed attacks

**Test Data Assumptions**:
- Assume unreviewed attacks exist in the ADR system

---

#### Test 4.2: Reviewed Attacks
**Description**: Verify reviewed attacks are included in results.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Attacks with "reviewed" status are included
- Status field accurately reflects reviewed state
- These represent attacks that have been analyzed
- No filtering excludes reviewed attacks

**Test Data Assumptions**:
- Assume reviewed attacks exist in the ADR system

---

#### Test 4.3: Suppressed Attacks
**Description**: Verify suppressed attacks are handled correctly.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Attacks with "suppressed" status are included (or verify if excluded)
- Status field accurately reflects suppressed state
- Behavior matches Contrast ADR default filtering
- Document whether suppressed attacks are returned

**Test Data Assumptions**:
- Assume suppressed attacks exist in the ADR system

---

#### Test 4.4: Active Attacks
**Description**: Verify active/ongoing attacks are included.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Active attacks are included in results
- Status may indicate "active" or similar
- `endTime` may equal `lastEventTime` for ongoing attacks
- Probe count may still be increasing

**Test Data Assumptions**:
- Assume active attacks may exist in the ADR system

---

#### Test 4.5: Status Field Values
**Description**: Document all possible status field values.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Status field contains one of the valid attack status values
- Common values may include: "unreviewed", "reviewed", "suppressed", "active"
- All status values are consistent across attacks
- Status values match Contrast ADR documentation

**Test Data Assumptions**:
- Assume attacks exist with various status values

---

### 5. Empty Results Tests

#### Test 5.1: Organization with No Attacks
**Description**: Test against an organization that has no attacks.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Returns empty list: `[]`
- Warning logged: "No attacks data returned (took {X} ms)"
- Response time is still logged
- No errors or exceptions occur
- Tool executes successfully with empty result

**Test Data Assumptions**:
- Use an organization with zero attacks
- Or test immediately after all attacks are suppressed/deleted

---

#### Test 5.2: Null Response from SDK
**Description**: Verify handling when SDK returns null.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- If SDK returns null, tool returns empty list: `[]`
- Warning logged: "No attacks data returned (took {X} ms)"
- No null pointer exceptions
- Graceful degradation

**Test Data Assumptions**:
- May require specific SDK configuration or mocking
- Simulates SDK error conditions

---

#### Test 5.3: Empty Attack List
**Description**: Verify handling when SDK returns empty list.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Returns empty list: `[]`
- Warning logged: "No attacks data returned (took {X} ms)"
- No errors or exceptions
- Distinguishable from null response

**Test Data Assumptions**:
- Use scenario where SDK successfully connects but finds no attacks

---

### 6. Volume Handling Tests

#### Test 6.1: Small Volume (1-10 Attacks)
**Description**: Test with a small number of attacks.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Returns all attacks (1-10 AttackSummary objects)
- Success message: "Successfully retrieved {1-10} attacks (took {X} ms)"
- Response time is reasonable (< 2000 ms)
- All data is complete and accurate

**Test Data Assumptions**:
- Assume 1-10 attacks exist in the ADR system

---

#### Test 6.2: Medium Volume (10-50 Attacks)
**Description**: Test with a moderate number of attacks.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Returns all attacks (10-50 AttackSummary objects)
- Success message: "Successfully retrieved {10-50} attacks (took {X} ms)"
- Response time is reasonable (< 5000 ms)
- All data is complete and accurate
- No performance degradation

**Test Data Assumptions**:
- Assume 10-50 attacks exist in the ADR system

---

#### Test 6.3: Large Volume (50-100 Attacks)
**Description**: Test with a large number of attacks.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Returns all attacks (50-100 AttackSummary objects)
- Success message: "Successfully retrieved {50-100} attacks (took {X} ms)"
- Response time is acceptable (< 10000 ms)
- All data is complete and accurate
- Memory usage is reasonable

**Test Data Assumptions**:
- Assume 50-100 attacks exist in the ADR system

---

#### Test 6.4: Very Large Volume (100+ Attacks)
**Description**: Test with a very large number of attacks.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Returns all attacks (100+ AttackSummary objects)
- Success message: "Successfully retrieved {100+} attacks (took {X} ms)"
- Response time is documented (may be > 10000 ms)
- All data is complete and accurate
- Note: Tool has no pagination, returns all attacks
- Consider whether pagination should be added in future

**Test Data Assumptions**:
- Assume 100+ attacks exist in the ADR system
- Document maximum observed attack count

---

#### Test 6.5: Attack Count Accuracy
**Description**: Verify the logged count matches actual returned items.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Log message count matches list size
- Example: "Successfully retrieved 42 attacks" → list has exactly 42 items
- Count is calculated after transformation to AttackSummary
- No attacks are lost during transformation

**Test Data Assumptions**:
- Assume any number of attacks exist

---

### 7. Performance Tests

#### Test 7.1: Response Time Logging
**Description**: Verify response time is logged correctly.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Start time captured before SDK initialization
- End time captured after processing complete
- Duration logged in milliseconds
- Log message format: "Successfully retrieved {N} attacks (took {X} ms)"
- Duration is reasonable for the data volume

**Test Data Assumptions**:
- Assume attacks exist in the system

---

#### Test 7.2: Error Response Time Logging
**Description**: Verify response time is logged even when errors occur.

**Input**:
```
(No parameters with SDK configured to fail)
```

**Expected Behavior**:
- Start time captured before SDK initialization
- End time captured in catch block
- Duration logged in error message
- Log message format: "Error retrieving attacks (after {X} ms): {error}"
- Performance data available for troubleshooting

**Test Data Assumptions**:
- May require invalid credentials or network issues to trigger error

---

#### Test 7.3: Performance with Large Datasets
**Description**: Document performance characteristics with large datasets.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Document response time for 10, 50, 100, 200+ attacks
- Performance degrades linearly (not exponentially)
- Identify any performance bottlenecks
- Transformation to AttackSummary is efficient
- No memory leaks or excessive garbage collection

**Test Data Assumptions**:
- Assume organizations with various attack counts
- Test across multiple volume ranges

---

#### Test 7.4: Cold Start Performance
**Description**: Test performance on first invocation after server start.

**Input**:
```
(No parameters, immediately after MCP server starts)
```

**Expected Behavior**:
- First invocation may be slower (SDK initialization)
- Duration is logged for comparison
- Subsequent invocations are faster
- Cold start overhead is acceptable (< 5 seconds additional)

**Test Data Assumptions**:
- Fresh MCP server instance
- Attacks exist in the system

---

### 8. Date Formatting Tests

#### Test 8.1: Start Time Formatting
**Description**: Verify startTime is formatted correctly.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- `startTime` is a formatted date string
- Format example: "Mon Oct 21 10:30:45 PDT 2025" (Java Date.toString() format)
- Human-readable and parseable
- Corresponds to `startTimeMs` epoch value
- Timezone is included

**Test Data Assumptions**:
- Assume attacks exist with various start times

---

#### Test 8.2: End Time Formatting
**Description**: Verify endTime is formatted correctly.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- `endTime` is a formatted date string
- Format matches `startTime` format
- Corresponds to `endTimeMs` epoch value
- endTime >= startTime (chronological order)
- Timezone is included

**Test Data Assumptions**:
- Assume attacks exist with various end times

---

#### Test 8.3: First Event Time Formatting
**Description**: Verify firstEventTime is formatted correctly.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- `firstEventTime` is a formatted date string
- Format matches other timestamp formats
- Corresponds to `firstEventTimeMs` epoch value
- firstEventTime <= lastEventTime (chronological order)
- Timezone is included

**Test Data Assumptions**:
- Assume attacks exist with event timing data

---

#### Test 8.4: Last Event Time Formatting
**Description**: Verify lastEventTime is formatted correctly.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- `lastEventTime` is a formatted date string
- Format matches other timestamp formats
- Corresponds to `lastEventTimeMs` epoch value
- lastEventTime >= firstEventTime (chronological order)
- For active attacks, may equal current time
- Timezone is included

**Test Data Assumptions**:
- Assume attacks exist with event timing data

---

#### Test 8.5: Epoch Milliseconds Accuracy
**Description**: Verify epoch milliseconds values are accurate.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- `startTimeMs`, `endTimeMs`, `firstEventTimeMs`, `lastEventTimeMs` are all positive longs
- Values represent milliseconds since Unix epoch (January 1, 1970)
- Values are in chronological order:
  - startTimeMs <= firstEventTimeMs <= lastEventTimeMs <= endTimeMs
- Formatted string dates correspond to epoch values
- Can be used for precise time calculations

**Test Data Assumptions**:
- Assume attacks exist with complete timing data

---

#### Test 8.6: Application Timestamp Formatting
**Description**: Verify application-specific timestamps are formatted correctly.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Within ApplicationAttackInfo, `startTime` and `endTime` are formatted strings
- Format matches attack-level timestamp formats
- Corresponds to `startTimeMs` and `endTimeMs` epoch values
- Application times may differ from attack-level times
- Timezone is included

**Test Data Assumptions**:
- Assume attacks exist that target applications

---

#### Test 8.7: Timezone Consistency
**Description**: Verify all timestamps use consistent timezone.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- All formatted timestamp strings include timezone information
- Timezone is consistent across all timestamps in a single response
- Timezone reflects server timezone or UTC
- Timezone abbreviation is included (e.g., "PDT", "EST", "UTC")
- Document which timezone is used

**Test Data Assumptions**:
- Assume attacks exist with various timestamps

---

#### Test 8.8: Date Formatting Edge Cases
**Description**: Test date formatting with edge case timestamps.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Very old attacks (years ago) format correctly
- Very recent attacks (seconds ago) format correctly
- Attacks spanning midnight format correctly
- Attacks spanning DST transitions format correctly
- No formatting errors or exceptions

**Test Data Assumptions**:
- Assume attacks exist with various timestamps including edge cases

---

### 9. Multiple Applications Tests

#### Test 9.1: Single Application Attack
**Description**: Verify attacks targeting a single application.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Attack has exactly one ApplicationAttackInfo in `applications` list
- Application information is complete (id, name, language, severity, status, timestamps)
- Attack summary reflects single-application attack
- No duplicate application entries

**Test Data Assumptions**:
- Assume attacks exist targeting individual applications

---

#### Test 9.2: Multi-Application Attack
**Description**: Verify attacks targeting multiple applications.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Attack has multiple ApplicationAttackInfo entries in `applications` list
- Each application has complete information
- Same attack affects multiple applications (shared attackId)
- Each application may have different severity or status
- No duplicate application entries for same application

**Test Data Assumptions**:
- Assume attacks exist that affect multiple applications
- Common in organization-wide attacks

---

#### Test 9.3: Application Diversity
**Description**: Verify attacks across different application types.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Applications have diverse languages (Java, Node, .NET, Python, Ruby, etc.)
- Application names are varied
- Each applicationId is unique
- Language field is populated for all applications
- Demonstrates ADR coverage across technology stack

**Test Data Assumptions**:
- Assume attacks exist targeting applications in different languages

---

#### Test 9.4: Application Severity Differences
**Description**: Verify same attack can have different severities per application.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- In multi-application attacks, severity may differ per application
- ApplicationAttackInfo.severity reflects application-specific severity
- Severity values may be: "LOW", "MEDIUM", "HIGH"
- Different severities reflect different application contexts/importance
- All severity values are valid

**Test Data Assumptions**:
- Assume multi-application attacks exist with varying severities

---

#### Test 9.5: Application Status Differences
**Description**: Verify same attack can have different statuses per application.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- In multi-application attacks, status may differ per application
- ApplicationAttackInfo.status reflects application-specific status
- One application may be "reviewed" while another is "unreviewed"
- Status differences reflect different application-level responses
- All status values are valid

**Test Data Assumptions**:
- Assume multi-application attacks exist with varying statuses

---

#### Test 9.6: Empty Applications List
**Description**: Verify handling of attacks with no application associations.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- If an attack has no associated applications, `applications` list is empty: `[]`
- No null values in applications field
- Attack data is still complete for other fields
- This may represent network-level attacks not tied to specific apps

**Test Data Assumptions**:
- Assume attacks may exist without application associations
- Or all attacks have at least one application

---

#### Test 9.7: Application Name and ID Consistency
**Description**: Verify application identification is consistent.

**Input**:
```
(No parameters)
```

**Expected Behavior**:
- Same applicationId always maps to same applicationName
- Same applicationName always maps to same applicationId
- No inconsistencies across different attacks
- Application data matches application registry in Contrast

**Test Data Assumptions**:
- Assume multiple attacks exist affecting same applications

---

### 10. Error Handling Tests

#### Test 10.1: SDK Initialization Failure
**Description**: Test behavior when SDK initialization fails.

**Input**:
```
(No parameters with invalid credentials)
```

**Expected Behavior**:
- Error logged: "Error retrieving attacks (after {X} ms): {error message}"
- Exception is thrown (IOException)
- Response time is still logged
- Error includes details about initialization failure
- Tool does not crash or hang

**Test Data Assumptions**:
- Use invalid apiKey, serviceKey, userName, or hostName

---

#### Test 10.2: Network Connectivity Issues
**Description**: Test behavior with network problems.

**Input**:
```
(No parameters with network disconnected or host unreachable)
```

**Expected Behavior**:
- Error logged: "Error retrieving attacks (after {X} ms): {error message}"
- Exception includes network-related error details
- Response time is logged (may show timeout duration)
- Tool handles timeout gracefully
- No infinite hangs

**Test Data Assumptions**:
- Simulate network issues or use unreachable host

---

#### Test 10.3: Unauthorized Access
**Description**: Test behavior with insufficient permissions.

**Input**:
```
(No parameters with credentials lacking ADR access)
```

**Expected Behavior**:
- Error logged: "Error retrieving attacks (after {X} ms): {error message}"
- Exception includes authorization error details
- Response time is logged
- Error message indicates permission issue
- Tool does not expose sensitive credential information

**Test Data Assumptions**:
- Use credentials without ADR access permissions

---

#### Test 10.4: SDK Extension Failure
**Description**: Test behavior when SDKExtension fails.

**Input**:
```
(No parameters with SDK working but extension failing)
```

**Expected Behavior**:
- Error logged: "Error retrieving attacks (after {X} ms): {error message}"
- Exception is thrown and logged
- Response time is logged
- Error details help troubleshoot issue
- Tool logs both SDK initialization success and extension failure

**Test Data Assumptions**:
- May require specific test conditions or mocking

---

#### Test 10.5: Malformed Attack Data
**Description**: Test behavior when SDK returns malformed attack data.

**Input**:
```
(No parameters with SDK returning incomplete/malformed data)
```

**Expected Behavior**:
- Tool attempts to process data
- Logs any transformation errors
- May skip malformed attacks or throw exception
- Error provides details about data issues
- Does not crash with null pointer exceptions

**Test Data Assumptions**:
- May require specific data conditions or SDK behavior

---

## Test Execution Guidelines

### Pre-Test Setup
1. Verify MCP server is running and connected to a valid Contrast Security instance
2. Ensure test environment has:
   - ADR enabled and configured
   - Multiple attacks from various sources
   - Attacks with different statuses (unreviewed, reviewed, suppressed)
   - Attacks matching different rules (SQL injection, XSS, path traversal, etc.)
   - Attacks with various probe counts (single probe to hundreds/thousands)
   - Attacks spanning different time periods
   - Attacks affecting single and multiple applications
   - Applications in different languages (Java, Node, .NET, etc.)
3. Configure logging to capture debug and info messages:
   - Log file location: `/tmp/mcp-contrast.log`
   - Use `--logging.level.root=DEBUG` for detailed logging
4. Document baseline attack count before testing

### During Testing
1. Invoke the tool via MCP interface
2. Capture complete response (all AttackSummary objects)
3. Verify response structure and data completeness
4. Check log files for:
   - Info message: "Retrieving attacks from Contrast ADR"
   - Debug message: "ContrastSDK initialized successfully for attacks retrieval"
   - Debug message: "SDKExtension initialized successfully for attacks retrieval"
   - Success message: "Successfully retrieved {N} attacks (took {X} ms)"
   - Or warning: "No attacks data returned (took {X} ms)"
   - Or error: "Error retrieving attacks (after {X} ms): {error}"
5. Record response times for performance analysis
6. Note any unexpected behavior or edge cases

### Test Data Recommendations
For comprehensive testing, the test environment should ideally have:
- At least 50-100 attacks for volume testing
- Attacks from at least 10 different source IPs
- Attacks matching 5+ different rule types:
  - SQL injection
  - Cross-site scripting (XSS)
  - Path traversal
  - Command injection
  - Other OWASP attack types
- Attacks in various statuses:
  - Unreviewed (at least 20)
  - Reviewed (at least 20)
  - Suppressed (at least 5)
  - Active/ongoing (if possible)
- Attacks with probe counts ranging from 1 to 1000+
- Attacks spanning at least 30 days (for timestamp diversity)
- Attacks affecting at least 5 different applications
- Multi-application attacks (same attack affecting 2+ apps)
- Applications in at least 3 different languages
- Mix of single-app and multi-app attacks

### Success Criteria
Each test passes when:
1. Response structure matches AttackSummary specification
2. All required fields are present and non-null
3. Data types are correct (strings, integers, longs, lists)
4. Timestamps are formatted correctly and chronologically ordered
5. Application information is complete and accurate
6. Response time is logged correctly
7. No unexpected exceptions or errors occur
8. Empty results are handled gracefully
9. Large volumes are handled without performance issues
10. Error conditions are logged with appropriate details

### Performance Benchmarks
Expected response times (approximate):
- 1-10 attacks: < 1 second
- 10-50 attacks: < 3 seconds
- 50-100 attacks: < 5 seconds
- 100+ attacks: < 10 seconds

Note: Actual response times depend on network latency, Contrast TeamServer load, and attack data complexity.

### Known Limitations and Notes
1. **No Pagination**: Tool returns ALL attacks with no pagination. For organizations with 500+ attacks, consider using the companion `get_attacks_filtered` tool which may support pagination.
2. **No Filtering**: Tool provides no filtering options. All attacks are returned regardless of status, severity, time period, or application. Use `get_attacks_filtered` for filtering capabilities.
3. **Memory Considerations**: Very large attack counts (1000+) may impact memory usage. Monitor heap usage during volume tests.
4. **Timestamp Format**: Uses Java Date.toString() format which includes timezone but is not ISO 8601. Epoch milliseconds provided for precise calculations.
5. **SDK Dependency**: Tool behavior depends on Contrast SDK and network connectivity to TeamServer.

---

## Appendix A: Example Response Structure

### Example AttackSummary Object
```json
{
  "attackId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "status": "reviewed",
  "source": "192.168.1.100",
  "rules": ["sql-injection", "suspicious-input"],
  "probes": 42,
  "startTime": "Mon Oct 21 10:30:45 PDT 2025",
  "endTime": "Mon Oct 21 10:35:12 PDT 2025",
  "startTimeMs": 1729531845000,
  "endTimeMs": 1729532112000,
  "firstEventTime": "Mon Oct 21 10:30:45 PDT 2025",
  "lastEventTime": "Mon Oct 21 10:35:12 PDT 2025",
  "firstEventTimeMs": 1729531845000,
  "lastEventTimeMs": 1729532112000,
  "applications": [
    {
      "applicationId": "app-123-456",
      "applicationName": "Payment Service",
      "language": "Java",
      "severity": "HIGH",
      "status": "reviewed",
      "startTime": "Mon Oct 21 10:30:45 PDT 2025",
      "endTime": "Mon Oct 21 10:35:12 PDT 2025",
      "startTimeMs": 1729531845000,
      "endTimeMs": 1729532112000
    }
  ]
}
```

### Example Response with Multiple Attacks
```json
[
  {
    "attackId": "attack-1-uuid",
    "status": "unreviewed",
    "source": "203.0.113.42",
    "rules": ["xss-reflected"],
    "probes": 5,
    "startTime": "...",
    "endTime": "...",
    "startTimeMs": 1729531845000,
    "endTimeMs": 1729532112000,
    "firstEventTime": "...",
    "lastEventTime": "...",
    "firstEventTimeMs": 1729531845000,
    "lastEventTimeMs": 1729532112000,
    "applications": [...]
  },
  {
    "attackId": "attack-2-uuid",
    "status": "reviewed",
    "source": "10.0.0.5",
    "rules": ["sql-injection"],
    "probes": 156,
    "startTime": "...",
    "endTime": "...",
    "startTimeMs": 1729431845000,
    "endTimeMs": 1729432112000,
    "firstEventTime": "...",
    "lastEventTime": "...",
    "firstEventTimeMs": 1729431845000,
    "lastEventTimeMs": 1729432112000,
    "applications": [...]
  }
]
```

### Example Empty Response
```json
[]
```

---

## Appendix B: Log Message Reference

### Success Messages
```
INFO: Retrieving attacks from Contrast ADR
DEBUG: ContrastSDK initialized successfully for attacks retrieval
DEBUG: SDKExtension initialized successfully for attacks retrieval
INFO: Successfully retrieved 42 attacks (took 1234 ms)
```

### Warning Messages
```
WARN: No attacks data returned (took 1234 ms)
```

### Error Messages
```
ERROR: Error retrieving attacks (after 1234 ms): Connection timeout
ERROR: Error retrieving attacks (after 567 ms): Unauthorized - insufficient permissions
ERROR: Error retrieving attacks (after 89 ms): SDK initialization failed
```

---

## Test Coverage Summary

This test plan covers:
- ✓ 2 basic functionality test cases
- ✓ 6 data completeness test cases
- ✓ 6 attack types test cases
- ✓ 5 attack status test cases
- ✓ 3 empty results test cases
- ✓ 5 volume handling test cases
- ✓ 4 performance test cases
- ✓ 8 date formatting test cases
- ✓ 7 multiple applications test cases
- ✓ 5 error handling test cases

**Total: 51 test cases**

Each test case is designed to be executed by an AI agent using the MCP server, with clear input parameters (none for this tool), expected behaviors, and test data assumptions.

---

## Appendix C: Comparison with get_attacks_filtered Tool

The `get_attacks` tool is a simplified version that returns all attacks with no filtering. For more advanced use cases, consider the companion tool:

**get_attacks_filtered** (lines 171+):
- Supports filtering by status, severity, applications, tags, and other criteria
- May support pagination for large datasets
- Allows more targeted queries
- Use when specific attack subsets are needed

**When to use get_attacks:**
- Quick overview of all attacks
- Initial reconnaissance
- Low attack volumes (< 100 attacks)
- When no filtering is needed

**When to use get_attacks_filtered:**
- Large attack volumes requiring pagination
- Specific attack status or severity needed
- Filtering to specific applications
- Advanced queries with multiple criteria
