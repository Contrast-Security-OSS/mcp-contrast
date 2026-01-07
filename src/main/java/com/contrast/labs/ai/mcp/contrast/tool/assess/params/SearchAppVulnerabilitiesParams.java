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
package com.contrast.labs.ai.mcp.contrast.tool.assess.params;

import com.contrast.labs.ai.mcp.contrast.tool.base.BaseToolParams;
import com.contrast.labs.ai.mcp.contrast.tool.validation.ToolValidationContext;
import com.contrastsecurity.http.RuleSeverity;
import com.contrastsecurity.http.ServerEnvironment;
import com.contrastsecurity.http.TraceFilterForm;
import com.contrastsecurity.models.TraceFilterBody;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Validation parameters for SearchAppVulnerabilitiesTool. Extends VulnerabilityFilterParams
 * functionality with application-scoped and session filtering capabilities.
 *
 * <p>Usage:
 *
 * <pre>{@code
 * var params = SearchAppVulnerabilitiesParams.of(
 *     "app-123", "CRITICAL", null, null, null, null, null, null, "branch", "main", false);
 * if (!params.isValid()) {
 *   // Handle errors
 * }
 * var filterForm = params.toTraceFilterForm();
 * }</pre>
 */
public class SearchAppVulnerabilitiesParams extends BaseToolParams {

  /** Valid status values for vulnerability filtering. */
  public static final Set<String> VALID_STATUSES =
      Set.of("Reported", "Suspicious", "Confirmed", "Remediated", "Fixed");

  /** Default statuses - excludes Fixed and Remediated to focus on actionable items. */
  public static final List<String> DEFAULT_STATUSES =
      List.of("Reported", "Suspicious", "Confirmed");

  private String appId;
  private EnumSet<RuleSeverity> severities;
  private List<String> statuses;
  private List<String> vulnTypes;
  private EnumSet<ServerEnvironment> environments;
  private Date lastSeenAfter;
  private Date lastSeenBefore;
  private List<String> vulnTags;
  private String sessionMetadataName;
  private String sessionMetadataValue;
  private Boolean useLatestSession;

  /** Private constructor - use static factory method {@link #of}. */
  private SearchAppVulnerabilitiesParams() {}

  /**
   * Parse and validate application vulnerability search parameters.
   *
   * @param appIdParam Application ID (required)
   * @param severitiesParam Comma-separated severities (CRITICAL,HIGH,MEDIUM,LOW,NOTE)
   * @param statusesParam Comma-separated statuses (Reported,Suspicious,Confirmed,Remediated,Fixed)
   * @param vulnTypesParam Comma-separated vulnerability types
   * @param environmentsParam Comma-separated environments (DEVELOPMENT,QA,PRODUCTION)
   * @param lastSeenAfterParam ISO date (YYYY-MM-DD) or epoch timestamp
   * @param lastSeenBeforeParam ISO date (YYYY-MM-DD) or epoch timestamp
   * @param vulnTagsParam Comma-separated vulnerability tags
   * @param sessionMetadataNameParam Session metadata field name (case-insensitive)
   * @param sessionMetadataValueParam Session metadata field value (requires name)
   * @param useLatestSessionParam Filter to latest session only
   * @return SearchAppVulnerabilitiesParams with validation state
   */
  public static SearchAppVulnerabilitiesParams of(
      String appIdParam,
      String severitiesParam,
      String statusesParam,
      String vulnTypesParam,
      String environmentsParam,
      String lastSeenAfterParam,
      String lastSeenBeforeParam,
      String vulnTagsParam,
      String sessionMetadataNameParam,
      String sessionMetadataValueParam,
      Boolean useLatestSessionParam) {

    var params = new SearchAppVulnerabilitiesParams();
    var ctx = new ToolValidationContext();

    // Required field
    ctx.require(appIdParam, "appId");
    params.appId = appIdParam;

    // Parse filter parameters with fluent API
    params.severities = ctx.enumSetParam(severitiesParam, RuleSeverity.class, "severities").get();

    params.statuses =
        ctx.stringListParam(statusesParam, "statuses")
            .allowedValues(VALID_STATUSES)
            .defaultTo(
                DEFAULT_STATUSES,
                "Showing actionable vulnerabilities only (excluding Fixed and Remediated). "
                    + "To see all statuses, specify statuses parameter explicitly.")
            .get();

    params.vulnTypes = ctx.stringListParam(vulnTypesParam, "vulnTypes").get();

    params.environments =
        ctx.enumSetParam(environmentsParam, ServerEnvironment.class, "environments").get();

    params.lastSeenAfter = ctx.dateParam(lastSeenAfterParam, "lastSeenAfter").get();
    params.lastSeenBefore = ctx.dateParam(lastSeenBeforeParam, "lastSeenBefore").get();
    ctx.validateDateRange(
        params.lastSeenAfter, params.lastSeenBefore, "lastSeenAfter", "lastSeenBefore");

    // Add time filter note if dates were specified
    if (params.lastSeenAfter != null || params.lastSeenBefore != null) {
      ctx.warnIf(
          true, "Time filters apply to LAST ACTIVITY DATE (lastTimeSeen), not discovery date.");
    }

    params.vulnTags = ctx.stringListParam(vulnTagsParam, "vulnTags").get();

    // Session filtering parameters with cross-field validation
    params.sessionMetadataName = sessionMetadataNameParam;
    params.sessionMetadataValue = sessionMetadataValueParam;
    params.useLatestSession = useLatestSessionParam;

    // Validate sessionMetadataValue requires sessionMetadataName
    ctx.requireIfPresent(
        sessionMetadataValueParam,
        "sessionMetadataValue",
        sessionMetadataNameParam,
        "sessionMetadataName");

    params.setValidationResult(ctx);
    return params;
  }

