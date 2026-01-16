# Test Cases for `search_applications` Tool

## Known Test Data

### Test Applications

**1. Adam Webgoat Route Session Metadata Test**
| Field | Value |
|-------|-------|
| App ID | b7323a57-f776-45fe-962f-2ac2ba5f684d |
| Status | offline |
| Language | Java |
| Tags | AppTag xEi, AppTag vIT, doing_a_thing, AppTag gz2 |
| Metadata | (none) |
| Technologies | Backbone, JSP, J2EE, Applet, jQuery, HTML5, Bootstrap |

**2. Terracotta Bank**
| Field | Value |
|-------|-------|
| App ID | 67c28e5e-c5ac-4491-8bc9-946f50292b71 |
| Status | offline |
| Language | Java |
| Tags | dsadsads, app-test-tag, abc, PSSA, app-other-tag, abc123, sacumentest, WebApi_AppTags, 悪いアプリケーション |
| Metadata | freeform = David Archer |
| Technologies | Spring MVC, JSP, netty, HTML5, J2EE |

**3. DemoRouteSession**
| Field | Value |
|-------|-------|
| App ID | 9e18a607-2b01-41c7-b35b-52a256840fea |
| Status | offline |
| Language | Java |
| Tags | AppTag LST, AppTag l1Q, ... (100+ tags) |
| Metadata | (none - session metadata only) |
| Technologies | (none) |

**4. webgoat-ty-test**
| Field | Value |
|-------|-------|
| App ID | b9b1396c-5914-4cf9-8138-40c5861d0006 |
| Status | offline |
| Language | Java |
| Tags | (none) |
| Metadata | (none) |
| Technologies | Spring MVC, ECS, JSP, Backbone, J2EE, Applet, jQuery, HTML5, Bootstrap |

### Shared Tags
- `doing_a_thing`: @contrast/cus-test-server (1 app)
- `app-test-tag`: Terracotta Bank + other apps (11 apps)

### Apps with Metadata
- `freeform = David Archer`: Terracotta Bank only
- Note: Filtering by metadata field name only (any value) is not supported

---

## Name Filter Tests

### Test 1: Exact name match
**Purpose:** Verify finding an application by exact name.

**Prompt:**
```
use contrast mcp to search for applications named "Terracotta Bank"
```

**Expected Result:** 2 applications returned (partial match)
- Terracotta Bank
- **Terracotta Bank (1)

---

### Test 2: Partial name match
**Purpose:** Verify partial, case-insensitive name matching.

**Prompt:**
```
use contrast mcp to search for applications with "webgoat" in the name
```

**Expected Result:** 118 applications returned (many webgoat variations)
- Includes: webgoat-ty-test, WebGoat_service27, WebgoatStaging, etc.
- hasMorePages: true (with default page size of 50)

---

### Test 3: Name filter case insensitivity
**Purpose:** Verify name filter is case-insensitive.

**Prompt:**
```
use contrast mcp to search for applications named "WEBGOAT"
```

**Expected Result:** Same 118 applications as Test 2
- Case doesn't matter: "WEBGOAT" matches "webgoat", "WebGoat", "Webgoat"

---

### Test 4: Name filter case insensitivity (lowercase)
**Purpose:** Verify lowercase name filter matches mixed-case app names.

**Prompt:**
```
use contrast mcp to search for applications named "demoroutesession"
```

**Expected Result:** 1 application returned
- DemoRouteSession (appId: 9e18a607-2b01-41c7-b35b-52a256840fea)

---

### Test 5: Non-matching name filter
**Purpose:** Verify non-existent application name returns empty results.

**Prompt:**
```
use contrast mcp to search for applications named "NonExistentApp12345"
```

**Expected Result:** 0 applications returned with warning "No results found matching the specified criteria."

---

### Test 6: Partial name in middle
**Purpose:** Verify partial matching works for substrings in the middle of names.

**Prompt:**
```
use contrast mcp to search for applications with "Route" in the name
```

**Expected Result:** 15+ applications including:
- DemoRouteSession
- Multiple servlet-route-coverage-* applications
- spring-petclinic-routes-test-*
- Note: Also matches apps with "route" in tags/metadata

---

## Tag Filter Tests

### Test 7: Tag filter (matching)
**Purpose:** Verify exact tag matching.

**Prompt:**
```
use contrast mcp to search for applications with tag "doing_a_thing"
```

**Expected Result:** 1 application returned
- @contrast/cus-test-server

---

### Test 8: Tag filter case insensitivity
**Purpose:** Verify tag filter is case-insensitive.

**Prompt:**
```
use contrast mcp to search for applications with tag "DOING_A_THING"
```

**Expected Result:** 1 application returned
- @contrast/cus-test-server
- Tags are case-insensitive: "DOING_A_THING" matches "doing_a_thing"

