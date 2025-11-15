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
