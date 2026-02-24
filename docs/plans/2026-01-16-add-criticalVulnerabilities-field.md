# Add criticalVulnerabilities Field to list_application_libraries Response

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add `criticalVulnerabilities` field to `LibraryExtended` so users can distinguish CRITICAL from HIGH severity CVEs in library results.

**Architecture:** TeamServer returns `critical_vulnerabilities` in the library API response, but the MCP server's `LibraryExtended` class does not map this field. The fix adds the field with `@SerializedName` annotation and updates all related test infrastructure.

**Tech Stack:** Java, Gson serialization, JUnit 5, Mockito, AssertJ

---

## Task 1: Add criticalVulnerabilities Field to LibraryExtended

**Files:**
- Modify: `src/main/java/com/contrast/labs/ai/mcp/contrast/sdkextension/data/LibraryExtended.java:83-87`

**Step 1: Write the failing test**

Add to `LibraryExtendedTest.java`:

```java
@Test
void criticalVulnerabilities_should_be_settable_and_gettable() {
  var library = new LibraryExtended();
  library.setCriticalVulnerabilities(5);

  assertThat(library.getCriticalVulnerabilities()).isEqualTo(5);
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=LibraryExtendedTest#criticalVulnerabilities_should_be_settable_and_gettable -q`
Expected: FAIL with compile error "cannot find symbol: method setCriticalVulnerabilities"

**Step 3: Add the field to LibraryExtended**

Add after `highVulnerabilities` field (around line 87):

```java
@SerializedName("critical_vulnerabilities")
private int criticalVulnerabilities;
```

Note: `@Data` annotation auto-generates getter/setter.

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=LibraryExtendedTest#criticalVulnerabilities_should_be_settable_and_gettable -q`
Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/contrast/labs/ai/mcp/contrast/sdkextension/data/LibraryExtended.java
git add src/test/java/com/contrast/labs/ai/mcp/contrast/sdkextension/data/LibraryExtendedTest.java
git commit -m "$(cat <<'EOF'
feat: add criticalVulnerabilities field to LibraryExtended

TeamServer API returns critical_vulnerabilities in library response
but the MCP server was not mapping this field. Users can now
distinguish between CRITICAL and HIGH severity CVEs.
EOF
)"
```

---

## Task 2: Update AnonymousLibraryExtendedBuilder

**Files:**
- Modify: `src/test/java/com/contrast/labs/ai/mcp/contrast/AnonymousLibraryExtendedBuilder.java:48-55`
- Modify: `src/test/java/com/contrast/labs/ai/mcp/contrast/AnonymousLibraryExtendedBuilder.java:195-222`

**Step 1: Write the failing test**

Create test file:

```java
// src/test/java/com/contrast/labs/ai/mcp/contrast/AnonymousLibraryExtendedBuilderTest.java
package com.contrast.labs.ai.mcp.contrast;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnonymousLibraryExtendedBuilderTest {

  @Test
  void builder_should_support_criticalVulnerabilities() {
    var library = AnonymousLibraryExtendedBuilder.validLibrary()
        .withCriticalVulnerabilities(3)
        .build();

    assertThat(library.getCriticalVulnerabilities()).isEqualTo(3);
  }

  @Test
  void builder_should_default_criticalVulnerabilities_to_zero() {
    var library = AnonymousLibraryExtendedBuilder.validLibrary().build();

    assertThat(library.getCriticalVulnerabilities()).isEqualTo(0);
  }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=AnonymousLibraryExtendedBuilderTest -q`
Expected: FAIL with compile error "cannot find symbol: method withCriticalVulnerabilities"

**Step 3: Add field and builder method**

Add field with default (after `highVulnerabilities`, around line 48):

```java
private int criticalVulnerabilities = 0;
```

Add builder method (after `withHighVulnerabilities`, around line 158):

```java
public AnonymousLibraryExtendedBuilder withCriticalVulnerabilities(int criticalVulnerabilities) {
  this.criticalVulnerabilities = criticalVulnerabilities;
  return this;
}
```

Add stubbing in `build()` method (after `getHighVulnerabilities` stubbing, around line 214):

```java
lenient().when(library.getCriticalVulnerabilities()).thenReturn(criticalVulnerabilities);
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=AnonymousLibraryExtendedBuilderTest -q`
Expected: PASS

**Step 5: Commit**

```bash
git add src/test/java/com/contrast/labs/ai/mcp/contrast/AnonymousLibraryExtendedBuilder.java
git add src/test/java/com/contrast/labs/ai/mcp/contrast/AnonymousLibraryExtendedBuilderTest.java
git commit -m "$(cat <<'EOF'
test: add criticalVulnerabilities support to AnonymousLibraryExtendedBuilder
EOF
)"
```

---

## Task 3: Update Manual Test Documentation

**Files:**
- Modify: `manual-tests/list-application-libraries-manual-test.md`

**Step 1: Update field list**

Find the "Key response fields per library" section (around line 11-22) and add `criticalVulnerabilities`:

```markdown
**Key response fields per library:**
- `filename` - Library file name (e.g., "log4j-core-2.17.1.jar")
- `version` - Current version in use
- `hash` - Unique library identifier
- `classCount` - Total classes in the library
- `classesUsed` - Classes actually loaded by the application
- `totalVulnerabilities` - Total CVE count
- `criticalVulnerabilities` - CRITICAL severity CVE count only
- `highVulnerabilities` - HIGH severity CVE count only (does not include CRITICAL)
- `vulnerabilities` - List of CVE details
- `grade` - Security grade (A, B, C, D, F)
- `monthsOutdated` - Months since latest version
- `latestVersion` - Latest available version
```

**Step 2: Update sample vulnerable libraries table**

Find "Sample Vulnerable Libraries" section (around line 49-55) and add criticalVulnerabilities column:

```markdown
### Sample Vulnerable Libraries (from spring-petclinic-live-example)

