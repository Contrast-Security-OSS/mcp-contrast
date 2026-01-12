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
package com.contrast.labs.ai.mcp.contrast.sdkextension;

import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibrariesExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.Application;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.sca.LibraryObservation;
import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.http.LibraryFilterForm;
import com.contrastsecurity.sdk.ContrastSDK;
import com.contrastsecurity.sdk.UserAgentProduct;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@Slf4j
public class SDKHelper {

  private static final String MCP_SERVER_NAME = "contrast-mcp";
  private static final String HTTPS_PROTOCOL = "https://";
  private static final String HTTP_PROTOCOL = "http://";
  private static Environment environment;

  @Autowired
  public void setEnvironment(Environment environment) {
    SDKHelper.environment = environment;
  }

  private static final Cache<String, List<LibraryExtended>> libraryCache =
      CacheBuilder.newBuilder().maximumSize(500000).expireAfterWrite(10, TimeUnit.MINUTES).build();

  private static final Cache<String, List<LibraryObservation>> libraryObservationsCache =
      CacheBuilder.newBuilder().maximumSize(500000).expireAfterWrite(10, TimeUnit.MINUTES).build();

  private static final Cache<String, List<Application>> applicationsCache =
      CacheBuilder.newBuilder().maximumSize(500000).expireAfterWrite(5, TimeUnit.MINUTES).build();

  /**
   * Retrieves a single page of libraries for an application with server-side pagination. Unlike
   * {@link #getLibsForID}, this method does NOT cache results and returns only the requested page
   * along with the total count for pagination metadata.
   *
   * @param appId The application ID
   * @param orgId The organization ID
   * @param extendedSDK The SDK extension instance
   * @param limit Number of items per page (max 50 per API)
   * @param offset Starting index (0-based)
   * @return LibrariesExtended containing the page of libraries and total count
   */
  public static LibrariesExtended getLibraryPage(
      String appId, String orgId, SDKExtension extendedSDK, int limit, int offset)
      throws IOException {

    // API enforces max limit of 50
    int effectiveLimit = Math.min(limit, 50);

    var filterForm = new LibraryFilterForm();
    filterForm.setLimit(effectiveLimit);
    filterForm.setOffset(offset);
    filterForm.setExpand(EnumSet.of(LibraryFilterForm.LibrariesExpandValues.VULNS));

    return extendedSDK.getLibrariesWithFilter(orgId, appId, filterForm);
  }

  public static List<LibraryExtended> getLibsForID(
      String appId, String orgId, SDKExtension extendedSDK) throws IOException {
    // Check cache for existing result
    var cachedLibraries = libraryCache.getIfPresent(appId);
    if (cachedLibraries != null) {
      log.info("Cache hit for appId: {}", appId);
      return cachedLibraries;
    }
    log.info("Cache miss for appId: {}, fetching libraries from SDK", appId);
    int libraryCallSize = 50;
    var filterForm = new LibraryFilterForm();
    filterForm.setLimit(libraryCallSize);
    filterForm.setExpand(EnumSet.of(LibraryFilterForm.LibrariesExpandValues.VULNS));
    var libraries = extendedSDK.getLibrariesWithFilter(orgId, appId, filterForm);
    var libs = new ArrayList<LibraryExtended>();
    libs.addAll(libraries.getLibraries());
    int offset = libraryCallSize;
    while (libraries.getLibraries().size() == libraryCallSize) {
      log.debug("Retrieved {} libraries, fetching more with offset: {}", libraryCallSize, offset);
      filterForm.setOffset(offset);
      libraries = extendedSDK.getLibrariesWithFilter(orgId, appId, filterForm);
      libs.addAll(libraries.getLibraries());
      offset += libraryCallSize;
    }

    log.info("Successfully retrieved {} libraries for application id: {}", libs.size(), appId);

    // Store result in cache
    libraryCache.put(appId, libs);

    return libs;
  }

