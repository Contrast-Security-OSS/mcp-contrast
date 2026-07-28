package com.contrast.labs.ai.mcp.contrast.result;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class RunBookEnumTest {

  static Stream<String> everyAttackType() {
    return Stream.of(RunBookEnum.values()).flatMap(runbook -> runbook.getAttackType().stream());
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("everyAttackType")
  void getRunBookEnumForTypeID_should_find_the_owning_runbook_for_every_declared_attack_type(
      String attackType) {
    var owner =
        Stream.of(RunBookEnum.values())
            .filter(runbook -> runbook.getAttackType().contains(attackType))
            .findFirst()
            .orElseThrow();

    assertThat(RunBookEnum.getRunBookEnumForTypeID(attackType)).contains(owner);
  }

  /**
   * The lookup returns the first match in declaration order, so a type claimed twice would make one
   * runbook unreachable without any compile or runtime error.
   */
  @Test
  void attack_types_should_be_claimed_by_exactly_one_runbook() {
    var seen = new ArrayList<String>();
    everyAttackType().map(type -> type.toLowerCase(Locale.ROOT)).forEach(seen::add);

    assertThat(seen).doesNotHaveDuplicates();
  }

  @Test
  void getRunBookEnumForTypeID_should_match_a_type_declared_after_the_first_in_its_list() {
    var aliases = RunBookEnum.COMMAND_INJECTION.getAttackType();
    var lastAlias = aliases.get(aliases.size() - 1);

    assertThat(aliases).hasSizeGreaterThan(1);
    assertThat(RunBookEnum.getRunBookEnumForTypeID(lastAlias))
        .contains(RunBookEnum.COMMAND_INJECTION);
  }

  @Test
  void getRunBookEnumForTypeID_should_ignore_case() {
    assertThat(RunBookEnum.getRunBookEnumForTypeID("SQL-INJECTION"))
        .contains(RunBookEnum.SQL_INJECTION);
    assertThat(RunBookEnum.getRunBookEnumForTypeID("Sql-Injection"))
        .contains(RunBookEnum.SQL_INJECTION);
  }

  @Test
  void getRunBookEnumForTypeID_should_return_empty_for_an_unknown_type() {
    assertThat(RunBookEnum.getRunBookEnumForTypeID("not-a-rule")).isEmpty();
  }

  @Test
  void getRunBookEnumForTypeID_should_return_empty_for_null() {
    assertThat(RunBookEnum.getRunBookEnumForTypeID(null)).isEmpty();
  }

  @Test
  void getRunBookEnumForTypeID_should_not_match_on_a_partial_type() {
    assertThat(RunBookEnum.getRunBookEnumForTypeID("cmd")).isEmpty();
    assertThat(RunBookEnum.getRunBookEnumForTypeID("cmd-injection-extra")).isEmpty();
  }

  @ParameterizedTest
  @EnumSource(RunBookEnum.class)
  void getAttackType_should_list_at_least_one_type_for_every_runbook(RunBookEnum runbook) {
    assertThat(runbook.getAttackType())
        .isNotEmpty()
        .allSatisfy(type -> assertThat(type).isNotBlank());
  }

  @ParameterizedTest
  @EnumSource(RunBookEnum.class)
  void getRunBook_should_return_markdown_with_a_title_and_triage_guidance(RunBookEnum runbook) {
    assertThat(runbook.getRunBook())
        .isNotBlank()
        .startsWith("---")
        .contains("layout: runbook")
        .contains("# ")
        .contains("## Post-Incident Activities");
  }
}
