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
package com.contrast.labs.ai.mcp.contrast.sdkextension.data.sca;

import java.util.List;

public class LibraryObservationsResponse {
  private List<LibraryObservation> observations;
  private int total;
  private String id;

  public List<LibraryObservation> getObservations() {
    return observations;
  }

  public void setObservations(List<LibraryObservation> observations) {
    this.observations = observations;
  }

  public int getTotal() {
    return total;
  }

  public void setTotal(int total) {
    this.total = total;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  @Override
  public String toString() {
    return "LibraryObservationsResponse{"
        + "observations="
        + observations
        + ", total="
        + total
        + ", id='"
        + id
        + '\''
        + '}';
  }
}
