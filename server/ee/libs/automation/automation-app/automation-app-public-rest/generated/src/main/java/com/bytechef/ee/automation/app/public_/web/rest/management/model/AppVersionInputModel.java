package com.bytechef.ee.automation.app.public_.web.rest.management.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * The values to create an app version with.
 */

@Schema(name = "AppVersionInput", description = "The values to create an app version with.")
@JsonTypeName("AppVersionInput")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-16T20:04:03.032204+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class AppVersionInputModel {

  private String name;

  private @Nullable String gitRef;

  private @Nullable String gitCommitSha;

  private @Nullable String description;

  public AppVersionInputModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AppVersionInputModel(String name) {
    this.name = name;
  }

  public AppVersionInputModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull @Size(max = 256) 
  @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public AppVersionInputModel gitRef(@Nullable String gitRef) {
    this.gitRef = gitRef;
    return this;
  }

  /**
   * Get gitRef
   * @return gitRef
   */
  @Size(max = 256) 
  @Schema(name = "gitRef", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("gitRef")
  public @Nullable String getGitRef() {
    return gitRef;
  }

  @JsonProperty("gitRef")
  public void setGitRef(@Nullable String gitRef) {
    this.gitRef = gitRef;
  }

  public AppVersionInputModel gitCommitSha(@Nullable String gitCommitSha) {
    this.gitCommitSha = gitCommitSha;
    return this;
  }

  /**
   * Get gitCommitSha
   * @return gitCommitSha
   */
  @Size(max = 64) 
  @Schema(name = "gitCommitSha", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("gitCommitSha")
  public @Nullable String getGitCommitSha() {
    return gitCommitSha;
  }

  @JsonProperty("gitCommitSha")
  public void setGitCommitSha(@Nullable String gitCommitSha) {
    this.gitCommitSha = gitCommitSha;
  }

  public AppVersionInputModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
   */
  @Size(max = 4096) 
  @Schema(name = "description", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppVersionInputModel appVersionInput = (AppVersionInputModel) o;
    return Objects.equals(this.name, appVersionInput.name) &&
        Objects.equals(this.gitRef, appVersionInput.gitRef) &&
        Objects.equals(this.gitCommitSha, appVersionInput.gitCommitSha) &&
        Objects.equals(this.description, appVersionInput.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, gitRef, gitCommitSha, description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppVersionInputModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    gitRef: ").append(toIndentedString(gitRef)).append("\n");
    sb.append("    gitCommitSha: ").append(toIndentedString(gitCommitSha)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
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

