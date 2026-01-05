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

import java.util.List;

/**
 * Contract for all tool parameter classes. Implementations use ToolValidationContext to collect
 * errors/warnings during validation.
 */
public interface ToolParams {

  /**
   * Returns true if all validations passed (no errors).
   *
   * @return true if valid, false if validation errors exist
   */
  boolean isValid();

  /**
   * Validation errors that prevent execution. Empty list if valid.
   *
   * @return immutable list of error messages
   */
  List<String> errors();

  /**
   * Non-fatal warnings (e.g., applied defaults, deprecated values). Execution continues with
   * warnings.
   *
   * @return immutable list of warning messages
   */
  List<String> warnings();
}