  /**
   * Retrieves library observations for a specific library in an application, with caching.
   *
   * @param libraryId The library ID
   * @param appId The application ID
   * @param orgId The organization ID
   * @param pageSize The number of items per page (default 25)
   * @param extendedSDK The SDK extension instance
   * @return List of library observations
   * @throws IOException If an I/O error occurs
   * @throws UnauthorizedException If the request is not authorized
   */
  public static List<LibraryObservation> getLibraryObservationsWithCache(
      String libraryId, String appId, String orgId, int pageSize, SDKExtension extendedSDK)
      throws IOException, UnauthorizedException {

    // Generate cache key from combination of library, app and org IDs
    var cacheKey = String.format("%s:%s:%s", orgId, appId, libraryId);

    // Check cache for existing result
    var cachedObservations = libraryObservationsCache.getIfPresent(cacheKey);
    if (cachedObservations != null) {
      log.info("Cache hit for library observations: {}", cacheKey);
      return cachedObservations;
    }

    log.info("Cache miss for library observations: {}, fetching from API", cacheKey);
    var observations = extendedSDK.getLibraryObservations(orgId, appId, libraryId, pageSize);

    log.info(
        "Successfully retrieved {} library observations for library: {} in app: {}",
        observations.size(),
        libraryId,
        appId);

    // Store result in cache
    libraryObservationsCache.put(cacheKey, observations);

    return observations;
  }

  /** Retrieves library observations with default page size of 25. */
  public static List<LibraryObservation> getLibraryObservationsWithCache(
      String libraryId, String appId, String orgId, SDKExtension extendedSDK)
      throws IOException, UnauthorizedException {
    return getLibraryObservationsWithCache(libraryId, appId, orgId, 25, extendedSDK);
  }

  /**
   * Constructs a URL with protocol and server. If the hostname already contains a protocol (e.g.,
   * "https://host.com"), it returns the hostname as is. Otherwise, it prepends the provided
   * protocol. Trailing slashes are removed to ensure consistent URL formatting.
   *
   * @param hostName The hostname, which may or may not include a protocol
   * @param protocol The protocol to use if hostname doesn't include one (e.g., "https")
   * @return A URL with protocol and hostname (without trailing slash), or null if hostname is
   *     null/empty
   * @throws IllegalArgumentException If the hostname contains an invalid protocol
   */
  public static String getProtocolAndServer(String hostName, String protocol) {
    if (hostName == null) {
      return null;
    }

    // Trim whitespace
    hostName = hostName.trim();

    // Return null for empty strings (consistent with null handling)
    if (hostName.isEmpty()) {
      return null;
    }

    String result;

    // Check if hostname contains a protocol separator
    if (hostName.contains("://")) {
      // Validate that it's a supported protocol
      if (!hostName.startsWith(HTTP_PROTOCOL) && !hostName.startsWith(HTTPS_PROTOCOL)) {
        throw new IllegalArgumentException(
            "Invalid protocol in hostname: "
                + hostName
                + ". Only http:// and https:// are supported.");
      }
      result = hostName;
    } else {
      // No protocol specified, prepend provided protocol (default to https if not specified)
      var effectiveProtocol = StringUtils.hasText(protocol) ? protocol : "https";
      result = effectiveProtocol + "://" + hostName;
    }

    // Remove trailing slash to prevent double slashes in URLs
    if (result.endsWith("/")) {
      result = result.substring(0, result.length() - 1);
    }

    return result;
  }

  // The withUserAgentProduct will generate a user agent header that looks like
  // User-Agent: contrast-mcp/1.0 contrast-sdk-java/3.4.2 Java/19.0.2+7
  public static ContrastSDK getSDK(
      String hostName,
      String apiKey,
      String serviceKey,
      String userName,
      String httpProxyHost,
      String httpProxyPort,
      String protocol) {
    log.info("Initializing ContrastSDK with username: {}, host: {}", userName, hostName);

    var baseUrl = getProtocolAndServer(hostName, protocol);
    var apiUrl = baseUrl + "/Contrast/api";
    log.info("API URL will be : {}", apiUrl);

    var mcpVersion = SDKHelper.environment.getProperty("spring.ai.mcp.server.version", "unknown");

    var builder =
        new ContrastSDK.Builder(userName, serviceKey, apiKey)
            .withApiUrl(apiUrl)
            .withUserAgentProduct(UserAgentProduct.of(MCP_SERVER_NAME, mcpVersion));

    if (StringUtils.hasText(httpProxyHost)) {
      int port = StringUtils.hasText(httpProxyPort) ? Integer.parseInt(httpProxyPort) : 80;
      log.debug("Configuring HTTP proxy: {}:{}", httpProxyHost, port);

      var proxy =
          new java.net.Proxy(
              java.net.Proxy.Type.HTTP, new java.net.InetSocketAddress(httpProxyHost, port));

      builder.withProxy(proxy);
    }

    return builder.build();
  }

