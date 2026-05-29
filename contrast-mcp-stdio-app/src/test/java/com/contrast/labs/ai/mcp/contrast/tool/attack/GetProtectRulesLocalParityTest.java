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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.client.SdkApiClient;
import com.contrast.labs.ai.mcp.contrast.config.ContrastSDKFactory;
import com.contrast.labs.ai.mcp.contrast.config.SDKExtensionFactory;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.ProtectData;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.Rule;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GetProtectRulesLocalParityTest {

  private static final String TEST_ORG_ID = "local-org-id-should-not-serialize";
  private static final String TEST_APP_ID = "test-app-456";

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock private ContrastSDKFactory sdkFactory;
  @Mock private SDKExtensionFactory sdkExtensionFactory;
  @Mock private SDKExtension sdkExtension;

  private GetProtectRulesTool tool;

  @BeforeEach
  void setUp() {
    var sdkApiClient = new SdkApiClient(sdkFactory, sdkExtensionFactory);
    tool = new GetProtectRulesTool(sdkApiClient);
    when(sdkFactory.getOrgId()).thenReturn(TEST_ORG_ID);
    when(sdkExtensionFactory.getSDKExtension()).thenReturn(sdkExtension);
  }

  @Test
  void getProtectRules_should_preserve_local_stdio_response_through_sdk_api_client()
      throws Exception {
    var protectData = createProtectData();
    when(sdkExtension.getProtectConfig(TEST_ORG_ID, TEST_APP_ID)).thenReturn(protectData);

    var response = tool.getProtectRules(TEST_APP_ID, null);
    var responseJson = objectMapper.writeValueAsString(response);

    assertThat(response.isSuccess()).isTrue();
    assertThat(response.found()).isTrue();
    assertThat(response.errors()).isEmpty();
    assertThat(response.warnings()).isEmpty();
    assertThat(response.data().getRules())
        .extracting(Rule::getName)
        .containsExactly("sql-injection", "xss-reflected");
    assertThat(responseJson).contains("sql-injection", "xss-reflected").doesNotContain(TEST_ORG_ID);
    verify(sdkExtension).getProtectConfig(TEST_ORG_ID, TEST_APP_ID);
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
