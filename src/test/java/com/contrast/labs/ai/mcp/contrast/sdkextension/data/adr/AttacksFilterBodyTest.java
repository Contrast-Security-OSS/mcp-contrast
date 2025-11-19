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
package com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/** Tests for AttacksFilterBody and its Builder pattern. */
class AttacksFilterBodyTest {

  // ========== Test: Default Values ==========

  @Test
  void testBuilder_DefaultValues_AreCorrect() {
    // When
    var filterBody = AttacksFilterBody.builder().build();

    // Then
    assertThat(filterBody.getQuickFilter()).isEqualTo("ALL");
    assertThat(filterBody.getKeyword()).isEqualTo("");
    assertThat(filterBody.isIncludeSuppressed()).isFalse();
    assertThat(filterBody.isIncludeBotBlockers()).isFalse();
    assertThat(filterBody.isIncludeIpBlacklist()).isFalse();
    assertThat(filterBody.getTags()).isNotNull().isEmpty();
    assertThat(filterBody.getStatusFilter()).isNotNull().isEmpty();
    assertThat(filterBody.getProtectionRules()).isNotNull().isEmpty();
    assertThat(filterBody.getApplications()).isNotNull().isEmpty();
    assertThat(filterBody.getApplicationImportances()).isNotNull().isEmpty();
    assertThat(filterBody.getAttackers()).isNotNull().isEmpty();
    assertThat(filterBody.getServers()).isNotNull().isEmpty();
    assertThat(filterBody.getServerEnvironments()).isNotNull().isEmpty();
    assertThat(filterBody.getSeverities()).isNotNull().isEmpty();
  }

  // ========== Test: String Fields ==========

  @Test
  void testBuilder_QuickFilter_SetsCorrectly() {
    // When
    var filterBody = AttacksFilterBody.builder().quickFilter("ACTIVE").build();

    // Then
    assertThat(filterBody.getQuickFilter()).isEqualTo("ACTIVE");
  }

  @Test
  void testBuilder_Keyword_SetsCorrectly() {
    // When
    var filterBody = AttacksFilterBody.builder().keyword("sql injection").build();

    // Then
    assertThat(filterBody.getKeyword()).isEqualTo("sql injection");
  }

  // ========== Test: Boolean Fields ==========

  @Test
  void testBuilder_IncludeSuppressed_SetsCorrectly() {
    // When
    var filterBody = AttacksFilterBody.builder().includeSuppressed(true).build();

    // Then
    assertThat(filterBody.isIncludeSuppressed()).isTrue();
  }

  @Test
  void testBuilder_IncludeBotBlockers_SetsCorrectly() {
    // When
    var filterBody = AttacksFilterBody.builder().includeBotBlockers(true).build();

    // Then
    assertThat(filterBody.isIncludeBotBlockers()).isTrue();
  }

  @Test
  void testBuilder_IncludeIpBlacklist_SetsCorrectly() {
    // When
    var filterBody = AttacksFilterBody.builder().includeIpBlacklist(true).build();

    // Then
    assertThat(filterBody.isIncludeIpBlacklist()).isTrue();
  }

  // ========== Test: List Fields ==========

  @Test
  void testBuilder_Tags_SetsCorrectly() {
    // Given
    var tags = Arrays.asList("tag1", "tag2");

    // When
    var filterBody = AttacksFilterBody.builder().tags(tags).build();

    // Then
    assertThat(filterBody.getTags()).hasSize(2);
    assertThat(filterBody.getTags().get(0)).isEqualTo("tag1");
    assertThat(filterBody.getTags().get(1)).isEqualTo("tag2");
  }

  @Test
  void testBuilder_StatusFilter_SetsCorrectly() {
    // Given
    var statuses = Arrays.asList("status1", "status2");

    // When
    var filterBody = AttacksFilterBody.builder().statusFilter(statuses).build();

    // Then
    assertThat(filterBody.getStatusFilter()).hasSize(2);
    assertThat(filterBody.getStatusFilter().get(0)).isEqualTo("status1");
  }

  @Test
  void testBuilder_ProtectionRules_SetsCorrectly() {
    // Given
    var rules = Arrays.asList("rule1", "rule2");

    // When
    var filterBody = AttacksFilterBody.builder().protectionRules(rules).build();

    // Then
    assertThat(filterBody.getProtectionRules()).hasSize(2);
    assertThat(filterBody.getProtectionRules().get(0)).isEqualTo("rule1");
  }

  @Test
  void testBuilder_Applications_SetsCorrectly() {
    // Given
    var apps = Arrays.asList("app1", "app2");

    // When
    var filterBody = AttacksFilterBody.builder().applications(apps).build();

    // Then
    assertThat(filterBody.getApplications()).hasSize(2);
    assertThat(filterBody.getApplications().get(0)).isEqualTo("app1");
  }

  @Test
  void testBuilder_ApplicationImportances_SetsCorrectly() {
    // Given
    var importances = Arrays.asList("CRITICAL", "HIGH");

    // When
    var filterBody = AttacksFilterBody.builder().applicationImportances(importances).build();

    // Then
    assertThat(filterBody.getApplicationImportances()).hasSize(2);
    assertThat(filterBody.getApplicationImportances().get(0)).isEqualTo("CRITICAL");
  }

