package com.contrast.labs.ai.mcp.contrast.sdkexstension.data.application;

import com.google.gson.annotations.SerializedName;

/** Represents metadata information for an application. */
public class Metadata {

  @SerializedName("id")
  private String id;

  @SerializedName("fieldName")
  private String name;

  @SerializedName("fieldValue")
  private String value;

  @SerializedName("type")
  private String type;

  // Getters and setters
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return "Metadata{"
        + "id='"
        + id
        + '\''
        + ", name='"
        + name
        + '\''
        + ", value='"
        + value
        + '\''
        + ", type='"
        + type
        + '\''
        + '}';
  }
}
