package com.contrast.labs.ai.mcp.contrast;

/**
 * Enumeration representing different risk levels with corresponding numeric values.
 */
public enum RiskLevel {
    /**
     * No risk - completely safe
     */
    ACCEPT_NO_RISK(0),

    /**
     * Low risk - minimal potential impact
     */
    LOW(1),

    /**
     * Medium risk - moderate potential impact
     */
    MEDIUM(2),

    /**
     * High risk - significant potential impact
     */
    HIGH(3),

    /**
     * Accept all risk - no security restrictions
     */
    ACCEPT_ALL_RISK(4);

    private final int value;

    RiskLevel(int value) {
        this.value = value;
    }

    public static RiskLevel fromString(String value) {
        if (value == null || value.equalsIgnoreCase(""))
        {
            return ACCEPT_NO_RISK;
        } else {
            switch (value.toUpperCase()) {
                case "LOW":
                    return LOW;
                case "MEDIUM":
                    return MEDIUM;
                case "HIGH":
                    return HIGH;
                case "ACCEPT_ALL_RISK":
                    return ACCEPT_ALL_RISK;
                case "ACCEPT_NO_RISK":
                    return ACCEPT_NO_RISK;
                default:
                    return ACCEPT_NO_RISK;
            }
        }
        
    }

    /**
     * Gets the integer value of the risk level.
     *
     * @return The integer value representing the risk level
     */
    public int getValue() {
        return value;
    }
}