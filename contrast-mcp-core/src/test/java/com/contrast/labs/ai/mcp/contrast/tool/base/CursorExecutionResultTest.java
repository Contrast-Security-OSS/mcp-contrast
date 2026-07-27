package com.contrast.labs.ai.mcp.contrast.tool.base;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class CursorExecutionResultTest {

  @Test
  void constructor_should_substitute_an_empty_list_when_items_is_null() {
    var result = new CursorExecutionResult<String>(null, "cursor", true);

    assertThat(result.items()).isEmpty();
    assertThat(result.nextCursor()).isEqualTo("cursor");
    assertThat(result.hasMore()).isTrue();
  }

  @Test
  void constructor_should_copy_the_items_so_later_source_changes_are_not_visible() {
    var source = new ArrayList<>(List.of("first"));

    var result = CursorExecutionResult.of(source, null, false);
    source.add("second");

    assertThat(result.items()).containsExactly("first");
  }

  @Test
  void items_should_reject_modification() {
    var result = CursorExecutionResult.of(List.of("first"), null, false);

    assertThatThrownBy(() -> result.items().add("second"))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void of_should_carry_the_cursor_and_more_flag_through() {
    var result = CursorExecutionResult.of(List.of("a", "b"), "next-token", true);

    assertThat(result.items()).containsExactly("a", "b");
    assertThat(result.nextCursor()).isEqualTo("next-token");
    assertThat(result.hasMore()).isTrue();
  }

  @Test
  void empty_should_describe_a_terminal_page_with_no_items() {
    var result = CursorExecutionResult.empty();

    assertThat(result.items()).isEmpty();
    assertThat(result.nextCursor()).isNull();
    assertThat(result.hasMore()).isFalse();
  }
}
