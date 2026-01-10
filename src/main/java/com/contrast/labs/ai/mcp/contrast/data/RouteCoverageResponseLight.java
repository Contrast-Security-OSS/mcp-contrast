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

import java.util.List;

/**
 * Lightweight route coverage response containing essential route data and aggregate statistics.
 * Uses RouteLight instead of full Route objects for reduced payload size.
 *
 * @param success Whether the API request succeeded
 * @param messages Any API messages (warnings, info)
 * @param totalRoutes Total number of routes in the response
 * @param exercisedCount Number of routes with EXERCISED status
 * @param discoveredCount Number of routes with DISCOVERED status (not yet exercised)
 * @param coveragePercent Percentage of routes that are exercised (0.0 to 100.0)
 * @param totalVulnerabilities Sum of vulnerabilities across all routes
 * @param totalCriticalVulnerabilities Sum of critical vulnerabilities across all routes
 * @param routes List of lightweight route objects
 */
public record RouteCoverageResponseLight(
    boolean success,
    List<String> messages,
    int totalRoutes,
    int exercisedCount,
    int discoveredCount,
    double coveragePercent,
    int totalVulnerabilities,
    int totalCriticalVulnerabilities,
    List<RouteLight> routes) {}
