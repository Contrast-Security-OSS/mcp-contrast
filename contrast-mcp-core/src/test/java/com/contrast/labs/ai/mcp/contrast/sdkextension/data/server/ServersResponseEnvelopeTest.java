package com.contrast.labs.ai.mcp.contrast.sdkextension.data.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ServersResponseEnvelopeTest {

  private static final String MALFORMED = "Malformed server response envelope";
  private static final String UNSUCCESSFUL = "Unsuccessful server response envelope";

  @Test
  void validateAndNormalize_should_reject_a_null_response() {
    assertThatThrownBy(() -> ServersResponseEnvelope.validateAndNormalize(null, tagFilter()))
        .isInstanceOf(IOException.class)
        .hasMessage(MALFORMED);
  }

  @Test
  void validateAndNormalize_should_reject_a_response_with_no_server_list() {
    var response = response(true, null, 0L, null);

    assertThatThrownBy(() -> ServersResponseEnvelope.validateAndNormalize(response, tagFilter()))
        .isInstanceOf(IOException.class)
        .hasMessage(MALFORMED);
  }

  @Test
  void validateAndNormalize_should_reject_a_response_with_no_count() {
    var response = response(true, List.of(), null, null);

    assertThatThrownBy(() -> ServersResponseEnvelope.validateAndNormalize(response, tagFilter()))
        .isInstanceOf(IOException.class)
        .hasMessage(MALFORMED);
  }

  /**
   * Shape is checked before the success flag, so a successful but malformed envelope still fails.
   */
  @Test
  void validateAndNormalize_should_reject_a_malformed_envelope_even_when_it_reports_success() {
    var response = response(true, null, 5L, null);

    assertThatThrownBy(() -> ServersResponseEnvelope.validateAndNormalize(response, tagFilter()))
        .isInstanceOf(IOException.class)
        .hasMessage(MALFORMED);
  }

  @Test
  void validateAndNormalize_should_accept_a_successful_response_untouched() throws IOException {
    var response = response(true, List.of(new ServerDetail()), 1L, null);

    ServersResponseEnvelope.validateAndNormalize(response, tagFilter());

    assertThat(response.isSuccess()).isTrue();
    assertThat(response.getServers()).hasSize(1);
  }

  @Test
  void validateAndNormalize_should_flip_a_zero_match_tag_filter_response_to_success()
      throws IOException {
    var response = response(false, List.of(), 0L, null);

    ServersResponseEnvelope.validateAndNormalize(response, tagFilter());

    assertThat(response.isSuccess()).isTrue();
    assertThat(response.getServers()).isEmpty();
  }

  @Test
  void validateAndNormalize_should_treat_an_empty_message_list_the_same_as_no_messages() {
    var response = response(false, List.of(), 0L, List.of());

    assertThatCode(() -> ServersResponseEnvelope.validateAndNormalize(response, tagFilter()))
        .doesNotThrowAnyException();
    assertThat(response.isSuccess()).isTrue();
  }

  @Test
  void validateAndNormalize_should_keep_a_failure_that_carries_error_messages() {
    var response = response(false, List.of(), 0L, List.of("Invalid tag"));

    assertThatThrownBy(() -> ServersResponseEnvelope.validateAndNormalize(response, tagFilter()))
        .isInstanceOf(IOException.class)
        .hasMessage(UNSUCCESSFUL);
    assertThat(response.isSuccess()).isFalse();
  }

  @Test
  void validateAndNormalize_should_keep_a_failure_when_the_count_disagrees_with_the_empty_list() {
    var response = response(false, List.of(), 7L, null);

    assertThatThrownBy(() -> ServersResponseEnvelope.validateAndNormalize(response, tagFilter()))
        .isInstanceOf(IOException.class)
        .hasMessage(UNSUCCESSFUL);
  }

  @Test
  void validateAndNormalize_should_keep_a_failure_that_still_returned_servers() {
    var response = response(false, List.of(new ServerDetail()), 0L, null);

    assertThatThrownBy(() -> ServersResponseEnvelope.validateAndNormalize(response, tagFilter()))
        .isInstanceOf(IOException.class)
        .hasMessage(UNSUCCESSFUL);
  }

  @Test
  void validateAndNormalize_should_keep_a_failure_when_no_filter_was_sent() {
    var response = response(false, List.of(), 0L, null);

    assertThatThrownBy(() -> ServersResponseEnvelope.validateAndNormalize(response, null))
        .isInstanceOf(IOException.class)
        .hasMessage(UNSUCCESSFUL);
  }

  @Test
  void validateAndNormalize_should_keep_a_failure_when_the_filter_carried_no_tags() {
    var response = response(false, List.of(), 0L, null);
    var filter = ServerFilterBody.builder().q("web").build();

    assertThatThrownBy(() -> ServersResponseEnvelope.validateAndNormalize(response, filter))
        .isInstanceOf(IOException.class)
        .hasMessage(UNSUCCESSFUL);
  }

  @Test
  void validateAndNormalize_should_keep_a_failure_when_the_tag_list_was_empty() {
    var response = response(false, List.of(), 0L, null);
    var filter = ServerFilterBody.builder().tags(List.of()).build();

    assertThatThrownBy(() -> ServersResponseEnvelope.validateAndNormalize(response, filter))
        .isInstanceOf(IOException.class)
        .hasMessage(UNSUCCESSFUL);
  }

  private static ServerFilterBody tagFilter() {
    return ServerFilterBody.builder().tags(List.of("production")).build();
  }

  private static ServersResponse response(
      boolean success, List<ServerDetail> servers, Long count, List<String> messages) {
    var response = new ServersResponse();
    response.setSuccess(success);
    response.setServers(servers);
    response.setCount(count);
    response.setMessages(messages);
    return response;
  }
}
