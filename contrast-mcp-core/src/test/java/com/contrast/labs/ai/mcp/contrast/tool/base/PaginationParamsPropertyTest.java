package com.contrast.labs.ai.mcp.contrast.tool.base;

import static com.contrast.labs.ai.mcp.contrast.tool.validation.ValidationConstants.DEFAULT_PAGE_SIZE;
import static com.contrast.labs.ai.mcp.contrast.tool.validation.ValidationConstants.MAX_PAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;

import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.constraints.IntRange;

class PaginationParamsPropertyTest {

  private static final int SAFE_MAX_PAGE = 10_000;

  @Property
  void page_should_always_be_at_least_one(@ForAll Integer page, @ForAll Integer pageSize) {
    var params = PaginationParams.of(page, pageSize);

    assertThat(params.page()).isGreaterThanOrEqualTo(1);
  }

  @Property
  void pageSize_should_always_be_in_valid_range(@ForAll Integer page, @ForAll Integer pageSize) {
    var params = PaginationParams.of(page, pageSize);

    assertThat(params.pageSize()).isBetween(1, MAX_PAGE_SIZE);
  }

  @Property
  void pageSize_should_respect_custom_max_when_explicitly_provided(
      @ForAll Integer page,
      @ForAll @IntRange(min = 1) int pageSize,
      @ForAll @IntRange(min = 1, max = MAX_PAGE_SIZE) int maxPageSize) {
    var params = PaginationParams.of(page, pageSize, maxPageSize);

    assertThat(params.pageSize()).isBetween(1, maxPageSize);
  }

  @Property
  void offset_should_equal_page_minus_one_times_pageSize(
      @ForAll @IntRange(min = 1, max = SAFE_MAX_PAGE) int page,
      @ForAll @IntRange(min = 1, max = MAX_PAGE_SIZE) int pageSize) {
    var params = PaginationParams.of(page, pageSize);

    assertThat(params.offset()).isEqualTo((page - 1) * pageSize);
  }

  @Property
  void limit_should_always_equal_pageSize(@ForAll Integer page, @ForAll Integer pageSize) {
    var params = PaginationParams.of(page, pageSize);

    assertThat(params.limit()).isEqualTo(params.pageSize());
  }

  @Property
  void of_should_be_idempotent_with_no_extra_notices(
      @ForAll @IntRange(min = 1, max = SAFE_MAX_PAGE) int page,
      @ForAll @IntRange(min = 1, max = MAX_PAGE_SIZE) int pageSize) {
    var first = PaginationParams.of(page, pageSize);
    var second = PaginationParams.of(first.page(), first.pageSize());

    assertThat(second.page()).isEqualTo(first.page());
    assertThat(second.pageSize()).isEqualTo(first.pageSize());
    assertThat(second.offset()).isEqualTo(first.offset());
    assertThat(second.notices()).isEmpty();
  }

  @Property
  void notices_should_be_empty_when_inputs_are_valid(
      @ForAll @IntRange(min = 1, max = SAFE_MAX_PAGE) int page,
      @ForAll @IntRange(min = 1, max = MAX_PAGE_SIZE) int pageSize) {
    var params = PaginationParams.of(page, pageSize);

    assertThat(params.notices()).isEmpty();
  }

  @Property
  void notices_should_be_nonempty_when_page_corrected(
      @ForAll @IntRange(min = Integer.MIN_VALUE, max = 0) int page) {
    var params = PaginationParams.of(page, null);

    assertThat(params.notices()).isNotEmpty();
    assertThat(params.page()).isEqualTo(1);
  }

  @Property
  void notices_should_be_nonempty_when_pageSize_below_one(
      @ForAll @IntRange(min = Integer.MIN_VALUE, max = 0) int pageSize) {
    var params = PaginationParams.of(null, pageSize);

    assertThat(params.notices()).isNotEmpty();
    assertThat(params.pageSize()).isEqualTo(DEFAULT_PAGE_SIZE);
  }

  @Property
  void notices_should_be_nonempty_when_pageSize_above_max(
      @ForAll @IntRange(min = MAX_PAGE_SIZE + 1) int pageSize) {
    var params = PaginationParams.of(null, pageSize);

    assertThat(params.notices()).isNotEmpty();
    assertThat(params.pageSize()).isEqualTo(MAX_PAGE_SIZE);
  }

  @Property
  void isValid_should_always_return_true(@ForAll Integer page, @ForAll Integer pageSize) {
    assertThat(PaginationParams.of(page, pageSize).isValid()).isTrue();
  }

  @Property
  void notices_should_be_immutable(@ForAll Integer page, @ForAll Integer pageSize) {
    var params = PaginationParams.of(page, pageSize);

    org.assertj.core.api.Assertions.assertThatThrownBy(() -> params.notices().add("boom"))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
