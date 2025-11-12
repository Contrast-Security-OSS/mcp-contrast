# Test Plan: list_vulns_by_app_latest_session Tool

## Overview

This test plan provides comprehensive test cases for the `list_vulns_by_app_latest_session` tool in AssessService.java (lines 250-281). This tool takes an application ID and returns vulnerabilities from the most recent session for that application.

## Tool Behavior

The tool performs the following operations:
1. Accepts an `appID` parameter (String)
2. Retrieves the Contrast SDK instance
3. Looks up the application by name using `SDKHelper.getApplicationByName()`
4. If application exists:
   - Calls `SDKExtension.getLatestSessionMetadata()` to retrieve the most recent session
   - Creates a `TraceFilterBody` with the agent session ID from the latest session
   - Calls `SDKExtension.getTracesExtended()` with the session filter
   - Maps traces to `VulnLight` objects using `VulnerabilityMapper`
   - Returns the list of vulnerabilities
5. If application doesn't exist, returns an empty list
6. Throws `IOException` on errors

## Test Data Requirements

### Applications
- **app-with-recent-session**: Application with recent vulnerabilities in latest session
- **app-with-multiple-sessions**: Application with multiple sessions (to test latest selection)
- **app-with-empty-latest-session**: Application with latest session containing no vulnerabilities
- **app-with-no-sessions**: Application that exists but has no sessions
- **non-existent-app**: Application name that doesn't exist

### Sessions
- **Session A** (older): Created 7 days ago, contains 5 vulnerabilities
- **Session B** (latest): Created 1 day ago, contains 3 vulnerabilities (different from Session A)

### Vulnerabilities
For Session A (older):
- SQL Injection (CRITICAL, uuid-session-a-1)
- XSS Reflected (HIGH, uuid-session-a-2)
- Path Traversal (MEDIUM, uuid-session-a-3)
- Command Injection (HIGH, uuid-session-a-4)
- LDAP Injection (LOW, uuid-session-a-5)

For Session B (latest):
- Crypto Bad MAC (CRITICAL, uuid-session-b-1)
- XXE (HIGH, uuid-session-b-2)
- Insecure Deserialization (MEDIUM, uuid-session-b-3)

## Test Cases

### 1. Basic Functionality - Getting Latest Session Vulnerabilities

**Objective**: Verify the tool correctly retrieves vulnerabilities from the latest session

**Test Data Needed**:
- Application: `app-with-recent-session`
- Latest session with 3 vulnerabilities (Session B data)

**Test Steps**:
1. Call tool with `appID = "app-with-recent-session"`
2. Verify return value is a List<VulnLight>
3. Verify list contains exactly 3 vulnerabilities
4. Verify vulnerabilities match Session B data (Crypto Bad MAC, XXE, Insecure Deserialization)
5. Verify UUIDs match Session B vulnerabilities (uuid-session-b-1, uuid-session-b-2, uuid-session-b-3)

**Expected Behavior**:
- Method returns successfully
- List size = 3
- All returned vulnerabilities have correct titles, severities, and UUIDs from Session B
- No IOException thrown
- Logs show: "Listing vulnerabilities for application: app-with-recent-session"

**Verification**:
```java
List<VulnLight> results = assessService.listVulnsInAppByNameForLatestSession("app-with-recent-session");
assertEquals(3, results.size());
assertTrue(results.stream().anyMatch(v -> v.uuid().equals("uuid-session-b-1")));
assertTrue(results.stream().anyMatch(v -> v.uuid().equals("uuid-session-b-2")));
assertTrue(results.stream().anyMatch(v -> v.uuid().equals("uuid-session-b-3")));
// Verify Session A vulnerabilities are NOT present
assertFalse(results.stream().anyMatch(v -> v.uuid().equals("uuid-session-a-1")));
```

---

### 2. Session Selection - Verify Latest Session is Used

**Objective**: Verify the tool correctly identifies and uses only the latest session, excluding older sessions

