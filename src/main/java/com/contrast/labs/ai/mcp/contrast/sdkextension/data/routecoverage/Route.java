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

/** Represents a route in the route coverage response. */
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

  // Getters and setters
  public App getApp() {
    return app;
  }

  public void setApp(App app) {
    this.app = app;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public List<Server> getServers() {
    return servers;
  }

  public void setServers(List<Server> servers) {
    this.servers = servers;
  }

  public List<String> getEnvironments() {
    return environments;
  }

  public void setEnvironments(List<String> environments) {
    this.environments = environments;
  }

  public int getVulnerabilities() {
    return vulnerabilities;
  }

  public void setVulnerabilities(int vulnerabilities) {
    this.vulnerabilities = vulnerabilities;
  }

  public long getExercised() {
    return exercised;
  }

  public void setExercised(long exercised) {
    this.exercised = exercised;
  }

  public long getDiscovered() {
    return discovered;
  }

  public void setDiscovered(long discovered) {
    this.discovered = discovered;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getRouteHash() {
    return routeHash;
  }

  public void setRouteHash(String routeHash) {
    this.routeHash = routeHash;
  }

  public String getRouteHashString() {
    return routeHashString;
  }

  public void setRouteHashString(String routeHashString) {
    this.routeHashString = routeHashString;
  }

  public int getServersTotal() {
    return serversTotal;
  }

  public void setServersTotal(int serversTotal) {
    this.serversTotal = serversTotal;
  }

  public int getCriticalVulnerabilities() {
    return criticalVulnerabilities;
  }

  public void setCriticalVulnerabilities(int criticalVulnerabilities) {
    this.criticalVulnerabilities = criticalVulnerabilities;
  }

  public RouteDetailsResponse getRouteDetailsResponse() {
    return routeDetailsResponse;
  }

  public void setRouteDetailsResponse(RouteDetailsResponse routeDetailsResponse) {
    this.routeDetailsResponse = routeDetailsResponse;
  }

  @Override
  public String toString() {
    return "Route{"
        + "app="
        + app
        + ", signature='"
        + signature
        + '\''
        + ", servers="
        + servers
        + ", environments="
        + environments
        + ", vulnerabilities="
        + vulnerabilities
        + ", exercised="
        + exercised
        + ", discovered="
        + discovered
        + ", status='"
        + status
        + '\''
        + ", routeHash='"
        + routeHash
        + '\''
        + ", routeHashString='"
        + routeHashString
        + '\''
        + ", serversTotal="
        + serversTotal
        + ", criticalVulnerabilities="
        + criticalVulnerabilities
        + '}';
  }
}
