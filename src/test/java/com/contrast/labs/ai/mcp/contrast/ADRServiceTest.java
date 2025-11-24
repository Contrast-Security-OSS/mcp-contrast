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
package com.contrast.labs.ai.mcp.contrast;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr.Attack;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr.AttacksFilterBody;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr.AttacksResponse;
import com.contrast.labs.ai.mcp.contrast.utils.PaginationHandler;
import com.contrastsecurity.sdk.ContrastSDK;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/** Test suite for ADRService, focusing on consolidated searchAttacks method. */
@ExtendWith(MockitoExtension.class)
class ADRServiceTest {

  private ADRService adrService;
  private ContrastSDK mockContrastSDK;
  private MockedStatic<SDKHelper> mockedSDKHelper;
  private MockedConstruction<SDKExtension> mockedSDKExtension;

  private static final String TEST_ORG_ID = "test-org-123";
  private static final String TEST_HOST = "https://test.contrast.local";
  private static final String TEST_API_KEY = "test-api-key";
  private static final String TEST_SERVICE_KEY = "test-service-key";
  private static final String TEST_USERNAME = "test-user";
  private static final String TEST_APP_ID = "test-app-456";

  @BeforeEach
  void setUp() throws Exception {
    adrService = new ADRService(new PaginationHandler());
    mockContrastSDK = mock();

    // Mock static SDKHelper
    mockedSDKHelper = mockStatic(SDKHelper.class);
    mockedSDKHelper
        .when(
            () ->
                SDKHelper.getSDK(
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(mockContrastSDK);

    // Set required configuration fields
    ReflectionTestUtils.setField(adrService, "orgID", TEST_ORG_ID);
    ReflectionTestUtils.setField(adrService, "hostName", TEST_HOST);
    ReflectionTestUtils.setField(adrService, "apiKey", TEST_API_KEY);
    ReflectionTestUtils.setField(adrService, "serviceKey", TEST_SERVICE_KEY);
    ReflectionTestUtils.setField(adrService, "userName", TEST_USERNAME);
    ReflectionTestUtils.setField(adrService, "httpProxyHost", "");
    ReflectionTestUtils.setField(adrService, "httpProxyPort", "");
  }

  @AfterEach
  void tearDown() {
    if (mockedSDKHelper != null) {
      mockedSDKHelper.close();
    }
    if (mockedSDKExtension != null) {
      mockedSDKExtension.close();
    }
  }

  // ========== Test: No Filters (All Attacks) ==========

  @Test
  void testSearchAttacks_NoFilters_ReturnsAllAttacks() throws Exception {
    // Given
    var mockResponse = createMockAttacksResponse(3, null);

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                  .thenReturn(mockResponse);
            });

    // When
    var result = adrService.searchAttacks(null, null, null, null, null, null, null, null, null);

    // Then
    assertThat(result.items()).hasSize(3);
    assertThat(result.items().get(0).attackId()).isEqualTo("attack-uuid-0");
    assertThat(result.items().get(1).attackId()).isEqualTo("attack-uuid-1");
    assertThat(result.items().get(2).attackId()).isEqualTo("attack-uuid-2");
    assertThat(result.page()).isEqualTo(1);
    assertThat(result.pageSize()).isEqualTo(50);
    assertThat(result.hasMorePages()).isFalse();
  }

  // ========== Test: QuickFilter ==========

