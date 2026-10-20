package com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model;

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
 * The values to create an API collection from.
 */

@Schema(name = "ApiCollectionInput", description = "The values to create an API collection from.")
@JsonTypeName("ApiCollectionInput")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T01:43:17.216712+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ApiCollectionInputModel {

  private Long projectId;

  private Integer projectVersion;

  private String name;

  private String contextPath;

  private Integer collectionVersion = 1;

  private @Nullable String description;

  public ApiCollectionInputModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ApiCollectionInputModel(Long projectId, Integer projectVersion, String name, String contextPath) {
    this.projectId = projectId;
    this.projectVersion = projectVersion;
    this.name = name;
    this.contextPath = contextPath;
  }

  public ApiCollectionInputModel projectId(Long projectId) {
    this.projectId = projectId;
    return this;
  }

  /**
   * Get projectId
   * @return projectId
   */
  @NotNull 
  @Schema(name = "projectId", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("projectId")
  public Long getProjectId() {
    return projectId;
  }

  @JsonProperty("projectId")
  public void setProjectId(Long projectId) {
    this.projectId = projectId;
  }

  public ApiCollectionInputModel projectVersion(Integer projectVersion) {
    this.projectVersion = projectVersion;
    return this;
  }

  /**
   * Get projectVersion
   * @return projectVersion
   */
  @NotNull 
  @Schema(name = "projectVersion", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("projectVersion")
  public Integer getProjectVersion() {
    return projectVersion;
  }

  @JsonProperty("projectVersion")
  public void setProjectVersion(Integer projectVersion) {
    this.projectVersion = projectVersion;
  }

  public ApiCollectionInputModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull 
  @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public ApiCollectionInputModel contextPath(String contextPath) {
    this.contextPath = contextPath;
    return this;
  }

  /**
   * The path every endpoint of the collection is served under. It must not start with a slash.
   * @return contextPath
   */
  @NotNull 
  @Schema(name = "contextPath", description = "The path every endpoint of the collection is served under. It must not start with a slash.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("contextPath")
  public String getContextPath() {
    return contextPath;
  }

  @JsonProperty("contextPath")
  public void setContextPath(String contextPath) {
    this.contextPath = contextPath;
  }

  public ApiCollectionInputModel collectionVersion(Integer collectionVersion) {
    this.collectionVersion = collectionVersion;
    return this;
  }

  /**
   * Get collectionVersion
   * @return collectionVersion
   */
  
  @Schema(name = "collectionVersion", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("collectionVersion")
  public Integer getCollectionVersion() {
    return collectionVersion;
  }

  @JsonProperty("collectionVersion")
  public void setCollectionVersion(Integer collectionVersion) {
    this.collectionVersion = collectionVersion;
  }

  public ApiCollectionInputModel description(@Nullable String description) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApiCollectionInputModel apiCollectionInput = (ApiCollectionInputModel) o;
    return Objects.equals(this.projectId, apiCollectionInput.projectId) &&
        Objects.equals(this.projectVersion, apiCollectionInput.projectVersion) &&
        Objects.equals(this.name, apiCollectionInput.name) &&
        Objects.equals(this.contextPath, apiCollectionInput.contextPath) &&
        Objects.equals(this.collectionVersion, apiCollectionInput.collectionVersion) &&
        Objects.equals(this.description, apiCollectionInput.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectId, projectVersion, name, contextPath, collectionVersion, description);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiCollectionInputModel {\n");
    sb.append("    projectId: ").append(toIndentedString(projectId)).append("\n");
    sb.append("    projectVersion: ").append(toIndentedString(projectVersion)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    contextPath: ").append(toIndentedString(contextPath)).append("\n");
    sb.append("    collectionVersion: ").append(toIndentedString(collectionVersion)).append("\n");
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

