package com.bytechef.ee.embedded.configuration.admin.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.admin.web.rest.model.AutomationWorkflowProjectWorkflowTemplateModel;
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
 * An automation workflow catalog project.
 */

@Schema(name = "AutomationWorkflowProject", description = "An automation workflow catalog project.")
@JsonTypeName("AutomationWorkflowProject")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-28T00:53:58.627309+02:00[Europe/Zagreb]", comments = "Generator version: 7.22.0")
public class AutomationWorkflowProjectModel {

  private @Nullable String name;

  /**
   * Whether copying this project's templates creates a per-user copy or a shared reference.
   */
  public enum KindEnum {
    COPY("COPY"),
    
    REFERENCE("REFERENCE");

    private final String value;

    KindEnum(String value) {
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
    public static KindEnum fromValue(String value) {
      for (KindEnum b : KindEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private @Nullable KindEnum kind;

  @Valid
  private List<@Valid AutomationWorkflowProjectWorkflowTemplateModel> workflowTemplates = new ArrayList<>();

  public AutomationWorkflowProjectModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of the automation workflow project.
   * @return name
   */
  
  @Schema(name = "name", description = "The name of the automation workflow project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public AutomationWorkflowProjectModel kind(@Nullable KindEnum kind) {
    this.kind = kind;
    return this;
  }

  /**
   * Whether copying this project's templates creates a per-user copy or a shared reference.
   * @return kind
   */
  
  @Schema(name = "kind", description = "Whether copying this project's templates creates a per-user copy or a shared reference.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("kind")
  public @Nullable KindEnum getKind() {
    return kind;
  }

  @JsonProperty("kind")
  public void setKind(@Nullable KindEnum kind) {
    this.kind = kind;
  }

  public AutomationWorkflowProjectModel workflowTemplates(List<@Valid AutomationWorkflowProjectWorkflowTemplateModel> workflowTemplates) {
    this.workflowTemplates = workflowTemplates;
    return this;
  }

  public AutomationWorkflowProjectModel addWorkflowTemplatesItem(AutomationWorkflowProjectWorkflowTemplateModel workflowTemplatesItem) {
    if (this.workflowTemplates == null) {
      this.workflowTemplates = new ArrayList<>();
    }
    this.workflowTemplates.add(workflowTemplatesItem);
    return this;
  }

  /**
   * The list of catalog workflow templates belonging to this project.
   * @return workflowTemplates
   */
  @Valid 
  @Schema(name = "workflowTemplates", description = "The list of catalog workflow templates belonging to this project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowTemplates")
  public List<@Valid AutomationWorkflowProjectWorkflowTemplateModel> getWorkflowTemplates() {
    return workflowTemplates;
  }

  @JsonProperty("workflowTemplates")
  public void setWorkflowTemplates(List<@Valid AutomationWorkflowProjectWorkflowTemplateModel> workflowTemplates) {
    this.workflowTemplates = workflowTemplates;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AutomationWorkflowProjectModel automationWorkflowProject = (AutomationWorkflowProjectModel) o;
    return Objects.equals(this.name, automationWorkflowProject.name) &&
        Objects.equals(this.kind, automationWorkflowProject.kind) &&
        Objects.equals(this.workflowTemplates, automationWorkflowProject.workflowTemplates);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, kind, workflowTemplates);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AutomationWorkflowProjectModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    kind: ").append(toIndentedString(kind)).append("\n");
    sb.append("    workflowTemplates: ").append(toIndentedString(workflowTemplates)).append("\n");
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

