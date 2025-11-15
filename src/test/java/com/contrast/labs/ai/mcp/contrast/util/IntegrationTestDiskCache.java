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
package com.contrast.labs.ai.mcp.contrast.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lightweight disk cache shared across integration tests.
 *
 * <p>The cache allows reuse of previously discovered test data on developer machines so repeated
 * runs skip expensive discovery phases. Cache entries are colocated with the repository but kept
 * outside version control via {@code test-cache/}.
 *
 * <p>Environment controls:
 *
 * <ul>
 *   <li>{@code CONTRAST_TEST_CACHE_DISABLE=true} - Disable disk caching entirely.
 *   <li>{@code CONTRAST_TEST_CACHE_CLEAR=true} - Clear all cached entries before the next run.
 *   <li>{@code CONTRAST_TEST_CACHE_TTL_HOURS=N} - Override the default entry TTL (12 hours).
 * </ul>
 */
public final class IntegrationTestDiskCache {

  private static final Logger logger = LoggerFactory.getLogger(IntegrationTestDiskCache.class);

  private static final Path CACHE_DIR = Path.of("test-cache");
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Duration DEFAULT_TTL = Duration.ofHours(resolveTtlHours());
  private static final boolean DISABLED =
      Boolean.parseBoolean(resolveEnvOrProperty("CONTRAST_TEST_CACHE_DISABLE", null, "false"));
  private static final boolean CLEAR_REQUESTED =
      Boolean.parseBoolean(resolveEnvOrProperty("CONTRAST_TEST_CACHE_CLEAR", null, "false"));
  private static final AtomicBoolean CLEARED = new AtomicBoolean(false);

  private IntegrationTestDiskCache() {}

  /**
   * Loads cached data for the given test if present and not expired.
   *
   * @param testName Display name (e.g. class simple name)
   * @param orgId Organization identifier
   * @param type Type token for deserialization
   * @return Optional cached value
   */
  public static <T> Optional<T> read(String testName, String orgId, Class<T> type) {
    return read(testName, orgId, type, DEFAULT_TTL);
  }

