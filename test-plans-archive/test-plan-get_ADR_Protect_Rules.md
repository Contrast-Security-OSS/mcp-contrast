# Test Plan: get_ADR_Protect_Rules Tool

> **NOTE (AIML-189)**: As of the consolidation in AIML-189, the duplicate `get_ADR_Protect_Rules` (app_name variant) tool has been removed. The remaining tool has been renamed from `get_ADR_Protect_Rules_by_app_id` to `get_ADR_Protect_Rules` and now exclusively uses application ID as input. Users should call `list_applications_with_name` first to get the application ID from a name.

## Overview
This test plan provides comprehensive coverage for the `get_ADR_Protect_Rules` tool (formerly `get_ADR_Protect_Rules_by_app_id`) in the ADRService class. This tool retrieves ADR/Protect security rules configured for a specific application using the application ID.

**Tool Location**: `/Users/chrisedwards/projects/contrast/mcp-contrast/src/main/java/com/contrast/labs/ai/mcp/contrast/ADRService.java` (lines 96-135)

**Tool Signature**: `public ProtectData getProtectDataByAppID(String appID) throws IOException`

**Test Execution Context**: These tests should be executed by an AI agent using the MCP server against a live Contrast Security instance with appropriate test data.

---

## Tool Description

The `get_ADR_Protect_Rules_by_app_id` tool:
- Takes an application ID (not application name) as input
- Returns a `ProtectData` object containing ADR/Protect rules for the application
- Validates that the application ID is not null or empty
- Logs performance metrics (execution time)
- Logs the number of rules retrieved
- Handles errors gracefully with descriptive error messages

---

## Data Model Reference

### ProtectData Structure
```java
{
  success: boolean,          // Indicates if the request was successful
  messages: List<String>,    // Any messages from the API
  rules: List<Rule>          // List of protect rules for the application
}
```

### Rule Structure
Each rule in the `rules` array contains:
```java
{
  name: String,                    // Rule name
  type: String,                    // Rule type
  description: String,             // Rule description
  development: String,             // Development environment mode
  qa: String,                      // QA environment mode
  production: String,              // Production environment mode
  id: int,                         // Numeric rule ID
  uuid: String,                    // Rule UUID
  can_block_at_perimeter: Boolean, // Can block at perimeter
  is_monitor_at_perimeter: Boolean,// Monitor at perimeter
  can_block: Boolean,              // Can block attacks
  parent_rule_uuid: String,        // Parent rule UUID (if any)
  parent_rule_name: String,        // Parent rule name (if any)
  cves: List<Cve>,                 // Related CVEs
  enabled_dev: Boolean,            // Enabled in development
  enabled_qa: Boolean,             // Enabled in QA
  enabled_prod: Boolean            // Enabled in production
}
```

---

## Test Categories

### 1. Basic Functionality Tests

#### Test 1.1: Valid Application ID with Active Protect Rules
**Description**: Retrieve protect rules for an application with Protect/ADR enabled and configured.

**Input**:
```
appID: "<valid-app-id-with-protect>"
```

**Expected Behavior**:
- Returns a `ProtectData` object
- `success: true`
- `rules` array contains one or more Rule objects
- Each rule has all required fields populated
- Log message: "Starting retrieval of protection rules for application ID: {appID}"
- Log message: "Successfully retrieved {N} protection rules for application ID: {appID} (took {ms} ms)"
- No errors or exceptions

**Verification Points**:
1. ProtectData is not null
2. ProtectData.isSuccess() returns true
3. ProtectData.getRules() is not null
4. ProtectData.getRules().size() > 0
5. Each rule has valid name, type, uuid
6. Timing information is logged

**Test Data Assumptions**:
- Assume a valid application exists with UUID/ID format
- Assume the application has Protect/ADR enabled
- Assume at least one protect rule is configured for the application

---

#### Test 1.2: Retrieve Complete Rule Details
**Description**: Verify all rule fields are populated correctly.

**Input**:
```
appID: "<valid-app-id-with-protect>"
```

