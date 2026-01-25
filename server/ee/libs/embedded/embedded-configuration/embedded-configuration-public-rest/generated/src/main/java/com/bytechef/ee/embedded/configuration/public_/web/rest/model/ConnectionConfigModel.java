package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.AuthorizationTypeModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.InputModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.OAuth2Model;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * The connection configuration
 */

@Schema(name = "ConnectionConfig", description = "The connection configuration")
@JsonTypeName("ConnectionConfig")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-23T12:36:59.411987+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class ConnectionConfigModel {

  private @Nullable AuthorizationTypeModel authorizationType;

  @Valid
  private List<@Valid InputModel> inputs = new ArrayList<>();

  private @Nullable OAuth2Model oauth2;

  public ConnectionConfigModel authorizationType(@Nullable AuthorizationTypeModel authorizationType) {
    this.authorizationType = authorizationType;
    return this;
  }

  /**
   * Get authorizationType
   * @return authorizationType
   */
  @Valid 
  @Schema(name = "authorizationType", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("authorizationType")
  public @Nullable AuthorizationTypeModel getAuthorizationType() {
    return authorizationType;
  }

  public void setAuthorizationType(@Nullable AuthorizationTypeModel authorizationType) {
    this.authorizationType = authorizationType;
  }

  public ConnectionConfigModel inputs(List<@Valid InputModel> inputs) {
    this.inputs = inputs;
    return this;
  }

  public ConnectionConfigModel addInputsItem(InputModel inputsItem) {
    if (this.inputs == null) {
      this.inputs = new ArrayList<>();
    }
    this.inputs.add(inputsItem);
    return this;
  }

  /**
   * Get inputs
   * @return inputs
   */
  @Valid 
  @Schema(name = "inputs", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputs")
  public List<@Valid InputModel> getInputs() {
    return inputs;
  }

  public void setInputs(List<@Valid InputModel> inputs) {
    this.inputs = inputs;
  }

  public ConnectionConfigModel oauth2(@Nullable OAuth2Model oauth2) {
    this.oauth2 = oauth2;
    return this;
  }

  /**
   * Get oauth2
   * @return oauth2
   */
  @Valid 
  @Schema(name = "oauth2", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("oauth2")
  public @Nullable OAuth2Model getOauth2() {
    return oauth2;
  }

  public void setOauth2(@Nullable OAuth2Model oauth2) {
    this.oauth2 = oauth2;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConnectionConfigModel connectionConfig = (ConnectionConfigModel) o;
    return Objects.equals(this.authorizationType, connectionConfig.authorizationType) &&
        Objects.equals(this.inputs, connectionConfig.inputs) &&
        Objects.equals(this.oauth2, connectionConfig.oauth2);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authorizationType, inputs, oauth2);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConnectionConfigModel {\n");
    sb.append("    authorizationType: ").append(toIndentedString(authorizationType)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("    oauth2: ").append(toIndentedString(oauth2)).append("\n");
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

