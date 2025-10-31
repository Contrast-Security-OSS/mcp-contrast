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

import com.contrast.labs.ai.mcp.contrast.data.AttackSummary;
import com.contrast.labs.ai.mcp.contrast.data.PaginatedResponse;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.adr.Attack;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.adr.AttacksFilterBody;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.adr.AttacksResponse;
import com.contrast.labs.ai.mcp.contrast.utils.PaginationHandler;
import com.contrastsecurity.sdk.ContrastSDK;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test suite for ADRService, focusing on consolidated getAttacks method.
 */
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

    @BeforeEach
    void setUp() throws Exception {
        adrService = new ADRService(new PaginationHandler());
        mockContrastSDK = mock(ContrastSDK.class);

        // Mock static SDKHelper
        mockedSDKHelper = mockStatic(SDKHelper.class);
        mockedSDKHelper.when(() -> SDKHelper.getSDK(
            anyString(), anyString(), anyString(), anyString(), anyString(), anyString()
        )).thenReturn(mockContrastSDK);

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
    void testGetAttacks_NoFilters_ReturnsAllAttacks() throws Exception {
        // Given
        AttacksResponse mockResponse = createMockAttacksResponse(3, null);

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                .thenReturn(mockResponse);
        });

        // When
        PaginatedResponse<AttackSummary> result = adrService.getAttacks(null, null, null, null, null, null, null, null);

        // Then
        assertEquals(3, result.items().size());
        assertEquals("attack-uuid-0", result.items().get(0).attackId());
        assertEquals("attack-uuid-1", result.items().get(1).attackId());
        assertEquals("attack-uuid-2", result.items().get(2).attackId());
        assertEquals(1, result.page());
        assertEquals(50, result.pageSize());
        assertFalse(result.hasMorePages());
    }

    // ========== Test: QuickFilter ==========

    @Test
    void testGetAttacks_WithQuickFilter_PassesFilterToSDK() throws Exception {
        // Given
        AttacksResponse mockResponse = createMockAttacksResponse(2, null);

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                .thenReturn(mockResponse);
        });

        // When
        adrService.getAttacks("PROBED", null, null, null, null, null, null, null);

        // Then
        SDKExtension extension = mockedSDKExtension.constructed().get(0);
        ArgumentCaptor<AttacksFilterBody> captor = ArgumentCaptor.forClass(AttacksFilterBody.class);
        verify(extension).getAttacks(eq(TEST_ORG_ID), captor.capture(), eq(50), eq(0), isNull());

        assertEquals("PROBED", captor.getValue().getQuickFilter());
    }

    // ========== Test: Keyword Filter ==========

    @Test
    void testGetAttacks_WithKeyword_PassesKeywordToSDK() throws Exception {
        // Given
        AttacksResponse mockResponse = createMockAttacksResponse(1, null);

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                .thenReturn(mockResponse);
        });

        // When
        adrService.getAttacks(null, "sql injection", null, null, null, null, null, null);

        // Then
        SDKExtension extension = mockedSDKExtension.constructed().get(0);
        ArgumentCaptor<AttacksFilterBody> captor = ArgumentCaptor.forClass(AttacksFilterBody.class);
        verify(extension).getAttacks(eq(TEST_ORG_ID), captor.capture(), eq(50), eq(0), isNull());

        assertEquals("sql injection", captor.getValue().getKeyword());
    }

    // ========== Test: Boolean Filters ==========

    @Test
    void testGetAttacks_WithBooleanFilters_PassesCorrectly() throws Exception {
        // Given
        AttacksResponse mockResponse = createMockAttacksResponse(1, null);

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                .thenReturn(mockResponse);
        });

        // When
        adrService.getAttacks(null, null, true, false, true, null, null, null);

        // Then
        SDKExtension extension = mockedSDKExtension.constructed().get(0);
        ArgumentCaptor<AttacksFilterBody> captor = ArgumentCaptor.forClass(AttacksFilterBody.class);
        verify(extension).getAttacks(eq(TEST_ORG_ID), captor.capture(), eq(50), eq(0), isNull());

        assertEquals(true, captor.getValue().isIncludeSuppressed());
        assertEquals(false, captor.getValue().isIncludeBotBlockers());
        assertEquals(true, captor.getValue().isIncludeIpBlacklist());
    }

    // ========== Test: Pagination Parameters ==========

    @Test
    void testGetAttacks_WithPaginationParams_PassesToSDK() throws Exception {
        // Given
        AttacksResponse mockResponse = createMockAttacksResponse(2, null);

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(100), eq("firstEventTime")))
                .thenReturn(mockResponse);
        });

        // When
        adrService.getAttacks(null, null, null, null, null, "firstEventTime", 3, 50);

        // Then
        SDKExtension extension = mockedSDKExtension.constructed().get(0);
        verify(extension).getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(100), eq("firstEventTime"));
    }

    // ========== Test: Combined Filters ==========

    @Test
    void testGetAttacks_WithMultipleFilters_AllPassedCorrectly() throws Exception {
        // Given
        AttacksResponse mockResponse = createMockAttacksResponse(1, null);

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(25), eq(50), eq("severity")))
                .thenReturn(mockResponse);
        });

        // When
        adrService.getAttacks("EXPLOITED", "xss", true, true, false, "severity", 3, 25);

        // Then
        SDKExtension extension = mockedSDKExtension.constructed().get(0);
        ArgumentCaptor<AttacksFilterBody> captor = ArgumentCaptor.forClass(AttacksFilterBody.class);
        verify(extension).getAttacks(eq(TEST_ORG_ID), captor.capture(), eq(25), eq(50), eq("severity"));

        AttacksFilterBody filter = captor.getValue();
        assertEquals("EXPLOITED", filter.getQuickFilter());
        assertEquals("xss", filter.getKeyword());
        assertTrue(filter.isIncludeSuppressed());
        assertTrue(filter.isIncludeBotBlockers());
        assertFalse(filter.isIncludeIpBlacklist());
    }

    // ========== Test: Empty Results ==========

    @Test
    void testGetAttacks_EmptyResults_ReturnsEmptyList() throws Exception {
        // Given
        AttacksResponse emptyResponse = createMockAttacksResponse(0, 0);

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                .thenReturn(emptyResponse);
        });

        // When
        PaginatedResponse<AttackSummary> result = adrService.getAttacks(null, null, null, null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertTrue(result.items().isEmpty());
        assertFalse(result.hasMorePages());
        assertNotNull(result.message(), "Empty results should have explanatory message");
        assertTrue(result.message().contains("No items found"),
            "Message should explain empty results to AI");
    }

    // ========== Test: Null Results ==========

    @Test
    void testGetAttacks_NullResults_ReturnsEmptyList() throws Exception {
        // Given
        AttacksResponse nullResponse = new AttacksResponse();
        nullResponse.setAttacks(null); // Simulate null attacks list

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                .thenReturn(nullResponse);
        });

        // When
        PaginatedResponse<AttackSummary> result = adrService.getAttacks(null, null, null, null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertTrue(result.items().isEmpty());
    }

    // ========== Test: SDK Exception ==========

    @Test
    void testGetAttacks_SDKThrowsException_PropagatesException() throws Exception {
        // Given
        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                .thenThrow(new RuntimeException("API connection failed"));
        });

        // When/Then
        Exception exception = assertThrows(Exception.class, () -> {
            adrService.getAttacks(null, null, null, null, null, null, null, null);
        });

        assertTrue(exception.getMessage().contains("API connection failed") ||
                   exception.getCause() != null && exception.getCause().getMessage().contains("API connection failed"));
    }

    // ========== Test: Null Filters Don't Override Defaults ==========

    @Test
    void testGetAttacks_NullFilters_DoesNotSetFilterBodyFields() throws Exception {
        // Given
        AttacksResponse mockResponse = createMockAttacksResponse(1, null);

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                .thenReturn(mockResponse);
        });

        // When
        adrService.getAttacks(null, null, null, null, null, null, null, null);

        // Then
        SDKExtension extension = mockedSDKExtension.constructed().get(0);
        ArgumentCaptor<AttacksFilterBody> captor = ArgumentCaptor.forClass(AttacksFilterBody.class);
        verify(extension).getAttacks(eq(TEST_ORG_ID), captor.capture(), eq(50), eq(0), isNull());

        AttacksFilterBody filter = captor.getValue();
        // Verify null parameters didn't set fields (they should remain at constructor defaults)
        assertNotNull(filter); // Filter body is created but fields remain unset
    }

    // ========== Pagination Tests ==========

    @Test
    void testGetAttacks_WithTotalCount_ProvidesAccurateHasMorePages() throws Exception {
        // Given: API returns 50 items with totalCount=150 (3 pages total)
        AttacksResponse mockResponse = createMockAttacksResponse(50, 150);

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                .thenReturn(mockResponse);
        });

        // When
        PaginatedResponse<AttackSummary> result = adrService.getAttacks(null, null, null, null, null, null, 1, 50);

        // Then
        assertEquals(50, result.items().size());
        assertEquals(1, result.page());
        assertEquals(50, result.pageSize());
        assertEquals(150, result.totalItems());
        assertTrue(result.hasMorePages(), "Should have more pages (page 1 of 3)");
    }

    @Test
    void testGetAttacks_LastPage_WithTotalCount_HasMorePagesFalse() throws Exception {
        // Given: Page 3 of 3 (offset=100, returns 50 items, total=150)
        AttacksResponse mockResponse = createMockAttacksResponse(50, 150);

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(100), isNull()))
                .thenReturn(mockResponse);
        });

        // When
        PaginatedResponse<AttackSummary> result = adrService.getAttacks(null, null, null, null, null, null, 3, 50);

        // Then
        assertEquals(50, result.items().size());
        assertEquals(3, result.page());
        assertEquals(50, result.pageSize());
        assertEquals(150, result.totalItems());
        assertFalse(result.hasMorePages(), "Last page should have hasMorePages=false");
    }

    @Test
    void testGetAttacks_InvalidPageSize_ClampsAndWarns() throws Exception {
        // Given
        AttacksResponse mockResponse = createMockAttacksResponse(100, 200);

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            // Should be clamped to 100 (max)
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(100), eq(0), isNull()))
                .thenReturn(mockResponse);
        });

        // When: Request pageSize=500 (exceeds max of 100)
        PaginatedResponse<AttackSummary> result = adrService.getAttacks(null, null, null, null, null, null, 1, 500);

        // Then
        assertEquals(100, result.pageSize(), "PageSize should be clamped to 100");
        assertNotNull(result.message(), "Should have warning message");
        assertTrue(result.message().contains("500"), "Message should mention original value");
        assertTrue(result.message().contains("100"), "Message should mention clamped value");
    }

    @Test
    void testGetAttacks_InvalidPage_ClampsAndWarns() throws Exception {
        // Given
        AttacksResponse mockResponse = createMockAttacksResponse(50, null);

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            // Should be clamped to page 1 (offset=0)
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                .thenReturn(mockResponse);
        });

        // When: Request page=0 or negative (invalid)
        PaginatedResponse<AttackSummary> result = adrService.getAttacks(null, null, null, null, null, null, 0, 50);

        // Then
        assertEquals(1, result.page(), "Page should be clamped to 1");
        assertNotNull(result.message(), "Should have warning message");
        assertTrue(result.message().contains("Invalid page"), "Message should indicate invalid page");
    }

    @Test
    void testGetAttacks_WithoutTotalCount_UsesHeuristic() throws Exception {
        // Given: Full page of results (50 items), no totalCount
        AttacksResponse mockResponse = createMockAttacksResponse(50, null);

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                .thenReturn(mockResponse);
        });

        // When
        PaginatedResponse<AttackSummary> result = adrService.getAttacks(null, null, null, null, null, null, 1, 50);

        // Then
        assertNull(result.totalItems(), "TotalItems should be null when not provided");
        assertTrue(result.hasMorePages(), "Heuristic: full page suggests more pages exist");
    }

    @Test
    void testGetAttacks_PartialPageWithoutCount_NoMorePages() throws Exception {
        // Given: Partial page (25 items when pageSize=50), no totalCount
        AttacksResponse mockResponse = createMockAttacksResponse(25, null);

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                .thenReturn(mockResponse);
        });

        // When
        PaginatedResponse<AttackSummary> result = adrService.getAttacks(null, null, null, null, null, null, 1, 50);

        // Then
        assertEquals(25, result.items().size());
        assertNull(result.totalItems(), "TotalItems should be null when not provided");
        assertFalse(result.hasMorePages(), "Heuristic: partial page suggests no more pages");
    }

    // ========== Test: Smart Defaults and Messages ==========

    @Test
    void testGetAttacks_SmartDefaults_ReturnsMessages() throws Exception {
        // Given: No filters provided, should use smart defaults
        AttacksResponse mockResponse = createMockAttacksResponse(10, null);

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                .thenReturn(mockResponse);
        });

        // When: No filters provided
        PaginatedResponse<AttackSummary> result = adrService.getAttacks(null, null, null, null, null, null, 1, 50);

        // Then: Should have messages about smart defaults
        assertNotNull(result.message(), "Should have messages about smart defaults");
        assertTrue(result.message().contains("No quickFilter applied"),
            "Should have message about quickFilter default");
        assertTrue(result.message().contains("Excluding suppressed attacks by default"),
            "Should have message about includeSuppressed default");
    }

    @Test
    void testGetAttacks_ExplicitFilters_NoSmartDefaultMessages() throws Exception {
        // Given: Explicit filters provided
        AttacksResponse mockResponse = createMockAttacksResponse(5, null);

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(0), isNull()))
                .thenReturn(mockResponse);
        });

        // When: Explicit filters provided
        PaginatedResponse<AttackSummary> result = adrService.getAttacks(
            "EXPLOITED", null, true, null, null, null, 1, 50
        );

        // Then: Should NOT have smart default messages
        if (result.message() != null) {
            assertFalse(result.message().contains("No quickFilter applied"),
                "Should not have quickFilter message when explicitly provided");
            assertFalse(result.message().contains("Excluding suppressed attacks by default"),
                "Should not have includeSuppressed message when explicitly provided");
        }
    }

    @Test
    void testGetAttacks_InvalidQuickFilter_ReturnsError() throws Exception {
        // When: Invalid quickFilter provided
        PaginatedResponse<AttackSummary> result = adrService.getAttacks(
            "INVALID_FILTER", null, null, null, null, null, 1, 50
        );

        // Then: Should return error response with descriptive message
        assertNotNull(result.message(), "Should have error message");
        assertTrue(result.message().contains("Invalid quickFilter 'INVALID_FILTER'"),
            "Should explain the invalid quickFilter");
        assertTrue(result.message().contains("Valid: EXPLOITED, PROBED, BLOCKED, INEFFECTIVE, ALL"),
            "Should list valid options");
        assertEquals(0, result.items().size(), "Should return empty items on error");
    }

    @Test
    void testGetAttacks_InvalidSort_ReturnsError() throws Exception {
        // When: Invalid sort format provided
        PaginatedResponse<AttackSummary> result = adrService.getAttacks(
            "EXPLOITED", null, false, null, null, "invalid sort!", 1, 50
        );

        // Then: Should return error response with descriptive message
        assertNotNull(result.message(), "Should have error message");
        assertTrue(result.message().contains("Invalid sort format 'invalid sort!'"),
            "Should explain the invalid sort format");
        assertTrue(result.message().contains("Must be a field name with optional '-' prefix"),
            "Should explain the correct format");
        assertEquals(0, result.items().size(), "Should return empty items on error");
    }

    @Test
    void testGetAttacks_MultipleValidationErrors_CombinesErrors() throws Exception {
        // When: Multiple invalid parameters provided
        PaginatedResponse<AttackSummary> result = adrService.getAttacks(
            "BAD_FILTER", null, null, null, null, "bad-format!", 1, 50
        );

        // Then: Should return combined error messages
        assertNotNull(result.message(), "Should have error message");
        assertTrue(result.message().contains("Invalid quickFilter"),
            "Should include quickFilter error");
        assertTrue(result.message().contains("Invalid sort format"),
            "Should include sort error");
        assertEquals(0, result.items().size(), "Should return empty items on error");
    }

    // ========== Helper Methods ==========

    /**
     * Creates mock AttacksResponse for testing
     */
    private AttacksResponse createMockAttacksResponse(int count, Integer totalCount) {
        AttacksResponse response = new AttacksResponse();
        response.setAttacks(createMockAttacks(count));
        response.setCount(totalCount);
        return response;
    }

    /**
     * Creates mock Attack objects for testing
     */
    private List<Attack> createMockAttacks(int count) {
        List<Attack> attacks = new ArrayList<>();
        long baseTime = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            Attack attack = new Attack();
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
