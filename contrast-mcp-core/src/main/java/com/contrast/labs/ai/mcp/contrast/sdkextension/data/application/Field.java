/*
 * Copyright 2026 Contrast Security
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

package com.contrast.labs.ai.mcp.contrast.sdkextension.data.application;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

@Data
public class Field {

  /** Represents Field information for an application. */

  /**
   * "validationErrorFields" : [ { "fieldId" : 29, "fieldType" : "STRING", "displayLabel" : "Custom
   * Name", "agentLabel" : "customName", "required" : false, "unique" : false, "subfields" : null,
   * "links" : [ ] }, { ... } ]
   */
  @SerializedName("fieldId")
  private String fieldId;

  @SerializedName("fieldType")
  private String fieldType;

  @SerializedName("displayLabel")
  private String displayLabel;

  @SerializedName("agentLabel")
  private String agentLabel;

  @SerializedName("required")
  private boolean required;

  @SerializedName("unique")
  private boolean unique;

  @SerializedName("subfields")
  private List<Field> subfields;

  @SerializedName("links")
  private List<String> links;
}
