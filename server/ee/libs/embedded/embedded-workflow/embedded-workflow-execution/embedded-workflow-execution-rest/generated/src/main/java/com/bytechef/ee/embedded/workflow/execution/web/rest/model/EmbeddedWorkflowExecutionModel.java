package com.bytechef.ee.embedded.workflow.execution.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.workflow.execution.web.rest.model.WorkflowExecutionBasicModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * An integration or an automation workflow execution of a connected user.
 */

@Schema(name = "EmbeddedWorkflowExecution", description = "An integration or an automation workflow execution of a connected user.")
@JsonTypeName("EmbeddedWorkflowExecution")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-07T15:21:13.860543+01:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class EmbeddedWorkflowExecutionModel {

  private @Nullable Object automationWorkflowExecution;

  private @Nullable WorkflowExecutionBasicModel integrationWorkflowExecution;

  /**
   * Whether the execution ran an integration workflow or an automation workflow.
   */
  public enum SourceEnum {
    AUTOMATION("AUTOMATION"),
    
    INTEGRATION("INTEGRATION");

    private final String value;

    SourceEnum(String value) {
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
    public static SourceEnum fromValue(String value) {
      for (SourceEnum b : SourceEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private SourceEnum source;

  public EmbeddedWorkflowExecutionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EmbeddedWorkflowExecutionModel(SourceEnum source) {
    this.source = source;
  }

  public EmbeddedWorkflowExecutionModel automationWorkflowExecution(@Nullable Object automationWorkflowExecution) {
    this.automationWorkflowExecution = automationWorkflowExecution;
    return this;
  }

  /**
   * The automation workflow execution, set when the source is AUTOMATION.
   * @return automationWorkflowExecution
   */
  
  @Schema(name = "automationWorkflowExecution", description = "The automation workflow execution, set when the source is AUTOMATION.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("automationWorkflowExecution")
  public @Nullable Object getAutomationWorkflowExecution() {
    return automationWorkflowExecution;
  }

  @JsonProperty("automationWorkflowExecution")
  public void setAutomationWorkflowExecution(@Nullable Object automationWorkflowExecution) {
    this.automationWorkflowExecution = automationWorkflowExecution;
  }

  public EmbeddedWorkflowExecutionModel integrationWorkflowExecution(@Nullable WorkflowExecutionBasicModel integrationWorkflowExecution) {
    this.integrationWorkflowExecution = integrationWorkflowExecution;
    return this;
  }

  /**
   * Get integrationWorkflowExecution
   * @return integrationWorkflowExecution
   */
  @Valid 
  @Schema(name = "integrationWorkflowExecution", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationWorkflowExecution")
  public @Nullable WorkflowExecutionBasicModel getIntegrationWorkflowExecution() {
    return integrationWorkflowExecution;
  }

  @JsonProperty("integrationWorkflowExecution")
  public void setIntegrationWorkflowExecution(@Nullable WorkflowExecutionBasicModel integrationWorkflowExecution) {
    this.integrationWorkflowExecution = integrationWorkflowExecution;
  }

  public EmbeddedWorkflowExecutionModel source(SourceEnum source) {
    this.source = source;
    return this;
  }

  /**
   * Whether the execution ran an integration workflow or an automation workflow.
   * @return source
   */
  @NotNull 
  @Schema(name = "source", description = "Whether the execution ran an integration workflow or an automation workflow.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("source")
  public SourceEnum getSource() {
    return source;
  }

  @JsonProperty("source")
  public void setSource(SourceEnum source) {
    this.source = source;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EmbeddedWorkflowExecutionModel embeddedWorkflowExecution = (EmbeddedWorkflowExecutionModel) o;
    return Objects.equals(this.automationWorkflowExecution, embeddedWorkflowExecution.automationWorkflowExecution) &&
        Objects.equals(this.integrationWorkflowExecution, embeddedWorkflowExecution.integrationWorkflowExecution) &&
        Objects.equals(this.source, embeddedWorkflowExecution.source);
  }

  @Override
  public int hashCode() {
    return Objects.hash(automationWorkflowExecution, integrationWorkflowExecution, source);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EmbeddedWorkflowExecutionModel {\n");
    sb.append("    automationWorkflowExecution: ").append(toIndentedString(automationWorkflowExecution)).append("\n");
    sb.append("    integrationWorkflowExecution: ").append(toIndentedString(integrationWorkflowExecution)).append("\n");
    sb.append("    source: ").append(toIndentedString(source)).append("\n");
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

