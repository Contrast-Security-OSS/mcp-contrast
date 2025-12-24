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
package com.contrast.labs.ai.mcp.contrast.tool.sast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.AnonymousProjectBuilder;
import com.contrast.labs.ai.mcp.contrast.config.ContrastConfig;
import com.contrastsecurity.sdk.ContrastSDK;
import com.contrastsecurity.sdk.scan.Projects;
import com.contrastsecurity.sdk.scan.ScanManager;
import java.io.IOException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for GetSastProjectTool. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetSastProjectToolTest {

  private GetSastProjectTool tool;

  @Mock private ContrastConfig config;

  @Mock private ContrastSDK sdk;

  @Mock private ScanManager scanManager;

  @Mock private Projects projects;

  private static final String TEST_ORG_ID = "test-org-123";
  private static final String TEST_PROJECT_NAME = "test-project";

  @BeforeEach
  void setUp() {
    tool = new GetSastProjectTool();
    ReflectionTestUtils.setField(tool, "config", config);
    when(config.createSDK()).thenReturn(sdk);
    when(config.getOrgId()).thenReturn(TEST_ORG_ID);
    when(sdk.scan(any())).thenReturn(scanManager);
    when(scanManager.projects()).thenReturn(projects);
  }

  @Test
  void getScanProject_should_return_project_when_project_exists() throws IOException {
    var mockProject =
        AnonymousProjectBuilder.validProject()
            .withName(TEST_PROJECT_NAME)
            .withId("project-123")
            .withLanguage("Java")
            .build();

    when(projects.findByName(TEST_PROJECT_NAME)).thenReturn(Optional.of(mockProject));

    var result = tool.getScanProject(TEST_PROJECT_NAME);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isTrue();
    assertThat(result.data()).isNotNull();
    assertThat(result.data().name()).isEqualTo(TEST_PROJECT_NAME);
    assertThat(result.data().id()).isEqualTo("project-123");
    assertThat(result.data().language()).isEqualTo("Java");
  }

  @Test
  void getScanProject_should_return_project_details() throws IOException {
    var mockProject =
        AnonymousProjectBuilder.validProject()
            .withName(TEST_PROJECT_NAME)
            .withId("project-456")
            .withLanguage("JavaScript")
            .withCritical(5)
            .withHigh(10)
            .withMedium(20)
            .withLow(30)
            .withCompletedScans(3)
            .build();

    when(projects.findByName(TEST_PROJECT_NAME)).thenReturn(Optional.of(mockProject));

    var result = tool.getScanProject(TEST_PROJECT_NAME);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data().critical()).isEqualTo(5);
    assertThat(result.data().high()).isEqualTo(10);
    assertThat(result.data().medium()).isEqualTo(20);
    assertThat(result.data().low()).isEqualTo(30);
    assertThat(result.data().completedScans()).isEqualTo(3);
  }

  @Test
  void getScanProject_should_return_validation_error_for_null_projectName() {
    var result = tool.getScanProject(null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("projectName is required"));
  }

  @Test
  void getScanProject_should_return_validation_error_for_empty_projectName() {
    var result = tool.getScanProject("");

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("projectName is required"));
  }

  @Test
  void getScanProject_should_return_notFound_when_project_does_not_exist() throws IOException {
    when(projects.findByName(TEST_PROJECT_NAME)).thenReturn(Optional.empty());

    var result = tool.getScanProject(TEST_PROJECT_NAME);

    assertThat(result.found()).isFalse();
    assertThat(result.data()).isNull();
  }

  @Test
  void getScanProject_should_handle_sdk_exception() throws IOException {
    when(projects.findByName(TEST_PROJECT_NAME)).thenThrow(new RuntimeException("SDK error"));

    var result = tool.getScanProject(TEST_PROJECT_NAME);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("Internal error"));
  }
}
