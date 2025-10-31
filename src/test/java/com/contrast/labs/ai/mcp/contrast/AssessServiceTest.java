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

import com.contrast.labs.ai.mcp.contrast.PaginationParams;
import com.contrast.labs.ai.mcp.contrast.data.PaginatedResponse;
import com.contrast.labs.ai.mcp.contrast.data.VulnLight;
import com.contrast.labs.ai.mcp.contrast.mapper.VulnerabilityMapper;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.utils.PaginationHandler;
import com.contrastsecurity.http.TraceFilterForm;
import com.contrastsecurity.models.Trace;
import com.contrastsecurity.models.Traces;
import com.contrastsecurity.models.Rules;
import com.contrastsecurity.sdk.ContrastSDK;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for AssessService pagination implementation.
 * Tests all 11 required pagination scenarios from pagination-spec-v1.0.md
 */
@ExtendWith(MockitoExtension.class)
class AssessServiceTest {

    private AssessService assessService;

    @Mock
    private ContrastSDK mockContrastSDK;

    @Mock
    private PaginationHandler mockPaginationHandler;

    private VulnerabilityMapper vulnerabilityMapper;

    private MockedStatic<SDKHelper> mockedSDKHelper;

    private static final String TEST_ORG_ID = "test-org-123";
    private static final String TEST_HOST = "https://test.contrast.local";
    private static final String TEST_API_KEY = "test-api-key";
    private static final String TEST_SERVICE_KEY = "test-service-key";
    private static final String TEST_USERNAME = "test-user";

    // Named constants for test timestamps
    private static final long JAN_15_2025_10_30_UTC = LocalDateTime.of(2025, 1, 15, 10, 30)
            .toInstant(ZoneOffset.UTC).toEpochMilli(); // 1736938200000L
    private static final long JAN_1_2024_00_00_UTC = LocalDateTime.of(2024, 1, 1, 0, 0)
            .toInstant(ZoneOffset.UTC).toEpochMilli(); // 1704067200000L
    private static final long FEB_19_2025_13_20_UTC = LocalDateTime.of(2025, 2, 19, 13, 20)
            .toInstant(ZoneOffset.UTC).toEpochMilli(); // 1740000000000L

    @BeforeEach
    void setUp() throws Exception {
        // Create real VulnerabilityMapper, mock PaginationHandler
        vulnerabilityMapper = new VulnerabilityMapper();

        // Create AssessService with real mapper and mocked pagination handler
        assessService = new AssessService(vulnerabilityMapper, mockPaginationHandler);

        // Setup simplified mock behavior for PaginationHandler
        // PaginationHandler logic is tested in its own test class
        lenient().when(mockPaginationHandler.wrapApiPaginatedItems(anyList(), any(PaginationParams.class), any(), anyList()))
            .thenAnswer(invocation -> {
                List<?> items = invocation.getArgument(0);
                PaginationParams params = invocation.getArgument(1);
                Integer totalItems = invocation.getArgument(2);
                // Return simple response - pagination logic tested in PaginationHandlerTest
                return new PaginatedResponse<>(items, params.page(), params.pageSize(), totalItems, false, null);
            });

        lenient().when(mockPaginationHandler.paginateInMemory(anyList(), any(PaginationParams.class), anyList()))
            .thenAnswer(invocation -> {
                List<?> allItems = invocation.getArgument(0);
                PaginationParams params = invocation.getArgument(1);
                // Return simple response - pagination logic tested in PaginationHandlerTest
                return new PaginatedResponse<>(allItems, params.page(), params.pageSize(), allItems.size(), false, null);
            });

        // Mock the static SDKHelper.getSDK() method
        mockedSDKHelper = mockStatic(SDKHelper.class);
        mockedSDKHelper.when(() -> SDKHelper.getSDK(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()
        )).thenReturn(mockContrastSDK);

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
        Traces mockTraces = createMockTraces(50, 150);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // When
        assessService.getAllVulnerabilities(2, 75, null, null, null, null, null, null, null, null);

        // Then - Verify SDK received correct parameters
        ArgumentCaptor<TraceFilterForm> captor = ArgumentCaptor.forClass(TraceFilterForm.class);
        verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

        TraceFilterForm form = captor.getValue();
        assertEquals(75, form.getOffset());  // (page 2 - 1) * 75
        assertEquals(75, form.getLimit());
    }

