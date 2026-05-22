package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

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
 * A component used in a catalog workflow.
 */

@Schema(name = "AutomationWorkflowProjectComponent", description = "A component used in a catalog workflow.")
@JsonTypeName("AutomationWorkflowProjectComponent")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-05-22T14:13:22.000646+02:00[Europe/Zagreb]", comments = "Generator version: 7.21.0")
public class AutomationWorkflowProjectComponentModel {

  private @Nullable String name;

  private @Nullable String title;

  private @Nullable String icon;

  public AutomationWorkflowProjectComponentModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of the component.
   * @return name
   */
  
  @Schema(name = "name", description = "The name of the component.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public AutomationWorkflowProjectComponentModel title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * The title of the component.
   * @return title
   */
  
  @Schema(name = "title", description = "The title of the component.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public AutomationWorkflowProjectComponentModel icon(@Nullable String icon) {
    this.icon = icon;
    return this;
  }

  /**
   * The icon of the component.
   * @return icon
   */
  
  @Schema(name = "icon", description = "The icon of the component.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("icon")
  public @Nullable String getIcon() {
    return icon;
  }

  @JsonProperty("icon")
  public void setIcon(@Nullable String icon) {
    this.icon = icon;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AutomationWorkflowProjectComponentModel automationWorkflowProjectComponent = (AutomationWorkflowProjectComponentModel) o;
    return Objects.equals(this.name, automationWorkflowProjectComponent.name) &&
        Objects.equals(this.title, automationWorkflowProjectComponent.title) &&
        Objects.equals(this.icon, automationWorkflowProjectComponent.icon);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, title, icon);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AutomationWorkflowProjectComponentModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
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

