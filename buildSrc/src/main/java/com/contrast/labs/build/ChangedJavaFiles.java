package com.contrast.labs.build;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds the Java files a changed-file verification task should inspect, by asking git.
 *
 * <p>With no base ref the working tree is used. With a base ref the comparison is a three-dot
 * range, so the gate covers exactly the commits being pushed or reviewed.
 */
public final class ChangedJavaFiles {

  private static final String DEFAULT_HEAD = "HEAD";
  private static final String JAVA_SUFFIX = ".java";

  private ChangedJavaFiles() {}

  /**
   * The changed Java files, split by whether the working tree still holds them.
   *
   * @param present files on disk, so coverage can be attributed to them
   * @param absentFromWorkingTree paths git named that are not on disk, which coverage generated
   *     from the working tree cannot describe
   */
  public record ChangedFiles(List<File> present, List<File> absentFromWorkingTree) {

    public boolean isEmpty() {
      return present.isEmpty() && absentFromWorkingTree.isEmpty();
    }
  }

  /**
   * Returns the changed {@code .java} files that live under {@code sourceSetDir}.
   *
   * @param rootDir repository root, used as the git working directory
   * @param sourceSetDir directory the results are filtered to, such as {@code src/main/java}
   * @param base ref to compare from, or null/blank to use the working tree
   * @param head ref to compare to; ignored when {@code base} is null/blank, defaults to HEAD
   */
  public static ChangedFiles forSourceSet(
      File rootDir, File sourceSetDir, String base, String head) {
    Path sourceSetPath = normalize(sourceSetDir);
    Set<String> paths = new LinkedHashSet<>();
    if (isBlank(base)) {
      paths.addAll(git(rootDir, workingTreeArgs()));
      paths.addAll(git(rootDir, untrackedArgs()));
    } else {
      paths.addAll(git(rootDir, rangeArgs(base, isBlank(head) ? DEFAULT_HEAD : head)));
    }

    List<File> present = new ArrayList<>();
    List<File> absent = new ArrayList<>();
    for (String path : paths) {
      if (!path.endsWith(JAVA_SUFFIX)) {
        continue;
      }
      File file = new File(rootDir, path);
      if (!normalize(file).startsWith(sourceSetPath)) {
        continue;
      }
      // Recorded rather than discarded, so the caller can report a file it could not measure.
      if (file.isFile()) {
        present.add(file);
      } else {
        absent.add(file);
      }
    }
    return new ChangedFiles(List.copyOf(present), List.copyOf(absent));
  }

  private static List<String> rangeArgs(String base, String head) {
    // base and head arrive from a Gradle property. --end-of-options stops git reading a
    // leading-dash value as an option, where --output= would write a file and return no paths.
    return List.of(
        "diff",
        "--name-only",
        "-z",
        "--diff-filter=ACMR",
        "--end-of-options",
        base + "..." + head,
        "--",
        "*.java");
  }

  private static List<String> workingTreeArgs() {
    return List.of("diff", "--name-only", "-z", "--diff-filter=ACMR", DEFAULT_HEAD, "--", "*.java");
  }

  private static List<String> untrackedArgs() {
    return List.of("ls-files", "--others", "--exclude-standard", "-z", "--", "*.java");
  }

  private static List<String> git(File rootDir, List<String> args) {
    // core.quotepath defaults to true, which C-quotes non-ASCII paths as
    // "src/main/java/Na\303\257ve.java". That fails the .java suffix test, so the file would drop
    // out of the gate unmeasured. -z then delimits with NUL, which no path can contain.
    List<String> command =
        new ArrayList<>(
            List.of("git", "-c", "core.quotepath=false", "-C", rootDir.getAbsolutePath()));
    command.addAll(args);

    Process process;
    try {
      process = new ProcessBuilder(command).start();
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to run: " + String.join(" ", command), e);
    }

    // git writes advice to stderr even on success, so merging the streams would feed that text
    // into the path list. Drained on its own thread because reading the pipes in sequence can
    // block on one while git waits on the other.
    StringBuilder errorOutput = new StringBuilder();
    Thread errorReader =
        Thread.ofVirtual()
            .start(
                () -> {
                  try (InputStream errors = process.getErrorStream()) {
                    errorOutput.append(new String(errors.readAllBytes(), StandardCharsets.UTF_8));
                  } catch (IOException e) {
                    errorOutput.append("Failed to read stderr: ").append(e.getMessage());
                  }
                });

    byte[] output;
    try (InputStream stdout = process.getInputStream()) {
      output = stdout.readAllBytes();
    } catch (IOException e) {
      throw new UncheckedIOException("Failed to read output of: " + String.join(" ", command), e);
    }

    int exitCode;
    try {
      errorReader.join();
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
              + errorOutput.toString().trim());
    }

    return nulDelimited(new String(output, StandardCharsets.UTF_8));
  }

  private static List<String> nulDelimited(String output) {
    List<String> paths = new ArrayList<>();
    for (String path : output.split("\0")) {
      if (!path.isEmpty()) {
        paths.add(path);
      }
    }
    return paths;
  }

  private static Path normalize(File file) {
    return file.toPath().toAbsolutePath().normalize();
  }

  private static boolean isBlank(String value) {
    return value == null || value.isBlank();
  }
}
