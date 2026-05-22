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
package com.contrast.labs.ai.mcp.contrast.tool.attack.params;

import com.contrast.labs.ai.mcp.contrast.tool.base.BaseToolParams;
import com.contrast.labs.ai.mcp.contrast.tool.validation.ToolValidationContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/** Validation parameters for GetProtectRulesTool. Validates required appId. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GetProtectRulesParams extends BaseToolParams {

  private String appId;

  public static GetProtectRulesParams of(String appId) {
    var params = new GetProtectRulesParams();
    var ctx = new ToolValidationContext();

    ctx.require(appId, "appId");
    params.appId = appId;

    params.setValidationResult(ctx);
    return params;
  }

  public String appId() {
    return appId;
  }
}
