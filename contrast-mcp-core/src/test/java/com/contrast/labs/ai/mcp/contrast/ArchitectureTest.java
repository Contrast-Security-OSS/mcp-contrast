/*
 * Copyright 2026 Contrast Security
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
import static com.tngtech.archunit.library.freeze.FreezingArchRule.freeze;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.stream.Collectors;

/**
 * Architectural rules for contrast-mcp-core.
 *
 * <p>Dependency and convention rules are wrapped in {@code freeze(...)}. Grandfathered violations
 * live in the committed store at {@code src/test/resources/archunit-store}; any new violation fails
 * immediately. The store is a ratchet: entries may only be removed, never added, without explicit
 * user approval (same policy as checkstyle suppressions). After fixing a grandfathered violation,
 * run {@code ./gradlew :contrast-mcp-core:test -ParchStoreUpdate} to shrink the store and commit
 * the result. Configuration lives in {@code src/test/resources/archunit.properties}.
 *
 * <p>The size and complexity guardrails at the bottom are NOT frozen because their violation
 * messages embed live counts. They ratchet manually like the coverage floors in the root
 * build.gradle: lower a threshold as improvements land, never raise one to make a build pass.
 */
@AnalyzeClasses(
    packages = "com.contrast.labs.ai.mcp.contrast",
    importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

  private ArchitectureTest() {}

  private static final String BASE = "com.contrast.labs.ai.mcp.contrast";

  // ── Layer enforcement ──────────────────────────────────────────────────────
  //
  // Dependency direction: tool -> {client, result, sdkextension, hints}
  //                       client -> {result, sdkextension}
  //                       result and sdkextension.data are leaf packages

  @ArchTest
  static final ArchRule results_should_not_depend_on_tools =
      freeze(
          noClasses()
              .that()
              .resideInAPackage(BASE + ".result..")
              .should()
              .dependOnClassesThat()
              .resideInAPackage(BASE + ".tool..")
              .as("Result models should not depend on tool implementations"));

  @ArchTest
  static final ArchRule data_models_should_not_depend_on_tools =
      freeze(
          noClasses()
              .that()
              .resideInAPackage(BASE + ".sdkextension.data..")
              .should()
              .dependOnClassesThat()
              .resideInAPackage(BASE + ".tool..")
              .as("Data models should not depend on tool implementations"));

  @ArchTest
  static final ArchRule hints_should_not_depend_on_tools =
      freeze(
          noClasses()
              .that()
              .resideInAPackage(BASE + ".hints..")
              .should()
              .dependOnClassesThat()
              .resideInAPackage(BASE + ".tool..")
              .as("Hints should not depend on tool implementations"));

  // ── SDK containment ────────────────────────────────────────────────────────
  //
  // Only the client and sdkextension packages may use the Contrast SDK
  // directly. Tools and result models should code against our own client
  // abstraction so the published core stays decoupled from SDK churn. The
  // grandfathered violations in the store are the burn-down list.

  @ArchTest
  static final ArchRule only_client_and_sdkextension_should_use_the_contrast_sdk =
      freeze(
          noClasses()
              .that()
              .resideOutsideOfPackages(BASE + ".client..", BASE + ".sdkextension..")
              .should()
              .dependOnClassesThat()
              .resideInAPackage("com.contrastsecurity..")
              .as("Only client and sdkextension may depend on the Contrast SDK directly"));

  // ── Dependency cycles ──────────────────────────────────────────────────────
  //
  // Top-level slices (tool, client, result, sdkextension, hints) must form a
  // DAG. Remaining grandfathered cycles are tracked in the store.

  @ArchTest
  static final ArchRule top_level_packages_should_be_cycle_free =
      freeze(
          slices()
              .matching(BASE + ".(*)..")
              .should()
              .beFreeOfCycles()
              .as("Top-level packages should be free of dependency cycles"));

  // ── Tool domain isolation ──────────────────────────────────────────────────
  //
  // Each tool domain (vulnerability, application, library, attack, server,
  // sast, coverage) should only depend on tool.base and tool.validation,
  // never on sibling domains.

  private static final String[] TOOL_DOMAINS = {
    "vulnerability", "application", "library", "attack", "server", "sast", "coverage"
  };

  private static String toolDomainOf(JavaClass javaClass) {
    var pkg = javaClass.getPackageName();
    for (var domain : TOOL_DOMAINS) {
      var prefix = BASE + ".tool." + domain;
      if (pkg.equals(prefix) || pkg.startsWith(prefix + ".")) {
        return domain;
      }
    }
    return null;
  }

  @ArchTest
  static final ArchRule tool_domains_should_not_cross_import =
      freeze(
          classes()
              .should(
                  new ArchCondition<JavaClass>("not depend on sibling tool domains") {
                    @Override
                    public void check(JavaClass javaClass, ConditionEvents events) {
                      var sourceDomain = toolDomainOf(javaClass);
                      if (sourceDomain == null) {
                        return;
                      }
                      javaClass.getDirectDependenciesFromSelf().stream()
                          .filter(
                              dep -> {
                                var targetDomain = toolDomainOf(dep.getTargetClass());
                                return targetDomain != null && !targetDomain.equals(sourceDomain);
                              })
                          .forEach(
                              dep ->
                                  events.add(
                                      SimpleConditionEvent.violated(
                                          javaClass, dep.getDescription())));
                    }
                  })
              .as(
                  "Tool domains should not depend on sibling domains"
                      + " (extract shared code to tool.base)"));

  // ── Structural conventions ─────────────────────────────────────────────────

  @ArchTest
  static final ArchRule params_should_live_in_params_package =
      freeze(
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
              .as("Domain-specific Params classes should live in a params sub-package"));

  @ArchTest
  static final ArchRule client_classes_should_live_in_client_package =
      freeze(
          classes()
              .that()
              .haveSimpleNameEndingWith("Client")
              .and()
              .resideInAPackage(BASE + "..")
              .should()
              .resideInAPackage(BASE + ".client..")
              .as("Client classes should live in the client package"));

  // ── Size and complexity guardrails ─────────────────────────────────────────
  //
  // Not frozen: violation messages embed live counts, so store entries would
  // stop matching as soon as a count changes. Thresholds sit a few points
  // above current maximums and ratchet manually like the coverage floors in
  // the root build.gradle: lower as improvements land, never raise.

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
                          .filter(m -> !"toString".equals(m.getName()))
                          .filter(m -> !"hashCode".equals(m.getName()))
                          .filter(m -> !"equals".equals(m.getName()))
                          .filter(m -> !"builder".equals(m.getName()))
                          .filter(m -> !"of".equals(m.getName()))
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
