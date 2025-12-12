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
package com.contrast.labs.ai.mcp.contrast.helpers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contrastsecurity.http.TraceFilterForm;
import com.contrastsecurity.models.MetadataItem;
import com.contrastsecurity.models.SessionMetadata;
import com.contrastsecurity.models.Trace;
import com.contrastsecurity.models.Traces;
import com.contrastsecurity.sdk.ContrastSDK;
import java.io.IOException;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tests for SessionFilteringHelper. */
@ExtendWith(MockitoExtension.class)
class SessionFilteringHelperTest {

  private static final int MAX_TRACES = 50_000;
  private static final String ORG_ID = "org-123";
  private static final String APP_ID = "app-456";

  private SessionFilteringHelper helper;

  @Mock private ContrastSDK sdk;

  @BeforeEach
  void setUp() {
    helper = new SessionFilteringHelper(MAX_TRACES);
  }

  // ========== fetchTracesWithEarlyTermination Tests ==========

  @Nested
  class FetchTracesWithEarlyTermination {

    @Test
    void should_terminate_early_when_target_reached() throws IOException {
      // Given: SDK returns 100 traces per page
      var tracePage1 = createTracesResponse(100);
      when(sdk.getTraces(anyString(), anyString(), any(TraceFilterForm.class)))
          .thenReturn(tracePage1);

      var filterForm = new TraceFilterForm();
      Predicate<Trace> matchAll = trace -> true;

      // When: requesting only 50 traces
      var result =
          helper.fetchTracesWithEarlyTermination(sdk, ORG_ID, APP_ID, filterForm, matchAll, 50);

      // Then: should return 50 traces and terminate early
      assertThat(result.traces()).hasSize(50);
      assertThat(result.wasTruncated()).isFalse();
      assertThat(result.hadFetchError()).isFalse();
      // Only one API call needed since first page has enough traces
      verify(sdk, times(1)).getTraces(anyString(), anyString(), any(TraceFilterForm.class));
    }

    @Test
    void should_continue_fetching_until_all_pages_exhausted() throws IOException {
      // Given: SDK returns fewer than PAGE_SIZE (500) traces, indicating last page
      var lastPage = createTracesResponse(50);
      when(sdk.getTraces(anyString(), anyString(), any(TraceFilterForm.class)))
          .thenReturn(lastPage);

      var filterForm = new TraceFilterForm();
      Predicate<Trace> matchAll = trace -> true;

      // When: requesting 100 traces but only 50 exist
      var result =
          helper.fetchTracesWithEarlyTermination(sdk, ORG_ID, APP_ID, filterForm, matchAll, 100);

      // Then: should return all 50 available traces (stopped because < PAGE_SIZE)
      assertThat(result.traces()).hasSize(50);
      assertThat(result.wasTruncated()).isFalse();
      assertThat(result.hadFetchError()).isFalse();
    }

    @Test
    void should_return_partial_result_on_api_error() throws IOException {
      // Given: SDK returns 500 traces (full page) on first page, then throws error on second
      // Must return full PAGE_SIZE to trigger next page fetch
      var tracePage1 = createTracesResponse(500);
      when(sdk.getTraces(anyString(), anyString(), any(TraceFilterForm.class)))
          .thenReturn(tracePage1)
          .thenThrow(new RuntimeException("API connection failed"));

      var filterForm = new TraceFilterForm();
      Predicate<Trace> matchAll = trace -> true;

      // When: fetching with target of 1000 traces (need to fetch multiple pages)
      var result =
          helper.fetchTracesWithEarlyTermination(sdk, ORG_ID, APP_ID, filterForm, matchAll, 1000);

      // Then: should return partial results with error flag
      assertThat(result.traces()).hasSize(500);
      assertThat(result.wasTruncated()).isFalse();
      assertThat(result.hadFetchError()).isTrue();
      assertThat(result.errorMessage()).contains("API error");
      assertThat(result.errorMessage()).contains("500 partial results");
    }

    @Test
    void should_apply_filter_predicate() throws IOException {
      // Given: SDK returns traces, but only some match the filter
      var traces = createTracesResponse(100);
      when(sdk.getTraces(anyString(), anyString(), any(TraceFilterForm.class))).thenReturn(traces);

      var filterForm = new TraceFilterForm();
      // Filter that only accepts every other trace
      int[] counter = {0};
      Predicate<Trace> filterHalf = trace -> (counter[0]++ % 2) == 0;

      // When: fetching with selective filter and target of 25
      var result =
          helper.fetchTracesWithEarlyTermination(sdk, ORG_ID, APP_ID, filterForm, filterHalf, 25);

      // Then: should return 25 filtered traces
      assertThat(result.traces()).hasSize(25);
      assertThat(result.wasTruncated()).isFalse();
    }

