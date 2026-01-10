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

import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.Observation;
import java.util.List;

/**
 * Lightweight route record for route coverage responses. Contains essential route information
 * without redundant application data or full server details.
 *
 * @param signature Code-level route identifier (e.g., Java method signature)
 * @param environments Distinct environments where route has been observed (DEVELOPMENT, QA,
 *     PRODUCTION)
 * @param status Route status (DISCOVERED, EXERCISED)
 * @param routeHash Unique route identifier hash
 * @param vulnerabilities Total vulnerability count for this route
 * @param criticalVulnerabilities Count of vulnerabilities with critical severity
 * @param exercised Timestamp when route was last exercised (0 if never)
 * @param discovered Timestamp when route was first discovered (immutable)
 * @param serversTotal Count of distinct enabled servers where route has been observed
 * @param observations List of observed HTTP interactions (verb + url)
 * @param totalObservations Total observation count
 */
public record RouteLight(
    String signature,
    List<String> environments,
    String status,
    String routeHash,
    int vulnerabilities,
    int criticalVulnerabilities,
    long exercised,
    long discovered,
    int serversTotal,
    List<Observation> observations,
    Long totalObservations) {}
