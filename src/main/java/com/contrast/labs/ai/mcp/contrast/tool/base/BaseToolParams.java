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
package com.contrast.labs.ai.mcp.contrast.tool.base;

import com.contrast.labs.ai.mcp.contrast.tool.validation.ToolValidationContext;
import java.util.List;

/**
 * Base class for tool parameters that provides the ToolParams boilerplate. Subclasses store
 * domain-specific fields and use composition with ToolValidationContext for validation.
 *
 * <p>Usage pattern:
 *
 * <pre>{@code
 * public class MyParams extends BaseToolParams {
 *   private String myField;
 *
 *   public static MyParams of(String value) {
 *     var params = new MyParams();
 *     var ctx = new ToolValidationContext();  // Composition, not inheritance
 *
 *     ctx.require(value, "myField");
 *     params.myField = value;
 *
 *     params.setValidationResult(ctx);  // Transfer errors/warnings
 *     return params;
 *   }
 *
 *   public String myField() { return myField; }
 * }
 * }</pre>
 */
public abstract class BaseToolParams implements ToolParams {

  private List<String> errors = List.of();
  private List<String> warnings = List.of();

  @Override
  public boolean isValid() {
    return errors.isEmpty();
  }

  @Override
  public List<String> errors() {
    return errors;
  }

  @Override
  public List<String> warnings() {
    return warnings;
  }

  /**
   * Transfer validation results from a ToolValidationContext to this params instance. Call this at
   * the end of static factory methods after all validation is complete.
   *
   * @param ctx the validation context containing errors and warnings
   */
  protected void setValidationResult(ToolValidationContext ctx) {
    this.errors = ctx.errors();
    this.warnings = ctx.warnings();
  }
}