**Test Data Needed**:
- Application: `app-with-multiple-sessions`
- Session A: Created 2025-01-10, agent_session_id = "session-a-123", 5 vulnerabilities
- Session B: Created 2025-01-17, agent_session_id = "session-b-456", 3 vulnerabilities (latest)

**Test Steps**:
1. Mock `SDKExtension.getLatestSessionMetadata()` to return Session B metadata
2. Capture the `TraceFilterBody` passed to `SDKExtension.getTracesExtended()`
3. Call tool with `appID = "app-with-multiple-sessions"`
4. Verify the filter contains agent_session_id = "session-b-456"
5. Verify returned vulnerabilities match Session B only (3 vulnerabilities)

**Expected Behavior**:
- `getLatestSessionMetadata()` is called exactly once
- `TraceFilterBody.getAgentSessionId()` returns "session-b-456"
- Results contain only Session B vulnerabilities
- Session A vulnerabilities are excluded
- No vulnerabilities from sessions older than Session B appear

**Verification**:
```java
// Capture the filter used
ArgumentCaptor<TraceFilterBody> filterCaptor = ArgumentCaptor.forClass(TraceFilterBody.class);
verify(mockSDKExtension).getTracesExtended(eq(TEST_ORG_ID), eq(TEST_APP_ID), filterCaptor.capture());

TraceFilterBody actualFilter = filterCaptor.getValue();
assertEquals("session-b-456", actualFilter.getAgentSessionId());

// Verify results
assertEquals(3, results.size());
assertTrue(results.stream().noneMatch(v -> v.uuid().startsWith("uuid-session-a-")));
```

---

### 3. Empty Results - Application with No Sessions

**Objective**: Verify graceful handling when application exists but has no sessions

**Test Data Needed**:
- Application: `app-with-no-sessions`
- Application exists in Contrast
- `getLatestSessionMetadata()` returns null (no sessions)

**Test Steps**:
1. Mock `SDKHelper.getApplicationByName()` to return an Application object
2. Mock `SDKExtension.getLatestSessionMetadata()` to return null
3. Call tool with `appID = "app-with-no-sessions"`
4. Verify empty list is returned (not null)
5. Verify no exception is thrown

**Expected Behavior**:
- Method returns empty list (size = 0)
- No IOException thrown
- No NullPointerException thrown
- `getTracesExtended()` is still called (filter will have null session ID)
- Logs show application was found but handled gracefully

**Verification**:
```java
List<VulnLight> results = assessService.listVulnsInAppByNameForLatestSession("app-with-no-sessions");
assertNotNull(results);
assertTrue(results.isEmpty());
```

---

### 4. Empty Results - Latest Session with No Vulnerabilities

**Objective**: Verify graceful handling when latest session exists but contains no vulnerabilities

**Test Data Needed**:
- Application: `app-with-empty-latest-session`
- Latest session exists (session-empty-789)
- `getTracesExtended()` returns TracesExtended with empty traces list

**Test Steps**:
1. Mock `getLatestSessionMetadata()` to return session with ID "session-empty-789"
2. Mock `getTracesExtended()` to return TracesExtended with empty traces list
3. Call tool with `appID = "app-with-empty-latest-session"`
4. Verify empty list is returned

**Expected Behavior**:
- Method returns empty list (size = 0)
- No IOException thrown
- Session metadata was retrieved successfully
- Filter was created with correct session ID
- Traces query executed but returned no results

**Verification**:
```java
List<VulnLight> results = assessService.listVulnsInAppByNameForLatestSession("app-with-empty-latest-session");
assertNotNull(results);
assertEquals(0, results.size());
verify(mockSDKExtension).getLatestSessionMetadata(TEST_ORG_ID, TEST_APP_ID);
verify(mockSDKExtension).getTracesExtended(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterBody.class));
```

---

### 5. Validation - Invalid Application Name

