package com.contrast.labs.ai.mcp.contrast;

import static org.assertj.core.api.Assertions.assertThat;

import com.contrast.labs.ai.mcp.contrast.mapper.VulnerabilityMapper;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKExtension;
import com.contrast.labs.ai.mcp.contrast.sdkextension.SDKHelper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

class LoggingAnnotationsTest {

  private static final List<Class<?>> LOG_TARGET_TYPES =
      List.of(
          AssessService.class,
          ADRService.class,
          SCAService.class,
          SastService.class,
          RouteCoverageService.class,
          PromptService.class,
          SDKHelper.class,
          SDKExtension.class,
          VulnerabilityMapper.class,
          FilterHelper.class,
          AttackFilterParams.class,
          VulnerabilityFilterParams.class);

  private static final List<Path> SOURCE_PATHS =
      List.of(
          Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/AssessService.java"),
          Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/ADRService.java"),
          Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/SCAService.java"),
          Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/SastService.java"),
          Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/RouteCoverageService.java"),
          Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/PromptService.java"),
          Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/sdkextension/SDKHelper.java"),
          Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/sdkextension/SDKExtension.java"),
          Path.of(
              "src/main/java/com/contrast/labs/ai/mcp/contrast/mapper/VulnerabilityMapper.java"),
          Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/FilterHelper.java"),
          Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/AttackFilterParams.java"),
          Path.of(
              "src/main/java/com/contrast/labs/ai/mcp/contrast/VulnerabilityFilterParams.java"));

  @Test
  void classesExposeSlf4jLogField() {
    for (var type : LOG_TARGET_TYPES) {
      var logField =
          Arrays.stream(type.getDeclaredFields())
              .filter(field -> field.getName().equals("log"))
              .findFirst();

      assertThat(logField)
          .as("Expected Lombok-generated log field on %s", type.getName())
          .isPresent()
          .get()
          .satisfies(field -> assertThat(field.getType()).isEqualTo(Logger.class));
    }
  }

  @Test
  void sourcesDoNotUseManualLoggerFactory() throws IOException {
    for (var path : SOURCE_PATHS) {
      var source = Files.readString(path);
      assertThat(source).doesNotContain("LoggerFactory.getLogger");
    }
  }
}
