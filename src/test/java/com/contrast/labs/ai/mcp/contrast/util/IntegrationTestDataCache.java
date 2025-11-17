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
package com.contrast.labs.ai.mcp.contrast.util;

import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.ProtectData;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Application;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.ApplicationsResponse;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.RouteCoverageResponse;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata.SessionMetadataResponse;
import com.contrastsecurity.exceptions.UnauthorizedException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Cache for expensive SDK lookups used across integration tests.
 *
 * <p>The integration tests frequently fetch the same Contrast data (applications list, libraries,
 * Protect configuration, etc.). Without caching each test class would make redundant API calls,
 * dramatically increasing end-to-end execution time. This cache keeps immutable copies of those
 * responses so the first caller pays the cost and subsequent callers get constant-time lookups.
 *
 * <p>The cache is intentionally simple and process-scoped - tests run against live infrastructure
 * so the data set is relatively small and does not require sophisticated eviction policies. Cached
 * values are safe to share across tests because the SDK objects are treated as read-only.
 */
public final class IntegrationTestDataCache {

  private static final Logger logger = LoggerFactory.getLogger(IntegrationTestDataCache.class);

  private static final ConcurrentMap<String, List<Application>> APPLICATIONS =
      new ConcurrentHashMap<>();
  private static final ConcurrentMap<LibraryKey, List<LibraryExtended>> APPLICATION_LIBRARIES =
      new ConcurrentHashMap<>();
  private static final ConcurrentMap<ProtectKey, Optional<ProtectData>> PROTECT_DATA =
      new ConcurrentHashMap<>();
  private static final ConcurrentMap<RouteCoverageKey, Optional<RouteCoverageResponse>>
      ROUTE_COVERAGE = new ConcurrentHashMap<>();
  private static final ConcurrentMap<SessionMetadataKey, Optional<SessionMetadataResponse>>
      SESSION_METADATA = new ConcurrentHashMap<>();

  private IntegrationTestDataCache() {}

  /**
   * Returns a cached list of applications for the given organization.
   *
   * @param orgId Organization identifier
   * @param sdkExtension Shared SDK extension
   * @return List of applications (never {@code null})
   * @throws IOException if the applications cannot be retrieved
   */
  public static List<Application> getApplications(String orgId, SDKExtension sdkExtension)
      throws IOException {
    var cached = APPLICATIONS.get(orgId);
    if (cached != null) {
      logger.debug("Returning cached applications for org {}", orgId);
      return cached;
    }

    logger.info("Fetching applications for org {} (not cached yet)", orgId);
    try {
      ApplicationsResponse response = sdkExtension.getApplications(orgId);
      List<Application> applications =
          response.getApplications() == null
              ? Collections.emptyList()
              : List.copyOf(response.getApplications());

      var previous = APPLICATIONS.putIfAbsent(orgId, applications);
      return previous != null ? previous : applications;
    } catch (UnauthorizedException e) {
      throw new IOException("Unable to fetch applications for org " + orgId, e);
    }
  }

  /**
   * Returns cached libraries for a given application, fetching them on-demand when missing.
   *
   * @param orgId Organization identifier
   * @param appId Application identifier
   * @param sdkExtension Shared SDK extension
   * @return Immutable list of libraries (never {@code null})
   * @throws IOException if the libraries cannot be retrieved
   */
  public static List<LibraryExtended> getLibraries(
      String orgId, String appId, SDKExtension sdkExtension) throws IOException {
    var key = new LibraryKey(orgId, appId);
    var cached = APPLICATION_LIBRARIES.get(key);
    if (cached != null) {
      logger.debug("Returning cached libraries for org {} app {}", orgId, appId);
      return cached;
    }

    logger.info("Fetching libraries for org {} app {} (not cached yet)", orgId, appId);
    try {
      var libraries = SDKHelper.getLibsForID(appId, orgId, sdkExtension);
      List<LibraryExtended> immutableLibraries =
          libraries == null ? Collections.emptyList() : List.copyOf(libraries);
      var previous = APPLICATION_LIBRARIES.putIfAbsent(key, immutableLibraries);
      return previous != null ? previous : immutableLibraries;
    } catch (UnauthorizedException e) {
      throw new IOException(
          String.format("Unable to fetch libraries for org %s app %s", orgId, appId), e);
    }
  }