**Objective**: Verify graceful handling when application ID doesn't exist

**Test Data Needed**:
- Application name: `non-existent-app`
- `SDKHelper.getApplicationByName()` returns Optional.empty()

**Test Steps**:
1. Mock `SDKHelper.getApplicationByName()` to return Optional.empty()
2. Call tool with `appID = "non-existent-app"`
3. Verify empty list is returned
4. Verify no exception is thrown
5. Verify SDK methods are not called (short-circuit behavior)

**Expected Behavior**:
- Method returns empty list (size = 0)
- No IOException thrown
- Logs show: "Application with name non-existent-app not found, returning empty list"
- `getLatestSessionMetadata()` is NOT called
- `getTracesExtended()` is NOT called

**Verification**:
```java
List<VulnLight> results = assessService.listVulnsInAppByNameForLatestSession("non-existent-app");
assertNotNull(results);
assertTrue(results.isEmpty());
verify(mockSDKExtension, never()).getLatestSessionMetadata(any(), any());
verify(mockSDKExtension, never()).getTracesExtended(any(), any(), any());
```

---

### 6. Validation - Null Application Name

**Objective**: Verify behavior with null input

**Test Data Needed**:
- Application name: null

**Test Steps**:
1. Call tool with `appID = null`
2. Observe behavior (either exception or empty list)

**Expected Behavior**:
- Method throws IOException OR returns empty list (implementation dependent)
- Error is logged
- No NullPointerException propagates to caller

**Verification**:
```java
// If implementation throws exception:
assertThrows(IOException.class, () ->
    assessService.listVulnsInAppByNameForLatestSession(null)
);

// OR if implementation returns empty list:
List<VulnLight> results = assessService.listVulnsInAppByNameForLatestSession(null);
assertNotNull(results);
assertTrue(results.isEmpty());
```

---

### 7. Validation - Empty String Application Name

**Objective**: Verify behavior with empty string input

**Test Data Needed**:
- Application name: ""

**Test Steps**:
1. Mock `SDKHelper.getApplicationByName()` to return Optional.empty()
2. Call tool with `appID = ""`
3. Verify empty list is returned

