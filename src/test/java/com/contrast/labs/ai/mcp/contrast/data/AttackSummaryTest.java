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

import com.contrast.labs.ai.mcp.contrast.FilterHelper;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.adr.Application;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.adr.Attack;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for AttackSummary, focusing on timestamp formatting consistency.
 */
class AttackSummaryTest {

    // Test timestamp: Jan 15, 2025 10:30:00 UTC
    private static final long TEST_TIMESTAMP = LocalDateTime.of(2025, 1, 15, 10, 30)
            .toInstant(ZoneOffset.UTC).toEpochMilli();

    @Test
    void testFromAttack_FormatsTimestampsWithFilterHelper() {
        // Given: A mock Attack object with known timestamps
        Attack attack = createMockAttack();

        // When: Creating AttackSummary from Attack
        AttackSummary summary = AttackSummary.fromAttack(attack);

        // Then: All timestamp strings should be in ISO 8601 format
        assertNotNull(summary.startTime(), "startTime should not be null");
        assertNotNull(summary.endTime(), "endTime should not be null");
        assertNotNull(summary.firstEventTime(), "firstEventTime should not be null");
        assertNotNull(summary.lastEventTime(), "lastEventTime should not be null");

        // Verify ISO 8601 format with timezone offset
        assertTrue(summary.startTime().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}"),
                "startTime should match ISO 8601 format: " + summary.startTime());
        assertTrue(summary.endTime().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}"),
                "endTime should match ISO 8601 format: " + summary.endTime());
        assertTrue(summary.firstEventTime().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}"),
                "firstEventTime should match ISO 8601 format: " + summary.firstEventTime());
        assertTrue(summary.lastEventTime().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}"),
                "lastEventTime should match ISO 8601 format: " + summary.lastEventTime());

        // Verify timestamps match expected FilterHelper output
        assertEquals(FilterHelper.formatTimestamp(attack.getStart_time()), summary.startTime(),
                "startTime should match FilterHelper.formatTimestamp output");
        assertEquals(FilterHelper.formatTimestamp(attack.getEnd_time()), summary.endTime(),
                "endTime should match FilterHelper.formatTimestamp output");
        assertEquals(FilterHelper.formatTimestamp(attack.getFirst_event_time()), summary.firstEventTime(),
                "firstEventTime should match FilterHelper.formatTimestamp output");
        assertEquals(FilterHelper.formatTimestamp(attack.getLast_event_time()), summary.lastEventTime(),
                "lastEventTime should match FilterHelper.formatTimestamp output");
    }

    @Test
    void testFromAttack_PreservesMillisecondTimestamps() {
        // Given: A mock Attack object
        Attack attack = createMockAttack();

        // When: Creating AttackSummary from Attack
        AttackSummary summary = AttackSummary.fromAttack(attack);

        // Then: Millisecond timestamps should be preserved exactly
        assertEquals(attack.getStart_time(), summary.startTimeMs(),
                "startTimeMs should preserve original millisecond value");
        assertEquals(attack.getEnd_time(), summary.endTimeMs(),
                "endTimeMs should preserve original millisecond value");
        assertEquals(attack.getFirst_event_time(), summary.firstEventTimeMs(),
                "firstEventTimeMs should preserve original millisecond value");
        assertEquals(attack.getLast_event_time(), summary.lastEventTimeMs(),
                "lastEventTimeMs should preserve original millisecond value");
    }

    @Test
    void testApplicationAttackInfo_FormatsTimestampsWithFilterHelper() {
        // Given: A mock Attack.ApplicationAttackInfo object
        Attack.ApplicationAttackInfo attackApp = createMockApplicationAttackInfo();

        // When: Creating ApplicationAttackInfo from Attack.ApplicationAttackInfo
        AttackSummary.ApplicationAttackInfo appInfo =
                AttackSummary.ApplicationAttackInfo.fromAttackApplication(attackApp);

        // Then: Timestamp strings should be in ISO 8601 format
        assertNotNull(appInfo.startTime(), "startTime should not be null");
        assertNotNull(appInfo.endTime(), "endTime should not be null");

        // Verify ISO 8601 format with timezone offset
        assertTrue(appInfo.startTime().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}"),
                "startTime should match ISO 8601 format: " + appInfo.startTime());
        assertTrue(appInfo.endTime().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}"),
                "endTime should match ISO 8601 format: " + appInfo.endTime());

        // Verify timestamps match expected FilterHelper output
        assertEquals(FilterHelper.formatTimestamp(attackApp.getStartTime()), appInfo.startTime(),
                "startTime should match FilterHelper.formatTimestamp output");
        assertEquals(FilterHelper.formatTimestamp(attackApp.getEndTime()), appInfo.endTime(),
                "endTime should match FilterHelper.formatTimestamp output");
    }

    @Test
    void testApplicationAttackInfo_PreservesMillisecondTimestamps() {
        // Given: A mock Attack.ApplicationAttackInfo object
        Attack.ApplicationAttackInfo attackApp = createMockApplicationAttackInfo();

        // When: Creating ApplicationAttackInfo
        AttackSummary.ApplicationAttackInfo appInfo =
                AttackSummary.ApplicationAttackInfo.fromAttackApplication(attackApp);

        // Then: Millisecond timestamps should be preserved exactly
        assertEquals(attackApp.getStartTime(), appInfo.startTimeMs(),
                "startTimeMs should preserve original millisecond value");
        assertEquals(attackApp.getEndTime(), appInfo.endTimeMs(),
                "endTimeMs should preserve original millisecond value");
    }

    @Test
    void testFromAttack_WithApplications_FormatsAllTimestamps() {
        // Given: Attack with application info
        Attack attack = createMockAttack();
        attack.setAttacksApplication(List.of(createMockApplicationAttackInfo()));

        // When: Creating AttackSummary
        AttackSummary summary = AttackSummary.fromAttack(attack);

        // Then: Attack timestamps should be formatted
        assertTrue(summary.startTime().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}"),
                "Attack startTime should match ISO 8601 format");

        // And: Application timestamps should also be formatted
        assertFalse(summary.applications().isEmpty(), "Should have application info");
        AttackSummary.ApplicationAttackInfo appInfo = summary.applications().get(0);
        assertTrue(appInfo.startTime().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}"),
                "Application startTime should match ISO 8601 format");
    }

    @Test
    void testTimestampFormat_ConsistentWithOtherMCPTools() {
        // Given: A mock Attack
        Attack attack = createMockAttack();

        // When: Creating AttackSummary
        AttackSummary summary = AttackSummary.fromAttack(attack);

        // Then: Format should be consistent with FilterHelper (same as VulnLight, ApplicationData, etc.)
        String expectedFormat = FilterHelper.formatTimestamp(TEST_TIMESTAMP);

        // Verify the format matches the pattern used throughout the codebase
        assertTrue(summary.startTime().matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}"),
                "Timestamp format should be consistent with other MCP tools");

        // Should NOT match old Date.toString() format (e.g., "Thu Jan 15 10:30:00 EST 2025")
        assertFalse(summary.startTime().matches("\\w{3} \\w{3} \\d{2} \\d{2}:\\d{2}:\\d{2} \\w+ \\d{4}"),
                "Should NOT use legacy Date.toString() format");
    }

    // ========== Helper Methods ==========

    private Attack createMockAttack() {
        Attack attack = new Attack();
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
        Attack.ApplicationAttackInfo attackApp = new Attack.ApplicationAttackInfo();

        Application app = new Application();
        app.setApp_id("test-app-id");
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