---

### Test 9: Tag filter case insensitivity (mixed case)
**Purpose:** Verify tag filter is case-insensitive with mixed case.

**Prompt:**
```
use contrast mcp to search for applications with tag "Doing_A_Thing"
```

**Expected Result:** 1 application returned
- @contrast/cus-test-server (matches "doing_a_thing")

---

### Test 10: Tag filter with common tag
**Purpose:** Verify finding multiple applications with a shared tag.

**Prompt:**
```
use contrast mcp to search for applications with tag "app-test-tag"
```

**Expected Result:** 11 applications returned
- Includes: Terracotta Bank, java-dummy-app-40, java-dummy-app-4, and others

---

### Test 11: Tag filter (non-matching)
**Purpose:** Verify non-existent tag returns empty results.

**Prompt:**
```
use contrast mcp to search for applications with tag "nonexistent-tag-xyz"
```

**Expected Result:** 0 applications returned

---

### Test 12: Tag with special characters
**Purpose:** Verify tag matching with special characters.

**Prompt:**
```
use contrast mcp to search for applications with tag "悪いアプリケーション"
```

**Expected Result:** Multiple applications returned including Terracotta Bank

---

## Metadata Filter Tests

### Test 13: Metadata name only (any value) - NOT SUPPORTED
**Purpose:** Verify that filtering by metadata field name only (without a value) is not supported.

**Prompt:**
```
use contrast mcp to search for applications that have metadata field "freeform"
```

**Expected Result:** Error or validation failure
- Filtering by metadata field name only (any value) is not supported
- Must provide at least one value to filter by
- Use `metadataFilters={"freeform":"some_value"}` instead

---

### Test 14: Metadata name + value (exact match)
**Purpose:** Verify filtering by both metadata name and value.

**Prompt:**
```
use contrast mcp to search for applications with metadata freeform="David Archer"
```

**Expected Result:** 1 application returned
- Terracotta Bank (appId: 67c28e5e-c5ac-4491-8bc9-946f50292b71)

---

### Test 15: Metadata name case insensitivity
**Purpose:** Verify metadata field name is case-insensitive.

**Prompt:**
```
use contrast mcp to search for applications with metadata FREEFORM="David Archer"
```

**Expected Result:** 1 application returned
- Terracotta Bank

---

### Test 16: Metadata value case insensitivity
**Purpose:** Verify metadata value is case-insensitive.

**Prompt:**
```
use contrast mcp to search for applications with metadata freeform="david archer"
```

**Expected Result:** 1 application returned
- Terracotta Bank (case-insensitive value matching)

---

### Test 17: Metadata name + value (non-matching value)
**Purpose:** Verify non-matching metadata value returns empty results.

**Prompt:**
```
use contrast mcp to search for applications with metadata freeform="NonExistentValue"
```

**Expected Result:** 0 applications returned

---

### Test 18: Metadata non-existent field
**Purpose:** Verify non-existent metadata field returns empty results.

**Prompt:**
```
use contrast mcp to search for applications with metadata field "nonexistent-field-xyz"
```

**Expected Result:** 0 applications returned

---

## Pagination Tests

### Test 19: Default pagination
**Purpose:** Verify default pagination behavior (page 1, pageSize 50).

**Prompt:**
```
use contrast mcp to search for applications with "webgoat" in the name
```

**Expected Result:**
- 50 applications returned (first page)
- totalItems: 118
- hasMorePages: true
- page: 1
- pageSize: 50

---

### Test 20: Custom page size
**Purpose:** Verify custom page size works correctly.

**Prompt:**
```
use contrast mcp to search for applications with "webgoat" in the name, page size 5
```

**Expected Result:**
- 5 applications returned
- totalItems: 118
- hasMorePages: true
- First 5 apps (e.g., WithCustomBuildpack, WebGoat_service27, WebGoat_service2, WebgoatStaging, WebgoatProtect)

---

### Test 21: Second page
**Purpose:** Verify retrieving second page of results.

**Prompt:**
```
use contrast mcp to search for applications with "webgoat" in the name, page 2, page size 5
```

**Expected Result:**
- 5 applications returned (different from page 1)
- page: 2
- hasMorePages: true
- Next 5 apps (e.g., WebGoatInternDemo, WebGoatDocker2, WebGoatDocker, WebGoatDemo, WebGoat81)

---

### Test 22: Page beyond results
**Purpose:** Verify requesting page beyond available data returns empty.

**Prompt:**
```
use contrast mcp to search for applications named "DemoRouteSession", page 2
```

**Expected Result:**
- 0 applications returned (only 1 result total, so page 2 is empty)
- hasMorePages: false

---

## Combined Filter Tests

