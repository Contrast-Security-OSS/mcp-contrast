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
import java.io.EOFException;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongUnaryOperator;
import javax.net.ssl.SSLException;
import lombok.extern.slf4j.Slf4j;

/**
 * Retries an action that fails with a transient error, using exponential backoff with full jitter.
 *
 * <p>Meant for integration test data discovery against a shared live org, where a timeout or a 5xx
 * from the API says nothing about the code under test. Do not wrap test bodies with it, a retried
 * assertion would hide a real regression. Only an allowlist of connectivity errors and HTTP
 * 429/500/502/503/504 is retried. Everything else (4xx, missing data, bugs) fails at once.
 */
@Slf4j
public final class TransientRetry {

  static final int MAX_ATTEMPTS = 3;
  static final long BASE_DELAY_MS = 1_000L;
  static final long MAX_DELAY_MS = 8_000L;

  private static final int HTTP_TOO_MANY_REQUESTS = 429;
  private static final int MAX_CAUSE_DEPTH = 10;
  private static final Set<Integer> TRANSIENT_HTTP_STATUSES =
      Set.of(
          HTTP_TOO_MANY_REQUESTS,
          HttpURLConnection.HTTP_INTERNAL_ERROR,
          HttpURLConnection.HTTP_BAD_GATEWAY,
          HttpURLConnection.HTTP_UNAVAILABLE,
          HttpURLConnection.HTTP_GATEWAY_TIMEOUT);

  /** Pauses between attempts. Replaceable so unit tests do not really sleep. */
  @FunctionalInterface
  interface Sleeper {
    void sleep(long millis) throws InterruptedException;
  }

  private final int maxAttempts;
  private final long baseDelayMs;
  private final long maxDelayMs;
  private final Sleeper sleeper;
  private final LongUnaryOperator jitter;

  TransientRetry(
      int maxAttempts,
      long baseDelayMs,
      long maxDelayMs,
      Sleeper sleeper,
      LongUnaryOperator jitter) {
    if (maxAttempts < 1) {
      throw new IllegalArgumentException("maxAttempts must be at least 1");
    }
    this.maxAttempts = maxAttempts;
    this.baseDelayMs = baseDelayMs;
    this.maxDelayMs = maxDelayMs;
    this.sleeper = sleeper;
    this.jitter = jitter;
  }

  /** Three attempts in total, 1s base delay doubling up to an 8s cap, full jitter. */
  public static TransientRetry withDefaults() {
    return new TransientRetry(
        MAX_ATTEMPTS,
        BASE_DELAY_MS,
        MAX_DELAY_MS,
        Thread::sleep,
        ceiling -> ThreadLocalRandom.current().nextLong(ceiling + 1));
  }

  int maxAttempts() {
    return maxAttempts;
  }

  /**
   * Runs the action, retrying transient failures. When every attempt fails, the last failure is
   * thrown with the earlier ones attached as suppressed exceptions.
   */
  public <T> T call(Callable<T> action) throws Exception {
    Exception previous = null;
    for (var attempt = 1; ; attempt++) {
      try {
        return action.call();
      } catch (Exception e) {
        if (previous != null && !e.equals(previous)) {
          e.addSuppressed(previous);
        }
        if (attempt >= maxAttempts || !isTransient(e)) {
          throw e;
        }
        var delayMs = backoffMs(attempt);
        log.warn(
            "Transient failure on attempt {}/{}, retrying in {}ms: {}",
            attempt,
            maxAttempts,
            delayMs,
            e.toString());
        if (!pause(delayMs)) {
          throw e;
        }
        previous = e;
      }
    }
  }

  /** Upper bound of the wait before the retry that follows the given (1-based) failed attempt. */
  long delayCeilingMs(int attempt) {
    var ceiling = baseDelayMs;
    for (var i = 1; i < attempt && ceiling < maxDelayMs; i++) {
      ceiling *= 2;
    }
    return Math.min(ceiling, maxDelayMs);
  }

  private long backoffMs(int attempt) {
    return jitter.applyAsLong(delayCeilingMs(attempt));
  }

  private boolean pause(long delayMs) {
    try {
      sleeper.sleep(delayMs);
      return true;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  /** True when the failure, or anything in its cause chain, is a retryable transient error. */
  static boolean isTransient(Throwable failure) {
    var current = failure;
    for (var depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
      if (isTransientFailure(current)) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private static boolean isTransientFailure(Throwable failure) {
    if (failure instanceof HttpResponseException http) {
      return TRANSIENT_HTTP_STATUSES.contains(http.getCode());
    }
    return failure instanceof SocketException
        || failure instanceof SocketTimeoutException
        || failure instanceof UnknownHostException
        || failure instanceof SSLException
        || failure instanceof EOFException;
  }
}
