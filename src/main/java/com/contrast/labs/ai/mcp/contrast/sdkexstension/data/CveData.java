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
package com.contrast.labs.ai.mcp.contrast.sdkexstension.data;

import java.util.List;

/** Root class for deserializing CVE data from JSON. */
public class CveData {

  private Cve cve;
  private ImpactStats impactStats;
  private List<Library> libraries;
  private List<App> apps;
  private List<Server> servers;

  public Cve getCve() {
    return cve;
  }

  public void setCve(Cve cve) {
    this.cve = cve;
  }

  public ImpactStats getImpactStats() {
    return impactStats;
  }

  public void setImpactStats(ImpactStats impactStats) {
    this.impactStats = impactStats;
  }

  public List<Library> getLibraries() {
    return libraries;
  }

  public void setLibraries(List<Library> libraries) {
    this.libraries = libraries;
  }

  public List<App> getApps() {
    return apps;
  }

  public void setApps(List<App> apps) {
    this.apps = apps;
  }

  public List<Server> getServers() {
    return servers;
  }

  public void setServers(List<Server> servers) {
    this.servers = servers;
  }
}
