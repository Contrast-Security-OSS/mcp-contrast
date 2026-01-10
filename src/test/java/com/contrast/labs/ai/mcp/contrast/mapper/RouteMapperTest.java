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
package com.contrast.labs.ai.mcp.contrast.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.App;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.Observation;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.Route;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.RouteCoverageResponse;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.Server;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RouteMapperTest {

  private RouteMapper mapper;

  @BeforeEach
  void setUp() {
    mapper = new RouteMapper();
  }

  @Test
  void toRouteLight_should_map_all_kept_fields() {
    var observation = new Observation();
    observation.setVerb("GET");
    observation.setUrl("/api/users");

    var route = new Route();
    route.setSignature("org.example.Controller.getUsers()");
    route.setEnvironments(List.of("PRODUCTION"));
    route.setStatus("EXERCISED");
    route.setRouteHash("hash-123");
    route.setVulnerabilities(5);
    route.setCriticalVulnerabilities(2);
    route.setExercised(1704067200000L);
    route.setDiscovered(1704063600000L);
    route.setServersTotal(3);
    route.setObservations(List.of(observation));
    route.setTotalObservations(1L);

    var result = mapper.toRouteLight(route);

    assertThat(result.signature()).isEqualTo("org.example.Controller.getUsers()");
    assertThat(result.environments()).containsExactly("PRODUCTION");
    assertThat(result.status()).isEqualTo("EXERCISED");
    assertThat(result.routeHash()).isEqualTo("hash-123");
    assertThat(result.vulnerabilities()).isEqualTo(5);
    assertThat(result.criticalVulnerabilities()).isEqualTo(2);
    assertThat(result.exercised()).isEqualTo(1704067200000L);
    assertThat(result.discovered()).isEqualTo(1704063600000L);
    assertThat(result.serversTotal()).isEqualTo(3);
    assertThat(result.observations()).hasSize(1);
    assertThat(result.observations().get(0).getVerb()).isEqualTo("GET");
    assertThat(result.totalObservations()).isEqualTo(1L);
  }

  @Test
  void toRouteLight_should_exclude_removed_fields() {
    var app = new App();
    app.setName("TestApp");
    app.setAppId("app-123");

    var server = new Server();
    server.setName("server-1");

    var route = new Route();
    route.setSignature("signature");
    route.setApp(app);
    route.setServers(List.of(server));
    route.setRouteHashString("hash-string-should-be-excluded");

    var result = mapper.toRouteLight(route);

    // RouteLight record doesn't have app, servers, or routeHashString fields
    // If it compiled, those fields were successfully excluded
    assertThat(result.signature()).isEqualTo("signature");
  }

  @Test
  void toRouteLight_should_handle_null_observations() {
    var route = new Route();
    route.setSignature("signature");
    route.setObservations(null);

    var result = mapper.toRouteLight(route);

    assertThat(result.observations()).isEmpty();
  }

  @Test
  void toRouteLight_should_handle_null_environments() {
    var route = new Route();
    route.setSignature("signature");
    route.setEnvironments(null);

    var result = mapper.toRouteLight(route);

    assertThat(result.environments()).isEmpty();
  }

  @Test
  void toResponseLight_should_compute_aggregates() {
    var exercisedRoute = new Route();
    exercisedRoute.setSignature("sig1");
    exercisedRoute.setStatus("EXERCISED");
    exercisedRoute.setVulnerabilities(3);
    exercisedRoute.setCriticalVulnerabilities(1);

    var discoveredRoute = new Route();
    discoveredRoute.setSignature("sig2");
    discoveredRoute.setStatus("DISCOVERED");
    discoveredRoute.setVulnerabilities(2);
    discoveredRoute.setCriticalVulnerabilities(0);

    var response = new RouteCoverageResponse();
    response.setSuccess(true);
    response.setMessages(List.of("OK"));
    response.setRoutes(List.of(exercisedRoute, discoveredRoute));

    var result = mapper.toResponseLight(response);

    assertThat(result.success()).isTrue();
    assertThat(result.messages()).containsExactly("OK");
    assertThat(result.totalRoutes()).isEqualTo(2);
    assertThat(result.exercisedCount()).isEqualTo(1);
    assertThat(result.discoveredCount()).isEqualTo(1);
    assertThat(result.coveragePercent()).isCloseTo(50.0, within(0.01));
    assertThat(result.totalVulnerabilities()).isEqualTo(5);
    assertThat(result.totalCriticalVulnerabilities()).isEqualTo(1);
    assertThat(result.routes()).hasSize(2);
  }

  @Test
  void toResponseLight_should_handle_null_routes() {
    var response = new RouteCoverageResponse();
    response.setSuccess(true);
    response.setRoutes(null);

    var result = mapper.toResponseLight(response);

    assertThat(result.totalRoutes()).isZero();
    assertThat(result.exercisedCount()).isZero();
    assertThat(result.discoveredCount()).isZero();
    assertThat(result.coveragePercent()).isZero();
    assertThat(result.routes()).isEmpty();
  }

  @Test
  void toResponseLight_should_handle_all_exercised() {
    var route1 = new Route();
    route1.setStatus("EXERCISED");
    var route2 = new Route();
    route2.setStatus("EXERCISED");

    var response = new RouteCoverageResponse();
    response.setSuccess(true);
    response.setRoutes(List.of(route1, route2));

    var result = mapper.toResponseLight(response);

    assertThat(result.totalRoutes()).isEqualTo(2);
    assertThat(result.exercisedCount()).isEqualTo(2);
    assertThat(result.discoveredCount()).isZero();
    assertThat(result.coveragePercent()).isCloseTo(100.0, within(0.01));
  }

  @Test
  void toResponseLight_should_match_status_case_insensitively() {
    var route1 = new Route();
    route1.setStatus("exercised"); // lowercase
    var route2 = new Route();
    route2.setStatus("Discovered"); // mixed case

    var response = new RouteCoverageResponse();
    response.setSuccess(true);
    response.setRoutes(List.of(route1, route2));

    var result = mapper.toResponseLight(response);

    assertThat(result.exercisedCount()).isEqualTo(1);
    assertThat(result.discoveredCount()).isEqualTo(1);
  }

  @Test
  void toResponseLight_should_round_coverage_percent_to_two_decimals() {
    // 1 out of 3 exercised = 33.333...%
    var exercised = new Route();
    exercised.setStatus("EXERCISED");
    var discovered1 = new Route();
    discovered1.setStatus("DISCOVERED");
    var discovered2 = new Route();
    discovered2.setStatus("DISCOVERED");

    var response = new RouteCoverageResponse();
    response.setSuccess(true);
    response.setRoutes(List.of(exercised, discovered1, discovered2));

    var result = mapper.toResponseLight(response);

    // Should be rounded to 33.33, not 33.333...
    assertThat(result.coveragePercent()).isEqualTo(33.33);
  }
}
