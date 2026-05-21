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
package com.contrast.labs.ai.mcp.contrast.sdkextension.data.routecoverage;

import java.util.List;
import lombok.Data;

/**
 * Root class that represents the route coverage response. This class maps to the root level of the
 * JSON response.
 */
@Data
public class RouteCoverageResponse {

  private boolean success;
  private List<String> messages;
  private List<Route> routes;
}
