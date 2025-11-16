package com.contrast.labs.ai.mcp.contrast;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class CollectionGuardTest {

  private static final List<Path> COLLECTION_SOURCES =
      List.of(
          Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/VulnerabilityFilterParams.java"),
          Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/utils/PaginationHandler.java"),
          Path.of("src/main/java/com/contrast/labs/ai/mcp/contrast/RouteCoverageService.java"));

  @Test
  void avoidsManualCollectionNullChecks() throws IOException {
    for (var path : COLLECTION_SOURCES) {
      var source = Files.readString(path);
      assertThat(source).doesNotContain("!= null && !").doesNotContain("== null ||");
    }
  }
}
