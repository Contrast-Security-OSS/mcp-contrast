# Test Plan: get_protect_rules Tool

## Overview

This test plan provides comprehensive testing guidance for the `get_protect_rules` MCP tool. This tool retrieves Protect/ADR rules for a specific application by its ID.

### Migration Notes

**This plan replaces:**
- `test-plan-get_ADR_Protect_Rules.md` (original at root level)

**Key Changes from Original Tool:**
- **Tool renamed**: `get_ADR_Protect_Rules` / `get_ADR_Protect_Rules_by_app_id` → `get_protect_rules`
- **Parameter unchanged**: Still uses `appId` (not app name)
- **Workflow**: Use `search_applications(name=...)` first to find appId

### Tool Signature

**MCP Tool Name:** `get_protect_rules`

**Parameters:**
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `appId` | String | Yes | Application ID (use search_applications to find) |

### Response Structure

**Returns:** `SingleToolResponse<ProtectData>`

```java
SingleToolResponse {
    ProtectData data,             // Protect configuration from SDK
    String message,               // Warnings or info messages
    boolean found                 // True if data returned
}

// ProtectData structure:
ProtectData {
    boolean success,              // API request succeeded
    List<String> messages,        // API messages
    List<Rule> rules              // List of protect rules
}

// Rule structure:
Rule {
    String name,                  // Rule name (e.g., "SQL Injection")
    String type,                  // Rule type (e.g., "sql-injection")
    String description,           // Rule description
    String development,           // Dev mode: "MONITOR", "BLOCK", "OFF"
    String qa,                    // QA mode
    String production,            // Production mode
    int id,                       // Numeric rule ID
    String uuid,                  // Rule UUID
    boolean can_block,            // Can block attacks
    boolean can_block_at_perimeter,
    boolean is_monitor_at_perimeter,
    boolean enabled_dev,          // Enabled in dev
    boolean enabled_qa,           // Enabled in QA
    boolean enabled_prod,         // Enabled in production
    String parent_rule_uuid,      // Parent rule UUID (if any)
    String parent_rule_name,      // Parent rule name (if any)
    List<Cve> cves                // Related CVEs
}
```

### Critical Behavioral Notes

| Behavior | Description |
|----------|-------------|
| **appId Required** | Must provide application ID, not name |
| **Premium Feature** | Protect/ADR requires license |
| **Null Response** | Returns null if no Protect config exists |
| **Empty Rules** | Returns warning if Protect enabled but no rules |
| **SDK Delegation** | Passes through to `SDKExtension.getProtectConfig()` |

---

## 1. Basic Functionality Tests

### Test Case 1.1: Retrieve Protect Rules - Success

**Objective:** Verify tool returns Protect rules for a valid application.

**Prerequisites:**
- Application with Protect enabled and rules configured

**Test Steps:**
1. Call `search_applications` to get an application with Protect enabled
2. Note the `appId` from the response
3. Call `get_protect_rules(appId="<app-id>")`

**Expected Results:**
- Response status: success
- `data` contains ProtectData with rules
- `data.rules` is array with 1+ rules
- `found: true`

---

### Test Case 1.2: Rule Structure Verification

**Objective:** Verify returned rules have correct structure.

**Test Steps:**
1. Call `get_protect_rules` with valid appId
2. Examine the first rule

**Expected Results:**
- Each rule contains:
  - `name`: non-empty (e.g., "SQL Injection")
  - `type`: rule type (e.g., "sql-injection")
  - `uuid`: unique identifier
  - `development`, `qa`, `production`: mode strings
  - `enabled_dev`, `enabled_qa`, `enabled_prod`: booleans
  - `can_block`, `can_block_at_perimeter`: capability flags

---

### Test Case 1.3: Multiple Rules Retrieval

**Objective:** Verify all rules returned for application.

**Test Steps:**
1. Call `get_protect_rules` for app with many rules
2. Count rules in response

**Expected Results:**
- Multiple rules returned (typically 5-20+)
- All rules have unique UUIDs
- Common rule types present (sql-injection, xss, path-traversal, etc.)

---

### Test Case 1.4: Environment-Specific Modes

**Objective:** Verify environment modes are populated.

**Test Steps:**
1. Call `get_protect_rules`
2. Examine `development`, `qa`, `production` fields

**Expected Results:**
- Each field contains mode string: "MONITOR", "BLOCK", or "OFF"
- Boolean flags align with modes (OFF = disabled, MONITOR/BLOCK = enabled)
- Different rules may have different mode configurations

---

## 2. Empty/Edge Case Tests

### Test Case 2.1: Application with Protect Disabled

**Objective:** Verify behavior when application doesn't have Protect.

**Prerequisites:**
- Application that exists but has no Protect configuration

