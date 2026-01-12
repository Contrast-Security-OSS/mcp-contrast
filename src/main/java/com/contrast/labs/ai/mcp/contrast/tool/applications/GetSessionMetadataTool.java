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
package com.contrast.labs.ai.mcp.contrast.tool.applications;

import com.contrast.labs.ai.mcp.contrast.tool.applications.params.GetSessionMetadataParams;
import com.contrast.labs.ai.mcp.contrast.tool.base.SingleTool;
import com.contrast.labs.ai.mcp.contrast.tool.base.SingleToolResponse;
import com.contrastsecurity.models.MetadataFilterResponse;
import java.util.List;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tool for retrieving session metadata for an application. Demonstrates the tool-per-class
 * pattern with SingleTool for non-paginated single-item retrieval.
 */
@Service
public class GetSessionMetadataTool
    extends SingleTool<GetSessionMetadataParams, MetadataFilterResponse> {

  @Tool(
      name = "get_session_metadata",
      description =
          """
          Retrieves session metadata for a specific application by its ID.

          Returns the session metadata available for the application, including branch names,
          build IDs, and other custom metadata fields that can be used for filtering
          vulnerabilities in search_app_vulnerabilities.

          Use search_applications(name=...) to find the application ID from a name.

          Related tools:
          - search_applications: Find application IDs by name, tag, or metadata
          - search_app_vulnerabilities: Search vulnerabilities with session filtering
          """)
  public SingleToolResponse<MetadataFilterResponse> getSessionMetadata(
      @ToolParam(description = "Application ID (use search_applications to find)") String appId) {
    return executePipeline(() -> GetSessionMetadataParams.of(appId));
  }

  @Override
  protected MetadataFilterResponse doExecute(GetSessionMetadataParams params, List<String> warnings)
      throws Exception {
    var sdk = getContrastSDK();
    var orgId = getOrgId();

    var response = sdk.getSessionMetadataForApplication(orgId, params.appId(), null);

    if (response == null) {
      warnings.add(
          "No session metadata found for this application. "
              + "This may indicate the application has no recorded sessions.");
      return null;
    }

    return response;
  }
}
