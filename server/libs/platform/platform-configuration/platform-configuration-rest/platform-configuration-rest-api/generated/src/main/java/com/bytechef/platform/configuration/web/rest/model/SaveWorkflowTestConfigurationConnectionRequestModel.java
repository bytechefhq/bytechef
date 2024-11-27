package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * SaveWorkflowTestConfigurationConnectionRequestModel
 */

@JsonTypeName("saveWorkflowTestConfigurationConnection_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-11-26T21:19:59.239958+01:00[Europe/Zagreb]", comments = "Generator version: 7.10.0")
public class SaveWorkflowTestConfigurationConnectionRequestModel {

  private Long connectionId;

  public SaveWorkflowTestConfigurationConnectionRequestModel connectionId(Long connectionId) {
    this.connectionId = connectionId;
    return this;
  }

  /**
   * Get connectionId
   * @return connectionId
   */
  
  @Schema(name = "connectionId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connectionId")
  public Long getConnectionId() {
    return connectionId;
  }

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
    SaveWorkflowTestConfigurationConnectionRequestModel saveWorkflowTestConfigurationConnectionRequest = (SaveWorkflowTestConfigurationConnectionRequestModel) o;
    return Objects.equals(this.connectionId, saveWorkflowTestConfigurationConnectionRequest.connectionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectionId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class SaveWorkflowTestConfigurationConnectionRequestModel {\n");
    sb.append("    connectionId: ").append(toIndentedString(connectionId)).append("\n");
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

