package com.bytechef.ee.embedded.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationStatusModel;
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
 * The integration version.
 */

@Schema(name = "IntegrationVersion", description = "The integration version.")
@JsonTypeName("IntegrationVersion")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-23T12:37:00.328650+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class IntegrationVersionModel {

  private @Nullable String description;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime publishedDate;

  private @Nullable Integer version;

  private @Nullable IntegrationStatusModel status;

  public IntegrationVersionModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of an integration version.
   * @return description
   */
  
  @Schema(name = "description", description = "The description of an integration version.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public IntegrationVersionModel publishedDate(@Nullable OffsetDateTime publishedDate) {
    this.publishedDate = publishedDate;
    return this;
  }

  /**
   * The published date.
   * @return publishedDate
   */
  @Valid 
  @Schema(name = "publishedDate", description = "The published date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("publishedDate")
  public @Nullable OffsetDateTime getPublishedDate() {
    return publishedDate;
  }

  public void setPublishedDate(@Nullable OffsetDateTime publishedDate) {
    this.publishedDate = publishedDate;
  }

  public IntegrationVersionModel version(@Nullable Integer version) {
    this.version = version;
    return this;
  }

  /**
   * The version of an integration.
   * @return version
   */
  
  @Schema(name = "version", accessMode = Schema.AccessMode.READ_ONLY, description = "The version of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("version")
  public @Nullable Integer getVersion() {
    return version;
  }

  public void setVersion(@Nullable Integer version) {
    this.version = version;
  }

  public IntegrationVersionModel status(@Nullable IntegrationStatusModel status) {
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
  public @Nullable IntegrationStatusModel getStatus() {
    return status;
  }

  public void setStatus(@Nullable IntegrationStatusModel status) {
    this.status = status;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IntegrationVersionModel integrationVersion = (IntegrationVersionModel) o;
    return Objects.equals(this.description, integrationVersion.description) &&
        Objects.equals(this.publishedDate, integrationVersion.publishedDate) &&
        Objects.equals(this.version, integrationVersion.version) &&
        Objects.equals(this.status, integrationVersion.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, publishedDate, version, status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationVersionModel {\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    publishedDate: ").append(toIndentedString(publishedDate)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

