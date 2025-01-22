package com.bytechef.embedded.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.embedded.configuration.web.rest.model.IntegrationStatusModel;
import com.bytechef.platform.tag.web.rest.model.TagModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-01-22T08:20:38.185479+01:00[Europe/Zagreb]", comments = "Generator version: 7.10.0")
public class IntegrationModel {

  private Boolean allowMultipleInstances = false;

  private String componentName;

  private String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime createdDate;

  private String description;

  private String icon;

  private Long id;

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime lastModifiedDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime lastPublishedDate;

  private IntegrationStatusModel lastStatus;

  private Integer lastIntegrationVersion;

  private String name;

  private com.bytechef.platform.category.web.rest.model.CategoryModel category;

  @Valid
  private List<Long> integrationWorkflowIds = new ArrayList<>();

  @Valid
  private List<@Valid TagModel> tags = new ArrayList<>();

  private Integer version;

  public IntegrationModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public IntegrationModel(Boolean allowMultipleInstances, String componentName) {
    this.allowMultipleInstances = allowMultipleInstances;
    this.componentName = componentName;
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

  public IntegrationModel createdDate(OffsetDateTime createdDate) {
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
  public OffsetDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(OffsetDateTime createdDate) {
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

  public IntegrationModel icon(String icon) {
    this.icon = icon;
    return this;
  }

  /**
   * The icon.
   * @return icon
   */
  
  @Schema(name = "icon", description = "The icon.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("icon")
  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
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

  public IntegrationModel lastModifiedDate(OffsetDateTime lastModifiedDate) {
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
  public OffsetDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public IntegrationModel lastPublishedDate(OffsetDateTime lastPublishedDate) {
    this.lastPublishedDate = lastPublishedDate;
    return this;
  }

  /**
   * The last published date.
   * @return lastPublishedDate
   */
  @Valid 
  @Schema(name = "lastPublishedDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The last published date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastPublishedDate")
  public OffsetDateTime getLastPublishedDate() {
    return lastPublishedDate;
  }

  public void setLastPublishedDate(OffsetDateTime lastPublishedDate) {
    this.lastPublishedDate = lastPublishedDate;
  }

  public IntegrationModel lastStatus(IntegrationStatusModel lastStatus) {
    this.lastStatus = lastStatus;
    return this;
  }

  /**
   * Get lastStatus
   * @return lastStatus
   */
  @Valid 
  @Schema(name = "lastStatus", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastStatus")
  public IntegrationStatusModel getLastStatus() {
    return lastStatus;
  }

  public void setLastStatus(IntegrationStatusModel lastStatus) {
    this.lastStatus = lastStatus;
  }

  public IntegrationModel lastIntegrationVersion(Integer lastIntegrationVersion) {
    this.lastIntegrationVersion = lastIntegrationVersion;
    return this;
  }

  /**
   * The last version of an integration.
   * @return lastIntegrationVersion
   */
  
  @Schema(name = "lastIntegrationVersion", accessMode = Schema.AccessMode.READ_ONLY, description = "The last version of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastIntegrationVersion")
  public Integer getLastIntegrationVersion() {
    return lastIntegrationVersion;
  }

  public void setLastIntegrationVersion(Integer lastIntegrationVersion) {
    this.lastIntegrationVersion = lastIntegrationVersion;
  }

  public IntegrationModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of an integration.
   * @return name
   */
  
  @Schema(name = "name", description = "The name of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public IntegrationModel category(com.bytechef.platform.category.web.rest.model.CategoryModel category) {
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
  public com.bytechef.platform.category.web.rest.model.CategoryModel getCategory() {
    return category;
  }

  public void setCategory(com.bytechef.platform.category.web.rest.model.CategoryModel category) {
    this.category = category;
  }

  public IntegrationModel integrationWorkflowIds(List<Long> integrationWorkflowIds) {
    this.integrationWorkflowIds = integrationWorkflowIds;
    return this;
  }

  public IntegrationModel addIntegrationWorkflowIdsItem(Long integrationWorkflowIdsItem) {
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
  public List<Long> getIntegrationWorkflowIds() {
    return integrationWorkflowIds;
  }

  public void setIntegrationWorkflowIds(List<Long> integrationWorkflowIds) {
    this.integrationWorkflowIds = integrationWorkflowIds;
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
        Objects.equals(this.componentName, integration.componentName) &&
        Objects.equals(this.createdBy, integration.createdBy) &&
        Objects.equals(this.createdDate, integration.createdDate) &&
        Objects.equals(this.description, integration.description) &&
        Objects.equals(this.icon, integration.icon) &&
        Objects.equals(this.id, integration.id) &&
        Objects.equals(this.lastModifiedBy, integration.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, integration.lastModifiedDate) &&
        Objects.equals(this.lastPublishedDate, integration.lastPublishedDate) &&
        Objects.equals(this.lastStatus, integration.lastStatus) &&
        Objects.equals(this.lastIntegrationVersion, integration.lastIntegrationVersion) &&
        Objects.equals(this.name, integration.name) &&
        Objects.equals(this.category, integration.category) &&
        Objects.equals(this.integrationWorkflowIds, integration.integrationWorkflowIds) &&
        Objects.equals(this.tags, integration.tags) &&
        Objects.equals(this.version, integration.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowMultipleInstances, componentName, createdBy, createdDate, description, icon, id, lastModifiedBy, lastModifiedDate, lastPublishedDate, lastStatus, lastIntegrationVersion, name, category, integrationWorkflowIds, tags, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationModel {\n");
    sb.append("    allowMultipleInstances: ").append(toIndentedString(allowMultipleInstances)).append("\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    lastPublishedDate: ").append(toIndentedString(lastPublishedDate)).append("\n");
    sb.append("    lastStatus: ").append(toIndentedString(lastStatus)).append("\n");
    sb.append("    lastIntegrationVersion: ").append(toIndentedString(lastIntegrationVersion)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    category: ").append(toIndentedString(category)).append("\n");
    sb.append("    integrationWorkflowIds: ").append(toIndentedString(integrationWorkflowIds)).append("\n");
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

