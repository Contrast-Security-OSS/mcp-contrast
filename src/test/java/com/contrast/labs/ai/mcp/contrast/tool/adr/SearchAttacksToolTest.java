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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contrast.labs.ai.mcp.contrast.config.ContrastConfig;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr.Attack;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr.AttacksFilterBody;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr.AttacksResponse;
import com.contrastsecurity.sdk.ContrastSDK;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

/** Unit tests for SearchAttacksTool. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SearchAttacksToolTest {

  private SearchAttacksTool tool;

  @Mock private ContrastConfig config;

  @Mock private ContrastSDK sdk;

  private static final String TEST_ORG_ID = "test-org-123";

  @BeforeEach
  void setUp() {
    tool = new SearchAttacksTool();
    ReflectionTestUtils.setField(tool, "config", config);
    when(config.getSDK()).thenReturn(sdk);
    when(config.getOrgId()).thenReturn(TEST_ORG_ID);
  }

  @Test
  void searchAttacks_should_return_attacks_with_no_filters() throws Exception {
    var mockResponse = createMockAttacksResponse(3, 3);

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                  .thenReturn(mockResponse);
            })) {
      var result = tool.searchAttacks(null, null, null, null, null, null, null, null, null);

      assertThat(result.items()).hasSize(3);
      assertThat(result.items().get(0).attackId()).isEqualTo("attack-uuid-0");
      assertThat(result.page()).isEqualTo(1);
      assertThat(result.pageSize()).isEqualTo(50);
    }
  }

  @Test
  void searchAttacks_should_pass_quickFilter_to_sdk() throws Exception {
    var mockResponse = createMockAttacksResponse(2, 2);

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                  .thenReturn(mockResponse);
            })) {
      tool.searchAttacks("ACTIVE", null, null, null, null, null, null, null, null);

      var extension = mockedConstruction.constructed().get(0);
      var captor = ArgumentCaptor.forClass(AttacksFilterBody.class);
      verify(extension).getAttacks(eq(TEST_ORG_ID), captor.capture(), eq(50), eq(0), isNull());

      assertThat(captor.getValue().getQuickFilter()).isEqualTo("ACTIVE");
    }
  }

  @Test
  void searchAttacks_should_pass_statusFilter_to_sdk() throws Exception {
    var mockResponse = createMockAttacksResponse(1, 1);

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                  .thenReturn(mockResponse);
            })) {
      tool.searchAttacks(null, "EXPLOITED", null, null, null, null, null, null, null);

      var extension = mockedConstruction.constructed().get(0);
      var captor = ArgumentCaptor.forClass(AttacksFilterBody.class);
      verify(extension).getAttacks(eq(TEST_ORG_ID), captor.capture(), eq(50), eq(0), isNull());

      assertThat(captor.getValue().getStatusFilter()).containsExactly("EXPLOITED");
    }
  }

  @Test
  void searchAttacks_should_pass_keyword_to_sdk() throws Exception {
    var mockResponse = createMockAttacksResponse(1, 1);

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                  .thenReturn(mockResponse);
            })) {
      tool.searchAttacks(null, null, "sql injection", null, null, null, null, null, null);

      var extension = mockedConstruction.constructed().get(0);
      var captor = ArgumentCaptor.forClass(AttacksFilterBody.class);
      verify(extension).getAttacks(eq(TEST_ORG_ID), captor.capture(), eq(50), eq(0), isNull());

      // Keyword is URL-encoded: space becomes +
      assertThat(captor.getValue().getKeyword()).isEqualTo("sql+injection");
    }
  }

  @Test
  void searchAttacks_should_pass_boolean_filters_to_sdk() throws Exception {
    var mockResponse = createMockAttacksResponse(1, 1);

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                  .thenReturn(mockResponse);
            })) {
      tool.searchAttacks(null, null, null, true, false, true, null, null, null);

      var extension = mockedConstruction.constructed().get(0);
      var captor = ArgumentCaptor.forClass(AttacksFilterBody.class);
      verify(extension).getAttacks(eq(TEST_ORG_ID), captor.capture(), eq(50), eq(0), isNull());

      assertThat(captor.getValue().isIncludeSuppressed()).isTrue();
      assertThat(captor.getValue().isIncludeBotBlockers()).isFalse();
      assertThat(captor.getValue().isIncludeIpBlacklist()).isTrue();
    }
  }

  @Test
  void searchAttacks_should_pass_pagination_to_sdk() throws Exception {
    var mockResponse = createMockAttacksResponse(25, 100);

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(25), eq(50), isNull()))
                  .thenReturn(mockResponse);
            })) {
      var result = tool.searchAttacks(null, null, null, null, null, null, null, 3, 25);

      assertThat(result.page()).isEqualTo(3);
      assertThat(result.pageSize()).isEqualTo(25);

      var extension = mockedConstruction.constructed().get(0);
      verify(extension)
          .getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(25), eq(50), isNull());
    }
  }

  @Test
  void searchAttacks_should_pass_sort_to_sdk() throws Exception {
    var mockResponse = createMockAttacksResponse(1, 1);

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      eq(TEST_ORG_ID),
                      any(AttacksFilterBody.class),
                      eq(50),
                      eq(0),
                      eq("-severity")))
                  .thenReturn(mockResponse);
            })) {
      tool.searchAttacks(null, null, null, null, null, null, "-severity", null, null);

      var extension = mockedConstruction.constructed().get(0);
      verify(extension)
          .getAttacks(
              eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), eq("-severity"));
    }
  }

  @Test
  void searchAttacks_should_return_validation_error_for_invalid_quickFilter() {
    var result = tool.searchAttacks("INVALID", null, null, null, null, null, null, null, null);

    assertThat(result.items()).isEmpty();
    assertThat(result.errors()).anyMatch(e -> e.contains("Invalid quickFilter"));
  }

  @Test
  void searchAttacks_should_return_validation_error_for_invalid_statusFilter() {
    var result = tool.searchAttacks(null, "INVALID", null, null, null, null, null, null, null);

    assertThat(result.items()).isEmpty();
    assertThat(result.errors()).anyMatch(e -> e.contains("Invalid statusFilter"));
  }

  @Test
  void searchAttacks_should_return_validation_error_for_invalid_sort() {
    var result = tool.searchAttacks(null, null, null, null, null, null, "bad!", null, null);

    assertThat(result.items()).isEmpty();
    assertThat(result.errors()).anyMatch(e -> e.contains("Invalid sort format"));
  }

  @Test
  void searchAttacks_should_handle_empty_results() throws Exception {
    var emptyResponse = createMockAttacksResponse(0, 0);

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      anyString(), any(AttacksFilterBody.class), anyInt(), anyInt(), any()))
                  .thenReturn(emptyResponse);
            })) {
      var result = tool.searchAttacks(null, null, null, null, null, null, null, null, null);

      assertThat(result.items()).isEmpty();
      assertThat(result.warnings()).anyMatch(w -> w.contains("No results found"));
    }
  }

  @Test
  void searchAttacks_should_handle_null_response() throws Exception {
    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      anyString(), any(AttacksFilterBody.class), anyInt(), anyInt(), any()))
                  .thenReturn(null);
            })) {
      var result = tool.searchAttacks(null, null, null, null, null, null, null, null, null);

      assertThat(result.items()).isEmpty();
      assertThat(result.warnings()).anyMatch(w -> w.contains("API returned no attack data"));
    }
  }

  @Test
  void searchAttacks_should_calculate_hasMorePages_correctly() throws Exception {
    var mockResponse = createMockAttacksResponse(50, 150);

    try (var mockedConstruction =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      anyString(), any(AttacksFilterBody.class), anyInt(), anyInt(), any()))
                  .thenReturn(mockResponse);
            })) {
      var result = tool.searchAttacks(null, null, null, null, null, null, null, 1, 50);

      assertThat(result.hasMorePages()).isTrue();
      assertThat(result.totalItems()).isEqualTo(150);
    }
  }

  // ========== Helper Methods ==========

  private AttacksResponse createMockAttacksResponse(int count, Integer totalCount) {
    var response = new AttacksResponse();
    response.setAttacks(createMockAttacks(count));
    response.setCount(totalCount);
    return response;
  }

  private List<Attack> createMockAttacks(int count) {
    var attacks = new ArrayList<Attack>();
    var baseTime = System.currentTimeMillis();

    for (int i = 0; i < count; i++) {
      var attack = new Attack();
      attack.setUuid("attack-uuid-" + i);
      attack.setStatus("PROBED");
      attack.setSource("192.168.1." + (100 + i));
      attack.setRules(List.of("sql-injection", "xss-reflected"));
      attack.setProbes(10 + i);
      attack.setStart_time(baseTime + (i * 1000));
      attack.setEnd_time(baseTime + (i * 1000) + 5000);
      attack.setFirst_event_time(baseTime + (i * 1000));
      attack.setLast_event_time(baseTime + (i * 1000) + 5000);
      attack.setAttacksApplication(List.of());
      attacks.add(attack);
    }

    return attacks;
  }
}
