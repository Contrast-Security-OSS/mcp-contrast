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

package com.contrast.labs.build;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses {@code .env}-style files into key-value pairs.
 *
 * <p>Handles comment lines, {@code export} prefixes, and single/double-quoted values. Values from
 * real environment variables take precedence when resolved through {@link #resolve}.
 */
public final class EnvFileParser {

  private EnvFileParser() {}

  /**
   * Parses an env file into a map of key-value pairs. Returns an empty map if the file is absent.
   */
  public static Map<String, String> parse(File envFile) {
    if (!envFile.exists()) {
      return Map.of();
    }
    try {
      return parseLines(Files.readAllLines(envFile.toPath(), StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read " + envFile, e);
    }
  }

  /** Parses raw lines into a map, for testability without touching the filesystem. */
  static Map<String, String> parseLines(Iterable<String> lines) {
    Map<String, String> values = new LinkedHashMap<>();
    for (String rawLine : lines) {
      String line = rawLine.trim();
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }
      if (line.startsWith("export ")) {
        line = line.substring("export ".length()).trim();
      }
      int separator = line.indexOf('=');
      if (separator < 1) {
        continue;
      }
      String key = line.substring(0, separator).trim();
      String value = line.substring(separator + 1).trim();
      if (value.length() > 1
          && ((value.startsWith("\"") && value.endsWith("\""))
              || (value.startsWith("'") && value.endsWith("'")))) {
        value = value.substring(1, value.length() - 1);
      }
      values.put(key, value);
    }
    return values;
  }

  /**
   * Resolves credential values for the given names. Real environment variables win over file
   * values.
   *
   * @param envFile the {@code .env} file to fall back to
   * @param credentialNames the environment variable names to resolve
   * @return map of name to resolved value (null when absent from both sources)
   */
  public static Map<String, String> resolve(File envFile, Iterable<String> credentialNames) {
    Map<String, String> fileValues = parse(envFile);
    Map<String, String> resolved = new LinkedHashMap<>();
    for (String name : credentialNames) {
      String envValue = System.getenv(name);
      resolved.put(name, envValue != null ? envValue : fileValues.get(name));
    }
    return resolved;
  }
}