**Expected Behavior**:
- Returns ProtectData with rules
- Each rule contains:
  - Non-empty `name` string
  - Non-empty `type` string
  - `description` (may be empty but not null)
  - Environment modes: `development`, `qa`, `production` (strings like "MONITOR", "BLOCK", "OFF")
  - Numeric `id` > 0
  - Non-empty `uuid` string
  - Boolean flags: `can_block_at_perimeter`, `is_monitor_at_perimeter`, `can_block`
  - `enabled_dev`, `enabled_qa`, `enabled_prod` booleans
  - `parent_rule_uuid` and `parent_rule_name` (may be null for top-level rules)
  - `cves` list (may be empty but not null)

**Verification Points**:
1. Inspect first rule in rules array
2. Verify all required fields are present
3. Verify data types match expected types
4. Verify environment-specific settings are present

**Test Data Assumptions**:
- Application has at least one configured protect rule
- Rules have various configurations (some enabled, some disabled, different modes)

---

#### Test 1.3: Multiple Rules Retrieval
**Description**: Test with an application that has multiple protect rules configured.

**Input**:
```
appID: "<app-id-with-multiple-rules>"
```

**Expected Behavior**:
- Returns ProtectData with multiple rules
- `rules` array size > 1
- Log message shows correct count: "Successfully retrieved {N} protection rules"
- Each rule is distinct (different uuid)
- Rules may include various types (e.g., SQL Injection, XSS, Command Injection)

**Verification Points**:
1. ProtectData.getRules().size() > 1
2. All rules have unique UUIDs
3. Rules have different names/types
4. Rule count in logs matches actual count

**Test Data Assumptions**:
- Application has multiple protect rules configured (ideally 5-10 rules)
- Rules cover different attack types

---

### 2. Input Validation Tests

#### Test 2.1: Null Application ID
**Description**: Test behavior when null application ID is provided.

**Input**:
```
appID: null
```

**Expected Behavior**:
- VALIDATION FAILURE: Throws IllegalArgumentException
- Error message: "Application ID cannot be null or empty"
- Log message: "Cannot retrieve protection rules - application ID is null or empty"
- No API call is made to Contrast SDK
- Execution stops immediately

**Verification Points**:
1. Exception is thrown
2. Exception is IllegalArgumentException
3. Error message is descriptive
4. No partial data is returned

**Test Data Assumptions**:
- N/A (validation test)

---

#### Test 2.2: Empty Application ID
**Description**: Test behavior when empty string application ID is provided.

**Input**:
```
Test cases:
- appID: ""
- appID: "   " (whitespace only)
```

**Expected Behavior**:
- VALIDATION FAILURE: Throws IllegalArgumentException
- Error message: "Application ID cannot be null or empty"
- Log message: "Cannot retrieve protection rules - application ID is null or empty"
- No API call is made to Contrast SDK
- Execution stops immediately

**Verification Points**:
1. Exception is thrown for both empty string and whitespace
2. Exception is IllegalArgumentException
3. Error message is descriptive
4. Validation happens before SDK initialization

**Test Data Assumptions**:
- N/A (validation test)

---

### 3. Empty and Null Results Tests

#### Test 3.1: Application with Protect Disabled
**Description**: Test with a valid application that has Protect/ADR disabled or not configured.

**Input**:
```
appID: "<valid-app-id-no-protect>"
```

**Expected Behavior**:
- Returns null or ProtectData with empty rules list
- Log message: "No protection data returned for application ID: {appID} (took {ms} ms)"
- Log message: "Successfully retrieved 0 protection rules" (if empty list)
- No error thrown
- Performance timing still logged

**Verification Points**:
1. Method returns null OR returns ProtectData with empty rules
2. No exception is thrown
3. Warning log message is present
4. Timing is still logged

**Test Data Assumptions**:
- Valid application exists but has Protect/ADR disabled
- Or application has never had Protect configured

---

#### Test 3.2: Null Response from SDK
**Description**: Test when the underlying SDK returns null for protect configuration.

**Input**:
```
appID: "<app-id-returning-null>"
```

**Expected Behavior**:
- Method returns null
- Log message: "No protection data returned for application ID: {appID} (took {ms} ms)"
- No exception thrown
- Graceful handling of null response

