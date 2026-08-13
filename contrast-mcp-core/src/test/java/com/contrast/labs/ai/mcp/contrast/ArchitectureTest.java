/*
 * Copyright 2025 Contrast Security
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.contrast.labs.ai.mcp.contrast;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Set;
import java.util.stream.Collectors;

@AnalyzeClasses(
    packages = "com.contrast.labs.ai.mcp.contrast",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  private static final String BASE = "com.contrast.labs.ai.mcp.contrast";

  // ── Layer enforcement ──────────────────────────────────────────────────────
  //
  // Dependency direction: tool -> {client, result, sdkextension, hints}
  //                       client -> {result, sdkextension}
  //                       result and sdkextension.data are leaf packages
  //
  // Known violation (mcp-r7xbx): AttackSummary and ServerSummary call
  // FilterHelper.formatTimestamp from tool.base. Fix by moving the timestamp
  // formatter to a shared utility outside the tool package.

  @ArchTest
  static final ArchRule results_should_not_depend_on_tools =
      noClasses()
          .that()
          .resideInAPackage(BASE + ".result..")
          .and()
          .haveNameNotMatching(".*AttackSummary.*")
          .and()
          .haveNameNotMatching(".*ServerSummary.*")
          .should()
          .dependOnClassesThat()
          .resideInAPackage(BASE + ".tool..")
          .as(
              "Result models should not depend on tool implementations"
                  + " (AttackSummary and ServerSummary are known exceptions, see mcp-r7xbx)");

  @ArchTest
  static final ArchRule data_models_should_not_depend_on_tools =
      noClasses()
          .that()
          .resideInAPackage(BASE + ".sdkextension.data..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage(BASE + ".tool..");

  @ArchTest
  static final ArchRule hints_should_not_depend_on_tools =
      noClasses()
          .that()
          .resideInAPackage(BASE + ".hints..")
          .should()
          .dependOnClassesThat()
          .resideInAPackage(BASE + ".tool..");

  // ── Dependency cycles ──────────────────────────────────────────────────────
  //
  // Top-level slices (tool, client, result, sdkextension, hints) must form a
  // DAG. The one known cycle is result -> tool.base (FilterHelper), tracked
  // by mcp-r7xbx. Once that is fixed, remove the ignoreDependency call.

  private static final DescribedPredicate<JavaClass> IN_RESULT_PACKAGE =
      new DescribedPredicate<>("in result package") {
        @Override
        public boolean test(JavaClass c) {
          return c.getPackageName().startsWith(BASE + ".result");
        }
      };

  private static final DescribedPredicate<JavaClass> IN_TOOL_PACKAGE =
      new DescribedPredicate<>("in tool package") {
        @Override
        public boolean test(JavaClass c) {
          return c.getPackageName().startsWith(BASE + ".tool");
        }
      };

  @ArchTest
  static final ArchRule top_level_packages_should_be_cycle_free =
      slices()
          .matching(BASE + ".(*)..")
          .should()
          .beFreeOfCycles()
          .ignoreDependency(IN_RESULT_PACKAGE, IN_TOOL_PACKAGE)
          .as("Top-level packages should be free of dependency cycles");

  // ── Tool domain isolation ──────────────────────────────────────────────────
  //
  // Each tool domain (vulnerability, application, library, attack, server,
  // sast, coverage) should only depend on tool.base and tool.validation,
  // never on sibling domains.
  //
  // Known violation (mcp-ax84i): tool.coverage depends on tool.application
  // for ApplicationLicenseDiscriminator. Fix by moving it to tool.base.

  private static final String[] TOOL_DOMAINS = {
    "vulnerability", "application", "library", "attack", "server", "sast", "coverage"
  };

  private static final Set<String> KNOWN_CROSS_DOMAIN_EXCEPTIONS = Set.of("coverage->application");

  @ArchTest
  static void tool_domains_should_not_cross_import(JavaClasses classes) {
    for (var domain : TOOL_DOMAINS) {
      for (var other : TOOL_DOMAINS) {
        if (domain.equals(other)) continue;
        var key = domain + "->" + other;
        if (KNOWN_CROSS_DOMAIN_EXCEPTIONS.contains(key)) continue;

        noClasses()
            .that()
            .resideInAPackage(BASE + ".tool." + domain + "..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(BASE + ".tool." + other + "..")
            .as(
                "tool."
                    + domain
                    + " should not depend on tool."
                    + other
                    + " (extract shared code to tool.base)")
            .check(classes);
      }
    }
  }

  // ── Structural conventions ─────────────────────────────────────────────────

  @ArchTest
  static final ArchRule params_should_live_in_params_package =
      classes()
          .that()
          .haveSimpleNameEndingWith("Params")
          .and()
          .resideInAPackage(BASE + ".tool..")
          .and()
          .resideOutsideOfPackage(BASE + ".tool.base..")
          .and()
          .resideOutsideOfPackage(BASE + ".tool.validation..")
          .should()
          .resideInAPackage("..params..")
          .as("Domain-specific Params classes should live in a params sub-package");

  @ArchTest
  static final ArchRule client_classes_should_live_in_client_package =
      classes()
          .that()
          .haveSimpleNameEndingWith("Client")
          .and()
          .resideInAPackage(BASE + "..")
          .should()
          .resideInAPackage(BASE + ".client..");

  // ── Size and complexity guardrails ─────────────────────────────────────────
  //
  // Thresholds sit a few points above current maximums so one new class or
  // method does not fail an unrelated PR, matching the headroom convention
  // used by the coverage floors in the root build.gradle.

  private static final int MAX_CLASSES_PER_PACKAGE = 20;

  @ArchTest
  static void packages_should_not_be_bloated(JavaClasses classes) {
    var packages =
        classes.stream()
            .filter(c -> !c.isInnerClass())
            .filter(c -> !c.getSimpleName().contains("$"))
            .filter(c -> !c.getName().contains("$"))
            .collect(Collectors.groupingBy(JavaClass::getPackageName, Collectors.counting()));

    packages.forEach(
        (pkg, count) -> {
          if (count > MAX_CLASSES_PER_PACKAGE) {
            throw new AssertionError(
                "Package "
                    + pkg
                    + " has "
                    + count
                    + " direct classes (max "
                    + MAX_CLASSES_PER_PACKAGE
                    + "). Split into sub-packages.");
          }
        });
  }

  private static final int MAX_NON_ACCESSOR_METHODS = 28;

  @ArchTest
  static final ArchRule classes_should_not_be_god_objects =
      classes()
          .should(
              new ArchCondition<JavaClass>(
                  "not have more than "
                      + MAX_NON_ACCESSOR_METHODS
                      + " non-accessor declared methods") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                  if (javaClass.isEnum()) return;
                  if (javaClass.isInnerClass()) return;
                  if (javaClass.getSimpleName().endsWith("Builder")) return;

                  long count =
                      javaClass.getMethods().stream()
                          .filter(m -> m.getOwner().equals(javaClass))
                          .filter(m -> !m.getName().startsWith("get"))
                          .filter(m -> !m.getName().startsWith("set"))
                          .filter(m -> !m.getName().startsWith("is"))
                          .filter(m -> !m.getName().equals("toString"))
                          .filter(m -> !m.getName().equals("hashCode"))
                          .filter(m -> !m.getName().equals("equals"))
                          .filter(m -> !m.getName().equals("builder"))
                          .filter(m -> !m.getName().equals("of"))
                          .count();

                  if (count > MAX_NON_ACCESSOR_METHODS) {
                    events.add(
                        SimpleConditionEvent.violated(
                            javaClass,
                            javaClass.getName()
                                + " has "
                                + count
                                + " non-accessor methods (max "
                                + MAX_NON_ACCESSOR_METHODS
                                + ")"));
                  }
                }
              });

  private static final int MAX_FAN_OUT = 30;

  @ArchTest
  static final ArchRule classes_should_not_have_excessive_fan_out =
      classes()
          .should(
              new ArchCondition<JavaClass>(
                  "not depend on more than " + MAX_FAN_OUT + " project classes") {
                @Override
                public void check(JavaClass javaClass, ConditionEvents events) {
                  if (javaClass.isEnum()) return;
                  if (javaClass.isInnerClass()) return;

                  long fanOut =
                      javaClass.getDirectDependenciesFromSelf().stream()
                          .map(dep -> dep.getTargetClass())
                          .filter(target -> target.getPackageName().startsWith(BASE))
                          .distinct()
                          .count();

                  if (fanOut > MAX_FAN_OUT) {
                    events.add(
                        SimpleConditionEvent.violated(
                            javaClass,
                            javaClass.getName()
                                + " depends on "
                                + fanOut
                                + " project classes (max "
                                + MAX_FAN_OUT
                                + ")"));
                  }
                }
              });
}
