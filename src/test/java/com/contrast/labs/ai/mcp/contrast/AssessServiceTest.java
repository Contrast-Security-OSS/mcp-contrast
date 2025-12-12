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
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.contrast.labs.ai.mcp.contrast.data.PaginatedResponse;
import com.contrast.labs.ai.mcp.contrast.data.VulnLight;
import com.contrast.labs.ai.mcp.contrast.mapper.VulnerabilityMapper;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.utils.PaginationHandler;
import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.http.TraceFilterForm;
import com.contrastsecurity.models.MetadataFilterResponse;
import com.contrastsecurity.models.MetadataItem;
import com.contrastsecurity.models.Rules;
import com.contrastsecurity.models.SessionMetadata;
import com.contrastsecurity.models.Trace;
import com.contrastsecurity.models.TraceFilterBody;
import com.contrastsecurity.models.Traces;
import com.contrastsecurity.sdk.ContrastSDK;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Comprehensive test suite for AssessService pagination implementation. Tests all 11 required
 * pagination scenarios from pagination-spec-v1.0.md
 */
@ExtendWith(MockitoExtension.class)
class AssessServiceTest {

  private AssessService assessService;

  @Mock private ContrastSDK mockContrastSDK;

  @Mock private PaginationHandler mockPaginationHandler;

  private VulnerabilityMapper vulnerabilityMapper;

  private MockedStatic<SDKHelper> mockedSDKHelper;

  private static final String TEST_ORG_ID = "test-org-123";
  private static final String TEST_HOST = "https://test.contrast.local";
  private static final String TEST_API_KEY = "test-api-key";
  private static final String TEST_SERVICE_KEY = "test-service-key";
  private static final String TEST_USERNAME = "test-user";
  private static final String TEST_APP_ID = "test-app-id-123";

  // Named constants for test timestamps
  private static final long JAN_15_2025_10_30_UTC =
      LocalDateTime.of(2025, 1, 15, 10, 30)
          .toInstant(ZoneOffset.UTC)
          .toEpochMilli(); // 1736938200000L
  private static final long JAN_1_2024_00_00_UTC =
      LocalDateTime.of(2024, 1, 1, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli(); // 1704067200000L
  private static final long FEB_19_2025_13_20_UTC =
      LocalDateTime.of(2025, 2, 19, 13, 20)
          .toInstant(ZoneOffset.UTC)
          .toEpochMilli(); // 1740000000000L

  @BeforeEach
  void setUp() throws Exception {
    // Create real VulnerabilityMapper, mock PaginationHandler
    vulnerabilityMapper = new VulnerabilityMapper();

    // Create AssessService with real mapper and mocked pagination handler
    assessService = new AssessService(vulnerabilityMapper, mockPaginationHandler);

    // Setup simplified mock behavior for PaginationHandler
    // PaginationHandler logic is tested in its own test class
    lenient()
        .when(
            mockPaginationHandler.createPaginatedResponse(
                anyList(), any(PaginationParams.class), any(), anyList()))
        .thenAnswer(
            invocation -> {
              List<?> items = invocation.getArgument(0);
              PaginationParams params = invocation.getArgument(1);
              Integer totalItems = invocation.getArgument(2);
              // Return simple response - pagination logic tested in PaginationHandlerTest
              return new PaginatedResponse<>(
                  items, params.page(), params.pageSize(), totalItems, false, List.of(), List.of());
            });

    // Mock the static SDKHelper.getSDK() method
    mockedSDKHelper = mockStatic(SDKHelper.class);
    mockedSDKHelper
        .when(
            () ->
                SDKHelper.getSDK(
                    anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
        .thenReturn(mockContrastSDK);

    // Set required configuration fields using reflection
    ReflectionTestUtils.setField(assessService, "orgID", TEST_ORG_ID);
    ReflectionTestUtils.setField(assessService, "hostName", TEST_HOST);
    ReflectionTestUtils.setField(assessService, "apiKey", TEST_API_KEY);
    ReflectionTestUtils.setField(assessService, "serviceKey", TEST_SERVICE_KEY);
    ReflectionTestUtils.setField(assessService, "userName", TEST_USERNAME);
    ReflectionTestUtils.setField(assessService, "httpProxyHost", "");
    ReflectionTestUtils.setField(assessService, "httpProxyPort", "");
  }

  @AfterEach
  void tearDown() {
    // Close the static mock
    if (mockedSDKHelper != null) {
      mockedSDKHelper.close();
    }
  }

  // ========== SDK Integration Tests ==========

  @Test
  void testSearchVulnerabilities_PassesCorrectParametersToSDK() throws Exception {
    // Given
    var mockTraces = createMockTraces(50, 150);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // When
    assessService.searchVulnerabilities(2, 75, null, null, null, null, null, null, null);

    // Then - Verify SDK received correct parameters
    var captor = ArgumentCaptor.forClass(TraceFilterForm.class);
    verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

    var form = captor.getValue();
    assertThat(form.getOffset()).isEqualTo(75); // (page 2 - 1) * 75
    assertThat(form.getLimit()).isEqualTo(75);
  }

  @Test
  void testSearchVulnerabilities_SetsExpandParametersCorrectly() throws Exception {
    // Given - Test that SESSION_METADATA and SERVER_ENVIRONMENTS expand are set
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // When
    assessService.searchVulnerabilities(1, 50, null, null, null, null, null, null, null);

    // Then - Verify expand parameters include SESSION_METADATA and SERVER_ENVIRONMENTS
    var captor = ArgumentCaptor.forClass(TraceFilterForm.class);
    verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

    var form = captor.getValue();
    assertThat(form.getExpand()).isNotNull();
    assertThat(form.getExpand())
        .as("Expand should include SESSION_METADATA")
        .contains(TraceFilterForm.TraceExpandValue.SESSION_METADATA);
    assertThat(form.getExpand())
        .as("Expand should include SERVER_ENVIRONMENTS")
        .contains(TraceFilterForm.TraceExpandValue.SERVER_ENVIRONMENTS);
  }

  @Test
  void testSearchVulnerabilities_CallsPaginationHandlerCorrectly() throws Exception {
    // Given
    var mockTraces = createMockTraces(50, 150);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // When
    assessService.searchVulnerabilities(1, 50, null, null, null, null, null, null, null);

    // Then - Verify PaginationHandler received correct arguments
    verify(mockPaginationHandler)
        .createPaginatedResponse(
            argThat(list -> list.size() == 50), // items
            argThat(p -> p.page() == 1 && p.pageSize() == 50), // params
            eq(150), // totalItems
            anyList() // warnings
            );
  }

  // ========== Routing Tests ==========

  @Test
  void testSearchVulnerabilities_AlwaysUsesOrgAPI() throws Exception {
    // Given
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // When
    assessService.searchVulnerabilities(1, 50, null, null, null, null, null, null, null);

    // Then - Verify org-level API was used (no app-specific routing)
    verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class));
    verify(mockContrastSDK, never()).getTraces(any(), any(), any(TraceFilterForm.class));
  }

  // ========== Empty Results Tests ==========

  @Test
  void testSearchVulnerabilities_EmptyResults_PassesEmptyListToPaginationHandler()
      throws Exception {
    // Given: SDK returns empty Traces (0 vulnerabilities)
    var emptyTraces = createMockTraces(0, 0);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(emptyTraces);

    // Mock PaginationHandler to return "No items found." warning like the real implementation
    when(mockPaginationHandler.createPaginatedResponse(
            anyList(), any(PaginationParams.class), any(), anyList()))
        .thenAnswer(
            invocation -> {
              var items = invocation.getArgument(0, List.class);
              var params = invocation.getArgument(1, PaginationParams.class);
              var totalItems = invocation.getArgument(2, Integer.class);
              // Real PaginationHandler returns "No items found." for empty page 1 results
              List<String> warnings =
                  items.isEmpty() && params.page() == 1 ? List.of("No items found.") : List.of();
              return new PaginatedResponse<>(
                  items, params.page(), params.pageSize(), totalItems, false, List.of(), warnings);
            });

    // When
    var result =
        assessService.searchVulnerabilities(1, 50, null, null, null, null, null, null, null);

    // Then: Verify empty list was passed to PaginationHandler
    verify(mockPaginationHandler)
        .createPaginatedResponse(
            argThat(list -> list.isEmpty()), // empty items list
            argThat(p -> p.page() == 1 && p.pageSize() == 50), // params
            eq(0), // totalItems
            anyList() // warnings
            );

    // Verify result contains helpful message for AI
    assertThat(result).isNotNull();
    assertThat(result.items()).isEmpty();
    assertThat(result.message()).as("Empty results should have explanatory message").isNotNull();
    assertThat(result.message())
        .as("Message should explain empty results to AI")
        .isEqualTo("No items found.");
  }

  // ========== get_session_metadata Tests ==========

  @Test
  void getSessionMetadata_should_return_metadata_when_valid_appId() throws Exception {
    var mockResponse = mock(MetadataFilterResponse.class);
    when(mockContrastSDK.getSessionMetadataForApplication(TEST_ORG_ID, TEST_APP_ID, null))
        .thenReturn(mockResponse);

    var result = assessService.getSessionMetadata(TEST_APP_ID);

    assertThat(result).isEqualTo(mockResponse);
    verify(mockContrastSDK).getSessionMetadataForApplication(TEST_ORG_ID, TEST_APP_ID, null);
  }

  @Test
  void getSessionMetadata_should_handle_null_appId() throws Exception {
    assessService.getSessionMetadata(null);

    verify(mockContrastSDK).getSessionMetadataForApplication(TEST_ORG_ID, null, null);
  }

  @Test
  void getSessionMetadata_should_handle_empty_appId() throws Exception {
    assessService.getSessionMetadata("");

    verify(mockContrastSDK).getSessionMetadataForApplication(TEST_ORG_ID, "", null);
  }

  @Test
  void getSessionMetadata_should_propagate_IOException_from_sdk() throws Exception {
    when(mockContrastSDK.getSessionMetadataForApplication(anyString(), anyString(), any()))
        .thenThrow(new IOException("SDK error"));

    assertThatThrownBy(() -> assessService.getSessionMetadata(TEST_APP_ID))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("SDK error");
  }

  @Test
  void getSessionMetadata_should_propagate_UnauthorizedException_from_sdk() throws Exception {
    // UnauthorizedException requires constructor params, so mock it
    var mockException = mock(UnauthorizedException.class);
    when(mockException.getMessage()).thenReturn("Unauthorized access");

    when(mockContrastSDK.getSessionMetadataForApplication(anyString(), anyString(), any()))
        .thenThrow(mockException);

    assertThatThrownBy(() -> assessService.getSessionMetadata(TEST_APP_ID))
        .isInstanceOf(UnauthorizedException.class)
        .hasMessage("Unauthorized access");
  }

  // ========== Helper Methods ==========

  /**
   * Creates a mock Traces object with the specified number of traces
   *
   * @param traceCount Number of traces to create
   * @param totalCount Total count to set (null if not available)
   */
  private Traces createMockTraces(int traceCount, Integer totalCount) {
    var mockTraces = new Traces();
    var traces = new ArrayList<Trace>();

    for (int i = 0; i < traceCount; i++) {
      Trace trace =
          AnonymousTraceBuilder.validTrace()
              .withTitle("Test Vulnerability " + i)
              .withRule("test-rule-" + i)
              .withUuid("uuid-" + i)
              .withSeverity("HIGH")
              .withLastTimeSeen(System.currentTimeMillis())
              .withStatus("REPORTED")
              .withFirstTimeSeen(System.currentTimeMillis() - 86400000L)
              .withClosedTime(null)
              .build();
      traces.add(trace);
    }

    // Use reflection to set private fields since Traces doesn't have setters
    try {
      var tracesField = Traces.class.getDeclaredField("traces");
      tracesField.setAccessible(true);
      tracesField.set(mockTraces, traces);

      var countField = Traces.class.getDeclaredField("count");
      countField.setAccessible(true);
      countField.set(mockTraces, totalCount);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create mock Traces", e);
    }

    return mockTraces;
  }

  // ========== List Vulnerability Types Tests ==========

  @Test
  void testListVulnerabilityTypes_Success() throws Exception {
    // Arrange
    var mockRules =
        createMockRules(
            "sql-injection", "xss-reflected", "path-traversal", "cmd-injection", "crypto-bad-mac");
    when(mockContrastSDK.getRules(TEST_ORG_ID)).thenReturn(mockRules);

    // Act
    var result = assessService.listVulnerabilityTypes();

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(5);

    // Verify sorted alphabetically
    assertThat(result.get(0)).isEqualTo("cmd-injection");
    assertThat(result.get(1)).isEqualTo("crypto-bad-mac");
    assertThat(result.get(2)).isEqualTo("path-traversal");
    assertThat(result.get(3)).isEqualTo("sql-injection");
    assertThat(result.get(4)).isEqualTo("xss-reflected");

    verify(mockContrastSDK).getRules(TEST_ORG_ID);
  }

  @Test
  void testListVulnerabilityTypes_EmptyRules() throws Exception {
    // Arrange - SDK returns empty Rules object
    var emptyRules = new Rules();
    when(mockContrastSDK.getRules(TEST_ORG_ID)).thenReturn(emptyRules);

    // Act
    var result = assessService.listVulnerabilityTypes();

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).as("Should return empty list when no rules available").isEmpty();
    verify(mockContrastSDK).getRules(TEST_ORG_ID);
  }

  @Test
  void testListVulnerabilityTypes_NullRulesObject() throws Exception {
    // Arrange - SDK returns null
    when(mockContrastSDK.getRules(TEST_ORG_ID)).thenReturn(null);

    // Act
    var result = assessService.listVulnerabilityTypes();

    // Assert
    assertThat(result).isNotNull();
    assertThat(result).as("Should return empty list when Rules object is null").isEmpty();
    verify(mockContrastSDK).getRules(TEST_ORG_ID);
  }

