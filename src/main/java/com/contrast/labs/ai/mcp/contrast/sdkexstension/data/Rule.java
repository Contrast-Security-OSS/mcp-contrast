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

public class Rule {
  private String name;
  private String type;
  private String description;
  private String development;
  private String qa;
  private String production;
  private int id;
  private String uuid;
  private Boolean can_block_at_perimeter;
  private Boolean is_monitor_at_perimeter;
  private Boolean can_block;
  private String parent_rule_uuid;
  private String parent_rule_name;
  private List<Cve> cves;
  private Boolean enabled_dev;
  private Boolean enabled_qa;
  private Boolean enabled_prod;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getDevelopment() {
    return development;
  }

  public void setDevelopment(String development) {
    this.development = development;
  }

  public String getQa() {
    return qa;
  }

  public void setQa(String qa) {
    this.qa = qa;
  }

  public String getProduction() {
    return production;
  }

  public void setProduction(String production) {
    this.production = production;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public Boolean getCanBlockAtPerimeter() {
    return can_block_at_perimeter;
  }

  public void setCanBlockAtPerimeter(Boolean canBlockAtPerimeter) {
    this.can_block_at_perimeter = canBlockAtPerimeter;
  }

  public Boolean getIsMonitorAtPerimeter() {
    return is_monitor_at_perimeter;
  }

  public void setIsMonitorAtPerimeter(Boolean isMonitorAtPerimeter) {
    this.is_monitor_at_perimeter = isMonitorAtPerimeter;
  }

  public Boolean getCanBlock() {
    return can_block;
  }

  public void setCanBlock(Boolean canBlock) {
    this.can_block = canBlock;
  }

  public String getParentRuleUuid() {
    return parent_rule_uuid;
  }

  public void setParentRuleUuid(String parentRuleUuid) {
    this.parent_rule_uuid = parentRuleUuid;
  }

  public String getParentRuleName() {
    return parent_rule_name;
  }

  public void setParentRuleName(String parentRuleName) {
    this.parent_rule_name = parentRuleName;
  }

  public List<Cve> getCves() {
    return cves;
  }

  public void setCves(List<Cve> cves) {
    this.cves = cves;
  }

  public Boolean getEnabledDev() {
    return enabled_dev;
  }

  public void setEnabledDev(Boolean enabledDev) {
    this.enabled_dev = enabledDev;
  }

  public Boolean getEnabledQa() {
    return enabled_qa;
  }

  public void setEnabledQa(Boolean enabledQa) {
    this.enabled_qa = enabledQa;
  }

  public Boolean getEnabledProd() {
    return enabled_prod;
  }

  public void setEnabledProd(Boolean enabledProd) {
    this.enabled_prod = enabledProd;
  }
}
