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

import com.contrastsecurity.http.TraceFilterForm;
import com.contrastsecurity.models.MetadataItem;
import com.contrastsecurity.models.SessionMetadata;
import com.contrastsecurity.models.Trace;
import com.contrastsecurity.models.Traces;
import com.contrastsecurity.sdk.ContrastSDK;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

/**
 * Helper class for session-based filtering of vulnerability traces.
 *
 * <p>Provides methods for:
 *
 * <ul>
 *   <li>Fetching traces with early termination optimization
 *   <li>Building predicates for session-based filtering
 *   <li>Handling multi-page fetches with safety limits
 * </ul>
 */
@Slf4j
public class SessionFilteringHelper {

  private static final int PAGE_SIZE = 500;
  private static final int MAX_PAGES = 100;

  private final int maxTracesForSessionFiltering;

  /**
   * Creates a new SessionFilteringHelper.
   *
   * @param maxTracesForSessionFiltering Maximum traces to fetch for session filtering to prevent
   *     memory exhaustion
   */
  public SessionFilteringHelper(int maxTracesForSessionFiltering) {
    this.maxTracesForSessionFiltering = maxTracesForSessionFiltering;
  }

  /**
   * Result of fetching traces for session filtering.
   *
   * @param traces The fetched traces (may be partial if error occurred)
   * @param wasTruncated True if results hit the max limit
   * @param hadFetchError True if an API error occurred during multi-page fetch
   * @param errorMessage Description of the fetch error (null if no error)
   */
  public record SessionFilteringResult(
      List<Trace> traces, boolean wasTruncated, boolean hadFetchError, String errorMessage) {

    /** Creates a successful result (no errors). */
    public static SessionFilteringResult success(List<Trace> traces, boolean wasTruncated) {
      return new SessionFilteringResult(traces, wasTruncated, false, null);
    }

    /** Creates a partial result due to fetch error. */
    public static SessionFilteringResult partial(List<Trace> traces, String errorMessage) {
      return new SessionFilteringResult(traces, false, true, errorMessage);
    }
  }

  /**
   * Fetches traces with early termination when enough filtered results are found. This optimizes
   * performance by avoiding unnecessary API calls when only a subset of matching traces are needed.
   *
   * <p>The method applies the filter predicate during fetching and stops as soon as the target
   * count of matching traces is reached. This reduces API calls from O(total/pageSize) to
   * O(targetCount/matchRate/pageSize) where matchRate is the percentage of traces matching the
   * filter.
   *
   * <p>Safety limits are enforced: MAX_PAGES (100) prevents runaway pagination, and
   * maxTracesForSessionFiltering prevents memory exhaustion if the filter matches everything.
   *
   * @param sdk The ContrastSDK instance
   * @param orgId The organization ID
   * @param appId The application ID
   * @param filterForm The filter form (will be modified to set limit/offset for pagination)
   * @param filterPredicate Predicate to filter traces; only matching traces are collected
   * @param targetCount Number of matching traces needed (typically offset + pageSize)
   * @return SessionFilteringResult containing filtered traces and status information
   * @throws IOException If an API error occurs
   */
  public SessionFilteringResult fetchTracesWithEarlyTermination(
      ContrastSDK sdk,
      String orgId,
      String appId,
      TraceFilterForm filterForm,
      Predicate<Trace> filterPredicate,
      int targetCount)
      throws IOException {
    // Pre-size for expected result set
    var matchingTraces = new ArrayList<Trace>(Math.min(targetCount, 1000));
    int offset = 0;
    int pagesChecked = 0;
    int totalTracesFetched = 0;

    while (matchingTraces.size() < targetCount && pagesChecked < MAX_PAGES) {
      // Also enforce memory limit on matching traces
      if (matchingTraces.size() >= maxTracesForSessionFiltering) {
        break;
      }

      filterForm.setLimit(PAGE_SIZE);
      filterForm.setOffset(offset);

      Traces pageResult;
      try {
        pageResult = sdk.getTraces(orgId, appId, filterForm);
      } catch (Exception e) {
        // Return partial results instead of discarding all fetched data
        String errorMsg =
            String.format(
                "API error during multi-page fetch at offset %d. Returning %d partial results."
                    + " Error: %s",
                offset, matchingTraces.size(), e.getMessage());
        log.warn(errorMsg, e);
        return SessionFilteringResult.partial(matchingTraces, errorMsg);
      }

      if (pageResult == null
          || pageResult.getTraces() == null
          || pageResult.getTraces().isEmpty()) {
        break;
      }

      pagesChecked++;
      totalTracesFetched += pageResult.getTraces().size();

      // Apply filter and collect matching traces with early termination
      for (Trace trace : pageResult.getTraces()) {
        if (filterPredicate.test(trace)) {
          matchingTraces.add(trace);
          if (matchingTraces.size() >= targetCount) {
            log.debug(
                "Early termination: found {} matching traces after checking {} pages ({} total"
                    + " traces)",
                matchingTraces.size(),
                pagesChecked,
                totalTracesFetched);
            return SessionFilteringResult.success(matchingTraces, false);
          }
          // Also check memory limit
          if (matchingTraces.size() >= maxTracesForSessionFiltering) {
            break;
          }
        }
      }

      log.debug(
          "Page {}: fetched {} traces, {} matching so far (target: {})",
          pagesChecked,
          pageResult.getTraces().size(),
          matchingTraces.size(),
          targetCount);

      // If we got fewer than PAGE_SIZE, we've reached the last page
      if (pageResult.getTraces().size() < PAGE_SIZE) {
        break;
      }

      offset += PAGE_SIZE;
    }

    // Check if we hit limits
    boolean wasTruncated = false;
    if (pagesChecked >= MAX_PAGES) {
      log.atWarn()
          .addKeyValue("appId", appId)
          .addKeyValue("pagesChecked", pagesChecked)
          .addKeyValue("matchingTraces", matchingTraces.size())
          .setMessage(
              "Reached MAX_PAGES limit during session filtering. Results may be incomplete.")
          .log();
      wasTruncated = true;
    } else if (matchingTraces.size() >= maxTracesForSessionFiltering) {
      log.atWarn()
          .addKeyValue("appId", appId)
          .addKeyValue("maxTraces", maxTracesForSessionFiltering)
          .setMessage("Reached maximum trace limit for session filtering. Results are incomplete.")
          .log();
      wasTruncated = true;
    }

    log.debug(
        "Session filtering complete: {} matching traces from {} total fetched ({} pages)",
        matchingTraces.size(),
        totalTracesFetched,
        pagesChecked);
    return SessionFilteringResult.success(matchingTraces, wasTruncated);
  }

