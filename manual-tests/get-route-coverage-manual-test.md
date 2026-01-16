# Test Cases for `get_route_coverage` Tool

## Known Test Data

**Application:** DemoRouteSession (appId: `9e18a607-2b01-41c7-b35b-52a256840fea`)

**Routes (3 total):**

| Signature | Status | Environment | Route Hash | Observations |
|-----------|--------|-------------|------------|--------------|
| Test.acmeDummy.login(String) | EXERCISED | DEVELOPMENT | -4830348719117206576 | GET /login/test, GET /login/test1, GET /login/test2 |
| Test.bearKing.update(String) | DISCOVERED | DEVELOPMENT | 3189172298702315763 | POST /update/user |
| Test.bearKing.create(String) | DISCOVERED | DEVELOPMENT | 7649651984418146470 | PUT /create/user |

**Coverage Statistics:**
- Total Routes: 3
- Exercised: 1 (33.33%)
- Discovered: 2

**Session Metadata:**

| Field | Values | Sessions |
|-------|--------|----------|
| repo | TS | 2 |
| commit | 100, 200 | 1 each |
| developer | Ellen, Sam | 1 each |

**Session-to-Route Mapping:**

| Session (developer/commit) | Routes | Specific Observations |
|----------------------------|--------|----------------------|
| Ellen / 100 | login, create | GET /login/test1, PUT /create/user |
| Sam / 200 | login, update | GET /login/test2, POST /update/user |

**Latest Session:** Sam / commit=200 (login + update routes)

---

## Unfiltered Query Tests

### Test 1: Get all routes (no filters)
**Purpose:** Verify retrieving all route coverage without any filters.

**Prompt:**
```
use contrast mcp to get the route coverage for the DemoRouteSession app
```

**Expected Result:** 3 routes returned
- Test.acmeDummy.login(String) - EXERCISED
- Test.bearKing.update(String) - DISCOVERED
- Test.bearKing.create(String) - DISCOVERED

Coverage: 33.33% (1 exercised / 3 total)

---

## Session Metadata Filter Tests

### Test 2: Filter by developer (Ellen)
**Purpose:** Verify filtering routes by developer session metadata.

**Prompt:**
```
use contrast mcp to get the route coverage for the DemoRouteSession app filtered by developer Ellen
```

**Expected Result:** 2 routes returned
- Test.acmeDummy.login(String) - observation: GET /login/test1
- Test.bearKing.create(String) - observation: PUT /create/user

---

### Test 3: Filter by developer (Sam)
**Purpose:** Verify filtering routes by a different developer value.

**Prompt:**
```
use contrast mcp to get the route coverage for the DemoRouteSession app filtered by developer Sam
```

**Expected Result:** 2 routes returned
- Test.acmeDummy.login(String) - observation: GET /login/test2
- Test.bearKing.update(String) - observation: POST /update/user

---

### Test 4: Filter by commit (100)
**Purpose:** Verify filtering routes by commit session metadata.

**Prompt:**
```
use contrast mcp to get the route coverage for the DemoRouteSession app filtered by commit 100
```

**Expected Result:** 2 routes returned (same as Ellen)
- Test.acmeDummy.login(String)
- Test.bearKing.create(String)

---

### Test 5: Filter by commit (200)
**Purpose:** Verify filtering routes by a different commit value.

**Prompt:**
```
use contrast mcp to get the route coverage for the DemoRouteSession app filtered by commit 200
```

**Expected Result:** 2 routes returned (same as Sam)
- Test.acmeDummy.login(String)
- Test.bearKing.update(String)

---

### Test 6: Filter by repo (TS)
**Purpose:** Verify filtering routes by repo session metadata that spans multiple sessions.

**Prompt:**
```
use contrast mcp to get the route coverage for the DemoRouteSession app filtered by repo TS
```

**Expected Result:** 2 routes returned
- Test.acmeDummy.login(String)
- Test.bearKing.update(String)

---

### Test 7: Non-matching metadata value
**Purpose:** Verify filtering by a session metadata value that doesn't exist returns empty results.

**Prompt:**
```
use contrast mcp to get the route coverage for the DemoRouteSession app filtered by developer John
```

**Expected Result:** 0 routes returned (empty list with success message "No sessions found with the provided filters")

---

### Test 8: Session metadata value case insensitivity
**Purpose:** Verify that session metadata values are case-insensitive.

**Prompt:**
```
use contrast mcp to get the route coverage for the DemoRouteSession app filtered by developer ellen
```

**Expected Result:** 2 routes returned (same as Test 2) - values are case-insensitive, "ellen" matches "Ellen"
- Test.acmeDummy.login(String)
- Test.bearKing.create(String)

---

### Test 9: Session metadata field case insensitivity
**Purpose:** Verify that session metadata field names are case-insensitive.

**Prompt:**
```
use contrast mcp to get the route coverage for the DemoRouteSession app filtered by DEVELOPER Ellen
```

**Expected Result:** 2 routes returned (same as Test 2)
- Test.acmeDummy.login(String)
- Test.bearKing.create(String)

---

## Latest Session Filter Tests

### Test 10: Filter by latest session
**Purpose:** Verify filtering to only the latest session's routes.

**Prompt:**
```
use contrast mcp to get the route coverage for the DemoRouteSession app using the latest session only
```

