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

import org.junit.jupiter.api.Test;

class PaginationParamsTest {

  @Test
  void testValidPaginationDefaults() {
    var params = PaginationParams.of(null, null);

    assertThat(params.page()).isEqualTo(1);
    assertThat(params.pageSize()).isEqualTo(50);
    assertThat(params.offset()).isEqualTo(0);
    assertThat(params.limit()).isEqualTo(50);
    assertThat(params.warnings()).isEmpty();
    assertThat(params.isValid()).isTrue();
  }

  @Test
  void testValidPaginationCustom() {
    var params = PaginationParams.of(3, 25);

    assertThat(params.page()).isEqualTo(3);
    assertThat(params.pageSize()).isEqualTo(25);
    assertThat(params.offset()).isEqualTo(50); // (3-1) * 25
    assertThat(params.limit()).isEqualTo(25);
    assertThat(params.warnings()).isEmpty();
    assertThat(params.isValid()).isTrue();
  }

  @Test
  void testInvalidPageNegative() {
    var params = PaginationParams.of(-5, 50);

    assertThat(params.page()).isEqualTo(1); // Clamped to 1
    assertThat(params.pageSize()).isEqualTo(50);
    assertThat(params.offset()).isEqualTo(0);
    assertThat(params.isValid()).isTrue(); // Always valid with soft failures
    assertThat(params.warnings().size()).isEqualTo(1);
    assertThat(params.warnings().get(0)).contains("Invalid page number -5");
  }

  @Test
  void testInvalidPageZero() {
    var params = PaginationParams.of(0, 50);

    assertThat(params.page()).isEqualTo(1); // Clamped to 1
    assertThat(params.pageSize()).isEqualTo(50);
    assertThat(params.offset()).isEqualTo(0);
    assertThat(params.isValid()).isTrue();
    assertThat(params.warnings().size()).isEqualTo(1);
    assertThat(params.warnings().get(0)).contains("Invalid page number 0");
  }

  @Test
  void testInvalidPageSizeNegative() {
    var params = PaginationParams.of(1, -10);

    assertThat(params.page()).isEqualTo(1);
    assertThat(params.pageSize()).isEqualTo(50); // Clamped to default 50
    assertThat(params.offset()).isEqualTo(0);
    assertThat(params.isValid()).isTrue();
    assertThat(params.warnings().size()).isEqualTo(1);
    assertThat(params.warnings().get(0)).contains("Invalid pageSize -10");
  }

  @Test
  void testInvalidPageSizeZero() {
    var params = PaginationParams.of(1, 0);

    assertThat(params.page()).isEqualTo(1);
    assertThat(params.pageSize()).isEqualTo(50); // Clamped to default 50
    assertThat(params.offset()).isEqualTo(0);
    assertThat(params.isValid()).isTrue();
    assertThat(params.warnings().size()).isEqualTo(1);
    assertThat(params.warnings().get(0)).contains("Invalid pageSize 0");
  }

  @Test
  void testPageSizeExceedsMaximum() {
    var params = PaginationParams.of(1, 200);

    assertThat(params.page()).isEqualTo(1);
    assertThat(params.pageSize()).isEqualTo(100); // Capped to 100
    assertThat(params.offset()).isEqualTo(0);
    assertThat(params.isValid()).isTrue();
    assertThat(params.warnings().size()).isEqualTo(1);
    assertThat(params.warnings().get(0)).contains("Requested pageSize 200 exceeds maximum 100");
  }

  @Test
  void testMultipleValidationWarnings() {
    var params = PaginationParams.of(-5, 200);

    assertThat(params.page()).isEqualTo(1); // Clamped to 1
    assertThat(params.pageSize()).isEqualTo(100); // Capped to 100
    assertThat(params.offset()).isEqualTo(0);
    assertThat(params.isValid()).isTrue();
    assertThat(params.warnings().size()).isEqualTo(2);
    assertThat(params.warnings().get(0)).contains("Invalid page number -5");
    assertThat(params.warnings().get(1)).contains("Requested pageSize 200 exceeds maximum 100");
  }

  @Test
  void testOffsetCalculation() {
    // Page 1
    var p1 = PaginationParams.of(1, 50);
    assertThat(p1.offset()).isEqualTo(0);

    // Page 2
    var p2 = PaginationParams.of(2, 50);
    assertThat(p2.offset()).isEqualTo(50);

    // Page 5 with custom page size
    var p3 = PaginationParams.of(5, 25);
    assertThat(p3.offset()).isEqualTo(100); // (5-1) * 25
  }

  @Test
  void testLimitMatchesPageSize() {
    var params = PaginationParams.of(1, 75);

    assertThat(params.pageSize()).isEqualTo(75);
    assertThat(params.limit()).isEqualTo(75);
  }

  @Test
  void testPageSizeBoundaryValues() {
    // Min valid
    var pMin = PaginationParams.of(1, 1);
    assertThat(pMin.pageSize()).isEqualTo(1);
    assertThat(pMin.warnings()).isEmpty();

    // Max valid
    var pMax = PaginationParams.of(1, 100);
    assertThat(pMax.pageSize()).isEqualTo(100);
    assertThat(pMax.warnings()).isEmpty();

    // Just over max
    var pOver = PaginationParams.of(1, 101);
    assertThat(pOver.pageSize()).isEqualTo(100);
    assertThat(pOver.warnings().size()).isEqualTo(1);
  }

  @Test
  void testWarningsAreImmutable() {
    var params = PaginationParams.of(-1, 200);

    // Should throw UnsupportedOperationException
    assertThatThrownBy(
            () -> {
              params.warnings().add("This should fail");
            })
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void testCustomMaxPageSize() {
    // Custom max of 50
    var params = PaginationParams.of(1, 100, 50);

    assertThat(params.page()).isEqualTo(1);
    assertThat(params.pageSize()).isEqualTo(50); // Capped to custom max 50
    assertThat(params.warnings()).hasSize(1);
    assertThat(params.warnings().get(0)).contains("Requested pageSize 100 exceeds maximum 50");
  }

  @Test
  void testCustomMaxPageSizeWithinLimit() {
    // Custom max of 50, requesting 30 (within limit)
    var params = PaginationParams.of(1, 30, 50);

    assertThat(params.page()).isEqualTo(1);
    assertThat(params.pageSize()).isEqualTo(30);
    assertThat(params.warnings()).isEmpty();
  }

  @Test
  void testCustomMaxPageSizeWithDefaultPageSize() {
    // Custom max of 50, requesting null (should use default 50)
    var params = PaginationParams.of(1, null, 50);

    assertThat(params.page()).isEqualTo(1);
    assertThat(params.pageSize()).isEqualTo(50);
    assertThat(params.warnings()).isEmpty();
  }
}
