package com.contrast.labs.ai.mcp.contrast.sdkextension.data.application;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

/** Represents the response from the applications endpoint. */
@Data
public class ApplicationsResponse {

  @SerializedName("success")
  private boolean success;

  @SerializedName("messages")
  private List<String> messages;

  @SerializedName("applications")
  private List<Application> applications;
}
