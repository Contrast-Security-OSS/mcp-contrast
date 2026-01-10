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
package com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class RouteTest {

  private final Gson gson = new Gson();

  @Test
  void should_deserialize_observations_from_json() {
    var json =
        """
        {
          "route_hash": "abc123",
          "signature": "GET /api/users",
          "observations": [
            {"verb": "GET", "url": "/api/users"},
            {"verb": "POST", "url": "/api/users"}
          ],
          "total_observations": 2
        }
        """;

    var route = gson.fromJson(json, Route.class);

    assertThat(route.getRouteHash()).isEqualTo("abc123");
    assertThat(route.getObservations()).hasSize(2);
    assertThat(route.getObservations().get(0).getVerb()).isEqualTo("GET");
    assertThat(route.getObservations().get(1).getVerb()).isEqualTo("POST");
    assertThat(route.getTotalObservations()).isEqualTo(2L);
  }

  @Test
  void should_handle_null_observations() {
    var json =
        """
        {
          "route_hash": "abc123",
          "signature": "GET /api/users"
        }
        """;

    var route = gson.fromJson(json, Route.class);

    assertThat(route.getObservations()).isNull();
    assertThat(route.getTotalObservations()).isNull();
  }
}