  /**
   * Returns cached Protect configuration for a given application.
   *
   * @param orgId Organization identifier
   * @param appId Application identifier
   * @param sdkExtension Shared SDK extension
   * @return Optional containing Protect configuration
   * @throws IOException if the configuration cannot be retrieved
   */
  public static Optional<ProtectData> getProtectConfig(
      String orgId, String appId, SDKExtension sdkExtension) throws IOException {
    var key = new ProtectKey(orgId, appId);
    var cached = PROTECT_DATA.get(key);
    if (cached != null) {
      logger.debug("Returning cached Protect data for org {} app {}", orgId, appId);
      return cached;
    }

    logger.info("Fetching Protect data for org {} app {} (not cached yet)", orgId, appId);
    try {
      var protectData = Optional.ofNullable(sdkExtension.getProtectConfig(orgId, appId));
      var previous = PROTECT_DATA.putIfAbsent(key, protectData);
      return previous != null ? previous : protectData;
    } catch (UnauthorizedException e) {
      throw new IOException(
          String.format("Unable to fetch Protect config for org %s app %s", orgId, appId), e);
    }
  }

  /**
   * Returns cached route coverage data for a given application.
   *
   * @param orgId Organization identifier
   * @param appId Application identifier
   * @param sdkExtension Shared SDK extension
   * @return Optional containing route coverage response
   * @throws IOException if the data cannot be retrieved
   */
  public static Optional<RouteCoverageResponse> getRouteCoverage(
      String orgId, String appId, SDKExtension sdkExtension) throws IOException {
    var key = new RouteCoverageKey(orgId, appId);
    var cached = ROUTE_COVERAGE.get(key);
    if (cached != null) {
      logger.debug("Returning cached route coverage for org {} app {}", orgId, appId);
      return cached;
    }

    logger.info("Fetching route coverage for org {} app {} (not cached yet)", orgId, appId);
    try {
      var response = Optional.ofNullable(sdkExtension.getRouteCoverage(orgId, appId, null));
      var previous = ROUTE_COVERAGE.putIfAbsent(key, response);
      return previous != null ? previous : response;
    } catch (UnauthorizedException e) {
      throw new IOException(
          String.format("Unable to fetch route coverage for org %s app %s", orgId, appId), e);
    }
  }

  /**
   * Returns cached session metadata for a given application.
   *
   * @param orgId Organization identifier
   * @param appId Application identifier
   * @param sdkExtension Shared SDK extension
   * @return Optional containing session metadata response
   * @throws IOException if the metadata cannot be retrieved
   */
  public static Optional<SessionMetadataResponse> getLatestSessionMetadata(
      String orgId, String appId, SDKExtension sdkExtension) throws IOException {
    var key = new SessionMetadataKey(orgId, appId);
    var cached = SESSION_METADATA.get(key);
    if (cached != null) {
      logger.debug("Returning cached session metadata for org {} app {}", orgId, appId);
      return cached;
    }

    logger.info("Fetching session metadata for org {} app {} (not cached yet)", orgId, appId);
    try {
      var response = Optional.ofNullable(sdkExtension.getLatestSessionMetadata(orgId, appId));
      var previous = SESSION_METADATA.putIfAbsent(key, response);
      return previous != null ? previous : response;
    } catch (UnauthorizedException e) {
      throw new IOException(
          String.format("Unable to fetch session metadata for org %s app %s", orgId, appId), e);
    }
  }

  private record LibraryKey(String orgId, String appId) {}

  private record ProtectKey(String orgId, String appId) {}

  private record RouteCoverageKey(String orgId, String appId) {}

  private record SessionMetadataKey(String orgId, String appId) {}
}
