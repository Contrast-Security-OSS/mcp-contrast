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
package com.contrast.labs.ai.mcp.contrast.sdkexstension.data.adr;

import java.util.Map;

/**
 * Class representing a chapter in the story of an attack event.
 */
public class Chapter {
    private String type;
    private String introText;
    private String introTextFormat;
    private Map<String, Object> introTextVariables;
    private String body;
    private String bodyFormat;
    private Map<String, Object> bodyFormatVariables;
    private String vector;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getIntroText() {
        return introText;
    }

    public void setIntroText(String introText) {
        this.introText = introText;
    }

    public String getIntroTextFormat() {
        return introTextFormat;
    }

    public void setIntroTextFormat(String introTextFormat) {
        this.introTextFormat = introTextFormat;
    }

    public Map<String, Object> getIntroTextVariables() {
        return introTextVariables;
    }

    public void setIntroTextVariables(Map<String, Object> introTextVariables) {
        this.introTextVariables = introTextVariables;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getBodyFormat() {
        return bodyFormat;
    }

    public void setBodyFormat(String bodyFormat) {
        this.bodyFormat = bodyFormat;
    }

    public Map<String, Object> getBodyFormatVariables() {
        return bodyFormatVariables;
    }

    public void setBodyFormatVariables(Map<String, Object> bodyFormatVariables) {
        this.bodyFormatVariables = bodyFormatVariables;
    }

    public String getVector() {
        return vector;
    }

    public void setVector(String vector) {
        this.vector = vector;
    }
}