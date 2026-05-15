package com.contrast.labs.ai.mcp.contrast.hints;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class HintProviderTest {

  @Test
  public void testGetHintsForRule() {
    // Test getting hints for SQL injection
    assertThat(HintProvider.getHintsForRule("sql-injection")).isNotEmpty();

    // Test getting hints for a non-existent rule
    assertThat(HintProvider.getHintsForRule("non-existent-rule")).isEmpty();
  }

  @Test
  public void testGetGeneralGuidance() {
    // General guidance should not be empty
    assertThat(HintProvider.getGeneralGuidance()).isNotEmpty();
  }

  @Test
  public void testGetAllHintsForRule() {
    // For SQL injection, we should get both general and specific hints
    assertThat(HintProvider.getAllHintsForRule("sql-injection").size())
        .isGreaterThan(HintProvider.getHintsForRule("sql-injection").size());
  }
}