**Test Steps:**
1. Find application without Protect enabled
2. Call `get_protect_rules(appId="<app-without-protect>")`

**Expected Results:**
- Response status: success (no exception)
- `data` is null
- `found: false`
- No error thrown

---

### Test Case 2.2: Application with Protect Enabled, No Rules

**Objective:** Verify behavior when Protect enabled but no rules configured.

**Test Steps:**
1. Find application with Protect but empty rule list
2. Call `get_protect_rules`

**Expected Results:**
- Response succeeds
- `data.rules` is empty array `[]`
- `message` contains warning: "Application has Protect enabled but no rules are configured."
- `found: true`

---

## 3. Validation Tests

### Test Case 3.1: Missing appId Parameter

**Objective:** Verify validation error for missing required parameter.

**Test Steps:**
1. Call `get_protect_rules` without appId (or with empty string)

**Expected Results:**
- Validation error returned
- Error message: "appId is required" or similar
- No API call made

---

### Test Case 3.2: Invalid appId Format

**Objective:** Verify behavior with malformed application ID.

**Test Steps:**
1. Call `get_protect_rules(appId="invalid-format")`
2. Call `get_protect_rules(appId="!!invalid!!")`

**Expected Results:**
- Either validation error or API error
- Clear error message
- No crash or hang

---

### Test Case 3.3: Nonexistent Application ID

**Objective:** Verify behavior when appId doesn't exist.

**Test Steps:**
1. Call `get_protect_rules(appId="00000000-0000-0000-0000-000000000000")`

**Expected Results:**
- Error response or null data
- Appropriate error message
- No crash

---

### Test Case 3.4: Whitespace in appId

**Objective:** Verify handling of whitespace.

**Test Steps:**
1. Call `get_protect_rules(appId=" abc123 ")` (with spaces)
2. Call `get_protect_rules(appId="   ")` (only whitespace)

**Expected Results:**
- Whitespace-only: validation error
- Leading/trailing spaces: either trimmed or error
- Consistent behavior

---

## 4. Integration Tests

### Test Case 4.1: Workflow with search_applications

**Objective:** Verify typical workflow of finding app then getting rules.

**Test Steps:**
1. Call `search_applications(name="<known-app-name>")`
2. Extract `appId` from results
3. Call `get_protect_rules(appId="<extracted-id>")`

**Expected Results:**
- Both calls succeed
- Rules returned for correct application
- IDs match between calls

---

### Test Case 4.2: Rule Types Match search_attacks Results

**Objective:** Verify Protect rules relate to attack types.

**Test Steps:**
1. Get Protect rules for an application
2. Note the rule types (e.g., "sql-injection", "xss")
3. Call `search_attacks` for same application
4. Check if attacks match rule types

**Expected Results:**
- Attack types should match Protect rule types
- Blocked attacks should relate to BLOCK-mode rules
- Monitored attacks should relate to MONITOR-mode rules

---

## 5. Response Structure Tests

### Test Case 5.1: Response Schema Validation

**Objective:** Verify response conforms to SingleToolResponse structure.

**Test Steps:**
1. Call `get_protect_rules` with valid appId
2. Examine response fields

**Expected Results:**
- Response has `data`, `message`, `found` fields
- `data` is ProtectData or null
- `data.success` is boolean
- `data.rules` is array

---

### Test Case 5.2: Warning Messages

**Objective:** Verify warning messages are informative.

**Test Steps:**
1. Test scenarios that generate warnings:
   - Empty rules list
   - No Protect configuration

**Expected Results:**
- Warnings are clear and actionable
- Help user understand the data state

---

## 6. Error Handling Tests

### Test Case 6.1: API Connection Failure

**Objective:** Verify graceful handling when API unavailable.

**Prerequisites:**
- Simulate connection failure

**Test Steps:**
1. Configure invalid credentials
2. Call `get_protect_rules`

**Expected Results:**
- Error response returned
- Error message indicates connection issue
- No crash or hang

---

### Test Case 6.2: Timeout Handling

**Objective:** Verify behavior with slow responses.

**Test Steps:**
1. Test with network delays if possible
2. Call `get_protect_rules`

**Expected Results:**
- Returns within reasonable timeout
- Either success or error (no hang)

---

### Test Case 6.3: Repeated Calls Consistency

**Objective:** Verify consistent results across calls.

**Test Steps:**
1. Call `get_protect_rules` 3 times with same appId
2. Compare results

**Expected Results:**
- All 3 calls return same data
- No variation
- Consistent performance

---

## 7. Data Integrity Tests

### Test Case 7.1: Rule UUID Uniqueness

**Objective:** Verify all rules have unique identifiers.

