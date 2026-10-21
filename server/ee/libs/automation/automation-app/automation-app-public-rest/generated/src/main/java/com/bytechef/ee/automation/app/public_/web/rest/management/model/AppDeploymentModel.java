package com.bytechef.ee.automation.app.public_.web.rest.management.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppDeploymentProviderModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppDeploymentStatusModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppVersionModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
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
 * A deployment of a version of the app to an app instance. A deployment is never changed once it is final.
 */

@Schema(name = "AppDeployment", description = "A deployment of a version of the app to an app instance. A deployment is never changed once it is final.")
@JsonTypeName("AppDeployment")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-16T19:32:54.592205+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class AppDeploymentModel {

  private @Nullable Long id;

  private @Nullable Long appInstanceId;

  private @Nullable AppVersionModel appVersion;

  private @Nullable AppDeploymentProviderModel provider;

  private @Nullable String providerDeploymentId;

  private @Nullable String url;

  private @Nullable AppDeploymentStatusModel status;

  private @Nullable String errorMessage;

  private @Nullable Boolean current;

  private @Nullable String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime finishedDate;

  public AppDeploymentModel id(@Nullable Long id) {
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

  public AppDeploymentModel appInstanceId(@Nullable Long appInstanceId) {
    this.appInstanceId = appInstanceId;
    return this;
  }

  /**
   * Get appInstanceId
   * @return appInstanceId
   */
  
  @Schema(name = "appInstanceId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("appInstanceId")
  public @Nullable Long getAppInstanceId() {
    return appInstanceId;
  }

  @JsonProperty("appInstanceId")
  public void setAppInstanceId(@Nullable Long appInstanceId) {
    this.appInstanceId = appInstanceId;
  }

  public AppDeploymentModel appVersion(@Nullable AppVersionModel appVersion) {
    this.appVersion = appVersion;
    return this;
  }

  /**
   * Get appVersion
   * @return appVersion
   */
  @Valid 
  @Schema(name = "appVersion", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("appVersion")
  public @Nullable AppVersionModel getAppVersion() {
    return appVersion;
  }

  @JsonProperty("appVersion")
  public void setAppVersion(@Nullable AppVersionModel appVersion) {
    this.appVersion = appVersion;
  }

  public AppDeploymentModel provider(@Nullable AppDeploymentProviderModel provider) {
    this.provider = provider;
    return this;
  }

  /**
   * Get provider
   * @return provider
   */
  @Valid 
  @Schema(name = "provider", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("provider")
  public @Nullable AppDeploymentProviderModel getProvider() {
    return provider;
  }

  @JsonProperty("provider")
  public void setProvider(@Nullable AppDeploymentProviderModel provider) {
    this.provider = provider;
  }

  public AppDeploymentModel providerDeploymentId(@Nullable String providerDeploymentId) {
    this.providerDeploymentId = providerDeploymentId;
    return this;
  }

  /**
   * Get providerDeploymentId
   * @return providerDeploymentId
   */
  
  @Schema(name = "providerDeploymentId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("providerDeploymentId")
  public @Nullable String getProviderDeploymentId() {
    return providerDeploymentId;
  }

  @JsonProperty("providerDeploymentId")
  public void setProviderDeploymentId(@Nullable String providerDeploymentId) {
    this.providerDeploymentId = providerDeploymentId;
  }

  public AppDeploymentModel url(@Nullable String url) {
    this.url = url;
    return this;
  }

  /**
   * Get url
   * @return url
   */
  
  @Schema(name = "url", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("url")
  public @Nullable String getUrl() {
    return url;
  }

  @JsonProperty("url")
  public void setUrl(@Nullable String url) {
    this.url = url;
  }

  public AppDeploymentModel status(@Nullable AppDeploymentStatusModel status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @Valid 
  @Schema(name = "status", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("status")
  public @Nullable AppDeploymentStatusModel getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable AppDeploymentStatusModel status) {
    this.status = status;
  }

  public AppDeploymentModel errorMessage(@Nullable String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  /**
   * Get errorMessage
   * @return errorMessage
   */
  
  @Schema(name = "errorMessage", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("errorMessage")
  public @Nullable String getErrorMessage() {
    return errorMessage;
  }

  @JsonProperty("errorMessage")
  public void setErrorMessage(@Nullable String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public AppDeploymentModel current(@Nullable Boolean current) {
    this.current = current;
    return this;
  }

  /**
   * Whether this is the instance's current deployment.
   * @return current
   */
  
  @Schema(name = "current", description = "Whether this is the instance's current deployment.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("current")
  public @Nullable Boolean getCurrent() {
    return current;
  }

  @JsonProperty("current")
  public void setCurrent(@Nullable Boolean current) {
    this.current = current;
  }

  public AppDeploymentModel createdBy(@Nullable String createdBy) {
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

  public AppDeploymentModel createdDate(@Nullable OffsetDateTime createdDate) {
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

  public AppDeploymentModel finishedDate(@Nullable OffsetDateTime finishedDate) {
    this.finishedDate = finishedDate;
    return this;
  }

  /**
   * Get finishedDate
   * @return finishedDate
   */
  @Valid 
  @Schema(name = "finishedDate", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("finishedDate")
  public @Nullable OffsetDateTime getFinishedDate() {
    return finishedDate;
  }

  @JsonProperty("finishedDate")
  public void setFinishedDate(@Nullable OffsetDateTime finishedDate) {
    this.finishedDate = finishedDate;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppDeploymentModel appDeployment = (AppDeploymentModel) o;
    return Objects.equals(this.id, appDeployment.id) &&
        Objects.equals(this.appInstanceId, appDeployment.appInstanceId) &&
        Objects.equals(this.appVersion, appDeployment.appVersion) &&
        Objects.equals(this.provider, appDeployment.provider) &&
        Objects.equals(this.providerDeploymentId, appDeployment.providerDeploymentId) &&
        Objects.equals(this.url, appDeployment.url) &&
        Objects.equals(this.status, appDeployment.status) &&
        Objects.equals(this.errorMessage, appDeployment.errorMessage) &&
        Objects.equals(this.current, appDeployment.current) &&
        Objects.equals(this.createdBy, appDeployment.createdBy) &&
        Objects.equals(this.createdDate, appDeployment.createdDate) &&
        Objects.equals(this.finishedDate, appDeployment.finishedDate);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, appInstanceId, appVersion, provider, providerDeploymentId, url, status, errorMessage, current, createdBy, createdDate, finishedDate);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppDeploymentModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    appInstanceId: ").append(toIndentedString(appInstanceId)).append("\n");
    sb.append("    appVersion: ").append(toIndentedString(appVersion)).append("\n");
    sb.append("    provider: ").append(toIndentedString(provider)).append("\n");
    sb.append("    providerDeploymentId: ").append(toIndentedString(providerDeploymentId)).append("\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    errorMessage: ").append(toIndentedString(errorMessage)).append("\n");
    sb.append("    current: ").append(toIndentedString(current)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    finishedDate: ").append(toIndentedString(finishedDate)).append("\n");
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

