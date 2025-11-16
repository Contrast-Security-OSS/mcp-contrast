package com.contrast.labs.ai.mcp.contrast;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class StreamToListUsageTest {

  private static final List<Path> STREAM_CALL_SITES =
      List.of(
          Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/AssessService.java"),
          Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/FilterHelper.java"),
          Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/ADRService.java"),
          Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/data/AttackSummary.java"),
          Path.of(
              "src/main/java/com/contrast/labs/ai/mcp/contrast/mapper/VulnerabilityMapper.java"));

  @Test
  void usesToListInsteadOfCollectors() throws IOException {
    for (var path : STREAM_CALL_SITES) {
      var source = Files.readString(path);
      assertThat(source).doesNotContain(".collect(Collectors.toList())");
    }
  }
}
