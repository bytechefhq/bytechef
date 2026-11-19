package com.bytechef.ee.embedded.configuration.web.rest.model;

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
 * Contains the connection an integration instance switches to.
 */

@Schema(name = "UpdateIntegrationInstanceConnectionRequest", description = "Contains the connection an integration instance switches to.")
@JsonTypeName("UpdateIntegrationInstanceConnectionRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-19T23:38:58.514983+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class UpdateIntegrationInstanceConnectionRequestModel {

  private Long connectionId;

  public UpdateIntegrationInstanceConnectionRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UpdateIntegrationInstanceConnectionRequestModel(Long connectionId) {
    this.connectionId = connectionId;
  }

  public UpdateIntegrationInstanceConnectionRequestModel connectionId(Long connectionId) {
    this.connectionId = connectionId;
    return this;
  }

  /**
   * The id of a connection of the integration instance's connected user.
   * @return connectionId
   */
  @NotNull 
  @Schema(name = "connectionId", description = "The id of a connection of the integration instance's connected user.", requiredMode = Schema.RequiredMode.REQUIRED)
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
    UpdateIntegrationInstanceConnectionRequestModel updateIntegrationInstanceConnectionRequest = (UpdateIntegrationInstanceConnectionRequestModel) o;
    return Objects.equals(this.connectionId, updateIntegrationInstanceConnectionRequest.connectionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectionId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateIntegrationInstanceConnectionRequestModel {\n");
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

