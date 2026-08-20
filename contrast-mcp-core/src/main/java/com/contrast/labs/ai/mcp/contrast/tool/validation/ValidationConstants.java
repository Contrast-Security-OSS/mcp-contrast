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
package com.contrast.labs.ai.mcp.contrast.tool.validation;

import java.util.List;
import java.util.Set;

/** Shared validation constants for MCP tools. */
public final class ValidationConstants {

  /** Default page number for pagination (1-indexed). */
  public static final int DEFAULT_PAGE = 1;

  /** Default page size for pagination. */
  public static final int DEFAULT_PAGE_SIZE = 50;

  /** Maximum allowed page size. */
  public static final int MAX_PAGE_SIZE = 100;

  /** Maximum page size enforced by the Contrast API for library endpoints. */
  public static final int API_MAX_PAGE_SIZE = 50;

  /** Default page size for library observations endpoint. */
  public static final int DEFAULT_LIBRARY_OBS_PAGE_SIZE = 25;

  /** Minimum page number (1-indexed). */
  public static final int MIN_PAGE = 1;

  /** Canonical vulnerability status for vulnerabilities marked not a problem. */
  public static final String NOT_A_PROBLEM_VULN_STATUS = "NotAProblem";

  /** Canonical vulnerability status for vulnerabilities auto-verified as remediated. */
  public static final String AUTO_REMEDIATED_VULN_STATUS = "AutoRemediated";

  /** Clarifies the legacy AutoRemediated status name. */
  public static final String AUTO_REMEDIATED_VULN_STATUS_NOTE =
      "AutoRemediated means the Agent verified a fix at runtime; Contrast did not fix it.";

  /** Canonical vulnerability statuses, formatted for tool parameter descriptions. */
  public static final String VALID_VULN_STATUSES_CSV =
      "Reported,Suspicious,Confirmed,"
          + NOT_A_PROBLEM_VULN_STATUS
          + ",Remediated,Fixed,"
          + AUTO_REMEDIATED_VULN_STATUS;

  /** Valid vulnerability status values. */
  public static final Set<String> VALID_VULN_STATUSES =
      Set.copyOf(List.of(VALID_VULN_STATUSES_CSV.split(",")));

  /** Default vulnerability statuses, formatted for tool parameter descriptions. */
  public static final String DEFAULT_VULN_STATUSES_CSV = "Reported,Suspicious,Confirmed";

  /** Default vulnerability statuses (actionable only). */
  public static final List<String> DEFAULT_VULN_STATUSES =
      List.of(DEFAULT_VULN_STATUSES_CSV.split(","));

  /** Description shared by vulnerability status tool parameters. */
  public static final String VULN_STATUSES_PARAM_DESCRIPTION =
      "Comma-separated vulnerability statuses: "
          + VALID_VULN_STATUSES_CSV
          + ". Default: "
          + DEFAULT_VULN_STATUSES_CSV
          + " (actionable only). "
          + AUTO_REMEDIATED_VULN_STATUS_NOTE;

  /** Notice emitted when a vulnerability search applies the default status filter. */
  public static final String DEFAULT_VULN_STATUSES_NOTICE =
      "Default vulnerability statuses: "
          + DEFAULT_VULN_STATUSES_CSV
          + " (actionable only). To see all vulnerability statuses, specify statuses explicitly.";

  /** Standing result semantics shared by vulnerability search tool descriptions. */
  public static final String VULN_STATUS_RESULT_SEMANTICS =
      "Result statuses use the same canonical vulnerability status values as the statuses filter.";

  private ValidationConstants() {}
}