**Expected Result:** 2 routes returned (Sam's session is latest)
- Test.acmeDummy.login(String) - observation: GET /login/test2
- Test.bearKing.update(String) - observation: POST /update/user

---

### Test 11: Latest session takes precedence over metadata filter
**Purpose:** Verify that useLatestSession takes precedence when both are provided.

**Prompt:**
```
use contrast mcp to get the route coverage for the DemoRouteSession app using the latest session, with developer Ellen
```

**Expected Result:** 2 routes returned from latest session (ignores developer filter)
- Test.acmeDummy.login(String)
- Test.bearKing.update(String)

Note: The tool description states "If both useLatestSession and session metadata are provided, useLatestSession takes precedence."

---

## Validation Error Tests

### Test 12: Missing appId
**Purpose:** Verify validation error for missing required appId parameter.

**Prompt:**
```
use contrast mcp to get route coverage without specifying an app
```

**Expected Result:** Validation error - "appId is required"

---

### Test 13: Unpaired sessionMetadataName (no value)
**Purpose:** Verify validation error when sessionMetadataName is provided without sessionMetadataValue.

**Prompt:**
```
use contrast mcp to get the route coverage for the DemoRouteSession app filtered by session metadata name "branch" only
```

**Expected Result:** Validation error - "sessionMetadataValue is required when sessionMetadataName is provided"

---

### Test 14: Unpaired sessionMetadataValue (no name)
**Purpose:** Verify validation error when sessionMetadataValue is provided without sessionMetadataName.

**Prompt:**
```
use contrast mcp to get the route coverage for the DemoRouteSession app filtered by session metadata value "main" only
```

**Expected Result:** Validation error - "sessionMetadataName is required when sessionMetadataValue is provided"

---

## Edge Case Tests

### Test 15: Invalid app ID
**Purpose:** Verify graceful handling of non-existent application ID.

**Prompt:**
```
use contrast mcp to get the route coverage for app ID "invalid-app-id-12345"
```

**Expected Result:** Either not found response or error - handled gracefully without crash

---

### Test 16: Non-existent metadata field name
**Purpose:** Verify graceful handling of non-existent session metadata field.

**Prompt:**
```
use contrast mcp to get the route coverage for the DemoRouteSession app filtered by nonexistent-field-xyz value "test"
```

**Expected Result:** 0 routes returned (empty list) - non-existent metadata returns no matching sessions

---

### Test 17: Empty string parameters treated as null
**Purpose:** Verify that empty string parameters are treated as null (unfiltered query).

**Prompt:**
```
use contrast mcp to get the route coverage for the DemoRouteSession app with empty session metadata name and value
```

**Expected Result:** 3 routes returned (all routes, same as unfiltered)
- Test.acmeDummy.login(String)
- Test.bearKing.update(String)
- Test.bearKing.create(String)

---

## Route Status Tests

### Test 18: Verify EXERCISED route properties
**Purpose:** Verify that exercised routes have correct status and timestamp.

**Prompt:**
```
use contrast mcp to get the route coverage for the DemoRouteSession app and show the exercised routes
```

**Expected Result:** The login route should show:
- status: EXERCISED
- exercised timestamp present (non-zero)
- discovered timestamp present
- Multiple observations (GET /login/test, GET /login/test1, GET /login/test2)

---

### Test 19: Verify DISCOVERED route properties
**Purpose:** Verify that discovered routes have correct status (not yet exercised).

**Prompt:**
```
use contrast mcp to get the route coverage for the DemoRouteSession app and show the discovered routes
```

**Expected Result:** The update and create routes should show:
- status: DISCOVERED
- exercised timestamp: 0 (not exercised)
- discovered timestamp present
- Single observation each

---

## Coverage Statistics Tests

### Test 20: Verify coverage percentage calculation
**Purpose:** Verify that coverage statistics are correctly calculated.

**Prompt:**
```
use contrast mcp to get the route coverage statistics for the DemoRouteSession app
```

**Expected Result:**
- totalRoutes: 3
- exercisedCount: 1
- discoveredCount: 2
- coveragePercent: 33.33

---

### Test 21: Session-filtered coverage statistics
**Purpose:** Verify coverage statistics when filtering by session metadata.

**Prompt:**
```
use contrast mcp to get the route coverage statistics for the DemoRouteSession app filtered by developer Ellen
```

**Expected Result:**
- totalRoutes: 2
- coveragePercent: 0.0 (session-scoped view doesn't aggregate overall status)

---

## Summary

| Test # | Category | Filter Type | Expected Route Count |
|--------|----------|-------------|---------------------|
| 1 | Unfiltered | None | 3 |
| 2 | Session Metadata | developer=Ellen | 2 |
| 3 | Session Metadata | developer=Sam | 2 |
| 4 | Session Metadata | commit=100 | 2 |
| 5 | Session Metadata | commit=200 | 2 |
| 6 | Session Metadata | repo=TS | 2 |
| 7 | Session Metadata | developer=John (non-matching) | 0 |
| 8 | Session Metadata | developer=ellen (case-insensitive) | 2 |
| 9 | Session Metadata | DEVELOPER=Ellen (field case) | 2 |
| 10 | Latest Session | useLatestSession=true | 2 |
| 11 | Precedence | latest + metadata | 2 |
| 12 | Validation | missing appId | error |
| 13 | Validation | unpaired name | error |
| 14 | Validation | unpaired value | error |
| 15 | Edge Case | invalid appId | error/not found |
| 16 | Edge Case | non-existent field | 0 |
| 17 | Edge Case | empty strings | 3 |
| 18 | Route Status | exercised routes | 1 |
| 19 | Route Status | discovered routes | 2 |
| 20 | Statistics | unfiltered stats | 3 total, 33.33% |
| 21 | Statistics | filtered stats | 2 total |