### Test 23: Name + Tag combined
**Purpose:** Verify combining name and tag filters (AND logic).

**Prompt:**
```
use contrast mcp to search for applications with "webgoat" in the name and tag "doing_a_thing"
```

**Expected Result:** 1 application returned
- @contrast/cus-test-server
- Note: Name filter searches displayName, contextPath, tags, and metadata values

---

### Test 24: Name + Metadata combined
**Purpose:** Verify combining name and metadata filters.

**Prompt:**
```
use contrast mcp to search for applications with "Terracotta" in the name and metadata freeform="David Archer"
```

**Expected Result:** 1 application returned
- Terracotta Bank

---

### Test 25: Multiple non-matching filters
**Purpose:** Verify combined filters that produce no results.

**Prompt:**
```
use contrast mcp to search for applications with "webgoat" in the name and metadata freeform="David Archer"
```

**Expected Result:** 0 applications returned
- No webgoat apps have freeform metadata with "David Archer"

---

## Edge Case Tests

### Test 26: No filters (all applications)
**Purpose:** Verify retrieving all applications without any filters.

**Prompt:**
```
use contrast mcp to list all applications
```

**Expected Result:** Returns first page of all applications
- Many applications returned (up to pageSize)
- hasMorePages: likely true
- Includes variety of apps across all languages/statuses

---

### Test 27: Empty string filters treated as no filter
**Purpose:** Verify empty string parameters are treated as null.

**Prompt:**
```
use contrast mcp to search for applications with empty name filter
```

**Expected Result:** Same as Test 26 (all applications returned)

---

### Test 28: Application fields verification
**Purpose:** Verify returned application data contains all expected fields.

**Prompt:**
```
use contrast mcp to search for application "webgoat-ty-test"
```

**Expected Result:** 1 application with all fields populated:
```json
{
  "name": "webgoat-ty-test",
  "status": "offline",
  "appID": "b9b1396c-5914-4cf9-8138-40c5861d0006",
  "lastSeenAt": "2024-10-07T14:28:00-04:00",
  "language": "Java",
  "metadata": [],
  "tags": [],
  "technologies": ["Spring MVC", "ECS", "JSP", "Backbone", "J2EE", "Applet", "jQuery", "HTML5", "Bootstrap"]
}
```

---

### Test 29: Application with metadata verification
**Purpose:** Verify application metadata is correctly returned.

**Prompt:**
```
use contrast mcp to search for application "Terracotta Bank" and show its metadata
```

**Expected Result:** Application with metadata:
```json
{
  "name": "Terracotta Bank",
  "appID": "67c28e5e-c5ac-4491-8bc9-946f50292b71",
  "metadata": [
    {"name": "freeform", "value": "David Archer"}
  ]
}
```

---

## Filtering Behavior Summary

| Filter | Matching Type | Case Sensitivity |
|--------|---------------|------------------|
| name | Partial | Case-INSENSITIVE |
| tag | Exact | Case-INSENSITIVE |
| metadataName | Exact | Case-INSENSITIVE |
| metadataValue | Exact | Case-INSENSITIVE |

---

## Summary

| Test # | Category | Filter | Expected Result |
|--------|----------|--------|-----------------|
| 1 | Name | exact match | 2 apps |
| 2 | Name | partial match | 118 apps |
| 3 | Name | case insensitive (upper) | 118 apps |
| 4 | Name | case insensitive (lower) | 1 app |
| 5 | Name | non-matching | 0 apps |
| 6 | Name | partial middle | 15+ apps |
| 7 | Tag | exact match | 1 app |
| 8 | Tag | case insensitive (UPPER) | 1 app |
| 9 | Tag | case insensitive (Mixed) | 1 app |
| 10 | Tag | common tag | 11 apps |
| 11 | Tag | non-matching | 0 apps |
| 12 | Tag | special characters | multiple apps |
| 13 | Metadata | name only | 0 apps (not supported) |
| 14 | Metadata | name + value | 1 app |
| 15 | Metadata | name case insensitive | 1 app |
| 16 | Metadata | value case insensitive | 1 app |
| 17 | Metadata | non-matching value | 0 apps |
| 18 | Metadata | non-existent field | 0 apps |
| 19 | Pagination | default | 50 apps, hasMore=true |
| 20 | Pagination | custom size | 5 apps |
| 21 | Pagination | page 2 | 5 different apps |
| 22 | Pagination | beyond results | 0 apps |
| 23 | Combined | name + tag | 1 app |
| 24 | Combined | name + metadata | 1 app |
| 25 | Combined | non-matching combo | 0 apps |
| 26 | Edge Case | no filters | all apps |
| 27 | Edge Case | empty strings | all apps |
| 28 | Edge Case | field verification | all fields |
| 29 | Edge Case | metadata verification | metadata present |
