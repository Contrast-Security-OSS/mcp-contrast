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

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

/** Represents a route in the route coverage response. */
@Data
public class Route {

  private App app;
  private String signature;
  private List<Server> servers;
  private List<String> environments;
  private int vulnerabilities;
  private long exercised;
  private long discovered;
  private String status;

  private RouteDetailsResponse routeDetailsResponse;

  @SerializedName("route_hash")
  private String routeHash;

  @SerializedName("route_hash_string")
  private String routeHashString;

  @SerializedName("servers_total")
  private int serversTotal;

  @SerializedName("critical_vulnerabilities")
  private int criticalVulnerabilities;

  @SerializedName("observations")
  private List<Observation> observations;

  @SerializedName("total_observations")
  private Long totalObservations;
}
