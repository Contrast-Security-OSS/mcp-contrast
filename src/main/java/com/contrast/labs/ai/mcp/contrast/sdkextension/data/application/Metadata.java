package com.contrast.labs.ai.mcp.contrast.sdkextension.data.application;

import com.google.gson.annotations.SerializedName;
import lombok.Data;

/** Represents metadata information for an application. */
@Data
public class Metadata {

  @SerializedName("id")
  private String id;

  @SerializedName("fieldName")
  private String name;

  @SerializedName("fieldValue")
  private String value;

  @SerializedName("type")
  private String type;
}
