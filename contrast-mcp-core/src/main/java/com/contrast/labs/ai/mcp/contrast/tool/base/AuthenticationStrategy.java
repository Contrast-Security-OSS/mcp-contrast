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

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.lang.Nullable;

/** Binds transport-specific authentication context for one tool execution. */
@FunctionalInterface
public interface AuthenticationStrategy {

  /**
   * Authenticates the current tool call and returns a scope that clears request-local state.
   *
   * @param toolContext Spring AI context for the current tool invocation, or null in local stdio
   *     mode/tests that do not need request context
   * @return scope closed after tool execution completes
   * @throws Exception when authentication context is missing or invalid
   */
  AutoCloseable authenticate(@Nullable ToolContext toolContext) throws Exception;
}
