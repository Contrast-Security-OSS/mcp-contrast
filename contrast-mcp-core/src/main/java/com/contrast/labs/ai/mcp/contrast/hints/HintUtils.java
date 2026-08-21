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

package com.contrast.labs.ai.mcp.contrast.hints;

import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.springframework.util.CollectionUtils;

/** Utility class for hint manipulation operations. */
@UtilityClass
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
    if (CollectionUtils.isEmpty(hints)) {
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
