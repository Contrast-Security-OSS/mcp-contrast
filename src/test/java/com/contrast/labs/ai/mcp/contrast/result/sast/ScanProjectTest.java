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
package com.contrast.labs.ai.mcp.contrast.result.sast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.contrastsecurity.sdk.scan.Project;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Unit tests for ScanProject record and its factory method. */
class ScanProjectTest {

  @Test
  void from_should_map_all_fields_correctly() {
    // Given
    Project mockProject = mock();
    when(mockProject.id()).thenReturn("proj-123");
    when(mockProject.name()).thenReturn("test-project");
    when(mockProject.organizationId()).thenReturn("org-456");
    when(mockProject.archived()).thenReturn(false);
    when(mockProject.language()).thenReturn("JAVA");
    when(mockProject.critical()).thenReturn(2);
    when(mockProject.high()).thenReturn(5);
    when(mockProject.medium()).thenReturn(10);
    when(mockProject.low()).thenReturn(3);
    when(mockProject.note()).thenReturn(1);
    when(mockProject.lastScanId()).thenReturn("scan-789");
    when(mockProject.lastScanTime()).thenReturn(Instant.parse("2025-01-01T12:00:00Z"));
    when(mockProject.completedScans()).thenReturn(15);
    when(mockProject.includeNamespaceFilters()).thenReturn(List.of("com.example"));
    when(mockProject.excludeNamespaceFilters()).thenReturn(List.of("com.example.test"));

    // When
    ScanProject result = ScanProject.from(mockProject);

    // Then
    assertThat(result.id()).isEqualTo("proj-123");
    assertThat(result.name()).isEqualTo("test-project");
    assertThat(result.organizationId()).isEqualTo("org-456");
    assertThat(result.archived()).isFalse();
    assertThat(result.language()).isEqualTo("JAVA");
    assertThat(result.critical()).isEqualTo(2);
    assertThat(result.high()).isEqualTo(5);
    assertThat(result.medium()).isEqualTo(10);
    assertThat(result.low()).isEqualTo(3);
    assertThat(result.note()).isEqualTo(1);
    assertThat(result.lastScanId()).isEqualTo("scan-789");
    assertThat(result.lastScanTime()).isEqualTo(Instant.parse("2025-01-01T12:00:00Z"));
    assertThat(result.completedScans()).isEqualTo(15);
    assertThat(result.includeNamespaceFilters()).containsExactly("com.example");
    assertThat(result.excludeNamespaceFilters()).containsExactly("com.example.test");
  }

  @Test
  void from_should_handle_null_lastScanId_and_lastScanTime() {
    // Given - project with no completed scans
    Project mockProject = createMinimalMockProject();
    when(mockProject.lastScanId()).thenReturn(null);
    when(mockProject.lastScanTime()).thenReturn(null);
    when(mockProject.completedScans()).thenReturn(0);

    // When
    ScanProject result = ScanProject.from(mockProject);

    // Then
    assertThat(result.lastScanId()).isNull();
    assertThat(result.lastScanTime()).isNull();
    assertThat(result.completedScans()).isZero();
  }

  @Test
  void from_should_handle_null_namespace_filters() {
    // Given - SDK may return null for empty filters
    Project mockProject = createMinimalMockProject();
    when(mockProject.includeNamespaceFilters()).thenReturn(null);
    when(mockProject.excludeNamespaceFilters()).thenReturn(null);

    // When
    ScanProject result = ScanProject.from(mockProject);

    // Then - should be empty list, not null
    assertThat(result.includeNamespaceFilters()).isEmpty();
    assertThat(result.excludeNamespaceFilters()).isEmpty();
  }

  @Test
  void from_should_create_defensive_copy_of_namespace_filters() {
    // Given
    var includeFilters = new ArrayList<>(List.of("com.example"));
    var excludeFilters = new ArrayList<>(List.of("com.test"));

    Project mockProject = createMinimalMockProject();
    when(mockProject.includeNamespaceFilters()).thenReturn(includeFilters);
    when(mockProject.excludeNamespaceFilters()).thenReturn(excludeFilters);

    // When
    ScanProject result = ScanProject.from(mockProject);

    // Then - modifying original should not affect result
    includeFilters.add("modified");
    excludeFilters.add("modified");

    assertThat(result.includeNamespaceFilters()).containsExactly("com.example");
    assertThat(result.excludeNamespaceFilters()).containsExactly("com.test");
  }

  @Test
  void from_should_throw_on_null_project() {
    assertThatThrownBy(() -> ScanProject.from(null)).isInstanceOf(NullPointerException.class);
  }

  private Project createMinimalMockProject() {
    Project mock = mock();
    when(mock.id()).thenReturn("id");
    when(mock.name()).thenReturn("name");
    when(mock.organizationId()).thenReturn("org");
    when(mock.language()).thenReturn("JAVA");
    when(mock.includeNamespaceFilters()).thenReturn(List.of());
    when(mock.excludeNamespaceFilters()).thenReturn(List.of());
    return mock;
  }
}
