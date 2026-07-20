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
package com.contrast.labs.ai.mcp.contrast.sdkextension.data.server;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/** Wire request body for the TeamServer server-filter endpoint. */
@Value
@Builder
public class ServerFilterBody {
  // TeamServer's POST filter contract intentionally uses this plural-plural wire spelling.
  List<String> applicationsIds;
  List<String> logLevels;
  List<String> tags;
  List<String> agentVersions;
  List<String> serverEnvironments;
  String q;
  String quickFilter;
}
