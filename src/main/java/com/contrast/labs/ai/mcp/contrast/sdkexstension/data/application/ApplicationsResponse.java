package com.contrast.labs.ai.mcp.contrast.sdkexstension.data.application;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Represents the response from the applications endpoint. */
public class ApplicationsResponse {

  @SerializedName("success")
  private boolean success;

  @SerializedName("messages")
  private List<String> messages;

  @SerializedName("applications")
  private List<Application> applications;

  // Getters and setters
  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public List<String> getMessages() {
    return messages;
  }

  public void setMessages(List<String> messages) {
    this.messages = messages;
  }

  public List<Application> getApplications() {
    return applications;
  }

  public void setApplications(List<Application> applications) {
    this.applications = applications;
  }

  @Override
  public String toString() {
    return "ApplicationsResponse{"
        + "success="
        + success
        + ", messages="
        + messages
        + ", applications="
        + applications
        + '}';
  }
}
