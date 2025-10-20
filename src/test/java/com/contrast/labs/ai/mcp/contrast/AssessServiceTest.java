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

import com.contrast.labs.ai.mcp.contrast.data.PaginatedResponse;
import com.contrast.labs.ai.mcp.contrast.data.VulnLight;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.utils.PaginationTestHelper;
import com.contrastsecurity.http.TraceFilterForm;
import com.contrastsecurity.models.Trace;
import com.contrastsecurity.models.Traces;
import com.contrastsecurity.models.Rules;
import com.contrastsecurity.models.Server;
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

    @InjectMocks
    private AssessService assessService;

    @Mock
    private ContrastSDK mockContrastSDK;

    private MockedStatic<SDKHelper> mockedSDKHelper;

    private static final String TEST_ORG_ID = "test-org-123";
    private static final String TEST_HOST = "https://test.contrast.local";
    private static final String TEST_API_KEY = "test-api-key";
    private static final String TEST_SERVICE_KEY = "test-service-key";
    private static final String TEST_USERNAME = "test-user";

    @BeforeEach
    void setUp() throws Exception {
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

    // ========== Test Case 1: First Page ==========
    @Test
    void testGetAllVulnerabilities_FirstPage_DefaultPageSize() throws Exception {
        // Arrange
        Traces mockTraces = createMockTraces(50, 150);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(1, null, null, null, null, null, null, null, null, null);

        // Assert
        PaginationTestHelper.assertValidPaginatedResponse(response, 1, 50);
        assertEquals(50, response.items().size());
        assertEquals(150, response.totalItems());
        assertTrue(response.hasMorePages());

        // Verify correct offset was used
        ArgumentCaptor<TraceFilterForm> captor = ArgumentCaptor.forClass(TraceFilterForm.class);
        verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());
        assertEquals(0, captor.getValue().getOffset());
        assertEquals(50, captor.getValue().getLimit());
    }

    // ========== Test Case 2: Middle Page ==========
    @Test
    void testGetAllVulnerabilities_MiddlePage() throws Exception {
        // Arrange
        Traces mockTraces = createMockTraces(50, 150);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(2, 50, null, null, null, null, null, null, null, null);

        // Assert
        PaginationTestHelper.assertValidPaginatedResponse(response, 2, 50);
        assertEquals(50, response.items().size());
        assertEquals(150, response.totalItems());
        assertTrue(response.hasMorePages());

        // Verify correct offset (page 2 = offset 50)
        ArgumentCaptor<TraceFilterForm> captor = ArgumentCaptor.forClass(TraceFilterForm.class);
        verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());
        assertEquals(50, captor.getValue().getOffset());
        assertEquals(50, captor.getValue().getLimit());
    }

    // ========== Test Case 3: Last Page ==========
    @Test
    void testGetAllVulnerabilities_LastPage() throws Exception {
        // Arrange - 125 total items, page 3 with pageSize 50 = last page (25 items)
        Traces mockTraces = createMockTraces(25, 125);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(3, 50, null, null, null, null, null, null, null, null);

        // Assert
        PaginationTestHelper.assertValidPaginatedResponse(response, 3, 50);
        assertEquals(25, response.items().size());
        assertEquals(125, response.totalItems());
        assertFalse(response.hasMorePages(), "Last page should have no more pages");

        // Verify correct offset (page 3 = offset 100)
        ArgumentCaptor<TraceFilterForm> captor = ArgumentCaptor.forClass(TraceFilterForm.class);
        verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());
        assertEquals(100, captor.getValue().getOffset());
    }

    // ========== Test Case 4: Beyond Last Page ==========
    @Test
    void testGetAllVulnerabilities_BeyondLastPage() throws Exception {
        // Arrange - 100 total items, requesting page 5 (way beyond available - only 2 pages exist)
        // Page 5 offset=200 is beyond the data, org API returns empty result
        Traces emptyPage = createMockTraces(0, 100);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(emptyPage);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(5, 50, null, null, null, null, null, null, null, null);

        // Assert
        PaginationTestHelper.assertValidPaginatedResponse(response, 5, 50);
        PaginationTestHelper.assertEmptyPage(response);
        PaginationTestHelper.assertHasValidationMessage(response);
        assertTrue(response.message().contains("exceeds available pages"),
                  "Message should explain page exceeds available pages (total: 2)");
        assertTrue(response.message().contains("total: 2"),
                  "Message should show total pages available");
    }

    // ========== Test Case 5: Empty Results ==========
    @Test
    void testGetAllVulnerabilities_EmptyResults() throws Exception {
        // Arrange - Organization API returns valid empty response (empty list + count=0 is valid per teamserver)
        Traces emptyResult = createMockTraces(0, 0);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(emptyResult);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(1, 50, null, null, null, null, null, null, null, null);

        // Assert
        PaginationTestHelper.assertValidPaginatedResponse(response, 1, 50);
        PaginationTestHelper.assertEmptyPage(response);
        assertEquals(0, response.totalItems(), "Empty result should have totalItems=0");
        PaginationTestHelper.assertHasValidationMessage(response);
        assertTrue(response.message().contains("No vulnerabilities found"),
                  "Message should explain no results found");
    }

    // ========== Test Case 6: Page Size Boundaries ==========
    @Test
    void testGetAllVulnerabilities_PageSizeDefault() throws Exception {
        // Arrange
        Traces mockTraces = createMockTraces(50, 200);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(1, 50, null, null, null, null, null, null, null, null);

        // Assert
        PaginationTestHelper.assertValidPaginatedResponse(response, 1, 50);
        assertEquals(50, response.items().size());
    }

    @Test
    void testGetAllVulnerabilities_PageSizeMaximum() throws Exception {
        // Arrange
        Traces mockTraces = createMockTraces(100, 200);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(1, 100, null, null, null, null, null, null, null, null);

        // Assert
        PaginationTestHelper.assertValidPaginatedResponse(response, 1, 100);
        assertEquals(100, response.items().size());
    }

    @Test
    void testGetAllVulnerabilities_PageSizeExceedsMaximum() throws Exception {
        // Arrange
        Traces mockTraces = createMockTraces(100, 200);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(1, 150, null, null, null, null, null, null, null, null);

        // Assert - Should be clamped to 100
        PaginationTestHelper.assertValidPaginatedResponse(response, 1, 100);
        PaginationTestHelper.assertHasValidationMessage(response);
        assertTrue(response.message().contains("exceeds maximum 100"),
                  "Message should explain pageSize was clamped");
    }

    // ========== Test Case 7: Single Page Results ==========
    @Test
    void testGetAllVulnerabilities_SinglePageResults() throws Exception {
        // Arrange - Only 25 items total, fits in one page
        Traces mockTraces = createMockTraces(25, 25);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(1, 50, null, null, null, null, null, null, null, null);

        // Assert
        PaginationTestHelper.assertValidPaginatedResponse(response, 1, 50);
        assertEquals(25, response.items().size());
        assertEquals(25, response.totalItems());
        assertFalse(response.hasMorePages(), "Single page should have no more pages");
    }

    // ========== Test Case 10: Null Parameters ==========
    @Test
    void testGetAllVulnerabilities_NullParameters() throws Exception {
        // Arrange
        Traces mockTraces = createMockTraces(50, 100);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(null, null, null, null, null, null, null, null, null, null);

        // Assert - Should use defaults: page=1, pageSize=50
        PaginationTestHelper.assertValidPaginatedResponse(response, 1, 50);
        assertEquals(50, response.items().size());

        // Verify defaults were applied
        ArgumentCaptor<TraceFilterForm> captor = ArgumentCaptor.forClass(TraceFilterForm.class);
        verify(mockContrastSDK).getTracesInOrg(eq(TEST_ORG_ID), captor.capture());
        assertEquals(0, captor.getValue().getOffset()); // page 1 = offset 0
        assertEquals(50, captor.getValue().getLimit()); // default pageSize
    }

    // ========== Test Case 11: totalItems Scenarios ==========
    @Test
    void testGetAllVulnerabilities_TotalItemsAvailable() throws Exception {
        // Arrange - SDK provides totalItems
        Traces mockTraces = createMockTraces(50, 200);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(1, 50, null, null, null, null, null, null, null, null);

        // Assert
        assertNotNull(response.totalItems(), "totalItems should be available from SDK");
        assertEquals(200, response.totalItems());
        PaginationTestHelper.assertHasMorePagesLogic(response);
    }

    @Test
    void testGetAllVulnerabilities_TotalItemsNull_FullPage() throws Exception {
        // Arrange - SDK doesn't provide count (null)
        Traces mockTraces = createMockTraces(50, null);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(1, 50, null, null, null, null, null, null, null, null);

        // Assert - Should use heuristic: full page = assume more exist
        assertNull(response.totalItems(), "totalItems should be null when not provided by SDK");
        assertTrue(response.hasMorePages(),
                  "Heuristic: full page should assume more pages exist");
    }

    @Test
    void testGetAllVulnerabilities_TotalItemsNull_PartialPage() throws Exception {
        // Arrange - SDK doesn't provide count, partial page returned
        Traces mockTraces = createMockTraces(25, null);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(1, 50, null, null, null, null, null, null, null, null);

        // Assert - Heuristic: partial page = no more pages
        assertNull(response.totalItems(), "totalItems should be null when not provided by SDK");
        assertFalse(response.hasMorePages(),
                   "Heuristic: partial page should assume no more pages");
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
        assertFalse(response.hasMorePages());

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

        // Should have message about smart defaults
        assertTrue(response.message().contains("actionable vulnerabilities only"));
        assertTrue(response.message().contains("excluding Fixed and Remediated"));
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

        // Should NOT have message about smart defaults when explicitly provided
        if (response.message() != null) {
            assertFalse(response.message().contains("actionable vulnerabilities only"));
        }
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

        // Should have message about time filter applying to lastTimeSeen
        assertTrue(response.message().contains("LAST ACTIVITY DATE"));
        assertTrue(response.message().contains("lastTimeSeen"));
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

        // Create servers with different environments
        Server server1 = mock(Server.class);
        when(server1.getEnvironment()).thenReturn("PRODUCTION");
        Server server2 = mock(Server.class);
        when(server2.getEnvironment()).thenReturn("QA");
        Server server3 = mock(Server.class);
        when(server3.getEnvironment()).thenReturn("PRODUCTION"); // Duplicate - should be deduplicated

        List<Server> servers1 = List.of(server1, server2, server3);
        when(trace1.getServers()).thenReturn(servers1);
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
        when(trace2.getServers()).thenReturn(new ArrayList<>());
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

        Server server4 = mock(Server.class);
        when(server4.getEnvironment()).thenReturn("DEVELOPMENT");
        when(trace3.getServers()).thenReturn(List.of(server4));
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
        long lastSeen = 1736938200000L;      // Jan 15, 2025 10:30:00 UTC
        long firstSeen = 1704067200000L;     // Jan 1, 2024 00:00:00 UTC
        long closed = 1740000000000L;        // Feb 19, 2025 13:20:00 UTC

        Trace trace = mock(Trace.class);
        when(trace.getTitle()).thenReturn("Test Vulnerability");
        when(trace.getRule()).thenReturn("test-rule");
        when(trace.getUuid()).thenReturn("test-uuid-123");
        when(trace.getSeverity()).thenReturn("HIGH");
        when(trace.getStatus()).thenReturn("Reported");
        when(trace.getLastTimeSeen()).thenReturn(lastSeen);
        when(trace.getFirstTimeSeen()).thenReturn(firstSeen);
        when(trace.getClosedTime()).thenReturn(closed);
        when(trace.getServers()).thenReturn(new ArrayList<>());

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
        when(trace.getLastTimeSeen()).thenReturn(1736938200000L);  // lastSeen is required
        when(trace.getFirstTimeSeen()).thenReturn(null);  // optional
        when(trace.getClosedTime()).thenReturn(null);  // optional
        when(trace.getServers()).thenReturn(new ArrayList<>());

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