  /**
   * Convert to SDK TraceFilterBody for POST endpoint API calls.
   *
   * @return TraceFilterBody configured with all filters
   */
  public TraceFilterBody toTraceFilterBody() {
    var body = new TraceFilterBody();
    // Note: tracked/untracked NOT set - primitive defaults (false) mean "return all"
    if (severities != null) {
      body.setSeverities(severities.stream().toList());
    }
    if (vulnTypes != null) {
      body.setVulnTypes(vulnTypes);
    }
    if (environments != null) {
      body.setEnvironments(environments.stream().toList());
    }
    if (lastSeenAfter != null) {
      body.setStartDate(lastSeenAfter);
    }
    if (lastSeenBefore != null) {
      body.setEndDate(lastSeenBefore);
    }
    if (vulnTags != null) {
      body.setFilterTags(vulnTags);
    }
    return body;
  }

  /**
   * Convert to SDK TraceFilterForm for API calls.
   *
   * @return TraceFilterForm configured with all filters
   */
  public TraceFilterForm toTraceFilterForm() {
    var form = new TraceFilterForm();
    // Override SDK defaults to return ALL vulnerabilities (tracked and untracked)
    // SDK defaults to tracked=true, untracked=false which filters out untracked vulns
    form.setTracked(true);
    form.setUntracked(true);
    if (severities != null) {
      form.setSeverities(severities);
    }
    if (statuses != null) {
      form.setStatus(statuses);
    }
    if (vulnTypes != null) {
      form.setVulnTypes(vulnTypes);
    }
    if (environments != null) {
      form.setEnvironments(environments);
    }
    if (lastSeenAfter != null) {
      form.setStartDate(lastSeenAfter);
    }
    if (lastSeenBefore != null) {
      form.setEndDate(lastSeenBefore);
    }
    if (vulnTags != null) {
      form.setFilterTags(vulnTags);
    }
    return form;
  }

  /**
   * Returns true if session-based filtering is needed.
   *
   * @return true if useLatestSession or sessionMetadataName is specified
   */
  public boolean needsSessionFiltering() {
    return Boolean.TRUE.equals(useLatestSession)
        || (sessionMetadataName != null && !sessionMetadataName.isBlank());
  }

  public String appId() {
    return appId;
  }

  public EnumSet<RuleSeverity> getSeverities() {
    return severities;
  }

  public List<String> getStatuses() {
    return statuses;
  }

  public List<String> getVulnTypes() {
    return vulnTypes;
  }

  public EnumSet<ServerEnvironment> getEnvironments() {
    return environments;
  }

  public Date getLastSeenAfter() {
    return lastSeenAfter;
  }

  public Date getLastSeenBefore() {
    return lastSeenBefore;
  }

  public List<String> getVulnTags() {
    return vulnTags;
  }

  public String getSessionMetadataName() {
    return sessionMetadataName;
  }

  public String getSessionMetadataValue() {
    return sessionMetadataValue;
  }

  public Boolean getUseLatestSession() {
    return useLatestSession;
  }
}
