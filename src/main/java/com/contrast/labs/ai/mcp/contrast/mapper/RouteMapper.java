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

import com.contrast.labs.ai.mcp.contrast.data.RouteCoverageResponseLight;
import com.contrast.labs.ai.mcp.contrast.data.RouteLight;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.Route;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.RouteCoverageResponse;
import java.util.Collections;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Mapper for transforming Route objects into lightweight RouteLight representations. Eliminates
 * redundant fields (app, servers, routeDetailsResponse, routeHashString) while preserving essential
 * route coverage data.
 */
@Component
public class RouteMapper {

  /**
   * Transform a Route object into a lightweight representation. Removes redundant app data, full
   * server list, and legacy fields.
   *
   * @param route The full route object from SDK
   * @return RouteLight with essential fields only
   */
  public RouteLight toRouteLight(Route route) {
    return new RouteLight(
        route.getSignature(),
        Optional.ofNullable(route.getEnvironments()).orElse(Collections.emptyList()),
        route.getStatus(),
        route.getRouteHash(),
        route.getVulnerabilities(),
        route.getCriticalVulnerabilities(),
        route.getExercised(),
        route.getDiscovered(),
        route.getServersTotal(),
        Optional.ofNullable(route.getObservations()).orElse(Collections.emptyList()),
        route.getTotalObservations());
  }

  /**
   * Transform full RouteCoverageResponse to lightweight version with aggregate statistics.
   *
   * @param response Full route coverage response from SDK
   * @return Lightweight response with RouteLight objects and computed aggregates
   */
  public RouteCoverageResponseLight toResponseLight(RouteCoverageResponse response) {
    var routes = Optional.ofNullable(response.getRoutes()).orElse(Collections.emptyList());

    var lightRoutes = routes.stream().map(this::toRouteLight).toList();

    int totalRoutes = routes.size();
    int exercisedCount =
        (int) routes.stream().filter(r -> "EXERCISED".equals(r.getStatus())).count();
    int discoveredCount = totalRoutes - exercisedCount;
    double coveragePercent = totalRoutes > 0 ? (exercisedCount * 100.0) / totalRoutes : 0.0;
    int totalVulnerabilities = routes.stream().mapToInt(Route::getVulnerabilities).sum();
    int totalCriticalVulnerabilities =
        routes.stream().mapToInt(Route::getCriticalVulnerabilities).sum();

    return new RouteCoverageResponseLight(
        response.isSuccess(),
        response.getMessages(),
        totalRoutes,
        exercisedCount,
        discoveredCount,
        coveragePercent,
        totalVulnerabilities,
        totalCriticalVulnerabilities,
        lightRoutes);
  }
}
