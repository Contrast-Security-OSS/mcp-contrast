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
package com.contrast.labs.ai.mcp.contrast.sdkextension;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.util.Date;

/**
 * Gson TypeAdapter that serializes/deserializes Date objects as epoch milliseconds.
 *
 * <p>The Contrast API expects dates in request bodies as epoch milliseconds (e.g., 1672531200000),
 * not Gson's default human-readable format (e.g., "Jan 1, 2023 12:00:00 AM").
 *
 * <p>Usage: Register with Gson using {@code .nullSafe()} wrapper for proper null handling:
 *
 * <pre>{@code
 * Gson gson = new GsonBuilder()
 *     .registerTypeAdapter(Date.class, new DateAsEpochTypeAdapter().nullSafe())
 *     .create();
 * }</pre>
 */
public class DateAsEpochTypeAdapter extends TypeAdapter<Date> {

  @Override
  public void write(JsonWriter out, Date value) throws IOException {
    out.value(value.getTime());
  }

  @Override
  public Date read(JsonReader in) throws IOException {
    return new Date(in.nextLong());
  }
}
