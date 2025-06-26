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
package com.contrast.labs.ai.mcp.contrast.sdkexstension;

import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.CveData;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.LibrariesExtended;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.ProtectData;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.application.ApplicationsResponse;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.routecoverage.RouteCoverageResponse;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.sca.LibraryObservation;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.sca.LibraryObservationsResponse;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.routecoverage.RouteDetailsResponse;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.sessionmetadata.SessionMetadataResponse;
import com.contrast.labs.ai.mcp.contrast.sdkexstension.data.traces.TracesExtended;
import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.http.*;
import com.contrastsecurity.models.RouteCoverageBySessionIDAndMetadataRequest;
import com.contrastsecurity.models.TraceFilterBody;
import com.contrastsecurity.models.Traces;
import com.contrastsecurity.sdk.ContrastSDK;
import com.contrastsecurity.sdk.internal.GsonFactory;
import com.google.gson.Gson;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class SDKExtension {

    private final ContrastSDK contrastSDK;
    private final UrlBuilder urlBuilder;
    private final Gson gson;

    public SDKExtension(ContrastSDK contrastSDK) {
        this.contrastSDK = contrastSDK;
        this.urlBuilder = UrlBuilder.getInstance();
        this.gson = GsonFactory.create();
    }

    public LibrariesExtended getLibrariesWithFilter(String organizationId, LibraryFilterForm filterForm)
            throws IOException, UnauthorizedException {
        try (InputStream is =
                     contrastSDK.makeRequest(
                             HttpMethod.GET, urlBuilder.getLibrariesFilterUrl(organizationId, filterForm));
             Reader reader = new InputStreamReader(is)) {
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
             Reader reader = new InputStreamReader(is)) {
            return gson.fromJson(reader, LibrariesExtended.class);
        }
    }

    public ProtectData getProtectConfig(String orgID, String appID) throws IOException {
        try (InputStream is =
                     contrastSDK.makeRequest(
                             HttpMethod.GET,
                             getProtectDataURL(orgID, appID));

        ) {
            Reader reader = new InputStreamReader(is);
            return gson.fromJson(reader, ProtectData.class);
        }
    }

    public CveData getAppsForCVE(String organizationId, String cveID) throws IOException {
        try (InputStream is =
                     contrastSDK.makeRequest(
                             HttpMethod.GET,
                             getCVEDataURL(organizationId, cveID, new FilterForm()));

        ) {
            Reader reader = new InputStreamReader(is);
            return gson.fromJson(reader, CveData.class);
        }
    }

    private String getProtectDataURL(String orgID, String appID) {
        return String.format("/ng/%s/protection/policy/%s?expand=skip_links", orgID, appID);
    }

    private String getCVEDataURL(String organizationId, String cve, FilterForm form) {
        String formString = form == null ? "" : form.toString();
        return String.format(
                "/ng/organizations/%s/cves/%s", organizationId, cve);
    }

    /**
     * Retrieves a list of all library observations for a specific library in an application.
     * This method handles pagination to ensure all observations are retrieved.
     *
     * @param organizationId The organization ID
     * @param applicationId The application ID
     * @param libraryId The library ID
     * @param pageSize The number of items per page (default 25)
     * @return List of all library observations
     * @throws IOException If an I/O error occurs
     * @throws UnauthorizedException If the request is not authorized
     */
    public List<LibraryObservation> getLibraryObservations(String organizationId, String applicationId, 
                                                         String libraryId, int pageSize) 
            throws IOException, UnauthorizedException {
        if (pageSize <= 0) {
            pageSize = 25; // Default page size
        }
        
        List<LibraryObservation> allObservations = new ArrayList<>();
        int offset = 0;
        int total;
        
        do {
            String url = getLibraryObservationsUrl(organizationId, applicationId, libraryId, offset, pageSize);
            
            try (InputStream is = contrastSDK.makeRequest(HttpMethod.GET, url);
                 Reader reader = new InputStreamReader(is)) {
                
                LibraryObservationsResponse response = gson.fromJson(reader, LibraryObservationsResponse.class);
                
                if (response.getObservations() != null) {
                    allObservations.addAll(response.getObservations());
                }
                
                total = response.getTotal();
                offset += pageSize;
            }
        } while (offset < total);
        
        return allObservations;
    }
    
    /**
     * Retrieves a list of all library observations using default page size of 25.
     */
    public List<LibraryObservation> getLibraryObservations(String organizationId, String applicationId, String libraryId) 
            throws IOException, UnauthorizedException {
        return getLibraryObservations(organizationId, applicationId, libraryId, 25);
    }
    
    /**
     * Builds URL for retrieving library observations
     */
    private String getLibraryObservationsUrl(String organizationId, String applicationId, String libraryId, 
                                           int offset, int limit) {
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
    public RouteDetailsResponse getRouteDetails(String organizationId, String applicationId, String routeHash)
            throws IOException, UnauthorizedException {
        String url = getRouteDetailsUrl(organizationId, applicationId, routeHash);

        try (InputStream is = contrastSDK.makeRequest(HttpMethod.GET, url);
             Reader reader = new InputStreamReader(is)) {
            return gson.fromJson(reader, RouteDetailsResponse.class);
        }
    }

    /**
     * Retrieves route coverage information for an application.
     *
     * @param organizationId The organization ID
     * @param appId The application ID
     * @param metadata Optional metadata request for filtering (can be null)
     * @return RouteCoverageResponse containing route coverage information
     * @throws IOException If an I/O error occurs
     * @throws UnauthorizedException If the request is not authorized
     */
    public RouteCoverageResponse getRouteCoverage(String organizationId, String appId,
                                                RouteCoverageBySessionIDAndMetadataRequest metadata)
            throws IOException, UnauthorizedException {

        InputStream is = null;

        try {
            if (metadata == null) {
                is = contrastSDK.makeRequest(
                        HttpMethod.GET,
                        urlBuilder.getRouteCoverageUrl(organizationId, appId));
            } else {
                is = contrastSDK.makeRequestWithBody(
                        HttpMethod.POST,
                        urlBuilder.getRouteCoverageWithMetadataUrl(organizationId, appId),
                        gson.toJson(metadata),
                        MediaType.JSON);
            }

            try (Reader reader = new InputStreamReader(is)) {
                return gson.fromJson(reader, RouteCoverageResponse.class);
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    /**
     * Builds URL for retrieving route details observations
     */
    private String getRouteDetailsUrl(String organizationId, String applicationId, String routeHash) {
        return String.format(
                "/ng/%s/applications/%s/route/%s/observations?expand=skip_links,session_metadata",
                organizationId, applicationId, routeHash);
    }

    public ApplicationsResponse getApplications(String organizationId)
            throws UnauthorizedException, IOException {
        String url = urlBuilder.getApplicationsUrl(organizationId)+"&expand=metadata,technologies,skip_links";
        try (InputStream is =
                     contrastSDK.makeRequest(HttpMethod.GET, url);
             Reader reader = new InputStreamReader(is)) {
            return this.gson.fromJson(reader, ApplicationsResponse.class);
        }
    }

    public Traces getTraces(String organizationId, String appId, TraceFilterBody filters)
            throws IOException, UnauthorizedException {
        try (InputStream is =
                     contrastSDK.makeRequestWithBody(
                             HttpMethod.POST,
                             urlBuilder.getTracesWithBodyUrl(organizationId, appId)+"?expand=session_metadata",
                             this.gson.toJson(filters),
                             MediaType.JSON);
             Reader reader = new InputStreamReader(is)) {
            return this.gson.fromJson(reader, Traces.class);
        }
    }

    public SessionMetadataResponse getLatestSessionMetadata(String organizationId, String appId)
            throws IOException, UnauthorizedException {

        String url = String.format(
                "/ng/organizations/%s/applications/%s/agent-sessions/latest",
                organizationId, appId);
        try (InputStream is =
                     contrastSDK.makeRequest(HttpMethod.GET, url);
             Reader reader = new InputStreamReader(is)) {
            return this.gson.fromJson(reader, com.contrast.labs.ai.mcp.contrast.sdkexstension.data.sessionmetadata.SessionMetadataResponse.class);
        }
    }

    public TracesExtended getTracesExtended(String organizationId, String appId, TraceFilterBody filters)
            throws IOException, UnauthorizedException {
        try (InputStream is =
                     contrastSDK.makeRequestWithBody(
                             HttpMethod.POST,
                             urlBuilder.getTracesWithBodyUrl(organizationId, appId)+"?expand=session_metadata",
                             this.gson.toJson(filters),
                             MediaType.JSON);
             Reader reader = new InputStreamReader(is)) {
            return this.gson.fromJson(reader, com.contrast.labs.ai.mcp.contrast.sdkexstension.data.traces.TracesExtended.class);
        }
    }


}
