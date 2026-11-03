/*
 * Copyright 2026 Contrast Security
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

import com.contrast.labs.ai.mcp.contrast.client.ContrastApiClient;
import com.contrast.labs.ai.mcp.contrast.tool.application.params.GetSessionMetadataParams;
import com.contrast.labs.ai.mcp.contrast.tool.base.NoticeCollector;
import com.contrast.labs.ai.mcp.contrast.tool.base.SingleTool;
import com.contrast.labs.ai.mcp.contrast.tool.base.SingleToolResponse;
import com.contrastsecurity.models.MetadataFilterResponse;
import org.springframework.ai.chat.model.ToolContext;
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

  private final ContrastApiClient contrastApiClient;

  public GetSessionMetadataTool(ContrastApiClient contrastApiClient) {
    this.contrastApiClient = contrastApiClient;
  }

  @Tool(
      name = "get_session_metadata",
      description =
          """
          Get the session metadata fields and values recorded for an application, such as branch or
          build. Field names feed sessionMetadataFilters in search_app_vulnerabilities and session
          filters in get_route_coverage. Use search_applications to find application IDs.
          """)
  public SingleToolResponse<MetadataFilterResponse> getSessionMetadata(
      @ToolParam(description = "Application ID") String appId, ToolContext toolContext) {
    return executePipeline(() -> GetSessionMetadataParams.of(appId), toolContext);
  }

  public SingleToolResponse<MetadataFilterResponse> getSessionMetadata(String appId) {
    return getSessionMetadata(appId, null);
  }

  @Override
  protected MetadataFilterResponse doExecute(
      GetSessionMetadataParams params, NoticeCollector collector) throws Exception {
    var response = contrastApiClient.getSessionMetadata(params.appId());

    if (response == null) {
      collector.notice("No session metadata found for this application.");
      return null;
    }

    return response;
  }
}
