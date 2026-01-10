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

import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.Observation;
import java.util.List;
import org.junit.jupiter.api.Test;

class RouteLightTest {

  @Test
  void should_create_route_light_with_all_fields() {
    var observation = new Observation();
    observation.setVerb("GET");
    observation.setUrl("/api/users");

    var route =
        new RouteLight(
            "org.example.Controller.getUsers()",
            List.of("PRODUCTION", "QA"),
            "EXERCISED",
            "hash-123",
            5,
            2,
            1704067200000L,
            1704063600000L,
            3,
            List.of(observation),
            1L);

    assertThat(route.signature()).isEqualTo("org.example.Controller.getUsers()");
    assertThat(route.environments()).containsExactly("PRODUCTION", "QA");
    assertThat(route.status()).isEqualTo("EXERCISED");
    assertThat(route.routeHash()).isEqualTo("hash-123");
    assertThat(route.vulnerabilities()).isEqualTo(5);
    assertThat(route.criticalVulnerabilities()).isEqualTo(2);
    assertThat(route.exercised()).isEqualTo(1704067200000L);
    assertThat(route.discovered()).isEqualTo(1704063600000L);
    assertThat(route.serversTotal()).isEqualTo(3);
    assertThat(route.observations()).hasSize(1);
    assertThat(route.totalObservations()).isEqualTo(1L);
  }

  @Test
  void should_handle_null_optional_fields() {
    var route =
        new RouteLight("signature", List.of(), "DISCOVERED", "hash", 0, 0, 0L, 0L, 0, null, null);

    assertThat(route.observations()).isNull();
    assertThat(route.totalObservations()).isNull();
  }
}
