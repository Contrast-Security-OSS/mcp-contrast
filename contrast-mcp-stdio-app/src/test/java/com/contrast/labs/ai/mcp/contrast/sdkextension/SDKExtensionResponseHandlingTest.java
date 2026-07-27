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
package com.contrast.labs.ai.mcp.contrast.sdkextension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr.AttacksFilterBody;
import com.contrast.labs.ai.mcp.contrast.tool.validation.ValidationConstants;
import com.contrastsecurity.http.HttpMethod;
import com.contrastsecurity.http.LibraryFilterForm;
import com.contrastsecurity.sdk.ContrastSDK;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.ArgumentCaptor;
import org.slf4j.LoggerFactory;

/**
 * Covers how SDKExtension turns a TeamServer body into a return value: pagination, the debug
 * buffering branch and the null-response defaults. URL construction is covered by SDKExtensionTest.
 *
 * <p>SAME_THREAD on the outer class: ApplicationsDebugLogging attaches an appender whose backing
 * list is a plain ArrayList to the shared SDKExtension logger, and the sibling Applications class
 * drives the one method that logs. Annotating only the nested class leaves those two concurrent.
 */
@Execution(ExecutionMode.SAME_THREAD)
class SDKExtensionResponseHandlingTest {

  private ContrastSDK sdk;
  private SDKExtension sdkExtension;

  @BeforeEach
  void setUp() {
    sdk = mock();
    sdkExtension = new SDKExtension(sdk);
  }

  private static InputStream body(String json) {
    return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
  }

  /** Returns each body in turn so a paginating method sees a different page on every request. */
  private void respondInSequence(String... bodies) throws Exception {
    var remaining = new ArrayDeque<>(List.of(bodies));
    when(sdk.makeRequest(any(), any()))
        .thenAnswer(invocation -> body(remaining.isEmpty() ? "{}" : remaining.poll()));
  }

  private List<String> requestedUrls() throws Exception {
    var urls = new ArrayList<String>();
    var captor = ArgumentCaptor.forClass(String.class);
    verify(sdk, atLeastOnce()).makeRequest(eq(HttpMethod.GET), captor.capture());
    urls.addAll(captor.getAllValues());
    return urls;
  }

  @Test
  void getLibrariesWithFilter_should_parse_the_organization_wide_response() throws Exception {
    when(sdk.makeRequest(any(), any()))
        .thenReturn(
            body(
                "{\"count\":2,\"libraries\":[{\"file_name\":\"a.jar\"},{\"file_name\":\"b.jar\"}]}"));

    var result = sdkExtension.getLibrariesWithFilter("org-1", new LibraryFilterForm());

    assertThat(result.getCount()).isEqualTo(2L);
    assertThat(result.getLibraries()).extracting("fileName").containsExactly("a.jar", "b.jar");
  }

  @Test
  void getLibrariesWithFilter_should_scope_the_request_to_the_application_when_given_an_app_id()
      throws Exception {
    when(sdk.makeRequest(any(), any())).thenReturn(body("{\"count\":0,\"libraries\":[]}"));

    sdkExtension.getLibrariesWithFilter("org-1", "app-7", new LibraryFilterForm());

    verify(sdk).makeRequest(eq(HttpMethod.GET), argThat(url -> url.contains("app-7")));
  }

  @Test
  void getLibrariesWithFilter_should_not_scope_to_an_application_when_no_app_id_is_given()
      throws Exception {
    when(sdk.makeRequest(any(), any())).thenReturn(body("{\"count\":0,\"libraries\":[]}"));

    sdkExtension.getLibrariesWithFilter("org-1", new LibraryFilterForm());

    verify(sdk).makeRequest(eq(HttpMethod.GET), argThat(url -> !url.contains("/applications/")));
  }

  @Nested
  class LibraryObservations {

    @Test
    void getLibraryObservations_should_collect_every_page_in_order() throws Exception {
      respondInSequence(
          "{\"total\":3,\"observations\":[{\"name\":\"first\"},{\"name\":\"second\"}]}",
          "{\"total\":3,\"observations\":[{\"name\":\"third\"}]}");

      var result = sdkExtension.getLibraryObservations("org-1", "app-1", "lib-1", 2);

      assertThat(result).extracting("name").containsExactly("first", "second", "third");
    }

