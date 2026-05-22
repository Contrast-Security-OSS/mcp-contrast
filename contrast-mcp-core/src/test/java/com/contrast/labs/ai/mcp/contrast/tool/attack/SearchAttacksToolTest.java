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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.client.ContrastApiClient;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr.Attack;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr.AttacksFilterBody;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr.AttacksResponse;
import com.contrastsecurity.exceptions.HttpResponseException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;

class SearchAttacksToolTest {

  private static final String SECRET_BODY = "token=raw-token-value&apiKey=secret";

  private ContrastApiClient contrastApiClient;
  private SearchAttacksTool tool;

  @BeforeEach
  void setUp() {
    contrastApiClient = mock();
    tool = new SearchAttacksTool(contrastApiClient);
  }

  @Test
  void searchAttacks_should_route_filters_and_pagination_through_contrast_api_client()
      throws Exception {
    var response = new AttacksResponse();
    var attack = new Attack();
    attack.setUuid("attack-uuid-0");
    response.setAttacks(List.of(attack));
    response.setCount(3);
    when(contrastApiClient.searchAttacks(any(), eq(25), eq(50), eq("-status")))
        .thenReturn(response);

    var result =
        tool.searchAttacks(
            3,
            25,
            "ACTIVE",
            "EXPLOITED",
            "SQL",
            true,
            false,
            true,
            "-status",
            "sql-injection",
            null);

    var filterCaptor = ArgumentCaptor.forClass(AttacksFilterBody.class);
    verify(contrastApiClient).searchAttacks(filterCaptor.capture(), eq(25), eq(50), eq("-status"));

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items())
        .singleElement()
        .satisfies(item -> assertThat(item.attackId()).isEqualTo("attack-uuid-0"));
    assertThat(result.page()).isEqualTo(3);
    assertThat(result.pageSize()).isEqualTo(25);
    assertThat(result.totalItems()).isEqualTo(3);
    assertThat(filterCaptor.getValue().getQuickFilter()).isEqualTo("ACTIVE");
    assertThat(filterCaptor.getValue().getStatusFilter()).containsExactly("EXPLOITED");
    assertThat(filterCaptor.getValue().getKeyword()).isEqualTo("SQL");
    assertThat(filterCaptor.getValue().isIncludeSuppressed()).isTrue();
    assertThat(filterCaptor.getValue().isIncludeBotBlockers()).isFalse();
    assertThat(filterCaptor.getValue().isIncludeIpBlacklist()).isTrue();
    assertThat(filterCaptor.getValue().getProtectionRules()).containsExactly("sql-injection");
  }

  @Test
  void searchAttacks_should_forward_tool_context_and_preserve_tool_name() throws Exception {
    var response = new AttacksResponse();
    response.setAttacks(List.of(attack("attack-uuid-1")));
    response.setCount(1);
    var toolContext = new ToolContext(Map.of("requestId", "req-123"));
    var capturedContext = new AtomicReference<ToolContext>();
    tool.setAuthenticationStrategy(
        context -> {
          capturedContext.set(context);
          return () -> {};
        });
    when(contrastApiClient.searchAttacks(any(), eq(10), eq(0), eq("-startTime")))
        .thenReturn(response);

    var result =
        tool.searchAttacks(
            1, 10, null, null, null, null, null, null, "-startTime", null, toolContext);
    Method method =
        SearchAttacksTool.class.getDeclaredMethod(
            "searchAttacks",
            Integer.class,
            Integer.class,
            String.class,
            String.class,
            String.class,
            Boolean.class,
            Boolean.class,
            Boolean.class,
            String.class,
            String.class,
            ToolContext.class);

    assertThat(result.isSuccess()).isTrue();
    assertThat(capturedContext.get()).isSameAs(toolContext);
    assertThat(method.getAnnotation(Tool.class).name()).isEqualTo("search_attacks");
  }

  @Test
  void searchAttacks_should_return_validation_error_for_invalid_quick_filter() {
    var result =
        tool.searchAttacks(null, null, "INVALID", null, null, null, null, null, null, null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).anyMatch(e -> e.contains("Invalid quickFilter"));
    verifyNoInteractions(contrastApiClient);
  }

  @Test
  void searchAttacks_should_cap_page_size_at_100() throws Exception {
    var response = new AttacksResponse();
    response.setAttacks(List.of(attack("attack-uuid-2")));
    response.setCount(200);
    when(contrastApiClient.searchAttacks(any(), eq(100), eq(0), eq("-startTime")))
        .thenReturn(response);

    var result =
        tool.searchAttacks(1, 250, null, null, null, null, null, null, "-startTime", null, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.pageSize()).isEqualTo(100);
    assertThat(result.warnings())
        .contains("Requested pageSize 250 exceeds maximum 100, capped to 100");
    verify(contrastApiClient, never()).searchAttacks(any(), eq(250), anyInt(), any());
  }

  @Test
  void searchAttacks_should_warn_when_api_returns_no_attack_data() throws Exception {
    when(contrastApiClient.searchAttacks(any(), eq(50), eq(0), eq("-startTime"))).thenReturn(null);

    var result =
        tool.searchAttacks(1, null, null, null, null, null, null, null, "-startTime", null, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).isEmpty();
    assertThat(result.warnings()).anyMatch(w -> w.contains("API returned no attack data"));
  }

  @Test
  void searchAttacks_should_map_downstream_403_without_exposing_response_body() throws Exception {
    when(contrastApiClient.searchAttacks(any(), eq(10), eq(0), eq("-startTime")))
        .thenThrow(apiFailure(403));

    var result =
        tool.searchAttacks(1, 10, null, null, null, null, null, null, "-startTime", null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .containsExactly("Access denied. User lacks permission for this resource.");
    assertThat(result.toString()).doesNotContain(SECRET_BODY, "/ng/org/attacks");
  }

  @Test
  void searchAttacks_should_map_downstream_429_without_exposing_response_body() throws Exception {
    when(contrastApiClient.searchAttacks(any(), eq(10), eq(0), eq("-startTime")))
        .thenThrow(apiFailure(429));

    var result =
        tool.searchAttacks(1, 10, null, null, null, null, null, null, "-startTime", null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors()).containsExactly("Rate limit exceeded. Retry after a brief pause.");
    assertThat(result.toString()).doesNotContain(SECRET_BODY, "/ng/org/attacks");
  }

  @Test
  void searchAttacks_should_map_downstream_5xx_without_exposing_response_body() throws Exception {
    when(contrastApiClient.searchAttacks(any(), eq(10), eq(0), eq("-startTime")))
        .thenThrow(apiFailure(503));

    var result =
        tool.searchAttacks(1, 10, null, null, null, null, null, null, "-startTime", null, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .containsExactly(
            "The service returned an error. Narrow filters or reduce page size, then retry.");
    assertThat(result.toString()).doesNotContain(SECRET_BODY, "/ng/org/attacks");
  }

  private static Attack attack(String attackId) {
    var attack = new Attack();
    attack.setUuid(attackId);
    return attack;
  }

  private static HttpResponseException apiFailure(int status) {
    return new HttpResponseException(
        "Downstream failure", "POST", "/ng/org/attacks", status, "Downstream failure", SECRET_BODY);
  }
}
