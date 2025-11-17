package com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage;

import com.contrastsecurity.models.RouteCoverageBySessionIDAndMetadataRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class RouteCoverageBySessionIDAndMetadataRequestExtended
    extends RouteCoverageBySessionIDAndMetadataRequest {

  public void setSessionId(String sessionId) {
    this.sessionID = sessionId;
  }
}
