package com.contrast.labs.ai.mcp.contrast.sdkextension.data.application;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import lombok.Data;

/** Represents an application from the Contrast API. */
@Data
public class Application {

  @SerializedName("name")
  private String name;

  @SerializedName("path")
  private String path;

  @SerializedName("language")
  private String language;

  @SerializedName("created")
  private Long created;

  @SerializedName("status")
  private String status;

  @SerializedName("importance")
  private Integer importance;

  @SerializedName("archived")
  private boolean archived;

  @SerializedName("assess")
  private boolean assess;

  @SerializedName("assessPending")
  private boolean assessPending;

  @SerializedName("primary")
  private boolean primary;

  @SerializedName("master")
  private boolean master;

  @SerializedName("notes")
  private String notes;

  @SerializedName("defend")
  private boolean defend;

  @SerializedName("roles")
  private List<String> roles;

  @SerializedName("tags")
  private List<String> tags;

  @SerializedName("techs")
  private List<String> techs;

  @SerializedName("policies")
  private List<String> policies;

  @SerializedName("metadataEntities")
  private List<Metadata> metadataEntities;

  @SerializedName("validationErrorFields")
  private List<Field> validationErrorFields;

  @SerializedName("missingRequiredFields")
  private List<Field> missingRequiredFields;

  @SerializedName("protect")
  private Object protect;

  @SerializedName("app_id")
  private String appId;

  @SerializedName("last_seen")
  private Long lastSeen;

  @SerializedName("last_reset")
  private Long lastReset;

  @SerializedName("size_shorthand")
  private String sizeShorthand;

  @SerializedName("size")
  private Long size;

  @SerializedName("code_shorthand")
  private String codeShorthand;

  @SerializedName("code")
  private Long code;

  @SerializedName("override_url")
  private String overrideUrl;

  @SerializedName("short_name")
  private String shortName;

  @SerializedName("importance_description")
  private String importanceDescription;

  @SerializedName("total_modules")
  private Integer totalModules;

  @SerializedName("servers_without_defend")
  private boolean serversWithoutDefend;

  @SerializedName("first_seen")
  private Long firstSeen;

  @SerializedName("onboarded_time")
  private Long onboardedTime;
}
