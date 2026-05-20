package com.contrast.labs.ai.mcp.contrast.sdkextension.data.application;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApplicationsFilterRequest {
  private String filterText;
  private String[] filterTags;
  private List<AppMetadataFilter> metadataFilters;
}
