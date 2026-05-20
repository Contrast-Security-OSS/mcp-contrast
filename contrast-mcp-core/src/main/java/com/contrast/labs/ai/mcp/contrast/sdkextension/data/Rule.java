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

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@Data
public class Rule {
  private String name;
  private String type;
  private String description;
  private String development;
  private String qa;
  private String production;
  private int id;
  private String uuid;

  @SerializedName("can_block_at_perimeter")
  private Boolean canBlockAtPerimeter;

  @SerializedName("is_monitor_at_perimeter")
  private Boolean isMonitorAtPerimeter;

  @SerializedName("can_block")
  private Boolean canBlock;

  @SerializedName("parent_rule_uuid")
  private String parentRuleUuid;

  @SerializedName("parent_rule_name")
  private String parentRuleName;

  private List<Cve> cves;

  @SerializedName("enabled_dev")
  private Boolean enabledDev;

  @SerializedName("enabled_qa")
  private Boolean enabledQa;

  @SerializedName("enabled_prod")
  private Boolean enabledProd;
}
