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
package com.contrast.labs.ai.mcp.contrast.sdkexstension.data.adr;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AttacksFilterBody and its Builder pattern.
 */
class AttacksFilterBodyTest {

    // ========== Test: Default Values ==========

    @Test
    void testBuilder_DefaultValues_AreCorrect() {
        // When
        AttacksFilterBody filterBody = AttacksFilterBody.builder().build();

        // Then
        assertEquals("ALL", filterBody.getQuickFilter());
        assertEquals("", filterBody.getKeyword());
        assertFalse(filterBody.isIncludeSuppressed());
        assertFalse(filterBody.isIncludeBotBlockers());
        assertFalse(filterBody.isIncludeIpBlacklist());
        assertNotNull(filterBody.getTags());
        assertTrue(filterBody.getTags().isEmpty());
        assertNotNull(filterBody.getStatusFilter());
        assertTrue(filterBody.getStatusFilter().isEmpty());
        assertNotNull(filterBody.getProtectionRules());
        assertTrue(filterBody.getProtectionRules().isEmpty());
        assertNotNull(filterBody.getApplications());
        assertTrue(filterBody.getApplications().isEmpty());
        assertNotNull(filterBody.getApplicationImportances());
        assertTrue(filterBody.getApplicationImportances().isEmpty());
        assertNotNull(filterBody.getAttackers());
        assertTrue(filterBody.getAttackers().isEmpty());
        assertNotNull(filterBody.getServers());
        assertTrue(filterBody.getServers().isEmpty());
        assertNotNull(filterBody.getServerEnvironments());
        assertTrue(filterBody.getServerEnvironments().isEmpty());
        assertNotNull(filterBody.getSeverities());
        assertTrue(filterBody.getSeverities().isEmpty());
    }

    // ========== Test: String Fields ==========

    @Test
    void testBuilder_QuickFilter_SetsCorrectly() {
        // When
        AttacksFilterBody filterBody = AttacksFilterBody.builder()
                .quickFilter("PROBED")
                .build();

        // Then
        assertEquals("PROBED", filterBody.getQuickFilter());
    }

    @Test
    void testBuilder_Keyword_SetsCorrectly() {
        // When
        AttacksFilterBody filterBody = AttacksFilterBody.builder()
                .keyword("sql injection")
                .build();

        // Then
        assertEquals("sql injection", filterBody.getKeyword());
    }

    // ========== Test: Boolean Fields ==========

    @Test
    void testBuilder_IncludeSuppressed_SetsCorrectly() {
        // When
        AttacksFilterBody filterBody = AttacksFilterBody.builder()
                .includeSuppressed(true)
                .build();

        // Then
        assertTrue(filterBody.isIncludeSuppressed());
    }

    @Test
    void testBuilder_IncludeBotBlockers_SetsCorrectly() {
        // When
        AttacksFilterBody filterBody = AttacksFilterBody.builder()
                .includeBotBlockers(true)
                .build();

        // Then
        assertTrue(filterBody.isIncludeBotBlockers());
    }

    @Test
    void testBuilder_IncludeIpBlacklist_SetsCorrectly() {
        // When
        AttacksFilterBody filterBody = AttacksFilterBody.builder()
                .includeIpBlacklist(true)
                .build();

        // Then
        assertTrue(filterBody.isIncludeIpBlacklist());
    }

    // ========== Test: List Fields ==========

    @Test
    void testBuilder_Tags_SetsCorrectly() {
        // Given
        List<String> tags = Arrays.asList("tag1", "tag2");

        // When
        AttacksFilterBody filterBody = AttacksFilterBody.builder()
                .tags(tags)
                .build();

        // Then
        assertEquals(2, filterBody.getTags().size());
        assertEquals("tag1", filterBody.getTags().get(0));
        assertEquals("tag2", filterBody.getTags().get(1));
    }

    @Test
    void testBuilder_StatusFilter_SetsCorrectly() {
        // Given
        List<String> statuses = Arrays.asList("status1", "status2");

        // When
        AttacksFilterBody filterBody = AttacksFilterBody.builder()
                .statusFilter(statuses)
                .build();

        // Then
        assertEquals(2, filterBody.getStatusFilter().size());
        assertEquals("status1", filterBody.getStatusFilter().get(0));
    }

    @Test
    void testBuilder_ProtectionRules_SetsCorrectly() {
        // Given
        List<String> rules = Arrays.asList("rule1", "rule2");

        // When
        AttacksFilterBody filterBody = AttacksFilterBody.builder()
                .protectionRules(rules)
                .build();

        // Then
        assertEquals(2, filterBody.getProtectionRules().size());
        assertEquals("rule1", filterBody.getProtectionRules().get(0));
    }

    @Test
    void testBuilder_Applications_SetsCorrectly() {
        // Given
        List<String> apps = Arrays.asList("app1", "app2");

        // When
        AttacksFilterBody filterBody = AttacksFilterBody.builder()
                .applications(apps)
                .build();

        // Then
        assertEquals(2, filterBody.getApplications().size());
        assertEquals("app1", filterBody.getApplications().get(0));
    }

    @Test
    void testBuilder_ApplicationImportances_SetsCorrectly() {
        // Given
        List<String> importances = Arrays.asList("CRITICAL", "HIGH");

        // When
        AttacksFilterBody filterBody = AttacksFilterBody.builder()
                .applicationImportances(importances)
                .build();

        // Then
        assertEquals(2, filterBody.getApplicationImportances().size());
        assertEquals("CRITICAL", filterBody.getApplicationImportances().get(0));
    }

