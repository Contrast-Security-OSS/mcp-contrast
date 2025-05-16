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
package com.contrast.labs.ai.mcp.contrast.sdkexstension.data.sca;

import java.time.Instant;

public class LibraryObservation {
    private String name;
    private Instant firstObservedTime;
    private Instant lastObservedTime;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getFirstObservedTime() {
        return firstObservedTime;
    }

    public void setFirstObservedTime(Instant firstObservedTime) {
        this.firstObservedTime = firstObservedTime;
    }

    public Instant getLastObservedTime() {
        return lastObservedTime;
    }

    public void setLastObservedTime(Instant lastObservedTime) {
        this.lastObservedTime = lastObservedTime;
    }

    @Override
    public String toString() {
        return "LibraryObservation{" +
                "name='" + name + '\'' +
                ", firstObservedTime=" + firstObservedTime +
                ", lastObservedTime=" + lastObservedTime +
                '}';
    }
}