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

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Data;

/** Class representing CVE vulnerability information. */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Cve {
  private Long id;
  private String name;
  private String description;
  private String status;
  private String cwe;
  private Double epssScore;
  private Double epssPercentile;
  private Boolean cisa;
  private String cvssScoreSource;
  private Long nvdPublished;
  private Long nvdModified;
  private Long firstSeen;

  // Legacy flat CVSS fields retained for the get_protect_rules contract.
  private String uuid;
  private String accessVector;
  private String accessComplexity;
  private String authentication;
  private String confidentialityImpact;
  private String integrityImpact;
  private String availabilityImpact;
  private List<String> references;

  private CvssV2 cvssv2;
  private CvssV3 cvssv3;

  // Preferred summary derived from the nested CVSS data.
  private Double score;
  private String severity;
}
