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
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(1, null);

        // Assert
        PaginationTestHelper.assertValidPaginatedResponse(response, 1, 50);
        assertEquals(50, response.items().size());
        assertEquals(150, response.totalItems());
        assertTrue(response.hasMorePages());
        assertNull(response.message());

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
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(2, 50);

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
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(3, 50);

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
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(5, 50);

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
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(1, 50);

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
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(1, 50);

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
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(1, 100);

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
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(1, 150);

        // Assert - Should be clamped to 100
        PaginationTestHelper.assertValidPaginatedResponse(response, 1, 100);
        PaginationTestHelper.assertHasValidationMessage(response);
        assertTrue(response.message().contains("exceeds maximum 100"),
                  "Message should explain pageSize was clamped");
    }

    // ========== Test Case 7: Invalid Page Input ==========
    @Test
    void testGetAllVulnerabilities_InvalidPageZero() throws Exception {
        // Arrange
        Traces mockTraces = createMockTraces(50, 100);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(0, 50);

        // Assert - Should be clamped to 1
        PaginationTestHelper.assertValidPaginatedResponse(response, 1, 50);
        PaginationTestHelper.assertHasValidationMessage(response);
        assertTrue(response.message().contains("Invalid page number 0"),
                  "Message should explain page was clamped");
    }

    @Test
    void testGetAllVulnerabilities_InvalidPageNegative() throws Exception {
        // Arrange
        Traces mockTraces = createMockTraces(50, 100);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(-5, 50);

        // Assert - Should be clamped to 1
        PaginationTestHelper.assertValidPaginatedResponse(response, 1, 50);
        PaginationTestHelper.assertHasValidationMessage(response);
        assertTrue(response.message().contains("Invalid page number -5"),
                  "Message should explain page was clamped");
    }

    // ========== Test Case 8: Invalid PageSize Input ==========
    @Test
    void testGetAllVulnerabilities_InvalidPageSizeZero() throws Exception {
        // Arrange
        Traces mockTraces = createMockTraces(50, 100);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(1, 0);

        // Assert - Should be clamped to 50
        PaginationTestHelper.assertValidPaginatedResponse(response, 1, 50);
        PaginationTestHelper.assertHasValidationMessage(response);
        assertTrue(response.message().contains("Invalid pageSize 0"),
                  "Message should explain pageSize was clamped");
    }

    @Test
    void testGetAllVulnerabilities_InvalidPageSizeNegative() throws Exception {
        // Arrange
        Traces mockTraces = createMockTraces(50, 100);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(1, -10);

        // Assert - Should be clamped to 50
        PaginationTestHelper.assertValidPaginatedResponse(response, 1, 50);
        PaginationTestHelper.assertHasValidationMessage(response);
        assertTrue(response.message().contains("Invalid pageSize -10"),
                  "Message should explain pageSize was clamped");
    }

    // ========== Test Case 9: Single Page Results ==========
    @Test
    void testGetAllVulnerabilities_SinglePageResults() throws Exception {
        // Arrange - Only 25 items total, fits in one page
        Traces mockTraces = createMockTraces(25, 25);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(1, 50);

        // Assert
        PaginationTestHelper.assertValidPaginatedResponse(response, 1, 50);
        assertEquals(25, response.items().size());
        assertEquals(25, response.totalItems());
        assertFalse(response.hasMorePages(), "Single page should have no more pages");
        PaginationTestHelper.assertNoValidationMessage(response);
    }

    // ========== Test Case 10: Null Parameters ==========
    @Test
    void testGetAllVulnerabilities_NullParameters() throws Exception {
        // Arrange
        Traces mockTraces = createMockTraces(50, 100);
        when(mockContrastSDK.getTracesInOrg(eq(TEST_ORG_ID), any(TraceFilterForm.class)))
            .thenReturn(mockTraces);

        // Act
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(null, null);

        // Assert - Should use defaults: page=1, pageSize=50
        PaginationTestHelper.assertValidPaginatedResponse(response, 1, 50);
        assertEquals(50, response.items().size());
        PaginationTestHelper.assertNoValidationMessage(response);

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
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(1, 50);

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
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(1, 50);

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
        PaginatedResponse<VulnLight> response = assessService.getAllVulnerabilities(1, 50);

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
}
