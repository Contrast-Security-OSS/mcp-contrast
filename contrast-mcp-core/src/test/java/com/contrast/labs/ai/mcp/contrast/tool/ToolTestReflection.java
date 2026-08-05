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
package com.contrast.labs.ai.mcp.contrast.tool;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.util.ClassUtils;

/** Reflection helpers for tests that inspect the agent-visible tool contract. */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ToolTestReflection {

  // Keep aligned by hand with the sibling copy at ../aiml-services/services/
  // aiml-hosted-mcp-server/src/test/java/com/contrastsecurity/aiml/hosted/mcp/util/
  // ToolTestReflection.java.
  public static String toolDescription(Class<?> toolClass, String methodName) {
    return toolMethod(toolClass, methodName).getAnnotation(Tool.class).description();
  }

  public static Method toolMethod(Class<?> toolClass, String methodName) {
    return Arrays.stream(toolClass.getDeclaredMethods())
        .filter(candidate -> candidate.getName().equals(methodName))
        .filter(candidate -> candidate.isAnnotationPresent(Tool.class))
        .findFirst()
        .orElseThrow();
  }

  public static List<Method> toolMethods(String basePackage) {
    var scanner = new ClassPathScanningCandidateComponentProvider(false);
    scanner.addIncludeFilter((metadataReader, metadataReaderFactory) -> true);

    return scanner.findCandidateComponents(basePackage).stream()
        .map(BeanDefinition::getBeanClassName)
        .filter(Objects::nonNull)
        .map(
            className -> ClassUtils.resolveClassName(className, ClassUtils.getDefaultClassLoader()))
        .flatMap(toolClass -> Arrays.stream(toolClass.getDeclaredMethods()))
        .filter(method -> method.isAnnotationPresent(Tool.class))
        .sorted(Comparator.comparing(method -> method.getAnnotation(Tool.class).name()))
        .toList();
  }
}
