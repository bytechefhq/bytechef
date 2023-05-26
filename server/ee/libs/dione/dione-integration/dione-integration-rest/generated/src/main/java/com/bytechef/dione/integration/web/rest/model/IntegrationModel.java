package com.bytechef.dione.integration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.category.web.rest.model.CategoryModel;
import com.bytechef.tag.web.rest.model.TagModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-27T06:33:32.145383+02:00[Europe/Zagreb]")
public class IntegrationModel {

  private CategoryModel category;

  private String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  private Long id;

  private String name;

  private String description;

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime publishedDate;

  private Integer integrationVersion;

  /**
   * A status of an integration.
   */
  public enum StatusEnum {
    PUBLISHED("PUBLISHED"),
    
    UNPUBLISHED("UNPUBLISHED");

    private String value;

    StatusEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private StatusEnum status;

  @Valid
  private List<@Valid TagModel> tags;

  @Valid
  private List<String> workflowIds;

  private Integer version;

  /**
   * Default constructor
   * @deprecated Use {@link IntegrationModel#IntegrationModel(String)}
   */
  @Deprecated
  public IntegrationModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public IntegrationModel(String name) {
    this.name = name;
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

  public IntegrationModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of the integration.
   * @return name
  */
  @NotNull 
  @Schema(name = "name", description = "The name of the integration.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public IntegrationModel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of the integration.
   * @return description
  */
  
  @Schema(name = "description", description = "The description of the integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
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

  public IntegrationModel integrationVersion(Integer integrationVersion) {
    this.integrationVersion = integrationVersion;
    return this;
  }

  /**
   * The version of an integration.
   * @return integrationVersion
  */
  
  @Schema(name = "integrationVersion", description = "The version of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationVersion")
  public Integer getIntegrationVersion() {
    return integrationVersion;
  }

  public void setIntegrationVersion(Integer integrationVersion) {
    this.integrationVersion = integrationVersion;
  }

  public IntegrationModel status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * A status of an integration.
   * @return status
  */
  
  @Schema(name = "status", description = "A status of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("status")
  public StatusEnum getStatus() {
    return status;
  }

  public void setStatus(StatusEnum status) {
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

  public IntegrationModel workflowIds(List<String> workflowIds) {
    this.workflowIds = workflowIds;
    return this;
  }

  public IntegrationModel addWorkflowIdsItem(String workflowIdsItem) {
    if (this.workflowIds == null) {
      this.workflowIds = new ArrayList<>();
    }
    this.workflowIds.add(workflowIdsItem);
    return this;
  }

  /**
   * The workflow ids belonging to this integration.
   * @return workflowIds
  */
  
  @Schema(name = "workflowIds", description = "The workflow ids belonging to this integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowIds")
  public List<String> getWorkflowIds() {
    return workflowIds;
  }

  public void setWorkflowIds(List<String> workflowIds) {
    this.workflowIds = workflowIds;
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
    return Objects.equals(this.category, integration.category) &&
        Objects.equals(this.createdBy, integration.createdBy) &&
        Objects.equals(this.createdDate, integration.createdDate) &&
        Objects.equals(this.id, integration.id) &&
        Objects.equals(this.name, integration.name) &&
        Objects.equals(this.description, integration.description) &&
        Objects.equals(this.lastModifiedBy, integration.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, integration.lastModifiedDate) &&
        Objects.equals(this.publishedDate, integration.publishedDate) &&
        Objects.equals(this.integrationVersion, integration.integrationVersion) &&
        Objects.equals(this.status, integration.status) &&
        Objects.equals(this.tags, integration.tags) &&
        Objects.equals(this.workflowIds, integration.workflowIds) &&
        Objects.equals(this.version, integration.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(category, createdBy, createdDate, id, name, description, lastModifiedBy, lastModifiedDate, publishedDate, integrationVersion, status, tags, workflowIds, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationModel {\n");
    sb.append("    category: ").append(toIndentedString(category)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    publishedDate: ").append(toIndentedString(publishedDate)).append("\n");
    sb.append("    integrationVersion: ").append(toIndentedString(integrationVersion)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
    sb.append("    workflowIds: ").append(toIndentedString(workflowIds)).append("\n");
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

