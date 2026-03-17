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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.contrastsecurity.exceptions.HttpResponseException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@Slf4j
class WarningCollectorTest {

  private WarningCollector collector;

  @BeforeEach
  void setUp() {
    collector = WarningCollector.forContext(log, Map.of());
  }

  @Test
  void tryFetch_should_return_empty_without_warning_when_supplier_returns_null() {
    var result = collector.<String>tryFetch("Optional data", () -> null);

    assertThat(result).isEmpty();
    assertThat(collector.snapshot()).isEmpty();
  }

  @Test
  void tryFetch_should_return_value_when_supplier_returns_non_null() {
    var result = collector.<String>tryFetch("Optional data", () -> "value");

    assertThat(result).contains("value");
    assertThat(collector.snapshot()).isEmpty();
  }

  @Test
  void tryFetch_should_return_empty_and_record_warning_when_supplier_throws() {
    var result =
        collector.<String>tryFetch(
            "Optional data",
            () -> {
              throw new RuntimeException("API error");
            });

    assertThat(result).isEmpty();
    assertThat(collector.snapshot()).hasSize(1);
    assertThat(collector.snapshot().get(0)).contains("Optional data");
  }

  @Test
  void tryRun_should_return_true_when_operation_succeeds() {
    var success = collector.tryRun("Stack trace data", () -> {});

    assertThat(success).isTrue();
    assertThat(collector.snapshot()).isEmpty();
  }

  @Test
  void tryRun_should_return_false_and_record_warning_when_operation_throws() {
    var success =
        collector.tryRun(
            "Stack trace data",
            () -> {
              throw new RuntimeException("SDK error");
            });

    assertThat(success).isFalse();
    assertThat(collector.snapshot()).hasSize(1);
    assertThat(collector.snapshot().get(0)).contains("Stack trace data");
  }

  @Test
  void tryFetch_should_indicate_retrieval_error_in_warning_when_supplier_throws() {
    collector.<String>tryFetch(
        "HTTP request data",
        () -> {
          throw new RuntimeException("Connection timeout");
        });

    assertThat(collector.snapshot().get(0))
        .isEqualTo("HTTP request data not available (retrieval error)");
  }

  @Test
  void tryRun_should_indicate_retrieval_error_in_warning_when_operation_throws() {
    collector.tryRun(
        "Stack trace data",
        () -> {
          throw new RuntimeException("SDK error");
        });

    assertThat(collector.snapshot().get(0))
        .isEqualTo("Stack trace data not available (retrieval error)");
  }

  @Test
  void warn_should_unconditionally_append_warning() {
    collector.warn("Something happened");

    assertThat(collector.snapshot()).containsExactly("Something happened");
  }

  @Test
  void warn_should_throw_when_message_is_null() {
    assertThatThrownBy(() -> collector.warn(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void warn_should_silently_skip_when_message_is_blank() {
    collector.warn("");
    collector.warn("   ");

    assertThat(collector.snapshot()).isEmpty();
  }

  @Test
  void multiple_failed_fetches_should_accumulate_warnings_independently() {
    collector.<String>tryFetch(
        "First data",
        () -> {
          throw new RuntimeException();
        });
    collector.<String>tryFetch(
        "Second data",
        () -> {
          throw new RuntimeException();
        });
    collector.tryRun(
        "Third data",
        () -> {
          throw new RuntimeException();
        });

    assertThat(collector.snapshot()).hasSize(3);
  }

  @Test
  void tryFetch_should_include_http_status_code_in_warning_when_http_exception_thrown() {
    collector.<String>tryFetch(
        "Stack trace data",
        () -> {
          throw new HttpResponseException(
              "Service Unavailable", "GET", "/api", 503, "Service Unavailable");
        });

    assertThat(collector.snapshot().get(0))
        .isEqualTo("Stack trace data not available (retrieval error, HTTP 503)");
  }

  @Test
  void tryRun_should_include_http_status_code_in_warning_when_http_exception_thrown() {
    collector.tryRun(
        "Class usage data",
        () -> {
          throw new HttpResponseException(
              "Internal Server Error", "GET", "/api", 500, "Internal Server Error");
        });

    assertThat(collector.snapshot().get(0))
        .isEqualTo("Class usage data not available (retrieval error, HTTP 500)");
  }

  @Test
  void snapshot_should_return_immutable_copy() {
    collector.warn("existing warning");

    var snapshot = collector.snapshot();
    assertThatThrownBy(() -> snapshot.add("injected"))
        .isInstanceOf(UnsupportedOperationException.class);

    assertThat(collector.snapshot()).containsExactly("existing warning");
  }
}