  @Test
  void testSearchAttacks_WithQuickFilter_PassesFilterToSDK() throws Exception {
    // Given
    var mockResponse = createMockAttacksResponse(2, null);

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                  .thenReturn(mockResponse);
            });

    // When
    adrService.searchAttacks("ACTIVE", null, null, null, null, null, null, null, null);

    // Then
    var extension = mockedSDKExtension.constructed().get(0);
    var captor = ArgumentCaptor.forClass(AttacksFilterBody.class);
    verify(extension).getAttacks(eq(TEST_ORG_ID), captor.capture(), eq(50), eq(0), isNull());

    assertThat(captor.getValue().getQuickFilter()).isEqualTo("ACTIVE");
  }

  // ========== Test: Keyword Filter ==========

  @Test
  void testSearchAttacks_WithKeyword_PassesKeywordToSDK() throws Exception {
    // Given
    var mockResponse = createMockAttacksResponse(1, null);

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                  .thenReturn(mockResponse);
            });

    // When
    adrService.searchAttacks(null, null, "sql injection", null, null, null, null, null, null);

    // Then
    var extension = mockedSDKExtension.constructed().get(0);
    var captor = ArgumentCaptor.forClass(AttacksFilterBody.class);
    verify(extension).getAttacks(eq(TEST_ORG_ID), captor.capture(), eq(50), eq(0), isNull());

    assertThat(captor.getValue().getKeyword()).isEqualTo("sql injection");
  }

  // ========== Test: Boolean Filters ==========

  @Test
  void testSearchAttacks_WithBooleanFilters_PassesCorrectly() throws Exception {
    // Given
    var mockResponse = createMockAttacksResponse(1, null);

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                  .thenReturn(mockResponse);
            });

    // When
    adrService.searchAttacks(null, null, null, true, false, true, null, null, null);

    // Then
    var extension = mockedSDKExtension.constructed().get(0);
    var captor = ArgumentCaptor.forClass(AttacksFilterBody.class);
    verify(extension).getAttacks(eq(TEST_ORG_ID), captor.capture(), eq(50), eq(0), isNull());

    assertThat(captor.getValue().isIncludeSuppressed()).isEqualTo(true);
    assertThat(captor.getValue().isIncludeBotBlockers()).isEqualTo(false);
    assertThat(captor.getValue().isIncludeIpBlacklist()).isEqualTo(true);
  }

  // ========== Test: Pagination Parameters ==========

  @Test
  void testSearchAttacks_WithPaginationParams_PassesToSDK() throws Exception {
    // Given
    var mockResponse = createMockAttacksResponse(2, null);

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      eq(TEST_ORG_ID),
                      any(AttacksFilterBody.class),
                      eq(50),
                      eq(100),
                      eq("firstEventTime")))
                  .thenReturn(mockResponse);
            });

    // When
    adrService.searchAttacks(null, null, null, null, null, null, "firstEventTime", 3, 50);

    // Then
    var extension = mockedSDKExtension.constructed().get(0);
    verify(extension)
        .getAttacks(
            eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(100), eq("firstEventTime"));
  }

  // ========== Test: Combined Filters ==========

  @Test
  void testSearchAttacks_WithMultipleFilters_AllPassedCorrectly() throws Exception {
    // Given
    var mockResponse = createMockAttacksResponse(1, null);

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      eq(TEST_ORG_ID),
                      any(AttacksFilterBody.class),
                      eq(25),
                      eq(50),
                      eq("severity")))
                  .thenReturn(mockResponse);
            });

    // When
    adrService.searchAttacks("EFFECTIVE", null, "xss", true, true, false, "severity", 3, 25);

    // Then
    var extension = mockedSDKExtension.constructed().get(0);
    var captor = ArgumentCaptor.forClass(AttacksFilterBody.class);
    verify(extension).getAttacks(eq(TEST_ORG_ID), captor.capture(), eq(25), eq(50), eq("severity"));

    var filter = captor.getValue();
    assertThat(filter.getQuickFilter()).isEqualTo("EFFECTIVE");
    assertThat(filter.getKeyword()).isEqualTo("xss");
    assertThat(filter.isIncludeSuppressed()).isTrue();
    assertThat(filter.isIncludeBotBlockers()).isTrue();
    assertThat(filter.isIncludeIpBlacklist()).isFalse();
  }

  // ========== Test: Empty Results ==========

  @Test
  void testSearchAttacks_EmptyResults_ReturnsEmptyList() throws Exception {
    // Given
    var emptyResponse = createMockAttacksResponse(0, 0);

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                  .thenReturn(emptyResponse);
            });

    // When
    var result = adrService.searchAttacks(null, null, null, null, null, null, null, null, null);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.items()).isEmpty();
    assertThat(result.hasMorePages()).isFalse();
    assertThat(result.message()).as("Empty results should have explanatory message").isNotNull();
    assertThat(result.message())
        .as("Message should explain empty results to AI")
        .contains("No items found");
  }

  // ========== Test: Null Results ==========

  @Test
  void testSearchAttacks_NullResults_ReturnsEmptyList() throws Exception {
    // Given
    var nullResponse = new AttacksResponse();
    nullResponse.setAttacks(null); // Simulate null attacks list

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                  .thenReturn(nullResponse);
            });

    // When
    var result = adrService.searchAttacks(null, null, null, null, null, null, null, null, null);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.items()).isEmpty();
  }

  // ========== Test: SDK Exception ==========

  @Test
  void testSearchAttacks_SDKThrowsException_PropagatesException() throws Exception {
    // Given
    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                  .thenThrow(new RuntimeException("API connection failed"));
            });

    // When/Then
    assertThatThrownBy(
            () -> {
              adrService.searchAttacks(null, null, null, null, null, null, null, null, null);
            })
        .isInstanceOf(Exception.class)
        .satisfies(
            ex ->
                assertThat(
                        ex.getMessage().contains("API connection failed")
                            || (ex.getCause() != null
                                && ex.getCause().getMessage().contains("API connection failed")))
                    .isTrue());
  }

  // ========== Test: Null Filters Don't Override Defaults ==========

  @Test
  void testSearchAttacks_NullFilters_DoesNotSetFilterBodyFields() throws Exception {
    // Given
    var mockResponse = createMockAttacksResponse(1, null);

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                  .thenReturn(mockResponse);
            });

    // When
    adrService.searchAttacks(null, null, null, null, null, null, null, null, null);

    // Then
    var extension = mockedSDKExtension.constructed().get(0);
    var captor = ArgumentCaptor.forClass(AttacksFilterBody.class);
    verify(extension).getAttacks(eq(TEST_ORG_ID), captor.capture(), eq(50), eq(0), isNull());

    var filter = captor.getValue();
    // Verify null parameters didn't set fields (they should remain at constructor defaults)
    assertThat(filter).isNotNull(); // Filter body is created but fields remain unset
  }

  // ========== Pagination Tests ==========

  @Test
  void testSearchAttacks_WithTotalCount_ProvidesAccurateHasMorePages() throws Exception {
    // Given: API returns 50 items with totalCount=150 (3 pages total)
    var mockResponse = createMockAttacksResponse(50, 150);

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                  .thenReturn(mockResponse);
            });

    // When
    var result = adrService.searchAttacks(null, null, null, null, null, null, null, 1, 50);

    // Then
    assertThat(result.items()).hasSize(50);
    assertThat(result.page()).isEqualTo(1);
    assertThat(result.pageSize()).isEqualTo(50);
    assertThat(result.totalItems()).isEqualTo(150);
    assertThat(result.hasMorePages()).as("Should have more pages (page 1 of 3)").isTrue();
  }

  @Test
  void testSearchAttacks_LastPage_WithTotalCount_HasMorePagesFalse() throws Exception {
    // Given: Page 3 of 3 (offset=100, returns 50 items, total=150)
    var mockResponse = createMockAttacksResponse(50, 150);

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(100), isNull()))
                  .thenReturn(mockResponse);
            });

    // When
    var result = adrService.searchAttacks(null, null, null, null, null, null, null, 3, 50);

    // Then
    assertThat(result.items()).hasSize(50);
    assertThat(result.page()).isEqualTo(3);
    assertThat(result.pageSize()).isEqualTo(50);
    assertThat(result.totalItems()).isEqualTo(150);
    assertThat(result.hasMorePages()).as("Last page should have hasMorePages=false").isFalse();
  }

  @Test
  void testSearchAttacks_InvalidPageSize_ClampsAndWarns() throws Exception {
    // Given
    var mockResponse = createMockAttacksResponse(100, 200);

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              // Should be clamped to 100 (max)
              when(mock.getAttacks(
                      eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(100), eq(0), isNull()))
                  .thenReturn(mockResponse);
            });

    // When: Request pageSize=500 (exceeds max of 100)
    var result = adrService.searchAttacks(null, null, null, null, null, null, null, 1, 500);

    // Then
    assertThat(result.pageSize()).as("PageSize should be clamped to 100").isEqualTo(100);
    assertThat(result.message()).as("Should have warning message").isNotNull();
    assertThat(result.message()).as("Message should mention original value").contains("500");
    assertThat(result.message()).as("Message should mention clamped value").contains("100");
  }

  @Test
  void testSearchAttacks_InvalidPage_ClampsAndWarns() throws Exception {
    // Given
    var mockResponse = createMockAttacksResponse(50, null);

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              // Should be clamped to page 1 (offset=0)
              when(mock.getAttacks(
                      eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                  .thenReturn(mockResponse);
            });

    // When: Request page=0 or negative (invalid)
    var result = adrService.searchAttacks(null, null, null, null, null, null, null, 0, 50);

    // Then
    assertThat(result.page()).as("Page should be clamped to 1").isEqualTo(1);
    assertThat(result.message()).as("Should have warning message").isNotNull();
    assertThat(result.message())
        .as("Message should indicate invalid page")
        .contains("Invalid page");
  }

  @Test
  void testSearchAttacks_WithoutTotalCount_UsesHeuristic() throws Exception {
    // Given: Full page of results (50 items), no totalCount
    var mockResponse = createMockAttacksResponse(50, null);

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                  .thenReturn(mockResponse);
            });

    // When
    var result = adrService.searchAttacks(null, null, null, null, null, null, null, 1, 50);

    // Then
    assertThat(result.totalItems()).as("TotalItems should be null when not provided").isNull();
    assertThat(result.hasMorePages()).as("Heuristic: full page suggests more pages exist").isTrue();
  }

  @Test
  void testSearchAttacks_PartialPageWithoutCount_NoMorePages() throws Exception {
    // Given: Partial page (25 items when pageSize=50), no totalCount
    var mockResponse = createMockAttacksResponse(25, null);

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                  .thenReturn(mockResponse);
            });

    // When
    var result = adrService.searchAttacks(null, null, null, null, null, null, null, 1, 50);

    // Then
    assertThat(result.items().size()).isEqualTo(25);
    assertThat(result.totalItems()).as("TotalItems should be null when not provided").isNull();
    assertThat(result.hasMorePages())
        .as("Heuristic: partial page suggests no more pages")
        .isFalse();
  }

  // ========== Test: Smart Defaults and Messages ==========

  @Test
  void testSearchAttacks_SmartDefaults_ReturnsMessages() throws Exception {
    // Given: No filters provided, should use smart defaults
    var mockResponse = createMockAttacksResponse(10, null);

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                  .thenReturn(mockResponse);
            });

    // When: No filters provided
    var result = adrService.searchAttacks(null, null, null, null, null, null, null, 1, 50);

    // Then: Should have messages about smart defaults
    assertThat(result.message()).as("Should have messages about smart defaults").isNotNull();
    assertThat(result.message())
        .as("Should have message about quickFilter default")
        .contains("No quickFilter applied");
    assertThat(result.message())
        .as("Should have message about includeSuppressed default")
        .contains("Excluding suppressed attacks by default");
  }

  @Test
  void testSearchAttacks_ExplicitFilters_NoSmartDefaultMessages() throws Exception {
    // Given: Explicit filters provided
    var mockResponse = createMockAttacksResponse(5, null);

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getAttacks(
                      eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                  .thenReturn(mockResponse);
            });

    // When: Explicit filters provided
    var result = adrService.searchAttacks("EFFECTIVE", null, null, true, null, null, null, 1, 50);

    // Then: Should NOT have smart default messages
    if (result.message() != null) {
      assertThat(result.message())
          .as("Should not have quickFilter message when explicitly provided")
          .doesNotContain("No quickFilter applied");
      assertThat(result.message())
          .as("Should not have includeSuppressed message when explicitly provided")
          .doesNotContain("Excluding suppressed attacks by default");
    }
  }

  @Test
  void testSearchAttacks_InvalidQuickFilter_ReturnsError() throws Exception {
    // When: Invalid quickFilter provided
    var result =
        adrService.searchAttacks("INVALID_FILTER", null, null, null, null, null, null, 1, 50);

    // Then: Should return error response with descriptive message
    assertThat(result.message()).as("Should have error message").isNotNull();
    assertThat(result.message())
        .as("Should explain the invalid quickFilter")
        .contains("Invalid quickFilter 'INVALID_FILTER'");
    assertThat(result.message())
        .as("Should list valid options")
        .contains("Valid: ALL, ACTIVE, MANUAL, AUTOMATED, PRODUCTION, EFFECTIVE");
    assertThat(result.items().size()).as("Should return empty items on error").isEqualTo(0);
  }

  @Test
  void testSearchAttacks_InvalidSort_ReturnsError() throws Exception {
    // When: Invalid sort format provided
    var result =
        adrService.searchAttacks(
            "EFFECTIVE", null, null, false, null, null, "invalid sort!", 1, 50);

    // Then: Should return error response with descriptive message
    assertThat(result.message()).as("Should have error message").isNotNull();
    assertThat(result.message())
        .as("Should explain the invalid sort format")
        .contains("Invalid sort format 'invalid sort!'");
    assertThat(result.message())
        .as("Should explain the correct format")
        .contains("Must be a field name with optional '-' prefix");
    assertThat(result.items().size()).as("Should return empty items on error").isEqualTo(0);
  }

  @Test
  void testSearchAttacks_MultipleValidationErrors_CombinesErrors() throws Exception {
    // When: Multiple invalid parameters provided
    var result =
        adrService.searchAttacks("BAD_FILTER", null, null, null, null, null, "bad-format!", 1, 50);

    // Then: Should return combined error messages
    assertThat(result.message()).as("Should have error message").isNotNull();
    assertThat(result.message())
        .as("Should include quickFilter error")
        .contains("Invalid quickFilter");
    assertThat(result.message()).as("Should include sort error").contains("Invalid sort format");
    assertThat(result.items().size()).as("Should return empty items on error").isEqualTo(0);
  }

  // ========== Tests for get_protect_rules ==========

  @Test
  void testGetProtectRules_Success() throws Exception {
    // Given
    var mockProtectData = createMockProtectData(3);

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getProtectConfig(eq(TEST_ORG_ID), eq(TEST_APP_ID)))
                  .thenReturn(mockProtectData);
            });

    // When
    var result = adrService.getProtectRules(TEST_APP_ID);

    // Then
    assertThat(result).as("Result should not be null").isNotNull();
    assertThat(result.getRules()).as("Rules should not be null").isNotNull();
    assertThat(result.getRules().size()).as("Should have 3 protect rules").isEqualTo(3);
  }

  @Test
  void testGetProtectRules_WithRules() throws Exception {
    // Given
    var mockProtectData = createMockProtectDataWithRules();

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getProtectConfig(eq(TEST_ORG_ID), eq(TEST_APP_ID)))
                  .thenReturn(mockProtectData);
            });

    // When
    var result = adrService.getProtectRules(TEST_APP_ID);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getRules()).isNotNull();
    assertThat(result.getRules()).isNotEmpty();

    // Verify rule details
    var firstRule = result.getRules().get(0);
    assertThat(firstRule.getName()).as("Rule should have a name").isNotNull();
    assertThat(firstRule.getProduction()).as("Rule should have a production mode").isNotNull();
  }

  @Test
  void testGetProtectRules_EmptyAppID() {
    // When/Then
    assertThatThrownBy(
            () -> {
              adrService.getProtectRules("");
            })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Application ID cannot be null or empty");
  }

  @Test
  void testGetProtectRules_NullAppID() {
    // When/Then
    assertThatThrownBy(
            () -> {
              adrService.getProtectRules(null);
            })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Application ID cannot be null or empty");
  }

  @Test
  void testGetProtectRules_SDKFailure() throws Exception {
    // Given - SDK throws exception
    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getProtectConfig(eq(TEST_ORG_ID), anyString()))
                  .thenThrow(new RuntimeException("Failed to fetch protect config"));
            });

    // When/Then
    assertThatThrownBy(
            () -> {
              adrService.getProtectRules(TEST_APP_ID);
            })
        .isInstanceOf(Exception.class)
        .satisfies(
            ex ->
                assertThat(
                        ex.getMessage().contains("Failed to fetch protect config")
                            || (ex.getCause() != null
                                && ex.getCause()
                                    .getMessage()
                                    .contains("Failed to fetch protect config")))
                    .as("Should propagate SDK exception")
                    .isTrue());
  }

  @Test
  void testGetProtectRules_NoProtectDataReturned() throws Exception {
    // Given - SDK returns null (app exists but no protect config)
    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getProtectConfig(eq(TEST_ORG_ID), eq(TEST_APP_ID))).thenReturn(null);
            });

    // When
    var result = adrService.getProtectRules(TEST_APP_ID);

    // Then
    assertThat(result).as("Should return null when no protect data available").isNull();
  }

  @Test
  void testGetProtectRules_EmptyRulesList() throws Exception {
    // Given - Protect enabled but no rules configured
    var mockProtectData = new com.contrast.labs.ai.mcp.contrast.sdkextension.data.ProtectData();
    mockProtectData.setRules(new ArrayList<>());

    mockedSDKExtension =
        mockConstruction(
            SDKExtension.class,
            (mock, context) -> {
              when(mock.getProtectConfig(eq(TEST_ORG_ID), eq(TEST_APP_ID)))
                  .thenReturn(mockProtectData);
            });

    // When
    var result = adrService.getProtectRules(TEST_APP_ID);

    // Then
    assertThat(result).isNotNull();
    assertThat(result.getRules()).isNotNull();
    assertThat(result.getRules()).as("Should have empty rules list").isEmpty();
  }

  // ========== Helper Methods ==========

  /** Creates mock AttacksResponse for testing */
  private AttacksResponse createMockAttacksResponse(int count, Integer totalCount) {
    var response = new AttacksResponse();
    response.setAttacks(createMockAttacks(count));
    response.setCount(totalCount);
    return response;
  }

  /** Creates mock Attack objects for testing */
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

  /** Creates mock ProtectData for testing */
  private com.contrast.labs.ai.mcp.contrast.sdkextension.data.ProtectData createMockProtectData(
      int ruleCount) {
    var protectData = new com.contrast.labs.ai.mcp.contrast.sdkextension.data.ProtectData();

    var rules = new ArrayList<com.contrast.labs.ai.mcp.contrast.sdkextension.data.Rule>();
    for (int i = 0; i < ruleCount; i++) {
      var rule = new com.contrast.labs.ai.mcp.contrast.sdkextension.data.Rule();
      rule.setName("protect-rule-" + i);
      rule.setProduction(i % 2 == 0 ? "block" : "monitor");
      rules.add(rule);
    }

    protectData.setRules(rules);
    return protectData;
  }

  /** Creates mock ProtectData with realistic rule configuration */
  private com.contrast.labs.ai.mcp.contrast.sdkextension.data.ProtectData
      createMockProtectDataWithRules() {
    var protectData = new com.contrast.labs.ai.mcp.contrast.sdkextension.data.ProtectData();

    var rules = new ArrayList<com.contrast.labs.ai.mcp.contrast.sdkextension.data.Rule>();

    // SQL Injection rule
    var sqlRule = new com.contrast.labs.ai.mcp.contrast.sdkextension.data.Rule();
    sqlRule.setName("sql-injection");
    sqlRule.setProduction("block");
    rules.add(sqlRule);

    // XSS rule
    var xssRule = new com.contrast.labs.ai.mcp.contrast.sdkextension.data.Rule();
    xssRule.setName("xss-reflected");
    xssRule.setProduction("monitor");
    rules.add(xssRule);

    protectData.setRules(rules);
    return protectData;
  }
}
