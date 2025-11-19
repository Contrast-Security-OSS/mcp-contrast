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
import com.contrastsecurity.models.Rules;
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
  void testGetAllVulnerabilities_PassesCorrectParametersToSDK() throws Exception {
    // Given
    var mockTraces = createMockTraces(50, 150);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // When
    assessService.getAllVulnerabilities(2, 75, null, null, null, null, null, null, null, null);

    // Then - Verify SDK received correct parameters
    var captor = ArgumentCaptor.forClass(TraceFilterForm.class);
    verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

    var form = captor.getValue();
    assertThat(form.getOffset()).isEqualTo(75); // (page 2 - 1) * 75
    assertThat(form.getLimit()).isEqualTo(75);
  }

  @Test
  void testGetAllVulnerabilities_SetsExpandParametersCorrectly() throws Exception {
    // Given - Test that SESSION_METADATA and SERVER_ENVIRONMENTS expand are set
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // When
    assessService.getAllVulnerabilities(1, 50, null, null, null, null, null, null, null, null);

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
  void testGetAllVulnerabilities_CallsPaginationHandlerCorrectly() throws Exception {
    // Given
    var mockTraces = createMockTraces(50, 150);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // When
    assessService.getAllVulnerabilities(1, 50, null, null, null, null, null, null, null, null);

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
  void testGetAllVulnerabilities_RoutesToAppSpecificAPI_WhenAppIdProvided() throws Exception {
    // Given
    var appId = "test-app-123";
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(appId), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // When
    assessService.getAllVulnerabilities(1, 50, null, null, appId, null, null, null, null, null);

    // Then - Verify app-specific API was used
    verify(mockContrastSDK).getTraces(eq(TEST_ORG_ID), eq(appId), any(TraceFilterForm.class));
    verify(mockContrastSDK, never()).getTracesInOrg(any(), any());
  }

  @Test
  void testGetAllVulnerabilities_RoutesToOrgAPI_WhenNoAppId() throws Exception {
    // Given
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // When
    assessService.getAllVulnerabilities(1, 50, null, null, null, null, null, null, null, null);

    // Then - Verify org-level API was used
    verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class));
    verify(mockContrastSDK, never()).getTraces(any(), any(), any(TraceFilterForm.class));
  }

  // ========== Empty Results Tests ==========

  @Test
  void testGetAllVulnerabilities_EmptyResults_PassesEmptyListToPaginationHandler()
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
        assessService.getAllVulnerabilities(1, 50, null, null, null, null, null, null, null, null);

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
  void testGetAllVulnerabilities_SeverityFilter() throws Exception {
    // Arrange
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Act
    var response =
        assessService.getAllVulnerabilities(
            1, 50, "CRITICAL,HIGH", null, null, null, null, null, null, null);

    // Assert
    assertThat(response).isNotNull();
    var captor = ArgumentCaptor.forClass(TraceFilterForm.class);
    verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

    var form = captor.getValue();
    assertThat(form.getSeverities()).isNotNull();
    assertThat(form.getSeverities().size()).isEqualTo(2);
  }

  @Test
  void testGetAllVulnerabilities_InvalidSeverity_HardFailure() throws Exception {
    // Act - Invalid severity causes hard failure, SDK should not be called
    var response =
        assessService.getAllVulnerabilities(
            1, 50, "CRITICAL,SUPER_HIGH", null, null, null, null, null, null, null);

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
  void testGetAllVulnerabilities_StatusSmartDefaults() throws Exception {
    // Arrange
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Act - no status provided, should use smart defaults
    var response =
        assessService.getAllVulnerabilities(1, 50, null, null, null, null, null, null, null, null);

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
  void testGetAllVulnerabilities_StatusExplicitOverride() throws Exception {
    // Arrange
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Act - explicitly provide statuses (including Fixed and Remediated)
    var response =
        assessService.getAllVulnerabilities(
            1, 50, null, "Reported,Fixed,Remediated", null, null, null, null, null, null);

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
  void testGetAllVulnerabilities_VulnTypesFilter() throws Exception {
    // Arrange
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Act
    var response =
        assessService.getAllVulnerabilities(
            1, 50, null, null, null, "sql-injection,xss-reflected", null, null, null, null);

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
  void testGetAllVulnerabilities_EnvironmentFilter() throws Exception {
    // Arrange
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Act
    var response =
        assessService.getAllVulnerabilities(
            1, 50, null, null, null, null, "PRODUCTION,QA", null, null, null);

    // Assert
    assertThat(response).isNotNull();
    var captor = ArgumentCaptor.forClass(TraceFilterForm.class);
    verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

    var form = captor.getValue();
    assertThat(form.getEnvironments()).isNotNull();
    assertThat(form.getEnvironments().size()).isEqualTo(2);
  }

  @Test
  void testGetAllVulnerabilities_DateFilterValid() throws Exception {
    // Arrange
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Act
    var response =
        assessService.getAllVulnerabilities(
            1, 50, null, null, null, null, null, "2025-01-01", "2025-12-31", null);

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
  void testGetAllVulnerabilities_VulnTagsFilter() throws Exception {
    // Arrange
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Act
    var response =
        assessService.getAllVulnerabilities(
            1, 50, null, null, null, null, null, null, null, "SmartFix Remediated,reviewed");

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
  void testGetAllVulnerabilities_MultipleFilters() throws Exception {
    // Arrange
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Act - combine severity, status, vulnTypes, and environment
    var response =
        assessService.getAllVulnerabilities(
            1,
            50,
            "CRITICAL,HIGH",
            "Reported,Confirmed",
            null,
            "sql-injection,cmd-injection",
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
  void testGetAllVulnerabilities_AppIdRouting() throws Exception {
    // Arrange
    var mockTraces = createMockTraces(10, 10);
    var testAppId = "test-app-123";
    when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(testAppId), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Act
    var response =
        assessService.getAllVulnerabilities(
            1, 50, null, null, testAppId, null, null, null, null, null);

    // Assert
    assertThat(response).isNotNull();
    // Verify it used app-specific API, not org-level
    verify(mockContrastSDK).getTraces(eq(TEST_ORG_ID), eq(testAppId), any(TraceFilterForm.class));
    verify(mockContrastSDK, never()).getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class));
  }

  @Test
  void testGetAllVulnerabilities_WhitespaceInFilters() throws Exception {
    // Arrange
    var mockTraces = createMockTraces(10, 10);
    when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
        .thenReturn(mockTraces);

    // Act - test whitespace handling: "CRITICAL , HIGH" instead of "CRITICAL,HIGH"
    var response =
        assessService.getAllVulnerabilities(
            1, 50, "CRITICAL , HIGH , ", null, null, null, null, null, null, null);

    // Assert
    assertThat(response).isNotNull();
    var captor = ArgumentCaptor.forClass(TraceFilterForm.class);
    verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

    var form = captor.getValue();
    assertThat(form.getSeverities()).isNotNull();
    assertThat(form.getSeverities().size()).isEqualTo(2);
  }

  @Test
  void testGetAllVulnerabilities_EnvironmentsInResponse() throws Exception {
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
        assessService.getAllVulnerabilities(1, 50, null, null, null, null, null, null, null, null);

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
        assessService.getAllVulnerabilities(1, 50, null, null, null, null, null, null, null, null);

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
        assessService.getAllVulnerabilities(1, 50, null, null, null, null, null, null, null, null);

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
}
