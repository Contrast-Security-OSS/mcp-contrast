# Date Filter Bug Fix Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Fix HTTP 400 errors when using `lastSeenAfter` and `lastSeenBefore` date filters in `search_app_vulnerabilities` and `search_vulnerabilities` tools.

**Architecture:** The SDK's `TraceFilterBody` uses `java.util.Date` for startDate/endDate, but Gson's default serialization produces human-readable strings like "Jan 1, 2023" instead of epoch milliseconds the API expects. We'll add a custom `TypeAdapter<Date>` in `SDKExtension` that serializes dates as epoch milliseconds.

**Tech Stack:** Java 17, Gson, JUnit 5, AssertJ

---

## Task 1: Create DateAsEpochTypeAdapter

**Files:**
- Create: `src/main/java/com/contrast/labs/ai/mcp/contrast/sdkextension/DateAsEpochTypeAdapter.java`
- Test: `src/test/java/com/contrast/labs/ai/mcp/contrast/sdkextension/DateAsEpochTypeAdapterTest.java`

**Step 1: Write the failing test**

```java
/*
 * Copyright 2025 Contrast Security
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.contrast.labs.ai.mcp.contrast.sdkextension;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DateAsEpochTypeAdapterTest {

  private Gson gson;

  @BeforeEach
  void setUp() {
    gson = new GsonBuilder()
        .registerTypeAdapter(Date.class, new DateAsEpochTypeAdapter().nullSafe())
        .create();
  }

  @Test
  void write_should_serialize_date_as_epoch_milliseconds() {
    // Given: A date object representing January 1, 2023 00:00:00 UTC
    var date = new Date(1672531200000L);

    // When: Serialized to JSON
    var json = gson.toJson(date);

    // Then: Should be epoch milliseconds, not human-readable string
    assertThat(json).isEqualTo("1672531200000");
  }

  @Test
  void read_should_deserialize_epoch_milliseconds_to_date() {
    // Given: JSON with epoch milliseconds
    var json = "1672531200000";

    // When: Deserialized to Date
    var date = gson.fromJson(json, Date.class);

    // Then: Should be the correct date
    assertThat(date).isEqualTo(new Date(1672531200000L));
  }

  @Test
  void write_should_handle_null_date() {
    // Given: A wrapper class with null date (nullSafe adapter)
    record DateWrapper(Date date) {}
    var wrapper = new DateWrapper(null);

    // When: Serialized to JSON
    var json = gson.toJson(wrapper);

    // Then: Should produce null in JSON
    assertThat(json).isEqualTo("{\"date\":null}");
  }

  @Test
  void read_should_handle_null_value() {
    // Given: JSON with null date
    record DateWrapper(Date date) {}
    var json = "{\"date\":null}";

    // When: Deserialized
    var wrapper = gson.fromJson(json, DateWrapper.class);

    // Then: Date should be null
    assertThat(wrapper.date()).isNull();
  }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=DateAsEpochTypeAdapterTest -q`
Expected: FAIL with "cannot find symbol: class DateAsEpochTypeAdapter"

**Step 3: Write minimal implementation**

```java
/*
 * Copyright 2025 Contrast Security
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.contrast.labs.ai.mcp.contrast.sdkextension;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Date;

/**
 * Gson TypeAdapter that serializes/deserializes Date objects as epoch milliseconds.
 *
 * <p>The Contrast API expects dates in request bodies as epoch milliseconds (e.g., 1672531200000),
 * not Gson's default human-readable format (e.g., "Jan 1, 2023 12:00:00 AM").
 *
 * <p>Usage: Register with Gson using {@code .nullSafe()} wrapper for proper null handling:
 * <pre>{@code
 * Gson gson = new GsonBuilder()
 *     .registerTypeAdapter(Date.class, new DateAsEpochTypeAdapter().nullSafe())
 *     .create();
 * }</pre>
 */
public class DateAsEpochTypeAdapter extends TypeAdapter<Date> {

  @Override
  public void write(JsonWriter out, Date value) throws IOException {
    out.value(value.getTime());
  }

  @Override
  public Date read(JsonReader in) throws IOException {
    return new Date(in.nextLong());
  }
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=DateAsEpochTypeAdapterTest -q`
Expected: PASS - all 4 tests pass

**Step 5: Commit**

```bash
git add src/main/java/com/contrast/labs/ai/mcp/contrast/sdkextension/DateAsEpochTypeAdapter.java \
        src/test/java/com/contrast/labs/ai/mcp/contrast/sdkextension/DateAsEpochTypeAdapterTest.java
git commit -m "$(cat <<'EOF'
feat: add DateAsEpochTypeAdapter for Contrast API date serialization

The Contrast API expects dates as epoch milliseconds in JSON request
bodies, but Gson's default serialization produces human-readable strings
like "Jan 1, 2023". This adapter ensures dates are serialized correctly.
EOF
)"
```

---

## Task 2: Register DateAsEpochTypeAdapter in SDKExtension

**Files:**
- Modify: `src/main/java/com/contrast/labs/ai/mcp/contrast/sdkextension/SDKExtension.java:64`
- Test: `src/test/java/com/contrast/labs/ai/mcp/contrast/sdkextension/SDKExtensionDateSerializationTest.java`

**Step 1: Write the failing test**

