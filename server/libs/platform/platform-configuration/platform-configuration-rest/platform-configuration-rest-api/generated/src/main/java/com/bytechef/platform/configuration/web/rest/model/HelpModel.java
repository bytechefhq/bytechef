package com.bytechef.platform.configuration.web.rest.model;

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
 * The help text that is meant to guide your users as to how to configure this action or trigger.
 */

@Schema(name = "Help", description = "The help text that is meant to guide your users as to how to configure this action or trigger.")
@JsonTypeName("Help")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-19T05:46:41.627972+01:00[Europe/Zagreb]", comments = "Generator version: 7.19.0")
public class HelpModel {

  private String body;

  private @Nullable String learnMoreUrl;

  public HelpModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public HelpModel(String body) {
    this.body = body;
  }

  public HelpModel body(String body) {
    this.body = body;
    return this;
  }

  /**
   * The help text
   * @return body
   */
  @NotNull 
  @Schema(name = "body", description = "The help text", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("body")
  public String getBody() {
    return body;
  }

  public void setBody(String body) {
    this.body = body;
  }

  public HelpModel learnMoreUrl(@Nullable String learnMoreUrl) {
    this.learnMoreUrl = learnMoreUrl;
    return this;
  }

  /**
   * The url to additional documentation
   * @return learnMoreUrl
   */
  
  @Schema(name = "learnMoreUrl", description = "The url to additional documentation", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("learnMoreUrl")
  public @Nullable String getLearnMoreUrl() {
    return learnMoreUrl;
  }

  public void setLearnMoreUrl(@Nullable String learnMoreUrl) {
    this.learnMoreUrl = learnMoreUrl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    HelpModel help = (HelpModel) o;
    return Objects.equals(this.body, help.body) &&
        Objects.equals(this.learnMoreUrl, help.learnMoreUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(body, learnMoreUrl);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class HelpModel {\n");
    sb.append("    body: ").append(toIndentedString(body)).append("\n");
    sb.append("    learnMoreUrl: ").append(toIndentedString(learnMoreUrl)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

