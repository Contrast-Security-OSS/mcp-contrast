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
package com.contrast.labs.ai.mcp.contrast.sdkextension.data;

/** Class representing the impact statistics for a CVE. */
public class ImpactStats {

  private int impactedAppCount;
  private int totalAppCount;
  private int impactedServerCount;
  private int totalServerCount;
  private double appPercentage;
  private double serverPercentage;

  public int getImpactedAppCount() {
    return impactedAppCount;
  }

  public void setImpactedAppCount(int impactedAppCount) {
    this.impactedAppCount = impactedAppCount;
  }

  public int getTotalAppCount() {
    return totalAppCount;
  }

  public void setTotalAppCount(int totalAppCount) {
    this.totalAppCount = totalAppCount;
  }

  public int getImpactedServerCount() {
    return impactedServerCount;
  }

  public void setImpactedServerCount(int impactedServerCount) {
    this.impactedServerCount = impactedServerCount;
  }

  public int getTotalServerCount() {
    return totalServerCount;
  }

  public void setTotalServerCount(int totalServerCount) {
    this.totalServerCount = totalServerCount;
  }

  public double getAppPercentage() {
    return appPercentage;
  }

  public void setAppPercentage(double appPercentage) {
    this.appPercentage = appPercentage;
  }

  public double getServerPercentage() {
    return serverPercentage;
  }

  public void setServerPercentage(double serverPercentage) {
    this.serverPercentage = serverPercentage;
  }
}
