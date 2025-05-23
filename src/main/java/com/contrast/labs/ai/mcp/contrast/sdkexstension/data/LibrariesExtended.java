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
package com.contrast.labs.ai.mcp.contrast.sdkexstension.data;


import java.util.List;

/** Base class for api calls for libraries. */
public class LibrariesExtended {

    public String getAverageScoreLetter() {
        return averageScoreLetter;
    }

    private String averageScoreLetter = null;

    public Integer getAverageScore() {
        return averageScore;
    }

    private Integer averageScore = null;

    public Integer getAverageMonths() {
        return averageMonths;
    }

    private Integer averageMonths = null;

    public List<LibraryExtended> getLibraries() {
        return libraries;
    }

    private List<LibraryExtended> libraries;
}

