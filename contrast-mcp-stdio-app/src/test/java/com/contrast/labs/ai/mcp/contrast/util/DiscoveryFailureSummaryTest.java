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

import static org.assertj.core.api.Assertions.assertThat;

import com.contrastsecurity.exceptions.HttpResponseException;
import java.io.UncheckedIOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;

class DiscoveryFailureSummaryTest {

  private static final String SECRET_BODY = "{\"apps\":[\"Customer Payroll Service\"]}";
  private static final String SECRET_PATH = "/ng/org-123/applications/app-456";
  private static final int MAX_DEPTH = 10;

  @Test
  void of_should_list_class_names_of_the_cause_chain_without_messages_when_failure_is_wrapped() {
    var failure =
        new UncheckedIOException(
            "lookup failed for Customer Payroll Service",
            new SocketTimeoutException("Read timed out from org-123"));

    var summary = DiscoveryFailureSummary.of(failure);

    assertThat(summary)
        .isEqualTo(
            DiscoveryFailureSummary.MARKER
                + " UncheckedIOException caused by SocketTimeoutException")
        .doesNotContain("Customer Payroll Service", "org-123", "Read timed out");
  }

  @Test
  void of_should_include_http_status_but_not_body_path_or_message_when_failure_is_http() {
    var failure =
        new HttpResponseException(
            "Customer Payroll Service lookup",
            "GET",
            SECRET_PATH,
            HttpURLConnection.HTTP_UNAVAILABLE,
            "Service Unavailable",
            SECRET_BODY);

    var summary = DiscoveryFailureSummary.of(failure);

    assertThat(summary)
        .isEqualTo(
            DiscoveryFailureSummary.MARKER + " HttpResponseException 503 Service Unavailable")
        .doesNotContain(SECRET_BODY, SECRET_PATH, "Customer Payroll Service");
  }

  @Test
  void of_should_print_only_the_code_when_http_failure_has_no_status_line() {
    var failure =
        new HttpResponseException(
            "failed", "GET", SECRET_PATH, HttpURLConnection.HTTP_BAD_GATEWAY, null, SECRET_BODY);

    assertThat(DiscoveryFailureSummary.of(failure))
        .isEqualTo(DiscoveryFailureSummary.MARKER + " HttpResponseException 502");
  }

  @Test
  void of_should_stop_after_ten_causes_when_the_cause_chain_is_cyclic() {
    var first = new IllegalStateException("first");
    var second = new IllegalArgumentException("second");
    first.initCause(second);
    second.initCause(first);

    var summary = DiscoveryFailureSummary.of(first);

    assertThat(summary.split(" caused by ")).hasSize(MAX_DEPTH);
  }
}
