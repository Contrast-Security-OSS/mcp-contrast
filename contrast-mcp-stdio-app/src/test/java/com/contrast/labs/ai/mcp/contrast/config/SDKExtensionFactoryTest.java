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

package com.contrast.labs.ai.mcp.contrast.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.contrastsecurity.sdk.ContrastSDK;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SDKExtensionFactoryTest {

  private ContrastSDKFactory contrastSDKFactory;
  private SDKExtensionFactory factory;

  @BeforeEach
  void setUp() {
    contrastSDKFactory = mock();
    ContrastSDK sdk = mock();
    when(contrastSDKFactory.getSDK()).thenReturn(sdk);
    factory = new SDKExtensionFactory(contrastSDKFactory);
  }

  @Test
  void getSDKExtension_should_return_the_same_instance_on_every_call() {
    var first = factory.getSDKExtension();
    var second = factory.getSDKExtension();

    assertThat(first).isNotNull().isSameAs(second);
  }

  @Test
  void getSDKExtension_should_build_the_underlying_sdk_only_once() {
    factory.getSDKExtension();
    factory.getSDKExtension();
    factory.getSDKExtension();

    verify(contrastSDKFactory, times(1)).getSDK();
  }

  @Test
  void getSDKExtension_should_not_touch_the_sdk_factory_until_first_use() {
    verify(contrastSDKFactory, times(0)).getSDK();
  }
}
