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
 * The version to deploy.
 */

@Schema(name = "AppDeploymentTriggerInput", description = "The version to deploy.")
@JsonTypeName("AppDeploymentTriggerInput")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-16T22:08:52.163262+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class AppDeploymentTriggerInputModel {

  private Long appVersionId;

  public AppDeploymentTriggerInputModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AppDeploymentTriggerInputModel(Long appVersionId) {
    this.appVersionId = appVersionId;
  }

  public AppDeploymentTriggerInputModel appVersionId(Long appVersionId) {
    this.appVersionId = appVersionId;
    return this;
  }

  /**
   * The id of a version of the instance's app.
   * @return appVersionId
   */
  @NotNull 
  @Schema(name = "appVersionId", description = "The id of a version of the instance's app.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("appVersionId")
  public Long getAppVersionId() {
    return appVersionId;
  }

  @JsonProperty("appVersionId")
  public void setAppVersionId(Long appVersionId) {
    this.appVersionId = appVersionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppDeploymentTriggerInputModel appDeploymentTriggerInput = (AppDeploymentTriggerInputModel) o;
    return Objects.equals(this.appVersionId, appDeploymentTriggerInput.appVersionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(appVersionId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppDeploymentTriggerInputModel {\n");
    sb.append("    appVersionId: ").append(toIndentedString(appVersionId)).append("\n");
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

