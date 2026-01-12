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
package com.contrast.labs.ai.mcp.contrast.tool.attack;

import com.contrast.labs.ai.mcp.contrast.data.AttackSummary;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.tool.attack.params.AttackFilterParams;
import com.contrast.labs.ai.mcp.contrast.tool.base.ExecutionResult;
import com.contrast.labs.ai.mcp.contrast.tool.base.PaginatedTool;
import com.contrast.labs.ai.mcp.contrast.tool.base.PaginatedToolResponse;
import com.contrast.labs.ai.mcp.contrast.tool.base.PaginationParams;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

/**
 * MCP tool for searching attacks across the organization. Demonstrates the tool-per-class pattern
 * with PaginatedTool.
 */
@Service
@RequiredArgsConstructor
public class SearchAttacksTool extends PaginatedTool<AttackFilterParams, AttackSummary> {

  @Tool(
      name = "search_attacks",
      description =
          """
          Retrieves attacks from Contrast ADR (Attack Detection and Response) with optional filtering
          and sorting. Supports filtering by attack categorization (quickFilter), outcome status
          (statusFilter), keywords, and other criteria.

          Returns a paginated list of attack summaries with key information including rule names,
          status, severity, affected applications, source IP, and probe counts.

          Common usage examples:
          - All attacks: No filters (returns all attack types)
          - Active attacks only: quickFilter="ACTIVE"
          - Exploited attacks: statusFilter="EXPLOITED"
          - SQL injection attacks: keyword="sql"
          - Blocked attacks: statusFilter="BLOCKED"
          - Production attacks: quickFilter="PRODUCTION"

          Filter combinations:
          - Effective exploits: quickFilter="EFFECTIVE", statusFilter="EXPLOITED"
          - Manual SQL attacks: quickFilter="MANUAL", keyword="sql"

          Response fields:
          - attackId: Unique identifier for the attack
          - status: Attack outcome (EXPLOITED, PROBED, BLOCKED, etc.)
          - source: Source IP address of the attacker
          - rules: Protection rules triggered by the attack
          - probes: Number of probe attempts
          - applications: List of affected applications with severity and status

          Related tools:
          - get_protect_rules: View configured protection rules for an application
          - search_applications: Find application IDs by name or tag
          """)
  public PaginatedToolResponse<AttackSummary> searchAttacks(
      @ToolParam(description = "Page number (1-based), default: 1", required = false) Integer page,
      @ToolParam(description = "Items per page (max 100), default: 50", required = false)
          Integer pageSize,
      @ToolParam(
              description =
                  "Quick filter for attack categorization. Valid: ALL (no filter), ACTIVE (ongoing"
                      + " attacks), MANUAL (human-initiated), AUTOMATED (bot attacks), PRODUCTION"
                      + " (prod environment), EFFECTIVE (excludes probed attacks)",
              required = false)
          String quickFilter,
      @ToolParam(
              description =
                  "Status filter for attack outcome. Valid: EXPLOITED (successfully exploited),"
                      + " PROBED (detected but not exploited), BLOCKED (blocked by Protect),"
                      + " BLOCKED_PERIMETER (blocked at perimeter), PROBED_PERIMETER (probed at"
                      + " perimeter), SUSPICIOUS (suspicious attack)",
              required = false)
          String statusFilter,
      @ToolParam(
              description = "Keyword to match against rule names, sources, or notes",
              required = false)
          String keyword,
      @ToolParam(description = "Include suppressed attacks when true", required = false)
          Boolean includeSuppressed,
      @ToolParam(description = "Include attacks flagged as bot blockers", required = false)
          Boolean includeBotBlockers,
      @ToolParam(description = "Include attacks from blacklisted IPs", required = false)
          Boolean includeIpBlacklist,
      @ToolParam(
              description =
                  "Sort field for ordering results. Valid fields: sourceIP, status, startTime,"
                      + " endTime, type. Use '-' prefix for descending order (e.g., '-startTime')."
                      + " Default: -startTime",
              required = false)
          String sort) {

    return executePipeline(
        page,
        pageSize,
        () ->
            AttackFilterParams.of(
                quickFilter,
                statusFilter,
                keyword,
                includeSuppressed,
                includeBotBlockers,
                includeIpBlacklist,
                sort));
  }

  @Override
  protected ExecutionResult<AttackSummary> doExecute(
      PaginationParams pagination, AttackFilterParams params, List<String> warnings)
      throws Exception {

    var sdk = getContrastSDK();
    var extendedSDK = new SDKExtension(sdk);

    var filterBody = params.toAttacksFilterBody();
    var attacksResponse =
        extendedSDK.getAttacks(
            getOrgId(), filterBody, pagination.limit(), pagination.offset(), params.getSort());

    if (attacksResponse == null || attacksResponse.getAttacks() == null) {
      warnings.add("API returned no attack data. Verify permissions and filters.");
      return ExecutionResult.empty();
    }

    var summaries = attacksResponse.getAttacks().stream().map(AttackSummary::fromAttack).toList();

    return ExecutionResult.of(summaries, attacksResponse.getTotalCount());
  }
}
