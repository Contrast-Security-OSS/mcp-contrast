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

/** Class representing CVE vulnerability information. */
public class Cve {
  private String availabilityImpact;
  private String name;
  private String uuid;
  private String description;
  private String status;
  private String accessVector;
  private String accessComplexity;
  private String authentication;
  private String confidentialityImpact;
  private String integrityImpact;
  private double score;
  private List<String> references;

  public String getAvailabilityImpact() {
    return availabilityImpact;
  }

  public void setAvailabilityImpact(String availabilityImpact) {
    this.availabilityImpact = availabilityImpact;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getAccessVector() {
    return accessVector;
  }

  public void setAccessVector(String accessVector) {
    this.accessVector = accessVector;
  }

  public String getAccessComplexity() {
    return accessComplexity;
  }

  public void setAccessComplexity(String accessComplexity) {
    this.accessComplexity = accessComplexity;
  }

  public String getAuthentication() {
    return authentication;
  }

  public void setAuthentication(String authentication) {
    this.authentication = authentication;
  }

  public String getConfidentialityImpact() {
    return confidentialityImpact;
  }

  public void setConfidentialityImpact(String confidentialityImpact) {
    this.confidentialityImpact = confidentialityImpact;
  }

  public String getIntegrityImpact() {
    return integrityImpact;
  }

  public void setIntegrityImpact(String integrityImpact) {
    this.integrityImpact = integrityImpact;
  }

  public double getScore() {
    return score;
  }

  public void setScore(double score) {
    this.score = score;
  }

  public List<String> getReferences() {
    return references;
  }

  public void setReferences(List<String> references) {
    this.references = references;
  }
}