    @Test
    void testGetAllVulnerabilities_CallsPaginationHandlerCorrectly() throws Exception {
        // Given
        Traces mockTraces = createMockTraces(50, 150);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // When
        assessService.getAllVulnerabilities(1, 50, null, null, null, null, null, null, null, null);

        // Then - Verify PaginationHandler received correct arguments
        verify(mockPaginationHandler).wrapApiPaginatedItems(
            argThat(list -> list.size() == 50),           // items
            argThat(p -> p.page() == 1 && p.pageSize() == 50), // params
            eq(150),                                       // totalItems
            anyList()                                      // warnings
        );
    }

    // ========== Routing Tests ==========

    @Test
    void testGetAllVulnerabilities_RoutesToAppSpecificAPI_WhenAppIdProvided() throws Exception {
        // Given
        String appId = "test-app-123";
        Traces mockTraces = createMockTraces(10, 10);
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
        Traces mockTraces = createMockTraces(10, 10);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // When
        assessService.getAllVulnerabilities(1, 50, null, null, null, null, null, null, null, null);

        // Then - Verify org-level API was used
        verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class));
        verify(mockContrastSDK, never()).getTraces(any(), any(), any(TraceFilterForm.class));
    }

    // ========== Helper Methods ==========

    /**
     * Creates a mock Traces object with the specified number of traces
     * @param traceCount Number of traces to create
     * @param totalCount Total count to set (null if not available)
     */
    private Traces createMockTraces(int traceCount, Integer totalCount) {
        Traces mockTraces = new Traces();
        List<Trace> traces = new ArrayList<>();

        for (int i = 0; i < traceCount; i++) {
            Trace trace = mock(Trace.class);
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
            java.lang.reflect.Field tracesField = Traces.class.getDeclaredField("traces");
            tracesField.setAccessible(true);
            tracesField.set(mockTraces, traces);

            java.lang.reflect.Field countField = Traces.class.getDeclaredField("count");
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
        Rules mockRules = createMockRules(
            "sql-injection",
            "xss-reflected",
            "path-traversal",
            "cmd-injection",
            "crypto-bad-mac"
        );
        when(mockContrastSDK.getRules(TEST_ORG_ID)).thenReturn(mockRules);

        // Act
        List<String> result = assessService.listVulnerabilityTypes();

        // Assert
        assertNotNull(result);
        assertEquals(5, result.size());

        // Verify sorted alphabetically
        assertEquals("cmd-injection", result.get(0));
        assertEquals("crypto-bad-mac", result.get(1));
        assertEquals("path-traversal", result.get(2));
        assertEquals("sql-injection", result.get(3));
        assertEquals("xss-reflected", result.get(4));

        verify(mockContrastSDK).getRules(TEST_ORG_ID);
    }

    @Test
    void testListVulnerabilityTypes_EmptyRules() throws Exception {
        // Arrange - SDK returns empty Rules object
        Rules emptyRules = new Rules();
        when(mockContrastSDK.getRules(TEST_ORG_ID)).thenReturn(emptyRules);

        // Act
        List<String> result = assessService.listVulnerabilityTypes();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Should return empty list when no rules available");
        verify(mockContrastSDK).getRules(TEST_ORG_ID);
    }

    @Test
    void testListVulnerabilityTypes_NullRulesObject() throws Exception {
        // Arrange - SDK returns null
        when(mockContrastSDK.getRules(TEST_ORG_ID)).thenReturn(null);

        // Act
        List<String> result = assessService.listVulnerabilityTypes();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty(), "Should return empty list when Rules object is null");
        verify(mockContrastSDK).getRules(TEST_ORG_ID);
    }

    @Test
    void testListVulnerabilityTypes_FiltersNullAndEmptyNames() throws Exception {
        // Arrange - Mix of valid, null, and empty names
        Rules mockRules = createMockRulesWithNulls(
            "sql-injection",
            null,
            "xss-reflected",
            "",
            "path-traversal",
            "   ",  // whitespace only - will be trimmed to empty
            "cmd-injection"
        );
        when(mockContrastSDK.getRules(TEST_ORG_ID)).thenReturn(mockRules);

        // Act
        List<String> result = assessService.listVulnerabilityTypes();

        // Assert
        assertNotNull(result);
        // Should only have the 4 valid names (whitespace-only gets trimmed to empty and filtered)
        assertEquals(4, result.size());
        assertTrue(result.contains("sql-injection"));
        assertTrue(result.contains("xss-reflected"));
        assertTrue(result.contains("path-traversal"));
        assertTrue(result.contains("cmd-injection"));

        // Verify sorted
        assertEquals("cmd-injection", result.get(0));
        assertEquals("path-traversal", result.get(1));
        assertEquals("sql-injection", result.get(2));
        assertEquals("xss-reflected", result.get(3));
    }

    @Test
    void testListVulnerabilityTypes_SDKThrowsException() throws Exception {
        // Arrange
        when(mockContrastSDK.getRules(TEST_ORG_ID))
            .thenThrow(new RuntimeException("API connection failed"));

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            assessService.listVulnerabilityTypes();
        });

        assertTrue(exception.getMessage().contains("Failed to retrieve vulnerability types"));
        verify(mockContrastSDK).getRules(TEST_ORG_ID);
    }

    @Test
    void testListVulnerabilityTypes_LargeRuleSet() throws Exception {
        // Arrange - Test with many rules to verify performance
        List<String> ruleNames = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            ruleNames.add("test-rule-" + i);
        }
        Rules mockRules = createMockRules(ruleNames.toArray(new String[0]));
        when(mockContrastSDK.getRules(TEST_ORG_ID)).thenReturn(mockRules);

        // Act
        List<String> result = assessService.listVulnerabilityTypes();

        // Assert
        assertNotNull(result);
        assertEquals(100, result.size());

        // Verify still sorted
        for (int i = 0; i < result.size() - 1; i++) {
            assertTrue(result.get(i).compareTo(result.get(i + 1)) < 0,
                "Rules should be sorted alphabetically");
        }
    }

    /**
     * Creates a mock Rules object with the specified rule names
     */
    private Rules createMockRules(String... ruleNames) {
        Rules rules = new Rules();
        List<Rules.Rule> ruleList = new ArrayList<>();

        for (String name : ruleNames) {
            Rules.Rule rule = rules.new Rule();
            // Use reflection to set the name since Rule doesn't have setters
            try {
                java.lang.reflect.Field nameField = Rules.Rule.class.getDeclaredField("name");
                nameField.setAccessible(true);
                nameField.set(rule, name);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create mock Rule", e);
            }
            ruleList.add(rule);
        }

        // Set the rules list using reflection
        try {
            java.lang.reflect.Field rulesField = Rules.class.getDeclaredField("rules");
            rulesField.setAccessible(true);
            rulesField.set(rules, ruleList);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set rules list", e);
        }

        return rules;
    }

    /**
     * Creates a mock Rules object that includes null/empty names for testing filtering
     */
    private Rules createMockRulesWithNulls(String... ruleNames) {
        Rules rules = new Rules();
        List<Rules.Rule> ruleList = new ArrayList<>();

        for (String name : ruleNames) {
            Rules.Rule rule = rules.new Rule();
            // Use reflection to set the name (including nulls and empty strings)
            try {
                java.lang.reflect.Field nameField = Rules.Rule.class.getDeclaredField("name");
                nameField.setAccessible(true);
                nameField.set(rule, name);
            } catch (Exception e) {
                throw new RuntimeException("Failed to create mock Rule", e);
            }
            ruleList.add(rule);
        }

        // Set the rules list using reflection
        try {
            java.lang.reflect.Field rulesField = Rules.class.getDeclaredField("rules");
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
        Traces mockTraces = createMockTraces(10, 10);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(
            1, 50, "CRITICAL,HIGH", null, null, null, null, null, null, null
        );

        // Assert
        assertNotNull(response);
        ArgumentCaptor<TraceFilterForm> captor = ArgumentCaptor.forClass(TraceFilterForm.class);
        verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

        TraceFilterForm form = captor.getValue();
        assertNotNull(form.getSeverities());
        assertEquals(2, form.getSeverities().size());
    }

    @Test
    void testGetAllVulnerabilities_InvalidSeverity_HardFailure() throws Exception {
        // Act - Invalid severity causes hard failure, SDK should not be called
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(
            1, 50, "CRITICAL,SUPER_HIGH", null, null, null, null, null, null, null
        );

        // Assert - Hard failure returns error response with empty items
        assertNotNull(response);
        assertTrue(response.items().isEmpty(), "Hard failure should return empty items");
        assertEquals(1, response.page());
        assertEquals(50, response.pageSize());
        assertEquals(0, response.totalItems());

        assertNotNull(response.message());
        assertTrue(response.message().contains("Invalid severity 'SUPER_HIGH'"));
        assertTrue(response.message().contains("Valid: CRITICAL, HIGH, MEDIUM, LOW, NOTE"));

        // Verify SDK was NOT called (hard failure stops execution)
        verify(mockContrastSDK, never()).getTracesInOrg(any(), any());
    }

    @Test
    void testGetAllVulnerabilities_StatusSmartDefaults() throws Exception {
        // Arrange
        Traces mockTraces = createMockTraces(10, 10);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act - no status provided, should use smart defaults
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(
            1, 50, null, null, null, null, null, null, null, null
        );

        // Assert
        assertNotNull(response);
        ArgumentCaptor<TraceFilterForm> captor = ArgumentCaptor.forClass(TraceFilterForm.class);
        verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

        TraceFilterForm form = captor.getValue();
        assertNotNull(form.getStatus());
        assertEquals(3, form.getStatus().size());
        assertTrue(form.getStatus().contains("Reported"));
        assertTrue(form.getStatus().contains("Suspicious"));
        assertTrue(form.getStatus().contains("Confirmed"));

        // Message content is tested in VulnerabilityFilterParamsTest
    }

    @Test
    void testGetAllVulnerabilities_StatusExplicitOverride() throws Exception {
        // Arrange
        Traces mockTraces = createMockTraces(10, 10);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act - explicitly provide statuses (including Fixed and Remediated)
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(
            1, 50, null, "Reported,Fixed,Remediated", null, null, null, null, null, null
        );

        // Assert
        assertNotNull(response);
        ArgumentCaptor<TraceFilterForm> captor = ArgumentCaptor.forClass(TraceFilterForm.class);
        verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

        TraceFilterForm form = captor.getValue();
        assertNotNull(form.getStatus());
        assertEquals(3, form.getStatus().size());
        assertTrue(form.getStatus().contains("Reported"));
        assertTrue(form.getStatus().contains("Fixed"));
        assertTrue(form.getStatus().contains("Remediated"));
    }

    @Test
    void testGetAllVulnerabilities_VulnTypesFilter() throws Exception {
        // Arrange
        Traces mockTraces = createMockTraces(10, 10);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(
            1, 50, null, null, null, "sql-injection,xss-reflected", null, null, null, null
        );

        // Assert
        assertNotNull(response);
        ArgumentCaptor<TraceFilterForm> captor = ArgumentCaptor.forClass(TraceFilterForm.class);
        verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

        TraceFilterForm form = captor.getValue();
        assertNotNull(form.getVulnTypes());
        assertEquals(2, form.getVulnTypes().size());
        assertTrue(form.getVulnTypes().contains("sql-injection"));
        assertTrue(form.getVulnTypes().contains("xss-reflected"));
    }

    @Test
    void testGetAllVulnerabilities_EnvironmentFilter() throws Exception {
        // Arrange
        Traces mockTraces = createMockTraces(10, 10);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(
            1, 50, null, null, null, null, "PRODUCTION,QA", null, null, null
        );

        // Assert
        assertNotNull(response);
        ArgumentCaptor<TraceFilterForm> captor = ArgumentCaptor.forClass(TraceFilterForm.class);
        verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

        TraceFilterForm form = captor.getValue();
        assertNotNull(form.getEnvironments());
        assertEquals(2, form.getEnvironments().size());
    }

    @Test
    void testGetAllVulnerabilities_DateFilterValid() throws Exception {
        // Arrange
        Traces mockTraces = createMockTraces(10, 10);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(
            1, 50, null, null, null, null, null, "2025-01-01", "2025-12-31", null
        );

        // Assert
        assertNotNull(response);
        ArgumentCaptor<TraceFilterForm> captor = ArgumentCaptor.forClass(TraceFilterForm.class);
        verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

        TraceFilterForm form = captor.getValue();
        assertNotNull(form.getStartDate());
        assertNotNull(form.getEndDate());

        // Message content is tested in VulnerabilityFilterParamsTest
    }


    @Test
    void testGetAllVulnerabilities_VulnTagsFilter() throws Exception {
        // Arrange
        Traces mockTraces = createMockTraces(10, 10);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(
            1, 50, null, null, null, null, null, null, null, "SmartFix Remediated,reviewed"
        );

        // Assert
        assertNotNull(response);
        ArgumentCaptor<TraceFilterForm> captor = ArgumentCaptor.forClass(TraceFilterForm.class);
        verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

        TraceFilterForm form = captor.getValue();
        assertNotNull(form.getFilterTags());
        assertEquals(2, form.getFilterTags().size());
        // Tags are URL-encoded to workaround SDK bug
        assertTrue(form.getFilterTags().contains("SmartFix+Remediated"));
        assertTrue(form.getFilterTags().contains("reviewed"));
    }

    @Test
    void testGetAllVulnerabilities_MultipleFilters() throws Exception {
        // Arrange
        Traces mockTraces = createMockTraces(10, 10);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act - combine severity, status, vulnTypes, and environment
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(
            1, 50,
            "CRITICAL,HIGH",
            "Reported,Confirmed",
            null,
            "sql-injection,cmd-injection",
            "PRODUCTION",
            "2025-01-01",
            null,
            null
        );

        // Assert
        assertNotNull(response);
        ArgumentCaptor<TraceFilterForm> captor = ArgumentCaptor.forClass(TraceFilterForm.class);
        verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

        TraceFilterForm form = captor.getValue();
        assertNotNull(form.getSeverities());
        assertNotNull(form.getStatus());
        assertNotNull(form.getVulnTypes());
        assertNotNull(form.getEnvironments());
        assertNotNull(form.getStartDate());
    }

    @Test
    void testGetAllVulnerabilities_AppIdRouting() throws Exception {
        // Arrange
        Traces mockTraces = createMockTraces(10, 10);
        String testAppId = "test-app-123";
        when(mockContrastSDK.getTraces(eq(TEST_ORG_ID), eq(testAppId), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(
            1, 50, null, null, testAppId, null, null, null, null, null
        );

        // Assert
        assertNotNull(response);
        // Verify it used app-specific API, not org-level
        verify(mockContrastSDK).getTraces(eq(TEST_ORG_ID), eq(testAppId), any(TraceFilterForm.class));
        verify(mockContrastSDK, never()).getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class));
    }

    @Test
    void testGetAllVulnerabilities_WhitespaceInFilters() throws Exception {
        // Arrange
        Traces mockTraces = createMockTraces(10, 10);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act - test whitespace handling: "CRITICAL , HIGH" instead of "CRITICAL,HIGH"
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(
            1, 50, "CRITICAL , HIGH , ", null, null, null, null, null, null, null
        );

        // Assert
        assertNotNull(response);
        ArgumentCaptor<TraceFilterForm> captor = ArgumentCaptor.forClass(TraceFilterForm.class);
        verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());

        TraceFilterForm form = captor.getValue();
        assertNotNull(form.getSeverities());
        assertEquals(2, form.getSeverities().size());
    }

    @Test
    void testGetAllVulnerabilities_EnvironmentsInResponse() throws Exception {
        // Arrange - Create traces with servers that have different environments
        Traces mockTraces = new Traces();
        List<Trace> traces = new ArrayList<>();

        // Trace 1: Multiple servers with different environments
        Trace trace1 = mock(Trace.class);
        when(trace1.getTitle()).thenReturn("SQL Injection");
        when(trace1.getRule()).thenReturn("sql-injection");
        when(trace1.getUuid()).thenReturn("uuid-1");
        when(trace1.getSeverity()).thenReturn("HIGH");
        when(trace1.getLastTimeSeen()).thenReturn(System.currentTimeMillis());
        when(trace1.getStatus()).thenReturn("REPORTED");
        when(trace1.getFirstTimeSeen()).thenReturn(System.currentTimeMillis() - 86400000L);
        when(trace1.getClosedTime()).thenReturn(null);

        // Set server_environments with different environments
        when(trace1.getServerEnvironments()).thenReturn(List.of("PRODUCTION", "QA", "PRODUCTION")); // Duplicate - should be deduplicated
        when(trace1.getTags()).thenReturn(new ArrayList<>());
        traces.add(trace1);

        // Trace 2: No servers
        Trace trace2 = mock(Trace.class);
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
        Trace trace3 = mock(Trace.class);
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
            java.lang.reflect.Field tracesField = Traces.class.getDeclaredField("traces");
            tracesField.setAccessible(true);
            tracesField.set(mockTraces, traces);

            java.lang.reflect.Field countField = Traces.class.getDeclaredField("count");
            countField.setAccessible(true);
            countField.set(mockTraces, 3);
        } catch (Exception e) {
            fail("Failed to setup mock traces: " + e.getMessage());
        }

        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(
            1, 50, null, null, null, null, null, null, null, null
        );

        // Assert
        assertNotNull(response);
        assertEquals(3, response.items().size());

        // Verify trace 1: Multiple environments, deduplicated and sorted
        VulnLight vuln1 = response.items().get(0);
        assertEquals("SQL Injection", vuln1.title());
        assertNotNull(vuln1.environments());
        assertEquals(2, vuln1.environments().size());
        assertTrue(vuln1.environments().contains("PRODUCTION"));
        assertTrue(vuln1.environments().contains("QA"));
        // Verify sorted order
        assertEquals("PRODUCTION", vuln1.environments().get(0));
        assertEquals("QA", vuln1.environments().get(1));

        // Verify trace 2: No servers = empty environments
        VulnLight vuln2 = response.items().get(1);
        assertEquals("XSS", vuln2.title());
        assertNotNull(vuln2.environments());
        assertEquals(0, vuln2.environments().size());

        // Verify trace 3: Single environment
        VulnLight vuln3 = response.items().get(2);
        assertEquals("Path Traversal", vuln3.title());
        assertNotNull(vuln3.environments());
        assertEquals(1, vuln3.environments().size());
        assertEquals("DEVELOPMENT", vuln3.environments().get(0));
    }

    @Test
    void testVulnLight_TimestampFields_ISO8601Format() throws Exception {
        // Arrange - Create trace with known timestamp values
        long lastSeen = JAN_15_2025_10_30_UTC;
        long firstSeen = JAN_1_2024_00_00_UTC;
        long closed = FEB_19_2025_13_20_UTC;

        Trace trace = mock(Trace.class);
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

        Traces mockTraces = mock(Traces.class);
        when(mockTraces.getTraces()).thenReturn(List.of(trace));
        when(mockTraces.getCount()).thenReturn(1);

        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(
            1, 50, null, null, null, null, null, null, null, null
        );

        // Assert
        assertNotNull(response);
        assertEquals(1, response.items().size());

        VulnLight vuln = response.items().get(0);

        // Verify field names use *At convention
        assertNotNull(vuln.lastSeenAt(), "lastSeenAt field should exist");
        assertNotNull(vuln.firstSeenAt(), "firstSeenAt field should exist");
        assertNotNull(vuln.closedAt(), "closedAt field should exist");

        // Verify ISO 8601 format with timezone offset (YYYY-MM-DDTHH:MM:SS+/-HH:MM)
        String iso8601Pattern = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}[+-]\\d{2}:\\d{2}";
        assertTrue(vuln.lastSeenAt().matches(iso8601Pattern),
            "lastSeenAt should be ISO 8601 with timezone: " + vuln.lastSeenAt());
        assertTrue(vuln.firstSeenAt().matches(iso8601Pattern),
            "firstSeenAt should be ISO 8601 with timezone: " + vuln.firstSeenAt());
        assertTrue(vuln.closedAt().matches(iso8601Pattern),
            "closedAt should be ISO 8601 with timezone: " + vuln.closedAt());

        // Verify timestamps include timezone offset
        assertTrue(vuln.lastSeenAt().contains("+") || vuln.lastSeenAt().contains("-"),
            "lastSeenAt should include timezone offset");
        assertTrue(vuln.firstSeenAt().contains("+") || vuln.firstSeenAt().contains("-"),
            "firstSeenAt should include timezone offset");
        assertTrue(vuln.closedAt().contains("+") || vuln.closedAt().contains("-"),
            "closedAt should include timezone offset");
    }

    @Test
    void testVulnLight_TimestampFields_NullHandling() throws Exception {
        // Arrange - Create trace with null timestamps
        Trace trace = mock(Trace.class);
        when(trace.getTitle()).thenReturn("Test Vulnerability");
        when(trace.getRule()).thenReturn("test-rule");
        when(trace.getUuid()).thenReturn("test-uuid-123");
        when(trace.getSeverity()).thenReturn("HIGH");
        when(trace.getStatus()).thenReturn("Reported");
        when(trace.getLastTimeSeen()).thenReturn(JAN_15_2025_10_30_UTC);  // lastSeen is required
        when(trace.getFirstTimeSeen()).thenReturn(null);  // optional
        when(trace.getClosedTime()).thenReturn(null);  // optional
        when(trace.getServerEnvironments()).thenReturn(new ArrayList<>());
        when(trace.getTags()).thenReturn(new ArrayList<>());

        Traces mockTraces = mock(Traces.class);
        when(mockTraces.getTraces()).thenReturn(List.of(trace));
        when(mockTraces.getCount()).thenReturn(1);

        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(
            1, 50, null, null, null, null, null, null, null, null
        );

        // Assert
        assertNotNull(response);
        assertEquals(1, response.items().size());

        VulnLight vuln = response.items().get(0);

        // Verify null timestamps are handled correctly
        assertNotNull(vuln.lastSeenAt(), "lastSeenAt should always be present");
        assertNull(vuln.firstSeenAt(), "firstSeenAt should be null when not set");
        assertNull(vuln.closedAt(), "closedAt should be null when not set");
    }
}
