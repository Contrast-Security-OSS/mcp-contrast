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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PaginationParamsTest {

    @Test
    void testValidPaginationDefaults() {
        PaginationParams params = PaginationParams.of(null, null);

        assertEquals(1, params.page());
        assertEquals(50, params.pageSize());
        assertEquals(0, params.offset());
        assertEquals(50, params.limit());
        assertTrue(params.warnings().isEmpty());
        assertTrue(params.isValid());
    }

    @Test
    void testValidPaginationCustom() {
        PaginationParams params = PaginationParams.of(3, 25);

        assertEquals(3, params.page());
        assertEquals(25, params.pageSize());
        assertEquals(50, params.offset());  // (3-1) * 25
        assertEquals(25, params.limit());
        assertTrue(params.warnings().isEmpty());
        assertTrue(params.isValid());
    }

    @Test
    void testInvalidPageNegative() {
        PaginationParams params = PaginationParams.of(-5, 50);

        assertEquals(1, params.page());  // Clamped to 1
        assertEquals(50, params.pageSize());
        assertEquals(0, params.offset());
        assertTrue(params.isValid());  // Always valid with soft failures
        assertEquals(1, params.warnings().size());
        assertTrue(params.warnings().get(0).contains("Invalid page number -5"));
    }

    @Test
    void testInvalidPageZero() {
        PaginationParams params = PaginationParams.of(0, 50);

        assertEquals(1, params.page());  // Clamped to 1
        assertEquals(50, params.pageSize());
        assertEquals(0, params.offset());
        assertTrue(params.isValid());
        assertEquals(1, params.warnings().size());
        assertTrue(params.warnings().get(0).contains("Invalid page number 0"));
    }

    @Test
    void testInvalidPageSizeNegative() {
        PaginationParams params = PaginationParams.of(1, -10);

        assertEquals(1, params.page());
        assertEquals(50, params.pageSize());  // Clamped to default 50
        assertEquals(0, params.offset());
        assertTrue(params.isValid());
        assertEquals(1, params.warnings().size());
        assertTrue(params.warnings().get(0).contains("Invalid pageSize -10"));
    }

    @Test
    void testInvalidPageSizeZero() {
        PaginationParams params = PaginationParams.of(1, 0);

        assertEquals(1, params.page());
        assertEquals(50, params.pageSize());  // Clamped to default 50
        assertEquals(0, params.offset());
        assertTrue(params.isValid());
        assertEquals(1, params.warnings().size());
        assertTrue(params.warnings().get(0).contains("Invalid pageSize 0"));
    }

    @Test
    void testPageSizeExceedsMaximum() {
        PaginationParams params = PaginationParams.of(1, 200);

        assertEquals(1, params.page());
        assertEquals(100, params.pageSize());  // Capped to 100
        assertEquals(0, params.offset());
        assertTrue(params.isValid());
        assertEquals(1, params.warnings().size());
        assertTrue(params.warnings().get(0).contains("Requested pageSize 200 exceeds maximum 100"));
    }

    @Test
    void testMultipleValidationWarnings() {
        PaginationParams params = PaginationParams.of(-5, 200);

        assertEquals(1, params.page());      // Clamped to 1
        assertEquals(100, params.pageSize()); // Capped to 100
        assertEquals(0, params.offset());
        assertTrue(params.isValid());
        assertEquals(2, params.warnings().size());
        assertTrue(params.warnings().get(0).contains("Invalid page number -5"));
        assertTrue(params.warnings().get(1).contains("Requested pageSize 200 exceeds maximum 100"));
    }

    @Test
    void testOffsetCalculation() {
        // Page 1
        PaginationParams p1 = PaginationParams.of(1, 50);
        assertEquals(0, p1.offset());

        // Page 2
        PaginationParams p2 = PaginationParams.of(2, 50);
        assertEquals(50, p2.offset());

        // Page 5 with custom page size
        PaginationParams p3 = PaginationParams.of(5, 25);
        assertEquals(100, p3.offset());  // (5-1) * 25
    }

    @Test
    void testLimitMatchesPageSize() {
        PaginationParams params = PaginationParams.of(1, 75);

        assertEquals(75, params.pageSize());
        assertEquals(75, params.limit());
    }

    @Test
    void testPageSizeBoundaryValues() {
        // Min valid
        PaginationParams pMin = PaginationParams.of(1, 1);
        assertEquals(1, pMin.pageSize());
        assertTrue(pMin.warnings().isEmpty());

        // Max valid
        PaginationParams pMax = PaginationParams.of(1, 100);
        assertEquals(100, pMax.pageSize());
        assertTrue(pMax.warnings().isEmpty());

        // Just over max
        PaginationParams pOver = PaginationParams.of(1, 101);
        assertEquals(100, pOver.pageSize());
        assertEquals(1, pOver.warnings().size());
    }

    @Test
    void testWarningsAreImmutable() {
        PaginationParams params = PaginationParams.of(-1, 200);

        // Should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> {
            params.warnings().add("This should fail");
        });
    }
}
