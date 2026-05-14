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
package com.contrast.labs.ai.mcp.contrast.tool.base;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * String constants for structured log keys used across the codebase.
 *
 * <p>Use these instead of inline string literals when calling {@code addKeyValue(...)} or building
 * a {@code WarningCollector} context map, so the key set stays consistent and renames are
 * compiler-enforced.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LoggingKeys {

  public static final String APP_ID = "appId";
  public static final String DURATION_MS = "durationMs";
  public static final String ERROR_COUNT = "errorCount";
  public static final String EXCEPTION_TYPE = "exceptionType";
  public static final String FILTER_NAME = "filterName";
  public static final String HTTP_STATUS = "httpStatus";
  public static final String ITEM_COUNT = "itemCount";
  public static final String REQUEST_ID = "requestId";
  public static final String TOTAL_ITEMS = "totalItems";
  public static final String VULN_ID = "vulnId";
}
