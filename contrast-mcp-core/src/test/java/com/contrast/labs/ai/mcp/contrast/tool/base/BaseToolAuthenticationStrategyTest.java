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

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

class BaseToolAuthenticationStrategyTest {

  private static final List<Path> PIPELINE_SOURCES =
      List.of(
          Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/tool/base/SingleTool.java"),
          Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/tool/base/PaginatedTool.java"));

  @Test
  void executePipeline_should_remain_noop_when_authentication_strategy_is_not_configured() {
    var events = new ArrayList<String>();
    var tool = new RecordingSingleTool(events);

    var result = tool.executePipeline(TestParams::valid, null);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.data()).isEqualTo("ok");
    assertThat(tool.isAuthenticationStrategyConfigured()).isFalse();
    assertThat(events).containsExactly("doExecute");
  }

  @Test
  void executePipeline_should_bind_configured_strategy_before_doExecute_and_close_afterward() {
    var events = new ArrayList<String>();
    var tool = new RecordingSingleTool(events);
    var context = new ToolContext(java.util.Map.of("requestId", "req-123"));
    tool.setAuthenticationStrategy(
        toolContext -> {
          events.add("authenticate");
          assertThat(toolContext).isSameAs(context);
          return () -> events.add("close");
        });

    var result = tool.executePipeline(TestParams::valid, context);

    assertThat(result.isSuccess()).isTrue();
    assertThat(tool.isAuthenticationStrategyConfigured()).isTrue();
    assertThat(events).containsExactly("authenticate", "doExecute", "close");
  }

  @Test
  void paginatedExecutePipeline_should_forward_toolContext_to_configured_strategy() {
    var events = new ArrayList<String>();
    var tool = new RecordingPaginatedTool(events);
    var context = new ToolContext(java.util.Map.of("requestId", "req-456"));
    tool.setAuthenticationStrategy(
        toolContext -> {
          events.add("authenticate");
          assertThat(toolContext).isSameAs(context);
          return () -> events.add("close");
        });

    var result = tool.executePipeline(1, 10, TestParams::valid, context);

    assertThat(result.isSuccess()).isTrue();
    assertThat(result.items()).containsExactly("ok");
    assertThat(events).containsExactly("authenticate", "doExecute", "close");
  }

  @Test
  void executePipeline_should_fail_closed_when_configured_strategy_returns_null_scope() {
    var events = new ArrayList<String>();
    var tool = new RecordingSingleTool(events);
    tool.setAuthenticationStrategy(
        toolContext -> {
          events.add("authenticate");
          return null;
        });

    var result = tool.executePipeline(TestParams::valid, null);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.errors())
        .singleElement()
        .satisfies(error -> assertThat(error).startsWith("An internal error occurred (ref: "));
    assertThat(events).containsExactly("authenticate");
  }

  @Test
  void executePipeline_should_return_error_when_authentication_strategy_throws() {
    var events = new ArrayList<String>();
    var tool = new RecordingSingleTool(events);
    var context = new ToolContext(java.util.Map.of("requestId", "req-123"));
    tool.setAuthenticationStrategy(
        toolContext -> {
          events.add("authenticate");
          throw new IllegalStateException("raw-token-value must not reach tool output");
        });

    var result = tool.executePipeline(TestParams::valid, context);

    assertThat(result.isSuccess()).isFalse();
    assertThat(result.found()).isFalse();
    assertThat(result.data()).isNull();
    assertThat(result.errors()).hasSize(1);
    assertThat(result.errors().get(0)).matches("An internal error occurred \\(ref: [0-9a-f]{8}\\)");
    assertThat(String.join(" ", result.errors())).doesNotContain("raw-token-value");
    assertThat(events).containsExactly("authenticate");
  }

  @Test
  void executePipeline_unexpected_error_logs_should_not_attach_stack_traces() throws Exception {
    for (var sourcePath : PIPELINE_SOURCES) {
      var source = Files.readString(sourcePath, StandardCharsets.UTF_8);

      assertThat(source).doesNotContain(".setCause(e)");
    }
  }

  @Test
  void isAuthenticationStrategyConfigured_should_expose_only_boolean_state() {
    var tool = new RecordingSingleTool(new ArrayList<>());

    assertThat(tool.isAuthenticationStrategyConfigured()).isFalse();

    tool.setAuthenticationStrategy(toolContext -> () -> {});

    assertThat(tool.isAuthenticationStrategyConfigured()).isTrue();
  }

  private static final class RecordingSingleTool extends SingleTool<TestParams, String> {

    private final List<String> events;

    private RecordingSingleTool(List<String> events) {
      this.events = events;
    }

    @Override
    protected String doExecute(TestParams params, WarningCollector collector) {
      events.add("doExecute");
      return "ok";
    }
  }

  private static final class RecordingPaginatedTool extends PaginatedTool<TestParams, String> {

    private final List<String> events;

    private RecordingPaginatedTool(List<String> events) {
      this.events = events;
    }

    @Override
    protected ExecutionResult<String> doExecute(
        PaginationParams pagination, TestParams params, WarningCollector collector) {
      events.add("doExecute");
      return ExecutionResult.of(List.of("ok"), 1);
    }
  }

  private static final class TestParams implements ToolParams {

    private static TestParams valid() {
      return new TestParams();
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public List<String> errors() {
      return List.of();
    }

    @Override
    public List<String> warnings() {
      return List.of();
    }
  }
}
