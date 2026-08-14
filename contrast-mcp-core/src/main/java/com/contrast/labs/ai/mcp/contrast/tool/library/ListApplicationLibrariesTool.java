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
package com.contrast.labs.ai.mcp.contrast.tool.library;

import com.contrast.labs.ai.mcp.contrast.client.ContrastApiClient;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryExtended;
import com.contrast.labs.ai.mcp.contrast.tool.base.ExecutionResult;
import com.contrast.labs.ai.mcp.contrast.tool.base.NoticeCollector;
import com.contrast.labs.ai.mcp.contrast.tool.base.PaginatedTool;
import com.contrast.labs.ai.mcp.contrast.tool.base.PaginatedToolResponse;
import com.contrast.labs.ai.mcp.contrast.tool.base.PaginationParams;
import com.contrast.labs.ai.mcp.contrast.tool.library.params.ListApplicationLibrariesParams;
import com.contrast.labs.ai.mcp.contrast.tool.validation.ValidationConstants;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tool for listing libraries used by an application. Returns all third-party libraries with
 * class usage statistics to help identify actively used vs potentially unused dependencies.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ListApplicationLibrariesTool
    extends PaginatedTool<ListApplicationLibrariesParams, LibraryExtended> {

  private final ContrastApiClient contrastApiClient;

  @Override
  protected int getMaxPageSize() {
    return ValidationConstants.API_MAX_PAGE_SIZE;
  }

  @Tool(
      name = "list_application_libraries",
      description =
          """
          List the third-party libraries in one application, with known CVEs, severity counts,
          security grade, and class usage. classesUsed 0 means no classes from that library were
          seen loaded, so it is likely unused and unlikely to be exploitable. Use
          search_applications to find application IDs. Use list_applications_by_cve for the reverse
          direction, from a CVE to affected applications.
          """)
  public PaginatedToolResponse<LibraryExtended> listApplicationLibraries(
      @ToolParam(description = "Page number (1-based), default: 1", required = false) Integer page,
      @ToolParam(description = "Items per page (max 50), default: 50", required = false)
          Integer pageSize,
      @ToolParam(description = "Application ID") String appId,
      ToolContext toolContext) {
    return executePipeline(
        page, pageSize, () -> ListApplicationLibrariesParams.of(appId), toolContext);
  }

  public PaginatedToolResponse<LibraryExtended> listApplicationLibraries(
      Integer page, Integer pageSize, String appId) {
    return listApplicationLibraries(page, pageSize, appId, null);
  }

  @Override
  protected ExecutionResult<LibraryExtended> doExecute(
      PaginationParams pagination, ListApplicationLibrariesParams params, NoticeCollector collector)
      throws Exception {

    log.debug("Retrieving libraries for application: {}", params.appId());

    var response =
        contrastApiClient.getLibraryPage(
            params.appId(), pagination.pageSize(), pagination.offset());
    var libraries = response.getLibraries();
    Long totalCount = response.getCount();
    int total = totalCount != null ? totalCount.intValue() : 0;

    if (libraries == null || libraries.isEmpty()) {
      if (pagination.offset() == 0 && total == 0) {
        collector.noticeForEmptyResults(
            "No libraries found for this application. "
                + "The application may not have any third-party dependencies, "
                + "or library data may not have been collected yet.");
      }
      return ExecutionResult.of(List.of(), total);
    }

    log.debug("Retrieved {} libraries for application {}", libraries.size(), params.appId());

    return ExecutionResult.of(libraries, total);
  }
}
