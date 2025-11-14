package com.contrast.labs.ai.mcp.contrast.hints;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class HintProviderTest {

  @Test
  public void testGetHintsForRule() {
    // Test getting hints for SQL injection
    assertFalse(HintProvider.getHintsForRule("sql-injection").isEmpty());

    // Test getting hints for a non-existent rule
    assertTrue(HintProvider.getHintsForRule("non-existent-rule").isEmpty());
  }

  @Test
  public void testGetGeneralGuidance() {
    // General guidance should not be empty
    assertFalse(HintProvider.getGeneralGuidance().isEmpty());
  }

  @Test
  public void testGetAllHintsForRule() {
    // For SQL injection, we should get both general and specific hints
    assertTrue(
        HintProvider.getAllHintsForRule("sql-injection").size()
            > HintProvider.getHintsForRule("sql-injection").size());
  }
}
