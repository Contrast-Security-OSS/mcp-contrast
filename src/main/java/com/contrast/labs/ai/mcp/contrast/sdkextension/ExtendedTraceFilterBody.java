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

import com.contrastsecurity.models.TraceFilterBody;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * Extended TraceFilterBody that adds fields not present in the SDK's base class.
 *
 * <p>The SDK's TraceFilterBody lacks the 'status' field that TeamServer's API supports. This class
 * adds it. The base class already has agentSessionId and metadataFilters fields.
 */
@Getter
@Setter
public class ExtendedTraceFilterBody extends TraceFilterBody {

  private Set<String> status;
}
