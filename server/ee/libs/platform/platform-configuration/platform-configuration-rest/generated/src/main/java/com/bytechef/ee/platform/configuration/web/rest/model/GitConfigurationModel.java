package com.bytechef.ee.platform.configuration.web.rest.model;

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
 * The git configuration.
 */

@Schema(name = "GitConfiguration", description = "The git configuration.")
@JsonTypeName("GitConfiguration")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-02T08:44:56.362789+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class GitConfigurationModel {

  private String url;

  private String username;

  private String password;

  public GitConfigurationModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public GitConfigurationModel(String url, String username, String password) {
    this.url = url;
    this.username = username;
    this.password = password;
  }

  public GitConfigurationModel url(String url) {
    this.url = url;
    return this;
  }

  /**
   * The repository url.
   * @return url
   */
  @NotNull 
  @Schema(name = "url", description = "The repository url.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("url")
  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public GitConfigurationModel username(String username) {
    this.username = username;
    return this;
  }

  /**
   * The username.
   * @return username
   */
  @NotNull 
  @Schema(name = "username", description = "The username.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("username")
  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public GitConfigurationModel password(String password) {
    this.password = password;
    return this;
  }

  /**
   * The password.
   * @return password
   */
  @NotNull 
  @Schema(name = "password", description = "The password.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("password")
  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GitConfigurationModel gitConfiguration = (GitConfigurationModel) o;
    return Objects.equals(this.url, gitConfiguration.url) &&
        Objects.equals(this.username, gitConfiguration.username) &&
        Objects.equals(this.password, gitConfiguration.password);
  }

  @Override
  public int hashCode() {
    return Objects.hash(url, username, password);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GitConfigurationModel {\n");
    sb.append("    url: ").append(toIndentedString(url)).append("\n");
    sb.append("    username: ").append(toIndentedString(username)).append("\n");
    sb.append("    password: ").append(toIndentedString(password)).append("\n");
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

