package com.contrast.labs.ai.mcp.contrast.tool.base;

import static com.contrast.labs.ai.mcp.contrast.tool.validation.ValidationConstants.DEFAULT_PAGE_SIZE;
import static com.contrast.labs.ai.mcp.contrast.tool.validation.ValidationConstants.MAX_PAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;
import net.jqwik.api.constraints.StringLength;

class CursorPaginationParamsPropertyTest {

  @Property
  void pageSize_should_always_be_in_valid_range(@ForAll String cursor, @ForAll Integer pageSize) {
    var params = CursorPaginationParams.of(cursor, pageSize);

    assertThat(params.pageSize()).isBetween(1, MAX_PAGE_SIZE);
  }

  @Property
  void pageSize_should_respect_custom_max(
      @ForAll String cursor,
      @ForAll Integer pageSize,
      @ForAll @IntRange(min = 1, max = MAX_PAGE_SIZE) int maxPageSize) {
    var params = CursorPaginationParams.of(cursor, pageSize, maxPageSize);

    assertThat(params.pageSize()).isBetween(1, maxPageSize);
  }

  @Property
  void limit_should_always_equal_pageSize(@ForAll String cursor, @ForAll Integer pageSize) {
    var params = CursorPaginationParams.of(cursor, pageSize);

    assertThat(params.limit()).isEqualTo(params.pageSize());
  }

  @Property
  void defaultSize_should_be_min_of_default_and_max(
      @ForAll @IntRange(min = 1, max = MAX_PAGE_SIZE) int maxPageSize) {
    var params = CursorPaginationParams.of(null, null, maxPageSize);
    int expectedDefault = Math.min(DEFAULT_PAGE_SIZE, maxPageSize);

    assertThat(params.pageSize()).isEqualTo(expectedDefault);
  }

  @Property
  void blank_cursor_should_normalize_to_null(@ForAll @StringLength(max = 10) String whitespace) {
    var blanked = whitespace.replaceAll("\\S", " ");
    var params = CursorPaginationParams.of(blanked, null);

    assertThat(params.cursor()).isNull();
  }

  @Property
  void cursor_normalization_should_be_idempotent(@ForAll String cursor) {
    var first = CursorPaginationParams.of(cursor, null);
    var second = CursorPaginationParams.of(first.cursor(), null);

    assertThat(second.cursor()).isEqualTo(first.cursor());
  }

  @Property
  void cursorPresence_should_be_total(@ForAll String cursor, @ForAll Integer pageSize) {
    var params = CursorPaginationParams.of(cursor, pageSize);
    var presence = params.cursorPresence();

    assertThat(presence).isIn("present", "absent");
    if (params.cursor() != null) {
      assertThat(presence).isEqualTo("present");
    } else {
      assertThat(presence).isEqualTo("absent");
    }
  }

  @Property
  void notices_should_be_empty_when_inputs_valid(
      @ForAll @IntRange(min = 1, max = MAX_PAGE_SIZE) int pageSize) {
    var params = CursorPaginationParams.of(null, pageSize);

    assertThat(params.notices()).isEmpty();
  }

  @Property
  void notices_should_be_nonempty_when_pageSize_below_one(
      @ForAll @IntRange(min = Integer.MIN_VALUE, max = 0) int pageSize) {
    var params = CursorPaginationParams.of(null, pageSize);

    assertThat(params.notices()).isNotEmpty();
  }

  @Property
  void notices_should_be_nonempty_when_pageSize_above_max(
      @ForAll @IntRange(min = MAX_PAGE_SIZE + 1) int pageSize) {
    var params = CursorPaginationParams.of(null, pageSize);

    assertThat(params.notices()).isNotEmpty();
    assertThat(params.pageSize()).isEqualTo(MAX_PAGE_SIZE);
  }

  @Property
  void isValid_should_always_return_true(@ForAll String cursor, @ForAll Integer pageSize) {
    assertThat(CursorPaginationParams.of(cursor, pageSize).isValid()).isTrue();
  }

  @Property
  void notices_should_be_immutable(@ForAll String cursor, @ForAll Integer pageSize) {
    var params = CursorPaginationParams.of(cursor, pageSize);

    assertThatThrownBy(() -> params.notices().add("boom"))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
