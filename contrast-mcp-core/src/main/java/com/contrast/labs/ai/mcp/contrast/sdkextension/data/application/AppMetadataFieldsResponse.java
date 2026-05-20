package com.contrast.labs.ai.mcp.contrast.sdkextension.data.application;

import java.util.List;
import lombok.Data;

@Data
public class AppMetadataFieldsResponse {
  private List<AppMetadataField> fields;
}
