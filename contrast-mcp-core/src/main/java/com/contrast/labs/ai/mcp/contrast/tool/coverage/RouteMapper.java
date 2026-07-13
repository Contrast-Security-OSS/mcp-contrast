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
package com.contrast.labs.ai.mcp.contrast.tool.coverage;

import com.contrast.labs.ai.mcp.contrast.result.RouteCoverageResponseLight;
import com.contrast.labs.ai.mcp.contrast.result.RouteLight;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.Route;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.RouteCoverageResponse;
import java.util.Collections;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Mapper for transforming Route objects into lightweight RouteLight representations. Eliminates
 * redundant fields (app, servers, routeHashString) while preserving essential route coverage data.
 */
@Component
public class RouteMapper {

  private static final String STATUS_DISCOVERED = "DISCOVERED";
  private static final String STATUS_EXERCISED = "EXERCISED";

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
        statusFor(route),
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
   * Transform full RouteCoverageResponse to lightweight version with aggregate statistics. Uses
   * single-pass computation for efficiency with large route lists.
   *
   * @param response Full route coverage response from SDK
   * @return Lightweight response with RouteLight objects and computed aggregates
   */
  public RouteCoverageResponseLight toResponseLight(RouteCoverageResponse response) {
    var routes = Optional.ofNullable(response.getRoutes()).orElse(Collections.emptyList());

    var lightRoutes = routes.stream().map(this::toRouteLight).toList();

    // Single-pass computation for all aggregates
    int exercisedCount = 0;
    int discoveredCount = 0;
    int totalVulnerabilities = 0;
    int totalCriticalVulnerabilities = 0;

    for (var route : routes) {
      var status = statusFor(route);
      if (STATUS_EXERCISED.equalsIgnoreCase(status)) {
        exercisedCount++;
      } else if (STATUS_DISCOVERED.equalsIgnoreCase(status)) {
        discoveredCount++;
      }
      totalVulnerabilities += route.getVulnerabilities();
      totalCriticalVulnerabilities += route.getCriticalVulnerabilities();
    }

    // Filtered responses omit route status, so prefer their authoritative aggregate counts.
    int totalRoutes = Optional.ofNullable(response.getCount()).orElse(routes.size());
    exercisedCount = Optional.ofNullable(response.getExercisedCount()).orElse(exercisedCount);
    discoveredCount = Optional.ofNullable(response.getDiscoveredCount()).orElse(discoveredCount);
    double coveragePercent =
        totalRoutes > 0 ? Math.round((exercisedCount * 100.0) / totalRoutes * 100.0) / 100.0 : 0.0;

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

  private static String statusFor(Route route) {
    if (StringUtils.hasText(route.getStatus())) {
      return route.getStatus();
    }
    return route.getExercised() > 0 ? STATUS_EXERCISED : STATUS_DISCOVERED;
  }
}
