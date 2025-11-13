package com.contrast.labs.ai.mcp.contrast.sdkexstension.data.application;

import com.google.gson.annotations.SerializedName;
import java.util.List;

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

  // Getters and setters
  public String getFieldId() {
    return fieldId;
  }

  public void setFieldId(String fieldId) {
    this.fieldId = fieldId;
  }

  public String getFieldType() {
    return fieldType;
  }

  public void setFieldType(String fieldType) {
    this.fieldType = fieldType;
  }

  public String getDisplayLabel() {
    return displayLabel;
  }

  public void setDisplayLabel(String displayLabel) {
    this.displayLabel = displayLabel;
  }

  public String getAgentLabel() {
    return agentLabel;
  }

  public void setAgentLabel(String agentLabel) {
    this.agentLabel = agentLabel;
  }

  public boolean isRequired() {
    return required;
  }

  public void setRequired(boolean required) {
    this.required = required;
  }

  public boolean isUnique() {
    return unique;
  }

  public void setUnique(boolean unique) {
    this.unique = unique;
  }

  public List<Field> getSubfields() {
    return subfields;
  }

  public void setSubfields(List<Field> subfields) {
    this.subfields = subfields;
  }

  public List<String> getLinks() {
    return links;
  }

  public void setLinks(List<String> links) {
    this.links = links;
  }

  @Override
  public String toString() {
    return "Field{"
        + "fieldId='"
        + fieldId
        + '\''
        + ", fieldType='"
        + fieldType
        + '\''
        + ", displayLabel='"
        + displayLabel
        + '\''
        + ", agentLabel='"
        + agentLabel
        + '\''
        + ", required="
        + required
        + '\''
        + ", unique="
        + unique
        + '\''
        + ", subfields="
        + subfields
        + '\''
        + ", links="
        + links
        + '}';
  }
}