    @Test
    void should_return_empty_when_no_traces() throws IOException {
      // Given: SDK returns no traces
      when(sdk.getTraces(anyString(), anyString(), any(TraceFilterForm.class))).thenReturn(null);

      var filterForm = new TraceFilterForm();

      // When
      var result =
          helper.fetchTracesWithEarlyTermination(sdk, ORG_ID, APP_ID, filterForm, t -> true, 10);

      // Then
      assertThat(result.traces()).isEmpty();
      assertThat(result.wasTruncated()).isFalse();
      assertThat(result.hadFetchError()).isFalse();
    }

    @Test
    void should_handle_null_traces_in_response() throws IOException {
      // Given: SDK returns Traces object with null list
      Traces traces = mock(Traces.class);
      when(traces.getTraces()).thenReturn(null);
      when(sdk.getTraces(anyString(), anyString(), any(TraceFilterForm.class))).thenReturn(traces);

      var filterForm = new TraceFilterForm();

      // When
      var result =
          helper.fetchTracesWithEarlyTermination(sdk, ORG_ID, APP_ID, filterForm, t -> true, 10);

      // Then
      assertThat(result.traces()).isEmpty();
      assertThat(result.wasTruncated()).isFalse();
    }

    private Traces createTracesResponse(int count) {
      Traces traces = mock(Traces.class);
      var traceList = new java.util.ArrayList<Trace>();
      for (int i = 0; i < count; i++) {
        Trace trace = mock(Trace.class);
        lenient().when(trace.getUuid()).thenReturn("trace-" + i);
        traceList.add(trace);
      }
      when(traces.getTraces()).thenReturn(traceList);
      return traces;
    }
  }

  // ========== fetchAllTracesForSessionFiltering Tests ==========

  @Nested
  class FetchAllTracesForSessionFiltering {

    @Test
    void should_delegate_to_fetchTracesWithEarlyTermination() throws IOException {
      // Given
      var traces = mock(Traces.class);
      when(traces.getTraces()).thenReturn(List.of(mock(Trace.class)));
      when(sdk.getTraces(anyString(), anyString(), any(TraceFilterForm.class))).thenReturn(traces);

      var filterForm = new TraceFilterForm();

      // When
      var result = helper.fetchAllTracesForSessionFiltering(sdk, ORG_ID, APP_ID, filterForm);

      // Then
      assertThat(result.traces()).hasSize(1);
    }
  }

  // ========== buildSessionFilterPredicate Tests ==========

  @Nested
  class BuildSessionFilterPredicate {

    @Test
    void should_return_always_true_when_no_filters() {
      // When: no filter criteria specified
      var predicate = helper.buildSessionFilterPredicate(null, null, null);

      // Then: should match any trace
      Trace trace = mock(Trace.class);
      assertThat(predicate.test(trace)).isTrue();
    }

    @Test
    void should_filter_by_agentSessionId() {
      // Given
      var predicate = helper.buildSessionFilterPredicate("session-123", null, null);

      // When: trace with matching session ID
      Trace matchingTrace = createTraceWithSessionId("session-123");
      Trace nonMatchingTrace = createTraceWithSessionId("other-session");
      Trace noSessionTrace = mock(Trace.class);
      when(noSessionTrace.getSessionMetadata()).thenReturn(null);

      // Then
      assertThat(predicate.test(matchingTrace)).isTrue();
      assertThat(predicate.test(nonMatchingTrace)).isFalse();
      assertThat(predicate.test(noSessionTrace)).isFalse();
    }

    @Test
    void should_filter_by_metadata_name_case_insensitive() {
      // Given
      var predicate = helper.buildSessionFilterPredicate(null, "UserID", null);

      // When: trace with metadata having different case
      Trace matchingTrace = createTraceWithMetadata("userId", "user-456");
      Trace nonMatchingTrace = createTraceWithMetadata("OtherField", "value");
      Trace noMetadataTrace = mock(Trace.class);
      when(noMetadataTrace.getSessionMetadata()).thenReturn(null);

      // Then
      assertThat(predicate.test(matchingTrace)).isTrue();
      assertThat(predicate.test(nonMatchingTrace)).isFalse();
      assertThat(predicate.test(noMetadataTrace)).isFalse();
    }

    @Test
    void should_filter_by_metadata_name_and_value() {
      // Given
      var predicate = helper.buildSessionFilterPredicate(null, "UserID", "user-789");

      // When
      Trace matchingTrace = createTraceWithMetadata("userId", "user-789");
      Trace wrongValueTrace = createTraceWithMetadata("userId", "user-000");
      Trace wrongNameTrace = createTraceWithMetadata("OtherField", "user-789");

      // Then
      assertThat(predicate.test(matchingTrace)).isTrue();
      assertThat(predicate.test(wrongValueTrace)).isFalse();
      assertThat(predicate.test(wrongNameTrace)).isFalse();
    }