  public static Optional<Application> getApplicationByName(
      String appName, String orgId, ContrastSDK contrastSDK) throws IOException {
    log.debug("Searching for application by name: {}", appName);
    for (Application app : getApplicationsWithCache(orgId, contrastSDK)) {
      if (app.getName().equalsIgnoreCase(appName)) {
        log.info("Found application - ID: {}, Name: {}", app.getAppId(), app.getName());
        return Optional.of(app);
      }
    }

    log.warn("No application found with name: {}, clearing cache and retrying", appName);
    clearApplicationsCache();
    for (Application app : getApplicationsWithCache(orgId, contrastSDK)) {
      if (app.getName().equalsIgnoreCase(appName)) {
        log.info(
            "Found application after cache clear - ID: {}, Name: {}",
            app.getAppId(),
            app.getName());
        return Optional.of(app);
      }
    }
    return Optional.empty();
  }

  /**
   * Retrieves all applications from Contrast with caching.
   *
   * @param orgId The organization ID
   * @param contrastSDK The Contrast SDK instance
   * @return List of applications
   * @throws IOException If an I/O error occurs
   */
  public static List<Application> getApplicationsWithCache(String orgId, ContrastSDK contrastSDK)
      throws IOException {
    // Generate cache key based on organization ID
    var cacheKey = String.format("applications:%s", orgId);

    // Check cache for existing result
    var cachedApplications = applicationsCache.getIfPresent(cacheKey);
    if (cachedApplications != null) {
      log.info("Cache hit for applications in org: {}", orgId);
      return cachedApplications;
    }

    log.info("Cache miss for applications in org: {}, fetching from API", orgId);
    var applications = new SDKExtension(contrastSDK).getApplications(orgId).getApplications();
    log.info(
        "Successfully retrieved {} applications from organization: {}", applications.size(), orgId);

    // Store result in cache
    applicationsCache.put(cacheKey, applications);

    return applications;
  }

  /**
   * Clears the libraries cache.
   *
   * @return The number of entries cleared
   */
  public static long clearLibraryCache() {
    long size = libraryCache.size();
    libraryCache.invalidateAll();
    libraryCache.cleanUp();
    log.info("Cleared {} entries from library cache", size);
    return size;
  }

  /**
   * Clears the library observations cache.
   *
   * @return The number of entries cleared
   */
  public static long clearLibraryObservationsCache() {
    long size = libraryObservationsCache.size();
    libraryObservationsCache.invalidateAll();
    libraryObservationsCache.cleanUp();
    log.info("Cleared {} entries from library observations cache", size);
    return size;
  }

  /**
   * Clears the applications cache.
   *
   * @return The number of entries cleared
   */
  public static long clearApplicationsCache() {
    long size = applicationsCache.size();
    applicationsCache.invalidateAll();
    applicationsCache.cleanUp();
    log.info("Cleared {} entries from applications cache", size);
    return size;
  }

  /**
   * Clears all caches maintained by the SDKHelper.
   *
   * @return Total number of entries cleared across all caches
   */
  public static long clearAllCaches() {
    long libraryEntries = clearLibraryCache();
    long observationsEntries = clearLibraryObservationsCache();
    long applicationsEntries = clearApplicationsCache();

    long totalCleared = libraryEntries + observationsEntries + applicationsEntries;
    log.info("Cleared a total of {} entries from all caches", totalCleared);

    return totalCleared;
  }
}
