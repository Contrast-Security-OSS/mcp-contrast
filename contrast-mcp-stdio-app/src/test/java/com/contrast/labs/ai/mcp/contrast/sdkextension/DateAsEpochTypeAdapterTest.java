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

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DateAsEpochTypeAdapterTest {

  private Gson gson;

  @BeforeEach
  void setUp() {
    gson =
        new GsonBuilder()
            .registerTypeAdapter(Date.class, new DateAsEpochTypeAdapter().nullSafe())
            .create();
  }

  @Test
  void write_should_serialize_date_as_epoch_milliseconds() {
    var date = new Date(1672531200000L); // Jan 1, 2023 UTC

    var json = gson.toJson(date);

    assertThat(json).isEqualTo("1672531200000");
  }

  @Test
  void read_should_deserialize_epoch_milliseconds_to_date() {
    var json = "1672531200000";

    var date = gson.fromJson(json, Date.class);

    assertThat(date).isEqualTo(new Date(1672531200000L));
  }

  @Test
  void write_should_handle_null_date() {
    // Gson omits null fields by default, so null Date becomes empty object
    record DateWrapper(Date date) {}
    var wrapper = new DateWrapper(null);

    var json = gson.toJson(wrapper);

    assertThat(json).isEqualTo("{}");
  }

  @Test
  void read_should_handle_null_value() {
    record DateWrapper(Date date) {}
    var json = "{\"date\":null}";

    var wrapper = gson.fromJson(json, DateWrapper.class);

    assertThat(wrapper.date()).isNull();
  }
}
