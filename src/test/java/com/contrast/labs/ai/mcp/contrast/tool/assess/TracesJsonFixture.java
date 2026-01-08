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
package com.contrast.labs.ai.mcp.contrast.tool.assess;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Test fixture helper for generating valid Traces JSON responses.
 *
 * <p>Centralizes JSON fixtures for tests that mock at the SDK boundary, ensuring valid JSON
 * structure and making tests more readable.
 */
class TracesJsonFixture {

  /** Returns JSON for an empty traces response with count of 0. */
  static String emptyTraces() {
    return """
    {"traces": [], "count": 0}
    """;
  }

  /**
   * Returns JSON for a single trace response.
   *
   * @param uuid the trace UUID
   * @param title the trace title
   * @param severity the severity (e.g., "CRITICAL", "HIGH")
   * @return JSON string representing a Traces response with one trace
   */
  static String singleTrace(String uuid, String title, String severity) {
    return """
    {
      "traces": [{
        "uuid": "%s",
        "title": "%s",
        "severity": "%s",
        "status": "Reported",
        "rule_name": "sql-injection"
      }],
      "count": 1
    }
    """
        .formatted(uuid, title, severity);
  }

  /**
   * Returns JSON for multiple traces response.
   *
   * @param count the number of traces to generate
   * @return JSON string representing a Traces response with specified number of traces
   */
  static String multipleTraces(int count) {
    var tracesJson =
        IntStream.range(0, count)
            .mapToObj(
                i ->
                    """
                    {
                      "uuid": "trace-%d",
                      "title": "Vulnerability %d",
                      "severity": "HIGH",
                      "status": "Reported",
                      "rule_name": "sql-injection"
                    }\
                    """
                        .formatted(i, i))
            .collect(Collectors.joining(",\n"));

    return """
    {
      "traces": [%s],
      "count": %d
    }
    """
        .formatted(tracesJson, count);
  }

  /**
   * Returns JSON for a traces response with null traces list. Used to test null handling in tools.
   */
  static String nullTraces() {
    return """
    {"traces": null, "count": 0}
    """;
  }

  /**
   * Returns JSON for a traces response with a specific total count. Useful for pagination tests
   * where the returned items differ from total.
   *
   * @param itemCount number of trace items to include
   * @param totalCount the total count to report (can be higher than itemCount)
   * @return JSON string representing a Traces response
   */
  static String tracesWithTotal(int itemCount, int totalCount) {
    var tracesJson =
        IntStream.range(0, itemCount)
            .mapToObj(
                i ->
                    """
                    {
                      "uuid": "trace-%d",
                      "title": "Vulnerability %d",
                      "severity": "HIGH",
                      "status": "Reported",
                      "rule_name": "sql-injection"
                    }\
                    """
                        .formatted(i, i))
            .collect(Collectors.joining(",\n"));

    return """
    {
      "traces": [%s],
      "count": %d
    }
    """
        .formatted(tracesJson, totalCount);
  }
}
