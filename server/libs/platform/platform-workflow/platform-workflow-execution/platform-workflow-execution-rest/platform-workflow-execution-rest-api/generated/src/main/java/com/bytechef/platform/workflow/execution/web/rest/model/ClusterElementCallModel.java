package com.bytechef.platform.workflow.execution.web.rest.model;

import java.net.URI;
import java.util.Objects;
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
 * The outcome of the calls a task made to one of its cluster elements.
 */

@Schema(name = "ClusterElementCall", description = "The outcome of the calls a task made to one of its cluster elements.")
@JsonTypeName("ClusterElementCall")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-23T23:56:46.804049+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ClusterElementCallModel {

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime endDate;

  private @Nullable String error;

  private String name;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime startDate;

  /**
   * FAILED if any call failed, otherwise the state of the latest call.
   */
  public enum StatusEnum {
    STARTED("STARTED"),
    
    COMPLETED("COMPLETED"),
    
    FAILED("FAILED");

    private final String value;

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

  public ClusterElementCallModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ClusterElementCallModel(String name, OffsetDateTime startDate, StatusEnum status) {
    this.name = name;
    this.startDate = startDate;
    this.status = status;
  }

  public ClusterElementCallModel endDate(@Nullable OffsetDateTime endDate) {
    this.endDate = endDate;
    return this;
  }

  /**
   * The time the last call ended.
   * @return endDate
   */
  @Valid 
  @Schema(name = "endDate", description = "The time the last call ended.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("endDate")
  public @Nullable OffsetDateTime getEndDate() {
    return endDate;
  }

  @JsonProperty("endDate")
  public void setEndDate(@Nullable OffsetDateTime endDate) {
    this.endDate = endDate;
  }

  public ClusterElementCallModel error(@Nullable String error) {
    this.error = error;
    return this;
  }

  /**
   * The error message of the first failed call.
   * @return error
   */
  
  @Schema(name = "error", description = "The error message of the first failed call.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("error")
  public @Nullable String getError() {
    return error;
  }

  @JsonProperty("error")
  public void setError(@Nullable String error) {
    this.error = error;
  }

  public ClusterElementCallModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The workflow node name of the cluster element.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The workflow node name of the cluster element.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public ClusterElementCallModel startDate(OffsetDateTime startDate) {
    this.startDate = startDate;
    return this;
  }

  /**
   * The time the first call started.
   * @return startDate
   */
  @NotNull @Valid 
  @Schema(name = "startDate", description = "The time the first call started.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("startDate")
  public OffsetDateTime getStartDate() {
    return startDate;
  }

  @JsonProperty("startDate")
  public void setStartDate(OffsetDateTime startDate) {
    this.startDate = startDate;
  }

  public ClusterElementCallModel status(StatusEnum status) {
    this.status = status;
    return this;
  }

  /**
   * FAILED if any call failed, otherwise the state of the latest call.
   * @return status
   */
  @NotNull 
  @Schema(name = "status", description = "FAILED if any call failed, otherwise the state of the latest call.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("status")
  public StatusEnum getStatus() {
    return status;
  }

  @JsonProperty("status")
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
    ClusterElementCallModel clusterElementCall = (ClusterElementCallModel) o;
    return Objects.equals(this.endDate, clusterElementCall.endDate) &&
        Objects.equals(this.error, clusterElementCall.error) &&
        Objects.equals(this.name, clusterElementCall.name) &&
        Objects.equals(this.startDate, clusterElementCall.startDate) &&
        Objects.equals(this.status, clusterElementCall.status);
  }

  @Override
  public int hashCode() {
    return Objects.hash(endDate, error, name, startDate, status);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClusterElementCallModel {\n");
    sb.append("    endDate: ").append(toIndentedString(endDate)).append("\n");
    sb.append("    error: ").append(toIndentedString(error)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    startDate: ").append(toIndentedString(startDate)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
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