    @Test
    void testBuilder_Attackers_SetsCorrectly() {
        // Given
        List<String> attackers = Arrays.asList("10.0.0.1", "10.0.0.2");

        // When
        AttacksFilterBody filterBody = AttacksFilterBody.builder()
                .attackers(attackers)
                .build();

        // Then
        assertEquals(2, filterBody.getAttackers().size());
        assertEquals("10.0.0.1", filterBody.getAttackers().get(0));
    }

    @Test
    void testBuilder_Servers_SetsCorrectly() {
        // Given
        List<String> servers = Arrays.asList("server1", "server2");

        // When
        AttacksFilterBody filterBody = AttacksFilterBody.builder()
                .servers(servers)
                .build();

        // Then
        assertEquals(2, filterBody.getServers().size());
        assertEquals("server1", filterBody.getServers().get(0));
    }

    @Test
    void testBuilder_ServerEnvironments_SetsCorrectly() {
        // Given
        List<String> envs = Arrays.asList("PRODUCTION", "QA");

        // When
        AttacksFilterBody filterBody = AttacksFilterBody.builder()
                .serverEnvironments(envs)
                .build();

        // Then
        assertEquals(2, filterBody.getServerEnvironments().size());
        assertEquals("PRODUCTION", filterBody.getServerEnvironments().get(0));
    }

    @Test
    void testBuilder_Severities_SetsCorrectly() {
        // Given
        List<String> severities = Arrays.asList("HIGH", "CRITICAL");

        // When
        AttacksFilterBody filterBody = AttacksFilterBody.builder()
                .severities(severities)
                .build();

        // Then
        assertEquals(2, filterBody.getSeverities().size());
        assertEquals("HIGH", filterBody.getSeverities().get(0));
    }

    // ========== Test: Immutability ==========

    @Test
    void testBuilder_ReturnedLists_AreImmutable() {
        // Given
        List<String> tags = Arrays.asList("tag1", "tag2");
        AttacksFilterBody filterBody = AttacksFilterBody.builder()
                .tags(tags)
                .build();

        // When/Then - Should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> {
            filterBody.getTags().add("tag3");
        });
    }

    @Test
    void testBuilder_ModifyingSourceList_DoesNotAffectBuiltObject() {
        // Given
        List<String> tags = new ArrayList<>();
        tags.add("tag1");
        tags.add("tag2");

        // When
        AttacksFilterBody filterBody = AttacksFilterBody.builder()
                .tags(tags)
                .build();

        // Modify source list after building
        tags.add("tag3");

        // Then - Built object should not be affected
        assertEquals(2, filterBody.getTags().size());
        assertFalse(filterBody.getTags().contains("tag3"));
    }

    // ========== Test: Fluent API ==========

    @Test
    void testBuilder_FluentAPI_ChainsCorrectly() {
        // When
        AttacksFilterBody filterBody = AttacksFilterBody.builder()
                .quickFilter("EXPLOITED")
                .keyword("xss")
                .includeSuppressed(true)
                .includeBotBlockers(false)
                .includeIpBlacklist(true)
                .tags(Arrays.asList("tag1"))
                .statusFilter(Arrays.asList("status1"))
                .build();

        // Then
        assertEquals("EXPLOITED", filterBody.getQuickFilter());
        assertEquals("xss", filterBody.getKeyword());
        assertTrue(filterBody.isIncludeSuppressed());
        assertFalse(filterBody.isIncludeBotBlockers());
        assertTrue(filterBody.isIncludeIpBlacklist());
        assertEquals(1, filterBody.getTags().size());
        assertEquals(1, filterBody.getStatusFilter().size());
    }

    // ========== Test: Complex Scenarios ==========

    @Test
    void testBuilder_AllFieldsSet_BuildsCorrectly() {
        // When
        AttacksFilterBody filterBody = AttacksFilterBody.builder()
                .quickFilter("PROBED")
                .keyword("sql")
                .includeSuppressed(true)
                .includeBotBlockers(true)
                .includeIpBlacklist(true)
                .tags(Arrays.asList("tag1", "tag2"))
                .statusFilter(Arrays.asList("status1"))
                .protectionRules(Arrays.asList("rule1"))
                .applications(Arrays.asList("app1"))
                .applicationImportances(Arrays.asList("HIGH"))
                .attackers(Arrays.asList("10.0.0.1"))
                .servers(Arrays.asList("server1"))
                .serverEnvironments(Arrays.asList("PRODUCTION"))
                .severities(Arrays.asList("CRITICAL"))
                .build();

        // Then
        assertEquals("PROBED", filterBody.getQuickFilter());
        assertEquals("sql", filterBody.getKeyword());
        assertTrue(filterBody.isIncludeSuppressed());
        assertTrue(filterBody.isIncludeBotBlockers());
        assertTrue(filterBody.isIncludeIpBlacklist());
        assertEquals(2, filterBody.getTags().size());
        assertEquals(1, filterBody.getStatusFilter().size());
        assertEquals(1, filterBody.getProtectionRules().size());
        assertEquals(1, filterBody.getApplications().size());
        assertEquals(1, filterBody.getApplicationImportances().size());
        assertEquals(1, filterBody.getAttackers().size());
        assertEquals(1, filterBody.getServers().size());
        assertEquals(1, filterBody.getServerEnvironments().size());
        assertEquals(1, filterBody.getSeverities().size());
    }

    @Test
    void testBuilder_EmptyLists_BuildsCorrectly() {
        // When
        AttacksFilterBody filterBody = AttacksFilterBody.builder()
                .tags(new ArrayList<>())
                .statusFilter(new ArrayList<>())
                .build();

        // Then
        assertNotNull(filterBody.getTags());
        assertTrue(filterBody.getTags().isEmpty());
        assertNotNull(filterBody.getStatusFilter());
        assertTrue(filterBody.getStatusFilter().isEmpty());
    }
}
