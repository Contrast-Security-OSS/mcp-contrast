package com.contrast.labs.ai.mcp.contrast.sdkextension.data.application;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppMetadataFilter {
  private long fieldID;
  private String[] values;
}
