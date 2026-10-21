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
 * The values to record a deployment with.
 */

@Schema(name = "AppDeploymentRecordInput", description = "The values to record a deployment with.")
@JsonTypeName("AppDeploymentRecordInput")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-16T19:32:54.592205+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class AppDeploymentRecordInputModel {

  private String versionName;

  private @Nullable String url;

  private @Nullable String gitCommitSha;

  public AppDeploymentRecordInputModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AppDeploymentRecordInputModel(String versionName) {
    this.versionName = versionName;
  }

  public AppDeploymentRecordInputModel versionName(String versionName) {
    this.versionName = versionName;
    return this;
  }

  /**
   * The name of the deployed version. A new name creates the version.
   * @return versionName
   */
  @NotNull @Size(max = 256) 
  @Schema(name = "versionName", description = "The name of the deployed version. A new name creates the version.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("versionName")
  public String getVersionName() {
    return versionName;
  }

  @JsonProperty("versionName")
  public void setVersionName(String versionName) {
    this.versionName = versionName;
  }

  public AppDeploymentRecordInputModel url(@Nullable String url) {
    this.url = url;
    return this;
  }

  /**
   * Where the deployed version is served, an absolute http or https URL.
   * @return url
   */
  @Size(max = 2048) 
  @Schema(name = "url", description = "Where the deployed version is served, an absolute http or https URL.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("url")
  public @Nullable String getUrl() {
    return url;
  }

  @JsonProperty("url")
  public void setUrl(@Nullable String url) {
    this.url = url;
  }

  public AppDeploymentRecordInputModel gitCommitSha(@Nullable String gitCommitSha) {
    this.gitCommitSha = gitCommitSha;
    return this;
  }

  /**
   * The commit of the deployed version. It must match the commit of an existing version of that name.
   * @return gitCommitSha
   */
  @Size(max = 64) 
  @Schema(name = "gitCommitSha", description = "The commit of the deployed version. It must match the commit of an existing version of that name.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("gitCommitSha")
  public @Nullable String getGitCommitSha() {
    return gitCommitSha;
  }

  @JsonProperty("gitCommitSha")
  public void setGitCommitSha(@Nullable String gitCommitSha) {
    this.gitCommitSha = gitCommitSha;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppDeploymentRecordInputModel appDeploymentRecordInput = (AppDeploymentRecordInputModel) o;
    return Objects.equals(this.versionName, appDeploymentRecordInput.versionName) &&
        Objects.equals(this.url, appDeploymentRecordInput.url) &&
        Objects.equals(this.gitCommitSha, appDeploymentRecordInput.gitCommitSha);
  }

  @Override
  public int hashCode() {
    return Objects.hash(versionName, url, gitCommitSha);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppDeploymentRecordInputModel {\n");
    sb.append("    versionName: ").append(toIndentedString(versionName)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    gitCommitSha: ").append(toIndentedString(gitCommitSha)).append("\n");
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

