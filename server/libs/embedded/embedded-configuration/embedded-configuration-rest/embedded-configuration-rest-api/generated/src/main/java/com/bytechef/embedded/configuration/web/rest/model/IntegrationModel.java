package com.bytechef.embedded.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.embedded.configuration.web.rest.model.CategoryModel;
import com.bytechef.embedded.configuration.web.rest.model.IntegrationStatusModel;
import com.bytechef.embedded.configuration.web.rest.model.TagModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A group of workflows that make one logical integration.
 */

@Schema(name = "Integration", description = "A group of workflows that make one logical integration.")
@JsonTypeName("Integration")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-06-12T12:40:28.680343+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
public class IntegrationModel {

  private Boolean allowMultipleInstances = false;

  private CategoryModel category;

  private String componentName;

  private Integer componentVersion;

  private String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  private String description;

  private Long id;

  private Integer integrationVersion;

  @Valid
  private List<String> integrationWorkflowIds = new ArrayList<>();

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime publishedDate;

  private IntegrationStatusModel status;

  @Valid
  private List<@Valid TagModel> tags = new ArrayList<>();

  private Integer version;

  public IntegrationModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public IntegrationModel(Boolean allowMultipleInstances, String componentName, Integer componentVersion) {
    this.allowMultipleInstances = allowMultipleInstances;
    this.componentName = componentName;
    this.componentVersion = componentVersion;
  }

  public IntegrationModel allowMultipleInstances(Boolean allowMultipleInstances) {
    this.allowMultipleInstances = allowMultipleInstances;
    return this;
  }

  /**
   * If multiple instances of an integration are allowed or not.
   * @return allowMultipleInstances
  */
  @NotNull 
  @Schema(name = "allowMultipleInstances", description = "If multiple instances of an integration are allowed or not.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("allowMultipleInstances")
  public Boolean getAllowMultipleInstances() {
    return allowMultipleInstances;
  }

  public void setAllowMultipleInstances(Boolean allowMultipleInstances) {
    this.allowMultipleInstances = allowMultipleInstances;
  }

  public IntegrationModel category(CategoryModel category) {
    this.category = category;
    return this;
  }

  /**
   * Get category
   * @return category
  */
  @Valid 
  @Schema(name = "category", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("category")
  public CategoryModel getCategory() {
    return category;
  }

  public void setCategory(CategoryModel category) {
    this.category = category;
  }

  public IntegrationModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The name of the integration's component.
   * @return componentName
  */
  @NotNull 
  @Schema(name = "componentName", description = "The name of the integration's component.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public IntegrationModel componentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
    return this;
  }

  /**
   * The version of the integration's component.
   * @return componentVersion
  */
  @NotNull 
  @Schema(name = "componentVersion", description = "The version of the integration's component.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentVersion")
  public Integer getComponentVersion() {
    return componentVersion;
  }

  public void setComponentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
  }

  public IntegrationModel createdBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * The created by.
   * @return createdBy
  */
  
  @Schema(name = "createdBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The created by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdBy")
  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public IntegrationModel createdDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
    return this;
  }

  /**
   * The created date.
   * @return createdDate
  */
  @Valid 
  @Schema(name = "createdDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The created date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdDate")
  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public IntegrationModel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of an integration.
   * @return description
  */
  
  @Schema(name = "description", description = "The description of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public IntegrationModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of an integration.
   * @return id
  */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public IntegrationModel integrationVersion(Integer integrationVersion) {
    this.integrationVersion = integrationVersion;
    return this;
  }

  /**
   * The version of an integration.
   * @return integrationVersion
  */
  
  @Schema(name = "integrationVersion", accessMode = Schema.AccessMode.READ_ONLY, description = "The version of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationVersion")
  public Integer getIntegrationVersion() {
    return integrationVersion;
  }

  public void setIntegrationVersion(Integer integrationVersion) {
    this.integrationVersion = integrationVersion;
  }

  public IntegrationModel integrationWorkflowIds(List<String> integrationWorkflowIds) {
    this.integrationWorkflowIds = integrationWorkflowIds;
    return this;
  }

  public IntegrationModel addIntegrationWorkflowIdsItem(String integrationWorkflowIdsItem) {
    if (this.integrationWorkflowIds == null) {
      this.integrationWorkflowIds = new ArrayList<>();
    }
    this.integrationWorkflowIds.add(integrationWorkflowIdsItem);
    return this;
  }

  /**
   * The integration workflow ids belonging to this integration.
   * @return integrationWorkflowIds
  */
  
  @Schema(name = "integrationWorkflowIds", description = "The integration workflow ids belonging to this integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationWorkflowIds")
  public List<String> getIntegrationWorkflowIds() {
    return integrationWorkflowIds;
  }

  public void setIntegrationWorkflowIds(List<String> integrationWorkflowIds) {
    this.integrationWorkflowIds = integrationWorkflowIds;
  }

  public IntegrationModel lastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
    return this;
  }

