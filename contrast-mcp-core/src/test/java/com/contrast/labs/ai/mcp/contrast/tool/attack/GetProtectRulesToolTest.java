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
package com.contrast.labs.ai.mcp.contrast.tool.attack;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.client.ContrastApiClient;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.ProtectData;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.Rule;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;

class GetProtectRulesToolTest {

  private static final String TEST_APP_ID = "test-app-456";

  private GetProtectRulesTool tool;
  private ContrastApiClient contrastApiClient;

  @BeforeEach
  void setUp() {
    contrastApiClient = mock();
    tool = new GetProtectRulesTool(contrastApiClient);
  }

  @Test
  void getProtectRules_should_return_rules_through_contrast_api_client() throws Exception {
    var protectData = createProtectData();
    when(contrastApiClient.getProtectRules(TEST_APP_ID)).thenReturn(protectData);

    var result = tool.getProtectRules(TEST_APP_ID);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isTrue();
    assertThat(result.data().getRules())
        .extracting(Rule::getName)
        .containsExactly("sql-injection", "xss-reflected");
    verify(contrastApiClient).getProtectRules(TEST_APP_ID);
  }

  @Test
  void getProtectRules_should_return_validation_error_for_null_appId() {
    var result = tool.getProtectRules(null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("appId is required");
    assertThat(result.warnings()).isEmpty();
  }

  @Test
  void getProtectRules_should_return_validation_error_for_blank_appId() {
    var result = tool.getProtectRules("   ", null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("appId is required");
    assertThat(result.warnings()).isEmpty();
  }

  @Test
  void getProtectRules_should_return_not_found_when_protect_data_is_null() throws Exception {
    when(contrastApiClient.getProtectRules(TEST_APP_ID)).thenReturn(null);

    var result = tool.getProtectRules(TEST_APP_ID, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.found()).isFalse();
    assertThat(result.data()).isNull();
    assertThat(result.warnings()).containsExactly("Resource not found");
  }

  @Test
  void getProtectRules_should_warn_when_no_rules_configured() throws Exception {
    var protectData = new ProtectData();
    protectData.setRules(new ArrayList<>());
    when(contrastApiClient.getProtectRules(TEST_APP_ID)).thenReturn(protectData);

    var result = tool.getProtectRules(TEST_APP_ID, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data().getRules()).isEmpty();
    assertThat(result.warnings())
        .containsExactly("Application has Protect enabled but no rules are configured.");
  }

  @Test
  void getProtectRules_should_return_sanitized_internal_error_when_client_fails() throws Exception {
    when(contrastApiClient.getProtectRules(TEST_APP_ID))
        .thenThrow(new IOException("Network error"));

    var result = tool.getProtectRules(TEST_APP_ID, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .singleElement()
        .satisfies(
            error -> {
              assertThat(error).startsWith("An internal error occurred (ref: ");
              assertThat(error).doesNotContain("Network error", TEST_APP_ID);
            });
  }

  @Test
  void getProtectRules_should_preserve_legacy_tool_name_and_forward_tool_context()
      throws Exception {
    var protectData = createProtectData();
    when(contrastApiClient.getProtectRules(TEST_APP_ID)).thenReturn(protectData);
    var toolContext = new ToolContext(Map.of("requestId", "req-123"));
    var capturedContext = new AtomicReference<ToolContext>();
    tool.setAuthenticationStrategy(
        context -> {
          capturedContext.set(context);
          return () -> {};
        });

    var result = tool.getProtectRules(TEST_APP_ID, toolContext);
    Method toolMethod =
        GetProtectRulesTool.class.getDeclaredMethod(
            "getProtectRules", String.class, ToolContext.class);

    assertThat(result.isSuccess()).isTrue();
    assertThat(capturedContext.get()).isSameAs(toolContext);
    assertThat(toolMethod.getAnnotation(Tool.class).name()).isEqualTo("get_protect_rules");
    assertThat(toolMethod.getParameterTypes()).containsExactly(String.class, ToolContext.class);
  }

  private static ProtectData createProtectData() {
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
