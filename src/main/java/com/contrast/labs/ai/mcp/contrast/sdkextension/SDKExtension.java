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

import com.contrast.labs.ai.mcp.contrast.sdkextension.data.CveData;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibrariesExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.ProtectData;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr.AttacksFilterBody;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr.AttacksResponse;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.ApplicationsResponse;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.RouteCoverageResponse;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.RouteDetailsResponse;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.sca.LibraryObservation;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.sca.LibraryObservationsResponse;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata.SessionMetadataResponse;
import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.http.FilterForm;
import com.contrastsecurity.http.HttpMethod;
import com.contrastsecurity.http.LibraryFilterForm;
import com.contrastsecurity.http.MediaType;
import com.contrastsecurity.http.TraceFilterForm.TraceExpandValue;
import com.contrastsecurity.http.UrlBuilder;
import com.contrastsecurity.models.RouteCoverageBySessionIDAndMetadataRequest;
import com.contrastsecurity.models.TraceFilterBody;
import com.contrastsecurity.models.Traces;
import com.contrastsecurity.sdk.ContrastSDK;
import com.contrastsecurity.sdk.internal.GsonFactory;
import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SDKExtension {

  private final ContrastSDK contrastSDK;
  private final UrlBuilder urlBuilder;
  private final Gson gson;

  public SDKExtension(ContrastSDK contrastSDK) {
    this.contrastSDK = contrastSDK;
    this.urlBuilder = UrlBuilder.getInstance();
    this.gson = GsonFactory.create();
  }

  public LibrariesExtended getLibrariesWithFilter(
      String organizationId, LibraryFilterForm filterForm)
      throws IOException, UnauthorizedException {
    try (InputStream is =
            contrastSDK.makeRequest(
                HttpMethod.GET, urlBuilder.getLibrariesFilterUrl(organizationId, filterForm));
        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
      return gson.fromJson(reader, LibrariesExtended.class);
    }
  }

  public LibrariesExtended getLibrariesWithFilter(
      String organizationId, String appId, LibraryFilterForm filterForm)
      throws IOException, UnauthorizedException {
    try (InputStream is =
            contrastSDK.makeRequest(
                HttpMethod.GET,
                urlBuilder.getLibrariesFilterUrl(organizationId, appId, filterForm));
        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
      return gson.fromJson(reader, LibrariesExtended.class);
    }
  }

  public ProtectData getProtectConfig(String orgId, String appId) throws IOException {
    try (InputStream is =
        contrastSDK.makeRequest(HttpMethod.GET, getProtectDataURL(orgId, appId)); ) {

      var reader = new InputStreamReader(is, StandardCharsets.UTF_8);
      return gson.fromJson(reader, ProtectData.class);
    }
  }

  public CveData getAppsForCVE(String organizationId, String cveID) throws IOException {
    try (InputStream is =
        contrastSDK.makeRequest(
            HttpMethod.GET, getCVEDataURL(organizationId, cveID, new FilterForm())); ) {

      var reader = new InputStreamReader(is, StandardCharsets.UTF_8);
      return gson.fromJson(reader, CveData.class);
    }
  }

  private String getProtectDataURL(String orgId, String appId) {
    return String.format("/ng/%s/protection/policy/%s?expand=skip_links", orgId, appId);
  }

  private String getCVEDataURL(String organizationId, String cve, FilterForm form) {
    var formString = form == null ? "" : form.toString();
    return String.format("/ng/organizations/%s/cves/%s", organizationId, cve);
  }

  /**
   * Retrieves a list of all library observations for a specific library in an application. This
   * method handles pagination to ensure all observations are retrieved.
   *
   * @param organizationId The organization ID
   * @param applicationId The application ID
   * @param libraryId The library ID
   * @param pageSize The number of items per page (default 25)
   * @return List of all library observations
   * @throws IOException If an I/O error occurs
   * @throws UnauthorizedException If the request is not authorized
   */
  public List<LibraryObservation> getLibraryObservations(
      String organizationId, String applicationId, String libraryId, int pageSize)
      throws IOException, UnauthorizedException {
    if (pageSize <= 0) {
      pageSize = 25; // Default page size
    }

    var allObservations = new ArrayList<LibraryObservation>();
    int offset = 0;
    int total;

    do {
      var url =
          getLibraryObservationsUrl(organizationId, applicationId, libraryId, offset, pageSize);

      try (InputStream is = contrastSDK.makeRequest(HttpMethod.GET, url);
          Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

        var response = gson.fromJson(reader, LibraryObservationsResponse.class);

        if (response.getObservations() != null) {
          allObservations.addAll(response.getObservations());
        }

        total = response.getTotal();
        offset += pageSize;
      }
    } while (offset < total);

    return allObservations;
  }

  /** Retrieves a list of all library observations using default page size of 25. */
  public List<LibraryObservation> getLibraryObservations(
      String organizationId, String applicationId, String libraryId)
      throws IOException, UnauthorizedException {
    return getLibraryObservations(organizationId, applicationId, libraryId, 25);
  }

  /** Builds URL for retrieving library observations */
  private String getLibraryObservationsUrl(
      String organizationId, String applicationId, String libraryId, int offset, int limit) {
    return String.format(
        "/ng/organizations/%s/applications/%s/libraries/%s/reports/library-usage?offset=%d&limit=%d&sortBy=lastObservedTime&sortDirection=DESC",
        organizationId, applicationId, libraryId, offset, limit);
  }

  /**
   * Retrieves the detailed observations for a specific route.
   *
   * @param organizationId The organization ID
   * @param applicationId The application ID
   * @param routeHash The unique hash identifying the route
   * @return RouteDetailsResponse containing observations for the route
   * @throws IOException If an I/O error occurs
   * @throws UnauthorizedException If the request is not authorized
   */
  public RouteDetailsResponse getRouteDetails(
      String organizationId, String applicationId, String routeHash)
      throws IOException, UnauthorizedException {
    var url = getRouteDetailsUrl(organizationId, applicationId, routeHash);

    try (InputStream is = contrastSDK.makeRequest(HttpMethod.GET, url);
        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
      return gson.fromJson(reader, RouteDetailsResponse.class);
    }
  }

  /**
   * Retrieves route coverage information for an application.
   *
   * <p>Uses GET with expand=observations for unfiltered requests, POST for filtered requests. Both
   * include observations inline, eliminating N+1 queries for route details.
   *
   * @param organizationId The organization ID
   * @param appId The application ID
   * @param metadata Optional metadata request for filtering (can be null for unfiltered)
   * @return RouteCoverageResponse containing route coverage information with observations
   * @throws IOException If an I/O error occurs
   * @throws UnauthorizedException If the request is not authorized
   */
  public RouteCoverageResponse getRouteCoverage(
      String organizationId, String appId, RouteCoverageBySessionIDAndMetadataRequest metadata)
      throws IOException, UnauthorizedException {

    InputStream is;

    if (metadata == null) {
      // GET for unfiltered - add expand=observations to include observations inline
      var url = urlBuilder.getRouteCoverageUrl(organizationId, appId) + "&expand=observations";
      is = contrastSDK.makeRequest(HttpMethod.GET, url);
    } else {
      // POST for filtered - URL already includes expand=observations
      var url = urlBuilder.getRouteCoverageWithMetadataUrl(organizationId, appId);
      is =
          contrastSDK.makeRequestWithBody(
              HttpMethod.POST, url, gson.toJson(metadata), MediaType.JSON);
    }

    try (is;
        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
      return gson.fromJson(reader, RouteCoverageResponse.class);
    }
  }

  /** Builds URL for retrieving route details observations */
  private String getRouteDetailsUrl(String organizationId, String applicationId, String routeHash) {
    return String.format(
        "/ng/%s/applications/%s/route/%s/observations?expand=skip_links,session_metadata",
        organizationId, applicationId, routeHash);
  }

  /**
   * Retrieves all applications for an organization.
   *
   * <p>When debug logging is enabled, this method will buffer the entire response in memory to log
   * it before parsing. For organizations with many applications, this could consume significant
   * memory (approximately 1-2 KB per application). In production environments, debug logging should
   * be disabled to stream responses directly without buffering.
   *
   * @param organizationId The organization ID
   * @return ApplicationsResponse containing all applications
   * @throws UnauthorizedException If the request is not authorized
   * @throws IOException If an I/O error occurs
   */
  public ApplicationsResponse getApplications(String organizationId)
      throws UnauthorizedException, IOException {
    var url =
        urlBuilder.getApplicationsUrl(organizationId) + "&expand=metadata,technologies,skip_links";

    // When debug logging is enabled, buffer the response for logging
    if (log.isDebugEnabled()) {
      try (InputStream is = contrastSDK.makeRequest(HttpMethod.GET, url)) {
        var responseContent = convertStreamToString(is);
        log.debug("Applications API response: {}", responseContent);

        // Parse the buffered response string
        try (Reader reader = new StringReader(responseContent)) {
          return this.gson.fromJson(reader, ApplicationsResponse.class);
        }
      }
    } else {
      // Stream response directly without buffering
      try (InputStream is = contrastSDK.makeRequest(HttpMethod.GET, url);
          Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
        return this.gson.fromJson(reader, ApplicationsResponse.class);
      }
    }
  }

  /**
   * Converts an InputStream to String by reading all available data.
   *
   * <p>This method fully consumes the InputStream. The caller is responsible for closing the
   * InputStream after this method returns.
   *
   * @param is The InputStream to convert
   * @return The string content of the stream
   * @throws IOException If an I/O error occurs
   */
  private String convertStreamToString(InputStream is) throws IOException {
    if (is == null) {
      return "";
    }

    var sb = new StringBuilder();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line).append("\n");
      }
    }
    return sb.toString();
  }

  public Traces getTraces(String organizationId, String appId, TraceFilterBody filters)
      throws IOException, UnauthorizedException {
    try (InputStream is =
            contrastSDK.makeRequestWithBody(
                HttpMethod.POST,
                urlBuilder.getTracesWithBodyUrl(organizationId, appId)
                    + "?expand=session_metadata,server_environments",
                this.gson.toJson(filters),
                MediaType.JSON);
        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
      return this.gson.fromJson(reader, Traces.class);
    }
  }

  /**
   * Get vulnerabilities for an application using POST endpoint with pagination.
   *
   * <p>This uses the POST /ng/{orgId}/traces/{appId}/filter endpoint with body-based filtering.
   * Unlike the deprecated GET endpoint with TraceFilterForm, this endpoint defaults to returning
   * ALL vulnerabilities when tracked/untracked are both false (primitive defaults).
   *
   * @param organizationId the organization ID
   * @param appId the application ID
   * @param filters the filter request body
   * @param limit max results to return
   * @param offset pagination offset
   * @param expand expand values to include additional data in response
   * @return Traces response
   */
  public Traces getTraces(
      String organizationId,
      String appId,
      TraceFilterBody filters,
      int limit,
      int offset,
      EnumSet<TraceExpandValue> expand)
      throws IOException, UnauthorizedException {

    var url =
        String.format(
            "/ng/%s/traces/%s/filter?limit=%d&offset=%d&sort=-lastTimeSeen%s",
            organizationId, appId, limit, offset, toExpandString(expand));

    try (InputStream is =
            contrastSDK.makeRequestWithBody(
                HttpMethod.POST, url, gson.toJson(filters), MediaType.JSON);
        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
      return gson.fromJson(reader, Traces.class);
    }
  }

  /**
   * Get vulnerabilities across all applications in the organization using POST endpoint.
   *
   * <p>This uses the POST /ng/{orgId}/orgtraces/filter endpoint with body-based filtering. Unlike
   * the deprecated GET endpoint with TraceFilterForm, this endpoint defaults to returning ALL
   * vulnerabilities when tracked/untracked are both false (primitive defaults).
   *
   * @param organizationId the organization ID
   * @param filters the filter request body
   * @param limit max results to return
   * @param offset pagination offset
   * @param expand expand values to include additional data in response
   * @return Traces response
   */
  public Traces getTracesInOrg(
      String organizationId,
      TraceFilterBody filters,
      int limit,
      int offset,
      EnumSet<TraceExpandValue> expand)
      throws IOException, UnauthorizedException {

    var url =
        String.format(
            "/ng/%s/orgtraces/filter?limit=%d&offset=%d&sort=-lastTimeSeen%s",
            organizationId, limit, offset, toExpandString(expand));

    try (InputStream is =
            contrastSDK.makeRequestWithBody(
                HttpMethod.POST, url, gson.toJson(filters), MediaType.JSON);
        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
      return gson.fromJson(reader, Traces.class);
    }
  }

  public SessionMetadataResponse getLatestSessionMetadata(String organizationId, String appId)
      throws IOException, UnauthorizedException {

    var url =
        String.format(
            "/ng/organizations/%s/applications/%s/agent-sessions/latest", organizationId, appId);
    try (InputStream is = contrastSDK.makeRequest(HttpMethod.GET, url);
        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
      return this.gson.fromJson(
          reader,
          com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata
              .SessionMetadataResponse.class);
    }
  }

  // ==== ADR (Attack Detection and Response) Methods ====

  /**
   * Retrieves attacks from the Contrast platform based on filter criteria.
   *
   * @param organizationId The organization ID
   * @param filterBody Filter criteria for attacks (can be null for default filter)
   * @param limit Maximum number of attacks to return (default: 1000)
   * @param offset Pagination offset (default: 0)
   * @param sort Sort order (default: -startTime)
   * @return AttacksResponse containing attacks list and pagination metadata (count)
   * @throws IOException If an I/O error occurs
   * @throws UnauthorizedException If the request is not authorized
   */
  public AttacksResponse getAttacks(
      String organizationId,
      AttacksFilterBody filterBody,
      Integer limit,
      Integer offset,
      String sort)
      throws IOException, UnauthorizedException {

    // Set default values if not provided
    if (limit == null) limit = 1000;
    if (offset == null) offset = 0;
    if (sort == null) sort = "-startTime";
    if (filterBody == null) filterBody = AttacksFilterBody.builder().build();

    var url =
        String.format(
            "/ng/%s/attacks?expand=skip_links&limit=%d&offset=%d&sort=%s",
            organizationId, limit, offset, sort);

    try (InputStream is =
            contrastSDK.makeRequestWithBody(
                HttpMethod.POST, url, this.gson.toJson(filterBody), MediaType.JSON);
        Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {

      // Parse complete JSON response including metadata
      var response = this.gson.fromJson(reader, AttacksResponse.class);

      // Handle null response gracefully
      if (response == null) {
        response = new AttacksResponse();
        response.setAttacks(new ArrayList<>());
      }

      // Ensure attacks list is never null
      if (response.getAttacks() == null) {
        response.setAttacks(new ArrayList<>());
      }

      return response;
    }
  }

  /**
   * Retrieves attacks with default parameters (limit=1000, offset=0, sort=-startTime).
   *
   * @param organizationId The organization ID
   * @param filterBody Filter criteria for attacks (can be null for default filter)
   * @return AttacksResponse containing attacks list and pagination metadata
   * @throws IOException If an I/O error occurs
   * @throws UnauthorizedException If the request is not authorized
   */
  public AttacksResponse getAttacks(String organizationId, AttacksFilterBody filterBody)
      throws IOException, UnauthorizedException {
    return getAttacks(organizationId, filterBody, null, null, null);
  }

  /**
   * Retrieves attacks with default filter and parameters.
   *
   * @param organizationId The organization ID
   * @return AttacksResponse containing attacks list and pagination metadata
   * @throws IOException If an I/O error occurs
   * @throws UnauthorizedException If the request is not authorized
   */
  public AttacksResponse getAttacks(String organizationId)
      throws IOException, UnauthorizedException {
    return getAttacks(organizationId, null, null, null, null);
  }

  /**
   * Converts an EnumSet of TraceExpandValue to a URL query parameter string.
   *
   * @param expand the expand values to convert
   * @return query parameter string (e.g., "&expand=session_metadata,application") or empty string
   */
  private String toExpandString(EnumSet<TraceExpandValue> expand) {
    if (expand == null || expand.isEmpty()) {
      return "";
    }
    var expandStr =
        expand.stream().map(TraceExpandValue::toString).collect(Collectors.joining(","));
    return "&expand=" + expandStr;
  }
}
