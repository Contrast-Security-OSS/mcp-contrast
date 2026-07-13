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

import lombok.Data;

/** Class representing CVSS v2 scoring details for a CVE. */
@Data
public class CvssV2 {

  private String accessVector;
  private String accessComplexity;
  private String authentication;
  private String confidentialityImpact;
  private String integrityImpact;
  private String availabilityImpact;
  private String exploitability;
  private String remediationLevel;
  private String reportConfidence;
  private String collateralDamagePotential;
  private String targetDistribution;
  private String confidentialityRequirement;
  private String integrityRequirement;
  private String availabilityRequirement;
  private String severity;
}
