package com.bytechef.atlas.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.atlas.web.rest.model.WorkflowFormatModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * PostWorkflowRequestModel
 */

@JsonTypeName("postWorkflow_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-01-10T15:10:10.566850+01:00[Europe/Zagreb]")
public class PostWorkflowRequestModel {

  @JsonProperty("definition")
  private String definition;

  @JsonProperty("format")
  private WorkflowFormatModel format;

  /**
   * Gets or Sets sourceType
   */
  public enum SourceTypeEnum {
    JDBC("JDBC");

    private String value;

    SourceTypeEnum(String value) {
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
    public static SourceTypeEnum fromValue(String value) {
      for (SourceTypeEnum b : SourceTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonProperty("sourceType")
  private SourceTypeEnum sourceType;

  public PostWorkflowRequestModel definition(String definition) {
    this.definition = definition;
    return this;
  }

  /**
   * Definition of the workflow that is executed as a job.
   * @return definition
  */
  @NotNull 
  @Schema(name = "definition", description = "Definition of the workflow that is executed as a job.", required = true)
  public String getDefinition() {
    return definition;
  }

  public void setDefinition(String definition) {
    this.definition = definition;
  }

  public PostWorkflowRequestModel format(WorkflowFormatModel format) {
    this.format = format;
    return this;
  }

  /**
   * Get format
   * @return format
  */
  @NotNull @Valid 
  @Schema(name = "format", required = true)
  public WorkflowFormatModel getFormat() {
    return format;
  }

  public void setFormat(WorkflowFormatModel format) {
    this.format = format;
  }

  public PostWorkflowRequestModel sourceType(SourceTypeEnum sourceType) {
    this.sourceType = sourceType;
    return this;
  }

  /**
   * Get sourceType
   * @return sourceType
  */
  
  @Schema(name = "sourceType", required = false)
  public SourceTypeEnum getSourceType() {
    return sourceType;
  }

  public void setSourceType(SourceTypeEnum sourceType) {
    this.sourceType = sourceType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PostWorkflowRequestModel postWorkflowRequest = (PostWorkflowRequestModel) o;
    return Objects.equals(this.definition, postWorkflowRequest.definition) &&
        Objects.equals(this.format, postWorkflowRequest.format) &&
        Objects.equals(this.sourceType, postWorkflowRequest.sourceType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(definition, format, sourceType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PostWorkflowRequestModel {\n");
    sb.append("    definition: ").append(toIndentedString(definition)).append("\n");
    sb.append("    format: ").append(toIndentedString(format)).append("\n");
    sb.append("    sourceType: ").append(toIndentedString(sourceType)).append("\n");
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

