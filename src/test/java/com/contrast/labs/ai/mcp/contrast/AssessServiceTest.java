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
                  items, params.page(), params.pageSize(), totalItems, false, null);
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

    // Mock PaginationHandler to return "No items found." message like the real implementation
    when(mockPaginationHandler.createPaginatedResponse(
            anyList(), any(PaginationParams.class), any(), anyList()))
        .thenAnswer(
            invocation -> {
              var items = invocation.getArgument(0, List.class);
              var params = invocation.getArgument(1, PaginationParams.class);
              var totalItems = invocation.getArgument(2, Integer.class);
              // Real PaginationHandler returns "No items found." for empty page 1 results
              var message = items.isEmpty() && params.page() == 1 ? "No items found." : null;
              return new PaginatedResponse<>(
                  items, params.page(), params.pageSize(), totalItems, false, message);
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
      Trace trace = mock();
      when(trace.getTitle()).thenReturn("Test Vulnerability " + i);
      when(trace.getRule()).thenReturn("test-rule-" + i);
      when(trace.getUuid()).thenReturn("uuid-" + i);
      when(trace.getSeverity()).thenReturn("HIGH");
      when(trace.getLastTimeSeen()).thenReturn(System.currentTimeMillis());
      when(trace.getStatus()).thenReturn("REPORTED");
      when(trace.getFirstTimeSeen()).thenReturn(System.currentTimeMillis() - 86400000L);
      when(trace.getClosedTime()).thenReturn(null);
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
    Trace trace1 = mock();
    when(trace1.getTitle()).thenReturn("SQL Injection");
    when(trace1.getRule()).thenReturn("sql-injection");
    when(trace1.getUuid()).thenReturn("uuid-1");
    when(trace1.getSeverity()).thenReturn("HIGH");
    when(trace1.getLastTimeSeen()).thenReturn(System.currentTimeMillis());
    when(trace1.getStatus()).thenReturn("REPORTED");
    when(trace1.getFirstTimeSeen()).thenReturn(System.currentTimeMillis() - 86400000L);
    when(trace1.getClosedTime()).thenReturn(null);

    // Set server_environments with different environments
    when(trace1.getServerEnvironments())
        .thenReturn(
            List.of("PRODUCTION", "QA", "PRODUCTION")); // Duplicate - should be deduplicated
    when(trace1.getTags()).thenReturn(new ArrayList<>());
    traces.add(trace1);

    // Trace 2: No servers
    Trace trace2 = mock();
    when(trace2.getTitle()).thenReturn("XSS");
    when(trace2.getRule()).thenReturn("xss-reflected");
    when(trace2.getUuid()).thenReturn("uuid-2");
    when(trace2.getSeverity()).thenReturn("MEDIUM");
    when(trace2.getLastTimeSeen()).thenReturn(System.currentTimeMillis());
    when(trace2.getStatus()).thenReturn("REPORTED");
    when(trace2.getFirstTimeSeen()).thenReturn(System.currentTimeMillis() - 86400000L);
    when(trace2.getClosedTime()).thenReturn(null);
    when(trace2.getServerEnvironments()).thenReturn(new ArrayList<>());
    when(trace2.getTags()).thenReturn(new ArrayList<>());
    traces.add(trace2);

    // Trace 3: Single server with one environment
    Trace trace3 = mock();
    when(trace3.getTitle()).thenReturn("Path Traversal");
    when(trace3.getRule()).thenReturn("path-traversal");
    when(trace3.getUuid()).thenReturn("uuid-3");
    when(trace3.getSeverity()).thenReturn("CRITICAL");
    when(trace3.getLastTimeSeen()).thenReturn(System.currentTimeMillis());
    when(trace3.getStatus()).thenReturn("CONFIRMED");
    when(trace3.getFirstTimeSeen()).thenReturn(System.currentTimeMillis() - 172800000L);
    when(trace3.getClosedTime()).thenReturn(null);
    when(trace3.getServerEnvironments()).thenReturn(List.of("DEVELOPMENT"));
    when(trace3.getTags()).thenReturn(new ArrayList<>());
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

    Trace trace = mock();
    when(trace.getTitle()).thenReturn("Test Vulnerability");
    when(trace.getRule()).thenReturn("test-rule");
    when(trace.getUuid()).thenReturn("test-uuid-123");
    when(trace.getSeverity()).thenReturn("HIGH");
    when(trace.getStatus()).thenReturn("Reported");
    when(trace.getLastTimeSeen()).thenReturn(lastSeen);
    when(trace.getFirstTimeSeen()).thenReturn(firstSeen);
    when(trace.getClosedTime()).thenReturn(closed);
    when(trace.getServerEnvironments()).thenReturn(new ArrayList<>());
    when(trace.getTags()).thenReturn(new ArrayList<>());

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
    Trace trace = mock();
    when(trace.getTitle()).thenReturn("Test Vulnerability");
    when(trace.getRule()).thenReturn("test-rule");
    when(trace.getUuid()).thenReturn("test-uuid-123");
    when(trace.getSeverity()).thenReturn("HIGH");
    when(trace.getStatus()).thenReturn("Reported");
    when(trace.getLastTimeSeen()).thenReturn(JAN_15_2025_10_30_UTC); // lastSeen is required
    when(trace.getFirstTimeSeen()).thenReturn(null); // optional
    when(trace.getClosedTime()).thenReturn(null); // optional
    when(trace.getServerEnvironments()).thenReturn(new ArrayList<>());
    when(trace.getTags()).thenReturn(new ArrayList<>());

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
  void search_applications_should_handle_null_metadata_entities() throws IOException {
    // Arrange - app with null metadata entities
    var app = AnonymousApplicationBuilder.validApp().withMetadataEntities(null).build();

    mockedSDKHelper
        .when(() -> SDKHelper.getApplicationsWithCache(anyString(), any()))
        .thenReturn(List.of(app));

    // Act - search with metadata filter
    var result = assessService.search_applications(null, null, "Environment", null);

    // Assert - no match and no NPE (defensive coding)
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
    for (int i = 0; i < 3; i++) {
      Trace trace = mock();
      when(trace.getTitle()).thenReturn("Vuln " + i);
      when(trace.getRule()).thenReturn("rule-" + i);
      when(trace.getUuid()).thenReturn("uuid-" + i);
      when(trace.getSeverity()).thenReturn("HIGH");
      when(trace.getStatus()).thenReturn("REPORTED");

      // Create session metadata with lenient stubbing for fields that may not be accessed
      var sessionMetadata = mock(SessionMetadata.class);
      if (i == 0) {
        // Trace 0: latest session, branch=main (should match both filters)
        lenient().when(sessionMetadata.getSessionId()).thenReturn("latest-session-id");
        var metadataItem = mock(MetadataItem.class);
        lenient().when(metadataItem.getDisplayLabel()).thenReturn("branch");
        lenient().when(metadataItem.getValue()).thenReturn("main");
        lenient().when(sessionMetadata.getMetadata()).thenReturn(List.of(metadataItem));
      } else if (i == 1) {
        // Trace 1: latest session, branch=develop (matches session but not metadata name/value)
        lenient().when(sessionMetadata.getSessionId()).thenReturn("latest-session-id");
        var metadataItem = mock(MetadataItem.class);
        lenient().when(metadataItem.getDisplayLabel()).thenReturn("branch");
        lenient().when(metadataItem.getValue()).thenReturn("develop");
        lenient().when(sessionMetadata.getMetadata()).thenReturn(List.of(metadataItem));
      } else {
        // Trace 2: old session, branch=main (matches metadata but not session)
        lenient().when(sessionMetadata.getSessionId()).thenReturn("old-session-id");
        var metadataItem = mock(MetadataItem.class);
        lenient().when(metadataItem.getDisplayLabel()).thenReturn("branch");
        lenient().when(metadataItem.getValue()).thenReturn("main");
        lenient().when(sessionMetadata.getMetadata()).thenReturn(List.of(metadataItem));
      }
      lenient().when(trace.getSessionMetadata()).thenReturn(List.of(sessionMetadata));
      traces.add(trace);
    }

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
      Trace trace = mock();
      when(trace.getTitle()).thenReturn("Vuln " + i);
      when(trace.getRule()).thenReturn("rule-" + i);
      when(trace.getUuid()).thenReturn("uuid-" + i);
      when(trace.getSeverity()).thenReturn("HIGH");
      when(trace.getStatus()).thenReturn("REPORTED");

      // Create session metadata with different session IDs (lenient for fields that may not be
      // accessed)
      var sessionMetadata = mock(SessionMetadata.class);
      if (i < 3) {
        // First 3 traces are in the latest session
        lenient().when(sessionMetadata.getSessionId()).thenReturn("latest-session-id");
      } else {
        // Last 2 traces are in older sessions
        lenient().when(sessionMetadata.getSessionId()).thenReturn("old-session-id-" + i);
      }
      lenient().when(sessionMetadata.getMetadata()).thenReturn(List.of());
      lenient().when(trace.getSessionMetadata()).thenReturn(List.of(sessionMetadata));
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
    Trace trace1 = mock();
    when(trace1.getTitle()).thenReturn("SQL Injection");
    when(trace1.getRule()).thenReturn("sql-injection");
    when(trace1.getUuid()).thenReturn("uuid-1");
    when(trace1.getSeverity()).thenReturn("HIGH");
    when(trace1.getLastTimeSeen()).thenReturn(System.currentTimeMillis());

    var sessionMetadata1 = mock(com.contrastsecurity.models.SessionMetadata.class);
    var metadataItem1 = mock(com.contrastsecurity.models.MetadataItem.class);
    when(metadataItem1.getDisplayLabel()).thenReturn("ENVIRONMENT"); // uppercase
    when(metadataItem1.getValue()).thenReturn("PRODUCTION"); // uppercase
    when(sessionMetadata1.getMetadata()).thenReturn(List.of(metadataItem1));
    when(trace1.getSessionMetadata()).thenReturn(List.of(sessionMetadata1));
    traces.add(trace1);

    // Create trace with non-matching metadata
    Trace trace2 = mock();
    when(trace2.getTitle()).thenReturn("XSS");
    when(trace2.getRule()).thenReturn("xss");
    when(trace2.getUuid()).thenReturn("uuid-2");
    when(trace2.getSeverity()).thenReturn("HIGH");
    when(trace2.getLastTimeSeen()).thenReturn(System.currentTimeMillis());

    var sessionMetadata2 = mock(com.contrastsecurity.models.SessionMetadata.class);
    var metadataItem2 = mock(com.contrastsecurity.models.MetadataItem.class);
    when(metadataItem2.getDisplayLabel()).thenReturn("environment");
    when(metadataItem2.getValue()).thenReturn("qa"); // different value
    when(sessionMetadata2.getMetadata()).thenReturn(List.of(metadataItem2));
    when(trace2.getSessionMetadata()).thenReturn(List.of(sessionMetadata2));
    traces.add(trace2);

    // Create trace with matching metadata (different case)
    Trace trace3 = mock();
    when(trace3.getTitle()).thenReturn("Command Injection");
    when(trace3.getRule()).thenReturn("cmd-injection");
    when(trace3.getUuid()).thenReturn("uuid-3");
    when(trace3.getSeverity()).thenReturn("CRITICAL");
    when(trace3.getLastTimeSeen()).thenReturn(System.currentTimeMillis());

    var sessionMetadata3 = mock(com.contrastsecurity.models.SessionMetadata.class);
    var metadataItem3 = mock(com.contrastsecurity.models.MetadataItem.class);
    when(metadataItem3.getDisplayLabel()).thenReturn("Environment"); // mixed case
    when(metadataItem3.getValue()).thenReturn("production"); // lowercase
    when(sessionMetadata3.getMetadata()).thenReturn(List.of(metadataItem3));
    when(trace3.getSessionMetadata()).thenReturn(List.of(sessionMetadata3));
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
    Trace trace1 = mock();
    when(trace1.getTitle()).thenReturn("SQL Injection vulnerability");
    when(trace1.getRule()).thenReturn("sql-injection");
    when(trace1.getUuid()).thenReturn("uuid-1");
    when(trace1.getSeverity()).thenReturn("HIGH");
    when(trace1.getLastTimeSeen()).thenReturn(System.currentTimeMillis());

    var sessionMetadata1 = mock(com.contrastsecurity.models.SessionMetadata.class);
    var metadataItem1 = mock(com.contrastsecurity.models.MetadataItem.class);
    when(metadataItem1.getDisplayLabel()).thenReturn("branch");
    // No getValue() stub needed - wildcard test doesn't check values
    when(sessionMetadata1.getMetadata()).thenReturn(List.of(metadataItem1));
    when(trace1.getSessionMetadata()).thenReturn(List.of(sessionMetadata1));
    traces.add(trace1);

    // Trace 2: has "branch" metadata with value "develop"
    Trace trace2 = mock();
    when(trace2.getTitle()).thenReturn("XSS vulnerability");
    when(trace2.getRule()).thenReturn("xss");
    when(trace2.getUuid()).thenReturn("uuid-2");
    when(trace2.getSeverity()).thenReturn("MEDIUM");
    when(trace2.getLastTimeSeen()).thenReturn(System.currentTimeMillis());

    var sessionMetadata2 = mock(com.contrastsecurity.models.SessionMetadata.class);
    var metadataItem2 = mock(com.contrastsecurity.models.MetadataItem.class);
    when(metadataItem2.getDisplayLabel()).thenReturn("branch");
    // No getValue() stub needed - wildcard test doesn't check values
    when(sessionMetadata2.getMetadata()).thenReturn(List.of(metadataItem2));
    when(trace2.getSessionMetadata()).thenReturn(List.of(sessionMetadata2));
    traces.add(trace2);

    // Trace 3: has "environment" metadata (different name, should not match)
    Trace trace3 = mock();
    when(trace3.getTitle()).thenReturn("Command Injection vulnerability");
    when(trace3.getRule()).thenReturn("cmd-injection");
    when(trace3.getUuid()).thenReturn("uuid-3");
    when(trace3.getSeverity()).thenReturn("CRITICAL");
    when(trace3.getLastTimeSeen()).thenReturn(System.currentTimeMillis());

    var sessionMetadata3 = mock(com.contrastsecurity.models.SessionMetadata.class);
    var metadataItem3 = mock(com.contrastsecurity.models.MetadataItem.class);
    when(metadataItem3.getDisplayLabel()).thenReturn("environment");
    // No getValue() stub needed - name doesn't match so value never checked
    when(sessionMetadata3.getMetadata()).thenReturn(List.of(metadataItem3));
    when(trace3.getSessionMetadata()).thenReturn(List.of(sessionMetadata3));
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
  void searchAppVulnerabilities_should_handle_metadata_item_with_null_value() throws Exception {
    // Given - traces with metadata items that have null values
    var mockTraces = mock(Traces.class);
    var traces = new ArrayList<com.contrastsecurity.models.Trace>();

    // Trace 1: has "branch" metadata with NULL value
    Trace trace1 = mock();
    when(trace1.getTitle()).thenReturn("SQL Injection vulnerability");
    when(trace1.getRule()).thenReturn("sql-injection");
    when(trace1.getUuid()).thenReturn("uuid-1");
    when(trace1.getSeverity()).thenReturn("HIGH");
    when(trace1.getLastTimeSeen()).thenReturn(System.currentTimeMillis());

    var sessionMetadata1 = mock(com.contrastsecurity.models.SessionMetadata.class);
    var metadataItem1 = mock(com.contrastsecurity.models.MetadataItem.class);
    when(metadataItem1.getDisplayLabel()).thenReturn("branch");
    when(metadataItem1.getValue()).thenReturn(null); // NULL value
    when(sessionMetadata1.getMetadata()).thenReturn(List.of(metadataItem1));
    when(trace1.getSessionMetadata()).thenReturn(List.of(sessionMetadata1));
    traces.add(trace1);

    // Trace 2: has "branch" metadata with actual value "main"
    Trace trace2 = mock();
    when(trace2.getTitle()).thenReturn("XSS vulnerability");
    when(trace2.getRule()).thenReturn("xss");
    when(trace2.getUuid()).thenReturn("uuid-2");
    when(trace2.getSeverity()).thenReturn("MEDIUM");
    when(trace2.getLastTimeSeen()).thenReturn(System.currentTimeMillis());

    var sessionMetadata2 = mock(com.contrastsecurity.models.SessionMetadata.class);
    var metadataItem2 = mock(com.contrastsecurity.models.MetadataItem.class);
    when(metadataItem2.getDisplayLabel()).thenReturn("branch");
    when(metadataItem2.getValue()).thenReturn("main");
    when(sessionMetadata2.getMetadata()).thenReturn(List.of(metadataItem2));
    when(trace2.getSessionMetadata()).thenReturn(List.of(sessionMetadata2));
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
    Trace trace0 = mock();
    when(trace0.getTitle()).thenReturn("SQL Injection");
    when(trace0.getRule()).thenReturn("sql-injection");
    when(trace0.getUuid()).thenReturn("uuid-reported");
    when(trace0.getSeverity()).thenReturn("HIGH");
    when(trace0.getStatus()).thenReturn("REPORTED");
    when(trace0.getLastTimeSeen()).thenReturn(System.currentTimeMillis());
    when(trace0.getFirstTimeSeen()).thenReturn(System.currentTimeMillis() - 86400000L);

    var sessionMetadata0 = mock(SessionMetadata.class);
    var metadataItem0 = mock(MetadataItem.class);
    lenient().when(metadataItem0.getDisplayLabel()).thenReturn("branch");
    lenient().when(metadataItem0.getValue()).thenReturn("main");
    lenient().when(sessionMetadata0.getMetadata()).thenReturn(List.of(metadataItem0));
    lenient().when(trace0.getSessionMetadata()).thenReturn(List.of(sessionMetadata0));
    traces.add(trace0);

    // Trace 1: Suspicious status, branch=main
    Trace trace1 = mock();
    when(trace1.getTitle()).thenReturn("Path Traversal");
    when(trace1.getRule()).thenReturn("path-traversal");
    when(trace1.getUuid()).thenReturn("uuid-suspicious");
    when(trace1.getSeverity()).thenReturn("HIGH");
    when(trace1.getStatus()).thenReturn("SUSPICIOUS");
    when(trace1.getLastTimeSeen()).thenReturn(System.currentTimeMillis());
    when(trace1.getFirstTimeSeen()).thenReturn(System.currentTimeMillis() - 86400000L);

    var sessionMetadata1 = mock(SessionMetadata.class);
    var metadataItem1 = mock(MetadataItem.class);
    lenient().when(metadataItem1.getDisplayLabel()).thenReturn("branch");
    lenient().when(metadataItem1.getValue()).thenReturn("main");
    lenient().when(sessionMetadata1.getMetadata()).thenReturn(List.of(metadataItem1));
    lenient().when(trace1.getSessionMetadata()).thenReturn(List.of(sessionMetadata1));
    traces.add(trace1);

    // Trace 2: Reported status, branch=develop (excluded by session metadata filter)
    Trace trace2 = mock();
    when(trace2.getTitle()).thenReturn("XSS Reflected");
    when(trace2.getRule()).thenReturn("xss-reflected");
    when(trace2.getUuid()).thenReturn("uuid-develop");
    when(trace2.getSeverity()).thenReturn("MEDIUM");
    when(trace2.getStatus()).thenReturn("REPORTED");
    when(trace2.getLastTimeSeen()).thenReturn(System.currentTimeMillis());
    when(trace2.getFirstTimeSeen()).thenReturn(System.currentTimeMillis() - 86400000L);

    var sessionMetadata2 = mock(SessionMetadata.class);
    var metadataItem2 = mock(MetadataItem.class);
    lenient().when(metadataItem2.getDisplayLabel()).thenReturn("branch");
    lenient().when(metadataItem2.getValue()).thenReturn("develop");
    lenient().when(sessionMetadata2.getMetadata()).thenReturn(List.of(metadataItem2));
    lenient().when(trace2.getSessionMetadata()).thenReturn(List.of(sessionMetadata2));
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
    Trace trace1 = mock();
    when(trace1.getTitle()).thenReturn("LDAP Injection");
    when(trace1.getRule()).thenReturn("ldap-injection");
    when(trace1.getUuid()).thenReturn("uuid-confirmed-old");
    when(trace1.getSeverity()).thenReturn("HIGH");
    when(trace1.getStatus()).thenReturn("CONFIRMED");
    when(trace1.getLastTimeSeen()).thenReturn(System.currentTimeMillis());
    when(trace1.getFirstTimeSeen()).thenReturn(System.currentTimeMillis() - 86400000L);

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
    Trace trace2 = mock();
    when(trace2.getTitle()).thenReturn("QA Vuln");
    when(trace2.getRule()).thenReturn("rule-2");
    when(trace2.getUuid()).thenReturn("uuid-2");
    when(trace2.getSeverity()).thenReturn("MEDIUM");
    when(trace2.getStatus()).thenReturn("REPORTED");
    when(trace2.getLastTimeSeen()).thenReturn(System.currentTimeMillis());
    when(trace2.getFirstTimeSeen()).thenReturn(System.currentTimeMillis() - 86400000L);

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
}
