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
package com.contrast.labs.ai.mcp.contrast.data;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrast.labs.ai.mcp.contrast.FilterHelper;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr.Application;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr.Attack;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Test suite for AttackSummary, focusing on timestamp formatting consistency. */
class AttackSummaryTest {

  // Test timestamp: Jan 15, 2025 10:30:00 UTC
  private static final long TEST_TIMESTAMP =
      LocalDateTime.of(2025, 1, 15, 10, 30).toInstant(ZoneOffset.UTC).toEpochMilli();

  @Test
  void testFromAttack_FormatsTimestampsWithFilterHelper() {
    // Given: A mock Attack object with known timestamps
    var attack = createMockAttack();

    // When: Creating AttackSummary from Attack
    var summary = AttackSummary.fromAttack(attack);

    // Then: All timestamp strings should be in ISO 8601 format
    assertThat(summary.startTime()).as("startTime should not be null").isNotNull();
    assertThat(summary.endTime()).as("endTime should not be null").isNotNull();
    assertThat(summary.firstEventTime()).as("firstEventTime should not be null").isNotNull();
    assertThat(summary.lastEventTime()).as("lastEventTime should not be null").isNotNull();

    // Verify ISO 8601 format with timezone offset
    assertThat(summary.startTime())
        .as("startTime should match ISO 8601 format: " + summary.startTime())
        .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}");
    assertThat(summary.endTime())
        .as("endTime should match ISO 8601 format: " + summary.endTime())
        .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}");
    assertThat(summary.firstEventTime())
        .as("firstEventTime should match ISO 8601 format: " + summary.firstEventTime())
        .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}");
    assertThat(summary.lastEventTime())
        .as("lastEventTime should match ISO 8601 format: " + summary.lastEventTime())
        .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}");

    // Verify timestamps match expected FilterHelper output
    assertThat(summary.startTime())
        .as("startTime should match FilterHelper.formatTimestamp output")
        .isEqualTo(FilterHelper.formatTimestamp(attack.getStart_time()));
    assertThat(summary.endTime())
        .as("endTime should match FilterHelper.formatTimestamp output")
        .isEqualTo(FilterHelper.formatTimestamp(attack.getEnd_time()));
    assertThat(summary.firstEventTime())
        .as("firstEventTime should match FilterHelper.formatTimestamp output")
        .isEqualTo(FilterHelper.formatTimestamp(attack.getFirst_event_time()));
    assertThat(summary.lastEventTime())
        .as("lastEventTime should match FilterHelper.formatTimestamp output")
        .isEqualTo(FilterHelper.formatTimestamp(attack.getLast_event_time()));
  }

  @Test
  void testFromAttack_PreservesMillisecondTimestamps() {
    // Given: A mock Attack object
    var attack = createMockAttack();

    // When: Creating AttackSummary from Attack
    var summary = AttackSummary.fromAttack(attack);

    // Then: Millisecond timestamps should be preserved exactly
    assertThat(summary.startTimeMs())
        .as("startTimeMs should preserve original millisecond value")
        .isEqualTo(attack.getStart_time());
    assertThat(summary.endTimeMs())
        .as("endTimeMs should preserve original millisecond value")
        .isEqualTo(attack.getEnd_time());
    assertThat(summary.firstEventTimeMs())
        .as("firstEventTimeMs should preserve original millisecond value")
        .isEqualTo(attack.getFirst_event_time());
    assertThat(summary.lastEventTimeMs())
        .as("lastEventTimeMs should preserve original millisecond value")
        .isEqualTo(attack.getLast_event_time());
  }

  @Test
  void testApplicationAttackInfo_FormatsTimestampsWithFilterHelper() {
    // Given: A mock Attack.ApplicationAttackInfo object
    var attackApp = createMockApplicationAttackInfo();

    // When: Creating ApplicationAttackInfo from Attack.ApplicationAttackInfo
    var appInfo = AttackSummary.ApplicationAttackInfo.fromAttackApplication(attackApp);

    // Then: Timestamp strings should be in ISO 8601 format
    assertThat(appInfo.startTime()).as("startTime should not be null").isNotNull();
    assertThat(appInfo.endTime()).as("endTime should not be null").isNotNull();

    // Verify ISO 8601 format with timezone offset
    assertThat(appInfo.startTime())
        .as("startTime should match ISO 8601 format: " + appInfo.startTime())
        .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}");
    assertThat(appInfo.endTime())
        .as("endTime should match ISO 8601 format: " + appInfo.endTime())
        .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}");

    // Verify timestamps match expected FilterHelper output
    assertThat(appInfo.startTime())
        .as("startTime should match FilterHelper.formatTimestamp output")
        .isEqualTo(FilterHelper.formatTimestamp(attackApp.getStartTime()));
    assertThat(appInfo.endTime())
        .as("endTime should match FilterHelper.formatTimestamp output")
        .isEqualTo(FilterHelper.formatTimestamp(attackApp.getEndTime()));
  }

  @Test
  void testApplicationAttackInfo_PreservesMillisecondTimestamps() {
    // Given: A mock Attack.ApplicationAttackInfo object
    var attackApp = createMockApplicationAttackInfo();

    // When: Creating ApplicationAttackInfo
    var appInfo = AttackSummary.ApplicationAttackInfo.fromAttackApplication(attackApp);

    // Then: Millisecond timestamps should be preserved exactly
    assertThat(appInfo.startTimeMs())
        .as("startTimeMs should preserve original millisecond value")
        .isEqualTo(attackApp.getStartTime());
    assertThat(appInfo.endTimeMs())
        .as("endTimeMs should preserve original millisecond value")
        .isEqualTo(attackApp.getEndTime());
  }

  @Test
  void testFromAttack_WithApplications_FormatsAllTimestamps() {
    // Given: Attack with application info
    var attack = createMockAttack();
    attack.setAttacksApplication(List.of(createMockApplicationAttackInfo()));

    // When: Creating AttackSummary
    var summary = AttackSummary.fromAttack(attack);

    // Then: Attack timestamps should be formatted
    assertThat(
            summary
                .startTime()
                .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}"))
        .as("Attack startTime should match ISO 8601 format")
        .isTrue();

    // And: Application timestamps should also be formatted
    assertThat(summary.applications().isEmpty()).as("Should have application info").isFalse();
    var appInfo = summary.applications().get(0);
    assertThat(
            appInfo
                .startTime()
                .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}"))
        .as("Application startTime should match ISO 8601 format")
        .isTrue();
  }

  @Test
  void testTimestampFormat_ConsistentWithOtherMCPTools() {
    // Given: A mock Attack
    var attack = createMockAttack();

    // When: Creating AttackSummary
    var summary = AttackSummary.fromAttack(attack);

    // Then: Format should be consistent with FilterHelper (same as VulnLight, ApplicationData,
    // etc.)
    var expectedFormat = FilterHelper.formatTimestamp(TEST_TIMESTAMP);

    // Verify the format matches the pattern used throughout the codebase
    assertThat(
            summary
                .startTime()
                .matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}"))
        .as("Timestamp format should be consistent with other MCP tools")
        .isTrue();

    // Should NOT match old Date.toString() format (e.g., "Thu Jan 15 10:30:00 EST 2025")
    assertThat(summary.startTime().matches("\\w{3} \\w{3} \\d{2} \\d{2}:\\d{2}:\\d{2} \\w+ \\d{4}"))
        .as("Should NOT use legacy Date.toString() format")
        .isFalse();
  }

  // ========== Helper Methods ==========

  private Attack createMockAttack() {
    var attack = new Attack();
    attack.setUuid("test-attack-uuid");
    attack.setStatus("PROBED");
    attack.setSource("192.168.1.100");
    attack.setRules(List.of("sql-injection", "xss-reflected"));
    attack.setProbes(10);
    attack.setStart_time(TEST_TIMESTAMP);
    attack.setEnd_time(TEST_TIMESTAMP + 5000);
    attack.setFirst_event_time(TEST_TIMESTAMP + 1000);
    attack.setLast_event_time(TEST_TIMESTAMP + 4000);
    attack.setAttacksApplication(List.of());
    return attack;
  }

  private Attack.ApplicationAttackInfo createMockApplicationAttackInfo() {
    var attackApp = new Attack.ApplicationAttackInfo();

    var app = new Application();
    app.setAppId("test-app-id");
    app.setName("Test Application");
    app.setLanguage("Java");
    attackApp.setApplication(app);

    attackApp.setSeverity("CRITICAL");
    attackApp.setStatus("EXPLOITED");
    attackApp.setStartTime(TEST_TIMESTAMP);
    attackApp.setEndTime(TEST_TIMESTAMP + 3000);

    return attackApp;
  }
}