    @Test
    void getLibraryObservations_should_advance_the_offset_by_the_page_size_on_each_request()
        throws Exception {
      respondInSequence(
          "{\"total\":3,\"observations\":[{\"name\":\"first\"},{\"name\":\"second\"}]}",
          "{\"total\":3,\"observations\":[{\"name\":\"third\"}]}");

      sdkExtension.getLibraryObservations("org-1", "app-1", "lib-1", 2);

      assertThat(requestedUrls())
          .hasSize(2)
          .satisfies(urls -> assertThat(urls.get(0)).contains("offset=0"))
          .satisfies(urls -> assertThat(urls.get(1)).contains("offset=2"));
    }

    @Test
    void getLibraryObservations_should_stop_after_one_page_when_the_total_fits_in_it()
        throws Exception {
      respondInSequence("{\"total\":1,\"observations\":[{\"name\":\"only\"}]}");

      var result = sdkExtension.getLibraryObservations("org-1", "app-1", "lib-1", 25);

      assertThat(result).hasSize(1);
      assertThat(requestedUrls()).hasSize(1);
    }

    @Test
    void getLibraryObservations_should_skip_a_page_that_carries_no_observation_list()
        throws Exception {
      respondInSequence("{\"total\":2}", "{\"total\":2,\"observations\":[{\"name\":\"late\"}]}");

      var result = sdkExtension.getLibraryObservations("org-1", "app-1", "lib-1", 1);

      assertThat(result).extracting("name").containsExactly("late");
    }

    @Test
    void getLibraryObservations_should_fall_back_to_the_default_page_size_when_given_zero()
        throws Exception {
      respondInSequence("{\"total\":0,\"observations\":[]}");

      sdkExtension.getLibraryObservations("org-1", "app-1", "lib-1", 0);

      assertThat(requestedUrls().get(0))
          .contains("limit=" + ValidationConstants.DEFAULT_LIBRARY_OBS_PAGE_SIZE);
    }

    @Test
    void getLibraryObservations_should_fall_back_to_the_default_page_size_when_given_a_negative()
        throws Exception {
      respondInSequence("{\"total\":0,\"observations\":[]}");

      sdkExtension.getLibraryObservations("org-1", "app-1", "lib-1", -5);

      assertThat(requestedUrls().get(0))
          .contains("limit=" + ValidationConstants.DEFAULT_LIBRARY_OBS_PAGE_SIZE);
    }

    @Test
    void getLibraryObservations_should_use_the_default_page_size_when_none_is_supplied()
        throws Exception {
      respondInSequence("{\"total\":0,\"observations\":[]}");

      sdkExtension.getLibraryObservations("org-1", "app-1", "lib-1");

      assertThat(requestedUrls().get(0))
          .contains("limit=" + ValidationConstants.DEFAULT_LIBRARY_OBS_PAGE_SIZE);
    }
  }

  @Nested
  class Applications {

    @Test
    void getApplications_should_parse_the_streamed_response() throws Exception {
      when(sdk.makeRequest(any(), any()))
          .thenReturn(body("{\"applications\":[{\"name\":\"WebGoat\"},{\"name\":\"Petclinic\"}]}"));

      var result = sdkExtension.getApplications("org-1");

      assertThat(result.getApplications())
          .extracting("name")
          .containsExactly("WebGoat", "Petclinic");
    }

    @Test
    void getApplications_should_request_the_metadata_and_technologies_expansions()
        throws Exception {
      when(sdk.makeRequest(any(), any())).thenReturn(body("{\"applications\":[]}"));

      sdkExtension.getApplications("org-1");

      verify(sdk)
          .makeRequest(
              eq(HttpMethod.GET),
              argThat(url -> url.contains("expand=metadata,technologies,skip_links")));
    }
  }

  /**
   * The debug branch buffers the whole body so it can be logged.
   *
   * <p>SLF4J returns a SubstituteLogger while another thread initializes its backend, which cannot
   * be cast to Logback's Logger. Isolating only these tests keeps their logger setup outside that
   * parallel startup window without serializing the rest of this class against the whole suite.
   */
  @Nested
  @Isolated("Casts the SLF4J logger to Logback while configuring a shared appender")
  class ApplicationsDebugLogging {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private Level originalLevel;

    @BeforeEach
    void enableDebug() {
      logger = (Logger) LoggerFactory.getLogger(SDKExtension.class);
      originalLevel = logger.getLevel();
      appender = new ListAppender<>();
      appender.start();
      logger.addAppender(appender);
      logger.setLevel(Level.DEBUG);
    }

    @AfterEach
    void restoreLevel() {
      logger.setLevel(originalLevel);
      logger.detachAppender(appender);
      appender.stop();
    }

