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
package com.contrast.labs.ai.mcp.contrast.tool.application;

import com.contrast.labs.ai.mcp.contrast.result.ApplicationData;
import com.contrast.labs.ai.mcp.contrast.result.Metadata;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.AppMetadataFilter;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Application;
import com.contrast.labs.ai.mcp.contrast.tool.application.params.ApplicationFilterParams;
import com.contrast.labs.ai.mcp.contrast.tool.base.ExecutionResult;
import com.contrast.labs.ai.mcp.contrast.tool.base.FilterHelper;
import com.contrast.labs.ai.mcp.contrast.tool.base.PaginatedTool;
import com.contrast.labs.ai.mcp.contrast.tool.base.PaginatedToolResponse;
import com.contrast.labs.ai.mcp.contrast.tool.base.PaginationParams;
import com.contrast.labs.ai.mcp.contrast.tool.validation.UnresolvedMetadataFilter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tool for searching applications in an organization. Uses server-side filtering via the POST
 * /applications/filter endpoint for optimal performance.
 */
@Slf4j
@Service
public class SearchApplicationsTool
    extends PaginatedTool<ApplicationFilterParams, ApplicationData> {

  @Tool(
      name = "search_applications",
      description =
          """
          Search applications with optional filters. Returns all applications if no filters specified.

          Filtering behavior:
          - name: Server-side text search on displayName, contextPath, tags, and metadata values
          - tag: Exact, case-insensitive matching
          - metadataFilters: JSON object for metadata field filtering
            - Format: {"fieldName":"value"} or {"fieldName":["value1","value2"]}
            - Multiple fields use AND logic, multiple values use OR logic
            - Field names are case-insensitive
            - Values are case-insensitive

          Related tools:
          - get_session_metadata: Get session metadata for an application
          - search_vulnerabilities: Search vulnerabilities across applications
          """)
  public PaginatedToolResponse<ApplicationData> searchApplications(
      @ToolParam(description = "Page number (1-based), default: 1", required = false) Integer page,
      @ToolParam(description = "Items per page (max 100), default: 50", required = false)
          Integer pageSize,
      @ToolParam(
              description = "Text search filter (searches name, contextPath, tags, metadata)",
              required = false)
          String name,
      @ToolParam(description = "Tag filter (exact match, case-insensitive)", required = false)
          String tag,
      @ToolParam(
              description =
                  "JSON object for metadata filters. Format: {\"field\":\"value\"} or"
                      + " {\"field\":[\"v1\",\"v2\"]}",
              required = false)
          String metadataFilters) {

    return executePipeline(
        page, pageSize, () -> ApplicationFilterParams.of(name, tag, metadataFilters));
  }

  @Override
  protected ExecutionResult<ApplicationData> doExecute(
      PaginationParams pagination, ApplicationFilterParams params, List<String> warnings)
      throws Exception {

    var sdk = getContrastSDK();
    var orgId = getOrgId();
    var sdkExtension = new SDKExtension(sdk);

    // Resolve metadata field names to IDs if metadata filters provided
    List<AppMetadataFilter> resolvedMetadataFilters = null;
    if (params.getMetadataFilters() != null && !params.getMetadataFilters().isEmpty()) {
      resolvedMetadataFilters =
          resolveAppMetadataFilters(sdkExtension, orgId, params.getMetadataFilters());
    }

    // Convert single tag to array for API
    String[] filterTags = params.getTag() != null ? new String[] {params.getTag()} : null;

    // Call server-side filter endpoint
    var response =
        sdkExtension.getApplicationsFiltered(
            orgId,
            params.getName(),
            filterTags,
            resolvedMetadataFilters,
            pagination.limit(),
            pagination.offset());

    if (response == null || response.getApplications() == null) {
      warnings.add("API returned no application data.");
      return ExecutionResult.empty();
    }

    var applications = response.getApplications().stream().map(this::toApplicationData).toList();

    return ExecutionResult.of(applications, response.getCount());
  }

  /**
   * Resolves user-provided metadata field names to numeric field IDs.
   *
   * @param sdkExtension SDK extension instance
   * @param orgId Organization ID
   * @param filters List of unresolved metadata filters with field names
   * @return List of resolved metadata filters with numeric field IDs
   * @throws Exception if any field name cannot be resolved
   */
  private List<AppMetadataFilter> resolveAppMetadataFilters(
      SDKExtension sdkExtension, String orgId, List<UnresolvedMetadataFilter> filters)
      throws Exception {

    var fieldNameToId = buildAppMetadataFieldMapping(sdkExtension, orgId);

    var notFoundFields =
        filters.stream()
            .map(UnresolvedMetadataFilter::fieldName)
            .filter(name -> !fieldNameToId.containsKey(name.toLowerCase()))
            .toList();

    if (!notFoundFields.isEmpty()) {
      throw new IllegalArgumentException(
          String.format(
              "Metadata field(s) not found: %s. Available fields can be discovered via the Contrast"
                  + " UI.",
              String.join(", ", notFoundFields)));
    }

    return filters.stream()
        .map(
            f -> {
              var fieldId = fieldNameToId.get(f.fieldName().toLowerCase());
              log.debug("Resolved app metadata field '{}' to ID '{}'", f.fieldName(), fieldId);
              return new AppMetadataFilter(fieldId, f.values().toArray(new String[0]));
            })
        .toList();
  }

  /**
   * Builds a case-insensitive mapping from field names to their numeric IDs.
   *
   * @param sdkExtension SDK extension instance
   * @param orgId Organization ID
   * @return Map of lowercase field names to field IDs
   */
  private Map<String, Long> buildAppMetadataFieldMapping(SDKExtension sdkExtension, String orgId)
      throws Exception {

    var fields = sdkExtension.getApplicationMetadataFields(orgId);
    var mapping = new HashMap<String, Long>();
    for (var field : fields) {
      if (field.getDisplayLabel() != null) {
        mapping.put(field.getDisplayLabel().toLowerCase(), field.getFieldId());
      }
    }
    return mapping;
  }

  /**
   * Converts an Application SDK model to ApplicationData response record.
   *
   * @param app the SDK application model
   * @return ApplicationData response record
   */
  private ApplicationData toApplicationData(Application app) {
    var metadata =
        app.getMetadataEntities().stream()
            .map(m -> new Metadata(m.getName(), m.getValue()))
            .toList();

    return new ApplicationData(
        app.getName(),
        app.getStatus(),
        app.getAppId(),
        FilterHelper.formatTimestamp(app.getLastSeen()),
        app.getLanguage(),
        metadata,
        app.getTags(),
        app.getTechs());
  }
}