    @Test
    void should_combine_filters_with_AND_logic() {
      // Given: both agentSessionId AND metadata filter specified
      var predicate = helper.buildSessionFilterPredicate("session-123", "UserID", "user-456");

      // Create trace that matches session but not metadata
      Trace sessionOnlyTrace =
          createTraceWithSessionIdAndMetadata("session-123", "OtherField", "value");
      // Create trace that matches metadata but not session
      Trace metadataOnlyTrace =
          createTraceWithSessionIdAndMetadata("other-session", "UserID", "user-456");
      // Create trace that matches both
      Trace bothMatchTrace =
          createTraceWithSessionIdAndMetadata("session-123", "UserID", "user-456");

      // Then: only trace matching both criteria should pass
      assertThat(predicate.test(sessionOnlyTrace)).isFalse();
      assertThat(predicate.test(metadataOnlyTrace)).isFalse();
      assertThat(predicate.test(bothMatchTrace)).isTrue();
    }

    @Test
    void should_handle_empty_metadata_name_as_no_filter() {
      // Given: empty string for metadata name
      var predicate = helper.buildSessionFilterPredicate(null, "", null);

      // Then: should match any trace (empty string treated as no filter)
      Trace trace = mock(Trace.class);
      assertThat(predicate.test(trace)).isTrue();
    }

    @Test
    void should_handle_whitespace_metadata_name_as_no_filter() {
      // Given: whitespace-only metadata name
      var predicate = helper.buildSessionFilterPredicate(null, "   ", null);

      // Then: should match any trace
      Trace trace = mock(Trace.class);
      assertThat(predicate.test(trace)).isTrue();
    }

    private Trace createTraceWithSessionId(String sessionId) {
      Trace trace = mock(Trace.class);
      SessionMetadata sm = mock(SessionMetadata.class);
      when(sm.getSessionId()).thenReturn(sessionId);
      when(trace.getSessionMetadata()).thenReturn(List.of(sm));
      return trace;
    }

    private Trace createTraceWithMetadata(String displayLabel, String value) {
      Trace trace = mock(Trace.class);
      SessionMetadata sm = mock(SessionMetadata.class);
      MetadataItem item = mock(MetadataItem.class);
      // Use lenient() because the predicate may short-circuit and not access all fields
      lenient().when(item.getDisplayLabel()).thenReturn(displayLabel);
      lenient().when(item.getValue()).thenReturn(value);
      lenient().when(sm.getMetadata()).thenReturn(List.of(item));
      lenient().when(trace.getSessionMetadata()).thenReturn(List.of(sm));
      return trace;
    }

    private Trace createTraceWithSessionIdAndMetadata(
        String sessionId, String displayLabel, String value) {
      Trace trace = mock(Trace.class);
      SessionMetadata sm = mock(SessionMetadata.class);
      MetadataItem item = mock(MetadataItem.class);
      // Use lenient() because the predicate may short-circuit and not access all fields
      lenient().when(sm.getSessionId()).thenReturn(sessionId);
      lenient().when(item.getDisplayLabel()).thenReturn(displayLabel);
      lenient().when(item.getValue()).thenReturn(value);
      lenient().when(sm.getMetadata()).thenReturn(List.of(item));
      lenient().when(trace.getSessionMetadata()).thenReturn(List.of(sm));
      return trace;
    }
  }

  // ========== SessionFilteringResult Tests ==========

  @Nested
  class SessionFilteringResultTest {

    @Test
    void success_should_create_result_without_error() {
      var traces = List.of(mock(Trace.class));
      var result = SessionFilteringHelper.SessionFilteringResult.success(traces, false);

      assertThat(result.traces()).isEqualTo(traces);
      assertThat(result.wasTruncated()).isFalse();
      assertThat(result.hadFetchError()).isFalse();
      assertThat(result.errorMessage()).isNull();
    }

    @Test
    void success_should_indicate_truncation() {
      var traces = List.of(mock(Trace.class));
      var result = SessionFilteringHelper.SessionFilteringResult.success(traces, true);

      assertThat(result.wasTruncated()).isTrue();
      assertThat(result.hadFetchError()).isFalse();
    }

    @Test
    void partial_should_create_result_with_error() {
      var traces = List.of(mock(Trace.class));
      var result = SessionFilteringHelper.SessionFilteringResult.partial(traces, "Error occurred");

      assertThat(result.traces()).isEqualTo(traces);
      assertThat(result.wasTruncated()).isFalse();
      assertThat(result.hadFetchError()).isTrue();
      assertThat(result.errorMessage()).isEqualTo("Error occurred");
    }
  }
}