**Test Steps:**
1. Get Protect rules
2. Extract all UUIDs
3. Check for duplicates

**Expected Results:**
- All UUIDs are unique
- No duplicate rules
- UUIDs are valid format

---

### Test Case 7.2: Parent-Child Rule Relationships

**Objective:** Verify rule hierarchy is correct.

**Test Steps:**
1. Get Protect rules
2. Find rules with `parent_rule_uuid` set
3. Verify parent exists in rule list

**Expected Results:**
- If parent_rule_uuid set, parent exists
- Top-level rules have null parent
- Hierarchy makes logical sense

---

### Test Case 7.3: CVE Information

**Objective:** Verify CVE data is correctly included.

**Test Steps:**
1. Get Protect rules
2. Find rules with CVEs

**Expected Results:**
- `cves` is array (not null)
- CVEs have valid identifiers (e.g., "CVE-2023-12345")
- CVE data relates to rule's attack type

---

## 8. Performance Tests

### Test Case 8.1: Response Time Benchmarks

**Objective:** Measure acceptable performance.

**Test Steps:**
1. Call tool and measure response time
2. Repeat 5 times
3. Calculate average

**Expected Results:**
- Normal case: < 3 seconds
- Many rules: < 5 seconds
- Consistent across calls

---

## Test Execution Guidelines

### Pre-Test Setup
1. Verify MCP server running with valid credentials
2. Have applications with:
   - Protect enabled with rules
   - Protect disabled
   - Various rule configurations

### Workflow for Finding Test Data
```
1. search_applications() → get list of apps
2. Pick apps with different Protect configurations
3. Note appIds for testing
4. Call get_protect_rules(appId) for each
```

### Success Criteria
The `get_protect_rules` tool passes testing if:
- Basic functionality test passes (TC 1.1)
- Rule structure is correct (TC 1.2)
- Empty cases handled gracefully (TC 2.1-2.2)
- Validation catches invalid input (TC 3.1-3.4)
- Integration with other tools works (TC 4.1)
- Error handling is graceful (TC 6.1)
- Performance is acceptable (< 5 seconds)

---

## Test Coverage Summary

| Category | Test Cases | Coverage |
|----------|------------|----------|
| Basic Functionality | 4 | Core behavior verification |
| Empty/Edge Cases | 2 | No data scenarios |
| Validation | 4 | Parameter validation |
| Integration | 2 | Workflow with other tools |
| Response Structure | 2 | Schema validation |
| Error Handling | 3 | Failures, timeouts |
| Data Integrity | 3 | Data quality checks |
| Performance | 1 | Response time |

**Total: 21 test cases**

---

## Appendix: Expected Behavior Examples

### Successful Response Example
```json
{
  "data": {
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
        "can_block": true,
        "can_block_at_perimeter": true,
        "is_monitor_at_perimeter": false,
        "enabled_dev": true,
        "enabled_qa": true,
        "enabled_prod": true,
        "parent_rule_uuid": null,
        "parent_rule_name": null,
        "cves": []
      },
      {
        "name": "Cross-Site Scripting (XSS)",
        "type": "xss-reflected",
        "description": "Detects XSS attacks",
        "development": "MONITOR",
        "qa": "MONITOR",
        "production": "MONITOR",
        "id": 1002,
        "uuid": "rule-uuid-2345-6789",
        "can_block": true,
        "can_block_at_perimeter": true,
        "is_monitor_at_perimeter": false,
        "enabled_dev": true,
        "enabled_qa": true,
        "enabled_prod": true,
        "parent_rule_uuid": null,
        "parent_rule_name": null,
        "cves": []
      }
    ]
  },
  "message": null,
  "found": true
}
```

### No Protect Configuration Response
```json
{
  "data": null,
  "message": null,
  "found": false
}
```

### Empty Rules Response
```json
{
  "data": {
    "success": true,
    "messages": [],
    "rules": []
  },
  "message": "Application has Protect enabled but no rules are configured.",
  "found": true
}
```

### Error Response Example
```json
{
  "data": null,
  "message": "Validation failed: appId is required",
  "found": false
}
```

---

## Logging Verification

Check MCP server logs (`/tmp/mcp-contrast.log`) for:
- Request received with appId
- Debug: "Retrieving protection configuration for application ID: {appId}"
- Debug: "Successfully retrieved {N} protection rules for application ID: {appId}"
- No error logs in success case

---

## References

- **Tool Implementation**: `tool/attack/GetProtectRulesTool.java`
- **Params Class**: `tool/attack/params/GetProtectRulesParams.java`
- **Related Tools**: `search_applications`, `search_attacks`
- **Old Test Plan**: `test-plan-get_ADR_Protect_Rules.md` (root level)
