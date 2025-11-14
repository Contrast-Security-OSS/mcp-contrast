package com.contrast.labs.ai.mcp.contrast.hints;

import java.util.ArrayList;
import java.util.List;

/** Utility class for hint manipulation operations. */
public class HintUtils {

  /**
   * Combines multiple lists of hints into a single list.
   *
   * @param lists The lists of hints to combine
   * @return A combined list of all hints
   */
  @SafeVarargs
  public static List<String> combineHints(List<String>... lists) {
    var result = new ArrayList<String>();

    for (List<String> list : lists) {
      if (list != null) {
        result.addAll(list);
      }
    }

    return result;
  }

  /**
   * Formats a list of hints into a single string with each hint on a new line.
   *
   * @param hints The list of hints to format
   * @return A formatted string containing all hints
   */
  public static String formatHints(List<String> hints) {
    if (hints == null || hints.isEmpty()) {
      return "";
    }

    var sb = new StringBuilder();
    for (int i = 0; i < hints.size(); i++) {
      if (i > 0) {
        sb.append("\n");
      }
      sb.append(hints.get(i));
    }

    return sb.toString();
  }
}
