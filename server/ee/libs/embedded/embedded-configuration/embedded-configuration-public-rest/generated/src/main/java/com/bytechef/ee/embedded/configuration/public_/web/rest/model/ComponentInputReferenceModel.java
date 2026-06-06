package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.ComponentPropertyGroupModel;
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
 * An all-or-nothing reference from a workflow input to a component-defined input group, with the resolved group the SDK renders.
 */

@Schema(name = "ComponentInputReference", description = "An all-or-nothing reference from a workflow input to a component-defined input group, with the resolved group the SDK renders.")
@JsonTypeName("ComponentInputReference")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-06-06T14:23:01.507447+02:00[Europe/Zagreb]", comments = "Generator version: 7.21.0")
public class ComponentInputReferenceModel {

  private String componentName;

  private Integer componentVersion;

  private String groupName;

  private @Nullable ComponentPropertyGroupModel group;

  public ComponentInputReferenceModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ComponentInputReferenceModel(String componentName, Integer componentVersion, String groupName) {
    this.componentName = componentName;
    this.componentVersion = componentVersion;
    this.groupName = groupName;
  }

  public ComponentInputReferenceModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The name of the referenced component.
   * @return componentName
   */
  @NotNull 
  @Schema(name = "componentName", description = "The name of the referenced component.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  @JsonProperty("componentName")
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public ComponentInputReferenceModel componentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
    return this;
  }

  /**
   * The version of the referenced component.
   * @return componentVersion
   */
  @NotNull 
  @Schema(name = "componentVersion", description = "The version of the referenced component.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentVersion")
  public Integer getComponentVersion() {
    return componentVersion;
  }

  @JsonProperty("componentVersion")
  public void setComponentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
  }

  public ComponentInputReferenceModel groupName(String groupName) {
    this.groupName = groupName;
    return this;
  }

  /**
   * The name of the referenced component input group.
   * @return groupName
   */
  @NotNull 
  @Schema(name = "groupName", description = "The name of the referenced component input group.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("groupName")
  public String getGroupName() {
    return groupName;
  }

  @JsonProperty("groupName")
  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public ComponentInputReferenceModel group(@Nullable ComponentPropertyGroupModel group) {
    this.group = group;
    return this;
  }

  /**
   * Get group
   * @return group
   */
  @Valid 
  @Schema(name = "group", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("group")
  public @Nullable ComponentPropertyGroupModel getGroup() {
    return group;
  }

  @JsonProperty("group")
  public void setGroup(@Nullable ComponentPropertyGroupModel group) {
    this.group = group;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ComponentInputReferenceModel componentInputReference = (ComponentInputReferenceModel) o;
    return Objects.equals(this.componentName, componentInputReference.componentName) &&
        Objects.equals(this.componentVersion, componentInputReference.componentVersion) &&
        Objects.equals(this.groupName, componentInputReference.groupName) &&
        Objects.equals(this.group, componentInputReference.group);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, componentVersion, groupName, group);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ComponentInputReferenceModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    groupName: ").append(toIndentedString(groupName)).append("\n");
    sb.append("    group: ").append(toIndentedString(group)).append("\n");
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

