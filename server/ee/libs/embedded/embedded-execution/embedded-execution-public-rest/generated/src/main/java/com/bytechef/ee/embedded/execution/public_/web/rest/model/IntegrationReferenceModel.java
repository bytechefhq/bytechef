package com.bytechef.ee.embedded.execution.public_.web.rest.model;

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
 * The integration a workflow execution belongs to.
 */

@Schema(name = "IntegrationReference", description = "The integration a workflow execution belongs to.")
@JsonTypeName("IntegrationReference")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-18T16:40:13.932099944Z[Etc/UTC]", comments = "Generator version: 7.22.0")
public class IntegrationReferenceModel {

  private @Nullable Long id;

  private @Nullable String componentName;

  public IntegrationReferenceModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of an integration.
   * @return id
   */
  
  @Schema(name = "id", description = "The id of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public IntegrationReferenceModel componentName(@Nullable String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The name of the integration's component.
   * @return componentName
   */
  
  @Schema(name = "componentName", description = "The name of the integration's component.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentName")
  public @Nullable String getComponentName() {
    return componentName;
  }

  @JsonProperty("componentName")
  public void setComponentName(@Nullable String componentName) {
    this.componentName = componentName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IntegrationReferenceModel integrationReference = (IntegrationReferenceModel) o;
    return Objects.equals(this.id, integrationReference.id) &&
        Objects.equals(this.componentName, integrationReference.componentName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, componentName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationReferenceModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
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

