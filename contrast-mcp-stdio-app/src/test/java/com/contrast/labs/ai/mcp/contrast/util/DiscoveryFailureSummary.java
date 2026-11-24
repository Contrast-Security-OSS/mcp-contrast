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
package com.contrast.labs.ai.mcp.contrast.util;

import com.contrastsecurity.exceptions.HttpResponseException;
import java.util.ArrayList;
import lombok.experimental.UtilityClass;

/**
 * One-line description of a discovery failure that is safe to print in public CI logs.
 *
 * <p>This repository is public, so its GitHub Actions logs are too. Exception messages can carry
 * org data (the SDK's {@code HttpResponseException} message includes the response body), so the
 * summary is built from an allowlist: exception class names along the cause chain, plus the HTTP
 * status code and status text for SDK HTTP errors. A field not on the list never appears.
 */
@UtilityClass
class DiscoveryFailureSummary {

  /**
   * Prefix on the summary line. The integrationTest task in contrast-mcp-stdio-app/build.gradle
   * echoes only stdout lines containing this text, so keep the two in sync.
   */
  static final String MARKER = "[discovery-failure]";

  private static final int MAX_CAUSE_DEPTH = 10;
  private static final String CAUSED_BY = " caused by ";

  /** Returns the marker followed by the cause chain, outermost exception first. */
  static String of(Throwable failure) {
    var parts = new ArrayList<String>();
    var current = failure;
    for (var depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
      parts.add(describe(current));
      current = current.getCause();
    }
    return MARKER + " " + String.join(CAUSED_BY, parts);
  }

  private static String describe(Throwable failure) {
    var name = failure.getClass().getSimpleName();
    if (!(failure instanceof HttpResponseException http)) {
      return name;
    }
    var withCode = name + " " + http.getCode();
    // getStatus() is null when the response had no status line.
    return http.getStatus() == null ? withCode : withCode + " " + http.getStatus();
  }
}