    @Test
    void getApplications_should_still_parse_the_response_when_debug_logging_is_on()
        throws Exception {
      when(sdk.makeRequest(any(), any()))
          .thenReturn(body("{\"applications\":[{\"name\":\"WebGoat\"}]}"));

      var result = sdkExtension.getApplications("org-1");

      assertThat(result.getApplications()).extracting("name").containsExactly("WebGoat");
    }

    @Test
    void getApplications_should_log_the_whole_response_body_when_debug_logging_is_on()
        throws Exception {
      when(sdk.makeRequest(any(), any()))
          .thenReturn(body("{\"applications\":[\n{\"name\":\"WebGoat\"}\n]}"));

      sdkExtension.getApplications("org-1");

      assertThat(appender.list)
          .extracting(ILoggingEvent::getFormattedMessage)
          .anySatisfy(
              message ->
                  assertThat(message)
                      .startsWith("Applications API response:")
                      .contains("\"applications\"")
                      .contains("WebGoat"));
    }

    @Test
    void getApplications_should_log_an_empty_body_rather_than_fail_when_the_stream_is_absent()
        throws Exception {
      when(sdk.makeRequest(any(), any())).thenReturn(null);

      var result = sdkExtension.getApplications("org-1");

      assertThat(result).isNull();
      assertThat(appender.list)
          .extracting(ILoggingEvent::getFormattedMessage)
          .contains("Applications API response: ");
    }
  }

  @Nested
  class MetadataFields {

    @Test
    void getApplicationMetadataFields_should_return_the_fields_from_the_response()
        throws Exception {
      when(sdk.makeRequest(any(), any()))
          .thenReturn(
              body(
                  "{\"fields\":[{\"fieldId\":1,\"displayLabel\":\"Team\"},"
                      + "{\"fieldId\":2,\"displayLabel\":\"Owner\"}]}"));

      var result = sdkExtension.getApplicationMetadataFields("org-1");

      assertThat(result)
          .extracting("fieldId", "displayLabel")
          .containsExactly(tuple(1L, "Team"), tuple(2L, "Owner"));
    }

    @Test
    void getApplicationMetadataFields_should_return_an_empty_list_when_the_response_has_no_fields()
        throws Exception {
      when(sdk.makeRequest(any(), any())).thenReturn(body("{}"));

      var result = sdkExtension.getApplicationMetadataFields("org-1");

      assertThat(result).isNotNull().isEmpty();
    }
  }

  @Nested
  class Attacks {

    @Test
    void getAttacks_should_return_an_empty_attack_list_when_the_body_parses_to_nothing()
        throws Exception {
      when(sdk.makeRequestWithBody(any(), any(), any(), any())).thenReturn(body("null"));

      var result =
          sdkExtension.getAttacks(
              "org-1", AttacksFilterBody.builder().build(), 10, 0, "-startTime");

      assertThat(result).isNotNull();
      assertThat(result.getAttacks()).isNotNull().isEmpty();
    }

    @Test
    void getAttacks_should_return_an_empty_attack_list_when_the_response_omits_attacks()
        throws Exception {
      when(sdk.makeRequestWithBody(any(), any(), any(), any())).thenReturn(body("{\"count\":0}"));

      var result =
          sdkExtension.getAttacks(
              "org-1", AttacksFilterBody.builder().build(), 10, 0, "-startTime");

      assertThat(result.getAttacks()).isNotNull().isEmpty();
      assertThat(result.getCount()).isZero();
    }

    @Test
    void getAttacks_should_keep_the_attacks_the_response_did_carry() throws Exception {
      when(sdk.makeRequestWithBody(any(), any(), any(), any()))
          .thenReturn(body("{\"count\":1,\"attacks\":[{\"uuid\":\"ATT-1\"}]}"));

      var result =
          sdkExtension.getAttacks(
              "org-1", AttacksFilterBody.builder().build(), 10, 0, "-startTime");

      assertThat(result.getAttacks()).extracting("uuid").containsExactly("ATT-1");
    }

    @Test
    void getAttacks_should_apply_the_default_limit_offset_and_sort_when_none_are_given()
        throws Exception {
      when(sdk.makeRequestWithBody(any(), any(), any(), any()))
          .thenReturn(body("{\"attacks\":[]}"));

      sdkExtension.getAttacks("org-1", AttacksFilterBody.builder().build());

      verify(sdk)
          .makeRequestWithBody(
              eq(HttpMethod.POST),
              argThat(
                  url ->
                      url.contains("limit=1000")
                          && url.contains("offset=0")
                          && url.contains("sort=-startTime")),
              any(),
              any());
    }
  }
}
