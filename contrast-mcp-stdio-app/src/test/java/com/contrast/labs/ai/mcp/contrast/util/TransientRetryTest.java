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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.contrastsecurity.exceptions.HttpResponseException;
import com.contrastsecurity.exceptions.ResourceNotFoundException;
import com.contrastsecurity.exceptions.UnauthorizedException;
import java.io.EOFException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import javax.net.ssl.SSLHandshakeException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TransientRetryTest {

  private static final int EXPECTED_DEFAULT_ATTEMPTS = 3;
  private static final long BASE_MS = 100L;
  private static final long MAX_MS = 500L;
  private static final long JITTERED_MS = 7L;

  private static final List<Integer> CODES =
      List.of(
          HttpURLConnection.HTTP_BAD_GATEWAY,
          HttpURLConnection.HTTP_UNAVAILABLE,
          HttpURLConnection.HTTP_GATEWAY_TIMEOUT);

  private final ArrayList<Long> sleeps = new ArrayList<>();

  private TransientRetry retry(int maxAttempts) {
    return new TransientRetry(maxAttempts, BASE_MS, MAX_MS, sleeps::add, ceiling -> JITTERED_MS);
  }

  private static HttpResponseException http(int code) {
    return new HttpResponseException("failed", "GET", "/ng/org/applications", code, "status");
  }

  @Test
  void withDefaults_should_allow_three_attempts_in_total() {
    assertThat(TransientRetry.withDefaults().maxAttempts()).isEqualTo(EXPECTED_DEFAULT_ATTEMPTS);
  }

  @Test
  void call_should_return_result_without_sleeping_when_first_attempt_succeeds() throws Exception {
    var result = retry(EXPECTED_DEFAULT_ATTEMPTS).call(() -> "ok");

    assertThat(result).isEqualTo("ok");
    assertThat(sleeps).isEmpty();
  }

  @Test
  void call_should_retry_and_return_result_when_transient_failure_then_success() throws Exception {
    var attempts = new AtomicInteger();

    var result =
        retry(EXPECTED_DEFAULT_ATTEMPTS)
            .call(
                () -> {
                  if (attempts.incrementAndGet() == 1) {
                    throw new SocketTimeoutException("Read timed out");
                  }
                  return "recovered";
                });

    assertThat(result).isEqualTo("recovered");
    assertThat(attempts).hasValue(2);
    assertThat(sleeps).containsExactly(JITTERED_MS);
  }

  @Test
  void call_should_stop_after_max_attempts_and_attach_earlier_failures_as_suppressed() {
    var attempts = new AtomicInteger();

    assertThatThrownBy(
            () ->
                retry(EXPECTED_DEFAULT_ATTEMPTS)
                    .call(
                        () -> {
                          throw http(CODES.get(attempts.getAndIncrement()));
                        }))
        .isInstanceOfSatisfying(
            HttpResponseException.class,
            last -> {
              assertThat(last.getCode()).isEqualTo(HttpURLConnection.HTTP_GATEWAY_TIMEOUT);
              assertThat(last.getSuppressed()).hasSize(1);
            });

    assertThat(attempts).hasValue(EXPECTED_DEFAULT_ATTEMPTS);
    assertThat(sleeps).hasSize(EXPECTED_DEFAULT_ATTEMPTS - 1);
  }

  @ParameterizedTest
  @ValueSource(ints = {429, 500, 502, 503, 504})
  void call_should_retry_when_http_status_is_transient(int status) throws Exception {
    var attempts = new AtomicInteger();

    var result =
        retry(EXPECTED_DEFAULT_ATTEMPTS)
            .call(
                () -> {
                  if (attempts.incrementAndGet() == 1) {
                    throw http(status);
                  }
                  return "recovered";
                });

    assertThat(result).isEqualTo("recovered");
    assertThat(attempts).hasValue(2);
  }

  @ParameterizedTest
  @ValueSource(ints = {400, 401, 403, 404, 409, 501})
  void call_should_not_retry_when_http_status_is_not_transient(int status) {
    var attempts = new AtomicInteger();

    assertThatThrownBy(
            () ->
                retry(EXPECTED_DEFAULT_ATTEMPTS)
                    .call(
                        () -> {
                          attempts.incrementAndGet();
                          throw http(status);
                        }))
        .isInstanceOf(HttpResponseException.class);

    assertThat(attempts).hasValue(1);
    assertThat(sleeps).isEmpty();
  }

  @Test
  void isTransient_should_reject_sdk_unauthorized_and_not_found_exceptions() {
    assertThat(
            TransientRetry.isTransient(
                new UnauthorizedException(
                    "no", "GET", "/x", HttpURLConnection.HTTP_FORBIDDEN, "Forbidden")))
        .isFalse();
    assertThat(
            TransientRetry.isTransient(
                new ResourceNotFoundException("no", "GET", "/x", "Not Found")))
        .isFalse();
  }

  @Test
  void call_should_not_retry_when_failure_is_missing_data_or_a_plain_bug() {
    var attempts = new AtomicInteger();

    assertThatThrownBy(
            () ->
                retry(EXPECTED_DEFAULT_ATTEMPTS)
                    .call(
                        () -> {
                          attempts.incrementAndGet();
                          throw new AbstractIntegrationTest.NoTestDataException("no apps");
                        }))
        .isInstanceOf(AbstractIntegrationTest.NoTestDataException.class);
    assertThatThrownBy(
            () ->
                retry(EXPECTED_DEFAULT_ATTEMPTS)
                    .call(
                        () -> {
                          attempts.incrementAndGet();
                          throw new IllegalStateException("bug");
                        }))
        .isInstanceOf(IllegalStateException.class);

    assertThat(attempts).hasValue(2);
    assertThat(sleeps).isEmpty();
  }

  @Test
  void isTransient_should_accept_connectivity_errors_and_reject_other_io_errors() {
    assertThat(TransientRetry.isTransient(new ConnectException("refused"))).isTrue();
    assertThat(TransientRetry.isTransient(new SocketTimeoutException("timeout"))).isTrue();
    assertThat(TransientRetry.isTransient(new UnknownHostException("host"))).isTrue();
    assertThat(TransientRetry.isTransient(new SSLHandshakeException("handshake"))).isTrue();
    assertThat(TransientRetry.isTransient(new EOFException("eof"))).isTrue();
    assertThat(TransientRetry.isTransient(new IOException("disk full"))).isFalse();
  }

  @Test
  void isTransient_should_find_a_transient_cause_wrapped_in_another_exception() {
    var wrapped = new IllegalStateException("discovery failed", new SocketTimeoutException("t"));

    assertThat(TransientRetry.isTransient(wrapped)).isTrue();
  }

  @Test
  void call_should_stop_and_keep_interrupt_flag_when_sleep_is_interrupted() {
    var attempts = new AtomicInteger();
    var interrupted =
        new TransientRetry(
            EXPECTED_DEFAULT_ATTEMPTS,
            BASE_MS,
            MAX_MS,
            ms -> {
              throw new InterruptedException("stop");
            },
            ceiling -> 0L);

    try {
      assertThatThrownBy(
              () ->
                  interrupted.call(
                      () -> {
                        attempts.incrementAndGet();
                        throw new SocketTimeoutException("t");
                      }))
          .isInstanceOf(SocketTimeoutException.class);

      assertThat(attempts).hasValue(1);
      assertThat(Thread.currentThread().isInterrupted()).isTrue();
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void delayCeilingMs_should_double_each_attempt_and_stop_at_the_cap() {
    var retry = retry(EXPECTED_DEFAULT_ATTEMPTS);

    assertThat(retry.delayCeilingMs(1)).isEqualTo(BASE_MS);
    assertThat(retry.delayCeilingMs(2)).isEqualTo(BASE_MS * 2);
    assertThat(retry.delayCeilingMs(3)).isEqualTo(BASE_MS * 4);
    assertThat(retry.delayCeilingMs(4)).isEqualTo(MAX_MS);
    assertThat(retry.delayCeilingMs(50)).isEqualTo(MAX_MS);
  }

  @Test
  void call_should_pass_the_jittered_delay_to_the_sleeper() throws Exception {
    var attempts = new AtomicInteger();
    var ceilings = new ArrayList<Long>();
    var retry =
        new TransientRetry(
            EXPECTED_DEFAULT_ATTEMPTS,
            BASE_MS,
            MAX_MS,
            sleeps::add,
            ceiling -> {
              ceilings.add(ceiling);
              return ceiling / 2;
            });

    retry.call(
        () -> {
          if (attempts.incrementAndGet() < EXPECTED_DEFAULT_ATTEMPTS) {
            throw new SocketTimeoutException("t");
          }
          return "ok";
        });

    assertThat(ceilings).containsExactly(BASE_MS, BASE_MS * 2);
    assertThat(sleeps).containsExactly(BASE_MS / 2, BASE_MS);
  }

  @Test
  void constructor_should_reject_zero_attempts() {
    assertThatThrownBy(() -> new TransientRetry(0, BASE_MS, MAX_MS, ms -> {}, c -> 0L))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
