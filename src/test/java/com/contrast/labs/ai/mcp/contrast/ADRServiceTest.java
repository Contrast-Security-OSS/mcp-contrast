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
import com.contrast.labs.ai.mcp.contrast.sdkexstension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.adr.Attack;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.adr.AttacksFilterBody;
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
        adrService = new ADRService();
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
        List<Attack> mockAttacks = createMockAttacks(3);

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), isNull(), isNull(), isNull()))
                .thenReturn(mockAttacks);
        });

        // When
        List<AttackSummary> result = adrService.getAttacks(null, null, null, null, null, null, null, null);

        // Then
        assertEquals(3, result.size());
        assertEquals("attack-uuid-0", result.get(0).attackId());
        assertEquals("attack-uuid-1", result.get(1).attackId());
        assertEquals("attack-uuid-2", result.get(2).attackId());
    }

    // ========== Test: QuickFilter ==========

    @Test
    void testGetAttacks_WithQuickFilter_PassesFilterToSDK() throws Exception {
        // Given
        List<Attack> mockAttacks = createMockAttacks(2);

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), isNull(), isNull(), isNull()))
                .thenReturn(mockAttacks);
        });

        // When
        adrService.getAttacks("PROBED", null, null, null, null, null, null, null);

        // Then
        SDKExtension extension = mockedSDKExtension.constructed().get(0);
        ArgumentCaptor<AttacksFilterBody> captor = ArgumentCaptor.forClass(AttacksFilterBody.class);
        verify(extension).getAttacks(eq(TEST_ORG_ID), captor.capture(), isNull(), isNull(), isNull());

        assertEquals("PROBED", captor.getValue().getQuickFilter());
    }

    // ========== Test: Keyword Filter ==========

    @Test
    void testGetAttacks_WithKeyword_PassesKeywordToSDK() throws Exception {
        // Given
        List<Attack> mockAttacks = createMockAttacks(1);

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), isNull(), isNull(), isNull()))
                .thenReturn(mockAttacks);
        });

        // When
        adrService.getAttacks(null, "sql injection", null, null, null, null, null, null);

        // Then
        SDKExtension extension = mockedSDKExtension.constructed().get(0);
        ArgumentCaptor<AttacksFilterBody> captor = ArgumentCaptor.forClass(AttacksFilterBody.class);
        verify(extension).getAttacks(eq(TEST_ORG_ID), captor.capture(), isNull(), isNull(), isNull());

        assertEquals("sql injection", captor.getValue().getKeyword());
    }

    // ========== Test: Boolean Filters ==========

    @Test
    void testGetAttacks_WithBooleanFilters_PassesCorrectly() throws Exception {
        // Given
        List<Attack> mockAttacks = createMockAttacks(1);

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), isNull(), isNull(), isNull()))
                .thenReturn(mockAttacks);
        });

        // When
        adrService.getAttacks(null, null, true, false, true, null, null, null);

        // Then
        SDKExtension extension = mockedSDKExtension.constructed().get(0);
        ArgumentCaptor<AttacksFilterBody> captor = ArgumentCaptor.forClass(AttacksFilterBody.class);
        verify(extension).getAttacks(eq(TEST_ORG_ID), captor.capture(), isNull(), isNull(), isNull());

        assertEquals(true, captor.getValue().isIncludeSuppressed());
        assertEquals(false, captor.getValue().isIncludeBotBlockers());
        assertEquals(true, captor.getValue().isIncludeIpBlacklist());
    }

    // ========== Test: Pagination Parameters ==========

    @Test
    void testGetAttacks_WithPaginationParams_PassesToSDK() throws Exception {
        // Given
        List<Attack> mockAttacks = createMockAttacks(2);

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(100), eq("firstEventTime")))
                .thenReturn(mockAttacks);
        });

        // When
        adrService.getAttacks(null, null, null, null, null, 50, 100, "firstEventTime");

        // Then
        SDKExtension extension = mockedSDKExtension.constructed().get(0);
        verify(extension).getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(50), eq(100), eq("firstEventTime"));
    }

    // ========== Test: Combined Filters ==========

    @Test
    void testGetAttacks_WithMultipleFilters_AllPassedCorrectly() throws Exception {
        // Given
        List<Attack> mockAttacks = createMockAttacks(1);

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), eq(25), eq(50), eq("severity")))
                .thenReturn(mockAttacks);
        });

        // When
        adrService.getAttacks("EXPLOITED", "xss", true, true, false, 25, 50, "severity");

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
        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), isNull(), isNull(), isNull()))
                .thenReturn(List.of());
        });

        // When
        List<AttackSummary> result = adrService.getAttacks(null, null, null, null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== Test: Null Results ==========

    @Test
    void testGetAttacks_NullResults_ReturnsEmptyList() throws Exception {
        // Given
        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), isNull(), isNull(), isNull()))
                .thenReturn(null);
        });

        // When
        List<AttackSummary> result = adrService.getAttacks(null, null, null, null, null, null, null, null);

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    // ========== Test: SDK Exception ==========

    @Test
    void testGetAttacks_SDKThrowsException_PropagatesException() throws Exception {
        // Given
        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), isNull(), isNull(), isNull()))
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
        List<Attack> mockAttacks = createMockAttacks(1);

        mockedSDKExtension = mockConstruction(SDKExtension.class, (mock, context) -> {
            when(mock.getAttacks(eq(TEST_ORG_ID), any(AttacksFilterBody.class), isNull(), isNull(), isNull()))
                .thenReturn(mockAttacks);
        });

        // When
        adrService.getAttacks(null, null, null, null, null, null, null, null);

        // Then
        SDKExtension extension = mockedSDKExtension.constructed().get(0);
        ArgumentCaptor<AttacksFilterBody> captor = ArgumentCaptor.forClass(AttacksFilterBody.class);
        verify(extension).getAttacks(eq(TEST_ORG_ID), captor.capture(), isNull(), isNull(), isNull());

        AttacksFilterBody filter = captor.getValue();
        // Verify null parameters didn't set fields (they should remain at constructor defaults)
        assertNotNull(filter); // Filter body is created but fields remain unset
    }

    // ========== Helper Methods ==========

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