  /**
   * The last modified by.
   * @return lastModifiedBy
  */
  
  @Schema(name = "lastModifiedBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedBy")
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  public IntegrationModel lastModifiedDate(LocalDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
    return this;
  }

  /**
   * The last modified date.
   * @return lastModifiedDate
  */
  @Valid 
  @Schema(name = "lastModifiedDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedDate")
  public LocalDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public IntegrationModel publishedDate(LocalDateTime publishedDate) {
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
  public LocalDateTime getPublishedDate() {
    return publishedDate;
  }

  public void setPublishedDate(LocalDateTime publishedDate) {
    this.publishedDate = publishedDate;
  }

  public IntegrationModel status(IntegrationStatusModel status) {
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
  public IntegrationStatusModel getStatus() {
    return status;
  }

  public void setStatus(IntegrationStatusModel status) {
    this.status = status;
  }

  public IntegrationModel tags(List<@Valid TagModel> tags) {
    this.tags = tags;
    return this;
  }

  public IntegrationModel addTagsItem(TagModel tagsItem) {
    if (this.tags == null) {
      this.tags = new ArrayList<>();
    }
    this.tags.add(tagsItem);
    return this;
  }

  /**
   * Get tags
   * @return tags
  */
  @Valid 
  @Schema(name = "tags", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("tags")
  public List<@Valid TagModel> getTags() {
    return tags;
  }

  public void setTags(List<@Valid TagModel> tags) {
    this.tags = tags;
  }

  public IntegrationModel version(Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Get version
   * @return version
  */
  
  @Schema(name = "__version", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("__version")
  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IntegrationModel integration = (IntegrationModel) o;
    return Objects.equals(this.allowMultipleInstances, integration.allowMultipleInstances) &&
        Objects.equals(this.category, integration.category) &&
        Objects.equals(this.componentName, integration.componentName) &&
        Objects.equals(this.componentVersion, integration.componentVersion) &&
        Objects.equals(this.createdBy, integration.createdBy) &&
        Objects.equals(this.createdDate, integration.createdDate) &&
        Objects.equals(this.description, integration.description) &&
        Objects.equals(this.id, integration.id) &&
        Objects.equals(this.integrationVersion, integration.integrationVersion) &&
        Objects.equals(this.integrationWorkflowIds, integration.integrationWorkflowIds) &&
        Objects.equals(this.lastModifiedBy, integration.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, integration.lastModifiedDate) &&
        Objects.equals(this.publishedDate, integration.publishedDate) &&
        Objects.equals(this.status, integration.status) &&
        Objects.equals(this.tags, integration.tags) &&
        Objects.equals(this.version, integration.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowMultipleInstances, category, componentName, componentVersion, createdBy, createdDate, description, id, integrationVersion, integrationWorkflowIds, lastModifiedBy, lastModifiedDate, publishedDate, status, tags, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationModel {\n");
    sb.append("    allowMultipleInstances: ").append(toIndentedString(allowMultipleInstances)).append("\n");
    sb.append("    category: ").append(toIndentedString(category)).append("\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    integrationVersion: ").append(toIndentedString(integrationVersion)).append("\n");
    sb.append("    integrationWorkflowIds: ").append(toIndentedString(integrationWorkflowIds)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    publishedDate: ").append(toIndentedString(publishedDate)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
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

