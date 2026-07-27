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
