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

package com.contrast.labs.ai.mcp.contrast.sdkextension.data.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Application overrides Lombok's generated getters for its collection fields so callers never
 * receive null. These tests hold that contract, since a plain Lombok getter would satisfy every
 * other use of the class while quietly reintroducing the null.
 */
class ApplicationTest {

  static Stream<Arguments> collectionGetters() {
    return Stream.of(
        Arguments.of("roles", (Function<Application, List<?>>) Application::getRoles),
        Arguments.of("tags", (Function<Application, List<?>>) Application::getTags),
        Arguments.of("techs", (Function<Application, List<?>>) Application::getTechs),
        Arguments.of("policies", (Function<Application, List<?>>) Application::getPolicies),
        Arguments.of(
            "metadataEntities", (Function<Application, List<?>>) Application::getMetadataEntities),
        Arguments.of(
            "validationErrorFields",
            (Function<Application, List<?>>) Application::getValidationErrorFields),
        Arguments.of(
            "missingRequiredFields",
            (Function<Application, List<?>>) Application::getMissingRequiredFields));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("collectionGetters")
  void getter_should_return_an_empty_list_when_the_field_was_never_set(
      String field, Function<Application, List<?>> getter) {
    assertThat(getter.apply(new Application())).as(field).isNotNull().isEmpty();
  }

  @Test
  void getRoles_should_return_the_configured_values() {
    var application = new Application();
    application.setRoles(List.of("admin", "viewer"));

    assertThat(application.getRoles()).containsExactly("admin", "viewer");
  }

  @Test
  void getTags_should_return_the_configured_values() {
    var application = new Application();
    application.setTags(List.of("prod"));

    assertThat(application.getTags()).containsExactly("prod");
  }

  @Test
  void getTechs_should_return_the_configured_values() {
    var application = new Application();
    application.setTechs(List.of("Java", "Spring"));

    assertThat(application.getTechs()).containsExactly("Java", "Spring");
  }

  @Test
  void getPolicies_should_return_the_configured_values() {
    var application = new Application();
    application.setPolicies(List.of("owasp-top-10"));

    assertThat(application.getPolicies()).containsExactly("owasp-top-10");
  }

  @Test
  void getMetadataEntities_should_return_the_configured_values() {
    var application = new Application();
    var metadata = new Metadata();
    application.setMetadataEntities(List.of(metadata));

    assertThat(application.getMetadataEntities()).containsExactly(metadata);
  }

  @Test
  void getValidationErrorFields_should_return_the_configured_values() {
    var application = new Application();
    var field = new Field();
    application.setValidationErrorFields(List.of(field));

    assertThat(application.getValidationErrorFields()).containsExactly(field);
  }

  @Test
  void getMissingRequiredFields_should_return_the_configured_values() {
    var application = new Application();
    var field = new Field();
    application.setMissingRequiredFields(List.of(field));

    assertThat(application.getMissingRequiredFields()).containsExactly(field);
  }
}