  /**
   * Fetches all traces without filtering, up to the maximum limit. Used when no in-memory session
   * filtering is possible (e.g., no filter predicate can be built).
   *
   * @param sdk The ContrastSDK instance
   * @param orgId The organization ID
   * @param appId The application ID
   * @param filterForm The filter form (will be modified to set limit/offset for pagination)
   * @return SessionFilteringResult containing traces and truncation status
   * @throws IOException If an API error occurs
   */
  public SessionFilteringResult fetchAllTracesForSessionFiltering(
      ContrastSDK sdk, String orgId, String appId, TraceFilterForm filterForm) throws IOException {
    // Delegate to early termination method with an always-true predicate and max target
    return fetchTracesWithEarlyTermination(
        sdk, orgId, appId, filterForm, trace -> true, maxTracesForSessionFiltering);
  }

  /**
   * Builds a predicate for filtering traces based on session criteria. The predicate combines agent
   * session ID filtering and/or session metadata name/value filtering.
   *
   * @param agentSessionId If non-null, traces must have this agent session ID
   * @param sessionMetadataName If non-null, traces must have metadata with this display label
   * @param sessionMetadataValue If non-null (and sessionMetadataName is set), metadata must have
   *     this value
   * @return Predicate that returns true for traces matching ALL specified criteria
   */
  public Predicate<Trace> buildSessionFilterPredicate(
      String agentSessionId, String sessionMetadataName, String sessionMetadataValue) {

    Predicate<Trace> predicate = trace -> true;

    // Add agent session ID filter if specified
    if (agentSessionId != null) {
      final String sessionIdToMatch = agentSessionId;
      predicate =
          predicate.and(
              trace ->
                  trace.getSessionMetadata() != null
                      && trace.getSessionMetadata().stream()
                          .anyMatch(sm -> sessionIdToMatch.equals(sm.getSessionId())));
    }

    // Add session metadata name/value filter if specified
    if (StringUtils.hasText(sessionMetadataName)) {
      final String nameToMatch = sessionMetadataName;
      final String valueToMatch = sessionMetadataValue;
      predicate =
          predicate.and(
              trace -> {
                if (trace.getSessionMetadata() == null) {
                  return false;
                }
                for (SessionMetadata sm : trace.getSessionMetadata()) {
                  if (sm.getMetadata() == null) {
                    continue;
                  }
                  for (MetadataItem item : sm.getMetadata()) {
                    boolean nameMatches =
                        item.getDisplayLabel() != null
                            && item.getDisplayLabel().equalsIgnoreCase(nameToMatch);
                    boolean valueMatches =
                        valueToMatch == null
                            || (item.getValue() != null
                                && item.getValue().equalsIgnoreCase(valueToMatch));
                    if (nameMatches && valueMatches) {
                      return true;
                    }
                  }
                }
                return false;
              });
    }

    return predicate;
  }
}
