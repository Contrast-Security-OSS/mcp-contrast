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
package com.contrast.labs.ai.mcp.contrast.data;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.List;
import org.junit.jupiter.api.Test;

class RouteCoverageResponseLightTest {

  @Test
  void should_create_response_with_routes_and_aggregates() {
    var route =
        new RouteLight(
            "signature",
            List.of("PRODUCTION"),
            "EXERCISED",
            "hash",
            1,
            0,
            100L,
            50L,
            2,
            List.of(),
            0L);

    var response =
        new RouteCoverageResponseLight(true, List.of(), 10, 7, 3, 70.0, 15, 5, List.of(route));

    assertThat(response.success()).isTrue();
    assertThat(response.messages()).isEmpty();
    assertThat(response.totalRoutes()).isEqualTo(10);
    assertThat(response.exercisedCount()).isEqualTo(7);
    assertThat(response.discoveredCount()).isEqualTo(3);
    assertThat(response.coveragePercent()).isCloseTo(70.0, within(0.01));
    assertThat(response.totalVulnerabilities()).isEqualTo(15);
    assertThat(response.totalCriticalVulnerabilities()).isEqualTo(5);
    assertThat(response.routes()).hasSize(1);
  }

  @Test
  void should_handle_empty_routes_with_zero_aggregates() {
    var response =
        new RouteCoverageResponseLight(
            true, List.of("No routes found"), 0, 0, 0, 0.0, 0, 0, List.of());

    assertThat(response.success()).isTrue();
    assertThat(response.messages()).containsExactly("No routes found");
    assertThat(response.totalRoutes()).isZero();
    assertThat(response.coveragePercent()).isZero();
    assertThat(response.routes()).isEmpty();
  }
}
