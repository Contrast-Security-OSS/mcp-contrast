package com.contrast.labs.ai.mcp.contrast;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class StringValidationTest {

  private static final List<Path> STRING_TARGETS =
      List.of(
          Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/hints/HintGenerator.java"),
          Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/SCAService.java"),
          Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/ADRService.java"));

  private static final List<Path> COLLECTION_TARGETS =
      List.of(
          Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/hints/HintUtils.java"),
          Path.of(
              "src/main/java/com/contrast/labs/ai/mcp/contrast/mapper/VulnerabilityMapper.java"));

  @Test
  void stringPredicatesUseStringUtilsHasText() throws IOException {
    for (var path : STRING_TARGETS) {
      var source = Files.readString(path);
      assertThat(source).contains("StringUtils.hasText");
      assertThat(source).doesNotContain("== null ||").doesNotContain(".trim().isEmpty()");
    }
  }

  @Test
  void collectionChecksAvoidManualNullEmptiness() throws IOException {
    for (var path : COLLECTION_TARGETS) {
      var source = Files.readString(path);
      assertThat(source).contains("CollectionUtils.");
    }
  }
}
