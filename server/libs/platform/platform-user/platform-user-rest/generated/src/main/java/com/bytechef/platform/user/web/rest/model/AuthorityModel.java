package com.bytechef.platform.user.web.rest.model;

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
 * A user authority.
 */

@Schema(name = "Authority", description = "A user authority.")
@JsonTypeName("Authority")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-02T08:44:56.357995+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class AuthorityModel {

  private String name;

  public AuthorityModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AuthorityModel(String name) {
    this.name = name;
  }

  public AuthorityModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of an authority.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The name of an authority.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AuthorityModel authority = (AuthorityModel) o;
    return Objects.equals(this.name, authority.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AuthorityModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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

