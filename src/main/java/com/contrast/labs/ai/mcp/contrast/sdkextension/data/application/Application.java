package com.contrast.labs.ai.mcp.contrast.sdkextension.data.application;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/** Represents an application from the Contrast API. */
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

  // Getters and setters
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public String getLanguage() {
    return language;
  }

  public void setLanguage(String language) {
    this.language = language;
  }

  public Long getCreated() {
    return created;
  }

  public void setCreated(Long created) {
    this.created = created;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Integer getImportance() {
    return importance;
  }

  public void setImportance(Integer importance) {
    this.importance = importance;
  }

  public boolean isArchived() {
    return archived;
  }

  public void setArchived(boolean archived) {
    this.archived = archived;
  }

  public boolean isAssess() {
    return assess;
  }

  public void setAssess(boolean assess) {
    this.assess = assess;
  }

  public boolean isAssessPending() {
    return assessPending;
  }

  public void setAssessPending(boolean assessPending) {
    this.assessPending = assessPending;
  }

  public boolean isPrimary() {
    return primary;
  }

  public void setPrimary(boolean primary) {
    this.primary = primary;
  }

  public boolean isMaster() {
    return master;
  }

  public void setMaster(boolean master) {
    this.master = master;
  }

  public String getNotes() {
    return notes;
  }

  public void setNotes(String notes) {
    this.notes = notes;
  }

  public boolean isDefend() {
    return defend;
  }

  public void setDefend(boolean defend) {
    this.defend = defend;
  }

  public List<String> getRoles() {
    return roles;
  }

  public void setRoles(List<String> roles) {
    this.roles = roles;
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public List<String> getTechs() {
    return techs;
  }

  public void setTechs(List<String> techs) {
    this.techs = techs;
  }

  public List<String> getPolicies() {
    return policies;
  }

  public void setPolicies(List<String> policies) {
    this.policies = policies;
  }

  public List<Metadata> getMetadataEntities() {
    return metadataEntities;
  }

  public void setMetadataEntities(List<Metadata> metadataEntities) {
    this.metadataEntities = metadataEntities;
  }

  public List<Field> getValidationErrorFields() {
    return validationErrorFields;
  }

  public void setValidationErrorFields(List<Field> validationErrorFields) {
    this.validationErrorFields = validationErrorFields;
  }

  public List<Field> getMissingRequiredFields() {
    return missingRequiredFields;
  }

  public void setMissingRequiredFields(List<Field> missingRequiredFields) {
    this.missingRequiredFields = missingRequiredFields;
  }

  public Object getProtect() {
    return protect;
  }

  public void setProtect(Object protect) {
    this.protect = protect;
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public Long getLastSeen() {
    return lastSeen;
  }

  public void setLastSeen(Long lastSeen) {
    this.lastSeen = lastSeen;
  }

  public Long getLastReset() {
    return lastReset;
  }

  public void setLastReset(Long lastReset) {
    this.lastReset = lastReset;
  }

  public String getSizeShorthand() {
    return sizeShorthand;
  }

  public void setSizeShorthand(String sizeShorthand) {
    this.sizeShorthand = sizeShorthand;
  }

  public Long getSize() {
    return size;
  }

  public void setSize(Long size) {
    this.size = size;
  }

  public String getCodeShorthand() {
    return codeShorthand;
  }

  public void setCodeShorthand(String codeShorthand) {
    this.codeShorthand = codeShorthand;
  }

  public Long getCode() {
    return code;
  }

  public void setCode(Long code) {
    this.code = code;
  }

  public String getOverrideUrl() {
    return overrideUrl;
  }

  public void setOverrideUrl(String overrideUrl) {
    this.overrideUrl = overrideUrl;
  }

  public String getShortName() {
    return shortName;
  }

  public void setShortName(String shortName) {
    this.shortName = shortName;
  }

  public String getImportanceDescription() {
    return importanceDescription;
  }

  public void setImportanceDescription(String importanceDescription) {
    this.importanceDescription = importanceDescription;
  }

  public Integer getTotalModules() {
    return totalModules;
  }

  public void setTotalModules(Integer totalModules) {
    this.totalModules = totalModules;
  }

  public boolean isServersWithoutDefend() {
    return serversWithoutDefend;
  }

  public void setServersWithoutDefend(boolean serversWithoutDefend) {
    this.serversWithoutDefend = serversWithoutDefend;
  }

  public Long getFirstSeen() {
    return firstSeen;
  }

  public void setFirstSeen(Long firstSeen) {
    this.firstSeen = firstSeen;
  }

  public Long getOnboardedTime() {
    return onboardedTime;
  }

  public void setOnboardedTime(Long onboardedTime) {
    this.onboardedTime = onboardedTime;
  }

  @Override
  public String toString() {
    return "Application{"
        + "name='"
        + name
        + '\''
        + ", path='"
        + path
        + '\''
        + ", language='"
        + language
        + '\''
        + ", status='"
        + status
        + '\''
        + ", appId='"
        + appId
        + '\''
        + '}';
  }
}
