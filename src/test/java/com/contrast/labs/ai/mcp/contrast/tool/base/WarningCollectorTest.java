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

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class WarningCollectorTest {

  private static final org.slf4j.Logger log = LoggerFactory.getLogger(WarningCollectorTest.class);

  @Test
  void tryFetch_should_return_value_when_supplier_succeeds() {
    var collector = WarningCollector.forContext(log, Map.of());

    var result = collector.<String>tryFetch("Test data", () -> "value");

    assertThat(result).contains("value");
    assertThat(collector.snapshot()).isEmpty();
  }

  @Test
  void tryFetch_should_return_empty_and_record_warning_when_supplier_throws() {
    var collector = WarningCollector.forContext(log, Map.of());

    var result =
        collector.<String>tryFetch(
            "Recommendation data",
            () -> {
              throw new RuntimeException("API error");
            });

    assertThat(result).isEmpty();
    assertThat(collector.snapshot()).hasSize(1);
    assertThat(collector.snapshot().get(0)).contains("Recommendation data");
  }

  @Test
  void tryFetch_should_return_empty_and_record_warning_when_supplier_returns_null() {
    var collector = WarningCollector.forContext(log, Map.of());

    var result = collector.<String>tryFetch("Null data", () -> null);

    assertThat(result).isEmpty();
    assertThat(collector.snapshot()).hasSize(1);
    assertThat(collector.snapshot().get(0)).contains("Null data");
  }

  @Test
  void tryFetchNonNull_should_return_empty_without_warning_when_supplier_returns_null() {
    var collector = WarningCollector.forContext(log, Map.of());

    var result = collector.<String>tryFetchNonNull("Optional data", () -> null);

    assertThat(result).isEmpty();
    assertThat(collector.snapshot()).isEmpty();
  }

  @Test
  void tryFetchNonNull_should_return_value_when_supplier_returns_non_null() {
    var collector = WarningCollector.forContext(log, Map.of());

    var result = collector.<String>tryFetchNonNull("Optional data", () -> "value");

    assertThat(result).contains("value");
    assertThat(collector.snapshot()).isEmpty();
  }

  @Test
  void tryFetchNonNull_should_return_empty_and_record_warning_when_supplier_throws() {
    var collector = WarningCollector.forContext(log, Map.of());

    var result =
        collector.<String>tryFetchNonNull(
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
    var collector = WarningCollector.forContext(log, Map.of());

    var success = collector.tryRun("Stack trace data", () -> {});

    assertThat(success).isTrue();
    assertThat(collector.snapshot()).isEmpty();
  }

  @Test
  void tryRun_should_return_false_and_record_warning_when_operation_throws() {
    var collector = WarningCollector.forContext(log, Map.of());

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
    var collector = WarningCollector.forContext(log, Map.of());

    collector.<String>tryFetch(
        "Recommendation data",
        () -> {
          throw new RuntimeException("403 Forbidden");
        });

    assertThat(collector.snapshot().get(0))
        .isEqualTo("Recommendation data not available (retrieval error)");
  }

  @Test
  void tryFetch_should_not_indicate_error_in_warning_when_supplier_returns_null() {
    var collector = WarningCollector.forContext(log, Map.of());

    collector.<String>tryFetch("Null data", () -> null);

    assertThat(collector.snapshot().get(0)).isEqualTo("Null data not available");
  }

  @Test
  void tryFetchNonNull_should_indicate_retrieval_error_in_warning_when_supplier_throws() {
    var collector = WarningCollector.forContext(log, Map.of());

    collector.<String>tryFetchNonNull(
        "HTTP request data",
        () -> {
          throw new RuntimeException("Connection timeout");
        });

    assertThat(collector.snapshot().get(0))
        .isEqualTo("HTTP request data not available (retrieval error)");
  }

  @Test
  void tryRun_should_indicate_retrieval_error_in_warning_when_operation_throws() {
    var collector = WarningCollector.forContext(log, Map.of());

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
    var collector = WarningCollector.forContext(log, Map.of());

    collector.warn("Something happened");

    assertThat(collector.snapshot()).containsExactly("Something happened");
  }

  @Test
  void multiple_failed_fetches_should_accumulate_warnings_independently() {
    var collector = WarningCollector.forContext(log, Map.of());

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
    collector.<String>tryFetch(
        "Third data",
        () -> {
          throw new RuntimeException();
        });

    assertThat(collector.snapshot()).hasSize(3);
  }

  @Test
  void snapshot_should_return_immutable_copy() {
    var collector = WarningCollector.forContext(log, Map.of());
    collector.warn("existing warning");

    var snapshot = collector.snapshot();
    assertThatThrownBy(() -> snapshot.add("injected"))
        .isInstanceOf(UnsupportedOperationException.class);

    assertThat(collector.snapshot()).containsExactly("existing warning");
  }
}