```java
/*
 * Copyright 2025 Contrast Security
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.contrast.labs.ai.mcp.contrast.sdkextension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.contrastsecurity.models.TraceFilterBody;
import com.contrastsecurity.sdk.ContrastSDK;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SDKExtensionDateSerializationTest {

  private SDKExtension sdkExtension;

  @BeforeEach
  void setUp() {
    ContrastSDK mockSdk = mock();
    sdkExtension = new SDKExtension(mockSdk);
  }

  @Test
  void gson_should_serialize_dates_as_epoch_milliseconds() {
    // Given: A TraceFilterBody with date filters
    var filterBody = new TraceFilterBody();
    filterBody.setStartDate(new Date(1672531200000L)); // Jan 1, 2023 UTC
    filterBody.setEndDate(new Date(1704067200000L));   // Jan 1, 2024 UTC

    // When: Serialized using SDKExtension's Gson
    var json = sdkExtension.toJson(filterBody);

    // Then: Dates should be epoch milliseconds, not human-readable strings
    assertThat(json).contains("\"startDate\":1672531200000");
    assertThat(json).contains("\"endDate\":1704067200000");
    assertThat(json).doesNotContain("Jan");
    assertThat(json).doesNotContain("2023");
  }
}
```

**Step 2: Run test to verify it fails**

Run: `mvn test -Dtest=SDKExtensionDateSerializationTest -q`
Expected: FAIL - either "cannot find symbol: method toJson" or dates serialize as human-readable

**Step 3: Write minimal implementation**

Modify `SDKExtension.java` to:
1. Use GsonFactory.builder() instead of GsonFactory.create()
2. Register the DateAsEpochTypeAdapter
3. Add a `toJson()` method for testing (and general utility)

In `SDKExtension.java`, change line 64 from:
```java
this.gson = GsonFactory.create();
```

To:
```java
this.gson = GsonFactory.builder()
    .registerTypeAdapter(Date.class, new DateAsEpochTypeAdapter().nullSafe())
    .create();
```

Also add a package-private method at the end of the class for testing:
```java
/**
 * Serialize an object to JSON using the configured Gson instance.
 * Package-private for testing.
 */
String toJson(Object obj) {
  return gson.toJson(obj);
}
```

**Step 4: Run test to verify it passes**

Run: `mvn test -Dtest=SDKExtensionDateSerializationTest -q`
Expected: PASS

**Step 5: Commit**

```bash
git add src/main/java/com/contrast/labs/ai/mcp/contrast/sdkextension/SDKExtension.java \
        src/test/java/com/contrast/labs/ai/mcp/contrast/sdkextension/SDKExtensionDateSerializationTest.java
git commit -m "$(cat <<'EOF'
fix: register DateAsEpochTypeAdapter in SDKExtension

Dates in TraceFilterBody are now serialized as epoch milliseconds
instead of Gson's default human-readable format. This fixes HTTP 400
errors when using lastSeenAfter/lastSeenBefore date filters.
EOF
)"
```

---

## Task 3: Add import for Date class in SDKExtension

**Files:**
- Modify: `src/main/java/com/contrast/labs/ai/mcp/contrast/sdkextension/SDKExtension.java`

**Step 1: Verify compilation**

The import `java.util.Date` is likely already present from the getTraces method, but verify.

Run: `mvn compile -q`
Expected: PASS (if Date import exists) or FAIL with "cannot find symbol: class Date"

**Step 2: Add import if needed**

If compilation fails, add this import to SDKExtension.java:
```java
import java.util.Date;
```

**Step 3: Run full test suite**

Run: `mvn test -q`
Expected: PASS - all existing tests still pass

**Step 4: Commit (if import was added)**

```bash
git add src/main/java/com/contrast/labs/ai/mcp/contrast/sdkextension/SDKExtension.java
git commit -m "chore: add Date import to SDKExtension"
```

---

## Task 4: Verify format and run all tests

**Files:**
- All modified files

**Step 1: Run format check**

Run: `make format && make check-test`
Expected: PASS - code is formatted and all tests pass

**Step 2: Address any formatting issues**

If Spotless fails, it will auto-fix. Re-run:
```bash
make check-test
```

**Step 3: Commit formatting changes (if any)**

```bash
git add -u
git commit -m "style: apply Spotless formatting"
```

---

## Task 5: Run integration tests (if credentials available)

**Files:**
- None (verification only)

**Step 1: Check if integration test credentials exist**

Run: `test -f .env.integration-test && echo "Credentials available" || echo "No credentials"`

**Step 2: Run integration tests (if available)**

Run: `make verify`
Expected: PASS - date filter tests (21-25) should now pass

**Step 3: Document results**

If integration tests pass, note that in the PR. If credentials are unavailable, note that CI/CD will verify.

---

## Summary

| Task | Description | Files Modified |
|------|-------------|----------------|
| 1 | Create DateAsEpochTypeAdapter | New: DateAsEpochTypeAdapter.java, DateAsEpochTypeAdapterTest.java |
| 2 | Register adapter in SDKExtension | SDKExtension.java, SDKExtensionDateSerializationTest.java |
| 3 | Verify imports | SDKExtension.java (if needed) |
| 4 | Format and full test | All |
| 5 | Integration test | Verification only |

**Acceptance Criteria:**
- [ ] Date filters work correctly in `search_app_vulnerabilities`
- [ ] Date filters work correctly in `search_vulnerabilities`
- [ ] All 5 date filter tests (21-25 in manual tests) pass
- [ ] Unit test added for Date serialization (DateAsEpochTypeAdapterTest)
- [ ] Integration test for date filtering (covered by existing manual tests)
