package com.bytechef.ee.automation.app.public_.web.rest.management.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppInstanceModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppStatusModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppVisibilityModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.ForeignAppInstanceModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
 * An app. It lists the instances of the request&#39;s environment the caller may see, and the instances of that environment that live in other workspaces as reduced rows.
 */

@Schema(name = "App", description = "An app. It lists the instances of the request's environment the caller may see, and the instances of that environment that live in other workspaces as reduced rows.")
@JsonTypeName("App")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-16T13:33:20.999364+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class AppModel {

  private @Nullable Long id;

  private @Nullable String uuid;

  private @Nullable String name;

  private @Nullable String description;

  private @Nullable Long workspaceId;

  private @Nullable Long ownerUserId;

  private @Nullable Long categoryId;

  private @Nullable Boolean hasIcon;

  private @Nullable AppStatusModel status;

  private @Nullable AppVisibilityModel visibility;

  private @Nullable Boolean portalDiscoverable;

  private List<@Valid AppInstanceModel> instances = new ArrayList<>();

  private List<@Valid ForeignAppInstanceModel> foreignInstances = new ArrayList<>();

  private @Nullable String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  private @Nullable String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastModifiedDate;

  private @Nullable Integer version;

  public AppModel id(@Nullable Long id) {
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

  public AppModel uuid(@Nullable String uuid) {
    this.uuid = uuid;
    return this;
  }

  /**
   * Get uuid
   * @return uuid
   */
  
  @Schema(name = "uuid", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("uuid")
  public @Nullable String getUuid() {
    return uuid;
  }

  @JsonProperty("uuid")
  public void setUuid(@Nullable String uuid) {
    this.uuid = uuid;
  }

  public AppModel name(@Nullable String name) {
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

  public AppModel description(@Nullable String description) {
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

  public AppModel workspaceId(@Nullable Long workspaceId) {
    this.workspaceId = workspaceId;
    return this;
  }

  /**
   * Get workspaceId
   * @return workspaceId
   */
  
  @Schema(name = "workspaceId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workspaceId")
  public @Nullable Long getWorkspaceId() {
    return workspaceId;
  }

  @JsonProperty("workspaceId")
  public void setWorkspaceId(@Nullable Long workspaceId) {
    this.workspaceId = workspaceId;
  }

  public AppModel ownerUserId(@Nullable Long ownerUserId) {
    this.ownerUserId = ownerUserId;
    return this;
  }

  /**
   * Get ownerUserId
   * @return ownerUserId
   */
  
  @Schema(name = "ownerUserId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("ownerUserId")
  public @Nullable Long getOwnerUserId() {
    return ownerUserId;
  }

  @JsonProperty("ownerUserId")
  public void setOwnerUserId(@Nullable Long ownerUserId) {
    this.ownerUserId = ownerUserId;
  }

  public AppModel categoryId(@Nullable Long categoryId) {
    this.categoryId = categoryId;
    return this;
  }

  /**
   * Get categoryId
   * @return categoryId
   */
  
  @Schema(name = "categoryId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("categoryId")
  public @Nullable Long getCategoryId() {
    return categoryId;
  }

  @JsonProperty("categoryId")
  public void setCategoryId(@Nullable Long categoryId) {
    this.categoryId = categoryId;
  }

  public AppModel hasIcon(@Nullable Boolean hasIcon) {
    this.hasIcon = hasIcon;
    return this;
  }

  /**
   * Whether the app has an icon. The image itself is served to signed-in Studio users only.
   * @return hasIcon
   */
  
  @Schema(name = "hasIcon", description = "Whether the app has an icon. The image itself is served to signed-in Studio users only.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("hasIcon")
  public @Nullable Boolean getHasIcon() {
    return hasIcon;
  }

  @JsonProperty("hasIcon")
  public void setHasIcon(@Nullable Boolean hasIcon) {
    this.hasIcon = hasIcon;
  }

  public AppModel status(@Nullable AppStatusModel status) {
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
  public @Nullable AppStatusModel getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable AppStatusModel status) {
    this.status = status;
  }

  public AppModel visibility(@Nullable AppVisibilityModel visibility) {
    this.visibility = visibility;
    return this;
  }

  /**
   * Get visibility
   * @return visibility
   */
  @Valid 
  @Schema(name = "visibility", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("visibility")
  public @Nullable AppVisibilityModel getVisibility() {
    return visibility;
  }

  @JsonProperty("visibility")
  public void setVisibility(@Nullable AppVisibilityModel visibility) {
    this.visibility = visibility;
  }

  public AppModel portalDiscoverable(@Nullable Boolean portalDiscoverable) {
    this.portalDiscoverable = portalDiscoverable;
    return this;
  }

  /**
   * Get portalDiscoverable
   * @return portalDiscoverable
   */
  
  @Schema(name = "portalDiscoverable", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("portalDiscoverable")
  public @Nullable Boolean getPortalDiscoverable() {
    return portalDiscoverable;
  }

  @JsonProperty("portalDiscoverable")
  public void setPortalDiscoverable(@Nullable Boolean portalDiscoverable) {
    this.portalDiscoverable = portalDiscoverable;
  }

  public AppModel instances(List<@Valid AppInstanceModel> instances) {
    this.instances = instances;
    return this;
  }

  public AppModel addInstancesItem(AppInstanceModel instancesItem) {
    if (this.instances == null) {
      this.instances = new ArrayList<>();
    }
    this.instances.add(instancesItem);
    return this;
  }

  /**
   * Get instances
   * @return instances
   */
  
  @Schema(name = "instances", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("instances")
  public List<@Valid AppInstanceModel> getInstances() {
    return instances;
  }

  @JsonProperty("instances")
  public void setInstances(List<@Valid AppInstanceModel> instances) {
    this.instances = instances;
  }

  public AppModel foreignInstances(List<@Valid ForeignAppInstanceModel> foreignInstances) {
    this.foreignInstances = foreignInstances;
    return this;
  }

  public AppModel addForeignInstancesItem(ForeignAppInstanceModel foreignInstancesItem) {
    if (this.foreignInstances == null) {
      this.foreignInstances = new ArrayList<>();
    }
    this.foreignInstances.add(foreignInstancesItem);
    return this;
  }

  /**
   * Get foreignInstances
   * @return foreignInstances
   */
  
  @Schema(name = "foreignInstances", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("foreignInstances")
  public List<@Valid ForeignAppInstanceModel> getForeignInstances() {
    return foreignInstances;
  }

  @JsonProperty("foreignInstances")
  public void setForeignInstances(List<@Valid ForeignAppInstanceModel> foreignInstances) {
    this.foreignInstances = foreignInstances;
  }

  public AppModel createdBy(@Nullable String createdBy) {
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

  public AppModel createdDate(@Nullable OffsetDateTime createdDate) {
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

  public AppModel lastModifiedBy(@Nullable String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
    return this;
  }

  /**
   * Get lastModifiedBy
   * @return lastModifiedBy
   */
  
  @Schema(name = "lastModifiedBy", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedBy")
  public @Nullable String getLastModifiedBy() {
    return lastModifiedBy;
  }

  @JsonProperty("lastModifiedBy")
  public void setLastModifiedBy(@Nullable String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  public AppModel lastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
    return this;
  }

  /**
   * Get lastModifiedDate
   * @return lastModifiedDate
   */
  @Valid 
  @Schema(name = "lastModifiedDate", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedDate")
  public @Nullable OffsetDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  @JsonProperty("lastModifiedDate")
  public void setLastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public AppModel version(@Nullable Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Get version
   * @return version
   */
  
  @Schema(name = "version", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("version")
  public @Nullable Integer getVersion() {
    return version;
  }

  @JsonProperty("version")
  public void setVersion(@Nullable Integer version) {
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
    AppModel app = (AppModel) o;
    return Objects.equals(this.id, app.id) &&
        Objects.equals(this.uuid, app.uuid) &&
        Objects.equals(this.name, app.name) &&
        Objects.equals(this.description, app.description) &&
        Objects.equals(this.workspaceId, app.workspaceId) &&
        Objects.equals(this.ownerUserId, app.ownerUserId) &&
        Objects.equals(this.categoryId, app.categoryId) &&
        Objects.equals(this.hasIcon, app.hasIcon) &&
        Objects.equals(this.status, app.status) &&
        Objects.equals(this.visibility, app.visibility) &&
        Objects.equals(this.portalDiscoverable, app.portalDiscoverable) &&
        Objects.equals(this.instances, app.instances) &&
        Objects.equals(this.foreignInstances, app.foreignInstances) &&
        Objects.equals(this.createdBy, app.createdBy) &&
        Objects.equals(this.createdDate, app.createdDate) &&
        Objects.equals(this.lastModifiedBy, app.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, app.lastModifiedDate) &&
        Objects.equals(this.version, app.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, uuid, name, description, workspaceId, ownerUserId, categoryId, hasIcon, status, visibility, portalDiscoverable, instances, foreignInstances, createdBy, createdDate, lastModifiedBy, lastModifiedDate, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    workspaceId: ").append(toIndentedString(workspaceId)).append("\n");
    sb.append("    ownerUserId: ").append(toIndentedString(ownerUserId)).append("\n");
    sb.append("    categoryId: ").append(toIndentedString(categoryId)).append("\n");
    sb.append("    hasIcon: ").append(toIndentedString(hasIcon)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    visibility: ").append(toIndentedString(visibility)).append("\n");
    sb.append("    portalDiscoverable: ").append(toIndentedString(portalDiscoverable)).append("\n");
    sb.append("    instances: ").append(toIndentedString(instances)).append("\n");
    sb.append("    foreignInstances: ").append(toIndentedString(foreignInstances)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
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