**Verification Points**:
1. Method returns null (not exception)
2. Warning log message is present
3. Timing information is logged
4. No downstream null pointer exceptions

**Test Data Assumptions**:
- Application exists but SDK returns null (may occur with certain permission/configuration scenarios)

---

### 4. Error Handling Tests

#### Test 4.1: Invalid Application ID Format
**Description**: Test with malformed application ID (not a valid UUID format).

**Input**:
```
Test cases:
- appID: "invalid-app-id"
- appID: "12345"
- appID: "not-a-uuid"
- appID: "00000000"
```

**Expected Behavior**:
- MAY throw IOException or return null/empty results depending on SDK behavior
- Error logged: "Error retrieving protection rules for application ID: {appID} (after {ms} ms): {error}"
- If IOException thrown, it includes descriptive message
- Performance timing logged even on error

**Verification Points**:
1. Error is handled gracefully (no uncaught exceptions)
2. Error message includes application ID
3. Error message includes timing information
4. Full stack trace logged for debugging

**Test Data Assumptions**:
- Use deliberately malformed application IDs
- Understand that SDK may handle format validation differently

---

#### Test 4.2: Non-Existent Application ID
**Description**: Test with a valid UUID format but non-existent application.

**Input**:
```
Test cases:
- appID: "00000000-0000-0000-0000-000000000000"
- appID: "ffffffff-ffff-ffff-ffff-ffffffffffff"
- appID: "12345678-1234-1234-1234-123456789abc" (valid format, doesn't exist)
```

**Expected Behavior**:
- MAY throw IOException or return null depending on SDK behavior
- Error logged: "Error retrieving protection rules for application ID: {appID} (after {ms} ms): {error}"
- If no error, returns null or empty ProtectData
- Warning logged if null returned

