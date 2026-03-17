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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;

/**
 * Accumulates warnings during MCP tool execution and encapsulates the try/catch/warn pattern for
 * optional data fetches.
 *
 * <p>Constructed once per request by the base classes and passed into {@code doExecute}. Tool
 * implementations use {@link #tryFetchRequired}, {@link #tryFetch}, and {@link #tryRun} for
 * optional enrichment fetches that should degrade gracefully on failure.
 *
 * <p>{@link #snapshot()} is package-private — only base classes call it when building the response.
 */
public final class WarningCollector {

  /** Checked supplier that may throw any exception. */
  @FunctionalInterface
  public interface CheckedSupplier<T> {
    T get() throws Exception;
  }

  /** Checked runnable that may throw any exception. */
  @FunctionalInterface
  public interface CheckedRunnable {
    void run() throws Exception;
  }

  private final Logger log;
  private final Map<String, Object> context;
  private final List<String> warnings = new ArrayList<>();

  private WarningCollector(Logger log, Map<String, Object> context) {
    this.log = log;
    this.context = context;
  }

  /**
   * Creates a new collector bound to the given logger and log context key/values. The context
   * entries are added to every WARN log emitted by {@link #tryFetchRequired}, {@link #tryFetch},
   * and {@link #tryRun}.
   */
  public static WarningCollector forContext(Logger log, Map<String, Object> context) {
    return new WarningCollector(log, context);
  }

  /**
   * Executes {@code fetch} and returns the result wrapped in an Optional.
   *
   * <p>On null result: records {@code description + " not available"} as a warning, returns empty.
   * Use this when null indicates a missing required value that the caller should know about.
   *
   * <p>On exception: logs WARN, records {@code description + " not available (retrieval error)"} as
   * a warning, returns empty. The {@code (retrieval error)} suffix signals to AI agents that the
   * absence was caused by a fetch failure, not a legitimate absence of data. Exception details are
   * logged server-side but excluded from the agent-visible warning to avoid leaking internal API
   * information.
   */
  public <T> Optional<T> tryFetchRequired(String description, CheckedSupplier<T> fetch) {
    try {
      var result = fetch.get();
      if (result == null) {
        warnings.add(description + " not available");
        return Optional.empty();
      }
      return Optional.of(result);
    } catch (Exception e) {
      logWarn(description, e);
      warnings.add(description + " not available (retrieval error)");
      return Optional.empty();
    }
  }

  /**
   * Executes {@code fetch} and returns the result wrapped in an Optional.
   *
   * <p>Null return is treated as absent — no warning emitted. Use this when null is a legitimate
   * outcome (e.g. optional enrichment that may simply not exist).
   *
   * <p>On exception: logs WARN, records {@code description + " not available (retrieval error)"} as
   * a warning, returns empty. The {@code (retrieval error)} suffix signals to AI agents that the
   * data was absent due to a fetch failure (e.g. permission denied, network error), not because the
   * data legitimately does not exist. Exception details are logged server-side but excluded from
   * the agent-visible warning to avoid leaking internal API information.
   */
  public <T> Optional<T> tryFetch(String description, CheckedSupplier<T> fetch) {
    try {
      return Optional.ofNullable(fetch.get());
    } catch (Exception e) {
      logWarn(description, e);
      warnings.add(description + " not available (retrieval error)");
      return Optional.empty();
    }
  }

  /**
   * Executes {@code operation}. Returns {@code true} on success, {@code false} if an exception is
   * thrown. On exception: logs WARN, records {@code description + " not available (retrieval
   * error)"} as a warning.
   *
   * <p>The {@code (retrieval error)} suffix signals to AI agents that the data was absent due to a
   * fetch failure. Exception details are logged server-side but excluded from the agent-visible
   * warning to avoid leaking internal API information.
   */
  public boolean tryRun(String description, CheckedRunnable operation) {
    try {
      operation.run();
      return true;
    } catch (Exception e) {
      logWarn(description, e);
      warnings.add(description + " not available (retrieval error)");
      return false;
    }
  }

  /** Unconditionally appends {@code message} to the warning list. */
  public void warn(String message) {
    warnings.add(message);
  }

  /**
   * Returns an immutable snapshot of all warnings accumulated so far. Package-private — only base
   * classes call this when building the response.
   */
  List<String> snapshot() {
    return List.copyOf(warnings);
  }

  private void logWarn(String description, Exception e) {
    var builder =
        log.atWarn().setCause(e).setMessage("Could not fetch: {}").addArgument(description);
    context.forEach(builder::addKeyValue);
    builder.log();
  }
}
