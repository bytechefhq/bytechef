package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * GetComponentTriggerEditorDescriptionRequestModel
 */

@JsonTypeName("getComponentTriggerEditorDescription_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-08T10:29:36.882469+02:00[Europe/Zagreb]")
public class GetComponentTriggerEditorDescriptionRequestModel {

  private Long connectionId;

  @Valid
  private Map<String, Object> parameters = new HashMap<>();

  /**
   * Default constructor
   * @deprecated Use {@link GetComponentTriggerEditorDescriptionRequestModel#GetComponentTriggerEditorDescriptionRequestModel(Long, Map<String, Object>)}
   */
  @Deprecated
  public GetComponentTriggerEditorDescriptionRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GetComponentTriggerEditorDescriptionRequestModel(Long connectionId, Map<String, Object> parameters) {
    this.connectionId = connectionId;
    this.parameters = parameters;
  }

  public GetComponentTriggerEditorDescriptionRequestModel connectionId(Long connectionId) {
    this.connectionId = connectionId;
    return this;
  }

  /**
   * The connection id.
   * @return connectionId
  */
  @NotNull 
  @Schema(name = "connectionId", description = "The connection id.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("connectionId")
  public Long getConnectionId() {
    return connectionId;
  }

  public void setConnectionId(Long connectionId) {
    this.connectionId = connectionId;
  }

  public GetComponentTriggerEditorDescriptionRequestModel parameters(Map<String, Object> parameters) {
    this.parameters = parameters;
    return this;
  }

  public GetComponentTriggerEditorDescriptionRequestModel putParametersItem(String key, Object parametersItem) {
    if (this.parameters == null) {
      this.parameters = new HashMap<>();
    }
    this.parameters.put(key, parametersItem);
    return this;
  }

  /**
   * The parameters of an trigger.
   * @return parameters
  */
  @NotNull 
  @Schema(name = "parameters", description = "The parameters of an trigger.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("parameters")
  public Map<String, Object> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetComponentTriggerEditorDescriptionRequestModel getComponentTriggerEditorDescriptionRequest = (GetComponentTriggerEditorDescriptionRequestModel) o;
    return Objects.equals(this.connectionId, getComponentTriggerEditorDescriptionRequest.connectionId) &&
        Objects.equals(this.parameters, getComponentTriggerEditorDescriptionRequest.parameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectionId, parameters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetComponentTriggerEditorDescriptionRequestModel {\n");
    sb.append("    connectionId: ").append(toIndentedString(connectionId)).append("\n");
    sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
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

