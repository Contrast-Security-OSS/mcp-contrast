/*
 * Copyright 2025 Contrast Security
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.contrast.labs.ai.mcp.contrast.sdkextension;

import com.contrastsecurity.models.TraceFilterBody;
import com.contrastsecurity.models.TraceMetadataFilter;
import java.util.List;

/**
 * Extended TraceFilterBody that provides helper methods for building filters with session
 * parameters (agentSessionId and resolved metadata field ID).
 *
 * <p>This class exists because TeamServer requires numeric field IDs for metadata filtering, but
 * users provide human-readable field names. The resolution happens in the tool layer, and this
 * class accepts the already-resolved numeric ID.
 */
public class ExtendedTraceFilterBody extends TraceFilterBody {

  /**
   * Creates an ExtendedTraceFilterBody from a base TraceFilterBody, adding session-related
   * parameters.
   *
   * @param source Base filter body with standard filters (severities, environments, etc.)
   * @param agentSessionId Agent session ID for latest session filtering (nullable)
   * @param resolvedFieldId Resolved numeric field ID for metadata filtering (nullable)
   * @param metadataValue Metadata filter value (nullable, requires resolvedFieldId)
   * @return Extended filter body with all parameters set
   */
  public static ExtendedTraceFilterBody fromWithSession(
      TraceFilterBody source, String agentSessionId, String resolvedFieldId, String metadataValue) {

    var extended = new ExtendedTraceFilterBody();

    // Copy base filters from source
    if (source.getSeverities() != null) {
      extended.setSeverities(source.getSeverities());
    }
    if (source.getVulnTypes() != null) {
      extended.setVulnTypes(source.getVulnTypes());
    }
    if (source.getEnvironments() != null) {
      extended.setEnvironments(source.getEnvironments());
    }
    if (source.getStartDate() != null) {
      extended.setStartDate(source.getStartDate());
    }
    if (source.getEndDate() != null) {
      extended.setEndDate(source.getEndDate());
    }
    if (source.getFilterTags() != null) {
      extended.setFilterTags(source.getFilterTags());
    }
    extended.setTracked(source.isTracked());
    extended.setUntracked(source.isUntracked());

    // Add session parameters
    if (agentSessionId != null) {
      extended.setAgentSessionId(agentSessionId);
    }

    if (resolvedFieldId != null && !resolvedFieldId.isBlank()) {
      var filter =
          metadataValue != null
              ? new TraceMetadataFilter(resolvedFieldId, metadataValue)
              : new TraceMetadataFilter(resolvedFieldId, List.of());
      extended.setMetadataFilters(List.of(filter));
    }

    return extended;
  }
}
