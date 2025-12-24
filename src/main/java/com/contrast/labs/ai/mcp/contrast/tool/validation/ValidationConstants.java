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

  /** Minimum page number (1-indexed). */
  public static final int MIN_PAGE = 1;

  /** Valid vulnerability status values. */
  public static final Set<String> VALID_VULN_STATUSES =
      Set.of(
          "Reported",
          "Suspicious",
          "Confirmed",
          "NotAProblem",
          "Remediated",
          "Fixed",
          "AutoRemediated");

  /** Default vulnerability statuses (actionable only, excludes Fixed/Remediated). */
  public static final List<String> DEFAULT_VULN_STATUSES =
      List.of("Reported", "Suspicious", "Confirmed");

  private ValidationConstants() {}
}