**Expected Behavior**:
- Method returns empty list
- Application lookup fails (empty name doesn't match any app)
- Behaves same as non-existent application

**Verification**:
```java
List<VulnLight> results = assessService.listVulnsInAppByNameForLatestSession("");
assertNotNull(results);
assertTrue(results.isEmpty());
```

---

### 8. Historical Data - Verify Older Sessions Excluded

**Objective**: Verify that vulnerabilities from older sessions are completely excluded

**Test Data Needed**:
- Application: `app-with-multiple-sessions`
- Session A (7 days old): 5 unique vulnerabilities
- Session B (1 day old, latest): 3 unique vulnerabilities (no overlap with Session A)
- Session C (10 days old): 2 unique vulnerabilities

**Test Steps**:
1. Mock `getLatestSessionMetadata()` to return Session B
2. Mock `getTracesExtended()` to return only Session B vulnerabilities
3. Call tool
4. Verify NO vulnerabilities from Session A appear in results
5. Verify NO vulnerabilities from Session C appear in results
6. Verify ONLY Session B vulnerabilities appear

**Expected Behavior**:
- Results contain exactly 3 vulnerabilities
- All results are from Session B
- Zero vulnerabilities from older sessions (A or C)
- Filter was applied with Session B's agent_session_id

**Verification**:
```java
List<VulnLight> results = assessService.listVulnsInAppByNameForLatestSession("app-with-multiple-sessions");
assertEquals(3, results.size());

// Session B UUIDs present
Set<String> sessionBUuids = Set.of("uuid-session-b-1", "uuid-session-b-2", "uuid-session-b-3");
Set<String> resultUuids = results.stream().map(VulnLight::uuid).collect(Collectors.toSet());
assertEquals(sessionBUuids, resultUuids);

// Session A UUIDs NOT present
assertFalse(resultUuids.contains("uuid-session-a-1"));
assertFalse(resultUuids.contains("uuid-session-a-2"));
assertFalse(resultUuids.contains("uuid-session-a-3"));
assertFalse(resultUuids.contains("uuid-session-a-4"));
assertFalse(resultUuids.contains("uuid-session-a-5"));
```

---

### 9. Error Handling - SDK Exception During Session Lookup

**Objective**: Verify proper exception handling when session metadata retrieval fails

**Test Data Needed**:
- Application: `app-with-recent-session`
- `getLatestSessionMetadata()` throws RuntimeException

**Test Steps**:
1. Mock `SDKHelper.getApplicationByName()` to return valid application
2. Mock `getLatestSessionMetadata()` to throw RuntimeException("API connection failed")
3. Call tool
4. Verify IOException is thrown
5. Verify error message contains helpful information

**Expected Behavior**:
- Method throws IOException
- Exception message contains "Failed to list vulnerabilities"
- Original exception is wrapped (cause chain preserved)
- Error is logged with application ID

**Verification**:
```java
Exception exception = assertThrows(IOException.class, () ->
    assessService.listVulnsInAppByNameForLatestSession("app-with-recent-session")
);
assertTrue(exception.getMessage().contains("Failed to list vulnerabilities"));
assertNotNull(exception.getCause());
```

---

### 10. Error Handling - SDK Exception During Traces Retrieval

**Objective**: Verify proper exception handling when vulnerability retrieval fails

**Test Data Needed**:
- Application: `app-with-recent-session`
- `getTracesExtended()` throws UnauthorizedException

**Test Steps**:
1. Mock `getLatestSessionMetadata()` to return valid session
2. Mock `getTracesExtended()` to throw UnauthorizedException
3. Call tool
4. Verify IOException is thrown

**Expected Behavior**:
- Method throws IOException
- Exception wraps the UnauthorizedException
- Error is logged

**Verification**:
```java
Exception exception = assertThrows(IOException.class, () ->
    assessService.listVulnsInAppByNameForLatestSession("app-with-recent-session")
);
assertTrue(exception.getMessage().contains("Failed to list vulnerabilities"));
```

---

### 11. Integration - VulnerabilityMapper Mapping

**Objective**: Verify VulnerabilityMapper correctly transforms TraceExtended to VulnLight

**Test Data Needed**:
- Application with latest session containing 1 vulnerability
- TraceExtended with complete data:
  - title: "SQL Injection in login"
  - rule: "sql-injection"
  - uuid: "test-uuid-123"
  - severity: "CRITICAL"
  - status: "Reported"
  - lastTimeSeen: 1736938200000L (2025-01-15 10:30 UTC)
  - firstTimeSeen: 1704067200000L (2024-01-01 00:00 UTC)
  - closedTime: null

**Test Steps**:
1. Create complete TraceExtended mock
2. Call tool
3. Verify VulnLight fields are correctly mapped
4. Verify timestamp formatting (ISO 8601 with timezone)

**Expected Behavior**:
- VulnLight.title() = "SQL Injection in login"
- VulnLight.rule() = "sql-injection"
- VulnLight.uuid() = "test-uuid-123"
- VulnLight.severity() = "CRITICAL"
- VulnLight.status() = "Reported"
- VulnLight.lastSeenAt() matches ISO 8601 format with timezone
- VulnLight.firstSeenAt() matches ISO 8601 format with timezone
- VulnLight.closedAt() is null

**Verification**:
```java
List<VulnLight> results = assessService.listVulnsInAppByNameForLatestSession("app-with-recent-session");
assertEquals(1, results.size());

VulnLight vuln = results.get(0);
assertEquals("SQL Injection in login", vuln.title());
assertEquals("sql-injection", vuln.rule());
assertEquals("test-uuid-123", vuln.uuid());
assertEquals("CRITICAL", vuln.severity());
assertEquals("Reported", vuln.status());

// Verify ISO 8601 timestamp format
String iso8601Pattern = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}";
assertTrue(vuln.lastSeenAt().matches(iso8601Pattern));
assertTrue(vuln.firstSeenAt().matches(iso8601Pattern));
assertNull(vuln.closedAt());
```

---

### 12. Edge Case - SessionMetadataResponse with Null AgentSession

**Objective**: Verify handling when session metadata is returned but agentSession is null

**Test Data Needed**:
- Application: `app-with-malformed-session`
- SessionMetadataResponse with null agentSession

**Test Steps**:
1. Mock `getLatestSessionMetadata()` to return SessionMetadataResponse where `getAgentSession()` returns null
2. Call tool
3. Verify filter is created with null agent session ID
4. Verify no NullPointerException

**Expected Behavior**:
- No NullPointerException thrown
- Filter is created but agentSessionId is not set (null)
- Query proceeds but may return broader results or empty results
- Method completes without error

**Verification**:
```java
List<VulnLight> results = assessService.listVulnsInAppByNameForLatestSession("app-with-malformed-session");
assertNotNull(results);
// Result size depends on backend behavior when session filter is null
```

---

### 13. Edge Case - SessionMetadataResponse with Null AgentSessionId

**Objective**: Verify handling when agentSession exists but agentSessionId is null

**Test Data Needed**:
- Application: `app-with-incomplete-session`
- SessionMetadataResponse where `getAgentSession().getAgentSessionId()` returns null

**Test Steps**:
1. Mock `getLatestSessionMetadata()` to return session with null agentSessionId
2. Call tool
3. Verify no NullPointerException
4. Verify filter is created but session ID is not set

**Expected Behavior**:
- No NullPointerException thrown
- Filter's agentSessionId field remains null
- Query proceeds without session filter
- Method completes successfully

**Verification**:
```java
List<VulnLight> results = assessService.listVulnsInAppByNameForLatestSession("app-with-incomplete-session");
assertNotNull(results);
```

---

## Test Implementation Notes

### Mocking Requirements

For each test, you will need to mock:

1. **SDKHelper.getApplicationByName()** (static method)
   - Use `MockedStatic<SDKHelper>`
   - Return `Optional<Application>` with app ID

2. **SDKExtension.getLatestSessionMetadata()**
   - Return `SessionMetadataResponse` with agent session details
   - Can return null for no-sessions scenarios

3. **SDKExtension.getTracesExtended()**
   - Return `TracesExtended` with list of `TraceExtended` objects
   - Each TraceExtended should have complete vulnerability data

4. **ContrastSDK.getSDK()** (static method via SDKHelper)
   - Return mocked ContrastSDK instance

### Test Utilities

Use existing test patterns from `AssessServiceTest.java`:
- `@ExtendWith(MockitoExtension.class)` for Mockito support
- `MockedStatic<SDKHelper>` for static method mocking
- `ReflectionTestUtils.setField()` to inject configuration
- `ArgumentCaptor` to verify method arguments
- Named timestamp constants for readability

### Assertions

For each test case:
- Verify return value (not null, correct size, correct content)
- Verify mock interactions (methods called/not called)
- Verify exception handling (correct exceptions thrown)
- Verify data transformation (VulnLight fields match TraceExtended)
- Verify logging (use log capture if needed)

## Success Criteria

All test cases pass with:
- 100% code coverage for the `listVulnsInAppByNameForLatestSession` method
- All edge cases handled gracefully
- All error paths tested
- Integration with VulnerabilityMapper verified
- Session filtering logic validated
- Historical data exclusion confirmed

## Dependencies

- JUnit 5
- Mockito (including MockedStatic)
- Spring Test (ReflectionTestUtils)
- Contrast SDK classes (Application, TraceExtended, etc.)
- Project-specific classes (VulnLight, SessionMetadataResponse, etc.)
