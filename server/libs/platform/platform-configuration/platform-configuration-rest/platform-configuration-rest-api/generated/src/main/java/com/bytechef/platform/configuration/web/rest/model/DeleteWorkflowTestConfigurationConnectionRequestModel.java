package com.bytechef.platform.configuration.web.rest.model;

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
 * DeleteWorkflowTestConfigurationConnectionRequestModel
 */

@JsonTypeName("deleteWorkflowTestConfigurationConnection_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-27T14:10:00.953014+02:00[Europe/Zagreb]", comments = "Generator version: 7.21.0")
public class DeleteWorkflowTestConfigurationConnectionRequestModel {

  private Long connectionId;

  public DeleteWorkflowTestConfigurationConnectionRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public DeleteWorkflowTestConfigurationConnectionRequestModel(Long connectionId) {
    this.connectionId = connectionId;
  }

  public DeleteWorkflowTestConfigurationConnectionRequestModel connectionId(Long connectionId) {
    this.connectionId = connectionId;
    return this;
  }

  /**
   * Get connectionId
   * @return connectionId
   */
  @NotNull 
  @Schema(name = "connectionId", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("connectionId")
  public Long getConnectionId() {
    return connectionId;
  }

  @JsonProperty("connectionId")
  public void setConnectionId(Long connectionId) {
    this.connectionId = connectionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DeleteWorkflowTestConfigurationConnectionRequestModel deleteWorkflowTestConfigurationConnectionRequest = (DeleteWorkflowTestConfigurationConnectionRequestModel) o;
    return Objects.equals(this.connectionId, deleteWorkflowTestConfigurationConnectionRequest.connectionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectionId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DeleteWorkflowTestConfigurationConnectionRequestModel {\n");
    sb.append("    connectionId: ").append(toIndentedString(connectionId)).append("\n");
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

