package com.contrast.labs.ai.mcp.contrast.sdkexstension;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.env.Environment;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class SDKHelperTest {

    @Mock
    private Environment environment;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        // Inject mocked Environment into SDKHelper's static field using reflection
        Field envField = SDKHelper.class.getDeclaredField("environment");
        envField.setAccessible(true);
        envField.set(null, environment);
        
        // Set up default property
        when(environment.getProperty("contrast.api.protocol", "https")).thenReturn("https");
    }

    @Test
    void testGetProtocolAndServer_WithNull() {
        assertNull(SDKHelper.getProtocolAndServer(null));
    }

    @Test
    void testGetProtocolAndServer_WithHttpProtocol() {
        String result = SDKHelper.getProtocolAndServer("http://example.com");
        assertEquals("http://example.com", result);
    }
    
    @Test
    void testGetProtocolAndServer_WithHttpsProtocol() {
        String result = SDKHelper.getProtocolAndServer("https://example.com");
        assertEquals("https://example.com", result);
    }

    @Test
    void testGetProtocolAndServer_WithoutProtocol() {
        String result = SDKHelper.getProtocolAndServer("example.com");
        assertEquals("https://example.com", result);
    }

    @Test
    void testGetProtocolAndServer_WithCustomProtocol() {
        when(environment.getProperty("contrast.api.protocol", "https")).thenReturn("http");
        
        String result = SDKHelper.getProtocolAndServer("example.com");
        assertEquals("http://example.com", result);
    }
    
    @Test
    void testGetSDK_WithHttpsUrl() {
        String hostWithProtocol = "https://custom.example.com";
        
        // Use reflection to access the private method that builds the API URL
        try {
            Method method = SDKHelper.class.getDeclaredMethod("getSDK", String.class, String.class, String.class, String.class, String.class, String.class);
            method.setAccessible(true);
            
            Object sdk = method.invoke(null, hostWithProtocol, "apiKey", "serviceKey", "username", null, null);
            
            assertNotNull(sdk);
            // The actual URL validation would require accessing ContrastSDK's internal state,
            // which is beyond the scope of a unit test.
            // In a real application, you would use integration tests to verify this behavior.
        } catch (Exception e) {
            fail("Exception occurred: " + e.getMessage());
        }
    }
}
