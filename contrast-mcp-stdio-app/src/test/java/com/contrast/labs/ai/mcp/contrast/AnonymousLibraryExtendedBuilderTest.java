package com.contrast.labs.ai.mcp.contrast;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnonymousLibraryExtendedBuilderTest {

  @Test
  void builder_should_support_criticalVulnerabilities() {
    var library =
        AnonymousLibraryExtendedBuilder.validLibrary().withCriticalVulnerabilities(3).build();

    assertThat(library.getCriticalVulnerabilities()).isEqualTo(3);
  }

  @Test
  void builder_should_default_criticalVulnerabilities_to_zero() {
    var library = AnonymousLibraryExtendedBuilder.validLibrary().build();

    assertThat(library.getCriticalVulnerabilities()).isEqualTo(0);
  }
}
