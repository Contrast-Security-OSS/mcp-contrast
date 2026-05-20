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

import com.contrast.labs.ai.mcp.contrast.result.ScanProject;
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
import com.contrast.labs.ai.mcp.contrast.sdkextension.data.sessionmetadata.SessionMetadataResponse;
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

public interface ContrastApiClient {

  ApplicationsResponse searchApplications(
      String name, String[] tags, List<AppMetadataFilter> metadataFilters, int limit, int offset)
      throws Exception;

  List<AppMetadataField> getApplicationMetadataFields() throws Exception;

  MetadataFilterResponse getSessionMetadata(String appId) throws Exception;

  SessionMetadataResponse getLatestSessionMetadata(String appId) throws Exception;

  Traces searchVulnerabilities(
      TraceFilterBody filters, int limit, int offset, EnumSet<TraceExpandValue> expand)
      throws Exception;

  Traces searchAppVulnerabilities(
      String appId,
      TraceFilterBody filters,
      int limit,
      int offset,
      EnumSet<TraceExpandValue> expand)
      throws Exception;

  Trace getVulnerability(String appId, String vulnerabilityId, EnumSet<TraceExpandValue> expand)
      throws Exception;

  RecommendationResponse getRecommendation(String vulnerabilityId) throws Exception;

  HttpRequestResponse getHttpRequest(String vulnerabilityId) throws Exception;

  EventSummaryResponse getEventSummary(String vulnerabilityId) throws Exception;

  Rules getRules() throws Exception;

  LibrariesExtended getLibraryPage(String appId, int limit, int offset) throws Exception;

  List<LibraryExtended> getAllLibraries(String appId) throws Exception;

  List<LibraryObservation> getLibraryObservations(String appId, String libraryId, int pageSize)
      throws Exception;

  CveData getApplicationsByCve(String cveId) throws Exception;

  AttacksResponse searchAttacks(AttacksFilterBody filters, int limit, int offset, String sort)
      throws Exception;

  ProtectData getProtectRules(String appId) throws Exception;

  RouteCoverageResponse getRouteCoverage(
      String appId, RouteCoverageBySessionIDAndMetadataRequest filter) throws Exception;

  ScanProject getScanProject(String projectName) throws Exception;
}
