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
package com.contrast.labs.ai.mcp.contrast.tool.adr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.config.ContrastConfig;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.ProtectData;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.Rule;
import com.contrastsecurity.sdk.ContrastSDK;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for GetProtectRulesTool. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GetProtectRulesToolTest {

  private GetProtectRulesTool tool;

  @Mock private ContrastConfig config;

  @Mock private ContrastSDK sdk;

  private static final String TEST_ORG_ID = "test-org-123";
  private static final String TEST_APP_ID = "test-app-456";

  @BeforeEach
  void setUp() {
    tool = new GetProtectRulesTool();
    ReflectionTestUtils.setField(tool, "config", config);
    when(config.createSDK()).thenReturn(sdk);
    when(config.getOrgId()).thenReturn(TEST_ORG_ID);
  }

  @Test
  void getProtectRules_should_return_rules_for_valid_appId() throws Exception {
    var mockProtectData = createMockProtectData(3);

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getProtectConfig(eq(TEST_ORG_ID), eq(TEST_APP_ID)))
                  .thenReturn(mockProtectData);
            })) {
      var result = tool.getProtectRules(TEST_APP_ID);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.found()).isTrue();
      assertThat(result.data()).isNotNull();
      assertThat(result.data().getRules()).hasSize(3);
    }
  }

  @Test
  void getProtectRules_should_return_rule_details() throws Exception {
    var mockProtectData = createMockProtectDataWithRules();

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getProtectConfig(eq(TEST_ORG_ID), eq(TEST_APP_ID)))
                  .thenReturn(mockProtectData);
            })) {
      var result = tool.getProtectRules(TEST_APP_ID);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.data().getRules()).isNotEmpty();

      var firstRule = result.data().getRules().get(0);
      assertThat(firstRule.getName()).isEqualTo("sql-injection");
      assertThat(firstRule.getProduction()).isEqualTo("block");
    }
  }

  @Test
  void getProtectRules_should_return_validation_error_for_null_appId() {
    var result = tool.getProtectRules(null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("appId is required"));
  }

  @Test
  void getProtectRules_should_return_validation_error_for_empty_appId() {
    var result = tool.getProtectRules("");

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("appId is required"));
  }

  @Test
  void getProtectRules_should_return_notFound_when_protectData_is_null() throws Exception {
    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getProtectConfig(eq(TEST_ORG_ID), eq(TEST_APP_ID))).thenReturn(null);
            })) {
      var result = tool.getProtectRules(TEST_APP_ID);

      assertThat(result.found()).isFalse();
      assertThat(result.data()).isNull();
    }
  }

  @Test
  void getProtectRules_should_warn_when_no_rules_configured() throws Exception {
    var emptyProtectData = new ProtectData();
    emptyProtectData.setRules(new ArrayList<>());

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getProtectConfig(eq(TEST_ORG_ID), eq(TEST_APP_ID)))
                  .thenReturn(emptyProtectData);
            })) {
      var result = tool.getProtectRules(TEST_APP_ID);

      assertThat(result.isSuccess()).isTrue();
      assertThat(result.data().getRules()).isEmpty();
      assertThat(result.warnings()).anyMatch(w -> w.contains("no rules are configured"));
    }
  }

  @Test
  void getProtectRules_should_handle_sdk_exception() throws Exception {
    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getProtectConfig(eq(TEST_ORG_ID), eq(TEST_APP_ID)))
                  .thenThrow(new IOException("Network error"));
            })) {
      var result = tool.getProtectRules(TEST_APP_ID);

      assertThat(result.isSuccess()).isFalse();
      assertThat(result.errors()).anyMatch(e -> e.contains("Internal error"));
    }
  }

  // ========== Helper Methods ==========

  private ProtectData createMockProtectData(int ruleCount) {
    var protectData = new ProtectData();

    var rules = new ArrayList<Rule>();
    for (int i = 0; i < ruleCount; i++) {
      var rule = new Rule();
      rule.setName("protect-rule-" + i);
      rule.setProduction(i % 2 == 0 ? "block" : "monitor");
      rules.add(rule);
    }

    protectData.setRules(rules);
    return protectData;
  }

  private ProtectData createMockProtectDataWithRules() {
    var protectData = new ProtectData();

    var rules = new ArrayList<Rule>();

    var sqlRule = new Rule();
    sqlRule.setName("sql-injection");
    sqlRule.setProduction("block");
    rules.add(sqlRule);

    var xssRule = new Rule();
    xssRule.setName("xss-reflected");
    xssRule.setProduction("monitor");
    rules.add(xssRule);

    protectData.setRules(rules);
    return protectData;
  }
}