**Verification Points**:
1. No uncaught exceptions
2. Error or warning logged appropriately
3. Method completes (doesn't hang)
4. Timing information logged

**Test Data Assumptions**:
- Use UUIDs that definitely don't exist in the organization
- May receive 404 or similar error from SDK

---

#### Test 4.3: Network/API Failure
**Description**: Test behavior when API call fails (simulated or real network issue).

**Input**:
```
appID: "<valid-app-id>"
(Trigger network failure scenario if possible)
```

**Expected Behavior**:
- Throws IOException with descriptive message
- Error logged: "Error retrieving protection rules for application ID: {appID} (after {ms} ms): {error}"
- Stack trace logged for debugging
- Timing information includes time until failure
- Error message is propagated to caller

**Verification Points**:
1. IOException is thrown
2. Error message is descriptive
3. Stack trace logged for debugging
4. Timing logged even on failure
5. No resource leaks (connections closed)

**Test Data Assumptions**:
- May require special test setup to simulate network failures
- Or test with temporarily unreachable Contrast instance

---

#### Test 4.4: Authentication/Authorization Failure
**Description**: Test with invalid credentials or insufficient permissions.

**Input**:
```
appID: "<valid-app-id>"
(With invalid/expired API keys or insufficient permissions)
```

**Expected Behavior**:
- Throws IOException or authentication-related exception
- Error logged: "Error retrieving protection rules for application ID: {appID} (after {ms} ms): {error}"
- Error message indicates authentication/authorization issue
- No partial data returned

**Verification Points**:
1. Exception is thrown
2. Error message indicates auth problem
3. Error logged with full details
4. Timing information present

**Test Data Assumptions**:
- Requires test scenario with invalid credentials
- Or application that current user doesn't have access to

---

### 5. Performance and Logging Tests

#### Test 5.1: Performance Timing Verification
**Description**: Verify that execution time is correctly logged for successful requests.

**Input**:
```
appID: "<valid-app-id-with-protect>"
```

**Expected Behavior**:
- Start time logged: "Starting retrieval of protection rules for application ID: {appID}"
- End time logged: "Successfully retrieved {N} protection rules for application ID: {appID} (took {ms} ms)"
- Duration value is reasonable (typically < 5000ms)
- Duration matches actual execution time

**Verification Points**:
1. Both start and end messages logged
2. Duration is a positive number
3. Duration is in milliseconds
4. Duration is reasonable (not 0, not excessively large)

**Test Data Assumptions**:
- Normal network conditions
- Responsive Contrast instance

---

#### Test 5.2: Performance Timing on Failure
**Description**: Verify that timing is logged even when requests fail.

**Input**:
```
appID: "<non-existent-app-id>"
```

**Expected Behavior**:
- Start time logged
- Error logged with timing: "Error retrieving protection rules for application ID: {appID} (after {ms} ms): {error}"
- Duration represents time until failure occurred
- Duration is a positive number

**Verification Points**:
1. Start message logged
2. Error message includes duration
3. Duration is positive
4. Timing helps diagnose slow failures

**Test Data Assumptions**:
- Application ID that will cause an error

---

#### Test 5.3: Rule Count Logging Accuracy
**Description**: Verify that the logged rule count matches actual returned rules.

**Input**:
```
appID: "<valid-app-id-with-known-rule-count>"
```

**Expected Behavior**:
- Log message: "Successfully retrieved {N} protection rules for application ID: {appID}"
- Logged count matches ProtectData.getRules().size()
- If 0 rules: "Successfully retrieved 0 protection rules"
- Count is accurate (not hardcoded or estimated)

**Verification Points**:
1. Rule count in log message
2. Rule count matches actual size of rules array
3. Count is correct even for edge cases (0, 1, many rules)

**Test Data Assumptions**:
- Applications with known/verified rule counts
- Test with 0, 1, and multiple rules scenarios

---

#### Test 5.4: Debug Logging Verification
**Description**: Verify that debug-level logs are present for troubleshooting.

**Input**:
```
appID: "<valid-app-id-with-protect>"
(With DEBUG log level enabled)
```

**Expected Behavior**:
- Debug log: "ContrastSDK initialized successfully for application ID: {appID}"
- Debug log: "SDKExtension initialized successfully for application ID: {appID}"
- Debug log: "Retrieving protection configuration for application ID: {appID}"
- Debug logs provide visibility into each step

**Verification Points**:
1. All debug messages present when log level is DEBUG
2. Messages provide useful troubleshooting information
3. Application ID included in each message for context
4. Debug logs don't include sensitive data

**Test Data Assumptions**:
- Log level set to DEBUG
- Access to log output

---

### 6. Comparison and Consistency Tests

#### Test 6.1: Consistency with get_ADR_Protect_Rules (Name-Based)
**Description**: Compare results from `get_ADR_Protect_Rules_by_app_id` with `get_ADR_Protect_Rules` for the same application.

**Input**:
```
Step 1: Get application ID for app name
Step 2: Call get_ADR_Protect_Rules_by_app_id with app ID
Step 3: Call get_ADR_Protect_Rules with app name
```

**Expected Behavior**:
- Both methods return same ProtectData structure
- Rule counts match
- Rule UUIDs match
- Rule configurations match (modes, enabled flags)
- Success flags match
- Messages match

**Verification Points**:
1. ProtectData.success same for both
2. Rule count identical
3. Rules in same order (or can be matched by UUID)
4. Each rule's fields match between both calls
5. No data discrepancies

**Test Data Assumptions**:
- Know both application name and ID
- Application has Protect configured
- Both methods use same underlying SDK

---

#### Test 6.2: Application Name to ID Resolution Verification
**Description**: Verify that `get_ADR_Protect_Rules` correctly resolves app name to ID and calls the same underlying method.

**Input**:
```
appName: "<valid-app-name>"
```

**Expected Behavior**:
- `get_ADR_Protect_Rules` logs: "Looking up application ID for name: {appName}"
- `get_ADR_Protect_Rules` logs: "Found application ID: {appID} for application: {appName}"
- Calls `getProtectDataByAppID(appID)` internally
- Results are identical to calling `get_ADR_Protect_Rules_by_app_id` directly with that ID

**Verification Points**:
1. Application name resolution logged
2. Application ID found and logged
3. Underlying method receives correct app ID
4. Results are consistent

**Test Data Assumptions**:
- Valid application name that exists in the organization
- Application has a single, unambiguous match

---

#### Test 6.3: Direct ID vs Name-Based Performance
**Description**: Compare performance of direct ID lookup vs name-based lookup.

**Input**:
```
Scenario 1: get_ADR_Protect_Rules_by_app_id("<app-id>")
Scenario 2: get_ADR_Protect_Rules("<app-name>")
```

**Expected Behavior**:
- Direct ID lookup (Scenario 1) is faster
- Name-based lookup includes extra time for app name resolution
- Both complete successfully
- Time difference logged shows name resolution overhead
- Typically: direct ID is 100-500ms faster (varies by network)

**Verification Points**:
1. Both complete successfully
2. Timings logged for both
3. Direct ID approach is faster
4. Time difference is reasonable (name lookup overhead)

**Test Data Assumptions**:
- Same application tested in both scenarios
- Normal network conditions
- Multiple test runs for average

---

### 7. Data Integrity Tests

#### Test 7.1: Rule Environment Configuration Validation
**Description**: Verify that environment-specific settings are correctly populated.

**Input**:
```
appID: "<app-id-with-environment-configs>"
```

**Expected Behavior**:
- Each rule has `development`, `qa`, `production` fields
- Values are strings like "MONITOR", "BLOCK", "OFF", "DISABLED"
- Corresponding boolean flags: `enabled_dev`, `enabled_qa`, `enabled_prod`
- Boolean flags consistent with mode strings (e.g., OFF = false, MONITOR/BLOCK = true)

**Verification Points**:
1. Environment fields present and non-null
2. Valid values for modes
3. Boolean flags consistent with modes
4. At least one environment typically configured

**Test Data Assumptions**:
- Application has environment-specific rule configurations
- Different rules may have different environment settings

---

#### Test 7.2: Rule Hierarchy Validation
**Description**: Verify parent-child rule relationships are correctly represented.

**Input**:
```
appID: "<app-id-with-rule-hierarchy>"
```

**Expected Behavior**:
- Some rules may have `parent_rule_uuid` and `parent_rule_name` populated
- Top-level rules have null parent fields
- Child rules reference valid parent UUIDs
- Parent rules should also appear in the rules list

**Verification Points**:
1. Rules with parents have valid parent_rule_uuid
2. Parent rules exist in the rules list (can be found by UUID)
3. Top-level rules have null parent fields
4. Rule hierarchy makes logical sense

**Test Data Assumptions**:
- Application has rules with parent-child relationships
- Or all rules are top-level (both scenarios valid)

---

#### Test 7.3: CVE Information Validation
**Description**: Verify that CVE data is correctly included for rules that address known vulnerabilities.

**Input**:
```
appID: "<app-id-with-cve-rules>"
```

**Expected Behavior**:
- Some rules have `cves` list populated
- CVE list contains Cve objects with valid CVE identifiers
- Rules not related to specific CVEs have empty cves list
- CVE data provides additional vulnerability context

**Verification Points**:
1. Rules.getCves() returns list (not null)
2. List may be empty or contain CVE objects
3. CVE objects have valid identifiers (e.g., "CVE-2023-12345")
4. CVE data is structured correctly

**Test Data Assumptions**:
- Some rules may address specific CVEs
- Not all rules will have CVE associations (both scenarios valid)

---

#### Test 7.4: Blocking Capability Flags Validation
**Description**: Verify that blocking capability flags are correctly set.

**Input**:
```
appID: "<app-id-with-various-rules>"
```

**Expected Behavior**:
- Each rule has boolean flags: `can_block`, `can_block_at_perimeter`, `is_monitor_at_perimeter`
- Flags indicate rule capabilities
- Some rules can block, others monitor only
- Flags are consistent with rule type and configuration

**Verification Points**:
1. All blocking flags are present (not null)
2. Flags are boolean values
3. At least some variation in flags across rules
4. Flags make logical sense for rule types

**Test Data Assumptions**:
- Application has mix of blocking and monitoring rules
- Different rules have different capabilities

---

### 8. Edge Cases and Special Scenarios

#### Test 8.1: New Application (Just Created)
**Description**: Test with a newly created application that may not have Protect configured yet.

**Input**:
```
appID: "<newly-created-app-id>"
```

**Expected Behavior**:
- Returns null or ProtectData with empty rules
- Log message: "No protection data returned for application ID: {appID}"
- No error thrown
- Timing logged

**Verification Points**:
1. No exception thrown
2. Returns null or empty ProtectData gracefully
3. Warning logged appropriately

**Test Data Assumptions**:
- Recently created application
- No Protect configuration yet applied

---

#### Test 8.2: Application with Default Protect Configuration
**Description**: Test with application using default/baseline Protect rules.

**Input**:
```
appID: "<app-id-with-defaults>"
```

**Expected Behavior**:
- Returns ProtectData with default rule set
- Rule count matches default configuration (varies by Contrast version)
- Most/all rules in MONITOR mode by default
- All rules have enabled flags set appropriately

**Verification Points**:
1. ProtectData returned successfully
2. Rule count > 0
3. Rules have default configurations
4. Environment modes typically MONITOR for defaults

**Test Data Assumptions**:
- Application with default Protect configuration
- No custom rule modifications

---

#### Test 8.3: Application with All Rules Disabled
**Description**: Test with application where all Protect rules are disabled.

**Input**:
```
appID: "<app-id-all-disabled>"
```

**Expected Behavior**:
- Returns ProtectData with rules list
- Rules present but all have enabled_* flags set to false
- Or environment modes set to "OFF" or "DISABLED"
- Success is still true (configuration retrieved successfully)

**Verification Points**:
1. ProtectData.success is true
2. Rules list is present
3. All rules have disabled flags or OFF modes
4. Represents valid configuration state

**Test Data Assumptions**:
- Application where administrator has disabled all rules
- Configuration exists but all rules turned off

---

#### Test 8.4: Large Number of Rules
**Description**: Test performance and handling with application having many protect rules.

**Input**:
```
appID: "<app-id-with-many-rules>"
```

**Expected Behavior**:
- Successfully retrieves all rules
- Performance is acceptable (under 10 seconds)
- Rule count logged correctly (e.g., "50 protection rules")
- All rules have complete data
- No truncation or pagination issues

**Verification Points**:
1. All rules retrieved (count matches expected)
2. Performance is reasonable
3. No memory issues
4. Each rule has complete data

**Test Data Assumptions**:
- Application with 20+ protect rules
- May include custom rules in addition to defaults

---

#### Test 8.5: Concurrent Requests
**Description**: Test behavior when multiple concurrent requests are made for different applications.

**Input**:
```
Concurrent calls:
- get_ADR_Protect_Rules_by_app_id("<app-id-1>")
- get_ADR_Protect_Rules_by_app_id("<app-id-2>")
- get_ADR_Protect_Rules_by_app_id("<app-id-3>")
```

**Expected Behavior**:
- All requests complete successfully
- Each returns correct data for its application ID
- No data mixing between requests
- Logging is thread-safe (messages not interleaved illegibly)
- No resource contention issues

**Verification Points**:
1. All requests succeed
2. Correct data returned for each app ID
3. Logs are readable
4. No exceptions due to concurrency

**Test Data Assumptions**:
- Multiple valid application IDs
- MCP server can handle concurrent requests

---

## Test Execution Guidelines

### Pre-Test Setup
1. Verify MCP server is running and connected to a valid Contrast Security instance
2. Ensure test environment has:
   - At least 3-5 applications with different Protect configurations:
     - At least one with multiple active rules
     - At least one with Protect disabled
     - At least one with default configuration
   - Valid application IDs (UUID format) recorded for testing
   - Applications in various states (new, configured, disabled)
   - Test credentials with appropriate permissions to access Protect data

### During Testing
1. Enable DEBUG logging to capture all log messages
2. Record all request parameters for each test
3. Capture complete response including all ProtectData fields
4. Verify response structure matches expected format
5. Check log files (`/tmp/mcp-contrast.log`) for:
   - Start and end timing messages
   - Debug messages showing SDK initialization
   - Rule count accuracy
   - Error messages (when testing error scenarios)
6. Note any unexpected behavior or edge cases
7. Compare results with Contrast UI when possible

### Test Data Recommendations
For comprehensive testing, the test environment should ideally have:
- **Application with active Protect**: 5-10 rules configured, mix of MONITOR and BLOCK modes
- **Application with Protect disabled**: Valid app but no Protect configuration
- **Application with defaults**: Baseline/default Protect configuration
- **Application with custom rules**: Custom-configured rules beyond defaults
- **New application**: Recently created, minimal configuration
- **Application with hierarchy**: Rules with parent-child relationships
- **Application with CVEs**: Rules addressing specific CVE vulnerabilities

### Success Criteria
Each test passes when:
1. Response structure matches expected format (ProtectData with correct fields)
2. Data validation works correctly (null/empty app ID rejected)
3. Rule data is complete and accurate
4. Logging is comprehensive (start, end, timing, rule count, debug messages)
5. Error handling is graceful and informative
6. Performance is acceptable (typically < 5000ms)
7. Results are consistent with other methods (get_ADR_Protect_Rules)
8. No unexpected exceptions or errors occur

### Performance Benchmarks
Expected performance ranges:
- **Normal request**: 500-2000ms (depending on network and number of rules)
- **Request with many rules**: 1000-5000ms
- **Failed request**: 100-1000ms (faster failure)
- **Name-based lookup overhead**: 100-500ms additional for app name resolution

### Known Considerations
1. **Application ID Format**: Must be valid UUID format (e.g., "12345678-1234-1234-1234-123456789abc")
2. **Null vs Empty Results**: Method returns null when SDK returns null, distinguishing from empty rules list
3. **Permissions**: User must have appropriate permissions to access Protect configuration
4. **Rule Count Variations**: Different Contrast versions may have different default rule counts
5. **Environment Modes**: Modes can be "MONITOR", "BLOCK", "OFF", "DISABLED" depending on configuration

---

## Appendix A: Example Test Execution Commands

These examples show how an AI agent might invoke the tool during testing:

### Successful Scenarios
```json
// Test 1.1: Valid application with Protect enabled
{
  "appID": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}

// Test 1.3: Multiple rules
{
  "appID": "b2c3d4e5-f6a7-8901-bcde-f12345678901"
}
```

### Validation Scenarios
```json
// Test 2.1: Null application ID
{
  "appID": null
}

// Test 2.2: Empty application ID
{
  "appID": ""
}
```

### Error Scenarios
```json
// Test 4.1: Invalid format
{
  "appID": "invalid-app-id"
}

// Test 4.2: Non-existent application
{
  "appID": "00000000-0000-0000-0000-000000000000"
}
```

### Comparison Scenarios
```json
// Test 6.1: Compare with name-based lookup
// Step 1: Get by ID
{
  "appID": "a1b2c3d4-e5f6-7890-abcd-ef1234567890"
}

// Step 2: Get by name (for comparison)
{
  "applicationName": "MyTestApplication"
}
```

---

## Appendix B: Example ProtectData Response

### Successful Response with Rules
```json
{
  "success": true,
  "messages": [],
  "rules": [
    {
      "name": "SQL Injection",
      "type": "sql-injection",
      "description": "Detects and prevents SQL injection attacks",
      "development": "MONITOR",
      "qa": "MONITOR",
      "production": "BLOCK",
      "id": 1001,
      "uuid": "rule-uuid-1234-5678",
      "can_block_at_perimeter": true,
      "is_monitor_at_perimeter": false,
      "can_block": true,
      "parent_rule_uuid": null,
      "parent_rule_name": null,
      "cves": [],
      "enabled_dev": true,
      "enabled_qa": true,
      "enabled_prod": true
    },
    {
      "name": "Cross-Site Scripting (XSS)",
      "type": "xss",
      "description": "Detects and prevents XSS attacks",
      "development": "MONITOR",
      "qa": "MONITOR",
      "production": "MONITOR",
      "id": 1002,
      "uuid": "rule-uuid-2345-6789",
      "can_block_at_perimeter": true,
      "is_monitor_at_perimeter": false,
      "can_block": true,
      "parent_rule_uuid": null,
      "parent_rule_name": null,
      "cves": [],
      "enabled_dev": true,
      "enabled_qa": true,
      "enabled_prod": true
    }
  ]
}
```

### Response with No Rules (Protect Disabled)
```json
null
```
OR
```json
{
  "success": true,
  "messages": [],
  "rules": []
}
```

---

## Appendix C: Expected Log Output Examples

### Successful Request
```
INFO  - Starting retrieval of protection rules for application ID: a1b2c3d4-e5f6-7890-abcd-ef1234567890
DEBUG - ContrastSDK initialized successfully for application ID: a1b2c3d4-e5f6-7890-abcd-ef1234567890
DEBUG - SDKExtension initialized successfully for application ID: a1b2c3d4-e5f6-7890-abcd-ef1234567890
DEBUG - Retrieving protection configuration for application ID: a1b2c3d4-e5f6-7890-abcd-ef1234567890
INFO  - Successfully retrieved 8 protection rules for application ID: a1b2c3d4-e5f6-7890-abcd-ef1234567890 (took 1250 ms)
```

### Request with No Rules
```
INFO  - Starting retrieval of protection rules for application ID: b2c3d4e5-f6a7-8901-bcde-f12345678901
DEBUG - ContrastSDK initialized successfully for application ID: b2c3d4e5-f6a7-8901-bcde-f12345678901
DEBUG - SDKExtension initialized successfully for application ID: b2c3d4e5-f6a7-8901-bcde-f12345678901
DEBUG - Retrieving protection configuration for application ID: b2c3d4e5-f6a7-8901-bcde-f12345678901
WARN  - No protection data returned for application ID: b2c3d4e5-f6a7-8901-bcde-f12345678901 (took 850 ms)
```

### Validation Error
```
ERROR - Cannot retrieve protection rules - application ID is null or empty
```

### API Error
```
INFO  - Starting retrieval of protection rules for application ID: invalid-id
DEBUG - ContrastSDK initialized successfully for application ID: invalid-id
DEBUG - SDKExtension initialized successfully for application ID: invalid-id
DEBUG - Retrieving protection configuration for application ID: invalid-id
ERROR - Error retrieving protection rules for application ID: invalid-id (after 450 ms): Application not found
[Stack trace...]
```

---

## Test Coverage Summary

This test plan covers:
- ✓ 3 basic functionality test cases (valid app, rule details, multiple rules)
- ✓ 2 input validation test cases (null, empty)
- ✓ 2 empty/null results test cases (Protect disabled, null SDK response)
- ✓ 4 error handling test cases (invalid format, non-existent app, network failure, auth failure)
- ✓ 4 performance/logging test cases (timing, timing on failure, rule count, debug logs)
- ✓ 3 comparison/consistency test cases (consistency with name-based method, name resolution, performance comparison)
- ✓ 4 data integrity test cases (environment config, rule hierarchy, CVE info, blocking flags)
- ✓ 5 edge cases test cases (new app, defaults, all disabled, many rules, concurrent requests)

**Total: 27 comprehensive test cases**

Each test case is designed to be executed by an AI agent using the MCP server, with clear input parameters, expected behaviors, verification points, and test data assumptions.

---

## Quick Reference: Test Priority Matrix

### High Priority (Must Test)
- Test 1.1: Valid application with Protect rules (core functionality)
- Test 2.1 & 2.2: Null/empty validation (input validation)
- Test 4.1 & 4.2: Invalid/non-existent app ID (error handling)
- Test 5.1: Performance timing verification (observability)
- Test 6.1: Consistency with name-based method (correctness)

### Medium Priority (Should Test)
- Test 1.2 & 1.3: Rule details and multiple rules (data completeness)
- Test 3.1 & 3.2: Empty results scenarios (edge cases)
- Test 5.3: Rule count accuracy (logging verification)
- Test 7.1-7.4: Data integrity tests (data quality)

### Low Priority (Nice to Test)
- Test 4.3 & 4.4: Network/auth failures (requires special setup)
- Test 6.2 & 6.3: Name resolution and performance comparison (optimization)
- Test 8.1-8.5: Special scenarios (comprehensive coverage)

This priority matrix helps AI agents focus on critical tests first while allowing flexibility for comprehensive testing when time permits.