  @Test
  void testListVulnerabilityTypes_FiltersNullAndEmptyNames() throws Exception {
    // Arrange - Mix of valid, null, and empty names
    var mockRules =
        createMockRulesWithNulls(
            "sql-injection",
            null,
            "xss-reflected",
            "",
            "path-traversal",
            "   ", // whitespace only - will be trimmed to empty
            "cmd-injection");
    when(mockContrastSDK.getRules(TEST_ORG_ID)).thenReturn(mockRules);

    // Act
    var result = assessService.listVulnerabilityTypes();

    // Assert
    assertThat(result).isNotNull();
    // Should only have the 4 valid names (whitespace-only gets trimmed to empty and filtered)
    assertThat(result.size()).isEqualTo(4);
    assertThat(result).contains("sql-injection");
    assertThat(result).contains("xss-reflected");
    assertThat(result).contains("path-traversal");
    assertThat(result).contains("cmd-injection");

    // Verify sorted
    assertThat(result.get(0)).isEqualTo("cmd-injection");
    assertThat(result.get(1)).isEqualTo("path-traversal");
    assertThat(result.get(2)).isEqualTo("sql-injection");
    assertThat(result.get(3)).isEqualTo("xss-reflected");
  }

  @Test
  void testListVulnerabilityTypes_SDKThrowsException() throws Exception {
    // Arrange
    when(mockContrastSDK.getRules(TEST_ORG_ID))
        .thenThrow(new RuntimeException("API connection failed"));

    // Act & Assert
    assertThatThrownBy(
            () -> {
              assessService.listVulnerabilityTypes();
            })
        .isInstanceOf(Exception.class)
        .hasMessageContaining("Failed to retrieve vulnerability types");
    verify(mockContrastSDK).getRules(TEST_ORG_ID);
  }

  @Test
  void testListVulnerabilityTypes_LargeRuleSet() throws Exception {
    // Arrange - Test with many rules to verify performance
    var ruleNames = new ArrayList<String>();
    for (int i = 0; i < 100; i++) {
      ruleNames.add("test-rule-" + i);
    }
    var mockRules = createMockRules(ruleNames.toArray(new String[0]));
    when(mockContrastSDK.getRules(TEST_ORG_ID)).thenReturn(mockRules);

    // Act
    var result = assessService.listVulnerabilityTypes();

    // Assert
    assertThat(result).isNotNull();
    assertThat(result.size()).isEqualTo(100);

    // Verify still sorted
    for (int i = 0; i < result.size() - 1; i++) {
      assertThat(result.get(i).compareTo(result.get(i + 1)))
          .as("Rules should be sorted alphabetically")
          .isLessThan(0);
    }
  }

  /** Creates a mock Rules object with the specified rule names */
  private Rules createMockRules(String... ruleNames) {
    var rules = new Rules();
    var ruleList = new ArrayList<Rules.Rule>();

    for (var name : ruleNames) {
      var rule = rules.new Rule();
      // Use reflection to set the name since Rule doesn't have setters
      try {
        var nameField = Rules.Rule.class.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(rule, name);
      } catch (Exception e) {
        throw new RuntimeException("Failed to create mock Rule", e);
      }
      ruleList.add(rule);
    }

    // Set the rules list using reflection
    try {
      var rulesField = Rules.class.getDeclaredField("rules");
      rulesField.setAccessible(true);
      rulesField.set(rules, ruleList);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set rules list", e);
    }

    return rules;
  }

  /** Creates a mock Rules object that includes null/empty names for testing filtering */
  private Rules createMockRulesWithNulls(String... ruleNames) {
    var rules = new Rules();
    var ruleList = new ArrayList<Rules.Rule>();

    for (var name : ruleNames) {
      var rule = rules.new Rule();
      // Use reflection to set the name (including nulls and empty strings)
      try {
        var nameField = Rules.Rule.class.getDeclaredField("name");
        nameField.setAccessible(true);
        nameField.set(rule, name);
      } catch (Exception e) {
        throw new RuntimeException("Failed to create mock Rule", e);
      }
      ruleList.add(rule);
    }

    // Set the rules list using reflection
    try {
      var rulesField = Rules.class.getDeclaredField("rules");
      rulesField.setAccessible(true);
      rulesField.set(rules, ruleList);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set rules list", e);
    }

    return rules;
  }

  // ========== Filter Tests ==========

  @Test
  void testSearchVulnerabilities_SeverityFilter() throws Exception {
    // Arrange
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Act
    var response =
        assessService.searchVulnerabilities(
            1, 50, "CRITICAL, HIGH", null, null, null, null, null, null);

    // Assert
    assertThat(response).isNotNull();
    var captor = ArgumentCaptor.forClass(TraceFilterForm.class);
    verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

    var form = captor.getValue();
    assertThat(form.getSeverities()).isNotNull();
    assertThat(form.getSeverities().size()).isEqualTo(2);
  }

  @Test
  void testSearchVulnerabilities_InvalidSeverity_HardFailure() throws Exception {
    // Act - Invalid severity causes hard failure, SDK should not be called
    var response =
        assessService.searchVulnerabilities(
            1, 50, "CRITICAL, SUPER_HIGH", null, null, null, null, null, null);

    // Assert - Hard failure returns error response with empty items
    assertThat(response).isNotNull();
    assertThat(response.items()).as("Hard failure should return empty items").isEmpty();
    assertThat(response.page()).isEqualTo(1);
    assertThat(response.pageSize()).isEqualTo(50);
    assertThat(response.totalItems()).isEqualTo(0);

    assertThat(response.message()).isNotNull();
    assertThat(response.message()).contains("Invalid severity 'SUPER_HIGH'");
    assertThat(response.message()).contains("Valid: CRITICAL, HIGH, MEDIUM, LOW, NOTE");

    // Verify SDK was NOT called (hard failure stops execution)
    verify(mockContrastSDK, never()).getTracesInOrg(any(), any());
  }

  @Test
  void testSearchVulnerabilities_StatusSmartDefaults() throws Exception {
    // Arrange
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Act - no status provided, should use smart defaults
    var response =
        assessService.searchVulnerabilities(1, 50, null, null, null, null, null, null, null);

    // Assert
    assertThat(response).isNotNull();
    var captor = ArgumentCaptor.forClass(TraceFilterForm.class);
    verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

    var form = captor.getValue();
    assertThat(form.getStatus()).isNotNull();
    assertThat(form.getStatus().size()).isEqualTo(3);
    assertThat(form.getStatus()).contains("Reported");
    assertThat(form.getStatus()).contains("Suspicious");
    assertThat(form.getStatus()).contains("Confirmed");

    // Message content is tested in VulnerabilityFilterParamsTest
  }

  @Test
  void testSearchVulnerabilities_StatusExplicitOverride() throws Exception {
    // Arrange
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Act - explicitly provide statuses (including Fixed and Remediated)
    var response =
        assessService.searchVulnerabilities(
            1, 50, null, "Reported,Fixed,Remediated", null, null, null, null, null);

    // Assert
    assertThat(response).isNotNull();
    var captor = ArgumentCaptor.forClass(TraceFilterForm.class);
    verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

    var form = captor.getValue();
    assertThat(form.getStatus()).isNotNull();
    assertThat(form.getStatus().size()).isEqualTo(3);
    assertThat(form.getStatus()).contains("Reported");
    assertThat(form.getStatus()).contains("Fixed");
    assertThat(form.getStatus()).contains("Remediated");
  }

  @Test
  void testSearchVulnerabilities_VulnTypesFilter() throws Exception {
    // Arrange
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Act
    var response =
        assessService.searchVulnerabilities(
            1, 50, null, null, "sql-injection, xss-reflected", null, null, null, null);

    // Assert
    assertThat(response).isNotNull();
    var captor = ArgumentCaptor.forClass(TraceFilterForm.class);
    verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

    var form = captor.getValue();
    assertThat(form.getVulnTypes()).isNotNull();
    assertThat(form.getVulnTypes().size()).isEqualTo(2);
    assertThat(form.getVulnTypes()).contains("sql-injection");
    assertThat(form.getVulnTypes()).contains("xss-reflected");
  }

  @Test
  void testSearchVulnerabilities_EnvironmentFilter() throws Exception {
    // Arrange
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Act
    var response =
        assessService.searchVulnerabilities(
            1, 50, null, null, null, "PRODUCTION, QA", null, null, null);

    // Assert
    assertThat(response).isNotNull();
    var captor = ArgumentCaptor.forClass(TraceFilterForm.class);
    verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

    var form = captor.getValue();
    assertThat(form.getEnvironments()).isNotNull();
    assertThat(form.getEnvironments().size()).isEqualTo(2);
  }

  @Test
  void testSearchVulnerabilities_DateFilterValid() throws Exception {
    // Arrange
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Act
    var response =
        assessService.searchVulnerabilities(
            1, 50, null, null, null, null, "2025-01-01", "2025-12-31", null);

    // Assert
    assertThat(response).isNotNull();
    var captor = ArgumentCaptor.forClass(TraceFilterForm.class);
    verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

    var form = captor.getValue();
    assertThat(form.getStartDate()).isNotNull();
    assertThat(form.getEndDate()).isNotNull();

    // Message content is tested in VulnerabilityFilterParamsTest
  }

  @Test
  void testSearchVulnerabilities_VulnTagsFilter() throws Exception {
    // Arrange
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Act
    var response =
        assessService.searchVulnerabilities(
            1, 50, null, null, null, null, null, null, "SmartFix Remediated,reviewed");

    // Assert
    assertThat(response).isNotNull();
    var captor = ArgumentCaptor.forClass(TraceFilterForm.class);
    verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

    var form = captor.getValue();
    assertThat(form.getFilterTags()).isNotNull();
    assertThat(form.getFilterTags().size()).isEqualTo(2);
    // SDK now handles URL encoding (AIML-193 complete) - tags passed through as-is
    assertThat(form.getFilterTags()).contains("SmartFix Remediated");
    assertThat(form.getFilterTags()).contains("reviewed");
  }

  @Test
  void testSearchVulnerabilities_MultipleFilters() throws Exception {
    // Arrange
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Act - combine severity, status, vulnTypes, and environment
    var response =
        assessService.searchVulnerabilities(
            1,
            50,
            "CRITICAL, HIGH",
            "Confirmed",
            "sql-injection, cmd-injection",
            "PRODUCTION",
            "2025-01-01",
            null,
            null);

    // Assert
    assertThat(response).isNotNull();
    var captor = ArgumentCaptor.forClass(TraceFilterForm.class);
    verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

    var form = captor.getValue();
    assertThat(form.getSeverities()).isNotNull();
    assertThat(form.getStatus()).isNotNull();
    assertThat(form.getVulnTypes()).isNotNull();
    assertThat(form.getEnvironments()).isNotNull();
    assertThat(form.getStartDate()).isNotNull();
  }

  @Test
  void testSearchVulnerabilities_WhitespaceInFilters() throws Exception {
    // Arrange
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Act - test whitespace handling: "CRITICAL , HIGH" instead of "CRITICAL,HIGH"
    var response =
        assessService.searchVulnerabilities(
            1, 50, "CRITICAL , HIGH", null, null, null, null, null, null);

    // Assert
    assertThat(response).isNotNull();
    var captor = ArgumentCaptor.forClass(TraceFilterForm.class);
    verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

    var form = captor.getValue();
    assertThat(form.getSeverities()).isNotNull();
    assertThat(form.getSeverities().size()).isEqualTo(2);
  }

  @Test
  void testSearchVulnerabilities_EnvironmentsInResponse() throws Exception {
    // Arrange - Create traces with servers that have different environments
    var mockTraces = new Traces();
    var traces = new ArrayList<Trace>();

    // Trace 1: Multiple servers with different environments
    Trace trace1 =
        AnonymousTraceBuilder.validTrace()
            .withTitle("SQL Injection")
            .withRule("sql-injection")
            .withUuid("uuid-1")
            .withSeverity("HIGH")
            .withLastTimeSeen(System.currentTimeMillis())
            .withStatus("REPORTED")
            .withFirstTimeSeen(System.currentTimeMillis() - 86400000L)
            .withClosedTime(null)
            .withServerEnvironments(
                List.of("PRODUCTION", "QA", "PRODUCTION")) // Duplicate - should be deduplicated
            .withTags(new ArrayList<>())
            .build();
    traces.add(trace1);

    // Trace 2: No servers
    Trace trace2 =
        AnonymousTraceBuilder.validTrace()
            .withTitle("XSS")
            .withRule("xss-reflected")
            .withUuid("uuid-2")
            .withSeverity("MEDIUM")
            .withLastTimeSeen(System.currentTimeMillis())
            .withStatus("REPORTED")
            .withFirstTimeSeen(System.currentTimeMillis() - 86400000L)
            .withClosedTime(null)
            .withServerEnvironments(new ArrayList<>())
            .withTags(new ArrayList<>())
            .build();
    traces.add(trace2);

    // Trace 3: Single server with one environment
    Trace trace3 =
        AnonymousTraceBuilder.validTrace()
            .withTitle("Path Traversal")
            .withRule("path-traversal")
            .withUuid("uuid-3")
            .withSeverity("CRITICAL")
            .withLastTimeSeen(System.currentTimeMillis())
            .withStatus("CONFIRMED")
            .withFirstTimeSeen(System.currentTimeMillis() - 172800000L)
            .withClosedTime(null)
            .withServerEnvironments(List.of("DEVELOPMENT"))
            .withTags(new ArrayList<>())
            .build();
    traces.add(trace3);

    // Set up mockTraces
    try {
      var tracesField = Traces.class.getDeclaredField("traces");
      tracesField.setAccessible(true);
      tracesField.set(mockTraces, traces);

      var countField = Traces.class.getDeclaredField("count");
      countField.setAccessible(true);
      countField.set(mockTraces, 3);
    } catch (Exception e) {
      fail("Failed to setup mock traces: " + e.getMessage());
    }

    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Act
    var response =
        assessService.searchVulnerabilities(1, 50, null, null, null, null, null, null, null);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.items()).hasSize(3);