  /**
   * Loads cached data for the given test if present and not expired.
   *
   * @param testName Display name (e.g. class simple name)
   * @param orgId Organization identifier
   * @param type Type token for deserialization
   * @param ttl Maximum cache age
   * @return Optional cached value
   */
  public static <T> Optional<T> read(String testName, String orgId, Class<T> type, Duration ttl) {

    if (DISABLED) {
      return Optional.empty();
    }

    maybeClearCacheDirectory();

    if (orgId == null || orgId.isBlank()) {
      logger.debug("Skipping disk cache load for {}: orgId is blank", testName);
      return Optional.empty();
    }

    Path cacheFile = cacheFile(testName, orgId);
    if (!Files.exists(cacheFile)) {
      return Optional.empty();
    }

    if (ttl != null && !ttl.isZero() && !ttl.isNegative()) {
      try {
        Instant lastModified = Files.getLastModifiedTime(cacheFile).toInstant();
        if (Instant.now().minus(ttl).isAfter(lastModified)) {
          logger.debug(
              "Disk cache entry for {} expired (age {}s)",
              testName,
              Duration.between(lastModified, Instant.now()).getSeconds());
          return Optional.empty();
        }
      } catch (IOException ex) {
        logger.warn("Unable to read cache timestamp for {}: {}", testName, ex.getMessage());
        return Optional.empty();
      }
    }

    try (Reader reader = Files.newBufferedReader(cacheFile, StandardCharsets.UTF_8)) {
      T value = GSON.fromJson(reader, type);
      logger.info("✓ Loaded cached discovery data for {}", testName);
      return Optional.ofNullable(value);
    } catch (Exception ex) {
      logger.warn("Failed to read cache entry for {}: {}", testName, ex.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Writes cached data for the given test if caching is enabled.
   *
   * @param testName Display name (e.g. class simple name)
   * @param orgId Organization identifier
   * @param value Value to persist
   */
  public static <T> void write(String testName, String orgId, T value) {
    if (DISABLED || value == null) {
      return;
    }

    if (orgId == null || orgId.isBlank()) {
      logger.debug("Skipping disk cache write for {}: orgId is blank", testName);
      return;
    }

    maybeClearCacheDirectory();

    Path cacheFile = cacheFile(testName, orgId);
    try {
      Files.createDirectories(cacheFile.getParent());

      Path tempFile =
          Files.createTempFile(cacheFile.getParent(), cacheFile.getFileName().toString(), ".tmp");
      try (Writer writer = Files.newBufferedWriter(tempFile, StandardCharsets.UTF_8)) {
        GSON.toJson(value, writer);
      }

      try {
        Files.move(
            tempFile,
            cacheFile,
            StandardCopyOption.REPLACE_EXISTING,
            StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException atomicEx) {
        // Retry without ATOMIC_MOVE for filesystems that do not support it
        Files.move(tempFile, cacheFile, StandardCopyOption.REPLACE_EXISTING);
      }
      logger.info("✓ Cached discovery data for {}", testName);
    } catch (IOException ex) {
      logger.warn("Unable to write cache entry for {}: {}", testName, ex.getMessage());
    }
  }

  /**
   * Loads cached data (if enabled) and delivers it to the supplied consumer.
   *
   * @param testName Display name (e.g. class simple name)
   * @param orgId Organization identifier
   * @param type Type token for deserialization
   * @param consumer Invoked when cached data exists
   * @return {@code true} when cached data was provided
   */
  public static <T> boolean loadIfPresent(
      String testName, String orgId, Class<T> type, Consumer<T> consumer) {
    var cached = read(testName, orgId, type);
    if (cached.isPresent()) {
      consumer.accept(cached.get());
      return true;
    }
    return false;
  }

  /**
   * @return whether disk caching is currently enabled
   */
  public static boolean isEnabled() {
    return !DISABLED;
  }

  private static void maybeClearCacheDirectory() {
    if (!CLEAR_REQUESTED || !CLEARED.compareAndSet(false, true)) {
      return;
    }

    try {
      if (Files.isDirectory(CACHE_DIR)) {
        Files.walk(CACHE_DIR)
            .sorted((a, b) -> b.compareTo(a)) // delete files before directories
            .forEach(
                path -> {
                  try {
                    Files.deleteIfExists(path);
                  } catch (IOException ex) {
                    logger.warn("Failed to delete cache path {}: {}", path, ex.getMessage());
                  }
                });
      }
    } catch (IOException ex) {
      logger.warn("Failed to clear cache directory {}: {}", CACHE_DIR, ex.getMessage());
    }
  }

  private static Path cacheFile(String testName, String orgId) {
    String sanitizedTestName = sanitize(testName);
    String fingerprint = fingerprint(orgId);
    return CACHE_DIR.resolve(sanitizedTestName + "-" + fingerprint + ".json");
  }

  private static String sanitize(String value) {
    if (value == null || value.isBlank()) {
      return "unknown-test";
    }
    return value.replaceAll("[^A-Za-z0-9._-]", "_");
  }

  private static String fingerprint(String orgId) {
    String host =
        resolveEnvOrProperty("CONTRAST_HOST_NAME", "contrast.host-name", "unknown-host")
            .toLowerCase(Locale.ROOT)
            .trim();
    String username =
        resolveEnvOrProperty("CONTRAST_USERNAME", "contrast.username", "unknown-user")
            .toLowerCase(Locale.ROOT)
            .trim();
    String normalizedOrg = orgId == null ? "unknown-org" : orgId.toLowerCase(Locale.ROOT).trim();

    String payload = host + "|" + username + "|" + normalizedOrg;
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
      // Shorten to 16 hex chars to keep filenames readable
      return HexFormat.of().formatHex(hash).substring(0, 16);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("Failed to compute cache fingerprint", ex);
    }
  }

  private static String resolveEnvOrProperty(
      String envKey, String propertyKey, String defaultValue) {
    String value = null;
    if (propertyKey != null) {
      value = System.getProperty(propertyKey);
    }
    if (value == null || value.isBlank()) {
      value = System.getenv(envKey);
    }
    if (value == null || value.isBlank()) {
      return defaultValue == null ? "" : defaultValue;
    }
    return value;
  }

  private static long resolveTtlHours() {
    String ttlValue = resolveEnvOrProperty("CONTRAST_TEST_CACHE_TTL_HOURS", null, null);
    if (ttlValue == null || ttlValue.isBlank()) {
      return 12L;
    }
    try {
      long parsed = Long.parseLong(ttlValue.trim());
      return parsed > 0 ? parsed : 12L;
    } catch (NumberFormatException ex) {
      logger.warn(
          "Invalid CONTRAST_TEST_CACHE_TTL_HOURS value '{}'; using default 12 hours", ttlValue);
      return 12L;
    }
  }
}
