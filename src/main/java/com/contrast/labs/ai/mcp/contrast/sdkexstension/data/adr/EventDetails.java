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

import java.util.List;

/** Class representing the details information in an attack event. */
public class EventDetails {
  private String file;
  private String method;
  private int line;
  private List<StackFrame> stackFrames;
  private int lastCustomFrame;
  private String signature;
  private String vector;
  private Object evidence;
  private String evidenceLabel;

  public String getFile() {
    return file;
  }

  public void setFile(String file) {
    this.file = file;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public int getLine() {
    return line;
  }

  public void setLine(int line) {
    this.line = line;
  }

  public List<StackFrame> getStackFrames() {
    return stackFrames;
  }

  public void setStackFrames(List<StackFrame> stackFrames) {
    this.stackFrames = stackFrames;
  }

  public int getLastCustomFrame() {
    return lastCustomFrame;
  }

  public void setLastCustomFrame(int lastCustomFrame) {
    this.lastCustomFrame = lastCustomFrame;
  }

  public String getSignature() {
    return signature;
  }

  public void setSignature(String signature) {
    this.signature = signature;
  }

  public String getVector() {
    return vector;
  }

  public void setVector(String vector) {
    this.vector = vector;
  }

  public Object getEvidence() {
    return evidence;
  }

  public void setEvidence(Object evidence) {
    this.evidence = evidence;
  }

  public String getEvidenceLabel() {
    return evidenceLabel;
  }

  public void setEvidenceLabel(String evidenceLabel) {
    this.evidenceLabel = evidenceLabel;
  }
}
