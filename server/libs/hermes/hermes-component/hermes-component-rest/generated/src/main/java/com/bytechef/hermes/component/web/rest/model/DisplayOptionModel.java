package com.bytechef.hermes.component.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.component.web.rest.model.DisplayOptionEntryValueModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

/**
 * DisplayOptionModel
 */

@JsonTypeName("DisplayOption")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-10-27T16:22:37.776273+02:00[Europe/Zagreb]")
public class DisplayOptionModel {

  @JsonProperty("hideWhen")
  @Valid
  private Map<String, DisplayOptionEntryValueModel> hideWhen = null;

  @JsonProperty("showWhen")
  @Valid
  private Map<String, DisplayOptionEntryValueModel> showWhen = null;

  public DisplayOptionModel hideWhen(Map<String, DisplayOptionEntryValueModel> hideWhen) {
    this.hideWhen = hideWhen;
    return this;
  }

  public DisplayOptionModel putHideWhenItem(String key, DisplayOptionEntryValueModel hideWhenItem) {
    if (this.hideWhen == null) {
      this.hideWhen = new HashMap<>();
    }
    this.hideWhen.put(key, hideWhenItem);
    return this;
  }

  /**
   * Get hideWhen
   * @return hideWhen
  */
  @Valid 
  @Schema(name = "hideWhen", required = false)
  public Map<String, DisplayOptionEntryValueModel> getHideWhen() {
    return hideWhen;
  }

  public void setHideWhen(Map<String, DisplayOptionEntryValueModel> hideWhen) {
    this.hideWhen = hideWhen;
  }

  public DisplayOptionModel showWhen(Map<String, DisplayOptionEntryValueModel> showWhen) {
    this.showWhen = showWhen;
    return this;
  }

  public DisplayOptionModel putShowWhenItem(String key, DisplayOptionEntryValueModel showWhenItem) {
    if (this.showWhen == null) {
      this.showWhen = new HashMap<>();
    }
    this.showWhen.put(key, showWhenItem);
    return this;
  }

  /**
   * Get showWhen
   * @return showWhen
  */
  @Valid 
  @Schema(name = "showWhen", required = false)
  public Map<String, DisplayOptionEntryValueModel> getShowWhen() {
    return showWhen;
  }

  public void setShowWhen(Map<String, DisplayOptionEntryValueModel> showWhen) {
    this.showWhen = showWhen;
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
    return Objects.equals(this.hideWhen, displayOption.hideWhen) &&
        Objects.equals(this.showWhen, displayOption.showWhen);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hideWhen, showWhen);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DisplayOptionModel {\n");
    sb.append("    hideWhen: ").append(toIndentedString(hideWhen)).append("\n");
    sb.append("    showWhen: ").append(toIndentedString(showWhen)).append("\n");
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

