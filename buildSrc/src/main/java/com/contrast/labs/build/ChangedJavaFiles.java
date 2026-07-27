package com.contrast.labs.build;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds the Java files a changed-file verification task should inspect, by asking git.
 *
 * <p>With no base ref the working tree is used, which is what a developer running the task by hand
 * wants. With a base ref the comparison is a three-dot range against a head, which is what the
 * pre-push hook wants so the gate covers exactly the commits being pushed.
 */
public final class ChangedJavaFiles {

  private static final String DEFAULT_HEAD = "HEAD";

  private ChangedJavaFiles() {}

  /**
   * Returns the changed {@code .java} files that live under {@code sourceSetDir}.
   *
   * @param rootDir repository root, used as the git working directory
   * @param sourceSetDir directory the results are filtered to, such as {@code src/main/java}
   * @param base ref to compare from, or null/blank to use the working tree
   * @param head ref to compare to; ignored when {@code base} is null/blank, defaults to HEAD
   */
  public static List<File> forSourceSet(File rootDir, File sourceSetDir, String base, String head) {
    Path sourceSetPath = normalize(sourceSetDir);
    Set<String> paths = new LinkedHashSet<>();
    if (isBlank(base)) {
      paths.addAll(git(rootDir, workingTreeArgs()));
      paths.addAll(git(rootDir, untrackedArgs()));
    } else {
      paths.addAll(git(rootDir, rangeArgs(base, isBlank(head) ? DEFAULT_HEAD : head)));
    }

    List<File> changed = new ArrayList<>();
    for (String path : paths) {
      if (!path.endsWith(".java")) {
        continue;
      }
      File file = new File(rootDir, path);
      // A rename or delete can name a path that no longer exists in the working tree.
      if (!file.isFile() || !normalize(file).startsWith(sourceSetPath)) {
        continue;
      }
      changed.add(file);
    }
    return List.copyOf(changed);
  }

  private static List<String> rangeArgs(String base, String head) {
    return List.of(
        "diff", "--name-only", "--diff-filter=ACMR", base + "..." + head, "--", "*.java");
  }

  private static List<String> workingTreeArgs() {
    return List.of("diff", "--name-only", "--diff-filter=ACMR", DEFAULT_HEAD, "--", "*.java");
  }

  private static List<String> untrackedArgs() {
    return List.of("ls-files", "--others", "--exclude-standard", "--", "*.java");
  }

  private static List<String> git(File rootDir, List<String> args) {
    List<String> command = new ArrayList<>(List.of("git", "-C", rootDir.getAbsolutePath()));
    command.addAll(args);

    Process process;
    try {
      process = new ProcessBuilder(command).redirectErrorStream(true).start();
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to run: " + String.join(" ", command), e);
    }

    String output;
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
      output = reader.lines().reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read output of: " + String.join(" ", command), e);
    }

    int exitCode;
    try {
      exitCode = process.waitFor();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Interrupted running: " + String.join(" ", command), e);
    }
    if (exitCode != 0) {
      throw new IllegalStateException(
          "Git command failed with exit code "
              + exitCode
              + ": "
              + String.join(" ", command)
              + System.lineSeparator()
              + output.trim());
    }

    return Arrays.stream(output.split("\n")).map(String::trim).filter(line -> !line.isEmpty()).toList();
  }

  private static Path normalize(File file) {
    return file.toPath().toAbsolutePath().normalize();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
