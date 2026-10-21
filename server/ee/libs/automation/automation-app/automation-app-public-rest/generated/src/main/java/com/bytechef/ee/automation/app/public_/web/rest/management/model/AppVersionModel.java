package com.bytechef.ee.automation.app.public_.web.rest.management.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.OffsetDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A version of an app: a name for code that ByteChef does not hold.
 */

@Schema(name = "AppVersion", description = "A version of an app: a name for code that ByteChef does not hold.")
@JsonTypeName("AppVersion")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-16T19:32:54.592205+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class AppVersionModel {

  private @Nullable Long id;

  private @Nullable Long appId;

  private @Nullable String name;

  private @Nullable String gitRef;

  private @Nullable String gitCommitSha;

  private @Nullable String description;

  private @Nullable String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  public AppVersionModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  
  @Schema(name = "id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public AppVersionModel appId(@Nullable Long appId) {
    this.appId = appId;
    return this;
  }

  /**
   * Get appId
   * @return appId
   */
  
  @Schema(name = "appId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("appId")
  public @Nullable Long getAppId() {
    return appId;
  }

  @JsonProperty("appId")
  public void setAppId(@Nullable Long appId) {
    this.appId = appId;
  }

  public AppVersionModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  
  @Schema(name = "name", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public AppVersionModel gitRef(@Nullable String gitRef) {
    this.gitRef = gitRef;
    return this;
  }

  /**
   * Get gitRef
   * @return gitRef
   */
  
  @Schema(name = "gitRef", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("gitRef")
  public @Nullable String getGitRef() {
    return gitRef;
  }

  @JsonProperty("gitRef")
  public void setGitRef(@Nullable String gitRef) {
    this.gitRef = gitRef;
  }

  public AppVersionModel gitCommitSha(@Nullable String gitCommitSha) {
    this.gitCommitSha = gitCommitSha;
    return this;
  }

  /**
   * Get gitCommitSha
   * @return gitCommitSha
   */
  
  @Schema(name = "gitCommitSha", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("gitCommitSha")
  public @Nullable String getGitCommitSha() {
    return gitCommitSha;
  }

  @JsonProperty("gitCommitSha")
  public void setGitCommitSha(@Nullable String gitCommitSha) {
    this.gitCommitSha = gitCommitSha;
  }

  public AppVersionModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
   */
  
  @Schema(name = "description", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public AppVersionModel createdBy(@Nullable String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * Get createdBy
   * @return createdBy
   */
  
  @Schema(name = "createdBy", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdBy")
  public @Nullable String getCreatedBy() {
    return createdBy;
  }

  @JsonProperty("createdBy")
  public void setCreatedBy(@Nullable String createdBy) {
    this.createdBy = createdBy;
  }

  public AppVersionModel createdDate(@Nullable OffsetDateTime createdDate) {
    this.createdDate = createdDate;
    return this;
  }

  /**
   * Get createdDate
   * @return createdDate
   */
  @Valid 
  @Schema(name = "createdDate", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdDate")
  public @Nullable OffsetDateTime getCreatedDate() {
    return createdDate;
  }

  @JsonProperty("createdDate")
  public void setCreatedDate(@Nullable OffsetDateTime createdDate) {
    this.createdDate = createdDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppVersionModel appVersion = (AppVersionModel) o;
    return Objects.equals(this.id, appVersion.id) &&
        Objects.equals(this.appId, appVersion.appId) &&
        Objects.equals(this.name, appVersion.name) &&
        Objects.equals(this.gitRef, appVersion.gitRef) &&
        Objects.equals(this.gitCommitSha, appVersion.gitCommitSha) &&
        Objects.equals(this.description, appVersion.description) &&
        Objects.equals(this.createdBy, appVersion.createdBy) &&
        Objects.equals(this.createdDate, appVersion.createdDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, appId, name, gitRef, gitCommitSha, description, createdBy, createdDate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppVersionModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    appId: ").append(toIndentedString(appId)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    gitRef: ").append(toIndentedString(gitRef)).append("\n");
    sb.append("    gitCommitSha: ").append(toIndentedString(gitCommitSha)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}

