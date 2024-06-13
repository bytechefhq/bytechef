package com.bytechef.platform.workflow.execution.web.rest.model;

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
 * Represents an execution of a workflow.
 */

@Schema(name = "JobBasic", description = "Represents an execution of a workflow.")
@JsonTypeName("JobBasic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-06-12T20:31:10.737926+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
public class JobBasicModel {

  private String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime endDate;

  private String id;

  private String label;

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  private Integer priority;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime startDate;

  /**
   * The job's status.
   */
  public enum StatusEnum {
    CREATED("CREATED"),
    
    STARTED("STARTED"),
    
    STOPPED("STOPPED"),
    
    FAILED("FAILED"),
    
    COMPLETED("COMPLETED");

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

  private String workflowId;

  public JobBasicModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public JobBasicModel(Integer priority, LocalDateTime startDate, StatusEnum status) {
    this.priority = priority;
    this.startDate = startDate;
    this.status = status;
  }

  public JobBasicModel createdBy(String createdBy) {
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

  public JobBasicModel createdDate(LocalDateTime createdDate) {
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

  public JobBasicModel endDate(LocalDateTime endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * The time execution entered end status COMPLETED, STOPPED, FAILED
   * @return endDate
  */
  @Valid 
  @Schema(name = "endDate", description = "The time execution entered end status COMPLETED, STOPPED, FAILED", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("endDate")
  public LocalDateTime getEndDate() {
    return endDate;
  }

  public void setEndDate(LocalDateTime endDate) {
    this.endDate = endDate;
  }

  public JobBasicModel id(String id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a job.
   * @return id
  */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a job.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public JobBasicModel label(String label) {
    this.label = label;
    return this;
  }

  /**
   * The job's human-readable name.
   * @return label
  */
  
  @Schema(name = "label", accessMode = Schema.AccessMode.READ_ONLY, description = "The job's human-readable name.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public JobBasicModel lastModifiedBy(String lastModifiedBy) {
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

  public JobBasicModel lastModifiedDate(LocalDateTime lastModifiedDate) {
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

  public JobBasicModel priority(Integer priority) {
    this.priority = priority;
    return this;
  }

  /**
   * The priority value.
   * @return priority
  */
  
  @Schema(name = "priority", accessMode = Schema.AccessMode.READ_ONLY, description = "The priority value.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("priority")
  public Integer getPriority() {
    return priority;
  }

  public void setPriority(Integer priority) {
    this.priority = priority;
  }

  public JobBasicModel startDate(LocalDateTime startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * The time of when the job began.
   * @return startDate
  */
  @Valid 
  @Schema(name = "startDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The time of when the job began.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("startDate")
  public LocalDateTime getStartDate() {
    return startDate;
  }

  public void setStartDate(LocalDateTime startDate) {
    this.startDate = startDate;
  }

  public JobBasicModel status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * The job's status.
   * @return status
  */
  
  @Schema(name = "status", accessMode = Schema.AccessMode.READ_ONLY, description = "The job's status.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("status")
  public StatusEnum getStatus() {
    return status;
  }

  public void setStatus(StatusEnum status) {
    this.status = status;
  }

  public JobBasicModel workflowId(String workflowId) {
    this.workflowId = workflowId;
    return this;
  }

  /**
   * Get workflowId
   * @return workflowId
  */
  
  @Schema(name = "workflowId", accessMode = Schema.AccessMode.READ_ONLY, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowId")
  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    JobBasicModel jobBasic = (JobBasicModel) o;
    return Objects.equals(this.createdBy, jobBasic.createdBy) &&
        Objects.equals(this.createdDate, jobBasic.createdDate) &&
        Objects.equals(this.endDate, jobBasic.endDate) &&
        Objects.equals(this.id, jobBasic.id) &&
        Objects.equals(this.label, jobBasic.label) &&
        Objects.equals(this.lastModifiedBy, jobBasic.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, jobBasic.lastModifiedDate) &&
        Objects.equals(this.priority, jobBasic.priority) &&
        Objects.equals(this.startDate, jobBasic.startDate) &&
        Objects.equals(this.status, jobBasic.status) &&
        Objects.equals(this.workflowId, jobBasic.workflowId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, endDate, id, label, lastModifiedBy, lastModifiedDate, priority, startDate, status, workflowId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class JobBasicModel {\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    priority: ").append(toIndentedString(priority)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    workflowId: ").append(toIndentedString(workflowId)).append("\n");
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