  @Test
  void testBuilder_Attackers_SetsCorrectly() {
    // Given
    var attackers = Arrays.asList("10.0.0.1", "10.0.0.2");

    // When
    var filterBody = AttacksFilterBody.builder().attackers(attackers).build();

    // Then
    assertThat(filterBody.getAttackers()).hasSize(2);
    assertThat(filterBody.getAttackers().get(0)).isEqualTo("10.0.0.1");
  }

  @Test
  void testBuilder_Servers_SetsCorrectly() {
    // Given
    var servers = Arrays.asList("server1", "server2");

    // When
    var filterBody = AttacksFilterBody.builder().servers(servers).build();

    // Then
    assertThat(filterBody.getServers()).hasSize(2);
    assertThat(filterBody.getServers().get(0)).isEqualTo("server1");
  }

  @Test
  void testBuilder_ServerEnvironments_SetsCorrectly() {
    // Given
    var envs = Arrays.asList("PRODUCTION", "QA");

    // When
    var filterBody = AttacksFilterBody.builder().serverEnvironments(envs).build();

    // Then
    assertThat(filterBody.getServerEnvironments()).hasSize(2);
    assertThat(filterBody.getServerEnvironments().get(0)).isEqualTo("PRODUCTION");
  }

  @Test
  void testBuilder_Severities_SetsCorrectly() {
    // Given
    var severities = Arrays.asList("HIGH", "CRITICAL");

    // When
    var filterBody = AttacksFilterBody.builder().severities(severities).build();

    // Then
    assertThat(filterBody.getSeverities()).hasSize(2);
    assertThat(filterBody.getSeverities().get(0)).isEqualTo("HIGH");
  }

  // ========== Test: Immutability ==========

  @Test
  void testBuilder_ReturnedLists_AreImmutable() {
    // Given
    var tags = Arrays.asList("tag1", "tag2");
    var filterBody = AttacksFilterBody.builder().tags(tags).build();

    // When/Then - Should throw UnsupportedOperationException
    assertThatThrownBy(() -> filterBody.getTags().add("tag3"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void testBuilder_ModifyingSourceList_DoesNotAffectBuiltObject() {
    // Given
    var tags = new ArrayList<String>();
    tags.add("tag1");
    tags.add("tag2");

    // When
    var filterBody = AttacksFilterBody.builder().tags(tags).build();

    // Modify source list after building
    tags.add("tag3");

    // Then - Built object should not be affected
    assertThat(filterBody.getTags()).hasSize(2);
    assertThat(filterBody.getTags()).doesNotContain("tag3");
  }

  // ========== Test: Fluent API ==========

  @Test
  void testBuilder_FluentAPI_ChainsCorrectly() {
    // When
    var filterBody =
        AttacksFilterBody.builder()
            .quickFilter("EFFECTIVE")
            .keyword("xss")
            .includeSuppressed(true)
            .includeBotBlockers(false)
            .includeIpBlacklist(true)
            .tags(Arrays.asList("tag1"))
            .statusFilter(Arrays.asList("status1"))
            .build();

    // Then
    assertThat(filterBody.getQuickFilter()).isEqualTo("EFFECTIVE");
    assertThat(filterBody.getKeyword()).isEqualTo("xss");
    assertThat(filterBody.isIncludeSuppressed()).isTrue();
    assertThat(filterBody.isIncludeBotBlockers()).isFalse();
    assertThat(filterBody.isIncludeIpBlacklist()).isTrue();
    assertThat(filterBody.getTags()).hasSize(1);
    assertThat(filterBody.getStatusFilter()).hasSize(1);
  }

  // ========== Test: Complex Scenarios ==========

  @Test
  void testBuilder_AllFieldsSet_BuildsCorrectly() {
    // When
    var filterBody =
        AttacksFilterBody.builder()
            .quickFilter("MANUAL")
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
    assertThat(filterBody.getQuickFilter()).isEqualTo("MANUAL");
    assertThat(filterBody.getKeyword()).isEqualTo("sql");
    assertThat(filterBody.isIncludeSuppressed()).isTrue();
    assertThat(filterBody.isIncludeBotBlockers()).isTrue();
    assertThat(filterBody.isIncludeIpBlacklist()).isTrue();
    assertThat(filterBody.getTags()).hasSize(2);
    assertThat(filterBody.getStatusFilter()).hasSize(1);
    assertThat(filterBody.getProtectionRules()).hasSize(1);
    assertThat(filterBody.getApplications()).hasSize(1);
    assertThat(filterBody.getApplicationImportances()).hasSize(1);
    assertThat(filterBody.getAttackers()).hasSize(1);
    assertThat(filterBody.getServers()).hasSize(1);
    assertThat(filterBody.getServerEnvironments()).hasSize(1);
    assertThat(filterBody.getSeverities()).hasSize(1);
  }

  @Test
  void testBuilder_EmptyLists_BuildsCorrectly() {
    // When
    var filterBody =
        AttacksFilterBody.builder().tags(new ArrayList<>()).statusFilter(new ArrayList<>()).build();

    // Then
    assertThat(filterBody.getTags()).isNotNull().isEmpty();
    assertThat(filterBody.getStatusFilter()).isNotNull().isEmpty();
  }
}