| Filename | CVE Count | Critical CVEs | High CVEs | Key CVEs |
|----------|-----------|---------------|-----------|----------|
| tomcat-embed-core-10.1.12.jar | 16 | 7 | 9 | CVE-2024-50379, CVE-2025-24813, CVE-2024-56337 |
| spring-security-core-6.1.3.jar | 3 | 1 | 2 | CVE-2024-22257, CVE-2024-22234 |
| spring-security-crypto-6.1.3.jar | 1 | 0 | 1 | CVE-2025-22228 |
```

**Step 3: Update Test 5 expected results**

Update Test 5 (Verify vulnerability severity distribution) to include criticalVulnerabilities:

```markdown
**Expected Result:**
- tomcat-embed-core-10.1.12.jar vulnerabilities include:
  - CVE-2024-50379 (CRITICAL)
  - CVE-2025-31651 (CRITICAL)
  - CVE-2025-24813 (CRITICAL)
  - CVE-2024-56337 (CRITICAL)
  - Multiple HIGH severity CVEs
- `criticalVulnerabilities` counts CRITICAL severity CVEs only
- `highVulnerabilities` counts only HIGH severity CVEs (not CRITICAL)
- `totalVulnerabilities` = `criticalVulnerabilities` + `highVulnerabilities` + other severities
```

**Step 4: Update response structure example**

Find the "Successful Response" example (around line 638-671) and add `criticalVulnerabilities`:

```json
{
  "items": [
    {
      "filename": "tomcat-embed-core-10.1.39.jar",
      "version": "10.1.39",
      "hash": "f6acead04214d5aaea82c2639392208df33b3abe",
      "classCount": 1524,
      "classesUsed": 384,
      "grade": "F",
      "totalVulnerabilities": 6,
      "criticalVulnerabilities": 1,
      "highVulnerabilities": 5,
      ...
    }
  ],
  ...
}
```

**Step 5: Run format check**

Run: `make format`

**Step 6: Commit**

```bash
git add manual-tests/list-application-libraries-manual-test.md
git commit -m "$(cat <<'EOF'
docs: add criticalVulnerabilities field to list-application-libraries manual test
EOF
)"
```

---

## Task 4: Update Tool Description

**Files:**
- Modify: `src/main/java/com/contrast/labs/ai/mcp/contrast/tool/library/ListApplicationLibrariesTool.java:51-75`

**Step 1: Update the tool description**

Update the `@Tool` annotation description to include `criticalVulnerabilities`:

```java
@Tool(
    name = "list_application_libraries",
    description =
        """
        Returns all libraries used by a specific application.

        Use search_applications(name=...) to find the application ID from a name.

        Response includes for each library:
        - filename: Library file name (e.g., "log4j-core-2.17.1.jar")
        - version: Library version
        - hash: Unique library hash for identification
        - classCount: Total classes in the library
        - classesUsed: Number of classes actually loaded by the application
        - totalVulnerabilities: Total CVE count
        - criticalVulnerabilities: CRITICAL severity CVE count
        - highVulnerabilities: HIGH severity CVE count (not CRITICAL)
        - vulnerabilities: Known CVEs affecting this library version
        - grade: Library security grade (A-F)

        Note: If classesUsed is 0, the library is likely not actively used and may
        be a transitive dependency. Libraries with 0 class usage are unlikely to
        be exploitable even if they have known vulnerabilities.

        Related tools:
        - search_applications: Find application IDs by name, tag, or metadata
        - list_applications_by_cve: Find applications affected by a specific CVE
        """)
```

**Step 2: Run format check**

Run: `make format`

**Step 3: Commit**

```bash
git add src/main/java/com/contrast/labs/ai/mcp/contrast/tool/library/ListApplicationLibrariesTool.java
git commit -m "$(cat <<'EOF'
docs: update list_application_libraries tool description with criticalVulnerabilities
EOF
)"
```

---

## Task 5: Run All Tests and Verify

**Step 1: Run all unit tests**

Run: `make check-test`
Expected: All tests pass

**Step 2: Run integration tests (if credentials available)**

Run: `source .env.integration-test && mvn verify -q`
Expected: Integration tests pass, criticalVulnerabilities field populated from API

**Step 3: Final commit if any format changes**

```bash
git status
# If changes exist:
git add -A
git commit -m "style: format code"
```

---

## Summary

| Task | Description | Files |
|------|-------------|-------|
| 1 | Add field to LibraryExtended | LibraryExtended.java, LibraryExtendedTest.java |
| 2 | Update test builder | AnonymousLibraryExtendedBuilder.java, new test |
| 3 | Update manual test docs | list-application-libraries-manual-test.md |
| 4 | Update tool description | ListApplicationLibrariesTool.java |
| 5 | Verify all tests pass | Run make check-test |

**Total estimated commits:** 5
