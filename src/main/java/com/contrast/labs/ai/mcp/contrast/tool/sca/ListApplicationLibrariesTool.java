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
package com.contrast.labs.ai.mcp.contrast.tool.sca;

import com.contrast.labs.ai.mcp.contrast.PaginationParams;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryExtended;
import com.contrast.labs.ai.mcp.contrast.tool.base.BasePaginatedTool;
import com.contrast.labs.ai.mcp.contrast.tool.base.ExecutionResult;
import com.contrast.labs.ai.mcp.contrast.tool.base.PaginatedToolResponse;
import com.contrast.labs.ai.mcp.contrast.tool.sca.params.ListApplicationLibrariesParams;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tool for listing libraries used by an application. Returns all third-party libraries with
 * class usage statistics to help identify actively used vs potentially unused dependencies.
 *
 * <p>Note: This tool will be DEPRECATED once search_libraries is implemented. The search_libraries
 * tool will provide more flexible filtering capabilities.
 */
@Service
@Slf4j
public class ListApplicationLibrariesTool
    extends BasePaginatedTool<ListApplicationLibrariesParams, LibraryExtended> {

  private static final int API_MAX_PAGE_SIZE = 50;

  @Override
  protected int getMaxPageSize() {
    return API_MAX_PAGE_SIZE;
  }

  @Tool(
      name = "list_application_libraries",
      description =
          """
          Returns all libraries used by a specific application.

          Use search_applications(name=...) to find the application ID from a name.

          Response includes for each library:
          - filename: Library file name (e.g., "log4j-core-2.17.1.jar")
          - version: Library version
          - hash: Unique library hash for identification
          - classCount: Total classes in the library
          - classesUsed: Number of classes actually loaded by the application
          - vulnerabilities: Known CVEs affecting this library version
          - grade: Library security grade (A-F)

          Note: If classesUsed is 0, the library is likely not actively used and may
          be a transitive dependency. Libraries with 0 class usage are unlikely to
          be exploitable even if they have known vulnerabilities.

          Related tools:
          - search_applications: Find application IDs by name, tag, or metadata
          - list_applications_by_cve: Find applications affected by a specific CVE
          """)
  public PaginatedToolResponse<LibraryExtended> listApplicationLibraries(
      @ToolParam(description = "Page number (1-based), default: 1", required = false) Integer page,
      @ToolParam(description = "Items per page (max 50), default: 50", required = false)
          Integer pageSize,
      @ToolParam(description = "Application ID (use search_applications to find)") String appId) {
    return executePipeline(page, pageSize, () -> ListApplicationLibrariesParams.of(appId));
  }

  @Override
  protected ExecutionResult<LibraryExtended> doExecute(
      PaginationParams pagination, ListApplicationLibrariesParams params, List<String> warnings)
      throws Exception {
    var sdk = getContrastSDK();
    var orgId = getOrgId();
    var extendedSDK = new SDKExtension(sdk);

    log.debug("Retrieving libraries for application: {}", params.appId());

    var libraries = SDKHelper.getLibsForID(params.appId(), orgId, extendedSDK);

    if (libraries == null || libraries.isEmpty()) {
      warnings.add(
          "No libraries found for this application. "
              + "The application may not have any third-party dependencies, "
              + "or library data may not have been collected yet.");
      return ExecutionResult.empty();
    }

    log.debug("Retrieved {} libraries for application {}", libraries.size(), params.appId());

    // Apply pagination to results (same pattern as SearchApplicationsTool)
    int startIndex = pagination.offset();
    int endIndex = Math.min(startIndex + pagination.pageSize(), libraries.size());

    var pagedLibraries =
        (startIndex < libraries.size())
            ? libraries.subList(startIndex, endIndex)
            : List.<LibraryExtended>of();

    return ExecutionResult.of(pagedLibraries, libraries.size());
  }
}
