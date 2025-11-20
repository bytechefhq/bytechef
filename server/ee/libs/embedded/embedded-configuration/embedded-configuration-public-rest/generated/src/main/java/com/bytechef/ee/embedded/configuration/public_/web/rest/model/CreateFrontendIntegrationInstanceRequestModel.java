package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.CreateFrontendIntegrationInstanceRequestConnectionModel;
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
 * CreateFrontendIntegrationInstanceRequestModel
 */

@JsonTypeName("createFrontendIntegrationInstance_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:27:31.516923+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class CreateFrontendIntegrationInstanceRequestModel {

  private CreateFrontendIntegrationInstanceRequestConnectionModel connection;

  public CreateFrontendIntegrationInstanceRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateFrontendIntegrationInstanceRequestModel(CreateFrontendIntegrationInstanceRequestConnectionModel connection) {
    this.connection = connection;
  }

  public CreateFrontendIntegrationInstanceRequestModel connection(CreateFrontendIntegrationInstanceRequestConnectionModel connection) {
    this.connection = connection;
    return this;
  }

  /**
   * Get connection
   * @return connection
   */
  @NotNull @Valid 
  @Schema(name = "connection", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("connection")
  public CreateFrontendIntegrationInstanceRequestConnectionModel getConnection() {
    return connection;
  }

  public void setConnection(CreateFrontendIntegrationInstanceRequestConnectionModel connection) {
    this.connection = connection;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateFrontendIntegrationInstanceRequestModel createFrontendIntegrationInstanceRequest = (CreateFrontendIntegrationInstanceRequestModel) o;
    return Objects.equals(this.connection, createFrontendIntegrationInstanceRequest.connection);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connection);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateFrontendIntegrationInstanceRequestModel {\n");
    sb.append("    connection: ").append(toIndentedString(connection)).append("\n");
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

