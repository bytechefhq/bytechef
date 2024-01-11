package com.bytechef.embedded.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * IntegrationInstanceAllOfIntegrationModel
 */

@JsonTypeName("IntegrationInstance_allOf_integration")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-03-17T08:45:30.138877+01:00[Europe/Zagreb]")
public class IntegrationInstanceAllOfIntegrationModel {

  private Boolean allowMultipleInstances = false;

  private String componentName;

  private Integer componentVersion;

  private String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  private Long id;

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime publishedDate;

  private Integer integrationVersion;

  /**
   * The status of an integration.
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

  public IntegrationInstanceAllOfIntegrationModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public IntegrationInstanceAllOfIntegrationModel(Boolean allowMultipleInstances, String componentName, Integer componentVersion) {
    this.allowMultipleInstances = allowMultipleInstances;
    this.componentName = componentName;
    this.componentVersion = componentVersion;
  }

  public IntegrationInstanceAllOfIntegrationModel allowMultipleInstances(Boolean allowMultipleInstances) {
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

  public IntegrationInstanceAllOfIntegrationModel componentName(String componentName) {
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

  public IntegrationInstanceAllOfIntegrationModel componentVersion(Integer componentVersion) {
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

  public IntegrationInstanceAllOfIntegrationModel createdBy(String createdBy) {
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

  public IntegrationInstanceAllOfIntegrationModel createdDate(LocalDateTime createdDate) {
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

  public IntegrationInstanceAllOfIntegrationModel id(Long id) {
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

  public IntegrationInstanceAllOfIntegrationModel lastModifiedBy(String lastModifiedBy) {
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

  public IntegrationInstanceAllOfIntegrationModel lastModifiedDate(LocalDateTime lastModifiedDate) {
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

  public IntegrationInstanceAllOfIntegrationModel publishedDate(LocalDateTime publishedDate) {
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

  public IntegrationInstanceAllOfIntegrationModel integrationVersion(Integer integrationVersion) {
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

  public IntegrationInstanceAllOfIntegrationModel status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * The status of an integration.
   * @return status
  */
  
  @Schema(name = "status", description = "The status of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("status")
  public StatusEnum getStatus() {
    return status;
  }

  public void setStatus(StatusEnum status) {
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
    IntegrationInstanceAllOfIntegrationModel integrationInstanceAllOfIntegration = (IntegrationInstanceAllOfIntegrationModel) o;
    return Objects.equals(this.allowMultipleInstances, integrationInstanceAllOfIntegration.allowMultipleInstances) &&
        Objects.equals(this.componentName, integrationInstanceAllOfIntegration.componentName) &&
        Objects.equals(this.componentVersion, integrationInstanceAllOfIntegration.componentVersion) &&
        Objects.equals(this.createdBy, integrationInstanceAllOfIntegration.createdBy) &&
        Objects.equals(this.createdDate, integrationInstanceAllOfIntegration.createdDate) &&
        Objects.equals(this.id, integrationInstanceAllOfIntegration.id) &&
        Objects.equals(this.lastModifiedBy, integrationInstanceAllOfIntegration.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, integrationInstanceAllOfIntegration.lastModifiedDate) &&
        Objects.equals(this.publishedDate, integrationInstanceAllOfIntegration.publishedDate) &&
        Objects.equals(this.integrationVersion, integrationInstanceAllOfIntegration.integrationVersion) &&
        Objects.equals(this.status, integrationInstanceAllOfIntegration.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(allowMultipleInstances, componentName, componentVersion, createdBy, createdDate, id, lastModifiedBy, lastModifiedDate, publishedDate, integrationVersion, status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationInstanceAllOfIntegrationModel {\n");
    sb.append("    allowMultipleInstances: ").append(toIndentedString(allowMultipleInstances)).append("\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    publishedDate: ").append(toIndentedString(publishedDate)).append("\n");
    sb.append("    integrationVersion: ").append(toIndentedString(integrationVersion)).append("\n");
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

