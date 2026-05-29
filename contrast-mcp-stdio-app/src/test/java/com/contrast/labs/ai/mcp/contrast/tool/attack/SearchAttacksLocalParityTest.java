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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.client.SdkApiClient;
import com.contrast.labs.ai.mcp.contrast.config.ContrastSDKFactory;
import com.contrast.labs.ai.mcp.contrast.config.SDKExtensionFactory;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr.Attack;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr.AttacksFilterBody;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr.AttacksResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchAttacksLocalParityTest {

  private static final String TEST_ORG_ID = "local-org-id-should-not-serialize";

  private final ObjectMapper objectMapper = new ObjectMapper();

  @Mock private ContrastSDKFactory sdkFactory;
  @Mock private SDKExtensionFactory sdkExtensionFactory;
  @Mock private SDKExtension sdkExtension;

  private SearchAttacksTool tool;

  @BeforeEach
  void setUp() {
    var sdkApiClient = new SdkApiClient(sdkFactory, sdkExtensionFactory);
    tool = new SearchAttacksTool(sdkApiClient);
    when(sdkFactory.getOrgId()).thenReturn(TEST_ORG_ID);
    when(sdkExtensionFactory.getSDKExtension()).thenReturn(sdkExtension);
  }

  @Test
  void searchAttacks_should_preserve_local_stdio_response_through_sdk_api_client()
      throws Exception {
    var response = new AttacksResponse();
    var attack = new Attack();
    attack.setUuid("attack-uuid-0");
    attack.setStatus("EXPLOITED");
    attack.setSource("192.0.2.10");
    attack.setRules(List.of("sql-injection"));
    response.setAttacks(List.of(attack));
    response.setCount(3);
    when(sdkExtension.getAttacks(eq(TEST_ORG_ID), any(), eq(25), eq(50), eq("-status")))
        .thenReturn(response);

    var result =
        tool.searchAttacks(
            3, 25, "ACTIVE", "EXPLOITED", "SQL", true, false, true, "-status", "sql-injection");
    var responseJson = objectMapper.writeValueAsString(result);

    var filterCaptor = ArgumentCaptor.forClass(AttacksFilterBody.class);
    verify(sdkExtension)
        .getAttacks(eq(TEST_ORG_ID), filterCaptor.capture(), eq(25), eq(50), eq("-status"));
    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items())
        .singleElement()
        .satisfies(item -> assertThat(item.attackId()).isEqualTo("attack-uuid-0"));
    assertThat(result.totalItems()).isEqualTo(3);
    assertThat(filterCaptor.getValue().getQuickFilter()).isEqualTo("ACTIVE");
    assertThat(filterCaptor.getValue().getStatusFilter()).containsExactly("EXPLOITED");
    assertThat(filterCaptor.getValue().getKeyword()).isEqualTo("SQL");
    assertThat(filterCaptor.getValue().getProtectionRules()).containsExactly("sql-injection");
    assertThat(responseJson).contains("attack-uuid-0").doesNotContain(TEST_ORG_ID);
  }
}
