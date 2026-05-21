package com.contrast.labs.ai.mcp.contrast.sdkextension.data.application;

import lombok.Data;

@Data
public class AppMetadataField {
  private long fieldId;
  private String displayLabel;
  private String agentLabel;
  private String fieldType;
}
