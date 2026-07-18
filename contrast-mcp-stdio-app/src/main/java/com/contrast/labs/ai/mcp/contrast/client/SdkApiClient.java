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
package com.contrast.labs.ai.mcp.contrast.client;

import com.contrast.labs.ai.mcp.contrast.config.ContrastSDKFactory;
import com.contrast.labs.ai.mcp.contrast.config.SDKExtensionFactory;
import com.contrast.labs.ai.mcp.contrast.result.ScanProject;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKHelper;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.CveData;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibrariesExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.LibraryExtended;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.ProtectData;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr.AttacksFilterBody;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.adr.AttacksResponse;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.AppMetadataField;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.AppMetadataFilter;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.application.ApplicationsResponse;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage.RouteCoverageResponse;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.sca.LibraryObservation;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.server.ServerFilterBody;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.server.ServersResponse;
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata.SessionMetadataResponse;
import com.contrastsecurity.exceptions.HttpResponseException;
import com.contrastsecurity.exceptions.UnauthorizedException;
import com.contrastsecurity.http.TraceFilterForm.TraceExpandValue;
import com.contrastsecurity.models.EventSummaryResponse;
import com.contrastsecurity.models.HttpRequestResponse;
import com.contrastsecurity.models.MetadataFilterResponse;
import com.contrastsecurity.models.RecommendationResponse;
import com.contrastsecurity.models.RouteCoverageBySessionIDAndMetadataRequest;
import com.contrastsecurity.models.Rules;
import com.contrastsecurity.models.Trace;
import com.contrastsecurity.models.TraceFilterBody;
import com.contrastsecurity.models.Traces;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SdkApiClient implements ContrastApiClient {

  private final ContrastSDKFactory contrastSDKFactory;
  private final SDKExtensionFactory sdkExtensionFactory;

  @Override
  public ApplicationsResponse searchApplications(
      String name, String[] tags, List<AppMetadataFilter> metadataFilters, int limit, int offset)
      throws Exception {
    return sdkExtensionFactory
        .getSDKExtension()
        .getApplicationsFiltered(localOrganization(), name, tags, metadataFilters, limit, offset);
  }

  @Override
  public List<AppMetadataField> getApplicationMetadataFields() throws Exception {
    return sdkExtensionFactory.getSDKExtension().getApplicationMetadataFields(localOrganization());
  }

  @Override
  public MetadataFilterResponse getSessionMetadata(String appId) throws Exception {
    return contrastSDKFactory
        .getSDK()
        .getSessionMetadataForApplication(localOrganization(), appId, null);
  }

  @Override
  public SessionMetadataResponse getLatestSessionMetadata(String appId) throws Exception {
    return sdkExtensionFactory
        .getSDKExtension()
        .getLatestSessionMetadata(localOrganization(), appId);
  }

  @Override
  public Traces searchVulnerabilities(
      TraceFilterBody filters, int limit, int offset, EnumSet<TraceExpandValue> expand)
      throws Exception {
    return sdkExtensionFactory
        .getSDKExtension()
        .getTracesInOrg(localOrganization(), filters, limit, offset, expand);
  }

  @Override
  public Traces searchAppVulnerabilities(
      String appId,
      TraceFilterBody filters,
      int limit,
      int offset,
      EnumSet<TraceExpandValue> expand)
      throws Exception {
    try {
      return sdkExtensionFactory
          .getSDKExtension()
          .getTraces(localOrganization(), appId, filters, limit, offset, expand);
    } catch (UnauthorizedException e) {
      throw normalizeForbidden(e);
    }
  }

  @Override
  public Trace getVulnerability(
      String appId, String vulnerabilityId, EnumSet<TraceExpandValue> expand) throws Exception {
    return contrastSDKFactory
        .getSDK()
        .getTrace(localOrganization(), appId, vulnerabilityId, expand);
  }

  @Override
  public RecommendationResponse getRecommendation(String vulnerabilityId) throws Exception {
    return contrastSDKFactory.getSDK().getRecommendation(localOrganization(), vulnerabilityId);
  }

  @Override
  public HttpRequestResponse getHttpRequest(String vulnerabilityId) throws Exception {
    return contrastSDKFactory.getSDK().getHttpRequest(localOrganization(), vulnerabilityId);
  }

  @Override
  public EventSummaryResponse getEventSummary(String vulnerabilityId) throws Exception {
    return contrastSDKFactory.getSDK().getEventSummary(localOrganization(), vulnerabilityId);
  }

  @Override
  public Rules getRules() throws Exception {
    return contrastSDKFactory.getSDK().getRules(localOrganization());
  }

  @Override
  public LibrariesExtended getLibraryPage(String appId, int limit, int offset) throws Exception {
    return SDKHelper.getLibraryPage(
        appId, localOrganization(), sdkExtensionFactory.getSDKExtension(), limit, offset);
  }

  @Override
  public List<LibraryExtended> getAllLibraries(String appId) throws Exception {
    return SDKHelper.getLibsForID(
        appId, localOrganization(), sdkExtensionFactory.getSDKExtension());
  }

  @Override
  public List<LibraryObservation> getLibraryObservations(
      String appId, String libraryId, int pageSize) throws Exception {
    return SDKHelper.getLibraryObservationsWithCache(
        libraryId, appId, localOrganization(), pageSize, sdkExtensionFactory.getSDKExtension());
  }

  @Override
  public CveData getApplicationsByCve(String cveId) throws Exception {
    return sdkExtensionFactory.getSDKExtension().getAppsForCVE(localOrganization(), cveId);
  }

  @Override
  public AttacksResponse searchAttacks(
      AttacksFilterBody filters, int limit, int offset, String sort) throws Exception {
    return sdkExtensionFactory
        .getSDKExtension()
        .getAttacks(localOrganization(), filters, limit, offset, sort);
  }

  @Override
  public ServersResponse searchServers(
      ServerFilterBody filters, int limit, int offset, String sort, boolean includeApplications)
      throws Exception {
    return sdkExtensionFactory
        .getSDKExtension()
        .getServersFiltered(localOrganization(), filters, limit, offset, sort, includeApplications);
  }

  @Override
  public ProtectData getProtectRules(String appId) throws Exception {
    return sdkExtensionFactory.getSDKExtension().getProtectConfig(localOrganization(), appId);
  }

  @Override
  public RouteCoverageResponse getRouteCoverage(
      String appId, RouteCoverageBySessionIDAndMetadataRequest filter) throws Exception {
    return sdkExtensionFactory
        .getSDKExtension()
        .getRouteCoverage(localOrganization(), appId, filter);
  }

  @Override
  public ScanProject getScanProject(String projectName) throws Exception {
    var project =
        contrastSDKFactory.getSDK().scan(localOrganization()).projects().findByName(projectName);
    return project.map(ScanProject::from).orElse(null);
  }

  private String localOrganization() {
    return contrastSDKFactory.getOrgId();
  }

  private static HttpResponseException normalizeForbidden(UnauthorizedException e) {
    if (e.getCode() != 403) {
      return e;
    }
    return new HttpResponseException(
        "Downstream request failed",
        "POST",
        "[redacted]",
        e.getCode(),
        e.getStatus(),
        "[redacted]");
  }
}
