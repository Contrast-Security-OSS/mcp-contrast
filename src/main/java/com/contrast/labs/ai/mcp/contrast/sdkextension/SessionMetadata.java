package com.contrast.labs.ai.mcp.contrast.sdkextension;

public class SessionMetadata {
  private String name;
  private String value;

  public SessionMetadata(String name, String value) {
    this.name = name;
    this.value = value;
  }

  public String name() {
    return name;
  }

  public String value() {
    return value;
  }

  @Override
  public String toString() {
    return "SessionMetadata{" + "name='" + name + '\'' + ", value='" + value + '\'' + '}';
  }
}
