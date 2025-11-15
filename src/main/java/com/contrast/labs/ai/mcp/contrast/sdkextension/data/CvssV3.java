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

/** Class representing CVSS v3 scoring details for a CVE. */
@Data
public class CvssV3 {

  private String attackVector;
  private String attackComplexity;
  private String privilegesRequired;
  private String userInteraction;
  private String scope;
  private String confidentialityImpact;
  private String integrityImpact;
  private String availabilityImpact;
  private double impactSubscore;
  private double exploitabilitySubscore;
  private double baseScore;
  private String vector;
  private String severity;
}