    // Verify trace 1: Multiple environments, deduplicated and sorted
    var vuln1 = response.items().get(0);
    assertThat(vuln1.title()).isEqualTo("SQL Injection");
    assertThat(vuln1.environments()).isNotNull();
    assertThat(vuln1.environments()).hasSize(2);
    assertThat(vuln1.environments()).contains("PRODUCTION");
    assertThat(vuln1.environments()).contains("QA");
    // Verify sorted order
    assertThat(vuln1.environments().get(0)).isEqualTo("PRODUCTION");
    assertThat(vuln1.environments().get(1)).isEqualTo("QA");

    // Verify trace 2: No servers = empty environments
    var vuln2 = response.items().get(1);
    assertThat(vuln2.title()).isEqualTo("XSS");
    assertThat(vuln2.environments()).isNotNull();
    assertThat(vuln2.environments()).hasSize(0);

    // Verify trace 3: Single environment
    var vuln3 = response.items().get(2);
    assertThat(vuln3.title()).isEqualTo("Path Traversal");
    assertThat(vuln3.environments()).isNotNull();
    assertThat(vuln3.environments()).hasSize(1);
    assertThat(vuln3.environments().get(0)).isEqualTo("DEVELOPMENT");
  }

  @Test
  void testVulnLight_TimestampFields_ISO8601Format() throws Exception {
    // Arrange - Create trace with known timestamp values
    var lastSeen = JAN_15_2025_10_30_UTC;
    var firstSeen = JAN_1_2024_00_00_UTC;
    var closed = FEB_19_2025_13_20_UTC;

    Trace trace =
        AnonymousTraceBuilder.validTrace()
            .withTitle("Test Vulnerability")
            .withRule("test-rule")
            .withUuid("test-uuid-123")
            .withSeverity("HIGH")
            .withStatus("Reported")
            .withLastTimeSeen(lastSeen)
            .withFirstTimeSeen(firstSeen)
            .withClosedTime(closed)
            .withServerEnvironments(new ArrayList<>())
            .withTags(new ArrayList<>())
            .build();

    Traces mockTraces = mock();
    when(mockTraces.getTraces()).thenReturn(List.of(trace));
    when(mockTraces.getCount()).thenReturn(1);

    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Act
    var response =
        assessService.searchVulnerabilities(1, 50, null, null, null, null, null, null, null);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.items().size()).isEqualTo(1);

    var vuln = response.items().get(0);

    // Verify field names use *At convention
    assertThat(vuln.lastSeenAt()).as("lastSeenAt field should exist").isNotNull();
    assertThat(vuln.firstSeenAt()).as("firstSeenAt field should exist").isNotNull();
    assertThat(vuln.closedAt()).as("closedAt field should exist").isNotNull();

    // Verify ISO 8601 format with timezone offset (YYYY-MM-DDTHH:MM:SS+/-HH:MM)
    var iso8601Pattern = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}";
    assertThat(vuln.lastSeenAt())
        .as("lastSeenAt should be ISO 8601 with timezone: " + vuln.lastSeenAt())
        .matches(iso8601Pattern);
    assertThat(vuln.firstSeenAt())
        .as("firstSeenAt should be ISO 8601 with timezone: " + vuln.firstSeenAt())
        .matches(iso8601Pattern);
    assertThat(vuln.closedAt())
        .as("closedAt should be ISO 8601 with timezone: " + vuln.closedAt())
        .matches(iso8601Pattern);

    // Verify timestamps include timezone offset
    assertThat(vuln.lastSeenAt().contains("+") || vuln.lastSeenAt().contains("-"))
        .as("lastSeenAt should include timezone offset")
        .isTrue();
    assertThat(vuln.firstSeenAt().contains("+") || vuln.firstSeenAt().contains("-"))
        .as("firstSeenAt should include timezone offset")
        .isTrue();
    assertThat(vuln.closedAt().contains("+") || vuln.closedAt().contains("-"))
        .as("closedAt should include timezone offset")
        .isTrue();
  }

  @Test
  void testVulnLight_TimestampFields_NullHandling() throws Exception {
    // Arrange - Create trace with null timestamps
    Trace trace =
        AnonymousTraceBuilder.validTrace()
            .withTitle("Test Vulnerability")
            .withRule("test-rule")
            .withUuid("test-uuid-123")
            .withSeverity("HIGH")
            .withStatus("Reported")
            .withLastTimeSeen(JAN_15_2025_10_30_UTC) // lastSeen is required
            .withFirstTimeSeen(null) // optional
            .withClosedTime(null) // optional
            .withServerEnvironments(new ArrayList<>())
            .withTags(new ArrayList<>())
            .build();

    Traces mockTraces = mock();
    when(mockTraces.getTraces()).thenReturn(List.of(trace));
    when(mockTraces.getCount()).thenReturn(1);

    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Act
    var response =
        assessService.searchVulnerabilities(1, 50, null, null, null, null, null, null, null);

    // Assert
    assertThat(response).isNotNull();
    assertThat(response.items().size()).isEqualTo(1);

    var vuln = response.items().get(0);

    // Verify null timestamps are handled correctly
    assertThat(vuln.lastSeenAt()).as("lastSeenAt should always be present").isNotNull();
    assertThat(vuln.firstSeenAt()).as("firstSeenAt should be null when not set").isNull();
    assertThat(vuln.closedAt()).as("closedAt should be null when not set").isNull();
  }

  // ==================== search_applications Tests ====================

  @Test
  void search_applications_should_validate_metadataValue_requires_metadataName() {
    // Act & Assert - verify parameter validation
    assertThatThrownBy(() -> assessService.search_applications(null, null, null, "orphan-value"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("metadataValue requires metadataName");
  }

  @Test
  void search_applications_should_return_all_when_no_filters() throws IOException {
    // Arrange
    com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Application app1 = mock();
    when(app1.getName()).thenReturn("App1");
    when(app1.getStatus()).thenReturn("ACTIVE");
    when(app1.getAppId()).thenReturn("app-1");
    when(app1.getLastSeen()).thenReturn(1000L);
    when(app1.getLanguage()).thenReturn("Java");
    when(app1.getTags()).thenReturn(List.of());
    when(app1.getTechs()).thenReturn(List.of());
    when(app1.getMetadataEntities()).thenReturn(List.of());

    com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Application app2 = mock();
    when(app2.getName()).thenReturn("App2");
    when(app2.getStatus()).thenReturn("ACTIVE");
    when(app2.getAppId()).thenReturn("app-2");
    when(app2.getLastSeen()).thenReturn(2000L);
    when(app2.getLanguage()).thenReturn("Python");
    when(app2.getTags()).thenReturn(List.of());
    when(app2.getTechs()).thenReturn(List.of());
    when(app2.getMetadataEntities()).thenReturn(List.of());

    mockedSDKHelper
        .when(() -> SDKHelper.getApplicationsWithCache(anyString(), any()))
        .thenReturn(List.of(app1, app2));

    // Act - no filters provided
    var result = assessService.search_applications(null, null, null, null);

    // Assert - returns all applications
    assertThat(result).hasSize(2);
    assertThat(result.get(0).name()).isEqualTo("App1");
    assertThat(result.get(1).name()).isEqualTo("App2");
  }

  @Test
  void search_applications_should_filter_by_name_partial_case_insensitive() throws IOException {
    // Arrange - only specify names, everything else is valid defaults
    var app1 = AnonymousApplicationBuilder.validApp().withName("MyProductionApp").build();
    var app2 = AnonymousApplicationBuilder.validApp().withName("TestingApp").build();

    mockedSDKHelper
        .when(() -> SDKHelper.getApplicationsWithCache(anyString(), any()))
        .thenReturn(List.of(app1, app2));

    // Act - search with lowercase "prod" should match "MyProductionApp"
    var result = assessService.search_applications("prod", null, null, null);

    // Assert - partial, case-insensitive match
    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("MyProductionApp");
  }

  @Test
  void search_applications_should_filter_by_name_no_matches() throws IOException {
    // Arrange - name doesn't match search term
    var app = AnonymousApplicationBuilder.validApp().withName("MyApp").build();

    mockedSDKHelper
        .when(() -> SDKHelper.getApplicationsWithCache(anyString(), any()))
        .thenReturn(List.of(app));

    // Act - search for non-existent name
    var result = assessService.search_applications("nonexistent", null, null, null);

    // Assert - empty result
    assertThat(result).isEmpty();
  }

  @Test
  void search_applications_should_filter_by_tag_exact_case_sensitive() throws IOException {
    // Arrange - demonstrate case-sensitive tag matching
    var app1 =
        AnonymousApplicationBuilder.validApp().withName("App1").withTag("Production").build();
    var app2 =
        AnonymousApplicationBuilder.validApp()
            .withTag("production")
            .build(); // lowercase doesn't match

    mockedSDKHelper
        .when(() -> SDKHelper.getApplicationsWithCache(anyString(), any()))
        .thenReturn(List.of(app1, app2));

    // Act - search with "Production" (capital P)
    var result = assessService.search_applications(null, "Production", null, null);

    // Assert - only exact case match (case-sensitive!)
    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("App1");
  }

  @Test
  void search_applications_should_handle_untagged_apps_when_filtering_by_tag() throws IOException {
    // Arrange - mix of tagged and untagged apps (never null collections pattern)
    var taggedApp =
        AnonymousApplicationBuilder.validApp().withName("TaggedApp").withTag("Production").build();
    var untaggedApp =
        AnonymousApplicationBuilder.validApp()
            .withName("UntaggedApp")
            .withTags(null)
            .build(); // null converted to empty list

    mockedSDKHelper
        .when(() -> SDKHelper.getApplicationsWithCache(anyString(), any()))
        .thenReturn(List.of(taggedApp, untaggedApp));

    // Act - filter by tag
    var result = assessService.search_applications(null, "Production", null, null);

    // Assert - only tagged app matches, no NPE on untagged app
    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("TaggedApp");
  }

  @Test
  void search_applications_should_filter_by_metadata_name_and_value() throws IOException {
    // Arrange - demonstrate case-insensitive metadata matching
    var app1 =
        AnonymousApplicationBuilder.validApp()
            .withName("ProdApp")
            .withMetadata("Environment", "Production")
            .build();
    var app2 =
        AnonymousApplicationBuilder.validApp().withMetadata("Environment", "Development").build();

    mockedSDKHelper
        .when(() -> SDKHelper.getApplicationsWithCache(anyString(), any()))
        .thenReturn(List.of(app1, app2));

    // Act - search with "environment" and "PRODUCTION" (case-insensitive)
    var result = assessService.search_applications(null, null, "environment", "PRODUCTION");

    // Assert - case-insensitive match for both name and value
    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("ProdApp");
  }

  @Test
  void search_applications_should_filter_by_metadata_name_only() throws IOException {
    // Arrange - match by metadata name regardless of value
    var app1 =
        AnonymousApplicationBuilder.validApp()
            .withName("App1")
            .withMetadata("Team", "Backend")
            .build();
    var app2 = AnonymousApplicationBuilder.validApp().withMetadata("Owner", "Alice").build();

    mockedSDKHelper
        .when(() -> SDKHelper.getApplicationsWithCache(anyString(), any()))
        .thenReturn(List.of(app1, app2));

    // Act - search by metadata name only (any value)
    var result = assessService.search_applications(null, null, "team", null);

    // Assert - matches any app with "team" metadata (case-insensitive)
    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("App1");
  }

  @Test
  void search_applications_should_combine_multiple_filters_with_and_logic() throws IOException {
    // Arrange - only app1 matches ALL filters (name, tag, metadata)
    var app1 =
        AnonymousApplicationBuilder.validApp()
            .withName("ProdApp1")
            .withTag("Production")
            .withMetadata("Environment", "Production")
            .build();

    var app2 =
        AnonymousApplicationBuilder.validApp()
            .withName("ProdApp2")
            .withTag("Development") // Wrong tag
            .withMetadata("Environment", "Production")
            .build();

    var app3 =
        AnonymousApplicationBuilder.validApp()
            .withName("TestApp") // Wrong name
            .withTag("Production")
            .withMetadata("Environment", "Production")
            .build();

    mockedSDKHelper
        .when(() -> SDKHelper.getApplicationsWithCache(anyString(), any()))
        .thenReturn(List.of(app1, app2, app3));

    // Act - combine name, tag, and metadata filters (AND logic)
    var result =
        assessService.search_applications("prod", "Production", "Environment", "Production");

    // Assert - only app1 matches ALL filters
    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("ProdApp1");
  }

  @Test
  void search_applications_should_handle_empty_metadata_list() throws IOException {
    // Arrange - app with empty metadata list (default from builder)
    var app = AnonymousApplicationBuilder.validApp().build();

    mockedSDKHelper
        .when(() -> SDKHelper.getApplicationsWithCache(anyString(), any()))
        .thenReturn(List.of(app));

    // Act - search with metadata filter
    var result = assessService.search_applications(null, null, "Environment", null);

    // Assert - no match (app has no metadata)
    assertThat(result).isEmpty();
  }

  @Test
  void search_applications_should_handle_empty_metadata_entities() throws IOException {
    // Arrange - app with no metadata (empty list via "never null collections" pattern)
    var app = AnonymousApplicationBuilder.validApp().withMetadataEntities(null).build();

    mockedSDKHelper
        .when(() -> SDKHelper.getApplicationsWithCache(anyString(), any()))
        .thenReturn(List.of(app));

    // Act - search with metadata filter
    var result = assessService.search_applications(null, null, "Environment", null);

    // Assert - no match (empty metadata doesn't match filter)
    assertThat(result).isEmpty();
  }

  @Test
  void search_applications_should_propagate_IOException_from_cache() throws IOException {
    // Arrange
    mockedSDKHelper
        .when(() -> SDKHelper.getApplicationsWithCache(anyString(), any()))
        .thenThrow(new IOException("Cache failure"));

    // Act & Assert - IOException propagates
    assertThatThrownBy(() -> assessService.search_applications(null, null, null, null))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Failed to search applications");
  }

  // ========== search_app_vulnerabilities Tests ==========

  @Test
  void searchAppVulnerabilities_should_return_error_when_appId_is_null() throws Exception {
    // Act
    var result =
        assessService.searchAppVulnerabilities(
            null, // null appId
            null, null, null, null, null, null, null, null, null, null, null, null);

    // Assert
    assertThat(result.message()).contains("appId parameter is required");
    assertThat(result.items()).isEmpty();
  }

  @Test
  void searchAppVulnerabilities_should_return_error_when_appId_is_blank() throws Exception {
    // Act
    var result =
        assessService.searchAppVulnerabilities(
            "   ", // blank appId
            null, null, null, null, null, null, null, null, null, null, null, null);

    // Assert
    assertThat(result.message()).contains("appId parameter is required");
    assertThat(result.items()).isEmpty();
  }

  @Test
  void
      searchAppVulnerabilities_should_support_both_useLatestSession_and_sessionMetadataName_together()
          throws Exception {
    // Given - Create traces with different sessions and metadata
    var mockTraces = new Traces();
    var traces = new ArrayList<Trace>();

    // Create 3 traces: 2 in latest session, 1 in old session
    // Of the 2 in latest session: 1 has branch=main, 1 has branch=develop
    // Trace 0: latest session, branch=main (should match both filters)
    traces.add(
        AnonymousTraceBuilder.validTrace()
            .withTitle("Vuln 0")
            .withRule("rule-0")
            .withUuid("uuid-0")
            .withSeverity("HIGH")
            .withStatus("REPORTED")
            .withSessionMetadata("latest-session-id", "branch", "main")
            .build());

    // Trace 1: latest session, branch=develop (matches session but not metadata name/value)
    traces.add(
        AnonymousTraceBuilder.validTrace()
            .withTitle("Vuln 1")
            .withRule("rule-1")
            .withUuid("uuid-1")
            .withSeverity("HIGH")
            .withStatus("REPORTED")
            .withSessionMetadata("latest-session-id", "branch", "develop")
            .build());

    // Trace 2: old session, branch=main (matches metadata but not session)
    traces.add(
        AnonymousTraceBuilder.validTrace()
            .withTitle("Vuln 2")
            .withRule("rule-2")
            .withUuid("uuid-2")
            .withSeverity("HIGH")
            .withStatus("REPORTED")
            .withSessionMetadata("old-session-id", "branch", "main")
            .build());

    // Set up mock traces
    try {
      var tracesField = Traces.class.getDeclaredField("traces");
      tracesField.setAccessible(true);
      tracesField.set(mockTraces, traces);

      var countField = Traces.class.getDeclaredField("count");
      countField.setAccessible(true);
      countField.set(mockTraces, 3);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create mock Traces", e);
    }

    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Mock SDKExtension to return latest session
    var mockAgentSession =
        new com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata.AgentSession();
    mockAgentSession.setAgentSessionId("latest-session-id");
    var mockSessionResponse =
        new com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata
            .SessionMetadataResponse();
    mockSessionResponse.setAgentSession(mockAgentSession);

    try (var mockedSDKExtension =
        mockConstruction(
            com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension.class,
            (mock, context) -> {
              when(mock.getLatestSessionMetadata(eq(TEST_ORG_ID), eq(TEST_APP_ID)))
                  .thenReturn(mockSessionResponse);
            })) {

      // When - both useLatestSession=true AND sessionMetadataName/Value
      var result =
          assessService.searchAppVulnerabilities(
              TEST_APP_ID,
              null, // page
              null, // pageSize
              null, // severities
              null, // statuses
              null, // vulnTypes
              null, // environments
              null, // lastSeenAfter
              null, // lastSeenBefore
              null, // vulnTags
              "branch", // sessionMetadataName
              "main", // sessionMetadataValue
              true); // useLatestSession=true

      // Then - only trace 0 should be returned (latest session + branch=main)
      assertThat(result.items()).hasSize(1);
      assertThat(result.items().get(0).title()).isEqualTo("Vuln 0");
      assertThat(result.totalItems()).isEqualTo(1);
    }
  }

  @Test
  void searchAppVulnerabilities_should_return_error_when_sessionMetadataValue_without_name()
      throws Exception {
    // Act - incomplete parameters
    var result =
        assessService.searchAppVulnerabilities(
            TEST_APP_ID,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null, // no sessionMetadataName
            "main", // but has sessionMetadataValue
            null);

    // Assert
    assertThat(result.message()).contains("sessionMetadataValue requires sessionMetadataName");
    assertThat(result.items()).isEmpty();
  }

  @Test
  void searchAppVulnerabilities_should_use_app_specific_endpoint() throws Exception {
    // Given
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // When
    assessService.searchAppVulnerabilities(
        TEST_APP_ID, null, null, null, null, null, null, null, null, null, null, null, null);

    // Then - verify app-specific API was called
    verify(mockContrastSDK).getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class));
  }

  @Test
  void searchAppVulnerabilities_should_expand_session_metadata_and_environments() throws Exception {
    // Given
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // When
    assessService.searchAppVulnerabilities(
        TEST_APP_ID, null, null, null, null, null, null, null, null, null, null, null, null);

    // Then - verify expand parameters
    var captor = ArgumentCaptor.forClass(TraceFilterForm.class);
    verify(mockContrastSDK).getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), captor.capture());

    var form = captor.getValue();
    assertThat(form.getExpand()).isNotNull();
    assertThat(form.getExpand()).contains(TraceFilterForm.TraceExpandValue.SESSION_METADATA);
    assertThat(form.getExpand()).contains(TraceFilterForm.TraceExpandValue.SERVER_ENVIRONMENTS);
    assertThat(form.getExpand()).contains(TraceFilterForm.TraceExpandValue.APPLICATION);
  }

  @Test
  void searchAppVulnerabilities_should_pass_pagination_params_to_SDK() throws Exception {
    // Given
    var mockTraces = createMockTraces(25, 100);
    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // When - request page 2 with 25 items per page
    assessService.searchAppVulnerabilities(
        TEST_APP_ID, 2, 25, null, null, null, null, null, null, null, null, null, null);

    // Then - verify SDK received correct pagination
    var captor = ArgumentCaptor.forClass(TraceFilterForm.class);
    verify(mockContrastSDK).getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), captor.capture());

    var form = captor.getValue();
    assertThat(form.getOffset()).isEqualTo(25); // (page 2 - 1) * 25
    assertThat(form.getLimit()).isEqualTo(25);
  }

  @Test
  void searchAppVulnerabilities_should_call_paginationHandler_correctly() throws Exception {
    // Given
    var mockTraces = createMockTraces(50, 150);
    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // When
    assessService.searchAppVulnerabilities(
        TEST_APP_ID, 1, 50, null, null, null, null, null, null, null, null, null, null);

    // Then - verify PaginationHandler was called
    verify(mockPaginationHandler)
        .createPaginatedResponse(
            argThat(list -> list.size() == 50), // items
            argThat(p -> p.page() == 1 && p.pageSize() == 50), // params
            eq(150), // totalItems
            anyList()); // warnings
  }

  @Test
  void searchAppVulnerabilities_should_return_error_on_invalid_severity() throws Exception {
    // Act - invalid severity
    var result =
        assessService.searchAppVulnerabilities(
            TEST_APP_ID,
            null,
            null,
            "INVALID_SEVERITY", // invalid
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    // Assert
    assertThat(result.message()).contains("Invalid severity");
    assertThat(result.items()).isEmpty();
  }

  @Test
  void searchAppVulnerabilities_should_return_error_on_invalid_environment() throws Exception {
    // Act - invalid environment
    var result =
        assessService.searchAppVulnerabilities(
            TEST_APP_ID,
            null,
            null,
            null,
            null,
            null,
            "INVALID_ENV", // invalid
            null,
            null,
            null,
            null,
            null,
            null);

    // Assert
    assertThat(result.message()).contains("Invalid environment");
    assertThat(result.items()).isEmpty();
  }

  @Test
  void searchAppVulnerabilities_should_throw_IOException_on_SDK_error() throws Exception {
    // Given - SDK throws exception
    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
        .thenThrow(new RuntimeException("SDK error"));

    // Act & Assert
    assertThatThrownBy(
            () ->
                assessService.searchAppVulnerabilities(
                    TEST_APP_ID,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Failed to search app vulnerabilities");
  }

  @Test
  void searchAppVulnerabilities_should_return_warning_when_useLatestSession_finds_no_session()
      throws Exception {
    // Given - SDK returns vulnerabilities, but SDKExtension returns null for latest session
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    try (var mockedSDKExtension =
        mockConstruction(
            com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension.class,
            (mock, context) -> {
              // Mock getLatestSessionMetadata to return null (no session found)
              when(mock.getLatestSessionMetadata(eq(TEST_ORG_ID), eq(TEST_APP_ID)))
                  .thenReturn(null);
            })) {

      // When - request with useLatestSession=true
      var result =
          assessService.searchAppVulnerabilities(
              TEST_APP_ID,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              null,
              true); // useLatestSession = true

      // Then - verify warning was passed to PaginationHandler
      @SuppressWarnings("unchecked")
      ArgumentCaptor<List<String>> warningsCaptor = ArgumentCaptor.forClass(List.class);
      verify(mockPaginationHandler)
          .createPaginatedResponse(
              anyList(), any(PaginationParams.class), any(), warningsCaptor.capture());

      var warnings = warningsCaptor.getValue();
      assertThat(warnings)
          .anyMatch(
              w ->
                  w.contains("No sessions found for this application")
                      && w.contains("Returning all vulnerabilities"));
      assertThat(result.items()).hasSize(10); // Should still return vulnerabilities

      // Verify SDK was called WITHOUT agentSessionId filter
      var formCaptor = ArgumentCaptor.forClass(TraceFilterForm.class);
      verify(mockContrastSDK).getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), formCaptor.capture());
      var form = formCaptor.getValue();
      assertThat(form).isNotNull();
    }
  }

  @Test
  void searchAppVulnerabilities_should_filter_by_useLatestSession_alone() throws Exception {
    // Given - Create traces with different session IDs
    var mockTraces = new Traces();
    var traces = new ArrayList<Trace>();

    // Create 5 traces: 3 in latest session, 2 in older sessions
    for (int i = 0; i < 5; i++) {
      String sessionId = i < 3 ? "latest-session-id" : "old-session-id-" + i;
      Trace trace =
          AnonymousTraceBuilder.validTrace()
              .withTitle("Vuln " + i)
              .withRule("rule-" + i)
              .withUuid("uuid-" + i)
              .withSeverity("HIGH")
              .withStatus("REPORTED")
              .withSessionMetadata(sessionId) // Simplified with builder helper method
              .build();
      traces.add(trace);
    }

    // Set up mock traces
    try {
      var tracesField = Traces.class.getDeclaredField("traces");
      tracesField.setAccessible(true);
      tracesField.set(mockTraces, traces);

      var countField = Traces.class.getDeclaredField("count");
      countField.setAccessible(true);
      countField.set(mockTraces, 5);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create mock Traces", e);
    }

    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Mock SDKExtension to return latest session
    var mockAgentSession =
        new com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata.AgentSession();
    mockAgentSession.setAgentSessionId("latest-session-id");
    var mockSessionResponse =
        new com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata
            .SessionMetadataResponse();
    mockSessionResponse.setAgentSession(mockAgentSession);

    try (var mockedSDKExtension =
        mockConstruction(
            com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension.class,
            (mock, context) -> {
              when(mock.getLatestSessionMetadata(eq(TEST_ORG_ID), eq(TEST_APP_ID)))
                  .thenReturn(mockSessionResponse);
            })) {

      // When - useLatestSession=true WITHOUT sessionMetadataName
      var result =
          assessService.searchAppVulnerabilities(
              TEST_APP_ID,
              null, // page
              null, // pageSize
              null, // severities
              null, // statuses
              null, // vulnTypes
              null, // environments
              null, // lastSeenAfter
              null, // lastSeenBefore
              null, // vulnTags
              null, // sessionMetadataName - NOT PROVIDED
              null, // sessionMetadataValue
              true); // useLatestSession=true

      // Then - only the 3 traces from latest session should be returned
      assertThat(result.items()).hasSize(3);
      assertThat(result.totalItems()).isEqualTo(3);
      assertThat(result.items()).extracting("title").containsExactly("Vuln 0", "Vuln 1", "Vuln 2");
    }
  }

  @Test
  void searchAppVulnerabilities_should_return_null_response_error_when_SDK_returns_null()
      throws Exception {
    // Given - SDK returns null Traces
    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
        .thenReturn(null);

    // When
    var result =
        assessService.searchAppVulnerabilities(
            TEST_APP_ID, null, null, null, null, null, null, null, null, null, null, null, null);

    // Then - verify error message about null response
    assertThat(result.message())
        .contains("App-level vulnerability API returned null for app " + TEST_APP_ID);
    assertThat(result.message()).contains("Please check API connectivity and permissions");
    assertThat(result.items()).isEmpty();
  }

  @Test
  void searchAppVulnerabilities_should_filter_by_session_metadata_case_insensitive()
      throws Exception {
    // Given - Create traces with session metadata
    var mockTraces = mock(Traces.class);
    var traces = new ArrayList<Trace>();

    // Create trace with matching metadata (case varies)
    Trace trace1 =
        AnonymousTraceBuilder.validTrace()
            .withTitle("SQL Injection")
            .withRule("sql-injection")
            .withUuid("uuid-1")
            .withSeverity("HIGH")
            .withLastTimeSeen(System.currentTimeMillis())
            .withSessionMetadata(
                "session-1", "ENVIRONMENT", "PRODUCTION") // uppercase - simplified!
            .build();
    traces.add(trace1);

    // Create trace with non-matching metadata
    Trace trace2 =
        AnonymousTraceBuilder.validTrace()
            .withTitle("XSS")
            .withRule("xss")
            .withUuid("uuid-2")
            .withSeverity("HIGH")
            .withLastTimeSeen(System.currentTimeMillis())
            .withSessionMetadata("session-2", "environment", "qa") // different value
            .build();
    traces.add(trace2);

    // Create trace with matching metadata (different case)
    Trace trace3 =
        AnonymousTraceBuilder.validTrace()
            .withTitle("Command Injection")
            .withRule("cmd-injection")
            .withUuid("uuid-3")
            .withSeverity("CRITICAL")
            .withLastTimeSeen(System.currentTimeMillis())
            .withSessionMetadata(
                "session-3", "Environment", "production") // mixed case - simplified!
            .build();
    traces.add(trace3);

    when(mockTraces.getTraces()).thenReturn(traces);
    // Note: getCount() not needed here - session metadata filtering uses filtered list size

    // Mock SDK to return all 3 traces (when session metadata filtering, SDK returns all)
    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // When - search with session metadata filter (lowercase)
    var result =
        assessService.searchAppVulnerabilities(
            TEST_APP_ID,
            1, // page
            50, // pageSize
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "environment", // sessionMetadataName (lowercase)
            "production", // sessionMetadataValue (lowercase)
            null);

    // Then - should return only traces 1 and 3 (case-insensitive match)
    assertThat(result.items()).hasSize(2);
    assertThat(result.items().get(0).type()).isEqualTo("sql-injection");
    assertThat(result.items().get(1).type()).isEqualTo("cmd-injection");

    // Verify total items reflects filtered count, not SDK count
    assertThat(result.totalItems()).isEqualTo(2);

    // Verify SDK was called with TraceFilterForm
    verify(mockContrastSDK).getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class));
  }

  @Test
  void searchAppVulnerabilities_should_treat_null_sessionMetadataValue_as_wildcard_match_any_value()
      throws Exception {
    // Given - 3 traces with different values for same metadata name
    var mockTraces = mock(Traces.class);
    var traces = new ArrayList<com.contrastsecurity.models.Trace>();

    // Trace 1: has "branch" metadata with value "main"
    Trace trace1 =
        AnonymousTraceBuilder.validTrace()
            .withTitle("SQL Injection vulnerability")
            .withRule("sql-injection")
            .withUuid("uuid-1")
            .withSeverity("HIGH")
            .withLastTimeSeen(System.currentTimeMillis())
            .withSessionMetadata("session-1", "branch", "main") // Value not checked in this test
            .build();
    traces.add(trace1);

    // Trace 2: has "branch" metadata with value "develop"
    Trace trace2 =
        AnonymousTraceBuilder.validTrace()
            .withTitle("XSS vulnerability")
            .withRule("xss")
            .withUuid("uuid-2")
            .withSeverity("MEDIUM")
            .withLastTimeSeen(System.currentTimeMillis())
            .withSessionMetadata("session-2", "branch", "develop") // Value not checked in this test
            .build();
    traces.add(trace2);

    // Trace 3: has "environment" metadata (different name, should not match)
    Trace trace3 =
        AnonymousTraceBuilder.validTrace()
            .withTitle("Command Injection vulnerability")
            .withRule("cmd-injection")
            .withUuid("uuid-3")
            .withSeverity("CRITICAL")
            .withLastTimeSeen(System.currentTimeMillis())
            .withSessionMetadata("session-3", "environment", "prod") // Different name
            .build();
    traces.add(trace3);

    when(mockTraces.getTraces()).thenReturn(traces);

    // Mock SDK to return all 3 traces
    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // When - search with sessionMetadataName but NULL sessionMetadataValue (wildcard)
    var result =
        assessService.searchAppVulnerabilities(
            TEST_APP_ID,
            1, // page
            50, // pageSize
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "branch", // sessionMetadataName
            null, // sessionMetadataValue = null (wildcard, match any value)
            null);

    // Then - should return traces 1 and 2 (both have "branch" metadata, regardless of value)
    assertThat(result.items()).hasSize(2);
    assertThat(result.items().get(0).vulnID()).isEqualTo("uuid-1");
    assertThat(result.items().get(1).vulnID()).isEqualTo("uuid-2");
    assertThat(result.totalItems()).isEqualTo(2);

    // Verify SDK was called
    verify(mockContrastSDK).getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class));
  }

  @Test
  void searchAppVulnerabilities_should_not_duplicate_vulns_in_multiple_matching_sessions()
      throws Exception {
    // Given - Create a vulnerability that appears in 2 sessions that BOTH match the filter
    var mockTraces = mock(Traces.class);
    var traces = new ArrayList<com.contrastsecurity.models.Trace>();

    // Create trace with 2 SessionMetadata objects that both match "branch=main"
    Trace trace =
        AnonymousTraceBuilder.validTrace()
            .withTitle("SQL Injection")
            .withRule("sql-injection")
            .withUuid("uuid-1")
            .withSeverity("HIGH")
            .withLastTimeSeen(System.currentTimeMillis())
            .withSessionMetadata("session-1", "branch", "main") // First SessionMetadata
            .withSessionMetadata("session-2", "branch", "main") // Second SessionMetadata
            .build();
    traces.add(trace);

    when(mockTraces.getTraces()).thenReturn(traces);

    // Mock SDK to return the trace
    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // When - search with session metadata filter
    var result =
        assessService.searchAppVulnerabilities(
            TEST_APP_ID,
            1, // page
            50, // pageSize
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "branch", // sessionMetadataName
            "main", // sessionMetadataValue
            null);

    // Then - vulnerability should appear ONCE, not twice (despite 2 matching sessions)
    assertThat(result.items()).hasSize(1);
    assertThat(result.items().get(0).vulnID()).isEqualTo("uuid-1");
    assertThat(result.items().get(0).type()).isEqualTo("sql-injection");
    assertThat(result.totalItems()).isEqualTo(1);

    // Verify SDK was called
    verify(mockContrastSDK).getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class));
  }

  @Test
  void searchAppVulnerabilities_should_handle_metadata_item_with_null_value() throws Exception {
    // Given - traces with metadata items that have null values
    var mockTraces = mock(Traces.class);
    var traces = new ArrayList<com.contrastsecurity.models.Trace>();

    // Trace 1: has "branch" metadata with NULL value
    Trace trace1 =
        AnonymousTraceBuilder.validTrace()
            .withTitle("SQL Injection vulnerability")
            .withRule("sql-injection")
            .withUuid("uuid-1")
            .withSeverity("HIGH")
            .withLastTimeSeen(System.currentTimeMillis())
            .withSessionMetadata("session-1", "branch", null) // NULL value
            .build();
    traces.add(trace1);

    // Trace 2: has "branch" metadata with actual value "main"
    Trace trace2 =
        AnonymousTraceBuilder.validTrace()
            .withTitle("XSS vulnerability")
            .withRule("xss")
            .withUuid("uuid-2")
            .withSeverity("MEDIUM")
            .withLastTimeSeen(System.currentTimeMillis())
            .withSessionMetadata("session-2", "branch", "main")
            .build();
    traces.add(trace2);

    when(mockTraces.getTraces()).thenReturn(traces);

    // Mock SDK to return both traces
    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // When - search with specific value "main"
    var result =
        assessService.searchAppVulnerabilities(
            TEST_APP_ID,
            1, // page
            50, // pageSize
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            "branch", // sessionMetadataName
            "main", // sessionMetadataValue = "main"
            null);

    // Then - should return only trace 2 (trace 1 has null value, doesn't match "main")
    assertThat(result.items()).hasSize(1);
    assertThat(result.items().get(0).vulnID()).isEqualTo("uuid-2");
    assertThat(result.totalItems()).isEqualTo(1);

    // Verify SDK was called and no NullPointerException occurred
    verify(mockContrastSDK).getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class));
  }

  @Test
  void searchAppVulnerabilities_should_pass_all_standard_filters_to_SDK() throws Exception {
    // Given
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // When - provide all standard filter parameters
    assessService.searchAppVulnerabilities(
        TEST_APP_ID,
        1, // page
        50, // pageSize
        "CRITICAL,HIGH", // severities
        "Reported,Confirmed", // statuses
        "sql-injection,xss-reflected", // vulnTypes
        "PRODUCTION,QA", // environments
        "2025-01-01", // lastSeenAfter
        "2025-12-31", // lastSeenBefore
        "reviewed,SmartFix Remediated", // vulnTags
        null, // sessionMetadataName
        null, // sessionMetadataValue
        null); // useLatestSession

    // Then - verify SDK received all filters correctly
    var captor = ArgumentCaptor.forClass(TraceFilterForm.class);
    verify(mockContrastSDK).getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), captor.capture());

    var form = captor.getValue();
    assertThat(form.getSeverities()).isNotNull();
    assertThat(form.getSeverities().size()).isEqualTo(2);
    assertThat(form.getStatus()).isNotNull();
    assertThat(form.getStatus().size()).isEqualTo(2);
    assertThat(form.getVulnTypes()).isNotNull();
    assertThat(form.getVulnTypes().size()).isEqualTo(2);
    assertThat(form.getEnvironments()).isNotNull();
    assertThat(form.getEnvironments().size()).isEqualTo(2);
    assertThat(form.getStartDate()).isNotNull();
    assertThat(form.getEndDate()).isNotNull();
    assertThat(form.getFilterTags()).isNotNull();
    assertThat(form.getFilterTags().size()).isEqualTo(2);
  }

  // ========== Status Filter Verification Tests (mcp-3sy) ==========
  // These tests verify that status filters work correctly after mcp-b9y's unified refactoring
  // eliminated the filterBody object, fixing the bug where status filters were ignored
  // when sessionMetadataName was provided.

  @Test
  void searchAppVulnerabilities_should_apply_status_filters_with_sessionMetadataName()
      throws Exception {
    // Given - Simulate SDK returning only traces matching status filter
    // In reality, SDK filters server-side. We mock that behavior here.
    var mockTraces = new Traces();
    var traces = new ArrayList<Trace>();

    // Trace 0: Reported status, branch=main
    Trace trace0 =
        AnonymousTraceBuilder.validTrace()
            .withTitle("SQL Injection")
            .withRule("sql-injection")
            .withUuid("uuid-reported")
            .withSeverity("HIGH")
            .withStatus("REPORTED")
            .withLastTimeSeen(System.currentTimeMillis())
            .withFirstTimeSeen(System.currentTimeMillis() - 86400000L)
            .withSessionMetadata("session-0", "branch", "main")
            .build();
    traces.add(trace0);

    // Trace 1: Suspicious status, branch=main
    Trace trace1 =
        AnonymousTraceBuilder.validTrace()
            .withTitle("Path Traversal")
            .withRule("path-traversal")
            .withUuid("uuid-suspicious")
            .withSeverity("HIGH")
            .withStatus("SUSPICIOUS")
            .withLastTimeSeen(System.currentTimeMillis())
            .withFirstTimeSeen(System.currentTimeMillis() - 86400000L)
            .withSessionMetadata("session-1", "branch", "main")
            .build();
    traces.add(trace1);

    // Trace 2: Reported status, branch=develop (excluded by session metadata filter)
    Trace trace2 =
        AnonymousTraceBuilder.validTrace()
            .withTitle("XSS Reflected")
            .withRule("xss-reflected")
            .withUuid("uuid-develop")
            .withSeverity("MEDIUM")
            .withStatus("REPORTED")
            .withLastTimeSeen(System.currentTimeMillis())
            .withFirstTimeSeen(System.currentTimeMillis() - 86400000L)
            .withSessionMetadata("session-2", "branch", "develop")
            .build();
    traces.add(trace2);

    // Setup mock traces using reflection
    // Note: SDK filters by status server-side, so Fixed/Remediated wouldn't be returned
    try {
      var tracesField = Traces.class.getDeclaredField("traces");
      tracesField.setAccessible(true);
      tracesField.set(mockTraces, traces);

      var countField = Traces.class.getDeclaredField("count");
      countField.setAccessible(true);
      countField.set(mockTraces, 3);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create mock Traces", e);
    }

    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // When - call with statuses="Reported,Suspicious" AND sessionMetadataName="branch"
    var result =
        assessService.searchAppVulnerabilities(
            TEST_APP_ID,
            null, // page
            null, // pageSize
            null, // severities
            "Reported,Suspicious", // statuses (explicit filter)
            null, // vulnTypes
            null, // environments
            null, // lastSeenAfter
            null, // lastSeenBefore
            null, // vulnTags
            "branch", // sessionMetadataName
            "main", // sessionMetadataValue
            null); // useLatestSession

    // Then - verify SDK received status filters (CRITICAL verification for mcp-3sy)
    var captor = ArgumentCaptor.forClass(TraceFilterForm.class);
    verify(mockContrastSDK).getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), captor.capture());

    var form = captor.getValue();
    assertThat(form.getStatus())
        .as("Status filters should be passed to SDK when session filtering is active")
        .isNotNull()
        .containsExactlyInAnyOrder("Reported", "Suspicious");

    // Verify results: trace0 and trace1 match (Reported + Suspicious with branch=main)
    // trace2 is excluded by session metadata filter (branch=develop)
    assertThat(result.items())
        .as("Should return traces matching both status and session metadata filters")
        .hasSize(2);
    assertThat(result.items().get(0).title()).isEqualTo("SQL Injection");
    assertThat(result.items().get(1).title()).isEqualTo("Path Traversal");
  }

  @Test
  void searchAppVulnerabilities_should_apply_status_filters_with_useLatestSession()
      throws Exception {
    // Given - Simulate SDK returning only Confirmed status traces (SDK filters server-side)
    var mockTraces = new Traces();
    var traces = new ArrayList<Trace>();

    // Trace 0: Confirmed status, latest session
    Trace trace0 = mock();
    when(trace0.getTitle()).thenReturn("Command Injection");
    when(trace0.getRule()).thenReturn("cmd-injection");
    when(trace0.getUuid()).thenReturn("uuid-confirmed-latest");
    when(trace0.getSeverity()).thenReturn("CRITICAL");
    when(trace0.getStatus()).thenReturn("CONFIRMED");
    when(trace0.getLastTimeSeen()).thenReturn(System.currentTimeMillis());
    when(trace0.getFirstTimeSeen()).thenReturn(System.currentTimeMillis() - 86400000L);

    var sessionMetadata0 = mock(SessionMetadata.class);
    lenient().when(sessionMetadata0.getSessionId()).thenReturn("latest-session-id");
    lenient().when(trace0.getSessionMetadata()).thenReturn(List.of(sessionMetadata0));
    traces.add(trace0);

    // Trace 1: Confirmed status, old session (excluded by useLatestSession filter)
    // Note: All stubs must be lenient because early termination filters traces during fetch,
    // so getters for filtered traces are never called (they're not converted to VulnLight)
    Trace trace1 = mock();
    lenient().when(trace1.getTitle()).thenReturn("LDAP Injection");
    lenient().when(trace1.getRule()).thenReturn("ldap-injection");
    lenient().when(trace1.getUuid()).thenReturn("uuid-confirmed-old");
    lenient().when(trace1.getSeverity()).thenReturn("HIGH");
    lenient().when(trace1.getStatus()).thenReturn("CONFIRMED");
    lenient().when(trace1.getLastTimeSeen()).thenReturn(System.currentTimeMillis());
    lenient().when(trace1.getFirstTimeSeen()).thenReturn(System.currentTimeMillis() - 86400000L);

    var sessionMetadata1 = mock(SessionMetadata.class);
    lenient().when(sessionMetadata1.getSessionId()).thenReturn("old-session-id");
    lenient().when(trace1.getSessionMetadata()).thenReturn(List.of(sessionMetadata1));
    traces.add(trace1);

    // Setup mock traces using reflection
    try {
      var tracesField = Traces.class.getDeclaredField("traces");
      tracesField.setAccessible(true);
      tracesField.set(mockTraces, traces);

      var countField = Traces.class.getDeclaredField("count");
      countField.setAccessible(true);
      countField.set(mockTraces, 2);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create mock Traces", e);
    }

    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Mock SDKExtension to return latest session
    var mockAgentSession =
        new com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata.AgentSession();
    mockAgentSession.setAgentSessionId("latest-session-id");
    var mockSessionResponse =
        new com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata
            .SessionMetadataResponse();
    mockSessionResponse.setAgentSession(mockAgentSession);

    try (var mockedSDKExtension =
        mockConstruction(
            com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension.class,
            (mock, context) -> {
              when(mock.getLatestSessionMetadata(eq(TEST_ORG_ID), eq(TEST_APP_ID)))
                  .thenReturn(mockSessionResponse);
            })) {

      // When - call with statuses="Confirmed" AND useLatestSession=true
      var result =
          assessService.searchAppVulnerabilities(
              TEST_APP_ID,
              null, // page
              null, // pageSize
              null, // severities
              "Confirmed", // statuses (explicit filter)
              null, // vulnTypes
              null, // environments
              null, // lastSeenAfter
              null, // lastSeenBefore
              null, // vulnTags
              null, // sessionMetadataName
              null, // sessionMetadataValue
              true); // useLatestSession=true

      // Then - verify SDK received status filters (CRITICAL verification for mcp-3sy)
      var captor = ArgumentCaptor.forClass(TraceFilterForm.class);
      verify(mockContrastSDK).getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), captor.capture());

      var form = captor.getValue();
      assertThat(form.getStatus())
          .as("Status filters should be passed to SDK even with useLatestSession")
          .isNotNull()
          .containsExactly("Confirmed");

      // Verify results: only trace0 matches (Confirmed + latest session)
      // trace1 excluded by in-memory session ID filtering
      assertThat(result.items())
          .as("Should return only Confirmed vulnerabilities from latest session")
          .hasSize(1);
      assertThat(result.items().get(0).title()).isEqualTo("Command Injection");
      assertThat(result.items().get(0).status()).isEqualTo("CONFIRMED");
    }
  }

  @Test
  void searchAppVulnerabilities_should_use_smart_defaults_with_sessionMetadataName()
      throws Exception {
    // Given - Simulate SDK returning only smart default statuses (SDK filters server-side)
    // Smart defaults = Reported, Suspicious, Confirmed (exclude Fixed and Remediated)
    var mockTraces = new Traces();
    var traces = new ArrayList<Trace>();

    // Trace 0: Reported (included in smart defaults), Environment=Production
    Trace trace0 = mock();
    when(trace0.getTitle()).thenReturn("Reported Vuln");
    when(trace0.getRule()).thenReturn("rule-0");
    when(trace0.getUuid()).thenReturn("uuid-0");
    when(trace0.getSeverity()).thenReturn("HIGH");
    when(trace0.getStatus()).thenReturn("REPORTED");
    when(trace0.getLastTimeSeen()).thenReturn(System.currentTimeMillis());
    when(trace0.getFirstTimeSeen()).thenReturn(System.currentTimeMillis() - 86400000L);

    var sessionMetadata0 = mock(SessionMetadata.class);
    var metadataItem0 = mock(MetadataItem.class);
    lenient().when(metadataItem0.getDisplayLabel()).thenReturn("Environment");
    lenient().when(metadataItem0.getValue()).thenReturn("Production");
    lenient().when(sessionMetadata0.getMetadata()).thenReturn(List.of(metadataItem0));
    lenient().when(trace0.getSessionMetadata()).thenReturn(List.of(sessionMetadata0));
    traces.add(trace0);

    // Trace 1: Confirmed (included in smart defaults), Environment=Production
    Trace trace1 = mock();
    when(trace1.getTitle()).thenReturn("Confirmed Vuln");
    when(trace1.getRule()).thenReturn("rule-1");
    when(trace1.getUuid()).thenReturn("uuid-1");
    when(trace1.getSeverity()).thenReturn("CRITICAL");
    when(trace1.getStatus()).thenReturn("CONFIRMED");
    when(trace1.getLastTimeSeen()).thenReturn(System.currentTimeMillis());
    when(trace1.getFirstTimeSeen()).thenReturn(System.currentTimeMillis() - 86400000L);

    var sessionMetadata1 = mock(SessionMetadata.class);
    var metadataItem1 = mock(MetadataItem.class);
    lenient().when(metadataItem1.getDisplayLabel()).thenReturn("Environment");
    lenient().when(metadataItem1.getValue()).thenReturn("Production");
    lenient().when(sessionMetadata1.getMetadata()).thenReturn(List.of(metadataItem1));
    lenient().when(trace1.getSessionMetadata()).thenReturn(List.of(sessionMetadata1));
    traces.add(trace1);

    // Trace 2: Reported (included in smart defaults), Environment=QA (excluded by metadata)
    // Note: All stubs must be lenient because early termination filters traces during fetch,
    // so getters for filtered traces are never called (they're not converted to VulnLight)
    Trace trace2 = mock();
    lenient().when(trace2.getTitle()).thenReturn("QA Vuln");
    lenient().when(trace2.getRule()).thenReturn("rule-2");
    lenient().when(trace2.getUuid()).thenReturn("uuid-2");
    lenient().when(trace2.getSeverity()).thenReturn("MEDIUM");
    lenient().when(trace2.getStatus()).thenReturn("REPORTED");
    lenient().when(trace2.getLastTimeSeen()).thenReturn(System.currentTimeMillis());
    lenient().when(trace2.getFirstTimeSeen()).thenReturn(System.currentTimeMillis() - 86400000L);

    var sessionMetadata2 = mock(SessionMetadata.class);
    var metadataItem2 = mock(MetadataItem.class);
    lenient().when(metadataItem2.getDisplayLabel()).thenReturn("Environment");
    lenient().when(metadataItem2.getValue()).thenReturn("QA");
    lenient().when(sessionMetadata2.getMetadata()).thenReturn(List.of(metadataItem2));
    lenient().when(trace2.getSessionMetadata()).thenReturn(List.of(sessionMetadata2));
    traces.add(trace2);

    // Setup mock traces using reflection
    // Note: Fixed/Remediated traces filtered out by SDK (smart defaults)
    try {
      var tracesField = Traces.class.getDeclaredField("traces");
      tracesField.setAccessible(true);
      tracesField.set(mockTraces, traces);

      var countField = Traces.class.getDeclaredField("count");
      countField.setAccessible(true);
      countField.set(mockTraces, 3);
    } catch (Exception e) {
      throw new RuntimeException("Failed to create mock Traces", e);
    }

    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // When - call with sessionMetadataName but NO explicit statuses (triggers smart defaults)
    var result =
        assessService.searchAppVulnerabilities(
            TEST_APP_ID,
            null, // page
            null, // pageSize
            null, // severities
            null, // statuses (NOT provided - should use smart defaults)
            null, // vulnTypes
            null, // environments
            null, // lastSeenAfter
            null, // lastSeenBefore
            null, // vulnTags
            "Environment", // sessionMetadataName
            "Production", // sessionMetadataValue
            null); // useLatestSession

    // Then - verify SDK received smart default statuses (CRITICAL verification for mcp-3sy)
    var captor = ArgumentCaptor.forClass(TraceFilterForm.class);
    verify(mockContrastSDK).getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), captor.capture());

    var form = captor.getValue();
    assertThat(form.getStatus())
        .as(
            "Smart default statuses should be passed to SDK (Reported, Suspicious, Confirmed -"
                + " excluding Fixed and Remediated)")
        .isNotNull()
        .containsExactlyInAnyOrder("Reported", "Suspicious", "Confirmed")
        .doesNotContain("Fixed", "Remediated");

    // Verify results: trace0 and trace1 match (Reported + Confirmed with Environment=Production)
    // trace2 excluded by session metadata filter (Environment=QA)
    // Note: Smart default warning message is tested separately in VulnerabilityFilterParamsTest
    assertThat(result.items())
        .as("Should return only actionable statuses (excluding Fixed and Remediated)")
        .hasSize(2);
    assertThat(result.items().get(0).title()).isEqualTo("Reported Vuln");
    assertThat(result.items().get(1).title()).isEqualTo("Confirmed Vuln");
  }

  // ========== Multi-Page Fetch Tests for Session Filtering (mcp-4it) ==========

  @Test
  void searchAppVulnerabilities_should_fetch_all_pages_when_useLatestSession_enabled()
      throws Exception {
    // Given - SDK returns 2 pages: 500 traces on first page, 200 on second page
    var page1Traces = mock(Traces.class);
    var page1List = new ArrayList<Trace>();
    for (int i = 0; i < 500; i++) {
      page1List.add(
          AnonymousTraceBuilder.validTrace()
              .withTitle("Page1-Vuln-" + i)
              .withSessionMetadata("latest-session-id", "branch", "main")
              .build());
    }
    when(page1Traces.getTraces()).thenReturn(page1List);

    var page2Traces = mock(Traces.class);
    var page2List = new ArrayList<Trace>();
    for (int i = 0; i < 200; i++) {
      page2List.add(
          AnonymousTraceBuilder.validTrace()
              .withTitle("Page2-Vuln-" + i)
              .withSessionMetadata("latest-session-id", "branch", "main")
              .build());
    }
    when(page2Traces.getTraces()).thenReturn(page2List);

    // Mock SDK to return different pages based on offset
    // Note: Page 2 has < 500 items so pagination loop stops after 2 calls
    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
        .thenAnswer(
            invocation -> {
              TraceFilterForm form = invocation.getArgument(2);
              if (form.getOffset() == 0) {
                return page1Traces;
              } else {
                return page2Traces;
              }
            });

    // Create SessionMetadataResponse with AgentSession
    var mockAgentSession =
        new com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata.AgentSession();
    mockAgentSession.setAgentSessionId("latest-session-id");
    var mockSessionResponse =
        new com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata
            .SessionMetadataResponse();
    mockSessionResponse.setAgentSession(mockAgentSession);

    // Mock SDKExtension to return a latest session
    try (var mockedSDKExtension =
        mockConstruction(
            com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension.class,
            (mock, context) -> {
              when(mock.getLatestSessionMetadata(eq(TEST_ORG_ID), eq(TEST_APP_ID)))
                  .thenReturn(mockSessionResponse);
            })) {

      // When - call with useLatestSession=true, requesting page 14 (offset 650)
      // This requires targetCount = 650 + 50 = 700, which requires both pages
      // Parameter order: appId, page, pageSize, severities, statuses, vulnTypes, environments,
      //   lastSeenAfter, lastSeenBefore, vulnTags, sessionMetadataName, sessionMetadataValue,
      //   useLatestSession
      var result =
          assessService.searchAppVulnerabilities(
              TEST_APP_ID,
              14, // page 14 (offset=650, needs 700 results)
              50, // pageSize
              null, // severities
              null, // statuses
              null, // vulnTypes
              null, // environments
              null, // lastSeenAfter
              null, // lastSeenBefore
              null, // vulnTags
              null, // sessionMetadataName
              null, // sessionMetadataValue
              true); // useLatestSession

      // Then - verify SDK was called at least twice to fetch multiple pages
      // (early termination needs 700 results, page 1 has 500, page 2 has 200)
      verify(mockContrastSDK, atLeast(2))
          .getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class));

      // Verify all 700 traces were fetched and available (totalItems reflects filtered count)
      // All traces match the session filter, so all 700 should be available
      assertThat(result.totalItems()).isEqualTo(700);

      // Verify page 14 of 50 items returned
      assertThat(result.items()).hasSize(50);
      assertThat(result.page()).isEqualTo(14);
    }
  }

  @Test
  void searchAppVulnerabilities_should_find_matching_trace_on_page2_with_session_metadata_filter()
      throws Exception {
    // Given - Page 1 has no matches, Page 2 has the matching trace
    var page1Traces = mock(Traces.class);
    var page1List = new ArrayList<Trace>();
    for (int i = 0; i < 500; i++) {
      page1List.add(
          AnonymousTraceBuilder.validTrace()
              .withTitle("Page1-Vuln-" + i)
              .withSessionMetadata("session-" + i, "branch", "develop") // No match
              .build());
    }
    when(page1Traces.getTraces()).thenReturn(page1List);

    var page2Traces = mock(Traces.class);
    var page2List = new ArrayList<Trace>();
    // Add one matching trace on page 2
    page2List.add(
        AnonymousTraceBuilder.validTrace()
            .withTitle("Matching-Vuln-On-Page2")
            .withSessionMetadata("session-match", "branch", "main") // Match!
            .build());
    // Add more non-matching traces
    for (int i = 1; i < 50; i++) {
      page2List.add(
          AnonymousTraceBuilder.validTrace()
              .withTitle("Page2-Vuln-" + i)
              .withSessionMetadata("session-other-" + i, "branch", "develop")
              .build());
    }
    when(page2Traces.getTraces()).thenReturn(page2List);

    // Mock SDK to return different pages based on offset
    // Note: Page 2 has < 500 items so pagination loop stops after 2 calls
    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
        .thenAnswer(
            invocation -> {
              TraceFilterForm form = invocation.getArgument(2);
              if (form.getOffset() == 0) {
                return page1Traces;
              } else {
                return page2Traces;
              }
            });

    // When - search with session metadata filter that matches trace on page 2
    // Parameter order: appId, page, pageSize, severities, statuses, vulnTypes, environments,
    //   lastSeenAfter, lastSeenBefore, vulnTags, sessionMetadataName, sessionMetadataValue,
    //   useLatestSession
    var result =
        assessService.searchAppVulnerabilities(
            TEST_APP_ID,
            1, // page
            50, // pageSize
            null, // severities
            null, // statuses
            null, // vulnTypes
            null, // environments
            null, // lastSeenAfter
            null, // lastSeenBefore
            null, // vulnTags
            "branch", // sessionMetadataName
            "main", // sessionMetadataValue - only matches one trace on page 2
            null); // useLatestSession

    // Then - should find the matching trace from page 2
    assertThat(result.items()).hasSize(1);
    assertThat(result.items().get(0).title()).isEqualTo("Matching-Vuln-On-Page2");
    assertThat(result.totalItems()).isEqualTo(1);
  }

  @Test
  void searchAppVulnerabilities_should_paginate_correctly_with_useLatestSession_and_no_sessions()
      throws Exception {
    // Given - Multiple pages of traces but no sessions exist (Codex Finding 2 scenario)
    var page1Traces = mock(Traces.class);
    var page1List = new ArrayList<Trace>();
    for (int i = 0; i < 100; i++) {
      page1List.add(
          AnonymousTraceBuilder.validTrace().withTitle("Vuln-" + i).withUuid("uuid-" + i).build());
    }
    when(page1Traces.getTraces()).thenReturn(page1List);

    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
        .thenReturn(page1Traces);

    // Mock SDKExtension to return null (no sessions found)
    try (var mockedSDKExtension =
        mockConstruction(
            com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension.class,
            (mock, context) -> {
              when(mock.getLatestSessionMetadata(eq(TEST_ORG_ID), eq(TEST_APP_ID)))
                  .thenReturn(null);
            })) {

      // When - request page 3 with useLatestSession=true but no sessions
      // Parameter order: appId, page, pageSize, severities, statuses, vulnTypes, environments,
      //   lastSeenAfter, lastSeenBefore, vulnTags, sessionMetadataName, sessionMetadataValue,
      //   useLatestSession
      var result =
          assessService.searchAppVulnerabilities(
              TEST_APP_ID,
              3, // page 3
              25, // pageSize
              null, // severities
              null, // statuses
              null, // vulnTypes
              null, // environments
              null, // lastSeenAfter
              null, // lastSeenBefore
              null, // vulnTags
              null, // sessionMetadataName
              null, // sessionMetadataValue
              true); // useLatestSession

      // Then - should return correct page 3 data (items 50-74)
      // With 100 total items, page 3 at size 25 should return items at indices 50-74
      assertThat(result.page()).isEqualTo(3);
      assertThat(result.pageSize()).isEqualTo(25);

      // Since no session filtering is applied (agentSessionId is null), all 100 traces
      // are available for in-memory pagination
      assertThat(result.totalItems()).isEqualTo(100);
      assertThat(result.items()).hasSize(25);

      // Verify PaginationHandler was called with warnings list containing the no-sessions warning
      var warningsCaptor = ArgumentCaptor.forClass(List.class);
      verify(mockPaginationHandler)
          .createPaginatedResponse(
              anyList(), any(PaginationParams.class), any(), warningsCaptor.capture());
      var warnings = (List<String>) warningsCaptor.getValue();
      assertThat(warnings).anyMatch(w -> w.contains("No sessions found"));
    }
  }

  @Test
  void searchAppVulnerabilities_should_handle_page3_exceeding_filtered_results() throws Exception {
    // Given - 200 total vulnerabilities, but session filter reduces to 80 matching (mcp-6xd
    // scenario)
    // User requests page 3 (offset=100) which exceeds filtered count
    var allTraces = mock(Traces.class);
    var traceList = new ArrayList<Trace>();

    // Create 200 traces: 80 matching session metadata, 120 non-matching
    for (int i = 0; i < 80; i++) {
      traceList.add(
          AnonymousTraceBuilder.validTrace()
              .withTitle("Matching-Vuln-" + i)
              .withUuid("matching-uuid-" + i)
              .withSessionMetadata("session-" + i, "branch", "main") // Matches filter
              .build());
    }
    for (int i = 80; i < 200; i++) {
      traceList.add(
          AnonymousTraceBuilder.validTrace()
              .withTitle("NonMatching-Vuln-" + i)
              .withUuid("nonmatching-uuid-" + i)
              .withSessionMetadata("session-" + i, "branch", "develop") // Does NOT match
              .build());
    }
    when(allTraces.getTraces()).thenReturn(traceList);

    // Return all 200 traces in a single page to simulate fetching all for session filtering
    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
        .thenReturn(allTraces);

    // When - request page 3 with session metadata filter (80 match, page 3 offset=100 > 80)
    // Parameter order: appId, page, pageSize, severities, statuses, vulnTypes, environments,
    //   lastSeenAfter, lastSeenBefore, vulnTags, sessionMetadataName, sessionMetadataValue,
    //   useLatestSession
    var result =
        assessService.searchAppVulnerabilities(
            TEST_APP_ID,
            3, // page 3 (offset would be 100)
            50, // pageSize
            null, // severities
            null, // statuses
            null, // vulnTypes
            null, // environments
            null, // lastSeenAfter
            null, // lastSeenBefore
            null, // vulnTags
            "branch", // sessionMetadataName
            "main", // sessionMetadataValue - matches 80 traces
            null); // useLatestSession

    // Then - verify behavior via ArgumentCaptor (mock doesn't compute message)
    // Key verifications for mcp-6xd: pagination is calculated on filtered results
    var itemsCaptor = ArgumentCaptor.forClass(List.class);
    var paramsCaptor = ArgumentCaptor.forClass(PaginationParams.class);
    var totalCaptor = ArgumentCaptor.forClass(Integer.class);

    verify(mockPaginationHandler)
        .createPaginatedResponse(
            itemsCaptor.capture(), paramsCaptor.capture(), totalCaptor.capture(), anyList());

    // 1. Items passed to PaginationHandler should be empty (offset 100 > 80 filtered results)
    assertThat(itemsCaptor.getValue()).isEmpty();
    // 2. totalItems passed should be filtered count (80), not original count (200)
    assertThat(totalCaptor.getValue()).isEqualTo(80);
    // 3. Pagination params should reflect the original request
    assertThat(paramsCaptor.getValue().page()).isEqualTo(3);
    assertThat(paramsCaptor.getValue().pageSize()).isEqualTo(50);

    // The mock returns these values - real PaginationHandler would compute the message
    assertThat(result.items()).isEmpty();
    assertThat(result.totalItems()).isEqualTo(80);
    assertThat(result.page()).isEqualTo(3);
  }

  @Test
  void searchAppVulnerabilities_should_paginate_filtered_results_correctly_on_page2()
      throws Exception {
    // Given - 200 total vulnerabilities, session filter reduces to 80 matching
    // User requests page 2 (offset=50) which should return items 51-80
    var allTraces = mock(Traces.class);
    var traceList = new ArrayList<Trace>();

    // Create 200 traces: 80 matching session metadata, 120 non-matching
    for (int i = 0; i < 80; i++) {
      traceList.add(
          AnonymousTraceBuilder.validTrace()
              .withTitle("Matching-Vuln-" + i)
              .withUuid("matching-uuid-" + i)
              .withSessionMetadata("session-" + i, "branch", "main") // Matches filter
              .build());
    }
    for (int i = 80; i < 200; i++) {
      traceList.add(
          AnonymousTraceBuilder.validTrace()
              .withTitle("NonMatching-Vuln-" + i)
              .withUuid("nonmatching-uuid-" + i)
              .withSessionMetadata("session-" + i, "branch", "develop") // Does NOT match
              .build());
    }
    when(allTraces.getTraces()).thenReturn(traceList);

    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
        .thenReturn(allTraces);

    // When - request page 2 with session metadata filter (80 match, page 2 should return 30 items)
    var result =
        assessService.searchAppVulnerabilities(
            TEST_APP_ID,
            2, // page 2 (offset would be 50)
            50, // pageSize
            null, // severities
            null, // statuses
            null, // vulnTypes
            null, // environments
            null, // lastSeenAfter
            null, // lastSeenBefore
            null, // vulnTags
            "branch", // sessionMetadataName
            "main", // sessionMetadataValue - matches 80 traces
            null); // useLatestSession

    // Then - verify correct items and pagination passed to PaginationHandler
    var itemsCaptor = ArgumentCaptor.forClass(List.class);
    var paramsCaptor = ArgumentCaptor.forClass(PaginationParams.class);
    var totalCaptor = ArgumentCaptor.forClass(Integer.class);

    verify(mockPaginationHandler)
        .createPaginatedResponse(
            itemsCaptor.capture(), paramsCaptor.capture(), totalCaptor.capture(), anyList());

    // Items passed should be items 50-79 (30 items)
    var items = (List<VulnLight>) itemsCaptor.getValue();
    assertThat(items).hasSize(30);
    // totalItems passed should be filtered count (80)
    assertThat(totalCaptor.getValue()).isEqualTo(80);
    // First item should be the 51st matching item (index 50)
    assertThat(items.get(0).title()).isEqualTo("Matching-Vuln-50");
    // Last item should be the 80th matching item (index 79)
    assertThat(items.get(29).title()).isEqualTo("Matching-Vuln-79");

    // Verify mock response reflects passed values
    assertThat(result.items()).hasSize(30);
    assertThat(result.totalItems()).isEqualTo(80);
    assertThat(result.page()).isEqualTo(2);
  }

  // ========== getVulnerabilityById Tests (mcp-3le fix) ==========

  @Test
  void getVulnerabilityById_should_call_getTrace_directly() throws Exception {
    // Given - This test verifies the fix for mcp-3le:
    // The method should call getTrace() directly instead of searching through
    // paginated results with getTraces() + stream filter.
    var vulnId = "test-vuln-uuid-123";
    var appId = "test-app-456";

    // Mock the new direct getTrace call (with expand parameter)
    var mockTrace = mock(Trace.class);
    when(mockTrace.getUuid()).thenReturn(vulnId);
    when(mockTrace.getTitle()).thenReturn("Test Vulnerability");
    when(mockTrace.getRule()).thenReturn("sql-injection");
    when(mockContrastSDK.getTrace(eq(TEST_ORG_ID), eq(appId), eq(vulnId), any()))
        .thenReturn(mockTrace);

    // Mock other required SDK calls (method has multiple dependencies)
    var mockRecommendation = mock(com.contrastsecurity.models.RecommendationResponse.class);
    var recommendation = mock(com.contrastsecurity.models.Recommendation.class);
    when(recommendation.getText()).thenReturn("Test recommendation");
    when(mockRecommendation.getRecommendation()).thenReturn(recommendation);
    when(mockContrastSDK.getRecommendation(eq(TEST_ORG_ID), eq(vulnId)))
        .thenReturn(mockRecommendation);

    var mockHttpRequest = mock(com.contrastsecurity.models.HttpRequestResponse.class);
    when(mockContrastSDK.getHttpRequest(eq(TEST_ORG_ID), eq(vulnId))).thenReturn(mockHttpRequest);

    var mockEventSummary = mock(com.contrastsecurity.models.EventSummaryResponse.class);
    when(mockEventSummary.getEvents()).thenReturn(List.of());
    when(mockContrastSDK.getEventSummary(eq(TEST_ORG_ID), eq(vulnId))).thenReturn(mockEventSummary);

    // Mock SDKHelper static methods for library data
    mockedSDKHelper
        .when(() -> SDKHelper.getLibsForID(eq(appId), eq(TEST_ORG_ID), any()))
        .thenReturn(List.of());

    // When - Call the method
    try {
      assessService.getVulnerabilityById(vulnId, appId);
    } catch (Exception e) {
      // Method may throw due to incomplete mocking of VulnerabilityMapper,
      // but we only need to verify getTrace was called correctly
    }

    // Then - Verify getTrace() was called directly with correct params and expand
    // This is the key verification for mcp-3le fix
    verify(mockContrastSDK).getTrace(eq(TEST_ORG_ID), eq(appId), eq(vulnId), any());

    // Verify the old buggy method (getTraces with filtering) was NOT called
    verify(mockContrastSDK, never())
        .getTraces(eq(TEST_ORG_ID), eq(appId), any(TraceFilterBody.class));
  }

  // ========== getVulnerabilityById null check tests (mcp-p5w fix) ==========

  @Test
  void getVulnerabilityById_should_throw_IOException_when_trace_is_null() throws Exception {
    // Given - getTrace returns null (can happen with API errors/permission issues)
    var vulnId = "test-vuln-uuid-null";
    var appId = "test-app-null";

    when(mockContrastSDK.getTrace(eq(TEST_ORG_ID), eq(appId), eq(vulnId), any())).thenReturn(null);

    // When/Then - Should throw IOException with descriptive message
    assertThatThrownBy(() -> assessService.getVulnerabilityById(vulnId, appId))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Vulnerability API returned null")
        .hasMessageContaining(vulnId)
        .hasMessageContaining(appId)
        .hasMessageContaining("connectivity")
        .hasMessageContaining("permissions");
  }

  @Test
  void getVulnerabilityById_should_handle_null_eventSummaryResponse() throws Exception {
    // Given - eventSummaryResponse is null
    var vulnId = "test-vuln-uuid-123";
    var appId = "test-app-456";

    // Mock trace to be non-null
    var mockTrace = mock(Trace.class);
    when(mockTrace.getUuid()).thenReturn(vulnId);
    when(mockTrace.getTitle()).thenReturn("Test Vulnerability");
    when(mockTrace.getRule()).thenReturn("sql-injection");
    when(mockContrastSDK.getTrace(eq(TEST_ORG_ID), eq(appId), eq(vulnId), any()))
        .thenReturn(mockTrace);

    // Mock recommendation (nullable)
    var mockRecommendation = mock(com.contrastsecurity.models.RecommendationResponse.class);
    var recommendation = mock(com.contrastsecurity.models.Recommendation.class);
    when(recommendation.getText()).thenReturn("Test recommendation");
    when(mockRecommendation.getRecommendation()).thenReturn(recommendation);
    when(mockContrastSDK.getRecommendation(eq(TEST_ORG_ID), eq(vulnId)))
        .thenReturn(mockRecommendation);

    // Mock http request (nullable)
    when(mockContrastSDK.getHttpRequest(eq(TEST_ORG_ID), eq(vulnId))).thenReturn(null);

    // Event summary is null - this should not cause NPE
    when(mockContrastSDK.getEventSummary(eq(TEST_ORG_ID), eq(vulnId))).thenReturn(null);

    // Mock SDKHelper static methods
    mockedSDKHelper
        .when(() -> SDKHelper.getLibsForID(eq(appId), eq(TEST_ORG_ID), any()))
        .thenReturn(List.of());

    // When - Should not throw NPE
    try {
      assessService.getVulnerabilityById(vulnId, appId);
    } catch (Exception e) {
      // May throw due to incomplete mapper mocking, but NOT NullPointerException
      assertThat(e).isNotInstanceOf(NullPointerException.class);
    }

    // Verify getTrace was called successfully
    verify(mockContrastSDK).getTrace(eq(TEST_ORG_ID), eq(appId), eq(vulnId), any());
  }

  @Test
  void getVulnerabilityById_should_handle_null_recommendation() throws Exception {
    // Given - recommendationResponse is null
    var vulnId = "test-vuln-uuid-123";
    var appId = "test-app-456";

    // Mock trace to be non-null
    var mockTrace = mock(Trace.class);
    when(mockTrace.getUuid()).thenReturn(vulnId);
    when(mockTrace.getTitle()).thenReturn("Test Vulnerability");
    when(mockTrace.getRule()).thenReturn("sql-injection");
    when(mockContrastSDK.getTrace(eq(TEST_ORG_ID), eq(appId), eq(vulnId), any()))
        .thenReturn(mockTrace);

    // Recommendation is null - this should not cause NPE
    when(mockContrastSDK.getRecommendation(eq(TEST_ORG_ID), eq(vulnId))).thenReturn(null);

    // Mock http request (nullable)
    when(mockContrastSDK.getHttpRequest(eq(TEST_ORG_ID), eq(vulnId))).thenReturn(null);

    // Mock event summary with empty events
    var mockEventSummary = mock(com.contrastsecurity.models.EventSummaryResponse.class);
    when(mockEventSummary.getEvents()).thenReturn(List.of());
    when(mockContrastSDK.getEventSummary(eq(TEST_ORG_ID), eq(vulnId))).thenReturn(mockEventSummary);

    // Mock SDKHelper static methods
    mockedSDKHelper
        .when(() -> SDKHelper.getLibsForID(eq(appId), eq(TEST_ORG_ID), any()))
        .thenReturn(List.of());

    // When - Should not throw NPE
    try {
      assessService.getVulnerabilityById(vulnId, appId);
    } catch (Exception e) {
      // May throw due to incomplete mapper mocking, but NOT NullPointerException
      assertThat(e).isNotInstanceOf(NullPointerException.class);
    }

    // Verify getTrace was called successfully
    verify(mockContrastSDK).getTrace(eq(TEST_ORG_ID), eq(appId), eq(vulnId), any());
  }

  // ========== Truncation Warning Tests (mcp-6r5) ==========

  @Test
  void searchAppVulnerabilities_should_include_truncation_warning_when_results_exceed_limit()
      throws Exception {
    // Given - Set a low limit for testing
    int testLimit = 100;
    org.springframework.test.util.ReflectionTestUtils.setField(
        assessService, "maxTracesForSessionFiltering", testLimit);

    try {
      // Create 150 traces (exceeds testLimit of 100)
      var page1Traces = mock(Traces.class);
      var page1List = new ArrayList<Trace>();
      for (int i = 0; i < 150; i++) {
        page1List.add(
            AnonymousTraceBuilder.validTrace()
                .withTitle("Vuln-" + i)
                .withSessionMetadata("session-id", "branch", "main")
                .build());
      }
      when(page1Traces.getTraces()).thenReturn(page1List);

      when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
          .thenReturn(page1Traces);

      // Create SessionMetadataResponse with AgentSession
      var mockAgentSession =
          new com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata.AgentSession();
      mockAgentSession.setAgentSessionId("session-id");
      var mockSessionResponse =
          new com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata
              .SessionMetadataResponse();
      mockSessionResponse.setAgentSession(mockAgentSession);

      try (var mockedSDKExtension =
          mockConstruction(
              com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension.class,
              (mock, context) -> {
                when(mock.getLatestSessionMetadata(eq(TEST_ORG_ID), eq(TEST_APP_ID)))
                    .thenReturn(mockSessionResponse);
              })) {

        // When - call with useLatestSession=true (triggers in-memory filtering)
        // Request page 3 to set targetCount=150, which exceeds maxTracesForSessionFiltering=100
        // Early termination will stop at 100 matching traces and generate truncation warning
        var result =
            assessService.searchAppVulnerabilities(
                TEST_APP_ID,
                3, // page (offset=100, so targetCount=150)
                50, // pageSize
                null, // severities
                null, // statuses
                null, // vulnTypes
                null, // environments
                null, // lastSeenAfter
                null, // lastSeenBefore
                null, // vulnTags
                null, // sessionMetadataName
                null, // sessionMetadataValue
                true); // useLatestSession

        // Then - verify truncation warning was passed to pagination handler
        // Capture the warnings argument passed to createPaginatedResponse
        @SuppressWarnings("unchecked")
        var warningsCaptor = ArgumentCaptor.forClass(List.class);

        verify(mockPaginationHandler)
            .createPaginatedResponse(anyList(), any(), anyInt(), warningsCaptor.capture());

        List<String> capturedWarnings = warningsCaptor.getValue();
        assertThat(capturedWarnings)
            .as("Should contain truncation warning when results exceed limit")
            .isNotNull()
            .isNotEmpty();

        var truncationWarning =
            capturedWarnings.stream().filter(w -> w.contains("IMPORTANT")).findFirst();
        assertThat(truncationWarning).as("Warning should be marked as IMPORTANT").isPresent();

        assertThat(truncationWarning.get())
            .as("Warning should mention results were truncated")
            .contains("truncated");

        assertThat(truncationWarning.get())
            .as("Warning should explain security implications")
            .contains("missing critical security findings");

        assertThat(truncationWarning.get())
            .as("Warning should mention the limit")
            .contains(String.valueOf(testLimit));
      }
    } finally {
      // Restore default limit
      org.springframework.test.util.ReflectionTestUtils.setField(
          assessService, "maxTracesForSessionFiltering", 50_000);
    }
  }

  @Test
  void searchAppVulnerabilities_should_return_partial_results_when_api_fails_mid_fetch()
      throws Exception {
    // Given - Page 1 succeeds, Page 2 fails with IOException
    var page1Traces = mock(Traces.class);
    var page1List = new ArrayList<Trace>();
    for (int i = 0; i < 500; i++) {
      page1List.add(
          AnonymousTraceBuilder.validTrace()
              .withTitle("Vuln-" + i)
              .withUuid("uuid-" + i)
              .withSessionMetadata("session-id", "branch", "main")
              .build());
    }
    when(page1Traces.getTraces()).thenReturn(page1List);

    // Mock SDK to return page 1 successfully, then throw on page 2
    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class)))
        .thenAnswer(
            invocation -> {
              TraceFilterForm form = invocation.getArgument(2);
              if (form.getOffset() == 0) {
                return page1Traces; // Page 1: success
              } else {
                throw new IOException("Network timeout on page 2"); // Page 2: failure
              }
            });

    // Create SessionMetadataResponse with AgentSession
    var mockAgentSession =
        new com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata.AgentSession();
    mockAgentSession.setAgentSessionId("session-id");
    var mockSessionResponse =
        new com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata
            .SessionMetadataResponse();
    mockSessionResponse.setAgentSession(mockAgentSession);

    try (var mockedSDKExtension =
        mockConstruction(
            com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension.class,
            (mock, context) -> {
              when(mock.getLatestSessionMetadata(eq(TEST_ORG_ID), eq(TEST_APP_ID)))
                  .thenReturn(mockSessionResponse);
            })) {

      // When - call with useLatestSession=true, requesting page 11 (offset=500)
      // This requires targetCount=550 matching results, but page 1 only has 500,
      // so page 2 is called and fails, triggering partial results behavior
      var result =
          assessService.searchAppVulnerabilities(
              TEST_APP_ID,
              11, // page (offset=500)
              50, // pageSize
              null, // severities
              null, // statuses
              null, // vulnTypes
              null, // environments
              null, // lastSeenAfter
              null, // lastSeenBefore
              null, // vulnTags
              null, // sessionMetadataName
              null, // sessionMetadataValue
              true); // useLatestSession

      // Then - verify partial results were returned (not exception thrown)
      // Page 11 (offset=500) with 500 total matching traces = empty page (offset >= total)
      assertThat(result.items())
          .as("Should return empty page when offset exceeds results")
          .isEmpty();

      // Verify warning was passed to pagination handler about partial data
      @SuppressWarnings("unchecked")
      var warningsCaptor = ArgumentCaptor.forClass(List.class);

      verify(mockPaginationHandler)
          .createPaginatedResponse(anyList(), any(), anyInt(), warningsCaptor.capture());

      List<String> capturedWarnings = warningsCaptor.getValue();
      assertThat(capturedWarnings)
          .as("Should contain warning about partial data")
          .isNotNull()
          .isNotEmpty();

      var partialDataWarning =
          capturedWarnings.stream().filter(w -> w.contains("Partial data")).findFirst();
      assertThat(partialDataWarning)
          .as("Warning should mention partial data was returned")
          .isPresent();

      assertThat(partialDataWarning.get())
          .as("Warning should mention API error")
          .contains("API error");

      assertThat(partialDataWarning.get())
          .as("Warning should indicate additional vulnerabilities may exist")
          .contains("Additional vulnerabilities may exist");

      // Verify totalItems reflects the matching traces found before page 2 error
      assertThat(result.totalItems())
          .as("Total items should reflect fetched matching traces")
          .isEqualTo(500);

      // Verify SDK was called at least once (early termination may reduce calls)
      verify(mockContrastSDK, atLeastOnce())
          .getTraces(eq(TEST_ORG_ID), eq(TEST_APP_ID), any(TraceFilterForm.class));
    }
  }
}
