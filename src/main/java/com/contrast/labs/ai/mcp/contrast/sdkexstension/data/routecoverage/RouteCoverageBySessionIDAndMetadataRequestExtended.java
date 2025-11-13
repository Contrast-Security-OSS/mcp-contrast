package com.contrast.labs.ai.mcp.contrast.sdkexstension.data.routecoverage;

import com.contrastsecurity.models.RouteCoverageBySessionIDAndMetadataRequest;

public class RouteCoverageBySessionIDAndMetadataRequestExtended
    extends RouteCoverageBySessionIDAndMetadataRequest {

  public void setSessionId(String sessionId) {
    this.sessionID = sessionId;
  }
}
