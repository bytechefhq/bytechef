package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Defines rules when a property should be shown or hidden.
 */

@Schema(name = "DisplayOption", description = "Defines rules when a property should be shown or hidden.")
@JsonTypeName("DisplayOption")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-01T08:54:46.758794+01:00[Europe/Zagreb]")
public class DisplayOptionModel {

  @JsonProperty("hide")
  @Valid
  private Map<String, List<Object>> hide = null;

  @JsonProperty("show")
  @Valid
  private Map<String, List<Object>> show = null;

  public DisplayOptionModel hide(Map<String, List<Object>> hide) {
    this.hide = hide;
    return this;
  }

  public DisplayOptionModel putHideItem(String key, List<Object> hideItem) {
    if (this.hide == null) {
      this.hide = new HashMap<>();
    }
    this.hide.put(key, hideItem);
    return this;
  }

  /**
   * The map of property names and list of values to check against if the property should be hidden.
   * @return hide
  */
  @Valid 
  @Schema(name = "hide", description = "The map of property names and list of values to check against if the property should be hidden.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Map<String, List<Object>> getHide() {
    return hide;
  }

  public void setHide(Map<String, List<Object>> hide) {
    this.hide = hide;
  }

  public DisplayOptionModel show(Map<String, List<Object>> show) {
    this.show = show;
    return this;
  }

  public DisplayOptionModel putShowItem(String key, List<Object> showItem) {
    if (this.show == null) {
      this.show = new HashMap<>();
    }
    this.show.put(key, showItem);
    return this;
  }

  /**
   * The map of property names and list of values to check against if the property should be shown.
   * @return show
  */
  @Valid 
  @Schema(name = "show", description = "The map of property names and list of values to check against if the property should be shown.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Map<String, List<Object>> getShow() {
    return show;
  }

  public void setShow(Map<String, List<Object>> show) {
    this.show = show;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DisplayOptionModel displayOption = (DisplayOptionModel) o;
    return Objects.equals(this.hide, displayOption.hide) &&
        Objects.equals(this.show, displayOption.show);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hide, show);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DisplayOptionModel {\n");
    sb.append("    hide: ").append(toIndentedString(hide)).append("\n");
    sb.append("    show: ").append(toIndentedString(show)).append("\n");
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

